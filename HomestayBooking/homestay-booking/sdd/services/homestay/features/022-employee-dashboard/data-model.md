# Data Model: FR-22 Employee Dashboard

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: read aggregates from FR-21/13/23/20/04

## Scope

FR-22 **does not own** persistent tables. **Reads** from:

- `housekeeping_tasks` (FR-21)
- `maintenance_tickets` (FR-13)
- `room_inspections` (FR-23 / FR-04 stub)
- `bookings` (FR-04) — check-out date for today/inspection context
- `rooms`, `properties` — display names
- `employee_property_assignments` (FR-20) — scope filter
- `users` (FR-01) — greeting fullName

## ERD (read paths)

```text
Employee (users) 1──* employee_property_assignments *──1 Property
Employee 1──* HousekeepingTask (assigned_employee_id)
Employee 1──* MaintenanceTicket (assigned_employee_id)
Employee 1──* RoomInspection (inspected_by)
Booking 1──0..1 RoomInspection
Booking 1──0..1 HousekeepingTask

EmployeeDashboardService:
  resolveAssignedPropertyIds(employeeId)
  countPendingHousekeeping(employeeId, propertyIds)
  countPendingMaintenance(employeeId, propertyIds)
  countPendingInspections(employeeId, propertyIds)
  listHousekeepingPreview(employeeId, propertyIds, limit=5)
  listMaintenancePreview(employeeId, propertyIds, limit=5)
  listInspectionPreview(employeeId, propertyIds, limit=5)
  listTodayTasks(employeeId, propertyIds, limit=10)
  buildAwaitingSummary(employeeId, propertyIds)
  buildCompletedTodaySummary(employeeId, propertyIds)
```

## DTO: EmployeeDashboardResponse

| Field | Type | Source |
|-------|------|--------|
| fullName | string | users.full_name from JWT |
| pendingHousekeeping | int | COUNT HK PENDING + IN_PROGRESS |
| pendingMaintenance | int | COUNT MT ASSIGNED + IN_PROGRESS |
| pendingInspections | int | COUNT RI PENDING + IN_PROGRESS |
| housekeepingTasks | HousekeepingTaskSummaryDto[] | preview max 5 |
| maintenanceTickets | MaintenanceTicketSummaryDto[] | preview max 5 |
| inspections | RoomInspectionSummaryDto[] | preview max 5 |
| todayTasks | EmployeeTaskSummaryDto[] | unified max 10 |
| awaiting | AwaitingSummaryDto | counts + previews max 3/type |
| completedToday | CompletedTodaySummaryDto | counts + previews max 3/type |
| noPropertyAssignment | boolean | true when FR-20 has zero properties |

### HousekeepingTaskSummaryDto

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| roomNumber | string | from room join |
| propertyName | string | |
| status | string | PENDING, IN_PROGRESS |
| createdAt | datetime | |

### MaintenanceTicketSummaryDto

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| title | string | issue title |
| roomNumber | string | |
| propertyName | string | |
| status | string | ASSIGNED, IN_PROGRESS |
| createdAt | datetime | |

### RoomInspectionSummaryDto

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| bookingId | UUID | |
| roomNumber | string | |
| propertyName | string | |
| checkOutDate | date | from booking |
| status | string | PENDING, IN_PROGRESS |

### EmployeeTaskSummaryDto (unified today / preview)

| Field | Type | Notes |
|-------|------|-------|
| type | enum | HOUSEKEEPING, MAINTENANCE, INSPECTION |
| id | UUID | source entity id |
| roomNumber | string | |
| propertyName | string | |
| status | string | |
| label | string | display title e.g. ticket title or "Dọn phòng" |
| checkOutDate | date? | for inspections / checkout-driven HK |

### AwaitingSummaryDto

| Field | Type |
|-------|------|
| housekeepingCount | int |
| maintenanceCount | int |
| inspectionCount | int |
| previews | EmployeeTaskSummaryDto[] (max 9) |

### CompletedTodaySummaryDto

| Field | Type |
|-------|------|
| housekeepingCount | int |
| maintenanceCount | int |
| inspectionCount | int |
| previews | EmployeeTaskSummaryDto[] (max 9) |

## Query Rules

### Pending housekeeping

```sql
assigned_employee_id = :employeeId
AND property_id IN (:propertyIds)
AND status IN ('PENDING', 'IN_PROGRESS')
```

### Pending maintenance

```sql
assigned_employee_id = :employeeId
AND property_id IN (:propertyIds)
AND status IN ('ASSIGNED', 'IN_PROGRESS')
AND deleted_at IS NULL
```

### Pending inspections

```sql
ri.inspected_by = :employeeId
AND ri.status IN ('PENDING', 'IN_PROGRESS')
AND b.property_id IN (:propertyIds)
```

### Completed today — housekeeping

```sql
assigned_employee_id = :employeeId
AND status = 'COMPLETED'
AND completed_at::date = :today
```

### Completed today — maintenance

```sql
assigned_employee_id = :employeeId
AND status = 'RESOLVED'
AND updated_at::date = :today
```

### Completed today — inspection

```sql
inspected_by = :employeeId
AND status = 'PASSED'
AND COALESCE(inspected_at, updated_at)::date = :today
```

### Timezone

All date comparisons use `Asia/Ho_Chi_Minh` local date.

## Optional Migration: V038__employee_dashboard_indexes_fr22.sql

```sql
-- Only if FR-23 migration lacks inspected_by index
CREATE INDEX IF NOT EXISTS idx_room_inspections_employee_status
  ON room_inspections(inspected_by, status);

CREATE INDEX IF NOT EXISTS idx_bookings_checkout_property
  ON bookings(property_id, check_out);
```

## Error Codes

| Code | HTTP | When |
|------|------|------|
| DASHBOARD_ACCESS_DENIED | 403 | Non-EMPLOYEE |
| EMPLOYEE_SUSPENDED | 403 | FR-01 suspended user |

## Cross-FR Dependencies

| FR | Relationship |
|----|--------------|
| FR-01 | JWT EMPLOYEE role + fullName |
| FR-20 | property scope via employee_property_assignments |
| FR-21 | housekeeping_tasks source |
| FR-13 | maintenance_tickets source |
| FR-23 | room_inspections source |
| FR-04 | booking check_out for today filter |

## KPI ↔ Preview Consistency

KPI counts MUST equal or exceed preview list lengths (previews are bounded subsets). Service builds counts from full COUNT queries, not `previews.length`.
