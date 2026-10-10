# Tasks: FR-14 Review & Rating

**Input**: Design documents from `specs/014-review-rating/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/review-api.yaml, quickstart.md

**Phụ thuộc**: FR-04 (booking CHECKED_OUT gate, `hasReview` on detail); FR-08 (Room + `average_rating`/`total_reviews`); FR-02 (reviewer display name); FR-06 (Manager property scope); FR-03 (discovery displays public reviews — FR-14 owns data). **Ranh giới**: FR-03 discovery UI; FR-14 Review CRUD + moderation + aggregates; Complaint out of scope.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase K.

**Organization**: Tasks grouped by user story (US1–US6) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US6 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/014-review-rating/contracts/review-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Reviews schema + room aggregate columns — after FR-04 bookings exist

- [ ] T001 Verify FR-04 `bookings` table + **CHECKED_OUT** status available per `specs/004-booking-inventory/quickstart.md` (blocker)
- [ ] T002 Create Flyway `Backend/src/main/resources/db/migration/V030__reviews_fr14.sql` — `reviews` table, `UNIQUE(booking_id)`, indexes per `data-model.md`
- [ ] T003 Add `average_rating DECIMAL(3,2)` and `total_reviews INTEGER DEFAULT 0` to `rooms` in `V030__reviews_fr14.sql` (IF NOT EXISTS)
- [ ] T004 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [ ] T005 [P] Add optional seed (1 CHECKED_OUT booking + 1 PUBLISHED review) in `V030__reviews_fr14.sql` for local demo

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Review entity, DTOs, aggregate service skeleton, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [ ] T006 [P] Create `ReviewStatus.java` in `Backend/src/main/java/com/homestay/enums/ReviewStatus.java` — PUBLISHED, HIDDEN
- [ ] T007 [P] Create `Review.java` entity in `Backend/src/main/java/com/homestay/entities/Review.java` per `data-model.md`
- [ ] T008 [P] Create `ReviewRepository.java` in `Backend/src/main/java/com/homestay/repositories/ReviewRepository.java` — `findByCustomerId`, `findByBookingId`, `findByRoomIdAndStatus`, `existsByBookingId`, property-scoped queries
- [ ] T009 [P] Create review DTOs in `Backend/src/main/java/com/homestay/dtos/review/` — `ReviewSummaryResponse`, `ReviewDetailResponse`, `ReviewPageResponse`, `PublicReviewResponse`, `CreateReviewRequest`, `UpdateReviewRequest`, `ModerateReviewRequest` per `contracts/review-api.yaml`
- [ ] T010 Create `ReviewBookingValidator.java` skeleton in `Backend/src/main/java/com/homestay/services/ReviewBookingValidator.java`
- [ ] T011 Create `RoomRatingAggregateService.java` skeleton in `Backend/src/main/java/com/homestay/services/RoomRatingAggregateService.java`
- [ ] T012 Create `ReviewService.java` skeleton in `Backend/src/main/java/com/homestay/services/ReviewService.java`
- [ ] T013 Create `ReviewQueryService.java` skeleton in `Backend/src/main/java/com/homestay/services/ReviewQueryService.java`
- [ ] T014 Create `ReviewModerationService.java` skeleton in `Backend/src/main/java/com/homestay/services/ReviewModerationService.java`
- [ ] T015 Register review routes + `permitAll` `GET /api/v1/rooms/*/reviews` in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Customer đánh giá phòng sau check-out (Priority: P1) 🎯 MVP

**Goal**: Customer submits rating 1–5 + comment for CHECKED_OUT booking; one per booking; status PUBLISHED; SCR-25

**Independent Test**: CHECKED_OUT booking → POST review → appears in GET /reviews/me; duplicate booking → 409; non-CHECKED_OUT → 400

### Implementation

- [ ] T016 [US1] Implement `assertEligibleForReview(bookingId, customerId)` — CHECKED_OUT + ownership + no existing review in `ReviewBookingValidator.java`
- [ ] T017 [US1] Implement `createReview(customerId, request)` — denorm roomId/propertyId, status PUBLISHED in `ReviewService.java`
- [ ] T018 [US1] Call `RoomRatingAggregateService.recalculate(roomId)` after create in `ReviewService.java`
- [ ] T019 [US1] Implement `listForCustomer(customerId, pageable)` in `ReviewService.java`
- [ ] T020 [US1] Create `CustomerReviewController.java` with `POST /api/v1/reviews` and `GET /api/v1/reviews/me` in `Backend/src/main/java/com/homestay/controllers/CustomerReviewController.java`
- [x] T021 [US1] Add Bean Validation on `CreateReviewRequest` — rating 1–5, comment 20–1000 chars in `CreateReviewRequest.java`
- [x] T022 [US1] Log `REVIEW_CREATED` to ActivityLog in `ReviewService.createReview()` in `ReviewService.java`
- [ ] T023 [P] [US1] Migrate `createReview` and `getMyReviews` to `/api/v1/reviews` and `/me` in `Frontend/src/api/reviewApi.ts`
- [ ] T024 [US1] Remove mock/localStorage submit path; wire live API on `Frontend/src/pages/customer/ReviewPages.tsx` SCR-25 `ReviewRatingPage`
- [ ] T025 [US1] Load booking via `bookingApi` when `bookingId` query param present on `Frontend/src/pages/customer/ReviewPages.tsx`

**Checkpoint**: US1 MVP — Customer create review testable

---

## Phase 4: User Story 2 — Customer xem, chỉnh sửa và xóa đánh giá (Priority: P1)

**Goal**: Customer GET/PUT/DELETE own reviews; edit updates public content if Published; delete frees booking slot

**Independent Test**: Edit own review → saved; delete → gone from /me and public; cross-customer edit → 403

### Implementation

- [ ] T026 [US2] Implement `getDetailForCustomer(reviewId, customerId)` ownership check in `ReviewService.java`
- [ ] T027 [US2] Implement `updateReview(reviewId, customerId, request)` — any status, recalc aggregate if Published in `ReviewService.java`
- [ ] T028 [US2] Implement `deleteReview(reviewId, customerId)` — hard delete + recalc aggregate in `ReviewService.java`
- [ ] T029 [US2] Add `GET /api/v1/reviews/{id}`, `PUT /api/v1/reviews/{id}`, `DELETE /api/v1/reviews/{id}` in `CustomerReviewController.java`
- [x] T030 [US2] Log `REVIEW_UPDATED` and `REVIEW_DELETED` to ActivityLog in `ReviewService.java`
- [ ] T031 [P] [US2] Add `updateReview` and `deleteReview` v1 paths in `Frontend/src/api/reviewApi.ts`
- [ ] T032 [US2] Wire My Reviews list + edit modal + delete confirm on `Frontend/src/pages/customer/ReviewPages.tsx` `MyReviewsPage` SCR-24
- [ ] T033 [US2] Show Hidden status badge on own reviews in `Frontend/src/pages/customer/ReviewPages.tsx`

**Checkpoint**: US2 testable — customer CRUD own reviews

---

## Phase 5: User Story 3 — Khách xem đánh giá công khai của phòng (Priority: P1)

**Goal**: Public paginated Published reviews per room; average/total on room; FR-03 Room Detail feed

**Independent Test**: 3 Published + 1 Hidden → public API returns 3; average excludes Hidden

### Implementation

- [ ] T034 [US3] Implement `listPublicByRoom(roomId, pageable)` — Published only, map `reviewerDisplayName` in `ReviewQueryService.java`
- [x] T035 [US3] Implement `recalculate(roomId)` full SQL AVG/COUNT Published in `RoomRatingAggregateService.java`
- [ ] T036 [US3] Create `PublicReviewController.java` with `GET /api/v1/rooms/{roomId}/reviews` in `Backend/src/main/java/com/homestay/controllers/PublicReviewController.java`
- [ ] T037 [US3] Ensure public response excludes email/phone — display name only in `PublicReviewResponse.java`
- [ ] T038 [P] [US3] Migrate `fetchRoomReviews` to `GET /api/v1/rooms/{id}/reviews` in `Frontend/src/api/roomsApi.ts`
- [ ] T039 [US3] Wire live reviews list + average on `Frontend/src/pages/public/RoomDetailPage.tsx`
- [ ] T040 [US3] Show empty state when no Published reviews on `Frontend/src/pages/public/RoomDetailPage.tsx`

**Checkpoint**: US3 testable — public room reviews + aggregates

---

## Phase 6: User Story 4 — Manager kiểm duyệt và ẩn đánh giá (Priority: P1)

**Goal**: Manager property-scoped list; Hide/Show with confirmation; SCR-65

**Independent Test**: Manager Hide Published → Hidden + removed from public; cross-property → 403

### Implementation

- [x] T041 [US4] Implement `listForManager(managerId, propertyId, status, pageable)` with `PropertyScopeService` in `ReviewQueryService.java`
- [x] T042 [US4] Implement `moderate(reviewId, moderatorId, status, scope)` — set moderatedBy/At, recalc aggregate in `ReviewModerationService.java`
- [x] T043 [US4] Enforce Manager property scope on moderate in `ReviewModerationService.java`
- [x] T044 [US4] Create `ManagerReviewController.java` with `GET /api/v1/manager/reviews` and `PATCH /api/v1/manager/reviews/{id}/status` in `Backend/src/main/java/com/homestay/controllers/ManagerReviewController.java`
- [x] T045 [US4] Log `REVIEW_HIDDEN` and `REVIEW_PUBLISHED` to ActivityLog in `ReviewModerationService.java`
- [x] T046 [P] [US4] Add `getManagerReviews` and `moderateReview` to `Frontend/src/api/reviewApi.ts`
- [x] T047 [US4] Replace `_sharedAdminData` mock with live API + property filter on `Frontend/src/pages/manager/ReviewMgmtPage.tsx`
- [x] T048 [US4] Add Hide/Show confirmation dialog on `Frontend/src/pages/manager/ReviewMgmtPage.tsx`

**Checkpoint**: US4 testable — manager moderation SCR-65

---

## Phase 7: User Story 5 — Admin kiểm duyệt toàn hệ thống (Priority: P2)

**Goal**: Admin global review list + Hide/Show; SCR-56 Content Moderation tab

**Independent Test**: Admin moderates review outside Manager property scope successfully

### Implementation

- [x] T049 [US5] Implement `listForAdmin(status, pageable)` — no property filter in `ReviewQueryService.java`
- [x] T050 [US5] Add Admin global path in `moderate()` without property gate in `ReviewModerationService.java`
- [x] T051 [US5] Create `AdminReviewController.java` with `GET /api/v1/admin/reviews` and `PATCH /api/v1/admin/reviews/{id}/status` in `Backend/src/main/java/com/homestay/controllers/AdminReviewController.java`
- [x] T052 [US5] Add Content Moderation reviews tab wired to admin API in `Frontend/src/pages/admin/AdminPages.tsx` (or dedicated component)
- [x] T053 [US5] Register admin moderation route if missing in `Frontend/src/App.tsx`

**Checkpoint**: US5 testable — admin global moderation

---

## Phase 8: User Story 6 — Điều hướng từ booking đã check-out (Priority: P2)

**Goal**: Booking detail shows Write Review CTA when CHECKED_OUT && !hasReview; SCR-25 prefill

**Independent Test**: CHECKED_OUT without review → CTA visible; with review → hidden/link to My Reviews

### Implementation

- [ ] T054 [US6] Add `hasReview` and optional `reviewId` to booking detail DTO in `Backend/src/main/java/com/homestay/dtos/booking/BookingDetailResponse.java` (or FR-04 mapper calling `ReviewRepository.existsByBookingId`)
- [ ] T055 [US6] Populate `hasReview` in booking detail service/query in `Backend/src/main/java/com/homestay/services/BookingService.java`
- [ ] T056 [P] [US6] Add `hasReview`/`reviewId` to `BookingDetailResponse` in `Frontend/src/api/bookingApi.ts`
- [ ] T057 [US6] Gate Write Review CTA on `CHECKED_OUT && !hasReview` in `Frontend/src/pages/customer/BookingDetailPage.tsx`
- [ ] T058 [US6] Add Write Review link on `Frontend/src/pages/customer/BookingListPage.tsx` only when eligible (replace status-only check)
- [ ] T059 [US6] Remove duplicate standalone `MyReviewsPage.tsx` / `ReviewRatingPage.tsx` routes if redundant — canonical `ReviewPages.tsx` in `Frontend/src/App.tsx`

**Checkpoint**: US6 testable — booking → review entry path

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Tests, quickstart validation, dedupe, discovery integration

- [x] T060 [P] Unit test CHECKED_OUT gate + one-per-booking in `Backend/src/test/java/com/homestay/unit/ReviewBookingValidatorTest.java`
- [x] T061 [P] Unit test aggregate recalc Published-only in `Backend/src/test/java/com/homestay/unit/RoomRatingAggregateServiceTest.java`
- [ ] T062 Integration test create + public filter + manager scope in `Backend/src/test/java/com/homestay/integration/ReviewControllerIT.java`
- [ ] T063 [P] Assert Hidden never in public list + duplicate create 409 in `ReviewControllerIT.java`
- [ ] T064 Run curl smoke tests in `specs/014-review-rating/quickstart.md` and fix gaps
- [x] T065 [P] Verify review routes in `Frontend/src/App.tsx` (customer/manager/admin)
- [x] T066 [P] Confirm `Frontend/src/pages/public/LandingPage.tsx` room cards use `averageRating`/`totalReviews` from room API after aggregates live

---

## Implement status (2026-07-23 gap-close)

Implemented against existing code (not greenfield). Covered in this pass:

- Comment **required 20–1000**; duplicate create → **ConflictException 409**
- `RoomRatingAggregateService` + `rooms.average_rating` / `total_reviews` (+ SchemaColumnPatcher)
- `ReviewModerationService` + `ManagerReviewController` / `AdminReviewController`
- ActivityLog `REVIEW_*`; `moderatedBy` / `moderatedAt` on Review
- FE: live `ReviewMgmtPage`, Admin Content Moderation tab, `reviewApi` moderate methods
- Unit tests: `ReviewServiceFr14Test` (passed)
- Booking detail `reviewId`; `RoomSummaryResponse` exposes aggregates

Deferred: dedicated Flyway V030 (using ddl-auto + SchemaColumnPatcher); full `ReviewControllerIT`; manual curl UAT after BE restart.
---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-04 bookings with CHECKED_OUT (blocker)
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP**; blocks US2 (edit needs create)
- **US2 (P1)**: After US1 (shared CustomerReviewController)
- **US3 (P1)**: After US1 (needs Published reviews to display); can parallel US2 after US1
- **US4 (P1)**: After US1 (needs reviews to moderate)
- **US5 (P2)**: After US4 (shared ReviewModerationService)
- **US6 (P2)**: After US1 (hasReview flag); frontend parallel with US2
- **Polish (Phase 9)**: After desired user stories complete

### User Story Dependencies

```text
Foundational → US1 → US2
                    → US3 (parallel after US1)
                    → US4 → US5
                    → US6 (after US1)
```

### Parallel Opportunities

**Phase 2** (after T002): T006–T009 all [P]  
**US1**: T023 parallel with backend after T020  
**US3**: T038 parallel with T034–T037  
**US4**: T046 parallel with T041–T045  
**Polish**: T060, T061, T063, T065, T066 parallel

### Parallel Example: User Story 1

```bash
T016–T022 ReviewBookingValidator + ReviewService + Controller
T023 reviewApi.ts
T024–T025 ReviewPages.tsx (after API contract stable)
```

### Parallel Example: User Story 3

```bash
T034–T037 PublicReviewController backend
T038 roomsApi.ts
T039–T040 RoomDetailPage.tsx
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T005)
2. Complete Phase 2: Foundational (T006–T015)
3. Complete Phase 3: User Story 1 (T016–T025)
4. **STOP and VALIDATE**: POST review + GET /reviews/me per `quickstart.md`
5. Demo SCR-25

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Customer create review (MVP)
3. US2 → Edit/delete own reviews
4. US3 → Public room reviews + aggregates
5. US4 → Manager moderation
6. US5 → Admin global moderation
7. US6 → Booking CTA + route cleanup
8. Polish → Tests + quickstart

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. After Foundational:
   - Dev A: US1 + US2 + US6 (Customer path)
   - Dev B: US3 (Public + RoomDetail)
   - Dev C: US4 + US5 (Moderation)
3. Polish when core paths work

---

## Notes

- **Hard delete** frees `booking_id` for re-review per spec assumption v1
- FR-03 discovery **reads** `GET /rooms/{id}/reviews` — FR-14 implements; do not duplicate write logic in FR-03
- Comment validation: min 20, max 1000 chars (not 200 from old mock)
- Manager SCR-65 route: `/manager/reviews` (existing in `App.tsx`)
- Employee: no review tasks — out of scope
- Commit after each task or logical group; stop at any checkpoint to validate story independently
