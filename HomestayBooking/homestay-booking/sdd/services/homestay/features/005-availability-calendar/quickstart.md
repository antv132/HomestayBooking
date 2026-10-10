# Quickstart: FR-05 Availability Calendar

**Feature**: `specs/005-availability-calendar` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: Seed room + property (FR-03/08); sample bookings (FR-04) and optional housekeeping tasks (FR-21) for multi-status demo.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Backend with public read `permitAll` for calendar GET

## Environment

Same as FR-01/03 quickstart. Optional frontend polling:

```bash
# frontend — calendar pages use refetchInterval 30000 ms until FR-15 WebSocket
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
| SCR-09 Guest Calendar | `/rooms/:id/calendar` | `GET /rooms/{id}/availability?month&year` |
| SCR-33 Room Status | `/manager/rooms/:id/status` | `PATCH /manager/rooms/{id}/status` |
| Manager calendar view | `/manager/rooms/:id` (detail tab) | `GET /manager/rooms/{id}/calendar?month&year` |
| FR-03 range check | Room detail mini calendar | `GET /rooms/{id}/availability?checkIn&checkOut` |
| Legacy compat | Room detail | `GET /rooms/{id}/calendar` |

## curl smoke tests

Replace `ROOM_ID`, `MGR_TOKEN`.

```bash
BASE=http://localhost:8080/api/v1
ROOM_ID="<uuid>"
MGR_TOKEN="<manager-jwt>"

# Public month calendar (SCR-09 / FR-05)
curl -s "$BASE/rooms/$ROOM_ID/availability?month=7&year=2026" | jq

# Expect days[].status in 8 enums and bookable flag
curl -s "$BASE/rooms/$ROOM_ID/availability?month=7&year=2026" | jq '.data.days[] | select(.bookable==false)'

# Range availability (FR-03 compat)
curl -s "$BASE/rooms/$ROOM_ID/availability?checkIn=2026-07-10&checkOut=2026-07-13" | jq

# Legacy calendar
curl -s "$BASE/rooms/$ROOM_ID/calendar" | jq

# Manager enriched calendar
curl -s "$BASE/manager/rooms/$ROOM_ID/calendar?month=7&year=2026" \
  -H "Authorization: Bearer $MGR_TOKEN" | jq

# Create maintenance block (SCR-33)
curl -s -X PATCH "$BASE/manager/rooms/$ROOM_ID/status" \
  -H "Authorization: Bearer $MGR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"MAINTENANCE","startDate":"2026-07-01","endDate":"2026-07-05","reason":"AC repair"}' | jq

# Re-fetch calendar — days 1-4 July should show MAINTENANCE
curl -s "$BASE/rooms/$ROOM_ID/availability?month=7&year=2026" | jq '.data.days[] | select(.date>="2026-07-01" and .date<="2026-07-05")'

# Try Available when HK incomplete (expect 409)
curl -s -X PATCH "$BASE/manager/rooms/$ROOM_ID/status" \
  -H "Authorization: Bearer $MGR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"AVAILABLE"}' | jq

# List status blocks
curl -s "$BASE/manager/rooms/$ROOM_ID/status-blocks" \
  -H "Authorization: Bearer $MGR_TOKEN" | jq
```

## Validation checklist

- [ ] Each day in month has exactly one `status` from 8-value enum
- [ ] `bookedDates` matches non-bookable days (FR-03 compat)
- [ ] Maintenance block rejected when overlaps active booking (409)
- [ ] Available rejected when housekeeping task open (409)
- [ ] Guest response has no `bookingId` fields
- [ ] Manager calendar includes optional booking/block ids
- [ ] Calendar UI legend shows 8 statuses on SCR-09

## Troubleshooting

| Issue | Fix |
|-------|-----|
| All days AVAILABLE | Seed bookings/blocks; verify CalendarStatusService wired |
| 403 manager calendar | Manager not assigned to room's property |
| FR-03 search still bookable on maintenance days | Wire search to `CalendarStatusService.isBookable` |
| Stale calendar after booking | Wait 30s refetch or hard refresh until FR-15 |
