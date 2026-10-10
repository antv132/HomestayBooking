# Quickstart: FR-02 User Profile

**Feature**: `specs/002-user-profile` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 auth running — user registered, verified, logged in with JWT.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Valid access token from FR-01 login

## Environment

Same as [FR-01 quickstart](../001-user-auth/quickstart.md). No additional env vars.

## Run

```bash
# Backend (after FR-01 scaffold + UserController)
cd backend && ./mvnw spring-boot:run

# Frontend
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route (by role) | API |
|--------|-----------------|-----|
| SCR-10 User Profile | `/customer/profile`, `/manager/profile`, `/employee/profile`, `/admin/profile` | GET `/api/v1/users/me` |
| SCR-11 Edit Profile | `.../profile/edit` | PUT `/api/v1/users/me` |

## curl smoke tests

```bash
# Set token from FR-01 login
TOKEN="<accessToken>"

# GET profile (SCR-10)
curl -s http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" | jq

# PUT profile (SCR-11)
curl -s -X PUT http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Nguyen Van A","phone":"0901234567"}' | jq

# Validation error — invalid phone
curl -s -X PUT http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Test","phone":"123"}' | jq

# Unauthenticated
curl -s http://localhost:8080/api/v1/users/me | jq
```

## Frontend verification

1. Login as Customer → `/customer/profile` — shows fullName, email, phone, role badge.
2. Edit → change fullName → Save → header shows new name (authStore sync).
3. Email field disabled on edit form.
4. Repeat with Manager at `/manager/profile`.

## Tests

```bash
# Backend
cd backend && ./mvnw test -Dtest=UserProfileServiceTest,UserControllerIT

# Frontend
cd frontend && npm run test -- ProfilePages
```

## Out of scope (this feature)

- SCR-12 Change Password → FR-01 `PUT /api/v1/auth/change-password`
- Admin view other customers → FR-09
- Avatar file upload → future enhancement (MVP: URL only)
