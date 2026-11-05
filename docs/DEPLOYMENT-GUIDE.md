# Run and deploy Tech Savvy Tickets

Azure provisioning and deployment use one entry point: `infra/azure-run-bicep.cmd`. The Bicep files in `infra/` are called by that launcher; do not run them individually.

## Before running the Azure launcher

- The project, including `.github/workflows/`, must already be pushed to the `main` branch of a GitHub repository. The launcher does not create a repository or push source code.
- Open that repository checkout in IntelliJ and use its PowerShell terminal.
- Sign in with the Azure account for this project's configured tenant and subscription. It needs permission to create resources, register apps, assign roles, and grant the configured API consent. The GitHub account needs permission to update the repository's Actions settings.
- Azure CLI and GitHub CLI are installed by the launcher through `winget` when they are missing. First use may open browser sign-in for Azure and GitHub.

The first push to `main` can start GitHub Actions before Azure and repository settings are configured. If a workflow fails because setup is incomplete, fix the setup by running the launcher; it saves the settings and queues fresh deployments.

The launcher is configured for this project's **live** Azure subscription and resource group (`pmc01`, UK South, Static Web Apps East US 2, and the tenant/subscription IDs in the script). It is not a staging or dry-run command. It generates separate PostgreSQL administrator and application passwords and stores them encrypted for the current Windows user.

## Provision or reconcile Azure

From the project root in IntelliJ's PowerShell terminal, run:

```powershell
.\infra\azure-run-bicep.cmd
```

The launcher provisions or updates resources in the configured resource group, configures the `Tech Savvy Tickets` Entra app and user role, creates or repairs GitHub's Azure workload identity trust, writes the required Actions secrets and variables, then queues the API and website workflows. On first setup it creates and configures the API Container App with a placeholder image until the queued API deployment completes. On later runs it preserves the existing Container App image, configuration, and database secrets while reconciling the other resources.

On the first API startup, Flyway connects as the PostgreSQL administrator, creates the non-superuser `tickets_app` login, and transfers the application tables to it. The deployment workflow waits for the API to become healthy, switches the Container App to `tickets_app`, and checks health again. Later API deployments use the application login; Flyway retains the needed permissions for subsequent migrations. This database login is for API-to-database access and does not replace Entra sign-in for people.

Rerunning the launcher reconciles resources in place; it does not tear down the resource group. The PostgreSQL administrator and initial application passwords are encrypted with Windows DPAPI under `%LOCALAPPDATA%\TechSavvyTickets`. Run the launcher as the same Windows user on the same PC so it can reuse the administrator password. Once the API Container App exists, the launcher preserves its database secrets rather than replacing them from local state.

The GitHub deployment identity has `AcrPush` on the container registry and `Container Apps Contributor` scoped to the API Container App, not the resource group. That role can manage the API app and its secrets. The launcher removes its previous resource-group `Contributor` assignment after the narrower assignments are deployed.

**Setup complete means the workflows were queued, not that the deployments passed.** Check both runs under GitHub → **Actions** and wait for green checks before testing the site. Backend changes under `backend/` trigger the API workflow; frontend changes under `frontend/` trigger the website workflow. The launcher explicitly queues both.

## Verify the deployment

1. Confirm the latest **Deploy API to Azure Container Apps** and **Deploy web app to Azure Static Web Apps** runs succeeded in GitHub Actions.
2. Open the API address printed by the launcher with `/actuator/health` appended. Its JSON response should contain `"status":"UP"`.
3. Open the website address printed by the launcher and sign in with the Azure account assigned the **Engineer** role.
4. Submit a test ticket and confirm it appears in the ticket list.

