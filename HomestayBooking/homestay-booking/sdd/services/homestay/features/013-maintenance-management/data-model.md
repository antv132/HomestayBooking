# Data Model: FR-13 Maintenance Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 MaintenanceTicket, Attachment

## Scope

FR-13 **owns** `maintenance_tickets` and **MAINTENANCE** `attachments`. **Consumes** FR-04 `bookings` (active validation), FR-08 `rooms` (property scope), FR-06 employee property assignments. **Emits** notification/outbox events for FR-15 on status transitions (Assigned, In Progress, Resolved, Closed).

## ERD

```text
Customer 1──* MaintenanceTicket *──1 Booking
Room 1──* MaintenanceTicket
Property 1──* Room (scope path)
Employee 0..1──* MaintenanceTicket (assignedEmployeeId)
MaintenanceTicket 1──* Attachment (entity_type = MAINTENANCE)

Manager assign ──> OPEN → ASSIGNED
Employee work  ──> ASSIGNED → IN_PROGRESS → RESOLVED
Manager close  ──> RESOLVED → CLOSED
```

## Table: maintenance_tickets

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| customer_id | UUID | FK users NOT NULL | Request owner |
| booking_id | UUID | FK bookings NOT NULL | Active booking only at create |
| room_id | UUID | FK rooms NOT NULL | Denormalized from booking |
| property_id | UUID | FK properties NOT NULL | Denormalized for Manager scope index |
| title | VARCHAR(200) | NOT NULL | |
| description | TEXT | NOT NULL | min 20 chars app validation |
| status | VARCHAR(20) | NOT NULL DEFAULT 'OPEN' | enum below |
| assigned_employee_id | UUID | FK users nullable | Employee assignee |
| assigned_at | TIMESTAMPTZ | nullable | |
| work_note | TEXT | nullable | Employee internal note |
| resolution_note | TEXT | nullable | Manager close note |
| verified_by | UUID | FK users nullable | Manager who closed |
| verified_at | TIMESTAMPTZ | nullable | |
| deleted_at | TIMESTAMPTZ | nullable | Soft delete |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `(customer_id, created_at DESC)` — SCR-22
- `(property_id, status, created_at DESC)` — SCR-41 Manager list
- `(assigned_employee_id, status)` — SCR-61 Employee workspace
- `(booking_id)` — lookup
- `(status)` WHERE deleted_at IS NULL — filter

### MaintenanceTicketStatus enum

`OPEN` | `ASSIGNED` | `IN_PROGRESS` | `RESOLVED` | `CLOSED`

DB stores UPPER_SNAKE; API may expose same or map from spec labels (Open → OPEN).

### Status transitions

```text
(create)                    → OPEN
Manager assign              → ASSIGNED (+ assignedEmployeeId, assignedAt)
Employee start              → IN_PROGRESS
Employee complete           → RESOLVED
Manager close + resolution  → CLOSED (+ verifiedBy, verifiedAt, resolutionNote)

Customer edit/delete        → only OPEN, not deleted
Any change after CLOSED     → rejected
```

## Table: attachments (shared)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| entity_type | VARCHAR(32) | NOT NULL | MAINTENANCE, DAMAGE_ITEM, ... |
| entity_id | UUID | NOT NULL | maintenance_tickets.id |
| file_url | VARCHAR(512) | NOT NULL | |
| file_name | VARCHAR(255) | NOT NULL | |
| file_size | BIGINT | NOT NULL | max 5MB |
| uploaded_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `(entity_type, entity_id)` — list images for ticket

FR-13 writes only `entity_type = 'MAINTENANCE'`.

## Validation Rules (application layer)

| Rule | Error code (suggested) |
|------|------------------------|
| Booking not CONFIRMED/CHECKED_IN | `MAINTENANCE_BOOKING_INACTIVE` |
| Booking not owned by customer | `UNAUTHORIZED` |
| Edit/delete status ≠ OPEN | `MAINTENANCE_NOT_EDITABLE` |
| Assignee not same property | `UNAUTHORIZED_PROPERTY_ACCESS` |
| Invalid status transition | `MAINTENANCE_INVALID_TRANSITION` |
| Close without resolutionNote | `VALIDATION_ERROR` |
| > 5 attachments or > 5MB | `FILE_UPLOAD_FAILURE` |
| Employee not assignee | `UNAUTHORIZED` |

## ActivityLog actions

`TICKET_CREATED`, `TICKET_UPDATED`, `TICKET_DELETED`, `TICKET_ASSIGNED`, `TICKET_STATUS_CHANGED`, `TICKET_CLOSED`

## Outbox / Notification payload (FR-15 stub)

```json
{
  "eventType": "MAINTENANCE_STATUS_CHANGED",
  "userId": "<customerId>",
  "relatedEntityId": "<ticketId>",
  "relatedEntityType": "MaintenanceTicket",
  "title": "Maintenance Update",
  "content": "Your request \"{title}\" is now {status}."
}
```

Emit on: ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED (not OPEN create — optional TICKET_CREATED P2).

## Flyway

```text
V029__maintenance_tickets_fr13.sql
  - CREATE maintenance_tickets
  - CREATE attachments (if not exists from FR-08/23)
  - indexes above
  - optional seed: 1 OPEN ticket for demo
```

If `attachments` already exists from another FR, V029 adds only missing columns/indexes.

## DTO Mapping Notes

| Response field | Source |
|----------------|--------|
| customerName | JOIN users |
| roomNumber | JOIN rooms |
| propertyName | JOIN properties |
| photoUrls | attachments WHERE entity_type=MAINTENANCE |
| assignedEmployeeName | JOIN users on assigned_employee_id |

## RBAC Matrix (implementation)

| Action | Customer | Employee | Manager | Admin |
|--------|----------|----------|---------|-------|
| Create | ✓ own booking | — | — | — |
| List | ✓ own | ✓ assigned | ✓ property | ✓ all read |
| Detail | ✓ own | ✓ assigned | ✓ property | ✓ read |
| Update content | ✓ Open only | — | — | — |
| Delete | ✓ Open only | — | — | — |
| Assign | — | — | ✓ | — |
| Update status | — | ✓ IN_PROGRESS/RESOLVED | — | — |
| Close | — | — | ✓ RESOLVED→CLOSED | — |

## Integration Points

| FR | Integration |
|----|-------------|
| FR-04 | `BookingRepository.findByIdAndCustomerId`; status IN (CONFIRMED, CHECKED_IN) |
| FR-06 | `PropertyScopeService.getManagerPropertyIds()`; employee property assignment check |
| FR-08 | Room exists; property_id denorm on create |
| FR-15 | `MaintenanceNotificationPublisher` → Outbox/event |
| FR-08 Maintenance lock | **No** automatic room status change from ticket |
