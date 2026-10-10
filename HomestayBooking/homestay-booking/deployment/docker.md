# Docker — Local SQL Server

**Compose file:** `Backend/compose.yml`  
**Env file:** `Backend/.env` (from `dev-environment.template`)

## Services

| Service | Image | Purpose |
|---------|-------|---------|
| `sqlserver` | `mcr.microsoft.com/mssql/server:2022-latest` | SQL Server 2022 Developer on port **1433** |
| `database-init` | same | One-shot: create DB `HomestayManagement` if missing |

**Volume:** `homestay_sql_data` — persists data across container restarts.

## Prerequisites

- Docker Desktop running
- Port `1433` free (stop local SQL Server service if conflicting)
- `DB_PASSWORD` set in `.env` (≥8 chars, mixed case, number, special char)

## Commands

From repo root:

```powershell
# Start
docker compose --env-file Backend/.env -f Backend/compose.yml up -d

# Status
docker compose --env-file Backend/.env -f Backend/compose.yml ps

# Stop (keep data)
docker compose --env-file Backend/.env -f Backend/compose.yml stop

# Remove containers (keep volume)
docker compose --env-file Backend/.env -f Backend/compose.yml down

# Remove containers AND data (destructive)
docker compose --env-file Backend/.env -f Backend/compose.yml down -v
```

## Healthcheck

Compose waits for SQL Server healthy before `database-init` runs `CREATE DATABASE`.

## What is NOT dockerized (edu)

- Spring Boot API — run via `mvnw spring-boot:run` on host
- React SPA — run via `npm run dev` on host

Containerizing full stack is optional post-edu; current team workflow is **DB-only Docker**.

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Port 1433 in use | Stop Windows SQL Server service or change host port in compose |
| Login failed for sa | Check `DB_PASSWORD` in `.env` matches compose env |
| Backend cannot connect | Verify `DB_URL` points to `localhost:1433` and DB name `HomestayManagement` |

See also: [`environments.md`](environments.md)
