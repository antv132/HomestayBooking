# Data Model: FR-06 Property Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Property, ManagerPropertyAssignment

## Scope

FR-06 **owns** `properties` and `manager_property_assignments`. Does not own `floors`, `rooms`, `EmployeePropertyAssignment`, or `PricingRule` writes.

## ERD

```text
User (MANAGER) 1──* ManagerPropertyAssignment *──1 Property
User (ADMIN) assigns via assigned_by FK
Property 1──* Floor (FR-07, read for stats)
Property 1──* PricingRule (future, out of scope)
```

## Table: properties

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| name | VARCHAR(255) | NOT NULL | |
| address | VARCHAR(500) | NOT NULL | api-spec `location` maps here |
| description | TEXT | nullable | |
| status | VARCHAR(16) | NOT NULL, DEFAULT 'INACTIVE' | ACTIVE, INACTIVE |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**: `(status)`, GIN/trgm on `(name, address)` for admin search (align FR-03 suggestions).

**Validation**:

- `name` not blank, max 255
- `address` not blank, max 500
- Cannot set `status = ACTIVE` unless active manager assignment exists (service + optional CHECK via trigger deferred)

## Table: manager_property_assignments

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| manager_id | UUID | FK users NOT NULL | Must be role MANAGER |
| property_id | UUID | FK properties NOT NULL | |
| assigned_by | UUID | FK users NOT NULL | Admin who assigned |
| assigned_at | TIMESTAMPTZ | NOT NULL | |
| status | VARCHAR(16) | NOT NULL | ACTIVE, INACTIVE |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- Partial unique: `(property_id) WHERE status = 'ACTIVE'` — one active manager per property
- `(manager_id, status)` — manager property list
- `(property_id, assigned_at DESC)` — history

**Validation on assign**:

- Target manager: `users.role = MANAGER`, `users.status = ACTIVE`
- Swap: deactivate current ACTIVE for property, insert new ACTIVE in same transaction

## State Transitions

### Property.status

```text
CREATE → INACTIVE (default)
INACTIVE → ACTIVE (requires active assignment)
ACTIVE → INACTIVE (allowed anytime; discovery hides)
```

### ManagerPropertyAssignment.status

```text
CREATE → ACTIVE (on assign)
ACTIVE → INACTIVE (on reassign or allowed unassign when property INACTIVE)
```

## API DTOs

### PropertySummaryResponse (admin list SCR-46)

```json
{
  "id": "uuid",
  "name": "Homestay Đà Nẵng",
  "address": "123 Beach Rd, Đà Nẵng",
  "status": "ACTIVE",
  "currentManager": {
    "id": "uuid",
    "fullName": "Nguyen Van A"
  },
  "createdAt": "2026-06-01T00:00:00Z",
  "updatedAt": "2026-06-01T00:00:00Z"
}
```

### CreatePropertyRequest (SCR-47)

```json
{
  "name": "Homestay Đà Nẵng",
  "address": "123 Beach Rd",
  "description": "Optional"
}
```

Default response status: **INACTIVE**.

### UpdatePropertyRequest (SCR-48)

```json
{
  "name": "Updated name",
  "address": "New address",
  "description": "...",
  "status": "ACTIVE"
}
```

### AssignManagerRequest (SCR-49)

```json
{
  "managerId": "uuid"
}
```

### ManagerAssignmentResponse

```json
{
  "id": "uuid",
  "managerId": "uuid",
  "managerName": "Nguyen Van A",
  "propertyId": "uuid",
  "assignedBy": "uuid",
  "assignedAt": "2026-06-27T10:00:00Z",
  "status": "ACTIVE"
}
```

### PropertyDetailResponse (manager read)

```json
{
  "id": "uuid",
  "name": "...",
  "address": "...",
  "description": "...",
  "status": "ACTIVE",
  "createdAt": "...",
  "updatedAt": "...",
  "stats": {
    "totalFloors": 0,
    "totalRooms": 0,
    "availableRooms": 0,
    "pendingDepositRooms": 0,
    "reservedRooms": 0,
    "occupiedRooms": 0,
    "maintenanceRooms": 0
  },
  "floors": []
}
```

`floors[]` populated when FR-07 exists; empty array in v1.

### PropertyPageResponse

Standard pagination wrapper: `{ content[], page, size, totalElements, totalPages }`.

## Flyway

```text
V005__properties.sql
V006__manager_property_assignments.sql
```

## ActivityLog Events

| Action | Event type |
|--------|------------|
| Create property | `PROPERTY_CREATED` |
| Update property | `PROPERTY_UPDATED` |
| Status change | `PROPERTY_STATUS_CHANGED` |
| Assign manager | `MANAGER_ASSIGNED` |
| Deactivate assignment | `MANAGER_ASSIGNMENT_ENDED` |

## Integration Points

| Feature | Integration |
|---------|-------------|
| FR-01 | JWT + ADMIN/MANAGER roles; SCR-50 manager list |
| FR-03 | Filter `properties.status = ACTIVE` in discovery/search |
| FR-04/05 | `@PropertyAccessValidator` uses active assignment |
| FR-07 | `floors.property_id` FK; stats query |
| FR-08 | `rooms.property_id`; room counts in detail stats |
| FR-17 | EmployeePropertyAssignment separate table/API |

## Security Rules

| Endpoint | Role |
|----------|------|
| `/admin/properties/**` | ADMIN |
| `/manager/properties/**` | MANAGER (scoped) |
| Manager GET `{id}` | 403 if no ACTIVE assignment for `{id}` |
