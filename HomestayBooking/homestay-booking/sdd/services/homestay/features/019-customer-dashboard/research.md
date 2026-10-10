# Research: FR-19 Customer Dashboard

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `customersApi.ts`, `CustomerDashboardPage.tsx`, api-spec SCR-15

## 1. Composite vs Multiple Endpoints

**Decision**: **Single** `GET /api/v1/customer/dashboard` returning full `CustomerDashboardResponse` in one round-trip.

**Rationale**: Spec SC-007; frontend already expects monolithic `CustomerDashboardData`; reduces waterfall on SCR-15.

**Alternatives considered**: Separate KPI + list endpoints — rejected (more latency, frontend refactor).

## 2. KPI Definitions

**Decision**:

| KPI | Query |
|-----|-------|
| `activeBookings` | COUNT bookings WHERE `customer_id` AND `status IN (CONFIRMED, CHECKED_IN)` |
| `pendingPayments` | COUNT payments JOIN bookings WHERE `customer_id` AND `status IN (PENDING, PENDING_VERIFICATION)` |
| `openTickets` | COUNT maintenance_tickets WHERE `customer_id` AND `status IN (OPEN, IN_PROGRESS)` |
| `unreadNotifications` | COUNT notifications WHERE `user_id` AND `is_read = false` |

**Rationale**: Spec assumptions; align figma KPI definitions.

## 3. Upcoming Check-in / Check-out Events

**Decision**:

- **upcomingCheckIn**: nearest booking WHERE `status = CONFIRMED` AND `check_in >= today` ORDER BY `check_in ASC` LIMIT 1
- **upcomingCheckOut**: nearest booking WHERE `status = CHECKED_IN` AND `check_out >= today` ORDER BY `check_out ASC` LIMIT 1

DTO: `{ bookingId, roomNumber, propertyName, date, daysUntil }` where `daysUntil = ChronoUnit.DAYS.between(today, date)` in `Asia/Ho_Chi_Minh`.

**Rationale**: Spec US-2; matches `UpcomingEvent` in `customersApi.ts`.

## 4. Upcoming Bookings List

**Decision**: Top **5** bookings WHERE `status IN (PENDING_DEPOSIT, CONFIRMED, CHECKED_IN)` AND `check_out >= today`, ORDER BY `check_in ASC`. Map to `BookingSummaryResponse` fields (reuse FR-04 DTO shape).

**Rationale**: Spec US-3 assumption max 5.

## 5. Recent Lists

**Decision**:

- `recentNotifications`: last **5**, `ORDER BY created_at DESC`
- `recentPayments`: last **3**, `ORDER BY created_at DESC`, scoped via booking → customer

**Rationale**: Spec assumptions; figma SCR-16.

## 6. API Path Normalization

**Decision**: `GET /api/v1/customer/dashboard` (singular `customer` per api-spec SCR-15). Deprecate `/api/customers/dashboard`.

**Rationale**: api-spec-by-screen.md; REST consistency with `/api/v1/customer/**`.

## 7. No New Tables

**Decision**: FR-19 **does not** create dashboard-specific tables or materialized views v1.

**Rationale**: Read-only aggregate; YAGNI.

**Alternatives considered**: `dashboard_snapshots` cache — rejected v1.

## 8. Optional Indexes (V035)

**Decision**: Optional migration indexes if explain plans slow:

- `bookings(customer_id, status, check_in)`
- `payments(booking_id, status, created_at DESC)`
- `notifications(user_id, is_read, created_at DESC)`

**Rationale**: SC-001 p95 < 3s.

## 9. Damage Dispute Banner (P2)

**Decision**: Optional response field `pendingDamageDispute: { damageReportId, amount, disputeDeadline } | null` when FR-23 `DamageReport.status = AWAITING_CUSTOMER_DISPUTE`.

**Rationale**: Spec US-6 P2; FR-23 owns dispute action — dashboard only shows alert + link.

## 10. Frontend Recent Payments Gap

**Decision**: Add **Recent Payments** card section to `CustomerDashboardPage.tsx` below upcoming bookings or in sidebar — 3 items + "Xem tất cả →" `/customer/payments`.

**Rationale**: Spec US-5; DTO already has `recentPayments` but UI not rendered.

## 11. Security

**Decision**: `@PreAuthorize("hasRole('CUSTOMER')")`; resolve `customerId` / `userId` from JWT `sub` — never accept customer id from query param.

**Rationale**: Spec FR-007, FR-008.

## 12. Pending Payment Status Values

**Decision**: Align FR-12 enum: count `PENDING` and `PENDING_VERIFICATION` (bank transfer awaiting staff confirm).

**Rationale**: Customer actionable payments still outstanding.
