# Implementation Plan: FR-14 Review & Rating

**Branch**: `016-review-rating` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/014-review-rating/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-14, §5 Review, §7 Validation, §8 Review and Feedback), `docs/api-spec-by-screen.md` (SCR-24/25), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.13, `docs/figma-generation-prompt.md` (SCR-30/31/65), `specs/003-room-discovery/contracts/rooms-discovery-api.yaml` (`GET /rooms/{id}/reviews`), frontend `reviewApi.ts`, `ReviewPages.tsx`, `ReviewMgmtPage.tsx`, `RoomDetailPage.tsx`

**Phụ thuộc**: FR-04 (booking CHECKED_OUT gate); FR-08 (Room + aggregate columns); FR-02 (customer display name); FR-06 (Manager property scope); FR-03 (discovery displays public reviews — FR-14 owns Review data + `ReviewQueryService`). **Ranh giới**: FR-03 owns discovery routes/UI enrichment; FR-14 owns Review CRUD, moderation, aggregates; Complaint out of scope.

## Summary

Triển khai **FR-14 Review & Rating**: bảng `reviews` + denormalized `rooms.average_rating`/`total_reviews`; Customer tạo/sửa/xóa review sau **Checked-out** (1 per booking, SCR-24/25); public paginated reviews per room (Published only); Manager moderate Hide/Show scoped by Property (SCR-65 `/manager/reviews`); Admin global moderation (SCR-56 tab); `RoomRatingAggregateService` recalc on create/update/delete/moderate. Stack: **Spring Boot 3 + JPA** + **React/TypeScript** — migrate `reviewApi.ts` và `roomsApi.ts` → `/api/v1/**`; replace mock/localStorage in `ReviewPages.tsx` và `ReviewMgmtPage.tsx`.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Flyway, Bean Validation  
**Storage**: SQL Server — `reviews` (V030); extend `rooms` with `average_rating`, `total_reviews`; read `bookings`, `users`  
**Testing**: JUnit 5 + Mockito; `ReviewBookingValidatorTest`; `RoomRatingAggregateServiceTest`; `ReviewControllerIT` RBAC + one-per-booking + public filter  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Public list p95 < 500ms; aggregate update synchronous in same transaction  
**Constraints**: UNIQUE `booking_id`; rating 1–5; comment 20–1000 chars; Published-only public; Manager property scope; `@Transactional` create+aggregate  
**Scale/Scope**: ~12 REST endpoints; 6 user stories; SCR-24/25/65 + Admin tab + FR-03 public read

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → ReviewService / ReviewModerationService → repositories |
| DTO + Bean Validation | PASS | CreateReviewRequest `@Min(1) @Max(5)`, comment length |
| Security-first (RBAC, scope) | PASS | Customer own; Manager property; Admin global; public read permitAll |
| No secrets in code | PASS | N/A for reviews |
| Test coverage ≥80% | PASS | One-per-booking + CHECKED_OUT gate + public filter IT |
| Standard API envelope | PASS | api-spec §1 |
| Audit log REVIEW_* | PASS | create, update, delete, hidden, published |
| Checked-out validation | PASS | FR-001 + §7 validation rules |

**Post-design re-check**: PASS — `RoomRatingAggregateService` single recalc entry; FR-03 consumes `ReviewQueryService` only.

## Project Structure

### Documentation (this feature)

