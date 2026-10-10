# Research: FR-05 Availability Calendar

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `docs/Specification_v2.md` FR-05, `docs/api-spec-by-screen.md` SCR-09/33, FR-03 `research.md` #3–4, frontend `AvailabilityCalendarPage.tsx`, `RoomStatusPage.tsx`

## 1. Calendar Data Model (Derived vs Persisted)

**Decision**: **Derived** `CalendarDayStatus` computed at query time; persist only **manual blocks** in `room_status_blocks`. Booking and housekeeping states read from FR-04/FR-21 tables.

**Rationale**: Single source of truth; avoids sync drift between calendar table and booking lifecycle.

**Alternatives considered**: Persist snapshot per day — rejected (high write volume, stale data).

## 2. Manual Status Blocks Table

**Decision**: New entity `RoomStatusBlock`: `{ roomId, status: MAINTENANCE | OUT_OF_SERVICE, startDate, endDate, reason, createdBy }`. Multiple non-overlapping blocks per room allowed; reject overlap with active bookings on create.

**Rationale**: FR-05 requires date-range maintenance on SCR-33; `Room.status` alone insufficient for per-day calendar.

**Alternatives considered**: Only update `Room.status` globally — rejected (no date-range calendar display).

## 3. Status Priority Resolver

**Decision**: `CalendarStatusResolver.resolve(sources)` applies spec priority: Occupied > Pending Deposit > Reserved > Cleaning In Progress > Pending Cleaning > Maintenance > Out Of Service > Available.

**Rationale**: spec Assumptions + US-5; testable pure function.

**Alternatives considered**: Layer painting (multiple statuses per day) — rejected (spec: one status per day).

## 4. Booking → Calendar Status Mapping

**Decision**:

| BookingStatus | Calendar day in [checkIn, checkOut) |
|---------------|-------------------------------------|
| PENDING_DEPOSIT | Pending Deposit |
| CONFIRMED | Reserved |
| CHECKED_IN, PENDING_INSPECTION, PENDING_DAMAGE_PAYMENT | Occupied |
| Terminal (CANCELLED, CHECKED_OUT, NO_SHOW) | No contribution |

**Rationale**: spec Assumptions; aligns with entity-ui-mapping §2.2.

**Alternatives considered**: CONFIRMED as Occupied — rejected (Reserved pre check-in).

## 5. Housekeeping → Calendar Status

**Decision**: If `Room.status` is PENDING_CLEANING or CLEANING_IN_PROGRESS, apply to **today** (property timezone) or from checkout date until HK completed. Prefer room operational status from FR-21 automation over Available default.

**Rationale**: FR-09; post-checkout flow in FR-04 plan.

**Alternatives considered**: HK only on checkout day — rejected (cleaning spans days).

## 6. Public API (SCR-09) — Enriched + Backward Compatible

**Decision**: `GET /api/v1/rooms/{id}/availability?month=&year=` returns:

```json
{
  "days": [{ "date": "2026-06-15", "status": "RESERVED", "bookable": false }],
  "bookedDates": ["2026-06-15", "2026-06-16"],
  "roomStatus": "AVAILABLE"
}
```

`bookedDates` = all days where `bookable === false` (FR-03 compat). Keep `GET .../availability?checkIn&checkOut` for range check (FR-03).

**Rationale**: spec Assumption backward compat; FR-03 frontend can migrate gradually.

**Alternatives considered**: Break FR-03 API — rejected.

## 7. Manager Calendar Endpoint

**Decision**: `GET /api/v1/manager/rooms/{id}/calendar?month&year=` — same `days[]` plus optional `bookingId`, `blockId` for tooltip; requires `@PropertyAccess`.

**Rationale**: US-2 manager detail; guest must not see booking ids (FR-002 privacy).

**Alternatives considered**: Same endpoint with role-based fields — acceptable fallback if one controller with `@JsonView`.

## 8. Manual Status Update (SCR-33)

**Decision**: `PATCH /api/v1/manager/rooms/{id}/status` body:

```json
{
  "status": "MAINTENANCE",
  "startDate": "2026-07-01",
  "endDate": "2026-07-05",
  "reason": "AC repair"
}
```

- `MAINTENANCE` / `OUT_OF_SERVICE` → create `RoomStatusBlock`, set `Room.status` if range includes today.
- `AVAILABLE` → only if no incomplete HK; clear active block if applicable; validate no future booking conflict.

**Rationale**: api-spec SCR-33 minimal `{status}` extended per screendesign date picker + textarea.

**Alternatives considered**: Separate POST `/blocks` — deferred (single PATCH simpler for SCR-33 page).

## 9. Housekeeping Gate on Available

**Decision**: `HousekeepingGateService.canSetAvailable(roomId)` queries latest open `HousekeepingTask` for room — reject if status ≠ COMPLETED.

**Rationale**: FR-006 spec; FR-21 rule "Manager cannot bypass HK".

**Alternatives considered**: Trust manager override — rejected.

## 10. Employee Read-Only Access

**Decision**: Reuse manager calendar GET with `@PreAuthorize EMPLOYEE` + property assignment; separate write denied on PATCH.

**Rationale**: US-4; minimal new surface.

**Alternatives considered**: Duplicate employee endpoint — rejected (YAGNI).

## 11. Real-Time Updates (FR-15 Deferred)

**Decision**: Frontend `refetchInterval: 30000` on calendar queries until WebSocket FR-15; document in quickstart.

**Rationale**: spec FR-012 ≤30s without requiring FR-15.

**Alternatives considered**: Manual refresh only — weaker SC-006.

## 12. Timezone & Date Range

**Decision**: All calendar boundaries use `ZoneId.of("Asia/Ho_Chi_Minh")`; stay nights half-open `[checkIn, checkOut)`.

**Rationale**: Consistent with FR-03/04 research; spec Edge Cases.

**Alternatives considered**: UTC — rejected for VN property ops.

## 13. Integration with FR-03 AvailabilityService

**Decision**: Refactor FR-03 `AvailabilityService` to call `CalendarStatusService.isBookable(roomId, checkIn, checkOut)` — all days in range must be `bookable`.

**Rationale**: Avoid duplicate booking overlap logic.

**Alternatives considered**: Duplicate queries — rejected.

## 14. Frontend Component Strategy

**Decision**: Extract shared `<RoomAvailabilityCalendar />` accepting `days: CalendarDay[]`; map status → CSS tokens in `utils/roomCalendar.ts` (8 colors). Update `AvailabilityCalendarPage` and optionally manager room detail.

**Rationale**: component-library §3.8; DRY for SCR-09 + manager view.

**Alternatives considered**: Inline styles only in page — current state, refactor in implementation.
