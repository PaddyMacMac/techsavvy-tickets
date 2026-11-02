# Tech Savvy Tickets: run it, then deploy it to Azure

Follow the sections in order. Every action is written out. Commands for your PC are for **Windows PowerShell**; Azure commands run in **Azure Cloud Shell (Bash)**.

> Honest note: this project was written without being able to download Maven/npm packages or run JDK 25 where it was built, so it has **not been compiled or tested yet**. Section 3 is where you find that out, and section 12 lists what to check first if something does not compile.

---

## 1. What you are building

```
Browser  ──►  Azure Static Web App (React, Vite)
   │                     │ sign-in redirect
   │                     ▼
   │            Microsoft Entra ID  (issues the token)
   │
   └──► Azure Container App (Spring Boot 4) ──► Azure Database for PostgreSQL
              checks the token on every call          (Flyway creates the tables)
```

* **Clients** sign in and see only the requests they raised. **Engineers** see everything and can move requests through their stages.
* Each request gets a reference (TSS-001000...) and a reply-by time based on its priority (urgent 1 h, high 4 h, normal 8 h, low 24 h).

## 2. How login works, and why

**Decision: Microsoft Entra ID, with the React app doing the sign-in (MSAL, authorisation-code flow with PKCE) and Spring Boot acting as a resource server that only validates tokens.**

