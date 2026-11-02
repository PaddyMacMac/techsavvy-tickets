# Tech Savvy Tickets

A job-request ticketing app for Tech Savvy Solutions Ltd. Clients can submit requests and track their own tickets; engineers can view and manage all requests.

## Features

- Role-based access for clients and engineers
- Ticket references and priority-based reply targets
- Microsoft Entra ID sign-in in Azure
- Local demo sign-in for development

## Technology

- **Backend:** Java 25, Spring Boot 4, Spring Security, PostgreSQL, Flyway
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS
- **Hosting:** Azure Container Apps (API) and Azure Static Web Apps (frontend)

## Preview

![Tech Savvy Tickets](public/images/Screenshot.png)

## Live demo

[View Tech Savvy Tickets](https://paddymacmac.github.io/techsavvy-tickets/)

## Run locally

Install JDK 25, Node.js 22, and Docker Desktop. From the project root, start the database:

```powershell
docker compose up -d
```

In a PowerShell terminal, start the backend with the local profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
mvn -pl backend spring-boot:run
```

In another terminal, install frontend dependencies and start Vite:

```powershell
cd frontend
npm ci
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Sign in with one of these local demo accounts:

| Role | Email | Password |
|---|---|---|
| Engineer | `engineer@tss.local` | `Engineer123!` |
| Client | `client@acme.local` | `Client123!` |

These demo accounts are for local development only.

## Run tests

Run from the project root:

```powershell
mvn -B verify
```

Run frontend checks from `frontend`:

```powershell
npm ci
npm run typecheck
npm test
npm run build
```

The backend integration tests use Testcontainers and require Docker.

## Deployment

See the [deployment guide](docs/DEPLOYMENT-GUIDE.md) for Azure setup, configuration, and deployment instructions.
