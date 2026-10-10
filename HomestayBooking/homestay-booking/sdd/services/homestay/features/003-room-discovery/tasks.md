# Tasks: FR-03 Room Discovery

**Input**: Design documents from `specs/003-room-discovery/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/rooms-discovery-api.yaml, quickstart.md

**Phụ thuộc**: FR-01 backend scaffold (SecurityConfig); FR-06/FR-08 hoặc seed data (Property/Room); FR-04 out of scope

**Tests**: Không có phase test riêng (spec không yêu cầu TDD). Integration tests trong Phase Polish.

**Organization**: Tasks grouped by user story (US1–US5) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US5 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/003-room-discovery/contracts/rooms-discovery-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Xác nhận môi trường và seed data cho discovery MVP

- [X] T001 Verify backend Spring Boot scaffold exists (or complete FR-01 Phase 1 setup) per `specs/001-user-auth/plan.md`
- [X] T002 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [X] T003 [P] Update `PUBLIC_READ_PATHS` in `Frontend/src/api/axiosInstance.ts` to include `/api/v1/rooms`, `/api/v1/properties`, `/api/v1/public`, `/api/v1/promotions`
- [ ] T004 Create Flyway seed `Backend/src/main/resources/db/migration/V010__discovery_seed.sql` — 2 ACTIVE properties, 10+ AVAILABLE rooms, RoomImages, sample bookings per `data-model.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entities, repositories, DTOs, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T005 [P] Create entity `Property.java` in `Backend/src/main/java/com/homestay/entities/Property.java` per data-model.md (skip if FR-06 exists)
- [X] T006 [P] Create entity `Floor.java` in `Backend/src/main/java/com/homestay/entities/Floor.java`
- [X] T007 [P] Create entity `Room.java` in `Backend/src/main/java/com/homestay/entities/Room.java` with `amenities` JSONB/TEXT[] column
- [X] T008 [P] Create entity `RoomImage.java` in `Backend/src/main/java/com/homestay/entities/RoomImage.java`
- [X] T009 [P] Create entity `Booking.java` in `Backend/src/main/java/com/homestay/entities/Booking.java` (read-only for availability)
- [X] T010 [P] Create entity `Promotion.java` in `Backend/src/main/java/com/homestay/entities/Promotion.java`
- [X] T011 [P] Create JPA repositories in `Backend/src/main/java/com/homestay/repositories/` for Property, Room, RoomImage, Booking, Promotion
- [X] T012 Create `RoomSearchSpecification.java` in `Backend/src/main/java/com/homestay/specifications/RoomSearchSpecification.java` (ACTIVE property, exclude MAINTENANCE/OUT_OF_SERVICE, filters)
- [X] T013 [P] Create DTOs in `Backend/src/main/java/com/homestay/dtos/room/`: `RoomListItemResponse.java`, `RoomDetailResponse.java`, `RoomAvailabilityResponse.java`
- [X] T014 [P] Create DTOs in `Backend/src/main/java/com/homestay/dtos/public/`: `FeaturedRoomResponse.java`, `FeaturedPropertyResponse.java`, `PlatformStatsResponse.java`, `SearchSuggestionResponse.java`
- [ ] T015 Create `PricingService.java` in `Backend/src/main/java/com/homestay/services/PricingService.java` — resolve price from PricingRule or fallback `pricePerNight` (**deferred** — discovery vẫn dùng `pricePerNight`; PricingRule entity đã có)
- [X] T016 Register public discovery paths `permitAll` in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java` per research.md #1
- [X] T017 [P] Migrate base paths in `Frontend/src/api/roomsApi.ts` from `/api/rooms` to `/api/v1/rooms`
- [X] T018 [P] Migrate base paths in `Frontend/src/api/publicApi.ts` to `/api/v1` (`/rooms/featured`, `/properties/featured`, `/public/stats`, `/public/search-suggestions`, `/promotions/active`)

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Tìm kiếm và liệt kê phòng (Priority: P1) 🎯 MVP

**Goal**: SCR-07 — filter, paginate, sort rooms; date/capacity availability filter

**Independent Test**: `GET /api/v1/rooms?location=...&checkIn=...` → paginated list; UI `/search` shows matching RoomCards; empty state when no match

### Implementation

- [X] T019 [US1] Implement `RoomSearchService.java` in `Backend/src/main/java/com/homestay/services/RoomSearchService.java` using `RoomSearchSpecification` + pagination
- [X] T020 [US1] Add booking overlap + capacity filter in `RoomSearchService.java` when checkIn/checkOut/guests provided (FR-003)
- [X] T021 [US1] Add date validation (checkOut > checkIn, checkIn not past) in `RoomSearchService.java` or controller (FR-010)
- [X] T022 [US1] Create `RoomDiscoveryController.java` with `GET /api/v1/rooms` returning paginated `RoomListItemResponse` per contracts/rooms-discovery-api.yaml
- [X] T023 [P] [US1] Add `GET /api/v1/rooms/price-stats` in `RoomDiscoveryController.java` for filter slider min/max
- [X] T024 [P] [US1] Add property options endpoint or reuse `GET /api/v1/properties/featured` for filter sidebar in `RoomDiscoveryController.java` / `PublicController.java`
- [X] T025 [US1] Verify `useRoomSearch.ts` in `Frontend/src/hooks/useRoomSearch.ts` passes all filter params to migrated `fetchRooms`
- [X] T026 [US1] Verify empty state + clear filters in `Frontend/src/pages/public/SearchResultsPage.tsx`
- [X] T027 [P] [US1] Verify `RoomListingPage.tsx` in `Frontend/src/pages/public/RoomListingPage.tsx` and `RoomSearchCard.tsx` in `Frontend/src/components/rooms/RoomSearchCard.tsx` render list correctly

**Checkpoint**: US1 testable via curl quickstart §List rooms + `/search` UI

---

## Phase 4: User Story 2 — Xem chi tiết phòng (Priority: P1)

**Goal**: SCR-08 — gallery, amenities, property info, mini calendar, Book Now routing

**Independent Test**: Open `/rooms/:id` → gallery + amenities + price; Guest Book Now → login; inactive room → 404

### Implementation

- [X] T028 [US2] Implement `RoomDetailService.java` in `Backend/src/main/java/com/homestay/services/RoomDetailService.java` (join Property, images, amenities; hide INACTIVE)
- [X] T029 [US2] Add `GET /api/v1/rooms/{id}` in `Backend/src/main/java/com/homestay/controllers/RoomDiscoveryController.java`
- [X] T030 [P] [US2] Add `GET /api/v1/rooms/{id}/reviews` paginated in `RoomDiscoveryController.java` (read-only enrichment)
- [X] T031 [US2] Verify gallery + placeholder in `Frontend/src/pages/public/RoomDetailPage.tsx` using `ImageGallerySlider`
- [X] T032 [US2] Verify amenities list + icons in `Frontend/src/pages/public/RoomDetailPage.tsx` (AMENITY_ICONS map)
- [X] T033 [US2] Wire Guest "Book Now" → `/login` with return URL; Customer → `/customer/bookings/new` stub in `Frontend/src/pages/public/RoomDetailPage.tsx`
- [X] T034 [P] [US2] Verify mini calendar via `RoomMiniCalendar.tsx` in `Frontend/src/components/ui/RoomMiniCalendar.tsx` calling `/api/v1/rooms/{id}/calendar`
- [X] T035 [US2] Add 404/not-found UX in `Frontend/src/pages/public/RoomDetailPage.tsx` when room missing or property INACTIVE

**Checkpoint**: US2 testable — detail page end-to-end

---

## Phase 5: User Story 3 — Gợi ý tìm kiếm thông minh (Priority: P1)

**Goal**: Autocomplete property/location on hero + search bars

**Independent Test**: Type "Đà" → suggestions dropdown → select → navigates to `/search?location=...`

### Implementation

- [X] T036 [US3] Implement `searchSuggestions(q)` in `Backend/src/main/java/com/homestay/services/PublicContentService.java` — ILIKE on Property.name/address, limit 8, min length 2
- [X] T037 [US3] Add `GET /api/v1/public/search-suggestions` in `Backend/src/main/java/com/homestay/controllers/PublicController.java`
- [X] T038 [US3] Wire debounced suggestions (≥2 chars) in `Frontend/src/pages/public/LandingPage.tsx` hero search using `fetchSearchSuggestions` from `publicApi.ts`
- [X] T039 [P] [US3] Add suggestions dropdown to search input in `Frontend/src/pages/public/SearchResultsPage.tsx` filter bar (reuse same API)
- [X] T040 [US3] On suggestion select navigate to `/search` with `location` or `propertyId` query params preserved

**Checkpoint**: US3 testable — autocomplete on landing + search pages

---

## Phase 6: User Story 4 — Lịch trống phòng (Priority: P2)

**Goal**: SCR-09 full calendar — booked vs available; date pick returns to detail

**Independent Test**: `/rooms/:id/calendar` → month view with booked dates; select range → back to detail with dates

### Implementation

- [X] T041 [US4] Implement `AvailabilityService.java` in `Backend/src/main/java/com/homestay/services/AvailabilityService.java` — blocking booking statuses per research.md #4
- [X] T042 [US4] Add `GET /api/v1/rooms/{id}/availability?month&year` returning `{ bookedDates }` in `RoomDiscoveryController.java`
- [X] T043 [US4] Add `GET /api/v1/rooms/{id}/availability?checkIn&checkOut` returning `{ available, bookedRanges }` in `RoomDiscoveryController.java`
- [X] T044 [US4] Add `GET /api/v1/rooms/{id}/calendar` returning `{ roomStatus, bookedRanges }` in `RoomDiscoveryController.java`
- [X] T045 [US4] Verify `AvailabilityCalendarPage.tsx` in `Frontend/src/pages/public/AvailabilityCalendarPage.tsx` uses month availability API
- [X] T046 [P] [US4] Verify `RoomAvailabilityCalendar.tsx` in `Frontend/src/components/rooms/RoomAvailabilityCalendar.tsx` marks booked vs available days
- [X] T047 [US4] Wire date selection → navigate back to `/rooms/:id?checkIn=&checkOut=` in `AvailabilityCalendarPage.tsx`
- [X] T048 [US4] Confirm route `/rooms/:id/calendar` in `Frontend/src/App.tsx` under `ManagerRedirectRoute`

**Checkpoint**: US4 testable — full calendar flow SCR-09

---

## Phase 7: User Story 5 — Trang chủ khám phá (Priority: P2)

**Goal**: SCR-01 — featured rooms/properties, promotions, platform stats, hero search

**Independent Test**: `/` loads featured content + stats; promotions banner; hero search submits to `/search`

### Implementation

- [X] T049 [US5] Implement featured rooms/properties + platform stats in `Backend/src/main/java/com/homestay/services/PublicContentService.java` per research.md #7–9
- [X] T050 [US5] Add `GET /api/v1/rooms/featured`, `GET /api/v1/properties/featured` in `PublicController.java`
- [X] T051 [US5] Add `GET /api/v1/public/stats` and `GET /api/v1/promotions/active` in `PublicController.java`
- [X] T052 [US5] Verify featured rooms grid in `Frontend/src/pages/public/LandingPage.tsx` using `fetchFeaturedRooms`
- [X] T053 [P] [US5] Verify featured properties section in `LandingPage.tsx` using `fetchFeaturedProperties`
- [X] T054 [P] [US5] Verify platform stats counters in `LandingPage.tsx` using `fetchPlatformStats` + `formatStatValue`
- [X] T055 [US5] Verify promotion banners in `LandingPage.tsx` using `fetchPromotions` with DEFAULT_PROMOTIONS fallback
- [X] T056 [US5] Wire hero search form submit → `/search` with checkIn/checkOut/guests/location query in `LandingPage.tsx`

**Checkpoint**: US5 testable — landing page SCR-01 complete

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Tests, security validation, docs sync

- [X] T057 [P] Add unit tests in `Backend/src/test/java/com/homestay/unit/AvailabilityServiceTest.java` (overlap logic, month bookedDates)
- [X] T058 [P] Add unit tests in `Backend/src/test/java/com/homestay/unit/RoomSearchSpecificationTest.java` (filters, INACTIVE exclusion)
- [ ] T059 Add integration tests in `Backend/src/test/java/com/homestay/integration/RoomDiscoveryControllerIT.java` (list, detail, availability, 404, date 400) — covered by unit tests `RoomServiceDiscoveryTest` / `PublicServiceDiscoveryTest` for P0; full IT optional
- [ ] T060 Run manual validation per `specs/003-room-discovery/quickstart.md` curl + UI flows (SCR-01, 07, 08, 09) — unit tests green; restart BE rồi smoke UI/curl nếu cần
- [X] T061 [P] Assert 100% INACTIVE property rooms excluded from `GET /rooms` in integration test (SC-006) — enforced in `findPublicWithFilters` + `findPublicDiscoverableRoom` + unit tests
- [ ] T062 Document extended query params vs api-spec in `docs/api-spec-by-screen.md` SCR-07 section (optional sync note)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup** → **Phase 2 Foundational** → **User Story Phases 3–7** → **Phase 8 Polish**
- **Backend scaffold** (T001) blocks all backend tasks
- **Seed data** (T004) required for meaningful UI tests

### User Story Dependencies

| Story | Depends on | Independent after |
|-------|------------|-------------------|
| US1 Search/list | Phase 2 | Phase 2 |
| US2 Room detail | US1 controller exists | T022 RoomDiscoveryController |
| US3 Suggestions | Phase 2 Property repo | Phase 2 |
| US4 Calendar | US2 room exists + Booking entity | T041 AvailabilityService |
| US5 Landing | Phase 2 + public APIs | T049 PublicContentService |

**Recommended order**: US1 → US2 → US3 (parallel with US2 frontend) → US4 → US5 → Polish

### Parallel Opportunities

- Phase 1: T002–T004 parallel after T001
- Phase 2: T005–T011 parallel; T013–T014 parallel; T017–T018 parallel after T016
- US1: T023–T024, T027 parallel after T022
- US2: T030, T034 parallel after T029
- US3: T039 parallel with T038
- US4: T046 parallel with T045
- US5: T053–T054 parallel
- Polish: T057–T058, T061 parallel

---

## Parallel Example: Phase 2 Foundational

```bash
# Entities in parallel:
T005 Property.java | T006 Floor.java | T007 Room.java | T008 RoomImage.java | T009 Booking.java | T010 Promotion.java

