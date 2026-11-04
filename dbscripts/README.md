# Database scripts

The schema itself is owned by Flyway: `backend/src/main/resources/db/migration` (applied automatically when the API starts, locally and in Azure).

`dev-seed.sql` adds demo tickets for local work. Run it **after** the backend has started once (so the tables exist):

```powershell
docker compose exec db psql -U tickets -d tickets -f /dev-seed.sql
```

The public `/demo` portfolio page uses separate fictional tickets held in browser memory. It does not read this seed file or write to the Azure database.
