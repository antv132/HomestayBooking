# Quickstart: FR-08 Room Management

**Feature**: `sdd/services/homestay/features/008-room-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-06 manager/employee property assignment; FR-07 ≥1 floor on seed property.

## Prerequisites

- JDK 17 (`JAVA_HOME`), Maven wrapper in `Backend/`
- Node 18+ / npm in `Frontend/`
- SQL Server reachable per `Backend/src/main/resources/application.yml`
- Schema includes `rooms`, `room_images`; apply `deleted_at` patch when implementing Phase A

## Environment

```bash
# Optional — local gallery storage (default app.upload.*)
# MIME jpeg/png/webp; max 10MB; max 10 images/room
```

Vite proxy: `/api` → backend (typically `http://localhost:8080`).

## Run

```bash
cd Backend && ./mvnw spring-boot:run
# Windows: Backend\CHAY-BACKEND.cmd or IDE HomestayApplication

cd Frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| SCR-29 Room List | `/manager/rooms` | `GET /api/v1/manager/rooms` |
| SCR-30 Add Room | `/manager/rooms/add` | `POST /api/v1/manager/rooms` |
| SCR-31 Edit Room | `/manager/rooms/:id/edit` | `GET/PUT /api/v1/manager/rooms/{id}` |
| SCR-32 Gallery | `/manager/rooms/:id/gallery` | `/api/v1/manager/rooms/{id}/images/*` |
| SCR-33 Status | `/manager/rooms/:id/status` | **FR-05** — not FR-08 |
| Delete room | SCR-29 action | `DELETE /api/v1/manager/rooms/{id}` (soft) |
| SCR-65 Employee | `/employee/rooms` | `GET /api/v1/employee/rooms` (or scoped list) |

## curl smoke tests

Replace tokens and IDs.

```bash
BASE=http://localhost:8080/api/v1
MGR_TOKEN="<manager-jwt>"
EMP_TOKEN="<employee-jwt>"
PROPERTY_ID="<uuid>"
FLOOR_ID="<uuid>"

# SCR-29 — list (soft-deleted hidden)
curl -s "$BASE/manager/rooms?propertyId=$PROPERTY_ID&page=0&size=10" \
  -H "Authorization: Bearer $MGR_TOKEN"

# SCR-30 — create (expect status AVAILABLE)
curl -s -X POST "$BASE/manager/rooms" \
  -H "Authorization: Bearer $MGR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"propertyId\":\"$PROPERTY_ID\",
    \"floorId\":\"$FLOOR_ID\",
    \"roomNumber\":\"101\",
    \"roomType\":\"Deluxe\",
    \"pricePerNight\":500000,
    \"capacity\":2
  }"

ROOM_ID="<created-id>"

# Duplicate room number → 409
curl -s -X POST "$BASE/manager/rooms" \
  -H "Authorization: Bearer $MGR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"propertyId\":\"$PROPERTY_ID\",
    \"floorId\":\"$FLOOR_ID\",
    \"roomNumber\":\"101\",
    \"roomType\":\"Standard\",
    \"pricePerNight\":300000,
    \"capacity\":2
  }"

# Soft-delete (no blocking booking) → 200; room gone from list
curl -s -X DELETE "$BASE/manager/rooms/$ROOM_ID" \
  -H "Authorization: Bearer $MGR_TOKEN"

# Soft-delete with blocking booking → 409
# (create booking PENDING_DEPOSIT/CONFIRMED on room first)

# Gallery multipart — reject non-image or >10MB or 11th image
curl -s -X POST "$BASE/manager/rooms/$ROOM_ID/images" \
  -H "Authorization: Bearer $MGR_TOKEN" \
  -F "files=@./sample.jpg"
```

## Validation checklist

- [ ] New room status **AVAILABLE**
- [ ] Duplicate active room number → **409**
- [ ] Floor from other property → **400**
- [ ] Soft-delete sets `deleted_at`; list/discovery hide room
- [ ] Soft-delete blocked for blocking booking statuses → **409**
- [ ] Soft-delete allowed when only `CHECKED_OUT` / `CANCELLED` / `NO_SHOW` history
- [ ] Gallery: jpeg/png/webp only; ≤10MB; max 10 images
- [ ] Exactly one primary after set-primary
- [ ] Employee write → **403**
- [ ] Occupied room: floor change same property allowed; other property denied

## Troubleshooting

| Issue | Fix |
|-------|-----|
| 403 manager | FR-06 assignment |
| Cannot create | Floor exists under property (FR-07) |
| Upload rejected | MIME/size/count; check UploadLimits 10MB |
| Still see deleted room | Filter `deleted_at IS NULL` on list queries |
| Delete blocked wrongly | Fix guard — no `COMPLETED`; allow `CHECKED_OUT` |
| Discovery empty | Property ACTIVE + room AVAILABLE + not soft-deleted |
