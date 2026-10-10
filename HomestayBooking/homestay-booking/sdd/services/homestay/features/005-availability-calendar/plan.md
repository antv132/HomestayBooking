# Implementation Plan: FR-05 Availability Calendar

**Branch**: `007-availability-calendar` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/005-availability-calendar/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-05, §5 Room status), `docs/api-spec-by-screen.md` (SCR-09, SCR-33), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md`, `docs/component-library.md` §3.8, frontend `AvailabilityCalendarPage.tsx`, `RoomStatusPage.tsx`, `roomsApi.ts`

**Phụ thuộc**: FR-04 (booking statuses on calendar); FR-21 (housekeeping → Pending Cleaning / Cleaning In Progress); FR-06/08 (Room/Property); FR-03 (SCR-09 UI + base availability endpoint). **Ranh giới**: WebSocket push (FR-15); FR-03 discovery search filter consumes bookable flag — không duplicate list logic.

## Summary

Triển khai **FR-05 Availability Calendar**: engine tính **trạng thái theo ngày** (8 values) từ booking + housekeeping + manual blocks; API mở rộng SCR-09; Manager/Admin cập nhật Maintenance/Out Of Service (SCR-33) với date range + housekeeping gate; Employee/Manager read-only/enriched views. Stack: **Spring Boot 3 + JPA + SQL Server** + **React/TypeScript** (SCR-09, SCR-33 đã có — enrich 8-status legend + API alignment `/api/v1`).

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA; Vite, Axios, React Router, React Query (optional 30s refetch)  
**Storage**: SQL Server — `rooms`, `room_status_blocks` (new), read `bookings`, `housekeeping_tasks`  
**Testing**: JUnit 5 + Mockito; `CalendarStatusResolverTest` priority matrix; `@SpringBootTest` calendar IT with seeded bookings/blocks  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Month calendar p95 < 500ms; SC-001 100% day accuracy in test matrix  
**Constraints**: Asia/Ho_Chi_Minh; half-open [checkIn, checkOut); property RBAC; envelope chuẩn; backward compat `bookedDates[]` on public API  
**Scale/Scope**: ~6 REST endpoints (extend 2, add 2); 5 user stories; SCR-09, SCR-33

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | CalendarController → CalendarStatusService → repositories |
| DTO + Bean Validation | PASS | Status block date range validation |
| Security-first (RBAC, property isolation) | PASS | Manager/Employee property scope |
| No secrets in code | PASS | N/A |
| Test coverage ≥80% | PASS | Priority resolver unit + calendar IT |
| Standard API envelope | PASS | api-spec §1 |
| Audit log ROOM_STATUS_CHANGED | PASS | Manual block create/clear |

**Post-design re-check**: PASS — derived calendar (no duplicate booking writes); FR-04/21 remain source of truth for automated transitions.

## Project Structure

### Documentation (this feature)

```text
specs/005-availability-calendar/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/availability-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── RoomCalendarController.java       # GET public + manager calendar
│   └── ManagerRoomStatusController.java  # PATCH status + blocks (SCR-33)
├── dtos/calendar/
│   ├── CalendarDayResponse.java
│   ├── RoomCalendarResponse.java
│   ├── UpdateRoomStatusRequest.java
│   └── RoomStatusBlockResponse.java
├── entities/RoomStatusBlock.java
├── enums/RoomCalendarStatus.java         # 8 display statuses
├── enums/RoomOperationalStatus.java      # aligns with Room.status
├── repositories/RoomStatusBlockRepository.java
├── services/
│   ├── CalendarStatusService.java        # compute month days
│   ├── CalendarStatusResolver.java       # priority merge per day
│   ├── RoomStatusBlockService.java       # manual maintenance/OOS
│   └── HousekeepingGateService.java      # block Available if HK incomplete
└── configs/SecurityConfig.java           # permitAll public calendar read

Backend/src/test/java/com/homestay/
├── unit/CalendarStatusResolverTest.java
└── integration/RoomCalendarControllerIT.java

Frontend/src/
├── api/roomsApi.ts                       # migrate calendar/availability/status → /api/v1
├── components/rooms/RoomAvailabilityCalendar.tsx  # shared 8-status grid (extract/refactor)
├── pages/public/AvailabilityCalendarPage.tsx      # SCR-09 enrich legend
├── pages/manager/RoomStatusPage.tsx               # SCR-33 date range + OOS
└── utils/roomCalendar.ts                          # status colors/labels
```

**Structure Decision**: Core work is **CalendarStatusService** + **room_status_blocks** table; frontend mostly enriches existing SCR-09/SCR-33 pages.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Flyway `room_status_blocks`; `RoomCalendarStatus` enum | data-model.md |
| **B** | `CalendarStatusResolver` + priority table | US-5, FR-010 |
| **C** | `CalendarStatusService` — aggregate bookings, HK, blocks, room.status | US-1, FR-008–009 |
| **D** | GET `/api/v1/rooms/{id}/availability?month&year` enriched + `bookedDates` compat | US-1, SCR-09 |
| **E** | GET `/api/v1/manager/rooms/{id}/calendar` with booking refs | US-2, FR-003 |
| **F** | `RoomStatusBlockService` + PATCH status with date range | US-3, FR-005–007 |
| **G** | `HousekeepingGateService` on manual Available | US-3, FR-006 |
| **H** | Employee read-only route + RBAC | US-4, FR-011 |
| **I** | Frontend 8-status legend + SCR-33 date picker | US-1–3 |
| **J** | React Query refetch 30s (SC-012 stub until FR-15) | FR-012 |
| **K** | Tests + quickstart | SC-001–SC-006 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| Calendar API | `GET /api/rooms/{id}/calendar` → bookedRanges only | `GET /api/v1/rooms/{id}/availability?month&year` → `days[{date,status,bookable}]` |
| Status display | occupied/pending/available (3 colors) | 8 statuses + legend per spec |
| Manager status | `PATCH /api/rooms/{id}/status` AVAILABLE/MAINTENANCE only | `/api/v1/manager/rooms/{id}/status` + startDate, endDate, reason, OUT_OF_SERVICE |
| HK gate | Not enforced in UI | Show error when Available rejected |
| Path prefix | `/api/rooms` | `/api/v1/rooms`, `/api/v1/manager/rooms` |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-04/21 not implemented | Seed bookings + housekeeping in Flyway for calendar tests |
| FR-03 AvailabilityService overlap | Refactor FR-03 to delegate to `CalendarStatusService` or share resolver |
| api-spec minimal SCR-09 response | Extend contract; keep `bookedDates` derived from non-bookable days |
| RoomStatusPage only 2 manual statuses | Add OUT_OF_SERVICE + date range fields per SCR-33 wireframe |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/availability-api.yaml](./contracts/availability-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
