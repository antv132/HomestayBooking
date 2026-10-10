# Implementation Plan: FR-04 Booking & Inventory Management

**Branch**: `main` (feature dir `004-booking-inventory`; HAS_GIT=false locally) | **Date**: 2026-07-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `sdd/services/homestay/features/004-booking-inventory/spec.md`

**Note**: Re-plan after Spec Sync clarify session 2026-07-23. Constitution: `.specify/memory/constitution.md` (align with root `CONSTITUTION.md`).

## Summary

Deliver (and align code to) **FR-04 Booking & Inventory**: Customer creates booking with **40%/60%** price snapshot, **half-open** inventory lock `[checkIn, checkOut)`, VNPay deposit via **Return URL** → Confirmed + contract Outbox; hold timeout (10m) and no-show (`checkInDate.atStartOfDay() + 24h`); customer cancel tiers (`≥7` / `3≤d<7` / `<3`); Manager scoped list, check-in (cash remaining at desk allowed), inspection-gated check-out + HK task, modify Confirmed, manager cancel 100%. **Edu:** VNPay-only online (TD-011); no IPN (TD-012b).

**Gap closure (post-clarify):** write-path overlap → half-open; create/modify MUST reject overlapping active `RoomStatusBlock` (MAINTENANCE/OOS).

## Technical Context

**Language/Version**: Java 17; TypeScript 5.x (Frontend)

**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Maven; React 18, Vite, TanStack Query, Zustand, Axios, Tailwind

**Storage**: SQL Server (JPA/Hibernate); uploads on disk (`UPLOAD_DIR`)

**Testing**: JUnit 5 + Mockito; `mvn -Pcoverage-critical verify` for Auth/Booking/Payment; Vitest/ESLint on FE

**Target Platform**: Local/dev Windows + Docker-capable Linux; browser SPA + REST API

**Project Type**: Full-stack web app (`Backend` + `Frontend`)

**Performance Goals**: SC-001 create+VNPay init &lt; 5 min UX; SC-002 0% overbooking under concurrent create; hold cancel within 5 min of deadline (SC-003); contract email within 10 min (SC-004)

**Constraints**: Layered Controller→Service→Repository; JWT Bearer; property-scoped Manager; upload ≤10MB; no secrets in repo; canonical booking statuses only; online pay = VNPay Return URL only

**Scale/Scope**: Edu single-property / multi-property assigned managers; booking screens SCR-15–19, 34–35, 37 check-in cash; ~8 booking statuses; scheduled jobs 1m poll

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*
*Source: `.specify/memory/constitution.md` v1.0.0*

- [x] **I. Spec-First**: `CONTEXT.md` + `spec.md` present; Clarifications Session 2026-07-23 recorded
- [x] **II. Tech Stack**: Java 17 / Spring Boot 3 / SQL Server / React 18+TS+Vite / JWT+BCrypt / REST — no swaps
- [x] **III. Layered Boundaries**: Booking/Payment/Contract services; DTOs + `ApiResponse`; no entity leak
- [x] **IV. Security**: Env secrets; validated requests; Manager property scope; no invented statuses; upload limits
- [x] **V. Tests**: Existing BookingCancelTier / NoShow / Modify / Coverage tests; extend for half-open + RoomStatusBlock
- [x] **Complexity**: None — no Constitution violations

### Post-design re-check

PASS — half-open overlap + RoomStatusBlock gate documented; damage fee remains separate field (clarify); edu deferrals TD-010/011/012b unchanged.

## Project Structure

### Documentation (this feature)

```text
sdd/services/homestay/features/004-booking-inventory/
├── CONTEXT.md
├── spec.md
├── plan.md              # This file
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── booking-api.yaml
├── checklists/
└── tasks.md             # /speckit-tasks — not created by this command
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controller/          # BookingV1Controller, ManagerBookingV1Controller, payments
├── service/             # BookingService, PaymentService, ContractService, HousekeepingTaskService
├── repository/          # BookingRepository, RoomRepository, RoomStatusBlockRepository
├── entity/              # Booking, Payment, Room, RoomStatusBlock, RoomInspection, …
├── dto/
├── job/                 # BookingHoldTimeoutJob, BookingNoShowJob, OutboxProcessorJob
└── security/

Backend/src/test/java/com/homestay/service/
├── BookingCancelTierTest.java
├── BookingNoShowServiceTest.java
├── BookingModifyServiceTest.java
└── BookingServiceCoverageTest.java

Frontend/src/
├── api/bookingApi.ts
├── pages/customer/      # BookingForm, List, Detail, Cancel, Deposit/Remaining/VNPay
└── pages/manager/       # BookingMgmt*, BookingCheckInOut
```

**Structure Decision**: Use existing Homestay `Backend/` + `Frontend/` layout (Constitution II). No new packages.

## Complexity Tracking

> No Constitution violations requiring justification.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |

## Implementation Notes (align-to-clarify)

| Item | Spec decision | Code action |
|------|---------------|-------------|
| Overlap | Half-open `[ci,co)` | Fix `existsOverlapBooking` / `Excluding` / search EXISTS to `checkIn < otherOut AND checkOut > otherIn` |
| Blocks | RoomStatusBlock on create+modify | Call `findActiveOverlappingRange` inside locked create/modify; 409 if any |
| Damage | Separate `damageFeeAmount` | Keep; ensure detail DTO/UI breakdown (already largely done) |
| Cancel day 7 | ≥7 → 100% | Already matches `resolveCustomerCancelRefundPercent` |
| No-show | `atStartOfDay()+24h` | Already matches `markNoShowExpiredConfirmed` |

## Phase 0 / Phase 1 outputs

- [research.md](./research.md) — decisions including clarify 2026-07-23
- [data-model.md](./data-model.md) — half-open + RoomStatusBlock validation
- [contracts/booking-api.yaml](./contracts/booking-api.yaml) — edu VNPay-first contract
- [quickstart.md](./quickstart.md) — smoke paths without deferred CK

**Next command**: `/speckit-tasks` (or implement gap table above if tasks already exist — refresh tasks for align-to-clarify).
