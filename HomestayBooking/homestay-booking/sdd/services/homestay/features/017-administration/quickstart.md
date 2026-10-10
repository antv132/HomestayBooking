# Quickstart: FR-17 Administration

**Feature**: `specs/017-administration` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 JWT (Customer, Admin); FR-09 optional (customer nav link); FR-14 for Content Moderation tab; V033 migration applied.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: 1 Customer user, 1 Admin user; default system_settings from V033

## Environment

```bash
# No feature-specific env vars — settings stored in DB
```

Vite proxy: `/api/v1` → `http://localhost:8080`

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| Customer My Complaints | `/customer/complaints` | `GET /complaints` |
| Customer Create Complaint | `/customer/complaints/create` | `POST /complaints` |
| SCR-54 Complaint Management | `/admin/complaints` | `GET /admin/complaints` |
| SCR-54 Complaint Detail | `/admin/complaints/:id` | `GET /admin/complaints/{id}`, `PATCH .../status` |
| SCR-56 Activity Logs | `/admin/system` (tab) | `GET /admin/activity-logs` |
| SCR-56 System Settings | `/admin/system` (tab) | `GET/PUT /admin/settings` |
| SCR-56 Content Moderation | `/admin/system` (tab) | FR-14 `GET/PATCH /admin/reviews/**` |
| FR-09 Customer Directory | `/admin/customers` | FR-09 `GET /admin/users?role=CUSTOMER` |

## Smoke test: Customer create complaint

```bash
BASE=http://localhost:8080/api/v1
CUSTOMER_TOKEN="<customer-jwt>"

curl -s -X POST "$BASE/complaints" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"subject":"Room not clean","description":"The bathroom was not cleaned before check-in as promised."}' | jq

# Expected: success true, data.status OPEN
```

## Smoke test: Admin list and resolve workflow

```bash
ADMIN_TOKEN="<admin-jwt>"
COMPLAINT_ID="<uuid-from-create>"

# List all complaints
curl -s "$BASE/admin/complaints?page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# OPEN → INVESTIGATING
curl -s -X PATCH "$BASE/admin/complaints/$COMPLAINT_ID/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"INVESTIGATING"}' | jq

# INVESTIGATING → RESOLVED (notes required)
curl -s -X PATCH "$BASE/admin/complaints/$COMPLAINT_ID/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"RESOLVED","resolutionNotes":"Issued partial refund of 10%."}' | jq

# RESOLVED → CLOSED
curl -s -X PATCH "$BASE/admin/complaints/$COMPLAINT_ID/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"CLOSED"}' | jq
```

## Smoke test: invalid transition

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X PATCH "$BASE/admin/complaints/$COMPLAINT_ID/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"OPEN"}'
# Expected: 400 (CLOSED is terminal)
```

## Smoke test: System Settings

```bash
# Get current settings
curl -s "$BASE/admin/settings" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Update deposit percentage
curl -s -X PUT "$BASE/admin/settings" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"depositPercentage":35,"supportEmail":"help@homestay.vn"}' | jq

# Verify persistence
curl -s "$BASE/admin/settings" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.data.depositPercentage'
# Expected: 35
```

## Smoke test: Activity Logs

```bash
curl -s "$BASE/admin/activity-logs?from=2026-01-01&to=2026-12-31&page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Expected: COMPLAINT_CREATED, COMPLAINT_STATUS_CHANGED, SETTINGS_UPDATED entries
```

## Smoke test: RBAC denial

```bash
curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/admin/settings" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
# Expected: 403

curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/admin/complaints" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
# Expected: 403
```

## Smoke test: Customer scope

```bash
OTHER_COMPLAINT_ID="<another-customer-complaint-uuid>"
curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/complaints/$OTHER_COMPLAINT_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
# Expected: 403
```

## Manual UI checklist

- [ ] Customer submits complaint → appears in My Complaints as OPEN
- [ ] Admin SCR-54 list shows complaint with customer name
- [ ] Admin drawer/detail shows full workflow buttons
- [ ] SCR-56 tabs switch without reload
- [ ] System Settings save persists after page refresh
- [ ] Activity Logs show recent complaint/settings actions
- [ ] Content Moderation tab loads reviews (requires FR-14)
- [ ] Manager/Customer cannot access `/admin/complaints` or `/admin/system`

## Troubleshooting

| Issue | Check |
|-------|-------|
| 403 on admin endpoints | JWT role must be ADMIN |
| 400 on RESOLVED | `resolutionNotes` min 10 chars |
| Empty activity logs | Ensure V033 ran; actions logged on create/status/settings |
| Content Moderation empty | FR-14 backend not implemented — expected blocker |
| Wrong complaint routes | Frontend should use `/admin/complaints`, not `/manager/complaints` |
