# Quickstart: FR-01 Authentication

**Feature**: `specs/001-user-auth` | **Plan**: [plan.md](./plan.md)

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Mailtrap (dev SMTP)
- Google OAuth Web Client ID

## Environment

```bash
# Backend/.env or application.yml
JWT_SECRET=<256-bit-secret>
JWT_ACCESS_TTL_MINUTES=15
JWT_REFRESH_TTL_DAYS=7
GOOGLE_CLIENT_ID=<client-id>
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=HomestayManagement;encrypt=true;trustServerCertificate=true
SPRING_MAIL_HOST=sandbox.smtp.mailtrap.io
FRONTEND_BASE_URL=http://localhost:5173

# Frontend/.env
VITE_GOOGLE_CLIENT_ID=<client-id>
VITE_API_URL=
```

## Run

```bash
# Backend (when scaffolded)
cd backend && ./mvnw spring-boot:run

# Frontend
cd frontend && npm install && npm run dev
```

Vite proxy (`vite.config.ts`):

```typescript
server: {
  proxy: { '/api/v1': { target: 'http://localhost:8080', changeOrigin: true } }
}
```

## Screen → Route → API (docs/screen.md + api-spec)

| Screen | Route | API |
|--------|-------|-----|
| SCR-02 Login | `/login` | POST `/api/v1/auth/login`, `/auth/google` |
| SCR-03 Register | `/register` | POST `/api/v1/auth/register` |
| SCR-04 OTP | `/verify-otp` | POST `/api/v1/auth/verify-otp`, `/auth/google/complete-link` |
| SCR-05 Forgot | `/forgot-password` | POST `/api/v1/auth/forgot-password` |
| SCR-06 Reset | `/reset-password?token=` | POST `/api/v1/auth/reset-password` |
| SCR-12 Change pwd | `/account/change-password` | PUT `/api/v1/auth/change-password` |

## curl smoke tests

```bash
# Register
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@test.com","password":"Pass1234","fullName":"Dev","phone":"0900000000"}'

# Verify OTP (use code from email/logs)
curl -s -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@test.com","otp":"123456"}'

# Login
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@test.com","password":"Pass1234"}'
```

## Tests

```bash
./mvnw test -Dtest="*Auth*"
cd frontend && npm run test
```

## Docs cross-reference

| Topic | Document |
|-------|----------|
| Business spec | `specs/001-user-auth/spec.md` |
| SRS FR-01 | `docs/Specification_v2.md` §3 |
| Screen API | `docs/api-spec-by-screen.md` SCR-02–06 |
| OpenAPI | `specs/001-user-auth/contracts/auth-api.yaml` |

## Next

```
/speckit-tasks
```
