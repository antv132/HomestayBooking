# Quickstart: FR-19 Customer Dashboard

**Feature**: `specs/019-customer-dashboard` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 (CUSTOMER JWT); FR-04 bookings; FR-12 payments; FR-13 maintenance; FR-15 notifications. Optional V035 indexes.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: Customer user with linked bookings, payments, tickets, notifications

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
| SCR-15 Customer Dashboard | `/customer/dashboard` | `GET /api/v1/customer/dashboard` |

## Smoke test: Customer dashboard composite

```bash
BASE=http://localhost:8080/api/v1
CUSTOMER_TOKEN="<customer-jwt>"

curl -s "$BASE/customer/dashboard" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq

# Expected: success true, data with KPI fields + lists
```

## Smoke test: KPI accuracy

```bash
# After seeding 2 CONFIRMED bookings for customer:
curl -s "$BASE/customer/dashboard" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.activeBookings'
# Expected: 2

# After 1 PENDING payment:
curl -s "$BASE/customer/dashboard" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.pendingPayments'
# Expected: 1
```

## Smoke test: Upcoming check-in event

```bash
curl -s "$BASE/customer/dashboard" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.upcomingCheckIn'

# Expected: bookingId, roomNumber, propertyName, date, daysUntil (or null if none)
```

## Smoke test: List bounds

```bash
curl -s "$BASE/customer/dashboard" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '{
    bookings: (.data.upcomingBookings | length),
    payments: (.data.recentPayments | length),
    notifications: (.data.recentNotifications | length)
  }'

# Expected: bookings <= 5, payments <= 3, notifications <= 5
```

## Smoke test: RBAC denial

```bash
ADMIN_TOKEN="<admin-jwt>"
GUEST=# no token

curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/customer/dashboard" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# Expected: 403

curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/customer/dashboard"
# Expected: 401
```

## Smoke test: Data isolation

```bash
CUSTOMER_B_TOKEN="<other-customer-jwt>"

curl -s "$BASE/customer/dashboard" \
  -H "Authorization: Bearer $CUSTOMER_B_TOKEN" | jq '.data.upcomingBookings[].id'

# Expected: only Customer B booking IDs — none from Customer A
```

## Smoke test: Empty customer (zero data)

```bash
NEW_CUSTOMER_TOKEN="<new-customer-no-bookings>"

curl -s "$BASE/customer/dashboard" \
  -H "Authorization: Bearer $NEW_CUSTOMER_TOKEN" | jq '.data | {
    activeBookings, pendingPayments, openTickets, unreadNotifications,
    upcomingCheckIn, upcomingCheckOut
  }'

# Expected: all counts 0, upcomingCheckIn/Out null, empty arrays — no error
```

## Manual UI checklist

- [ ] Login as Customer → `/customer/dashboard` loads KPI row
- [ ] Pending payments KPI highlighted when > 0
- [ ] Upcoming check-in/out cards show countdown; empty state when none
- [ ] Upcoming bookings list (max 5); PENDING_DEPOSIT shows pay CTA
- [ ] Recent notifications (5) with unread styling
- [ ] **Recent payments section** (3 items) — add if missing in UI
- [ ] Quick links: Tìm phòng, Booking của tôi, Báo cáo sự cố
- [ ] Manager/Admin redirected or 403 on customer dashboard route
- [ ] Damage dispute banner (P2) when FR-23 pending dispute exists

## Troubleshooting

| Issue | Check |
|-------|-------|
| 404 on dashboard API | Endpoint must be `/api/v1/customer/dashboard` not `/api/customers/dashboard` |
| KPI mismatch | Verify status enums align FR-04/12/13/15; timezone Asia/Ho_Chi_Minh |
| Slow load (>3s) | Apply V035 indexes; check N+1 in CustomerDashboardService |
| Empty lists with data | customerId scope from JWT must match seeded customer |
| 403 as Customer | JWT role must be CUSTOMER |
