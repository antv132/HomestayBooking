# Data Model: FR-21 Housekeeping Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 HousekeepingTask

## Scope

FR-21 **owns** `housekeeping_tasks`. **Updates** `rooms.status` (FR-08) on task lifecycle. **Reads** `bookings`, `users` (employee), `properties`. **Hook** from FR-04 checkout.

## ERD

```text
Booking 1──0..1 HousekeepingTask (optional booking_id for auto-create idempotency)
Property 1──* HousekeepingTask
Room 1──* HousekeepingTask
User (EMPLOYEE) 1──* HousekeepingTask (assigned_employee_id)

HousekeepingTaskService:
  onBookingCheckedOut(bookingId)
  listByProperty(propertyId, status, page)
  createManual(roomId, assigneeId?, managerId)
  assignEmployee(taskId, employeeId, managerId)
  updateStatus(taskId, status, employeeId)  # Employee
  cancel(taskId, note, managerId)
```

## Table: housekeeping_tasks

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| property_id | UUID | FK properties NOT NULL | |
| room_id | UUID | FK rooms NOT NULL | |
| booking_id | UUID | FK bookings nullable | Auto-create source; idempotency |
| assigned_employee_id | UUID | FK users nullable | EMPLOYEE role |
| status | VARCHAR(20) | NOT NULL | PENDING, IN_PROGRESS, COMPLETED, CANCELLED |
| note | TEXT | nullable | Cancel/complete notes |
| created_at | TIMESTAMPTZ | NOT NULL | |
| started_at | TIMESTAMPTZ | nullable | Set on IN_PROGRESS |
| completed_at | TIMESTAMPTZ | nullable | Set on COMPLETED |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `(property_id, status, created_at DESC)` — manager board/list
- `(assigned_employee_id, status)` — employee SCR-60
- `(room_id, status)` — open task lookup for bypass guard
- Partial unique: `(booking_id) WHERE booking_id IS NOT NULL AND status IN ('PENDING','IN_PROGRESS')` — idempotent auto-create

## State Transitions

### HousekeepingTask.status

```text
AUTO-CREATE / MANUAL CREATE → PENDING
PENDING → IN_PROGRESS (Employee start; sets started_at)
IN_PROGRESS → COMPLETED (Employee finish; sets completed_at)
PENDING | IN_PROGRESS → CANCELLED (Manager cancel)
COMPLETED | CANCELLED → terminal
```

### Room.status (FR-08 sync)

```text
onBookingCheckedOut / task PENDING created → PENDING_CLEANING
task IN_PROGRESS → CLEANING_IN_PROGRESS
task COMPLETED → AVAILABLE
task CANCELLED from IN_PROGRESS → PENDING_CLEANING
```

## API DTOs

### HousekeepingTaskResponse

| Field | Type |
|-------|------|
| id | UUID |
| propertyId | UUID |
| propertyName | string |
| roomId | UUID |
| roomNumber | string |
| roomName | string (alias roomNumber) |
| bookingId | UUID? |
| assignedEmployeeId | UUID? |
| assigneeName | string? |
| status | enum |
| note | string? |
| createdAt | datetime |
| startedAt | datetime? |
| completedAt | datetime? |

### CreateHousekeepingTaskRequest

| Field | Required |
|-------|----------|
| roomId | yes |
| assigneeId | no |

### AssignHousekeepingTaskRequest

| Field | Required |
|-------|----------|
| assigneeId | yes |

### UpdateHousekeepingStatusRequest (Employee)

| Field | Required | Values |
|-------|----------|--------|
| status | yes | IN_PROGRESS, COMPLETED |

### CancelHousekeepingTaskRequest (Manager)

| Field | Required |
|-------|----------|
| note | no |

## Error Codes

| Code | HTTP | When |
|------|------|------|
| UNAUTHORIZED_PROPERTY_ACCESS | 403 | Manager wrong property |
| TASK_NOT_ASSIGNED | 403 | Employee not assignee |
| INVALID_STATUS_TRANSITION | 400 | Skip In Progress, etc. |
| EMPLOYEE_WRONG_PROPERTY | 400 | Assignee not at property |
| OPEN_HOUSEKEEPING_EXISTS | 409 | Duplicate auto-create |
| ROOM_AVAILABLE_BYPASS_DENIED | 400 | Manual Available with open task |
| TASK_TERMINAL | 400 | Update completed/cancelled task |

## Migration: V037__housekeeping_tasks_fr21.sql

```sql
CREATE TABLE housekeeping_tasks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id UUID NOT NULL REFERENCES properties(id),
  room_id UUID NOT NULL REFERENCES rooms(id),
  booking_id UUID REFERENCES bookings(id),
  assigned_employee_id UUID REFERENCES users(id),
  status VARCHAR(20) NOT NULL,
  note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hk_property_status ON housekeeping_tasks (property_id, status, created_at DESC);
CREATE INDEX idx_hk_assignee_status ON housekeeping_tasks (assigned_employee_id, status);
CREATE INDEX idx_hk_room_status ON housekeeping_tasks (room_id, status);

CREATE UNIQUE INDEX uq_hk_booking_active
  ON housekeeping_tasks (booking_id)
  WHERE booking_id IS NOT NULL AND status IN ('PENDING', 'IN_PROGRESS');
```

## Cross-FR Dependencies

| FR | Relationship |
|----|--------------|
| FR-04 | Calls `onBookingCheckedOut` after Checked-out |
| FR-08 | Room status read/update; bypass guard |
| FR-20 | Validate assignee property assignment |
| FR-06 | Manager property scope |
| FR-23/FR-12 | Preconditions before checkout (upstream) |
| FR-22 | Consumes task counts (downstream) |
