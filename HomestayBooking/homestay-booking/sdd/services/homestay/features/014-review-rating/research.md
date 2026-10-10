# Research: FR-14 Review & Rating

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `reviewApi.ts`, `ReviewPages.tsx`, FR-03 `rooms-discovery-api.yaml`

## 1. Table Ownership

**Decision**: FR-14 **owns** `reviews` table and **owns** maintenance of `rooms.average_rating` + `rooms.total_reviews` denormalized columns.

**Rationale**: Spec FR-010; FR-03 discovery reads aggregates for featured sort and room cards — transactional update on review lifecycle avoids expensive aggregate queries per request.

**Alternatives considered**: Materialized view only — rejected (adds refresh lag; spec SC-006 wants <5s accuracy).

## 2. Checked-out Booking Gate

**Decision**: `ReviewBookingValidator.assertEligibleForReview(bookingId, customerId)` requires booking status **CHECKED_OUT**, customer ownership, and `NOT EXISTS review WHERE booking_id`.

**Rationale**: Spec FR-001, FR-002; §7 validation rules.

**Alternatives considered**: Allow review after Checked-in — rejected per FR-14.

## 3. One Review Per Booking

**Decision**: `UNIQUE (booking_id)` on `reviews` table + application check before insert.

**Rationale**: Spec FR-002; DB constraint prevents race duplicates.

**Alternatives considered**: Soft-delete keeps unique — use hard delete or partial unique index `WHERE deleted_at IS NULL` if soft-delete adopted; v1 **hard delete** frees slot for re-review per spec assumption.

## 4. Review Status Model

**Decision**: Enum `ReviewStatus`: **PUBLISHED** (default on create), **HIDDEN** (moderation). No PENDING moderation queue v1.

**Rationale**: Spec FR-005; manual Hide only; customer edit keeps current status.

**Alternatives considered**: PENDING_APPROVAL workflow — rejected (YAGNI v1).

## 5. Room Aggregate Recalculation

**Decision**: `RoomRatingAggregateService.recalculate(roomId)` in same `@Transactional` as review create/update/delete/moderate:

```sql
average_rating = AVG(rating) WHERE status=PUBLISHED AND room_id=?
total_reviews = COUNT(*) WHERE status=PUBLISHED AND room_id=?
```

Null average when zero Published reviews.

**Rationale**: Spec FR-010, SC-006; immediate consistency.

**Alternatives considered**: Async event — rejected for v1 simplicity.

## 6. Public Reviews API (FR-03 Boundary)

**Decision**: FR-14 implements `GET /api/v1/rooms/{id}/reviews` in `PublicReviewController` (permitAll). Returns Published only with `reviewerDisplayName`, rating, comment, createdAt. FR-03 `RoomDiscoveryController` may delegate or frontend calls same endpoint — single implementation owned by FR-14.

**Rationale**: Spec FR-017; `roomsApi.ts` already calls this path; avoid duplicate logic in FR-03.

**Alternatives considered**: Duplicate query in FR-03 — rejected.

## 7. Customer API Surface

**Decision**: Align `api-spec-by-screen.md` + extend:

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/reviews/me` | CUSTOMER |
| POST | `/api/v1/reviews` | CUSTOMER |
| GET | `/api/v1/reviews/{id}` | CUSTOMER own |
| PUT | `/api/v1/reviews/{id}` | CUSTOMER own |
| DELETE | `/api/v1/reviews/{id}` | CUSTOMER own |

**Rationale**: Existing `reviewApi.ts` already has CRUD; api-spec only documents GET me + POST.

**Alternatives considered**: PATCH only — PUT matches existing frontend.

## 8. Moderation API

**Decision**: `PATCH /api/v1/manager/reviews/{id}/status` body `{ status: HIDDEN|PUBLISHED }` scoped by property via room join. Admin: `PATCH /api/v1/admin/reviews/{id}/status` same body, no scope. Set `moderatedBy`, `moderatedAt`.

**Rationale**: figma SCR-65 Hide/Show; entity-ui-mapping SCR-56 Admin tab.

**Alternatives considered**: Separate hide/show endpoints — single PATCH sufficient.

## 9. Delete and Re-review

**Decision**: **Hard delete** review row; `UNIQUE booking_id` freed; customer may submit new review for same booking.

**Rationale**: Spec assumption v1 US-2 scenario 3.

**Alternatives considered**: Immutable one-shot forever — conflicts with delete UX in spec.

## 10. Validation Rules

**Decision**: `rating` `@Min(1) @Max(5)`; `comment` `@NotBlank @Size(min=20, max=1000)`; trim whitespace before validate.

**Rationale**: Spec FR-004; figma SCR-30 min 20 chars.

**Alternatives considered**: 200 char max from old ReviewPages mock — use 1000 per figma/spec assumption.

## 11. Public Reviewer Identity

**Decision**: Public response exposes `reviewerDisplayName` from user profile (FR-02 `fullName` or `displayName`) — never email/phone.

**Rationale**: Spec FR-015.

**Alternatives considered**: Anonymous — rejected per spec assumption.

## 12. Manager List Scope

**Decision**: `GET /api/v1/manager/reviews?propertyId=&status=` joins review → room → property; filter `property_id IN managerAssignedIds`.

**Rationale**: Spec FR-011; consistent with maintenance/payment scope pattern.

**Alternatives considered**: Manager sees all — rejected.

## 13. Flyway Version

**Decision**: `V030__reviews_fr14.sql` after FR-13 V029.

**Rationale**: Sequential migration numbering.

## 14. Frontend Mock Removal

**Decision**: Remove `BOOKINGS_MOCK`, `localStorage` reviewed_mock_bookings, `_sharedAdminData` REVIEWS from production paths; wire `reviewApi` v1 + `bookingApi.getBookingDetail` for SCR-25 prefill.

**Rationale**: `ReviewPages.tsx` currently dual mock/API path — consolidate.

**Alternatives considered**: Keep mock for dev — use Flyway seed instead.

## 15. Booking Detail CTA (US-6)

**Decision**: `BookingDetailResponse` includes `hasReview: boolean` (or `reviewId`) computed server-side; frontend shows Write Review when `CHECKED_OUT && !hasReview`.

**Rationale**: Spec US-6; avoids extra round-trip guessing.

**Alternatives considered**: Client calls reviews/me and matches bookingId — extra call; server flag cleaner.
