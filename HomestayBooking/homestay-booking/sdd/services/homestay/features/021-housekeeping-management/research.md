# Research: FR-21 Housekeeping Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: entity-ui-mapping §2.2, api-spec SCR-40/SCR-60

## 1. Table Ownership

**Decision**: FR-21 **owns** `housekeeping_tasks` table (V037).

**Rationale**: Spec §5 HousekeepingTask entity; FR-20 owns employee assignments only.

**Alternatives considered**: Embed in bookings table — rejected (task lifecycle independent).

## 2. Auto-Create Trigger

**Decision**: `HousekeepingTaskService.onBookingCheckedOut(UUID bookingId)` invoked from FR-04 checkout completion (after FR-23/FR-12 preconditions satisfied upstream).

**Rationale**: Spec assumption; FR-21 owns task + room transition logic.

**Alternatives considered**: Database trigger — rejected (business logic in service layer).

## 3. Idempotency

**Decision**: Add optional `booking_id` FK on `housekeeping_tasks`; partial unique index `(booking_id) WHERE status IN ('PENDING','IN_PROGRESS')` — one active task per checkout.

**Rationale**: Spec US1 — no duplicate task per checkout event.

## 4. Task Status Enum

**Decision**: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` (API uppercase; display Pending/In Progress).

**Transitions**:
- PENDING → IN_PROGRESS (Employee start)
- IN_PROGRESS → COMPLETED (Employee finish)
- PENDING|IN_PROGRESS → CANCELLED (Manager cancel)
- No skip PENDING → COMPLETED

**Rationale**: Spec FR-010 strict transitions.

## 5. Room Status Sync

**Decision**: Transactional updates in `HousekeepingTaskService`:

| Task transition | Room status |
|-----------------|-------------|
| Auto-create on checkout | PENDING_CLEANING |
| IN_PROGRESS | CLEANING_IN_PROGRESS |
| COMPLETED | AVAILABLE |
| CANCELLED from IN_PROGRESS | PENDING_CLEANING |

**Rationale**: entity-ui-mapping §2.2; spec FR-002/007/008.

## 6. Manager vs Employee APIs

**Decision**:
- **Manager** `/api/v1/manager/housekeeping-tasks/**` — list, create, assign, cancel; property scoped
- **Employee** `/api/v1/employee/housekeeping-tasks/**` — list assigned, PATCH status
- **Admin** `/api/v1/admin/housekeeping-tasks` — GET read-only, optional propertyId filter

**Rationale**: Access matrix; api-spec baseline.

## 7. Assign Employee Validation

**Decision**: `assignedEmployeeId` must have ACTIVE `employee_property_assignments` for task's `propertyId` (FR-20).

**Rationale**: Spec FR-011; §10 acceptance.

## 8. Manual Task Create

**Decision**: `POST /manager/housekeeping-tasks` body `{ roomId, assigneeId? }` — room must belong to manager's property; room status should be PENDING_CLEANING or allow create from OCCUPIED edge (reject AVAILABLE).

**Rationale**: api-spec SCR-40 POST shape.

## 9. Available Bypass Guard

**Decision**: `RoomStatusGuardService.assertCanSetAvailable(roomId)` — reject if open housekeeping task exists with status PENDING or IN_PROGRESS for that room. Called from FR-08 `RoomService.updateStatus`.

**Rationale**: Spec FR-009; Agents.md "Manager CANNOT bypass Housekeeping".

## 10. Admin Read-Only

**Decision**: Admin controller exposes GET only; no POST/PATCH on admin routes.

**Rationale**: Access matrix R-only for Admin on Housekeeping.

## 11. Migration Number

**Decision**: `V037__housekeeping_tasks_fr21.sql` after V036 (FR-20).

## 12. Frontend Routes

**Decision**:
- Manager: `/manager/housekeeping` — SCR-40 kanban/board (To Do, In Progress, Done)
- Employee: `/employee/housekeeping` — SCR-60 touch-friendly list with Start/Finish buttons

**Rationale**: screen.md; screendesign SCR-40/SCR-60.

## 13. History Filter

**Decision**: Manager list supports `?status=COMPLETED&fromDate=&toDate=` for US6 history (default board shows PENDING + IN_PROGRESS; Done column shows recent COMPLETED).

**Rationale**: Spec US6 audit/history.