| Option | Verdict |
|---|---|
| **Entra ID + MSAL + Spring resource server** (chosen) | The app never sees a password. MFA, password reset and conditional access are Microsoft's job. Roles come from Entra "app roles", so you grant access in the Azure portal. Free tier is enough. |
| Static Web Apps built-in auth | Simple, but it protects the web site, not your separate API on Container Apps (that needs the paid Standard plan's linked backend). |
| Spring Authorization Server / Keycloak | Full control, but you would run and patch an identity system. Not worth it for a ticket form. |
| Entra External ID (customers) | The right long-term home if many *client companies* will sign in with their own accounts. Same code: only the authority URL and tenant change. Start with a normal tenant and invite clients as guests. |

How the pieces fit:
1. The browser sends the person to Microsoft to sign in. Microsoft sends back an **access token** for *your* API (scope `tickets.access`).
2. The React app puts that token in an `Authorization: Bearer ...` header.
3. Spring Security checks the token's signature, issuer (your tenant), audience (your app) and expiry, then reads the `roles` claim (`ENGINEER` or `CLIENT`).
4. A filter binds the person into a Java **ScopedValue** for the request, so services can ask "who is calling?" without touching Spring Security, and virtual threads forked for the dashboard inherit it.

**Local development uses a second login mode** (profile `local`): a demo email/password form that the backend turns into a signed token. It exists only so you can work without Azure. It is never enabled in Azure.

## 3. Install the tools (once)

1. **JDK 25** (Temurin): https://adoptium.net → choose 25 → install.
2. **Node.js 22 LTS**: https://nodejs.org
3. **Docker Desktop**: https://www.docker.com/products/docker-desktop (start it; the tests and local database need it).
4. **Git**: https://git-scm.com
5. **IntelliJ IDEA** (Ultimate is best; Community can run the backend and tests, but the npm run configuration and TypeScript tooling are Ultimate features).

## 4. Open the project in IntelliJ and run it locally

1. Unzip `techsavvy-tickets.zip` somewhere short, e.g. `C:\dev\techsavvy-tickets`.
2. IntelliJ → **File → Open** → choose the `techsavvy-tickets` folder (the one containing `pom.xml`). Choose **Trust Project**. If asked, click **Load Maven Project**.
3. **File → Project Structure → Project** → set **SDK** to JDK 25 and **Language level** to **25 (Preview) - Structured concurrency, ...** → OK. (The code uses `StructuredTaskScope`, a preview API in JDK 25.)
4. Wait for Maven to finish importing (progress bar bottom right).
5. Start the database. In IntelliJ open the **Terminal** tab and run:
   ```powershell
   docker compose up -d
   ```
6. Install the frontend packages (this also creates `package-lock.json`, which GitHub needs later):
   ```powershell
   cd frontend
   npm install
   cd ..
   ```
7. Run the tests:
   ```powershell
   cd backend
   mvn verify
   cd ..\frontend
   npm test
   cd ..
   ```
   (If `mvn` is not found, use IntelliJ's **Maven** panel → *tickets-api* → *Lifecycle* → double-click **verify**.)
8. Start the backend: in the run-configuration drop-down (top right) pick **Backend (local)** → green play button. Wait for `Started TicketsApplication`. If the drop-down is empty, create **Run → Edit Configurations → + → Application**, main class `uk.co.techsavvysolutions.tickets.TicketsApplication`, VM options `--enable-preview`, environment variable `SPRING_PROFILES_ACTIVE=local`, working directory `backend`.
9. (Optional) add demo tickets:
   ```powershell
   docker compose exec db psql -U tickets -d tickets -f /seed/dev-seed.sql
   ```
10. Start the frontend: pick **Frontend (vite dev)** and press play, or in the Terminal: `cd frontend; npm run dev`.
11. Open http://localhost:5173 and sign in with `engineer@tss.local` / `Engineer123!` (sees everything) or `client@acme.local` / `Client123!` (sees only their own).

## 5. Put it on GitHub

1. https://github.com/new → name **techsavvy-tickets** → **Private** → *do not* add a README → Create repository.
2. In the IntelliJ Terminal at the project root:
   ```powershell
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/YOUR-GITHUB-USERNAME/techsavvy-tickets.git
   git push -u origin main
   ```
   Check `frontend/package-lock.json` is in the commit (`git ls-files frontend/package-lock.json`). If not, you skipped step 4.6: run `npm install` in `frontend`, then `git add . ; git commit -m "Add lockfile" ; git push`.
3. The first push runs the **CI** workflow (GitHub → Actions tab). The two *deploy* workflows will fail until section 8 is done. That is expected.

## 6. Create the Azure resources (Cloud Shell)

1. Go to https://portal.azure.com and click the **Cloud Shell** icon (`>_`) at the top. Choose **Bash**. (First time only: let it create its storage.)
2. Click the **Upload/Download files** icon → **Upload** → select `infra/azure-setup-1.sh` and `infra/azure-setup-2.sh` from your project.
3. Edit the three lines at the top of the script:
   ```bash
   code azure-setup-1.sh
   ```
   * `SUFFIX`: short unique text such as your initials + digits.
   * `GITHUB_REPO`: `your-username/techsavvy-tickets` exactly as in the GitHub URL.
   * `LOCATION`: leave `uksouth`. If the script stops with a "location restricted / not available for your subscription" error (common on free trials), change it to `westus3`, run `bash azure-cleanup.sh <suffix>`, wait a few minutes, and run part 1 again.
   Save with **Ctrl+S**, close with **Ctrl+Q**.
4. Run it (about 10-15 minutes):
   ```bash
   bash azure-setup-1.sh
   ```
5. At the end it prints a block of **secrets and variables**. Keep that window open.

## 7. Create the Entra app registration (portal)

Portal → search **Microsoft Entra ID** → left menu **App registrations** → **New registration**.

1. **Name:** `Tech Savvy Tickets`. **Supported account types:** *Accounts in this organizational directory only*. **Redirect URI:** platform **Single-page application (SPA)**, value `http://localhost:5173/`. Click **Register**.
2. On the Overview page copy the **Application (client) ID**. (The tenant ID was printed by part 1.)
3. **Authentication** → under *Single-page application* click **Add URI** → `https://<your-web-address>/` (the "web site address" from part 1, with a trailing `/`) → **Save**.
4. **Expose an API** → next to *Application ID URI* click **Add** → accept the default (`api://<client-id>`) → **Save**. Then **Add a scope**: name `tickets.access`; who can consent *Admins and users*; display name and description `Use the tickets API`; state *Enabled* → **Add scope**.
5. **Manifest** → find `requestedAccessTokenVersion` (inside `api`) and set it to `2` (it is `null` by default; if you are on the older manifest it is `accessTokenAcceptedVersion`). **Save**. *Skipping this is the most common cause of 401 errors: Entra otherwise issues v1 tokens with a different issuer.*
6. **App roles** → **Create app role** twice:
   * Display name `Engineer`, allowed member types *Users/Groups*, value `ENGINEER`, description `Tech Savvy engineer`, enabled → Apply.
   * Display name `Client`, value `CLIENT`, description `Client staff member`, enabled → Apply.
7. **API permissions** → **Add a permission** → **My APIs** → *Tech Savvy Tickets* → **Delegated** → tick `tickets.access` → **Add permissions** → **Grant admin consent for ...** → Yes. (This stops a consent pop-up.)
8. Give people access: Entra ID → **Enterprise applications** → *Tech Savvy Tickets* → **Properties** → set **Assignment required?** to **Yes** → Save. Then **Users and groups** → **Add user/group** → pick yourself → *Select a role* → **Engineer** → Assign. Repeat for each person (choose **Client** for client staff). For people outside your organisation: Entra ID → **Users** → **Invite external user**, then assign them the Client role.
9. Back in Cloud Shell run part 2 (replace the values):
   ```bash
   bash azure-setup-2.sh <your-suffix> <application-client-id>
   ```

## 8. Add the GitHub secrets and variables

GitHub → your repo → **Settings** → **Secrets and variables** → **Actions**.

* Tab **Secrets** → **New repository secret**, four times: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`, `AZURE_STATIC_WEB_APPS_API_TOKEN` (values from the part 1 output; copy the token with no spaces).
* Tab **Variables** → **New repository variable**, six times: `ACR_NAME`, `ACA_APP_NAME`, `AZURE_RESOURCE_GROUP`, `VITE_API_BASE_URL`, `VITE_ENTRA_TENANT_ID`, and `VITE_ENTRA_CLIENT_ID` (the client ID from section 7).

## 9. Deploy

1. GitHub → **Actions** → pick **Deploy API to Azure Container Apps** → **Run workflow** → Run. Wait for green (about 5 minutes).
2. Same for **Deploy web app to Azure Static Web Apps**.
3. Check the API: open `https://<api-address>/actuator/health` → `{"status":"UP"}`. The first request after idle takes ~20 seconds because the app scales to zero to save money.
4. Open your web address, click **Sign in with Microsoft**, sign in as the person you assigned the *Engineer* role, and log a test request.

From now on, pushing to `main` redeploys whichever half you changed.

## 10. Daily workflow from IntelliJ

* Develop locally with the two run configurations (section 4).
* Commit and push from **Git → Commit** / **Push** (or the Terminal). GitHub Actions does the rest.
* Logs: portal → Resource group `rg-tss-tickets-<suffix>` → *tickets-api* → **Monitoring → Log stream**.
* New database change: add `V2__something.sql` in `backend/src/main/resources/db/migration`. Never edit an applied one. It runs automatically on the next deploy.

## 11. Cost control

The resources are small but not free forever. When you are done experimenting, run `bash azure-cleanup.sh <suffix>` in Cloud Shell. To pause only the database: `az postgres flexible-server stop --resource-group rg-tss-tickets-<suffix> --name pg-tss-<suffix>` (it auto-restarts after 7 days).
Check what you are on: portal → **Cost Management** → Cost analysis.

## 12. If something goes wrong

| Symptom | Likely cause and fix |
|---|---|
| Maven: `release version 25 not supported` | IntelliJ/Maven is using an older JDK. Project Structure → SDK 25; Settings → Build → Maven → Runner → JRE = project JDK. Check `mvn -v` says 25. |
| Compile error on `StructuredTaskScope` | Language level is not *25 (Preview)*, or the JVM option `--enable-preview` is missing. |
| Compile error on a Spring/Boot 4 import (test classes especially) | Boot 4 moved packages and starters. Compare with https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide. The likely spots are the `@WebMvcTest` import and the Testcontainers imports in `backend/src/test`. |
| `UnsupportedClassVersionError ... preview` in Docker/Azure | The container must start with `java --enable-preview`; check the `ENTRYPOINT` in `backend/Dockerfile`. |
| `mvn verify` fails with "Could not find a valid Docker environment" | Start Docker Desktop. |
| Web app: *401* from the API after sign-in | Manifest step 7.5 missed (token version 2), or `ENTRA_CLIENT_ID`/`ENTRA_TENANT_ID` on the container app are wrong. View the log stream. |
| *403* after sign-in | The person has no app role assigned (section 7 step 8). |
| Browser console: CORS error | `CORS_ALLOWED_ORIGINS` must equal the web address exactly (`https://...azurestaticapps.net`, no trailing slash). Re-run part 2 or edit the variable in the portal (Container App → Containers → Environment variables). |
| Microsoft page says *redirect URI mismatch* | Add the exact address to Entra → Authentication → SPA (section 7 step 3), trailing `/` included. |
| Static Web App deploy: "No matching Static Web App was found or the api key was invalid" | The `AZURE_STATIC_WEB_APPS_API_TOKEN` secret is wrong or has stray spaces. Re-run: `az staticwebapp secrets list --name swa-tss-<suffix> --query properties.apiKey -o tsv`. |
| `azure/login` fails: *AADSTS70025 / no matching federated identity* | `GITHUB_REPO` in part 1 did not match your repo, or you are deploying from a branch other than `main`. |
| Container app shows the Microsoft "quickstart" page | The backend deploy workflow has not run yet. |
| API crashes at start: `Failed to configure a DataSource` / password authentication failed | Environment variables not set (part 2 not run) or the stored password changed. Check them in the portal. |
| Container Apps cannot pull the image | The AcrPull role had not propagated. Run `az containerapp registry set --name tickets-api --resource-group rg-tss-tickets-<suffix> --server <acr>.azurecr.io --identity system` and redeploy. |

## 13. Where Spring AI and pgvector fit next

The local database image already includes pgvector (`dbscripts/local`), and `dbscripts/azure/02-enable-pgvector.sql` shows how to allow it in Azure. A natural next step is a `TriageAdvisor` port in the domain layer with a Spring AI implementation that suggests a category and priority from the description, and a `tickets_embedding` table for finding similar past jobs. The `TicketEvents` listener is the place to hook it in without touching the controller.

## 14. What is where

```
backend/            Spring Boot 4, JDK 25      (api / application / domain / infrastructure / config)
  src/main/resources/db/migration   Flyway schema scripts
frontend/           Vite + React 19 + Tailwind (auth / api / components / features)
dbscripts/          Local init, demo seed data, optional Azure hardening
infra/              Azure Cloud Shell scripts (setup part 1 and 2, cleanup)
.github/workflows/  ci.yml, deploy-backend.yml, deploy-frontend.yml
.run/               Shared IntelliJ run configurations
docker-compose.yml  Local PostgreSQL (pgvector image)
```

Design notes: layered architecture with ports (`TicketRepository`, `CurrentUserProvider`, `TicketEvents`) defined in the domain and implemented in infrastructure; records for DTOs, commands and events; sealed interfaces for `TicketScope`, `TicketEvent` and `DomainException` handled with exhaustive pattern-matching switches; strategy (`SlaPolicy`, `AuthStrategy`), factory (`TicketFactory`), adapter (`JpaTicketRepository`); virtual threads for requests; `StructuredTaskScope` for the three-way dashboard summary; `ScopedValue` for the signed-in user; JSpecify `@NullMarked` on every package.