For an interview portfolio link, append `/demo` to the website address printed by the launcher. The current live link is [Tech Savvy Tickets demo](https://calm-cliff-05a01740f.3.azurestaticapps.net/demo). The demo accounts are:

| View | Email | Password |
|---|---|---|
| Engineer | `engineer@tss.local` | `Engineer123!` |
| Client | `client@acme.local` | `Client123!` |

The demo login runs in the browser and only selects a role for fictional sample data. Engineers can view and update the team queue; clients see their sample requests and can submit a sample request. The demo does not sign in to Entra, call the Azure API, or use the Azure database. Changes reset on sign-out, leaving the page, or refresh. These public credentials are not secure accounts; never use them with real data or reuse their passwords. The full app at the root URL still uses Entra sign-in and assigned app roles. If the Static Web App is recreated and its hostname changes, update the live link in this guide and the root README.

To give another person access later, use Microsoft Entra ID → **Enterprise applications** → **Tech Savvy Tickets** → **Users and groups**, and assign **Engineer** or **Client**. Invite external users to the tenant first if needed.

## Normal development

- Develop and test locally using the instructions in the root [README](../README.md).
- Push changes to `main`. Changes under `backend/` trigger the API deployment; changes under `frontend/` trigger the website deployment. Each workflow can also be started manually from GitHub Actions.
- View API logs in Azure Portal → the resource group → `tickets-api` → **Monitoring → Log stream**.
- Add database changes as new Flyway migrations in `backend/src/main/resources/db/migration`. Do not edit migrations that have already run.

## Reset live database and load sample tickets

To replace all live tickets, companies, and requesters with the sample records, open `dbscripts/azure-reset-and-seed.sql` in IntelliJ, select the Azure `tickets` database connection, and execute the whole file. The script assigns the sample tickets to the Entra user ID, name, and email currently in its requester insert; update those values if a different account should own the tickets. Ensure your current public IP is temporarily allowed in the PostgreSQL firewall before connecting.

This permanently deletes existing live ticket data; back up the database first if you may need it. Remove the temporary firewall rule after the database operation.

## CORS and deployment errors

| Symptom | What to check |
|---|---|
| Browser reports missing `Access-Control-Allow-Origin` | Use the website URL printed by the latest launcher run; deleting and recreating a Static Web App can change its generated hostname, and an old saved URL will be rejected. Then confirm the API workflow succeeded and the Container App is running the application image, not `mcr.microsoft.com/k8se/quickstart:latest`. The launcher sets `CORS_ALLOWED_ORIGINS` to the current Static Web App origin without a trailing slash. Do not use `no-cors`. |
| `azure/login` reports `AADSTS700213` or no matching federated identity | Rerun the launcher from the correct GitHub checkout. It reads the repository's current OIDC subject, including immutable GitHub owner/repository IDs, and updates the Azure trust entry. |
| API health is unavailable after running the launcher | Check the latest API run under GitHub → **Actions**. A failed health check now reports the HTTP status, active revision state, and recent Container Apps system events. On a rerun, the launcher preserves the existing API configuration and secrets; on first setup, the placeholder image is active until the queued API deployment completes. |
| API rollout fails with `ContainerAppSecretRefNotFound` for `db-app-password` | The Container App has not received the application-password secret yet. Run `.\infra\azure-run-bicep.cmd` from the correct checkout, same Windows account, and same PC; then check the newly queued API workflow. |
| Sign-in returns 401 | Confirm both deployment workflows passed and the website has the tenant and client ID configured by the launcher. |
| Sign-in returns 403 | Assign the user an **Engineer** or **Client** role in the Entra enterprise application. |
| Static Web App workflow reports an invalid token | Rerun the launcher to refresh the repository settings, then check the latest website workflow. |
| Local Maven reports `release version 25 not supported` | Set IntelliJ's Project SDK and Maven runner JRE to JDK 25. |
| Local tests report no valid Docker environment | Start Docker Desktop. |

## Remove the Azure resources

To deliberately delete the live Azure resource group, run from the project root:

```powershell
.\infra\azure-run-bicep.cmd -Cleanup
```

Type `DELETE` at the confirmation prompt. Azure deletes the resource group and its resources in the background. This includes the API, database, registry, and website; the Entra app registrations and GitHub Actions settings are separate and remain. Remove those separately only if you no longer need them.
