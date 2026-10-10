# Quickstart: FR-22 Employee Dashboard

**Feature**: `specs/022-employee-dashboard` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 (EMPLOYEE JWT); FR-20 employee property assignments; FR-21 housekeeping tasks; FR-13 maintenance tickets; FR-23 room inspections (or FR-04 stub). Optional V038 indexes.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: Employee user assigned to Property A with housekeeping, maintenance, and inspection tasks

### Dev seed checklist (T005)

Sample employee should have FR-20 ACTIVE assignment + approximately:
- 3 housekeeping tasks (`PENDING` / `IN_PROGRESS`) assigned to employee
- 2 maintenance tickets (`ASSIGNED` / `IN_PROGRESS`, `deleted_at` null)
- 1 room inspection (`PENDING` or `IN_PROGRESS`) in assigned property (unassigned or `inspected_by` = employee)

Optional index migration: `V038__employee_dashboard_indexes_fr22.sql`

## Environment

```bash
# No feature-specific env vars
```

Vite proxy: `/api/v1` → `http://localhost:8080`

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| SCR-59 Employee Dashboard | `/employee/dashboard` | `GET /api/v1/employee/dashboard` |
| SCR-59 KPI subset | — | `GET /api/v1/employee/kpis` |

## Smoke test: Employee dashboard composite

```bash
BASE=http://localhost:8080/api/v1
EMPLOYEE_TOKEN="<employee-jwt>"

curl -s "$BASE/employee/dashboard" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" | jq

# Expected: success true, data with fullName, KPI fields, lists, todayTasks, awaiting, completedToday
```

## Smoke test: KPI accuracy

```bash
# After seeding 3 PENDING housekeeping tasks assigned to employee:
curl -s "$BASE/employee/dashboard" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" | jq '.data.pendingHousekeeping'
# Expected: 3

# KPI alias endpoint:
curl -s "$BASE/employee/kpis" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" | jq '.data'
# Expected: { pendingHousekeeping: 3, pendingMaintenance: N, pendingInspections: M }
```

## Smoke test: List bounds

```bash
curl -s "$BASE/employee/dashboard" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" | jq '{
    hk: (.data.housekeepingTasks | length),
    mt: (.data.maintenanceTickets | length),
    ri: (.data.inspections | length),
    today: (.data.todayTasks | length)
  }'

# Expected: hk <= 5, mt <= 5, ri <= 5, today <= 10
```

## Smoke test: Today's tasks

```bash
# Seed inspection with booking check_out = today:
curl -s "$BASE/employee/dashboard" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" | jq '.data.todayTasks[] | select(.type=="INSPECTION")'

# Expected: at least one inspection item with checkOutDate = today
```

## Smoke test: RBAC denial

```bash
MANAGER_TOKEN="<manager-jwt>"

curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/employee/dashboard" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
# Expected: 403

curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/employee/dashboard"
# Expected: 401
```

## Smoke test: Data isolation

```bash
EMPLOYEE_B_TOKEN="<other-employee-jwt>"

curl -s "$BASE/employee/dashboard" \
  -H "Authorization: Bearer $EMPLOYEE_B_TOKEN" | jq '.data.housekeepingTasks[].id'

# Expected: only Employee B task IDs — none from Employee A
```

## Smoke test: No property assignment

```bash
UNASSIGNED_EMPLOYEE_TOKEN="<employee-without-fr20-assignment>"

curl -s "$BASE/employee/dashboard" \
  -H "Authorization: Bearer $UNASSIGNED_EMPLOYEE_TOKEN" | jq '.data | {
    pendingHousekeeping, pendingMaintenance, pendingInspections, noPropertyAssignment
  }'

# Expected: all counts 0, noPropertyAssignment true, empty arrays
```

## Manual UI checklist

- [ ] Login as Employee → `/employee/dashboard` loads greeting + 3 KPI action cards
- [ ] Action cards show pending counts; tap navigates to SCR-60/61/62
- [ ] Housekeeping preview list (max 5) with room + status
- [ ] Maintenance preview list (max 5) with title + room
- [ ] Inspection preview list (max 5) with check-out date
- [ ] Section "Tác vụ hôm nay" shows checkout-driven items
- [ ] Sections "Đang chờ" vs "Đã hoàn thành hôm nay" with counts
- [ ] Empty states when no tasks; no crash
- [ ] Mobile layout: touch-friendly cards min-height 100px
- [ ] Manager/Customer blocked on employee dashboard route
- [ ] No Start/Finish/Pass buttons on dashboard (read-only)

## Troubleshooting

| Issue | Check |
|-------|-------|
| 404 on dashboard API | Endpoint must be `/api/v1/employee/dashboard` |
| KPI mismatch | Verify assigned_employee_id / inspected_by matches JWT user |
| Empty lists with data | FR-20 property assignment must include task property_id |
| Inspections missing | room_inspections.inspected_by must equal employee; booking join for check_out |
| Slow load (>3s) | Apply V038 indexes; avoid N+1 in EmployeeDashboardService |
| 403 as Employee | JWT role must be EMPLOYEE; account not SUSPENDED |
