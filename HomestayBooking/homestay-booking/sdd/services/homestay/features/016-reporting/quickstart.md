# Quickstart: FR-16 Reporting

**Feature**: `specs/016-reporting` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 JWT (Manager, Admin); FR-06 Manager property assignment; FR-04 bookings + FR-12 PAID payments seeded; optional V032 indexes.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: 2 properties, rooms, bookings with PAID payments across 3+ months, Manager assigned to property P1 only

## Environment

```bash
# Report timezone (backend default)
# APP_REPORT_TIMEZONE=Asia/Ho_Chi_Minh
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
| SCR-27 Manager Dashboard | `/manager/dashboard` | `GET /reports/property-kpis?propertyId=` |
| SCR-44 Reports hub | `/manager/reports` | — |
| SCR-44 Revenue | `/manager/reports/revenue` | `GET /reports/revenue` |
| SCR-44 Occupancy | `/manager/reports/occupancy` | `GET /reports/occupancy` |
| SCR-44 Booking Trends | `/manager/reports/bookings` | `GET /reports/booking-trends` |
| SCR-45 Admin Dashboard | `/admin/dashboard` | `GET /reports/global-kpis` |
| SCR-55 Global Reports | `/admin/reports` | `GET /admin/reports/revenue?year=` |

## Smoke test: Manager property KPIs

```bash
BASE=http://localhost:8080/api/v1
MANAGER_TOKEN="<manager-jwt>"
PROPERTY_ID="<assigned-property-uuid>"

curl -s "$BASE/reports/property-kpis?propertyId=$PROPERTY_ID" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq

# Expected: totalRooms, occupancyRate, revenue, pendingCheckIns
```

## Smoke test: Manager revenue report

```bash
curl -s "$BASE/reports/revenue?propertyId=$PROPERTY_ID&from=2026-01-01&to=2026-06-30&groupBy=month" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq

# Validate: totalRevenue == sum(byPeriod[].revenue)
# Cross-check with SQL:
# SELECT SUM(amount) FROM payments p JOIN bookings b ... WHERE status='PAID' AND paid_at BETWEEN ...
```

## Smoke test: scope denial

```bash
OTHER_PROPERTY="<not-assigned-uuid>"
curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/reports/revenue?propertyId=$OTHER_PROPERTY&from=2026-01-01&to=2026-06-30" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
# Expected: 403
```

## Smoke test: occupancy report

```bash
curl -s "$BASE/reports/occupancy?propertyId=$PROPERTY_ID&from=2026-01-01&to=2026-06-30&groupBy=month" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq
# Expected: overallOccupancyRate between 0 and 100
```

## Smoke test: booking trends

```bash
curl -s "$BASE/reports/booking-trends?propertyId=$PROPERTY_ID&from=2026-01-01&to=2026-06-30&groupBy=month" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq
```

## Smoke test: Admin global KPIs

```bash
ADMIN_TOKEN="<admin-jwt>"

curl -s "$BASE/reports/global-kpis" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
# Expected: totalProperties, totalFloors, totalRooms, totalBookings
```

## Smoke test: Admin yearly revenue

```bash
curl -s "$BASE/admin/reports/revenue?year=2026" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

curl -s "$BASE/admin/reports/revenue?year=2026&propertyId=$PROPERTY_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
# Second call: filtered to single property
```

## Frontend verification

1. Login as Manager → `/manager/dashboard` — KPI cards load for selected property
2. `/manager/reports/revenue` — charts + CSV export match API `byPeriod`
3. `/manager/reports/occupancy` — no mock data; live bars
4. Login as Admin → `/admin/dashboard` and `/admin/reports`

## Troubleshooting

| Symptom | Check |
|---------|-------|
| Revenue always 0 | PAID payments seeded? `paid_at` in range? |
| 403 on valid property | Manager assignment ACTIVE in FR-06 |
| Occupancy > 100% | Room-nights overlap bug — see unit test |
| Slow reports | Apply V032 indexes |
| Admin routes 404 | Add `/admin/dashboard` routes in App.tsx |

## Acceptance checklist (SC-001–SC-008)

- [ ] SC-001: KPI dashboard < 3s
- [ ] SC-002: Cross-property access blocked 100%
- [ ] SC-003: Revenue matches payment PAID sum
- [ ] SC-004: Occupancy within 1% of manual calc
- [ ] SC-005: Global KPI counts match DB
- [ ] SC-006: Manager can complete 3 report tabs < 2 min
- [ ] SC-007: 12-month range < 5s
- [ ] SC-008: CSV export matches table rows
