#!/usr/bin/env bash
# PART 2 of 2. Run in Azure Cloud Shell AFTER the Entra app registration.
# Usage:  bash azure-setup-2.sh <suffix> <entra-client-id>
set -euo pipefail

SUFFIX="${1:?Usage: bash azure-setup-2.sh <suffix> <entra-client-id>}"
ENTRA_CLIENT_ID="${2:?Usage: bash azure-setup-2.sh <suffix> <entra-client-id>}"
# shellcheck disable=SC1090
source "$HOME/.tss-${SUFFIX}.env"

echo "==> Telling the API where it lives and how to log people in"
az containerapp ingress update --name "$APP" --resource-group "$RG" --target-port 8080 -o none
az containerapp secret set --name "$APP" --resource-group "$RG" --secrets "db-password=${PG_PASSWORD}" -o none
az containerapp update --name "$APP" --resource-group "$RG" --set-env-vars \
  "SPRING_PROFILES_ACTIVE=azure" \
  "DB_URL=jdbc:postgresql://${PG_HOST}:5432/tickets?sslmode=require" \
  "DB_USER=${PG_ADMIN}" \
  "DB_PASSWORD=secretref:db-password" \
  "ENTRA_TENANT_ID=${TENANT_ID}" \
  "ENTRA_CLIENT_ID=${ENTRA_CLIENT_ID}" \
  "CORS_ALLOWED_ORIGINS=https://${SWA_HOST}" -o none

cat <<DONE

=====================  PART 2 COMPLETE  =====================
Add one more GitHub Actions VARIABLE:
    VITE_ENTRA_CLIENT_ID = ${ENTRA_CLIENT_ID}

Then push to the main branch. GitHub builds and deploys both halves.
Web site: https://${SWA_HOST}
API health (after the first deploy): https://${API_FQDN}/actuator/health
=============================================================
DONE
