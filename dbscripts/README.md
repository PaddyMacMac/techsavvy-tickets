# Database scripts

The database schema is owned by Flyway migrations in `backend/src/main/resources/db/migration`. The API applies pending migrations automatically at startup, both locally and in Azure. Add schema changes as new versioned migration files; do not edit migrations that have already run.

In Azure, the first migration run uses the PostgreSQL administrator login to create the restricted `tickets_app` login and transfer ownership of the application tables. The API then uses `tickets_app` for normal operation and later migrations. This is separate from Microsoft Entra, which authenticates human users to the application.

`dev-seed.sql` inserts sample tickets into the local Docker Compose database. Start the database and backend at least once first, so Flyway has created the tables, then run:

```powershell
docker compose exec db psql -U tickets -d tickets -f /dev-seed.sql
```

The public `/demo` portfolio page uses separate fictional tickets held in browser memory. It does not read this seed file or write to the Azure database.

## Reset the Azure database to a known sample state

For the Azure-hosted `tickets` database, run the full `azure-reset-and-seed.sql` script in IntelliJ with the Azure database connection selected. It truncates the live `tickets`, `requesters`, and `companies` tables and reseeds them with sample records. Use it only on the Azure database. If a different Entra account should own the seeded tickets, update the `requesters` insert in the script before executing it.