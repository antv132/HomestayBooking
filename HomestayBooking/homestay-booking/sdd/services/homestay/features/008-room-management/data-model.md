# Data Model: FR-08 Room Management

**Date**: 2026-07-23  
**Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

## Scope

FR-08 **owns** `rooms` (incl. soft-delete) and `room_images`. Reads `floors`, `properties`, `bookings` (delete guard). Does **not** own status PATCH (FR-05) or PricingRule.

## ERD

```text
Property 1──* Floor 1──* Room 1──* RoomImage
Property 1──* Room (property_id denormalized)
Room 1──* Booking (FK retained after soft-delete)
```

## Table: rooms

| Column | Type (SQL Server) | Constraints | Notes |
|--------|-------------------|-------------|-------|
| id | UNIQUEIDENTIFIER | PK | |
| property_id | UNIQUEIDENTIFIER | FK properties NOT NULL | |
| floor_id | UNIQUEIDENTIFIER | FK floors NOT NULL | Must match floor.property_id |
| room_number | NVARCHAR(50) | NOT NULL | api `roomNumber` |
| room_type | NVARCHAR(100) | NOT NULL on create | |
| area | DECIMAL(8,2) | nullable | m² |
| price_per_night | DECIMAL(15,2) | NOT NULL | > 0 |
| capacity | INT | NOT NULL | >= 1 |
| status | NVARCHAR(20) | NOT NULL DEFAULT 'AVAILABLE' | 8 enum §5 |
| description | NVARCHAR(MAX) | nullable | |
| amenities | NVARCHAR(MAX) / JSON | nullable | string[] via Hibernate JSON |
| deleted_at | DATETIME2 | nullable | null = active; set on soft-delete |
| created_at | DATETIME2 | NOT NULL | |
| updated_at | DATETIME2 | NOT NULL | |

**Indexes**:

- Unique active room number: prefer filtered unique `(property_id, room_number) WHERE deleted_at IS NULL`; else app-level + legacy unique + reuse policy documented in research §2
- `(property_id, status)`, `(floor_id)`, `(deleted_at)` for list filters

**Validation**:

- `floor_id` same property as `property_id`
- `price_per_night` > 0, `capacity` >= 1, `room_type` not blank
- Unique room number among active rooms in property
- Soft-delete only when no blocking bookings
- Manager/public lists: `deleted_at IS NULL`

### Room.Status (operational)

`AVAILABLE`, `PENDING_DEPOSIT`, `RESERVED`, `OCCUPIED`, `PENDING_CLEANING`, `CLEANING_IN_PROGRESS`, `MAINTENANCE`, `OUT_OF_SERVICE`

### Soft-delete state

```text
[active: deleted_at IS NULL] --DELETE (no blocking bookings)--> [soft-deleted: deleted_at set]
```

No restore in FR-08 v1 (out of scope unless later admin tool).

## Table: room_images

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UNIQUEIDENTIFIER | PK | |
| room_id | UNIQUEIDENTIFIER | FK rooms | Keep on room soft-delete |
| image_url | NVARCHAR(1024) | NOT NULL | |
| is_primary | BIT | NOT NULL DEFAULT 0 | One primary per room (service) |
| sort_order | INT | NOT NULL DEFAULT 0 | |
| created_at | DATETIME2 | NOT NULL | |

**Rules**: Max **10** rows per `room_id`. MIME jpeg/png/webp; ≤10MB/file on upload.

**Indexes**: `(room_id, sort_order)`

## Delete Guard (bookings read)

Block soft-delete when:

```sql
EXISTS (
  SELECT 1 FROM bookings b
  WHERE b.room_id = :roomId
  AND b.status IN (
    'PENDING_DEPOSIT', 'CONFIRMED', 'CHECKED_IN',
    'PENDING_INSPECTION', 'PENDING_DAMAGE_PAYMENT'
  )
)
```

Equivalent: `status NOT IN ('CANCELLED', 'CHECKED_OUT', 'NO_SHOW')` for current canonical enum (same set as inventory overlap).

**Do not use** legacy `COMPLETED` status.

## State / Transitions (Room CRUD)

| Action | Precondition | Effect |
|--------|--------------|--------|
| Create | Assigned property; valid floor; unique room# | status=AVAILABLE; deleted_at=null |
| Update metadata | Active room; floor same property | Update fields; price does not rewrite booking totals |
| Soft-delete | Active; no blocking bookings | deleted_at=now(); hidden from lists |
| Upload image | Active; count &lt; 10; MIME+size OK | New RoomImage; first may be primary |

## API DTOs (summary)

### CreateRoomRequest

`propertyId`, `floorId`, `roomNumber`, `roomType` (required), `pricePerNight`, `capacity`, optional `area`, `description`, `amenities[]` — **no status**.

### UpdateRoomRequest

Partial: `floorId`, `roomNumber`, `roomType`, `pricePerNight`, `capacity`, `area`, `description`, `amenities`.

### RoomSummaryResponse / RoomDetailResponse

Include operational `status`, `primaryImageUrl` / `images[]`. Soft-deleted rooms omitted from list; detail MAY 404 for manager list UX.

### Soft-delete response

`200` + message; `data` null or `{ id, deletedAt }`.

## Schema change

```text
ALTER TABLE rooms ADD deleted_at DATETIME2 NULL;
-- + filtered unique / service uniqueness for active rooms
```

Existing Flyway/schema bootstrap may already create `rooms`/`room_images` — patcher or migration Vxxx`__rooms_deleted_at.sql`.

## ActivityLog Events

| Event | When |
|-------|------|
| ROOM_CREATED | POST |
| ROOM_UPDATED | PUT |
| ROOM_DELETED | soft-delete DELETE |
| ROOM_IMAGE_ADDED / REMOVED / REORDERED / PRIMARY_SET | gallery |

## Integration Points

| Feature | Integration |
|---------|-------------|
| FR-06 | PropertyAccessValidator on writes |
| FR-07 | floor FK; same-property move |
| FR-03 | Public discovery excludes soft-deleted |
| FR-04 | Booking statuses for delete guard; price snapshot |
| FR-05 | Manual status / calendar — out of scope |

## Security matrix

| Endpoint | MANAGER | EMPLOYEE |
|----------|---------|----------|
| GET list/detail (active) | Assigned | Assigned read-only |
| POST/PUT/DELETE room | Assigned | Denied |
| Gallery mutations | Assigned | Denied |