```text
specs/014-review-rating/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/review-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── CustomerReviewController.java       # SCR-24/25 — /reviews/me, CRUD
│   ├── ManagerReviewController.java        # SCR-65 — list + PATCH status
│   ├── AdminReviewController.java          # SCR-56 tab — global moderate
│   └── PublicReviewController.java         # GET /rooms/{id}/reviews (FR-03 feed)
├── dtos/review/
│   ├── ReviewSummaryResponse.java
│   ├── ReviewDetailResponse.java
│   ├── ReviewPageResponse.java
│   ├── PublicReviewResponse.java
│   ├── CreateReviewRequest.java
│   ├── UpdateReviewRequest.java
│   └── ModerateReviewRequest.java
├── entities/
│   └── Review.java
├── enums/
│   └── ReviewStatus.java                     # PUBLISHED, HIDDEN
├── repositories/
│   └── ReviewRepository.java
├── services/
│   ├── ReviewService.java
│   ├── ReviewBookingValidator.java           # CHECKED_OUT + ownership
│   ├── ReviewModerationService.java
│   ├── ReviewQueryService.java               # public + manager scoped queries
│   └── RoomRatingAggregateService.java       # update rooms aggregates
└── configs/
    └── SecurityConfig.java                   # permitAll public reviews GET

Backend/src/main/resources/db/migration/
└── V030__reviews_fr14.sql

Backend/src/test/java/com/homestay/
├── unit/ReviewBookingValidatorTest.java
├── unit/RoomRatingAggregateServiceTest.java
└── integration/ReviewControllerIT.java

Frontend/src/
├── api/reviewApi.ts                          # migrate → /api/v1/reviews/**
├── api/roomsApi.ts                           # fetchRoomReviews → /api/v1/rooms/{id}/reviews
├── pages/customer/
│   └── ReviewPages.tsx                       # SCR-24/25 — remove mock/localStorage
├── pages/manager/
│   └── ReviewMgmtPage.tsx                    # SCR-65 — live Hide/Show
├── pages/admin/
│   └── AdminPages.tsx                        # Content Moderation tab (extend)
└── pages/public/
    └── RoomDetailPage.tsx                    # wire live reviews + aggregates
```

**Structure Decision**: FR-14 **owns** `reviews` table and room rating aggregates. **Consumes** FR-04 bookings, FR-08 rooms. **Feeds** FR-03 discovery via `PublicReviewController` + room detail fields. Frontend review UI **exists** — migrate API and remove mocks.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Flyway V030 `reviews` + room aggregate columns | data-model.md |
| **B** | Review entity, enum, repo, DTOs, validators | Foundational |
| **C** | ReviewService create + ReviewBookingValidator | US-1 |
| **D** | CustomerReviewController CRUD + aggregate on create | US-1, US-2 |
| **E** | Customer update/delete + booking re-review after delete | US-2 |
| **F** | ReviewQueryService public Published list | US-3 |
| **G** | PublicReviewController + FR-03 room detail aggregates | US-3 |
| **H** | ReviewModerationService Manager scoped | US-4 |
| **I** | AdminReviewController global moderate | US-5 |
| **J** | BookingDetail isReviewed CTA + frontend migration | US-6 |
| **K** | Tests + quickstart | SC-001–SC-008 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| Create review | `POST /api/reviews` + localStorage mock | `POST /api/v1/reviews` |
| My reviews | `GET /api/reviews/my` + mock | `GET /api/v1/reviews/me` |
| Update/delete | `/api/reviews/{id}` | `PUT/DELETE /api/v1/reviews/{id}` |
| Manager moderate | `_sharedAdminData` REVIEWS mock | `GET /manager/reviews`, `PATCH .../status` |
| Public room reviews | `GET /api/rooms/{id}/reviews` | `GET /api/v1/rooms/{id}/reviews` Published only |
| Review form | mock bookings fallback | `bookingApi` + CHECKED_OUT gate |
| Admin moderation | missing live tab | SCR-56 Content Moderation tab |
| Duplicate pages | `MyReviewsPage.tsx`, `ReviewRatingPage.tsx` | Consolidate exports in `ReviewPages.tsx` |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-04 CHECKED_OUT bookings missing | Blocker — seed Checked-out booking in V030 |
| FR-03 already stubbed reviews endpoint | FR-14 implements `PublicReviewController`; FR-03 frontend already calls path |
| Aggregate drift | Recalc in `RoomRatingAggregateService` on every mutation |
| Delete → re-review ambiguity | Spec assumption v1 — document in quickstart |
| Manager SCR-65 vs SCR-56 Admin | Shared `ReviewModerationService` with scope param |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/review-api.yaml](./contracts/review-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
