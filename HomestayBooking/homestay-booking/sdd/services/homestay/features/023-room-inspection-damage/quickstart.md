# Quickstart: FR-23 Room Inspection & Damage Resolution

**Feature**: `specs/023-room-inspection-damage` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 (JWT all roles); FR-04 (booking CHECKED_IN / requestCheckout); FR-06/20 property scope; FR-12 payments; FR-15 notifications optional.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: Checked-in booking with check-out today; Employee/Manager/Admin/Customer users at same property

## Environment

```bash
# Optional escalation threshold override (default 5000000 VND)
# app.damage.escalation-threshold-vnd=5000000
```

Vite proxy: `/api/v1` → `http://localhost:8080`

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | Key APIs |
|--------|-------|----------|
| SCR-62 Room Inspection Hub | `/employee/inspections` | `GET/POST /employee/room-inspections`, `PATCH .../submit` |
| SCR-63 Damage Report List | `/employee/damage-reports` | `GET /employee/damage-reports` |
| SCR-64 Create Damage Report | `/employee/damage-reports/new` | `POST /employee/damage-reports` |
| SCR-42 Inspection Management | `/manager/inspections` | `GET /manager/room-inspections` |
| SCR-43 Damage Reports | `/manager/damage-reports` | `GET /manager/damage-reports`, `PATCH .../approve` |
| SCR-53 Damage Escalation | `/admin/damage-escalation` | `GET /admin/damage-reports`, `PATCH .../co-approve` |
| Customer Dispute | dashboard/notification | `PATCH /customer/damage-reports/{id}/dispute` |

## Smoke test: Happy path (Pass — no damage)

```bash
BASE=http://localhost:8080/api/v1
EMPLOYEE_TOKEN="<employee-jwt>"
MANAGER_TOKEN="<manager-jwt>"
BOOKING_ID="<checked-in-booking-uuid>"

# 1. Request checkout (FR-04) — creates inspection
curl -s -X POST "$BASE/manager/bookings/$BOOKING_ID/request-checkout" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq

# 2. Start inspection
curl -s -X POST "$BASE/employee/room-inspections" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"bookingId\":\"$BOOKING_ID\"}" | jq

INSPECTION_ID="<from-response>"

# 3. Submit PASS
curl -s -X PATCH "$BASE/employee/room-inspections/$INSPECTION_ID/submit" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"result":"PASS","checklist":{"tv":true,"minibar":true,"bed":true,"bathroom":true}}' | jq

# 4. Complete checkout (FR-04) — should succeed when balance paid
curl -s -X POST "$BASE/manager/bookings/$BOOKING_ID/checkout" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq
```

## Smoke test: Damage path + Manager approve (≤ 5M)

```bash
# After FAIL inspection:
curl -s -X PATCH "$BASE/employee/room-inspections/$INSPECTION_ID/submit" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"result":"FAIL","checklist":{"tv":false,"minibar":true,"bed":true,"bathroom":true},"note":"TV cracked"}' | jq

# Create damage report
curl -s -X POST "$BASE/employee/damage-reports" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"inspectionId\":\"$INSPECTION_ID\",\"items\":[{\"itemName\":\"TV\",\"description\":\"Screen cracked\",\"estimatedCost\":4000000}]}" | jq

REPORT_ID="<from-response>"

# Manager approve 4M
curl -s -X PATCH "$BASE/manager/damage-reports/$REPORT_ID/approve" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fee":4000000}' | jq
# Expected: status APPROVED, damage fee on booking, DAMAGE_FEE payment pending
```

## Smoke test: Escalation > 5M + Admin co-approve

```bash
curl -s -X PATCH "$BASE/manager/damage-reports/$REPORT_ID/approve" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fee":6000000}' | jq
# Expected: status ESCALATED

ADMIN_TOKEN="<admin-jwt>"
curl -s -X PATCH "$BASE/admin/damage-reports/$REPORT_ID/co-approve" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"approvedFee":6000000}' | jq
# Expected: status APPROVED
```

## Smoke test: Customer Dispute (24h window)

```bash
CUSTOMER_TOKEN="<customer-jwt>"

curl -s -X PATCH "$BASE/customer/damage-reports/$REPORT_ID/dispute" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"note":"I did not damage the TV. It was already broken."}' | jq
# Expected: status DISPUTED
```

## Smoke test: Checkout blocked

```bash
# Attempt checkout before inspection complete:
curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$BASE/manager/bookings/$BOOKING_ID/checkout" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
# Expected: 409 INSPECTION_REQUIRED
```

## Smoke test: RBAC

```bash
curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/manager/damage-reports?propertyId=<other-property>" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
# Expected: 403 if manager not assigned to property
```

## Manual UI checklist

- [ ] Employee SCR-62: list rooms, checklist drawer, Pass/Fail
- [ ] Fail → navigate to SCR-64 create report with items + photos
- [ ] SCR-63 lists employee reports
- [ ] Manager SCR-42 inspection table with Passed/Failed badges
- [ ] Manager SCR-43 drawer: view photos, approve fee; >5M shows escalate message
- [ ] Admin SCR-53: escalated list + co-approve
- [ ] Customer Dispute within 24h from notification/dashboard
- [ ] Checkout blocked until inspection + damage paid
- [ ] P2: Manager mark Outstanding Debt blocks new booking

## Troubleshooting

| Issue | Check |
|-------|-------|
| 409 INSPECTION_REQUIRED | Complete inspection PASS or damage flow first |
| 409 DAMAGE_FEE_UNPAID | Pay DAMAGE_FEE via FR-12 before checkout |
| Escalation not triggered | fee > 5000000; check `DamageEscalationProperties` |
| Dispute 400 | Within 24h of `approvedAt`; Asia/Ho_Chi_Minh |
| Duplicate inspection | Unique booking_id on room_inspections |
| FR-21 no housekeeping | Checkout must complete first; FR-21 hook downstream |
