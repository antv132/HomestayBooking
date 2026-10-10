# Quickstart: FR-07 Structure Management

**Feature**: `specs/007-structure-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-06 running — at least one property with Manager assignment; optional FR-08 rooms for populated tree leaves.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Flyway V008 applied (after FR-06 V005–V007)

## Environment

Same as FR-01/06 quickstart. Vite proxy: `/api/v1` → `http://localhost:8080`

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| SCR-28 Structure Tree (Manager) | `/manager/structure` | `GET /properties/{id}/tree` |
| Add/Edit Floor modal | (on SCR-28) | `POST/PUT /manager/floors` |
| Delete Floor | (on SCR-28) | `DELETE /manager/floors/{id}` |
| Floor list (Room filter helper) | `/manager/rooms` uses floors | `GET /manager/floors?propertyId=` |
| Admin read-only tree | `/admin/structure` (planned) | `GET /admin/properties/{id}/tree` |
| Legacy compat | StructureTreePage migration | `GET /properties/{id}/structure` |

## curl smoke tests

Replace tokens and IDs.

```bash
BASE=http://localhost:8080/api/v1
MGR_TOKEN="<manager-jwt>"
ADMIN_TOKEN="<admin-jwt>"
PROPERTY_ID="<assigned-property-uuid>"

# SCR-28 — structure tree
curl -s "$BASE/properties/$PROPERTY_ID/tree" \
  -H "Authorization: Bearer $MGR_TOKEN" | jq

# Legacy structure alias
curl -s "$BASE/properties/$PROPERTY_ID/structure" \
  -H "Authorization: Bearer $MGR_TOKEN" | jq

# Create floor
curl -s -X POST "$BASE/manager/floors" \
  -H "Authorization: Bearer $MGR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"propertyId\":\"$PROPERTY_ID\",\"floorNumber\":1,\"description\":\"Ground floor\"}" | jq

FLOOR_ID=$(curl -s -X POST "$BASE/manager/floors" \
  -H "Authorization: Bearer $MGR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"propertyId\":\"$PROPERTY_ID\",\"floorNumber\":2,\"description\":\"Second floor\"}" | jq -r '.data.id')

# Duplicate floor number (expect 409)
curl -s -X POST "$BASE/manager/floors" \
  -H "Authorization: Bearer $MGR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"propertyId\":\"$PROPERTY_ID\",\"floorNumber\":1,\"description\":\"Duplicate\"}" | jq

# List floors
curl -s "$BASE/manager/floors?propertyId=$PROPERTY_ID" \
  -H "Authorization: Bearer $MGR_TOKEN" | jq

# Update floor
curl -s -X PUT "$BASE/manager/floors/$FLOOR_ID" \
  -H "Authorization: Bearer $MGR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"description":"Updated description"}' | jq

# Delete empty floor (expect 200)
curl -s -X DELETE "$BASE/manager/floors/$FLOOR_ID" \
  -H "Authorization: Bearer $MGR_TOKEN" | jq

# Delete floor with rooms (expect 409 when FR-08 rooms exist)
# curl -s -X DELETE "$BASE/manager/floors/$FLOOR_WITH_ROOMS" ...

# Unassigned property (expect 403)
curl -s "$BASE/properties/<other-property-uuid>/tree" \
  -H "Authorization: Bearer $MGR_TOKEN" | jq

# Admin read-only tree
curl -s "$BASE/admin/properties/$PROPERTY_ID/tree" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Admin cannot create floor (expect 403)
curl -s -X POST "$BASE/manager/floors" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"propertyId\":\"$PROPERTY_ID\",\"floorNumber\":99}" | jq
```

## Validation checklist

- [ ] Tree returns floors sorted by `floorNumber` ascending
- [ ] Room nodes appear under floors when FR-08 rooms exist (read-only)
- [ ] Duplicate floor number in same property returns 409
- [ ] Delete floor with rooms returns 409
- [ ] Manager 403 on unassigned property tree
- [ ] Admin can GET tree on any property
- [ ] Admin POST/PUT/DELETE floor returns 403
- [ ] UI SCR-28 Add/Edit Floor modal refreshes tree after save

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Empty tree, no floors | Create floors via POST; verify propertyId |
| 403 on tree | Manager not assigned — run FR-06 assign flow |
| Rooms always empty | FR-08 rooms table/seed not applied yet — expected |
| Frontend 404 | Migrate `floorApi.ts` from `/api/floors` to `/api/v1/manager/floors` |
| V008 migration fails | Ensure V005 properties exists first |
