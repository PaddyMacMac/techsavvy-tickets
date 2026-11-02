#!/usr/bin/env bash
# PART 1 of 2. Run in Azure Cloud Shell (Bash).
# Safe to re-run: it checks what already exists, skips it, and carries on from where it stopped.
set -euo pipefail

############ EDIT THESE 3 LINES ONLY ############
SUFFIX="pmc01"                                      # 3-8 lowercase letters/digits, makes names globally unique
GITHUB_REPO="paddymacmac/techsavvy-tickets"         # your GitHub owner/repo, exactly as in the repo URL
LOCATION="uksouth"                                  # if you get a "location restricted" error use: westus3
#################################################

if [[ "$GITHUB_REPO" == YOUR-GITHUB-USERNAME* ]]; then
  echo "Edit GITHUB_REPO at the top of this script first (owner/repo, e.g. jane-doe/techsavvy-tickets)." >&2
  exit 1
fi

RG="rg-tss-tickets-${SUFFIX}"
ACR="tssacr${SUFFIX}"
ENV_NAME="cae-tss-${SUFFIX}"
APP="tickets-api"
SWA="swa-tss-${SUFFIX}"
PG="pg-tss-${SUFFIX}"
PG_ADMIN="tssadmin"
STATE_FILE="$HOME/.tss-${SUFFIX}.env"

touch "$STATE_FILE"; chmod 600 "$STATE_FILE"
# shellcheck disable=SC1090
source "$STATE_FILE"

save() {  # save KEY VALUE -> remembered in the state file for re-runs and for part 2
  grep -v "^$1=" "$STATE_FILE" > "$STATE_FILE.tmp" || true
  echo "$1=$2" >> "$STATE_FILE.tmp"
  mv "$STATE_FILE.tmp" "$STATE_FILE"
  chmod 600 "$STATE_FILE"
}

exists() { "$@" >/dev/null 2>&1; }

echo "==> 1/9 Registering resource providers (one-off, can take a few minutes)"
for p in Microsoft.App Microsoft.OperationalInsights Microsoft.ContainerRegistry Microsoft.DBforPostgreSQL Microsoft.Web; do
  az provider register --namespace "$p" --wait
done

echo "==> 2/9 Resource group"
az group create --name "$RG" --location "$LOCATION" -o none

echo "==> 3/9 Container registry"
if exists az acr show --name "$ACR" --resource-group "$RG"; then
  echo "    already exists"
else
  az acr create --resource-group "$RG" --name "$ACR" --sku Basic -o none
fi

ACR_ID=$(az acr show --name "$ACR" --resource-group "$RG" --query id -o tsv)
ACR_SERVER=$(az acr show --name "$ACR" --resource-group "$RG" --query loginServer -o tsv)

echo "==> 4/9 PostgreSQL flexible server (takes about 5 minutes the first time)"
if exists az postgres flexible-server show --resource-group "$RG" --name "$PG"; then
  echo "    server already exists"
  if [[ -z "${PG_PASSWORD:-}" ]]; then
    echo "    no saved password found, setting a new one"
    PG_PASSWORD="$(openssl rand -base64 24 | tr -d "/+=" | cut -c1-24)Aa1"
    az postgres flexible-server update --resource-group "$RG" --name "$PG" --admin-password "$PG_PASSWORD" -o none
  fi
else
  PG_PASSWORD="${PG_PASSWORD:-$(openssl rand -base64 24 | tr -d "/+=" | cut -c1-24)Aa1}"
  save PG_PASSWORD "$PG_PASSWORD"
  az postgres flexible-server create \
    --resource-group "$RG" --name "$PG" --location "$LOCATION" \
    --admin-user "$PG_ADMIN" --admin-password "$PG_PASSWORD" \
    --sku-name Standard_B1ms --tier Burstable --storage-size 32 --version 17 \
    --public-access 0.0.0.0 --yes -o none
fi

save PG_PASSWORD "$PG_PASSWORD"

if exists az postgres flexible-server db show --resource-group "$RG" --server-name "$PG" --database-name tickets; then
  echo "    database already exists"
else
  az postgres flexible-server db create --resource-group "$RG" --server-name "$PG" --database-name tickets -o none
fi

PG_HOST=$(az postgres flexible-server show --resource-group "$RG" --name "$PG" --query fullyQualifiedDomainName -o tsv)

echo "==> 5/9 Container Apps environment (2-4 minutes)"
if exists az containerapp env show --name "$ENV_NAME" --resource-group "$RG"; then
  echo "    already exists"