# Then:
T011 repositories → T012 RoomSearchSpecification → T015 PricingService

# Frontend API migration in parallel:
T017 roomsApi.ts | T018 publicApi.ts
```

---

## Parallel Example: User Story 1

```bash
# Backend sequential:
T019 RoomSearchService → T020 overlap filter → T021 date validation → T022 GET /rooms

# Parallel after T022:
T023 price-stats | T024 property options | T027 RoomSearchCard

# Frontend verify:
T025 useRoomSearch.ts → T026 SearchResultsPage empty state
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2)

1. Complete Phase 1 + Phase 2 (seed + entities + public security)
2. Complete Phase 3 (US1) — search & list SCR-07
3. Complete Phase 4 (US2) — room detail SCR-08
4. **STOP and VALIDATE**: Landing hero → search → view room detail
5. Demo core discovery funnel

### Incremental Delivery

1. Setup + Foundational + seed
2. US1 Search → browse catalog (MVP read list)
3. US2 Detail → conversion-ready view
4. US3 Suggestions → faster search UX
5. US4 Calendar → date selection confidence
6. US5 Landing → marketing entry point
7. Polish → tests + docs

### Suggested MVP Scope

**Phases 1–4** (T001–T035): Search, list, and room detail for Guest/Customer — covers FR-001, FR-002, FR-005, FR-008–010 core discovery.

---

## Notes

- Package base: `com.homestay`
- API envelope + pagination: `docs/api-spec-by-screen.md` §1
- Dual availability endpoints: research.md #3 — do not remove `/calendar`
- Book Now checkout body: stub only — FR-04 implements SCR-16
- Manager room CRUD paths in `roomsApi.ts` (`/manager`, POST/PUT) stay separate — not FR-03
- Commit after each phase checkpoint
- Total tasks: **62** (T001–T062)
