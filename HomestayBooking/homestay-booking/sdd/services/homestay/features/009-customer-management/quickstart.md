# Quickstart: FR-09 Customer Management

**Feature**: `sdd/services/homestay/features/009-customer-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 users + auth + refresh tokens; FR-04 bookings; FR-12 payments (for Total Spend).

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server
- Admin JWT + ≥1 CUSTOMER (ACTIVE) with optional PAID payments / bookings

## Environment

Vite proxy: `/api` → `http://localhost:8080` (as-is uses `/api/admin/users`)

## Run

```bash
cd Backend && ./mvnw spring-boot:run
cd Frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API (as-is / target) |
|--------|-------|----------------------|
| SCR-51 Customer Directory | `/admin/customers` | `GET /api/admin/users?role=CUSTOMER` |
| Drawer profile | (row click) | `GET /api/admin/users/{id}` |
| Drawer bookings | (row click) | `GET /api/admin/users/{id}/bookings` |
| Suspend / Activate | (confirm → action) | `PATCH /api/v1/admin/users/{id}/status` or hardened PUT |

## curl smoke tests

Replace tokens and IDs. Prefer `/api/v1` once migrated; otherwise use `/api`.

```bash
BASE=http://localhost:8080/api
ADMIN_TOKEN="<admin-jwt>"
CUSTOMER_ID="<customer-uuid>"

# List — default all statuses
curl -s "$BASE/admin/users?role=CUSTOMER&page=0&size=10" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Filter Suspended
curl -s "$BASE/admin/users?role=CUSTOMER&status=SUSPENDED" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Detail + bookings
curl -s "$BASE/admin/users/$CUSTOMER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
curl -s "$BASE/admin/users/$CUSTOMER_ID/bookings?page=0&size=10" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Suspend (after implement: ACTIVE→SUSPENDED + revoke refresh)
curl -s -X PUT "$BASE/admin/users/$CUSTOMER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"SUSPENDED"}'

# Refresh must fail after suspend (use customer's refresh token)
curl -s -X POST "$BASE/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<customer-refresh>"}'

# Login must fail
curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"Password1"}'

# Activate
curl -s -X PUT "$BASE/admin/users/$CUSTOMER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"ACTIVE"}'

# INACTIVE → expect 409
curl -s -X PUT "$BASE/admin/users/<inactive-uuid>" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"SUSPENDED"}'

# Manager denied
curl -s "$BASE/admin/users?role=CUSTOMER" \
  -H "Authorization: Bearer <manager-jwt>"
```

## Validation checklist

- [ ] List returns only `role=CUSTOMER`; default includes INACTIVE/ACTIVE/SUSPENDED
- [ ] `totalBookings` + `totalSpend` (Payment PAID) on rows; Total Spend visible in UI
- [ ] Search matches fullName or email; filter SUSPENDED works
- [ ] Suspend → login blocked; refresh token revoked (SC-002)
- [ ] Activate restores login
- [ ] INACTIVE status change → **409**; FE actions disabled
- [ ] Non-admin → **403**
- [ ] Drawer loads booking history for selected customer only
- [ ] ActivityLog rows `USER_SUSPENDED` / `USER_ACTIVATED`
- [ ] Confirm dialog before Suspend/Activate

## Troubleshooting

| Issue | Fix |
|-------|-----|
| 403 on admin calls | Use ADMIN JWT |
| Toggle still sets INACTIVE | Implement FR-09 status rules + FE map to SUSPENDED |
| Refresh still works after Suspend | Call `revokeAllByUser` in status update |
| Aggregates zero | No PAID payments / bookings — expected for new users |
| Manager list requested | Out of FR-09 — separate feature if ever needed |
