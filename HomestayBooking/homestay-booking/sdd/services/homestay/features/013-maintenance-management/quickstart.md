# Quickstart: FR-13 Maintenance Management

**Feature**: `specs/013-maintenance-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-04 booking (CONFIRMED or CHECKED_IN); FR-01 JWT (Customer, Manager, Employee); FR-06 Manager property assignment + Employee on same property; Flyway V029 applied.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: 1 property, 1 room, 1 confirmed booking, 1 manager, 1 employee assigned to property

## Environment

```bash
APP_UPLOADS_DIR=./uploads/maintenance
# Optional FR-15 stub — no extra env required for v1
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
| SCR-22 List | `/customer/maintenance` | `GET /maintenance-tickets/me` |
| SCR-23 Create | `/customer/maintenance/create` | `POST /maintenance-tickets` (multipart) |
| Customer Detail | `/customer/maintenance/:id` | `GET /maintenance-tickets/{id}` |
| SCR-41 Manager | `/manager/maintenance` | `GET /manager/maintenance-tickets?propertyId=` |
| Manager Drawer | `/manager/maintenance/:id` | `PATCH .../assign`, `PATCH .../close` |
| SCR-61 Employee | `/employee/maintenance` (new) | `GET /employee/maintenance-tickets` |

## Smoke test: full lifecycle

```bash
BASE=http://localhost:8080/api/v1
CUSTOMER_TOKEN="<customer-jwt>"
MANAGER_TOKEN="<manager-jwt>"
EMPLOYEE_TOKEN="<employee-jwt>"
BOOKING_ID="<confirmed-booking-uuid>"
PROPERTY_ID="<property-uuid>"
EMPLOYEE_ID="<employee-uuid>"

# 1. Customer creates ticket
curl -s -X POST "$BASE/maintenance-tickets" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -F "bookingId=$BOOKING_ID" \
  -F "title=AC not cooling" \
  -F "description=Air conditioner in bedroom stopped working since morning." \
  -F "files=@./test-fixture/ac-photo.jpg" | jq

TICKET_ID=$(curl -s "$BASE/maintenance-tickets/me" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq -r '.data.content[0].id')

# Expected status: OPEN

# 2. Manager assigns employee
curl -s -X PATCH "$BASE/manager/maintenance-tickets/$TICKET_ID/assign" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"assigneeId\":\"$EMPLOYEE_ID\"}" | jq

# Expected status: ASSIGNED

# 3. Employee starts work
curl -s -X PATCH "$BASE/employee/maintenance-tickets/$TICKET_ID/status" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS","workNote":"Replaced capacitor"}' | jq

# 4. Employee marks resolved
curl -s -X PATCH "$BASE/employee/maintenance-tickets/$TICKET_ID/status" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"RESOLVED"}' | jq

# 5. Manager closes with resolution note
curl -s -X PATCH "$BASE/manager/maintenance-tickets/$TICKET_ID/close" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"resolutionNote":"AC repaired and tested. Room temperature normal."}' | jq

# Expected status: CLOSED
curl -s "$BASE/maintenance-tickets/$TICKET_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.status'
```

## Smoke test: Open-only edit/delete

```bash
# Edit while OPEN — should succeed
curl -s -X PUT "$BASE/maintenance-tickets/$TICKET_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -F "title=AC fixed title" \
  -F "description=Updated description with more than twenty characters." | jq

# After ASSIGNED — edit should fail 409
# (re-create OPEN ticket or use fresh ticket for delete test)
curl -s -X DELETE "$BASE/maintenance-tickets/$OPEN_TICKET_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq
```

## Smoke test: scope enforcement

```bash
# Manager wrong property — 403
curl -s "$BASE/manager/maintenance-tickets?propertyId=<other-property-uuid>" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq

# Employee not assignee — 403 on status update
curl -s -X PATCH "$BASE/employee/maintenance-tickets/$TICKET_ID/status" \
  -H "Authorization: Bearer $OTHER_EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}' | jq
```

## Verification checklist

- [ ] Create requires CONFIRMED/CHECKED_IN booking
- [ ] Status flow OPEN→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED enforced
- [ ] Manager cannot skip to IN_PROGRESS without assign
- [ ] Customer cannot edit after ASSIGNED
- [ ] Attachments max 5 files, image types only
- [ ] Notification event logged/outbox on status changes (FR-15 stub)
- [ ] ActivityLog TICKET_* entries created

## Frontend manual test

1. Login as Customer → Maintenance → Create Request → submit with photo
2. Login as Manager → Maintenance → open Drawer → assign Employee
3. Login as Employee → Maintenance Workspace → Start → Finish
4. Login as Manager → close with resolution note
5. Customer sees CLOSED + resolution note on detail
