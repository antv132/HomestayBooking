# Research: FR-16 Reporting

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `reportApi.ts`, `managerApi.ts`, api-spec SCR-27/44/45/55

## 1. Persistence Strategy

**Decision**: **No dedicated `reports` table** v1 — all metrics computed on-demand via SQL aggregates against source tables (`payments`, `bookings`, `rooms`, `properties`, `floors`, `users`).

**Rationale**: Spec Assumptions; read-only feature; YAGNI for materialized views until performance proven.

**Alternatives considered**: Materialized view `report_daily_snapshots` — rejected v1 (adds refresh job complexity).

## 2. Revenue Calculation

**Decision**: `totalRevenue = SUM(payments.amount)` WHERE `payments.status = 'PAID'` AND `payments.paid_at` (or `updated_at` when paid) within `[from, to]` timezone **Asia/Ho_Chi_Minh**, joined `bookings → rooms → properties` for property scope.

Split:
- `depositRevenue` — `payment.type = DEPOSIT`
- `balanceRevenue` — `payment.type = REMAINING_BALANCE`

Exclude payments linked to bookings `CANCELLED` where refund processed (assumption: if status PAID on cancelled booking, still counts unless `refunded_at` set — v1 count all PAID).

**Rationale**: Spec FR-008; `reportApi.ts` `RevenueReportData` shape.

**Alternatives considered**: Revenue by booking `check_in` date — rejected (cash basis from payments per FR-12).

## 3. Occupancy Rate Formula

**Decision**: For date range `[from, to]`:

```text
available_room_nights = COUNT(rooms in property) × days_in_range
occupied_room_nights = SUM for each booking (status IN CONFIRMED, CHECKED_IN, CHECKED_OUT):
  overlap_nights(booking.check_in, booking.check_out, from, to)
occupancy_rate = (occupied_room_nights / available_room_nights) × 100
```

Cap at 100%; return 0 if zero rooms.

**Rationale**: Spec FR-009; industry standard room-night occupancy.

**Alternatives considered**: Snapshot at end of day only — rejected (less accurate for trends).

## 4. Booking Trend Counting

**Decision**: Count bookings WHERE `status IN (CONFIRMED, CHECKED_IN, CHECKED_OUT)` grouped by `created_at` (or `confirmed_at` if available) into periods `month`/`week`. Exclude `PENDING_DEPOSIT`, `CANCELLED`, `NO_SHOW`.

**Rationale**: Spec FR-010 assumption.

**Alternatives considered**: Count by check-in date — use **created_at** for "new bookings" trend semantics.

## 5. Manager Property Scope

**Decision**: `ReportPropertyScopeValidator.assertManagerAccess(managerId, propertyId)` joins `manager_property_assignments` WHERE `status = ACTIVE` — reuse FR-06 `PropertyScopeService` if exists.

Manager with multiple properties: `propertyId` **required** on property-scoped endpoints; list endpoint returns only assigned IDs.

**Rationale**: Spec FR-001, FR-011, FR-014; §8 Property-level Authorization.

**Alternatives considered**: Filter by assignment date — v1 full property history per spec assumption.

## 6. API Surface Unification

**Decision**: Align `api-spec-by-screen.md` + extend `reportApi.ts`:

| Method | Path | Role | Screen |
|--------|------|------|--------|
| GET | `/api/v1/reports/property-kpis` | MANAGER | SCR-27 |
| GET | `/api/v1/reports/revenue` | MANAGER | SCR-44 Revenue |
| GET | `/api/v1/reports/occupancy` | MANAGER | SCR-44 Occupancy |
| GET | `/api/v1/reports/booking-trends` | MANAGER | SCR-44 Booking Trends |
| GET | `/api/v1/reports/properties/{id}` | MANAGER | SCR-44 summary |
| GET | `/api/v1/reports/global-kpis` | ADMIN | SCR-45 |
| GET | `/api/v1/admin/reports/revenue` | ADMIN | SCR-55 |
| GET | `/api/v1/admin/reports/occupancy` | ADMIN | SCR-55 |
| GET | `/api/v1/admin/reports/booking-trends` | ADMIN | SCR-55 |

