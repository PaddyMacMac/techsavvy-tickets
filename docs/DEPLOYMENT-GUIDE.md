# Run and deploy Tech Savvy Tickets

Azure provisioning and deployment use one entry point: `infra/azure-run-bicep.cmd`. The Bicep files in `infra/` are called by that launcher; do not run them individually. The old Bash setup and cleanup scripts have been removed.

## Before the first Azure run

- The project, including `.github/workflows/`, must already be pushed to the `main` branch of a GitHub repository. The launcher does not create a repository or push source code.
- Open that repository checkout in IntelliJ and use its PowerShell terminal.
- Sign in with the Azure account for this project's configured tenant and subscription. It needs permission to create resources, register apps, assign roles, and grant the configured API consent. The GitHub account needs permission to update the repository's Actions settings.
- Azure CLI and GitHub CLI are installed by the launcher through `winget` when they are missing. First use may open browser sign-in for Azure and GitHub.

The first push to `main` can start GitHub Actions before Azure and repository settings are configured. If an initial deploy run fails, run the launcher below; it configures those settings and queues new deploy runs.

The launcher is configured for this project (`pmc01`, UK South, Static Web Apps East US 2, and the tenant/subscription IDs in the script). It generates a PostgreSQL admin password and stores it encrypted for the current Windows user.

## Provision or reconcile Azure

From the project root in IntelliJ's PowerShell terminal, run:

```powershell
.\infra\azure-run-bicep.cmd
```

The launcher provisions or updates the resource group and its resources, configures the `Tech Savvy Tickets` Entra app and user role, creates or repairs GitHub's Azure workload identity trust, writes the required Actions secrets and variables, then queues the API and website workflows. You do not need to create the Entra app in the portal, copy secrets or variables by hand, or manually start the workflows.

Rerunning the launcher reconciles resources in place; it does not tear down the resource group. The same Windows account reuses its encrypted database password. A different Windows account or PC generates a new password and updates the server and API configuration without deleting database data.

**Setup complete means the workflows were queued, not that the deployments passed.** Check both runs under GitHub → **Actions** and wait for green checks before testing the site.

## Verify the deployment

1. Confirm **Deploy API to Azure Container Apps** and **Deploy web app to Azure Static Web Apps** both succeeded in GitHub Actions.
2. Open the API address printed by the launcher with `/actuator/health` appended. Its JSON response should contain `"status":"UP"`. The app may take up to 30 seconds to wake.
3. Open the website address printed by the launcher and sign in with the Azure account assigned the **Engineer** role.
4. Submit a test request and confirm it appears in the ticket list.

For an interview portfolio link, append `/demo` to the website address printed by the launcher. The current live link is [Tech Savvy Tickets demo](https://calm-cliff-05a01740f.3.azurestaticapps.net/demo). The demo accounts are:

| View | Email | Password |
|---|---|---|
| Engineer | `engineer@tss.local` | `Engineer123!` |
| Client | `client@acme.local` | `Client123!` |

The demo login runs in the browser and only selects a role for fictional sample data. Engineers can view and update the team queue; clients see their sample requests and can submit a sample request. The demo does not sign in to Entra, call the Azure API, or use the Azure database. Changes reset on sign-out, leaving the page, or refresh. These public credentials are not secure accounts; never use them with real data or reuse their passwords. The full app at the root URL still uses Entra sign-in and assigned app roles. If the Static Web App is recreated and its hostname changes, update the live link in this guide and the root README.

To give another person access later, use Microsoft Entra ID → **Enterprise applications** → **Tech Savvy Tickets** → **Users and groups**, and assign **Engineer** or **Client**. Invite external users to the tenant first if needed.

## Normal development

- Develop and test locally using the instructions in the root [README](../README.md).
- Push changes to `main`. Changes under `backend/` deploy the API; changes under `frontend/` deploy the website.
- View API logs in Azure Portal → the resource group → `tickets-api` → **Monitoring → Log stream**.
- Add database changes as new Flyway migrations in `backend/src/main/resources/db/migration`. Do not edit migrations that have already run.

## CORS and deployment errors

| Symptom | What to check |
|---|---|
| Browser reports missing `Access-Control-Allow-Origin` | Use the website URL printed by the latest launcher run; deleting and recreating a Static Web App can change its generated hostname, and an old saved URL will be rejected. Then confirm the API workflow succeeded and the Container App is running the application image, not `mcr.microsoft.com/k8se/quickstart:latest`. The launcher sets `CORS_ALLOWED_ORIGINS` to the current Static Web App origin without a trailing slash. Do not use `no-cors`. |
| `azure/login` reports `AADSTS700213` or no matching federated identity | Rerun the launcher from the correct GitHub checkout. It reads the repository's current OIDC subject, including immutable GitHub owner/repository IDs, and updates the Azure trust entry. |
| Container App shows the Microsoft quickstart page or API health is unavailable | The API workflow did not finish successfully. Open its failed run under GitHub → **Actions** and inspect the failed step. |
| Sign-in returns 401 | Confirm both deployment workflows passed and the website has the tenant and client ID configured by the launcher. |
| Sign-in returns 403 | Assign the user an **Engineer** or **Client** role in the Entra enterprise application. |
| Static Web App workflow reports an invalid token | Rerun the launcher to refresh the repository settings, then check the latest website workflow. |
| Local Maven reports `release version 25 not supported` | Set IntelliJ's Project SDK and Maven runner JRE to JDK 25. |
| Local tests report no valid Docker environment | Start Docker Desktop. |

## Remove the Azure resources

To deliberately delete the Azure resource group, run from the project root:

```powershell
.\infra\azure-run-bicep.cmd -Cleanup
```

Type `DELETE` at the confirmation prompt. Azure deletes the resource group and its resources in the background. The Entra app registrations and GitHub Actions settings are separate and remain; remove those separately only if you no longer need them.
