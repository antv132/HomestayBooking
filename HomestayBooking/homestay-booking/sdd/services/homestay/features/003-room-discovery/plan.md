# Implementation Plan: FR-03 Room Discovery

**Branch**: `004-room-discovery` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/003-room-discovery/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-03, §2 Guest/Customer, §5 Property/Room/RoomImage/Booking/PricingRule, §10), `docs/api-spec-by-screen.md` (SCR-01, SCR-07–09), `docs/screen.md`, `docs/screendesign.md`, `AGENTS.md`

**Phụ thuộc**: FR-06/FR-08 (Property/Room data) hoặc seed data; FR-01 optional (Guest browse); FR-04 out of scope (Book Now → login/checkout stub)

## Summary

Triển khai module **FR-03 Room Discovery** cho Guest/Customer: tìm kiếm & liệt kê phòng (SCR-07), chi tiết phòng + gallery/amenities (SCR-08), gợi ý search, lịch trống read-only (SCR-09), trang chủ featured + stats + promotions (SCR-01). Stack: **React + TypeScript** (pages/components **đã có** — align `/api/v1/*`) + **Spring Boot** (RoomDiscoveryController, PublicController, RoomSearchService, AvailabilityService). **Out of scope**: booking (FR-04), manager room CRUD (FR-08), calendar status update (FR-05).

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, JPA Specifications; Vite, Axios, React Router, React Query (optional cache)  
**Storage**: SQL Server — `properties`, `floors`, `rooms`, `room_images`, `bookings`, `pricing_rules`, `promotions` (read-heavy)  
**Testing**: JUnit 5 + Mockito; `@SpringBootTest` integration; Vitest + RTL for search hooks  
**Target Platform**: Web (public portal responsive)  
**Project Type**: Web application (`Frontend/` + `Backend/`)  
**Performance Goals**: Search p95 < 500ms; SC-001/002 (95% < 3s/2s user-facing); pagination required  
**Constraints**: Public read `permitAll`; chỉ ACTIVE property; ẩn Maintenance/Out Of Service; envelope chuẩn api-spec §1  
**Scale/Scope**: ~10 public endpoints; 5 user stories; SCR-01, 07, 08, 09

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture (Controller → Service → Repository) | PASS | AGENTS.md |
| DTO + Bean Validation | PASS | Date range validation FR-010 |
| Security-first (public read only, no write) | PASS | Guest/Customer read paths |
| No secrets in code | PASS | N/A |
| Test coverage ≥80% | PASS | Search + availability integration |
| Standard API envelope + pagination | PASS | api-spec-by-screen.md §1 |
| Pagination on list endpoints | PASS | GET /rooms paginated |

**Post-design re-check**: PASS — JPA Specifications for filters; no booking writes in FR-03.

## Project Structure

### Documentation (this feature)

```text
specs/003-room-discovery/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/rooms-discovery-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── RoomDiscoveryController.java    # GET /api/v1/rooms, /rooms/{id}, /availability
│   └── PublicController.java           # featured, stats, suggestions, promotions
├── dtos/room/
│   ├── RoomListItemResponse.java
│   ├── RoomDetailResponse.java
│   ├── RoomAvailabilityResponse.java
│   └── RoomSearchRequest.java          # query param binding
├── dtos/public/
│   ├── FeaturedRoomResponse.java
│   ├── PlatformStatsResponse.java
│   └── SearchSuggestionResponse.java
├── entities/ Property, Floor, Room, RoomImage, Booking, PricingRule, Promotion
├── repositories/ + specifications/RoomSearchSpecification.java
├── services/
│   ├── RoomSearchService.java
│   ├── RoomDetailService.java
│   ├── AvailabilityService.java
│   ├── PricingService.java             # resolve price for date range
│   └── PublicContentService.java
└── configs/SecurityConfig.java         # permitAll public read paths

Backend/src/test/java/com/homestay/
├── unit/AvailabilityServiceTest.java
├── unit/RoomSearchSpecificationTest.java
└── integration/RoomDiscoveryControllerIT.java

Frontend/src/
├── api/roomsApi.ts           # migrate → /api/v1/rooms/*
├── api/publicApi.ts          # migrate → /api/v1/public/*, /properties/featured
├── hooks/useRoomSearch.ts    # exists — align params
├── pages/public/
│   ├── LandingPage.tsx       # SCR-01 (exists)
│   ├── RoomListingPage.tsx   # SCR-07 (exists)
│   ├── SearchResultsPage.tsx # SCR-07 (exists)
│   ├── RoomDetailPage.tsx    # SCR-08 (exists)
│   └── AvailabilityCalendarPage.tsx  # SCR-09 (verify/create)
├── components/rooms/RoomSearchCard.tsx
├── components/ui/RoomMiniCalendar.tsx
└── components/rooms/RoomAvailabilityCalendar.tsx
```

**Structure Decision**: Frontend discovery UI **largely implemented**; backend scaffold + public APIs are primary work. Reuse FR-01/06/08 entity definitions when available.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Entities + seed/migrations (Property, Room, RoomImage, Booking read) | data-model.md |
| **B** | RoomSearchService + Specification filters | US-1, FR-001–003, FR-008–010 |
| **C** | GET /rooms, /rooms/{id} controllers | US-1, US-2, contracts |
| **D** | AvailabilityService + GET /availability + /calendar | US-2, US-4, SCR-09 |
| **E** | PublicController (featured, stats, suggestions, promotions) | US-3, US-5, SCR-01 |
| **F** | PricingService (PricingRule for search dates) | spec Assumptions |
| **G** | Frontend path migration + SCR-09 page wiring | all US |
| **H** | Tests + quickstart validation | SC-001–006 |

## Frontend Gap Analysis (hiện trạng)

| Item | Hiện tại | Target (docs + contracts) |
|------|----------|---------------------------|
| Room list API | `GET /api/rooms` | `GET /api/v1/rooms` + full filter params |
| Room detail | `GET /api/rooms/{id}` | `GET /api/v1/rooms/{id}` |
| Calendar | `GET /api/rooms/{id}/calendar` | Keep + add `GET /api/v1/rooms/{id}/availability?month&year` (SCR-09) |
| Date availability | `GET .../availability?checkIn&checkOut` | Same under `/api/v1` |
| Featured | `/api/rooms/featured`, `/api/properties/featured` | `/api/v1/rooms/featured`, `/api/v1/properties/featured` |
| Public | `/api/public/stats`, `/api/public/search-suggestions` | `/api/v1/public/stats`, `/api/v1/public/search-suggestions` |
| Promotions | `/api/public/promotions` | `/api/v1/promotions/active` (api-spec SCR-01) |
| SCR-09 page | Component exists (`RoomAvailabilityCalendar`) | Dedicated route `/rooms/:id/calendar` |

## Risks

| Risk | Mitigation |
|------|------------|
| Backend chưa có | Scaffold with FR-06/08 entities or minimal seed for discovery MVP |
| api-spec minimal query params | Extend contract to match frontend `useRoomSearch` filters |
| `/calendar` vs `/availability` dual endpoints | Both supported — research.md #3 |
| No room data without FR-08 | Flyway seed script `V0xx__discovery_seed.sql` for dev/demo |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/rooms-discovery-api.yaml](./contracts/rooms-discovery-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