Deprecate `GET /api/manager/dashboard` — FR-16 `ManagerDashboardPage` composes from `property-kpis` + optional trend endpoints (or keep thin composite facade calling same services).

**Rationale**: api-spec partial coverage; frontend already expects rich revenue shape.

**Alternatives considered**: Single mega dashboard endpoint — split for testability and SCR-44 tab alignment.

## 7. GroupBy Period Format

**Decision**: `groupBy=month` → period key `yyyy-MM`; `groupBy=week` → `yyyy-'W'ww` (ISO week). Default `month`.

**Rationale**: `reportApi.ts` PeriodRevenue; `managerApi.ts` chart keys.

**Alternatives considered**: Daily grouping — omit v1 unless range < 31 days P2.

## 8. Date Range Validation

**Decision**: `@NotNull from`, `@NotNull to`, `to >= from`, max span **366 days** — return `400 VALIDATION_ERROR`.

**Rationale**: Spec edge case; SC-007 performance.

**Alternatives considered**: Unlimited range — rejected.

## 9. Global KPIs (Admin SCR-45)

**Decision**: `GlobalKpisResponse`:

- `totalRevenue` — all-time or current month (assumption: **current calendar month** for dashboard card + `totalRevenueAllTime` optional)
- `totalProperties`, `totalFloors`, `totalRooms`
- `availableRooms`, `occupiedRooms` — current snapshot from `rooms.status`
- `totalBookings` — count all non-cancelled
- `newCustomers` — users role CUSTOMER created last 30 days (P2 nice-to-have)

**Rationale**: Spec US-3; §8 acceptance Admin Dashboard KPIs.

**Alternatives considered**: Revenue all-time only — show both month + all-time on dashboard.

## 10. Admin Global Reports Year Filter

**Decision**: `GET /admin/reports/revenue?year=2026&propertyId=` returns `monthlyData: [{ month: 1..12, revenue, bookingCount }]`. Optional `propertyId` filters scope.

**Rationale**: api-spec SCR-55; spec US-4.

**Alternatives considered**: Same `from`/`to` as Manager — Admin year filter is primary per api-spec; support both in contract.

## 11. Performance Indexes (Optional V032)

**Decision**: `V032__reporting_indexes_fr16.sql`:

```sql
CREATE INDEX IF NOT EXISTS idx_payments_paid_at_status ON payments (paid_at, status) WHERE status = 'PAID';
CREATE INDEX IF NOT EXISTS idx_bookings_property_dates ON bookings (room_id, status, check_in, check_out);
```

**Rationale**: SC-001, SC-007; no schema change to domain tables.

**Alternatives considered**: No indexes — add as optional phase A.

## 12. CSV Export

**Decision**: **Client-side only** (existing `RevenueReportPage` export) — no backend export endpoint v1.

**Rationale**: Spec US-5 P2; YAGNI.

**Alternatives considered**: Server-generated CSV — rejected v1.

## 13. Frontend Route Mapping

**Decision**:

| Screen | Route |
|--------|-------|
| SCR-27 Manager Dashboard | `/manager/dashboard` (exists) |
| SCR-44 Reports hub | `/manager/reports` (exists) |
| SCR-44 Revenue | `/manager/reports/revenue` (exists) |
| SCR-44 Occupancy | `/manager/reports/occupancy` (exists) |
| SCR-44 Booking Trends | `/manager/reports/bookings` → new `BookingTrendReportPage` |
| SCR-45 Admin Dashboard | `/admin/dashboard` (new) |
| SCR-55 Global Reports | `/admin/reports` (new) |

**Rationale**: `App.tsx` current state; screendesign tabs.

**Alternatives considered**: Single admin page — separate dashboard vs deep reports per docs.

## 14. Flyway Version

**Decision**: `V032__reporting_indexes_fr16.sql` after FR-15 V031 — indexes only, optional for MVP.

**Rationale**: Sequential migration numbering.

**Alternatives considered**: No migration file — still ship empty/comments-only V032 for feature traceability.
