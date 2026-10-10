# Research: FR-20 Employee Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: FR-06 manager assignments, FR-09 admin user mgmt, api-spec SCR-39

## 1. Table Ownership

**Decision**: FR-20 **owns** `employee_property_assignments` table (V036). FR-06 explicitly excludes EmployeePropertyAssignment.

**Rationale**: Spec entity §5; parallel to `manager_property_assignments` in FR-06.

**Alternatives considered**: Embed propertyId on users table — rejected (loses assignment history).

## 2. One ACTIVE Assignment Constraint

**Decision**: Partial unique index `UNIQUE (employee_id) WHERE status = 'ACTIVE'` — at most one ACTIVE assignment per employee globally.

**Rationale**: Spec FR-004; Employee belongs to one Property at a time.

**Alternatives considered**: Application-only check — rejected (race conditions).

## 3. Manager vs Admin API Surfaces

**Decision**: Split controllers:
- **Manager**: `/api/v1/manager/employees/**` — `propertyId` required on list/assign/create; scope via `PropertyScopeService.assertManagerAccess(managerId, propertyId)`
- **Admin**: `/api/v1/admin/employees/**` — `propertyId` optional filter; no property scope limit on mutations

**Rationale**: Align FR-06 Admin/Manager split; api-spec SCR-39 baseline is Manager-only.

**Alternatives considered**: Single `/employees` with role-based scope in service — rejected (inconsistent with codebase FR-06/09).

## 4. Create Employee Flow

**Decision**: `POST .../employees` body `{ fullName, email, phone, propertyId }`:
1. Create `users` row `role=EMPLOYEE`, `status=ACTIVE` (or INACTIVE + invite — align FR-01 existing pattern)
2. Create `employee_property_assignments` ACTIVE in same `@Transactional`
3. Trigger FR-01 password-set/invite email (no plaintext password on form)

**Rationale**: Spec US3; security-first.

**Alternatives considered**: Two-step create then assign — acceptable as separate endpoints but UX modal combines both.

## 5. Assign Existing Employee

**Decision**: `POST .../employees/assign` body `{ employeeId, propertyId }`:
- Reject if employee has ACTIVE assignment elsewhere
- Reject if employee `status = SUSPENDED`
- Reject if property `status != ACTIVE`
- Reject if Manager lacks property access

**Rationale**: api-spec SCR-39; spec US2 edge cases.

## 6. Unassigned Employee Picker

**Decision**: `GET .../employees/unassigned?search=` returns EMPLOYEE users with **no** ACTIVE assignment (paginated) for Assign modal dropdown.

**Rationale**: Manager needs to pick existing accounts not yet on a property.

## 7. Update & Status

**Decision**:
- `PUT .../employees/{id}` — `fullName`, `phone` only; email immutable v1
- `PATCH .../employees/{id}/status` — `ACTIVE` ↔ `SUSPENDED` only (not INACTIVE toggle by Manager)

**Rationale**: Spec US4/US5; align FR-09 customer status pattern.

## 8. Reassign (P2)

**Decision**: Admin-only `PATCH /api/v1/admin/employees/{id}/reassign` body `{ propertyId }`:
- Transaction: deactivate current ACTIVE → INACTIVE; create or reactivate assignment at new property → ACTIVE
- Optional warning if open housekeeping/maintenance tasks exist (log only v1)

**Rationale**: Spec US6 Admin-only cross-property.

## 9. List Query

**Decision**: Join `users` + `employee_property_assignments` WHERE `role=EMPLOYEE` AND `epa.status=ACTIVE` AND `epa.property_id = :propertyId`. Support `search` on fullName/email ILIKE. Paginate default size 20.

**Rationale**: Spec US1 columns; SC-001 search performance.

## 10. Audit Events

**Decision**: Log via FR-17 `ActivityLogService` (if available) or stub:
- `EMPLOYEE_CREATED`, `EMPLOYEE_ASSIGNED`, `EMPLOYEE_REASSIGNED`, `EMPLOYEE_STATUS_CHANGED`, `EMPLOYEE_UPDATED`

**Rationale**: Spec FR-014 SHOULD audit.

## 11. Migration Number

**Decision**: `V036__employee_property_assignments_fr20.sql` after V035 (FR-19 optional indexes).

**Rationale**: Sequential Flyway convention in repo.

## 12. Frontend Route

**Decision**:
- Manager: `/manager/employees` — property from Manager session/context selector (reuse FR-06 property context)
- Admin: `/admin/employees` — global Property dropdown filter

**Rationale**: screendesign SCR-39 Manager actor; spec Admin assumption.

## 13. Hard Delete

**Decision**: **Out of scope v1** — use Suspend only. Admin DELETE deferred P2+.

**Rationale**: Spec assumption soft-delete via Suspend; avoids FK complexity with FR-21/13 tasks.
