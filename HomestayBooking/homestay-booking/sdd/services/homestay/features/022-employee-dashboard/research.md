# Research: FR-22 Employee Dashboard

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: api-spec SCR-59, screendesign SCR-59, FR-19 pattern

## 1. Composite vs Multiple Endpoints

**Decision**: **Single** `GET /api/v1/employee/dashboard` returning full `EmployeeDashboardResponse` in one round-trip.

**Rationale**: Spec SC-007; mirrors FR-19 Customer Dashboard; SCR-59 needs KPI + multiple preview sections — one call avoids waterfall on mobile.

**Alternatives considered**: Keep only `GET /api/v1/employee/kpis` (api-spec SCR-59) — rejected (insufficient for lists/today/completed sections). Separate calls per task type — rejected (latency).

## 2. KPI Endpoint Compatibility

**Decision**: `GET /api/v1/employee/kpis` MAY remain as thin alias returning only `{ pendingHousekeeping, pendingMaintenance, pendingInspections }` extracted from dashboard service — or deprecated in favor of dashboard. Primary contract: `/employee/dashboard`.

**Rationale**: api-spec-by-screen.md documents kpis; full dashboard supersedes for SCR-59 implementation.

## 3. KPI Definitions

**Decision**:

| KPI | Query |
|-----|-------|
| `pendingHousekeeping` | COUNT `housekeeping_tasks` WHERE `assigned_employee_id` = :employeeId AND `status IN (PENDING, IN_PROGRESS)` AND `property_id IN (:assignedPropertyIds)` |
| `pendingMaintenance` | COUNT `maintenance_tickets` WHERE `assigned_employee_id` = :employeeId AND `status IN (ASSIGNED, IN_PROGRESS)` AND `property_id IN (:assignedPropertyIds)` AND `deleted_at IS NULL` |
| `pendingInspections` | COUNT `room_inspections` ri JOIN bookings b WHERE `ri.inspected_by` = :employeeId AND `ri.status IN (PENDING, IN_PROGRESS)` AND `b.property_id IN (:assignedPropertyIds)` |

**Rationale**: Spec assumptions; align FR-21/13/23 status enums.

## 4. Preview Lists (per type)

**Decision**:

| List | Filter | Order | Limit |
|------|--------|-------|-------|
| `housekeepingTasks` | assigned + PENDING/IN_PROGRESS | PENDING first, `created_at ASC` | 5 |
| `maintenanceTickets` | assigned + ASSIGNED/IN_PROGRESS | ASSIGNED first, `created_at ASC` | 5 |
| `inspections` | inspected_by + PENDING/IN_PROGRESS | `check_out ASC` via booking join | 5 |

**Rationale**: Spec US2–US3, US6; max 5 preview per type.

## 5. Today's Tasks (unified section)

**Decision**: `todayTasks[]` — union of:

- **Housekeeping**: assigned + PENDING/IN_PROGRESS AND (`created_at::date = today` OR linked booking `check_out::date = today`)
- **Maintenance**: assigned + ASSIGNED/IN_PROGRESS AND (`created_at::date = today` OR `assigned_at::date = today`)
- **Inspection**: inspected_by = employee + PENDING/IN_PROGRESS AND booking `check_out::date = today`

Each item: `{ type, id, roomNumber, propertyName, status, label, checkOutDate? }`. Max **10** items, sorted by urgency (inspection checkout today first, then housekeeping, maintenance).

**Rationale**: Spec US4; checkout-driven inspection priority per FR-23.

## 6. Awaiting vs Completed Today

**Decision**:

- **awaitingSummary**: `{ housekeepingCount, maintenanceCount, inspectionCount, previews[] }` where previews = up to **3** items per type (9 max) of pending work (same filters as KPI).
- **completedToday**: `{ housekeepingCount, maintenanceCount, inspectionCount, previews[] }` where:
  - Housekeeping: `status = COMPLETED` AND `completed_at::date = today`
  - Maintenance: `status = RESOLVED` AND `updated_at::date = today` (or dedicated resolved_at if FR-13 adds)
  - Inspection: `status = PASSED` AND `inspected_at::date = today` (or `updated_at` if inspected_at absent in stub)

Previews max **3** per type.

**Rationale**: Spec US5; "today only" for completed section.

## 7. Property Scope (FR-20)

**Decision**: Resolve `assignedPropertyIds` from `employee_property_assignments` for current employee. All queries filter `property_id IN (:ids)`. If employee has **zero** assignments → return zeros + empty lists + friendly empty state message key `noPropertyAssignment`.

**Rationale**: Spec FR-010; FR-20 scope.

## 8. No New Tables

**Decision**: FR-22 **does not** create dashboard-specific tables or materialized views v1.

**Rationale**: Read-only aggregate; YAGNI (same as FR-19).

## 9. Optional Indexes (V038)

**Decision**: Optional migration if explain plans slow:

- `room_inspections(inspected_by, status)` — if not covered by FR-23 migration
- Composite already on `housekeeping_tasks(assigned_employee_id, status)` and `maintenance_tickets(assigned_employee_id, status)` per FR-21/13

**Rationale**: SC-001 p95 < 3s.

## 10. Greeting

**Decision**: Response includes `fullName` from JWT user profile (FR-01/02). Frontend renders `Xin chào, {fullName}` or `Hello, {fullName}` per locale.

**Rationale**: Spec US1; screendesign "Hello, [Name]".

## 11. Security

**Decision**: `@PreAuthorize("hasRole('EMPLOYEE')")`; resolve `employeeId` from JWT `sub` — never accept employee id from query param.

**Rationale**: Spec FR-010, FR-011.

## 12. Frontend Mobile-First

**Decision**: SCR-59 uses large action cards (`min-height: 100px`, `radius-lg`, `shadow-md` per component-library). Stack layout on mobile; no status mutation buttons on dashboard — tap card navigates to workspace.

**Rationale**: Spec FR-014; screendesign SCR-59.

## 13. Navigation Targets

**Decision**:

| Action Card | Route |
|-------------|-------|
| Housekeeping | `/employee/housekeeping` (SCR-60, FR-21) |
| Maintenance | `/employee/maintenance` (SCR-61, FR-13) |
| Inspections | `/employee/inspections` (SCR-62, FR-23) |

**Rationale**: screendesign navigation; FR-22 read-only handoff.

## 14. Timezone

**Decision**: All "today" comparisons use `Asia/Ho_Chi_Minh` local date.

**Rationale**: Spec edge cases; align FR-19.
