# Quickstart: FR-21 Housekeeping Management

**Feature**: `specs/021-housekeeping-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 JWT (Manager, Employee); FR-06 property scope; FR-08 rooms; FR-20 employee assignments; V037 migration; checked-out booking for auto-create test.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: Manager + Property, Employee assigned to property, Room, Booking Checked-out (or manual task create)

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
| SCR-40 Housekeeping Tasks | `/manager/housekeeping` | `GET /manager/housekeeping-tasks?propertyId=` |
| SCR-40 Assign (drawer) | `/manager/housekeeping` | `PATCH /manager/housekeeping-tasks/{id}/assign` |
| SCR-60 Housekeeping Workspace | `/employee/housekeeping` | `GET /employee/housekeeping-tasks` |
| SCR-60 Start/Finish | `/employee/housekeeping` | `PATCH /employee/housekeeping-tasks/{id}/status` |

## Smoke test: Auto-create on checkout

```bash
BASE=http://localhost:8080/api/v1
# After FR-04 checkout completes for a booking:
PROPERTY_ID="<property-uuid>"
MANAGER_TOKEN="<manager-jwt>"

curl -s "$BASE/manager/housekeeping-tasks?propertyId=$PROPERTY_ID&status=PENDING" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq '.data.content | length'
# Expected: >= 1 new PENDING task; room status PENDING_CLEANING
```

## Smoke test: Manager list tasks

```bash
curl -s "$BASE/manager/housekeeping-tasks?propertyId=$PROPERTY_ID" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq

# Expected: success true, tasks with roomNumber, status, assigneeName
```

## Smoke test: Manager create + assign

```bash
ROOM_ID="<room-uuid>"
EMPLOYEE_ID="<employee-at-property>"

curl -s -X POST "$BASE/manager/housekeeping-tasks" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roomId": "'"$ROOM_ID"'", "assigneeId": "'"$EMPLOYEE_ID"'"}' | jq

TASK_ID="<uuid-from-response>"

curl -s -X PATCH "$BASE/manager/housekeeping-tasks/$TASK_ID/assign" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"assigneeId": "'"$EMPLOYEE_ID"'"}' | jq
```

## Smoke test: Employee start and complete

```bash
EMPLOYEE_TOKEN="<employee-jwt>"

curl -s -X PATCH "$BASE/employee/housekeeping-tasks/$TASK_ID/status" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}' | jq
# Expected: room CLEANING_IN_PROGRESS

curl -s -X PATCH "$BASE/employee/housekeeping-tasks/$TASK_ID/status" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}' | jq
# Expected: room AVAILABLE
```

## Smoke test: Skip transition rejected

```bash
# On PENDING task, try COMPLETED directly:
curl -s -o /dev/null -w "%{http_code}" \
  -X PATCH "$BASE/employee/housekeeping-tasks/$TASK_ID/status" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
# Expected: 400
```

## Smoke test: Manager cancel

```bash
curl -s -X PATCH "$BASE/manager/housekeeping-tasks/$TASK_ID/cancel" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"note": "Booking reversed"}' | jq '.data.status'
# Expected: CANCELLED
```

## Smoke test: RBAC

```bash
CUSTOMER_TOKEN="<customer-jwt>"

curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/manager/housekeeping-tasks?propertyId=$PROPERTY_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
# Expected: 403
```

## Manual UI checklist

- [ ] Manager `/manager/housekeeping` — board columns Pending / In Progress / Done
- [ ] Task card shows room + assignee
- [ ] Assign drawer picks Employee at same property
- [ ] Employee `/employee/housekeeping` — Start → Finish flow
- [ ] Room becomes Available only after Complete
- [ ] Manager cannot manually set room Available with open task (FR-08)
- [ ] Admin read-only list (no assign/cancel buttons)
- [ ] Empty state when no tasks

## Troubleshooting

| Issue | Check |
|-------|-------|
| No auto task after checkout | FR-04 hook calls `onBookingCheckedOut`; booking must be CHECKED_OUT |
| 403 manager list | propertyId must be manager-assigned property |
| 400 assign employee | Employee must have ACTIVE assignment at property (FR-20) |
| 409 duplicate task | booking_id unique index — one active task per checkout |
| V037 not applied | Run Flyway migration |
