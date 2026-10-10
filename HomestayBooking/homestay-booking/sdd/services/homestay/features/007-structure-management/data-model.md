# Data Model: FR-07 Structure Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Floor, Room (read-only in tree)

## Scope

FR-07 **owns** `floors` table and Floor CRUD. **Reads** `properties`, `manager_property_assignments` (FR-06), `rooms` (FR-08 write owner). Does not create/update/delete rooms.

## ERD

```text
Property 1──* Floor 1──* Room (read join for tree)
ManagerPropertyAssignment scopes Manager access to Property
```

## Table: floors

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| property_id | UUID | FK properties NOT NULL | |
| floor_number | INTEGER | NOT NULL, > 0 | Display label "Floor N" |
| description | TEXT | nullable | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `UNIQUE (property_id, floor_number)` — FR-006
- `(property_id, floor_number)` — tree sort

**Validation**:

- `floor_number` integer ≥ 1
- Unique per property on insert/update
- Delete only if no child rooms (`rooms.floor_id`)

## Room (read-only in FR-07 tree)

Uses FR-08 `rooms` table when present:

| Column used in tree | Display |
|---------------------|---------|
| id, room_number, room_type, status | Tree leaf node |

No writes from FR-07 services.

## API DTOs

### CreateFloorRequest

```json
{
  "propertyId": "uuid",
  "floorNumber": 1,
  "description": "Ground floor lobby"
}
```

### UpdateFloorRequest

```json
{
  "floorNumber": 2,
  "description": "Updated"
}
```

### FloorResponse

```json
{
  "id": "uuid",
  "propertyId": "uuid",
  "floorNumber": 1,
  "description": "...",
  "roomCount": 0,
  "createdAt": "...",
  "updatedAt": "..."
}
```

### RoomTreeNode

```json
{
  "id": "uuid",
  "roomNumber": "101",
  "roomType": "STANDARD",
  "status": "AVAILABLE"
}
```

### FloorTreeNode

```json
{
  "id": "uuid",
  "floorNumber": 1,
  "description": "...",
  "roomCount": 2,
  "rooms": []
}
```

### StructureTreeResponse

```json
{
  "propertyId": "uuid",
  "propertyName": "Homestay DN",
  "floors": []
}
```

## Flyway

```text
V008__floors.sql
```

Placed after FR-06 `V005`–`V007`, before FR-03 seed if floors needed in discovery seed (optional).

## ActivityLog

| Event | When |
|-------|------|
| FLOOR_CREATED | POST floor |
| FLOOR_UPDATED | PUT floor |
| FLOOR_DELETED | DELETE floor (empty) |

## Integration Points

| Feature | Integration |
|---------|-------------|
| FR-06 | PropertyAccessValidator; manager property selector |
| FR-08 | `rooms.floor_id` FK; room nodes in tree; delete blocked if rooms exist |
| FR-05 | Room status enum on tree badges (8 values) |
| FR-03 | No direct change; discovery uses rooms under ACTIVE property |

## Security

| Operation | MANAGER | ADMIN |
|-----------|---------|-------|
| GET tree | Assigned property only | All properties |
| Floor CRUD | Assigned property only | **Denied** (403) |
| GET floors list | Assigned | Optional read-all for admin audit |

## Query: Structure Tree

```sql
-- Conceptual: floors ordered by floor_number, rooms by room_number
SELECT f.*, r.*
FROM floors f
LEFT JOIN rooms r ON r.floor_id = f.id
WHERE f.property_id = :propertyId
ORDER BY f.floor_number, r.room_number;
```

Implement via JPA `@Query` or projection DTO in `StructureTreeService`.
