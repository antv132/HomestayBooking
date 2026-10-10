# Research: FR-04 Booking & Inventory Management

**Date**: 2026-06-27 · **Updated**: 2026-07-23 (Spec Sync clarify + edu cuts)  
**Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

## 1. Inventory Locking (Zero Overbooking)

**Decision**: SQL Server + JPA: pessimistic write lock on `Room` (`findByIdForUpdate`), then overlap query on active bookings using **half-open** `[checkIn, checkOut)`:

```text
requested.checkIn < other.checkOut AND requested.checkOut > other.checkIn
```

Exclude terminal statuses: `CANCELLED`, `CHECKED_OUT`, `NO_SHOW`. Same transaction as insert. Also reject if `RoomStatusBlockRepository.existsActiveOverlappingStay(roomId, checkIn, checkOut)`.

**Rationale**: Spec clarify 2026-07-23 (align FR-05 calendar); same-day turnover allowed; SC-002.

**Alternatives considered**: Inclusive `[checkIn, checkOut]` — rejected (blocks same-day turnover; diverges from calendar). PostgreSQL `EXCLUDE` — rejected (SQL Server). Separate lock table — unnecessary inside room-row lock.

## 2. Hold Timeout Duration

**Decision**: Default **10 minutes** (`app.booking.hold-timeout-minutes` / `BOOKING_HOLD_TIMEOUT_MINUTES`). Job every ~60s cancels unpaid `PENDING_DEPOSIT` with `holdExpiresAt < now`.

**Rationale**: Spec Assumptions; Spec Sync 2026-07-23 (not 15m hold).

**Alternatives considered**: 15-minute hold — rejected (confusion with FR-12 reconcile interval).

## 3. Pricing Snapshot

**Decision**: On create: `totalAmount = pricePerNight * nights`, `depositAmount = total * 0.40`, `remainingAmount = total - deposit`. Persist snapshot; Manager modify recalculates for new dates/room only. **Do not** fold damage into `remainingAmount`.

**Rationale**: FR-003; clarify damage fee separate.

**Alternatives considered**: Live price on read — rejected.

## 4. Booking State Machine

**Decision**: Canonical enum (AGENTS.md / spec FR-004). Transitions in `BookingService` / payment side-effects (illegal → 409/BusinessException).

```text
→ PENDING_DEPOSIT [create]
PENDING_DEPOSIT → CONFIRMED [VNPay deposit Return URL PAID]
PENDING_DEPOSIT → CANCELLED [hold timeout | customer cancel]
CONFIRMED → CHECKED_IN [manager check-in + remaining paid or remainingCollected cash]
CONFIRMED → CANCELLED [customer/manager cancel]
CONFIRMED → NO_SHOW [job: now > checkInDate.atStartOfDay()+N hours]
CHECKED_IN → PENDING_INSPECTION [checkout step 1]
PENDING_INSPECTION → CHECKED_OUT [inspection PASSED or damage PAID]
PENDING_INSPECTION → PENDING_DAMAGE_PAYMENT [damage approved unpaid]
PENDING_DAMAGE_PAYMENT → PENDING_INSPECTION / CHECKED_OUT [damage PAID then finish]
```

**Rationale**: Spec + implemented flow.

## 5. Deposit Confirmation Paths (Edu)

**Decision**: **VNPay Return URL only** → Payment DEPOSIT PAID → booking CONFIRMED + Outbox contract. ~~IPN~~ TD-012b. ~~Bank transfer + receipt + Manager verify~~ TD-011 out of edu scope. **Exception kept:** cash remaining at check-in desk (`remainingCollected` → Payment CASH PAID).

**Rationale**: Spec FR-005/017 Assumptions 2026-07-23.

**Alternatives considered**: Dual deposit methods — deferred edu.

## 6. Contract Generation Trigger (Outbox)

**Decision**: On CONFIRMED, enqueue Outbox `CONTRACT_GENERATE` (or equivalent); `OutboxProcessorJob` generates/sends. No sync PDF on request thread.

**Rationale**: FR-006; TD-006 Done pattern.

**Alternatives considered**: Sync PDF — rejected (latency/coupling).

## 7. Cancellation Refund Tiers

**Decision**: Calendar days until `checkInDate`: `days >= 7 → 100%`, `3 <= days < 7 → 50%`, `days < 3 → 0%` of **paid deposit**. Preview endpoint before confirm. PENDING_DEPOSIT unpaid → 0%.

**Rationale**: Spec clarify day-7 = 100% (FR-010).

**Alternatives considered**: Day 7 = 50% — rejected. Refund on total — rejected.

## 8. Manager Cancel vs Customer Cancel

**Decision**: Manager cancel always **100%** deposit refund + required reason. Customer uses tiered policy.

**Rationale**: FR-016.

## 9. Modify Confirmed Booking

**Decision**: Manager-only when CONFIRMED; re-check half-open overlap **excluding self** + RoomStatusBlock; recalculate snapshot; return price delta message. Block Checked-in/Cancelled/No-show.

**Rationale**: FR-015 + clarify.

## 10. Check-in / Check-out Gates

**Decision**:
- Check-in: CONFIRMED; not before check-in date; ID docs + key handover; remaining must be PAID or `remainingCollected=true` (cash desk).
- Check-out: inspection gate FR-013; damage via separate `DAMAGE_FEE` payment PAID; then PENDING_CLEANING + HousekeepingTask.

**Rationale**: FR-012–014; clarify B on damage.

## 11. No-show Scheduled Job

**Decision**: Poll ~60s; CONFIRMED where `now > checkInDate.atStartOfDay().plusHours(no-show-after-hours)` (default 24) → NO_SHOW; deposit kept; inventory released (status excluded from overlap).

**Rationale**: Spec clarify 2026-07-23; no `expectedCheckInTime` column.

**Alternatives considered**: End-of-day only; schema time field — rejected for edu.

## 12. API Path Standardization

**Decision**: Prefer `/api/v1/bookings/**` (customer) and `/api/v1/manager/bookings/**`. Legacy check-in/out blocked (TD-004).

**Rationale**: AGENTS.md; FR-011/012.

## 13. Security & RBAC

**Decision**: Customer own-booking only; Manager property-scoped via assignment validator; jobs internal.

**Rationale**: Constitution IV.

## 14. RoomStatusBlock Gate

**Decision**: Create + Manager modify MUST call `existsActiveOverlappingStay` (block inclusive nights vs stay half-open). Overlap → 409 Conflict. Legacy `Room.status` MAINTENANCE/OOS/cleaning still blocked as defense in depth.

**Rationale**: Spec clarify Q2; FR-002/015; US1#5.

**Alternatives considered**: Calendar-only blocks — rejected (write hole).

## 15. Frontend Integration

**Decision**: Existing pages; VNPay deposit/remaining/damage; cancel preview tiers; manager modify/check-in-out. No SCR-20 receipt UX unless TD-011 reopened.

**Rationale**: Edu scope + current FE.

## 16. Damage Fee Accounting

**Decision**: Persist `damageFeeAmount`; payment type `DAMAGE_FEE`; do **not** mutate `remainingAmount` (60% stay). UI/API show breakdown; checkout waits for PAID.

**Rationale**: Spec clarify Q3.
