# Testing Guide — Homestay / Resort

## Goals

- Unit + Integration/API + E2E + Manual UAT
- **Coverage:** ≥80% LINE on critical BE services (Auth, Booking, Payment, VNPay, DamageFee) via `mvn -Pcoverage-critical verify` — see `testing/coverage-control.md`
- Soft floor whole bundle ≥15% (`mvn verify`)
- SQL Server test DB without Docker: `HomestayManagement_Test` (optional); default unit tests use H2

## Backend

```bash
cd Backend
# Use JDK 17 (JAVA_HOME)
mvn test
mvn verify                      # soft floor 15%
mvn -Pcoverage-critical verify  # critical set ≥80%
```

Report: `target/site/jacoco/index.html`

Test profile: `Backend/src/test/resources/application-test.yml`

Optional SQL Server:

```bash
set TEST_DB_URL=jdbc:sqlserver://localhost:1433;databaseName=HomestayManagement_Test;encrypt=true;trustServerCertificate=true
set TEST_DB_USER=sa
set TEST_DB_PASSWORD=your_password
set TEST_DB_DRIVER=com.microsoft.sqlserver.jdbc.SQLServerDriver
set TEST_HIBERNATE_DIALECT=org.hibernate.dialect.SQLServerDialect
```

## Frontend

Vitest/coverage scripts are **not yet wired** (TD-012). Until then:

```bash
cd Frontend
npm ci
npm run build
```

## Manual UAT

See `docs/uat-checklist.md`.

## CI

See `.github/workflows/tests.yml` and `deployment/ci-cd.md`.

## Agent prompts

- `prompts/testing.md` — general test authoring
- `prompts/coverage.md` — raise critical coverage to ≥80%
