# Database scripts

The schema itself is owned by Flyway: `backend/src/main/resources/db/migration` (applied automatically when the API starts, locally and in Azure).

| Folder   | What it is for |
|----------|----------------|
| `seed/`  | Demo tickets for local work. Run **after** the backend has started once (so the tables exist): `docker compose exec db psql -U tickets -d tickets -f /seed/dev-seed.sql` |
| `azure/` | Optional hardening for the Azure database: a least-privilege app login. |

The public `/demo` portfolio page uses separate fictional tickets held in browser memory. It does not read this seed file or write to the Azure database.