else
  az containerapp env create --name "$ENV_NAME" --resource-group "$RG" --location "$LOCATION" -o none
fi

echo "==> 6/9 Container app (placeholder image until GitHub deploys the real one)"
if exists az containerapp show --name "$APP" --resource-group "$RG"; then
  echo "    already exists"
else
  az containerapp create \
    --name "$APP" --resource-group "$RG" --environment "$ENV_NAME" \
    --image mcr.microsoft.com/k8se/quickstart:latest --target-port 80 --ingress external \
    --cpu 0.5 --memory 1.0Gi --min-replicas 0 --max-replicas 2 --system-assigned -o none
fi

APP_PRINCIPAL=$(az containerapp show --name "$APP" --resource-group "$RG" --query identity.principalId -o tsv)

if [[ -z "$(az role assignment list --assignee "$APP_PRINCIPAL" --scope "$ACR_ID" --role AcrPull --query "[0].id" -o tsv)" ]]; then
  az role assignment create --assignee-object-id "$APP_PRINCIPAL" --assignee-principal-type ServicePrincipal \
    --role AcrPull --scope "$ACR_ID" -o none
  echo "    waiting 60s for the AcrPull role to take effect..."
  sleep 60
fi

az containerapp registry set --name "$APP" --resource-group "$RG" --server "$ACR_SERVER" --identity system -o none

API_FQDN=$(az containerapp show --name "$APP" --resource-group "$RG" --query properties.configuration.ingress.fqdn -o tsv)

echo "==> 7/9 Static Web App"
if exists az staticwebapp show --name "$SWA" --resource-group "$RG"; then
  echo "    already exists"
else
  az staticwebapp create --name "$SWA" --resource-group "$RG" --location westeurope --sku Free -o none
fi

SWA_HOST=$(az staticwebapp show --name "$SWA" --resource-group "$RG" --query defaultHostname -o tsv)
SWA_TOKEN=$(az staticwebapp secrets list --name "$SWA" --resource-group "$RG" --query properties.apiKey -o tsv)

echo "==> 8/9 GitHub sign-in identity (OIDC, no stored password)"
SUB_ID=$(az account show --query id -o tsv)
TENANT_ID=$(az account show --query tenantId -o tsv)

GH_APP_ID=$(az ad app list --display-name "gh-deploy-tss-${SUFFIX}" --query "[0].appId" -o tsv)

if [[ -z "$GH_APP_ID" ]]; then
  GH_APP_ID=$(az ad app create --display-name "gh-deploy-tss-${SUFFIX}" --query appId -o tsv)
fi

az ad sp show --id "$GH_APP_ID" >/dev/null 2>&1 || az ad sp create --id "$GH_APP_ID" -o none

if [[ -z "$(az ad app federated-credential list --id "$GH_APP_ID" --query "[?name==\"github-main\"].name" -o tsv)" ]]; then
  az ad app federated-credential create --id "$GH_APP_ID" --parameters "{
    \"name\": \"github-main\",
    \"issuer\": \"https://token.actions.githubusercontent.com\",
    \"subject\": \"repo:${GITHUB_REPO}:ref:refs/heads/main\",
    \"audiences\": [\"api://AzureADTokenExchange\"]
  }" -o none
fi

GH_SP_OBJECT=$(az ad sp show --id "$GH_APP_ID" --query id -o tsv)
RG_ID=$(az group show --name "$RG" --query id -o tsv)

az role assignment create --assignee-object-id "$GH_SP_OBJECT" --assignee-principal-type ServicePrincipal --role Contributor --scope "$RG_ID" -o none 2>/dev/null || true
az role assignment create --assignee-object-id "$GH_SP_OBJECT" --assignee-principal-type ServicePrincipal --role AcrPush --scope "$ACR_ID" -o none 2>/dev/null || true

echo "==> 9/9 Saving details for part 2"
save SUFFIX "$SUFFIX"
save RG "$RG"
save ACR "$ACR"
save APP "$APP"
save PG_HOST "$PG_HOST"
save PG_ADMIN "$PG_ADMIN"
save API_FQDN "$API_FQDN"
save SWA_HOST "$SWA_HOST"
save TENANT_ID "$TENANT_ID"

cat <<SUMMARY

=====================  PART 1 COMPLETE  =====================
Details are saved in $STATE_FILE (it holds the database password - keep it private).

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

NEXT: do the Entra app registration, then run azure-setup-2.sh.
=============================================================
SUMMARY
