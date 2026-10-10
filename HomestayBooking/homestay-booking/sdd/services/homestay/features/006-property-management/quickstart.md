# Quickstart: FR-06 Property Management

**Feature**: `specs/006-property-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 auth running; at least one ADMIN and two MANAGER users (ACTIVE).

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Flyway V005–V006 applied before FR-03 seed V010

## Environment

Same as FR-01 quickstart. Vite proxy: `/api/v1` → `http://localhost:8080`

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| SCR-46 Admin list | `/admin/properties` | `GET /admin/properties` |
| SCR-47 Create | `/admin/properties/new` | `POST /admin/properties` |
| SCR-48 Edit | `/admin/properties/:id/edit` | `PUT /admin/properties/{id}` |
| SCR-49 Assign Manager | `/admin/properties/:id/manager` | `PATCH /admin/properties/{id}/manager` |
| SCR-50 Manager picker | (embedded in SCR-49) | `GET /admin/users?role=MANAGER&status=ACTIVE` |
| Manager list | `/manager/properties` | `GET /manager/properties` |
| Manager detail | `/manager/properties/:id` | `GET /manager/properties/{id}/detail` |

## curl smoke tests

Replace tokens and IDs.

```bash
BASE=http://localhost:8080/api/v1
ADMIN_TOKEN="<admin-jwt>"
MGR1_TOKEN="<manager1-jwt>"
MGR2_ID="<manager2-uuid>"

# SCR-46 — list all properties
curl -s "$BASE/admin/properties?page=0&size=10" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# SCR-47 — create property (defaults INACTIVE)
curl -s -X POST "$BASE/admin/properties" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Homestay Test","address":"123 Beach Rd, Đà Nẵng","description":"Demo"}' | jq

PROPERTY_ID=$(curl -s -X POST "$BASE/admin/properties" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Assign Test","address":"456 Mountain Rd"}' | jq -r '.data.id')

# Try activate without manager (expect 409)
curl -s -X PUT "$BASE/admin/properties/$PROPERTY_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"ACTIVE"}' | jq

# SCR-49 — assign manager
curl -s -X PATCH "$BASE/admin/properties/$PROPERTY_ID/manager" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"managerId\":\"$MGR2_ID\"}" | jq

# Now activate (expect 200)
curl -s -X PUT "$BASE/admin/properties/$PROPERTY_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"ACTIVE"}' | jq

# Manager scoped list (only assigned)
curl -s "$BASE/manager/properties" \
  -H "Authorization: Bearer $MGR1_TOKEN" | jq

# Manager detail — assigned property
curl -s "$BASE/manager/properties/$PROPERTY_ID/detail" \
  -H "Authorization: Bearer $(curl -s ... # manager2 token)" | jq

# Manager detail — unassigned (expect 403)
curl -s "$BASE/manager/properties/$PROPERTY_ID" \
  -H "Authorization: Bearer $MGR1_TOKEN" | jq

# Reassign manager — history preserved
curl -s -X PATCH "$BASE/admin/properties/$PROPERTY_ID/manager" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"managerId":"<other-manager-uuid>"}' | jq

curl -s "$BASE/admin/properties/$PROPERTY_ID/assignments?page=0&size=10" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Deactivate property (FR-03 discovery should hide)
curl -s -X PUT "$BASE/admin/properties/$PROPERTY_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"INACTIVE"}' | jq

curl -s "$BASE/rooms?propertyId=$PROPERTY_ID" | jq '.data.content | length'
# Expect 0 rooms or empty when property INACTIVE (FR-03 filter)
```

## Validation checklist

- [ ] New property created with status **INACTIVE**
- [ ] Cannot set **ACTIVE** without manager assignment (409)
- [ ] Assign manager creates ACTIVE assignment; manager sees property in list
- [ ] Reassign deactivates old assignment; history shows INACTIVE row
- [ ] Only one ACTIVE assignment per property (DB unique index)
- [ ] Manager cannot access unassigned property (403)
- [ ] Customer/Guest discovery excludes INACTIVE property (FR-03 integration)
- [ ] Admin list shows `currentManager.fullName` column
- [ ] Manager UI has no create/edit/delete property actions

## Troubleshooting

| Issue | Fix |
|-------|-----|
| 403 on admin endpoints | Login as ADMIN role |
| Manager sees all properties | Wire `GET /manager/properties` not legacy `/api/properties` |
| Activate still fails after assign | Verify assignment status ACTIVE and manager user ACTIVE |
| Duplicate active assignment | Check partial unique index on `manager_property_assignments` |
| FR-03 seed fails | Apply V005–V006 before V010 |
