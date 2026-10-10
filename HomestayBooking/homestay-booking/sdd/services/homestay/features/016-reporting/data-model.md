# Data Model: FR-16 Reporting

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: aggregate reads from FR-04/06/07/08/12 entities

## Scope

FR-16 **does not own** persistent report tables v1. **Reads** from:

- `payments` (FR-12) — revenue source
- `bookings` (FR-04) — booking trends, occupancy nights, check-ins today
- `rooms`, `floors`, `properties` (FR-06/07/08) — counts, status snapshot
- `users` (FR-01/09) — new customers count (optional)
- `manager_property_assignments` (FR-06) — scope validation

## ERD (read paths)

```text
Property 1──* Floor 1──* Room 1──* Booking 1──* Payment (PAID)

Manager ──assigned──> Property (scope gate)

ReportQuery:
  RevenueReportService    ──> payments JOIN bookings JOIN rooms
  OccupancyReportService  ──> bookings + rooms (room-nights)
  BookingTrendReportService ──> bookings (count by period)
  PropertyKpisService     ──> composite counts + current month revenue
  GlobalKpisService       ──> system-wide COUNT/SUM
```

## DTO: PropertyKpisResponse (SCR-27)

| Field | Type | Source |
|-------|------|--------|
| propertyId | UUID | param |
| totalRooms | int | COUNT rooms WHERE property_id |
| occupancyRate | decimal | OccupancyReportService current month or today snapshot |
| revenue | long | SUM PAID payments current month for property |
| pendingCheckIns | int | COUNT bookings CHECK_IN date = today AND status CONFIRMED |
| pendingApprovals | int | COUNT payments PENDING verification + maintenance OPEN (assumption) |

## DTO: RevenueReportResponse (SCR-44 / reportApi.ts)

| Field | Type | Notes |
|-------|------|-------|
| totalRevenue | long | |
| depositRevenue | long | DEPOSIT PAID |
| balanceRevenue | long | REMAINING_BALANCE PAID |
| totalBookingCount | int | distinct bookings with ≥1 PAID payment in range |
| byPeriod | PeriodRevenue[] | `{ period, revenue, bookingCount }` |
| byProperty | PropertyRevenue[] | Manager: single property; Admin no filter: all properties |

## DTO: OccupancyReportResponse

| Field | Type | Notes |
|-------|------|-------|
| overallOccupancyRate | decimal | weighted for range |
| totalRooms | int | |
| occupiedRooms | int | snapshot or avg — document as snapshot at `to` date for summary card |
| availableRooms | int | totalRooms - occupied (snapshot) |
| byPeriod | PeriodOccupancy[] | `{ period, occupancyRate, occupiedRoomNights, availableRoomNights }` |
| byProperty | PropertyOccupancy[] | Manager single property; Admin multi |

## DTO: BookingTrendReportResponse

| Field | Type | Notes |
|-------|------|-------|
| totalBookings | int | in range |
| byPeriod | PeriodBookingTrend[] | `{ period, bookingCount, cancelledCount? }` |
| cancelledCount optional P2 | | |

## DTO: GlobalKpisResponse (SCR-45)

| Field | Type | Notes |
|-------|------|-------|
| totalRevenue | long | current month all properties |
| totalRevenueAllTime | long | optional |
| totalProperties | int | |
| totalFloors | int | |
| totalRooms | int | |
| availableRooms | int | rooms.status = AVAILABLE |
| occupiedRooms | int | OCCUPIED or derived from active bookings |
| totalBookings | int | non-cancelled |
| totalBookingsActive | int | CONFIRMED + CHECKED_IN |
| newCustomers | int | last 30 days (optional) |

## DTO: AdminYearlyRevenueResponse (SCR-55)

| Field | Type | Notes |
|-------|------|-------|
| year | int | |
| propertyId | UUID nullable | filter |
| totalRevenue | long | |
| monthlyData | MonthlyRevenue[] | `{ month: 1-12, revenue, bookingCount }` |

## Validation Rules

| Rule | Error |
|------|-------|
| Manager access property not assigned | `UNAUTHORIZED_PROPERTY_ACCESS` |
| `to` before `from` | `VALIDATION_ERROR` |
| Range > 366 days | `VALIDATION_ERROR` |
| Invalid `groupBy` | `VALIDATION_ERROR` |
| propertyId required for Manager scoped reports | `VALIDATION_ERROR` |
| Admin-only global endpoints by Manager | `403 FORBIDDEN` |

## SQL Sketches (conceptual)

### Revenue by period (month)

```sql
SELECT to_char(p.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') AS period,
       SUM(p.amount) AS revenue,
       COUNT(DISTINCT p.booking_id) AS booking_count
FROM payments p
JOIN bookings b ON b.id = p.booking_id
JOIN rooms r ON r.id = b.room_id
WHERE p.status = 'PAID'
  AND r.property_id = :propertyId
  AND p.paid_at >= :from AND p.paid_at < :to
GROUP BY 1
ORDER BY 1;
```

### Occupancy room-nights (application layer or SQL generate_series)

Overlap nights computed in `OccupancyReportService` for clarity; unit test against hand-calculated sample.

## Optional Migration: V032

```sql
-- V032__reporting_indexes_fr16.sql
CREATE INDEX IF NOT EXISTS idx_payments_paid_reporting
  ON payments (status, paid_at) WHERE status = 'PAID';

CREATE INDEX IF NOT EXISTS idx_bookings_room_dates
  ON bookings (room_id, check_in, check_out, status);
```

No new tables.

## Cross-FR Dependencies

| FR | Data used |
|----|-----------|
| FR-04 | `bookings.status`, dates, `room_id` |
| FR-12 | `payments.amount`, `status`, `type`, `paid_at` |
| FR-06 | `properties`, `manager_property_assignments` |
| FR-07 | `floors` count |
| FR-08 | `rooms.status`, count per property |
| FR-09 | `users` created_at for newCustomers |

## Frontend Type Alignment

Migrate `Frontend/src/api/reportApi.ts` types to match `RevenueReportResponse` exactly. Add `occupancyApi` / extend `reportApi` with `getOccupancy`, `getBookingTrends`, `getPropertyKpis`, `getGlobalKpis`.
