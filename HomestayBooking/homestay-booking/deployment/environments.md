# Environments

**Full setup guide:** [`docs/environment-setup.md`](../docs/environment-setup.md)

## Standard versions

| Component | Version |
|-----------|---------|
| Java | 17 |
| Maven | 3.9.9 (wrapper) |
| Node.js | 20.x |
| npm | 10+ |
| SQL Server | 2022 Developer (Docker) |

## Workspaces (canonical)

| Role | Path | URL (local) |
|------|------|-------------|
| Backend | `Backend/` | http://localhost:8080 |
| Frontend | `Frontend/` | http://localhost:5173 (Vite default) |
| Swagger | BE | http://localhost:8080/swagger-ui.html |

Do **not** use root `backend/` or `frontend/` folders.

## Environment matrix

| Env | Purpose | Database | Secrets |
|-----|---------|----------|---------|
| **Local dev** | Daily development | Docker SQL Server → `HomestayManagement` | `Backend/.env` (gitignored) |
| **Local test (unit)** | `mvn test` | H2 in-memory | `application-test.yml` |
| **Local test (optional)** | Integration | SQL Server `HomestayManagement_Test` | Env vars `TEST_DB_*` |
| **CI** | GitHub Actions | H2 (BE tests) | No real secrets in workflow |
| **Staging / Prod** | Not in repo scope | SQL Server managed | Host/env injection only — never commit |

## First-time local setup (Windows)

```powershell
# 1. Env file
Copy-Item Backend/dev-environment.template Backend/.env
# Edit .env — DB_PASSWORD must meet SQL Server policy

# 2. Database
docker compose --env-file Backend/.env -f Backend/compose.yml up -d

# 3. Backend
cd Backend
.\mvnw.cmd spring-boot:run

# 4. Frontend (new terminal)
cd Frontend
npm ci
npm run dev
```

## Environment variables (backend)

Template: `Backend/dev-environment.template`

Typical keys (never commit values):

- `DB_URL`, `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_REFRESH_SECRET`
- `VNPAY_*` (sandbox)
- `BOOKING_HOLD_TIMEOUT_MINUTES` (default 10)

## Pre-push verification

```powershell
cd Backend; .\mvnw.cmd test
cd ../Frontend; npm ci; npm run lint; npm run test:run; npm run build
```

## Related

- [`docker.md`](docker.md) — Compose details
- [`ci-cd.md`](ci-cd.md) — GitHub Actions
