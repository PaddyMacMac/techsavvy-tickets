#!/usr/bin/env bash
# Deletes EVERYTHING created by azure-setup-1.sh (stops all charges). Usage: bash azure-cleanup.sh <suffix>
set -euo pipefail
SUFFIX="${1:?Usage: bash azure-cleanup.sh <suffix>}"
az group delete --name "rg-tss-tickets-${SUFFIX}" --yes --no-wait
echo "Deletion started. Also delete the 'gh-deploy-tss-${SUFFIX}' and 'Tech Savvy Tickets' app registrations in Entra ID if you no longer need them."
