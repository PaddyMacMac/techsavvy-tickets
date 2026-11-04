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

The launcher provisions or updates resources in the configured resource group, configures the `Tech Savvy Tickets` Entra app and user role, creates or repairs GitHub's Azure workload identity trust, writes the required Actions secrets and variables, then queues the API and website workflows. The resource reconciliation can temporarily set the Container App to a placeholder image; the API may be unavailable until the queued deployment finishes. Do not treat the launcher's “setup complete” message as a successful application deployment.

On the first API startup, Flyway connects as the PostgreSQL administrator, creates the non-superuser `tickets_app` login, and transfers the application tables to it. The deployment workflow waits for the API to become healthy, switches the Container App to `tickets_app`, and checks health again. Later API deployments use the application login; Flyway retains the needed permissions for subsequent migrations. This database login is for API-to-database access and does not replace Entra sign-in for people.

Rerunning the launcher reconciles resources in place; it does not tear down the resource group. The PostgreSQL administrator and application passwords are encrypted with Windows DPAPI under `%LOCALAPPDATA%\TechSavvyTickets`. Run the launcher as the same Windows user on the same PC so it can reuse them. If the encrypted application password is missing after the app has switched to `tickets_app`, the launcher intentionally stops rather than replacing the password and breaking database access. Restore the original Windows user state before reconciling.

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

## CORS and deployment errors

| Symptom | What to check |
|---|---|
| Browser reports missing `Access-Control-Allow-Origin` | Use the website URL printed by the latest launcher run; deleting and recreating a Static Web App can change its generated hostname, and an old saved URL will be rejected. Then confirm the API workflow succeeded and the Container App is running the application image, not `mcr.microsoft.com/k8se/quickstart:latest`. The launcher sets `CORS_ALLOWED_ORIGINS` to the current Static Web App origin without a trailing slash. Do not use `no-cors`. |
| `azure/login` reports `AADSTS700213` or no matching federated identity | Rerun the launcher from the correct GitHub checkout. It reads the repository's current OIDC subject, including immutable GitHub owner/repository IDs, and updates the Azure trust entry. |
| Container App shows the Microsoft quickstart page or API health is unavailable after running the launcher | The launcher reconciles the Container App before it queues the application deployment, so the placeholder image may be active temporarily. Check the latest API run under GitHub → **Actions** and let it finish. If it failed before deployment because GitHub settings were not yet configured, run the launcher again after correcting the reported setup issue; it queues a fresh API deployment. |
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
