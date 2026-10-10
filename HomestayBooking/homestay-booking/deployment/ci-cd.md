# CI/CD

Repos are **separate Git remotes**. Each has its own workflow:

| Repo | Workflow |
|------|----------|
| `Backend` → [SWP391-G6-Dorm/Backend](https://github.com/SWP391-G6-Dorm/Backend) | [`Backend/.github/workflows/tests.yml`](../Backend/.github/workflows/tests.yml) |
| `Frontend` → [SWP391-G6-Dorm/Frontend](https://github.com/SWP391-G6-Dorm/Frontend) | [`Frontend/.github/workflows/tests.yml`](../Frontend/.github/workflows/tests.yml) |

Local workspace copy (not a git root): [`.github/workflows/tests.yml`](../.github/workflows/tests.yml) — reference only for monorepo layout.

## Triggers

- Push to `main`, `DEV`, `dev`
- Pull requests

## Jobs

| Repo | Job | Steps |
|------|-----|--------|
| Backend | `backend` | Java 17 → `./mvnw -B -Pcoverage-critical verify` → upload JaCoCo |
| Frontend | `frontend` | Node 20 → `npm ci` → `npm run build` |
| Frontend | `e2e-smoke` | Disabled until Playwright scripts exist |

## Coverage gate

Critical classes: AuthService, BookingService, PaymentService, VNPayService, DamageFeeSettlementService.  
Playbook: [`testing/coverage-control.md`](../testing/coverage-control.md).

```bash
mvn -B -Pcoverage-critical verify
```

## What CI does NOT do (current)

- Deploy to cloud / VPS
- Build Docker image for API
- Run against real SQL Server (unit tests use H2)
- VNPay sandbox integration tests

## Local parity

Match CI before opening PR:

```powershell
cd Backend
.\mvnw.cmd test

cd ../Frontend
npm ci
npm run lint
npm run test:coverage
npx playwright install chromium
npm run test:e2e
```

## Branch conventions

Per `AGENTS.md`:

- `feat/*`, `fix/*`, `spec/*`, `refactor/*`
- Commit: `[type]([scope]): [description]`

## Artifacts

- `jacoco-report` — backend coverage HTML
- `vitest-coverage` — frontend coverage

Download from GitHub Actions run summary.

## Future (optional)

- Add deploy workflow when staging host exists
- SQL Server service container for integration tests
- Required status checks on `main` / `DEV`

## Related

- [`../testing/strategy.md`](../testing/strategy.md)
- [`environments.md`](environments.md)
