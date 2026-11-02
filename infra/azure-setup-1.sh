#!/usr/bin/env bash
# PART 1 of 2. Run in Azure Cloud Shell (Bash). Creates everything except the Entra app registration.
# Safe to re-run: it skips things that already exist.
set -euo pipefail

############ EDIT THESE 3 LINES ONLY ############
SUFFIX="pmc01"                              # 3-8 lowercase letters/digits, makes names globally unique
GITHUB_REPO="PaddyMacMac/techsavvy-tickets"  # your GitHub owner/repo, exactly as in the repo URL
LOCATION="uksouth"                         # if you get a "location restricted" error use: westus3
#################################################

if [[ "$GITHUB_REPO" == YOUR-GITHUB-USERNAME* ]]; then
  echo "Edit GITHUB_REPO at the top of this script first (owner/repo, e.g. jane-doe/techsavvy-tickets)." >&2; exit 1
fi

RG="rg-tss-tickets-${SUFFIX}"
ACR="tssacr${SUFFIX}"
ENV_NAME="cae-tss-${SUFFIX}"
APP="tickets-api"
SWA="swa-tss-${SUFFIX}"
PG="pg-tss-${SUFFIX}"
PG_ADMIN="tssadmin"
STATE_FILE="$HOME/.tss-${SUFFIX}.env"

echo "==> Registering resource providers (one-off, can take a minute)"
for p in Microsoft.App Microsoft.OperationalInsights Microsoft.ContainerRegistry Microsoft.DBforPostgreSQL Microsoft.Web; do
  az provider register --namespace "$p" --wait
done

echo "==> Resource group"
az group create --name "$RG" --location "$LOCATION" -o none

echo "==> Container registry"
az acr create --resource-group "$RG" --name "$ACR" --sku Basic -o none
ACR_ID=$(az acr show --name "$ACR" --query id -o tsv)
ACR_SERVER=$(az acr show --name "$ACR" --query loginServer -o tsv)

echo "==> PostgreSQL flexible server (takes about 5 minutes)"
if [[ -f "$STATE_FILE" ]] && grep -q '^PG_PASSWORD=' "$STATE_FILE"; then
  # shellcheck disable=SC1090
  source "$STATE_FILE"
else
  PG_PASSWORD="$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-24)Aa1"
fi
az postgres flexible-server create \
  --resource-group "$RG" --name "$PG" --location "$LOCATION" \
  --admin-user "$PG_ADMIN" --admin-password "$PG_PASSWORD" \
  --sku-name Standard_B1ms --tier Burstable --storage-size 32 --version 17 \
  --public-access 0.0.0.0 --yes -o none
az postgres flexible-server db create --resource-group "$RG" --server-name "$PG" --database-name tickets -o none
PG_HOST=$(az postgres flexible-server show --resource-group "$RG" --name "$PG" --query fullyQualifiedDomainName -o tsv)

echo "==> Container Apps environment"
az containerapp env create --name "$ENV_NAME" --resource-group "$RG" --location "$LOCATION" -o none

echo "==> Container app (placeholder image until GitHub deploys the real one)"
az containerapp create \
  --name "$APP" --resource-group "$RG" --environment "$ENV_NAME" \
  --image mcr.microsoft.com/k8se/quickstart:latest --target-port 80 --ingress external \
  --cpu 0.5 --memory 1.0Gi --min-replicas 0 --max-replicas 2 --system-assigned -o none
APP_PRINCIPAL=$(az containerapp show --name "$APP" --resource-group "$RG" --query identity.principalId -o tsv)
az role assignment create --assignee-object-id "$APP_PRINCIPAL" --assignee-principal-type ServicePrincipal \
  --role AcrPull --scope "$ACR_ID" -o none
echo "    waiting 60s for the AcrPull role to propagate..."; sleep 60
az containerapp registry set --name "$APP" --resource-group "$RG" --server "$ACR_SERVER" --identity system -o none
API_FQDN=$(az containerapp show --name "$APP" --resource-group "$RG" --query properties.configuration.ingress.fqdn -o tsv)

echo "==> Static Web App"
az staticwebapp create --name "$SWA" --resource-group "$RG" --location westeurope --sku Free -o none
SWA_HOST=$(az staticwebapp show --name "$SWA" --resource-group "$RG" --query defaultHostname -o tsv)
SWA_TOKEN=$(az staticwebapp secrets list --name "$SWA" --resource-group "$RG" --query properties.apiKey -o tsv)

echo "==> GitHub sign-in identity (OIDC, no stored password)"
SUB_ID=$(az account show --query id -o tsv)
TENANT_ID=$(az account show --query tenantId -o tsv)
GH_APP_ID=$(az ad app create --display-name "gh-deploy-tss-${SUFFIX}" --query appId -o tsv)
az ad sp create --id "$GH_APP_ID" -o none
az ad app federated-credential create --id "$GH_APP_ID" --parameters "{
  \"name\": \"github-main\",
  \"issuer\": \"https://token.actions.githubusercontent.com\",
  \"subject\": \"repo:${GITHUB_REPO}:ref:refs/heads/main\",
  \"audiences\": [\"api://AzureADTokenExchange\"]
}" -o none
GH_SP_OBJECT=$(az ad sp show --id "$GH_APP_ID" --query id -o tsv)
RG_ID=$(az group show --name "$RG" --query id -o tsv)
az role assignment create --assignee-object-id "$GH_SP_OBJECT" --assignee-principal-type ServicePrincipal --role Contributor --scope "$RG_ID" -o none
az role assignment create --assignee-object-id "$GH_SP_OBJECT" --assignee-principal-type ServicePrincipal --role AcrPush --scope "$ACR_ID" -o none

cat > "$STATE_FILE" <<STATE
SUFFIX=$SUFFIX
RG=$RG
ACR=$ACR
APP=$APP
PG_HOST=$PG_HOST
PG_ADMIN=$PG_ADMIN
PG_PASSWORD=$PG_PASSWORD
API_FQDN=$API_FQDN
SWA_HOST=$SWA_HOST
TENANT_ID=$TENANT_ID
STATE
chmod 600 "$STATE_FILE"

cat <<SUMMARY

=====================  PART 1 COMPLETE  =====================
Saved to $STATE_FILE (the database password is in there - keep it private)

Your web site address : https://${SWA_HOST}
Your API address      : https://${API_FQDN}

GitHub -> repo -> Settings -> Secrets and variables -> Actions

  SECRETS (tab "Secrets" > New repository secret)
    AZURE_CLIENT_ID                    = ${GH_APP_ID}
    AZURE_TENANT_ID                    = ${TENANT_ID}
    AZURE_SUBSCRIPTION_ID              = ${SUB_ID}
    AZURE_STATIC_WEB_APPS_API_TOKEN    = ${SWA_TOKEN}

  VARIABLES (tab "Variables" > New repository variable)
    ACR_NAME                = ${ACR}
    ACA_APP_NAME            = ${APP}
    AZURE_RESOURCE_GROUP    = ${RG}
    VITE_API_BASE_URL       = https://${API_FQDN}
    VITE_ENTRA_TENANT_ID    = ${TENANT_ID}
    (VITE_ENTRA_CLIENT_ID is added after the Entra step in the guide)

NEXT: do the Entra app registration (guide section 7), then run azure-setup-2.sh.
=============================================================
SUMMARY
