# Data Model: FR-14 Review & Rating

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Review

## Scope

FR-14 **owns** `reviews` and **maintains** denormalized `rooms.average_rating`, `rooms.total_reviews`. **Consumes** FR-04 `bookings` (CHECKED_OUT gate), FR-08 `rooms`, FR-02 user display names. **Feeds** FR-03 public discovery read APIs.

## ERD

```text
Customer 1──* Review *──1 Booking (UNIQUE)
Room 1──* Review
Review.status PUBLISHED | HIDDEN

create/update/delete/moderate ──> RoomRatingAggregateService ──> rooms.average_rating, total_reviews
```

## Table: reviews

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| customer_id | UUID | FK users NOT NULL | Reviewer |
| booking_id | UUID | FK bookings NOT NULL UNIQUE | One review per booking |
| room_id | UUID | FK rooms NOT NULL | Denormalized from booking |
| property_id | UUID | FK properties NOT NULL | Denormalized for Manager scope index |
| rating | SMALLINT | NOT NULL CHECK 1–5 | |
| comment | TEXT | NOT NULL | 20–1000 chars app validation |
| status | VARCHAR(16) | NOT NULL DEFAULT 'PUBLISHED' | PUBLISHED, HIDDEN |
| moderated_by | UUID | FK users nullable | Manager/Admin |
| moderated_at | TIMESTAMPTZ | nullable | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `UNIQUE (booking_id)` — one review per booking
- `(customer_id, created_at DESC)` — SCR-24 GET /reviews/me
- `(room_id, status, created_at DESC)` — public list Published
- `(property_id, status, created_at DESC)` — Manager moderation list

### ReviewStatus enum

`PUBLISHED` | `HIDDEN`

### Status transitions (moderation only)

```text
(create)           → PUBLISHED
Manager/Admin Hide → HIDDEN
Manager/Admin Show → PUBLISHED

Customer edit      → status unchanged
Customer delete    → row removed; booking_id slot freed
```

## Table: rooms (extension — FR-08 owned, FR-14 maintains aggregates)

| Column | Type | Notes |
|--------|------|-------|
| average_rating | DECIMAL(3,2) | nullable; NULL when no Published reviews |
| total_reviews | INTEGER | NOT NULL DEFAULT 0; count Published only |

V030 adds columns if missing from FR-08 schema.

## Validation Rules (application layer)

| Rule | Error code (suggested) |
|------|------------------------|
| Booking not CHECKED_OUT | `REVIEW_BOOKING_NOT_ELIGIBLE` |
| Booking not owned by customer | `UNAUTHORIZED` |
| Booking already reviewed | `BOOKING_ALREADY_REVIEWED` |
| Rating not 1–5 | `VALIDATION_ERROR` |
| Comment < 20 or > 1000 chars | `VALIDATION_ERROR` |
| Edit/delete not own review | `UNAUTHORIZED` |
| Manager moderate out of scope | `UNAUTHORIZED_PROPERTY_ACCESS` |
| Invalid moderation transition | `REVIEW_INVALID_STATUS` |

## ActivityLog actions

`REVIEW_CREATED`, `REVIEW_UPDATED`, `REVIEW_DELETED`, `REVIEW_HIDDEN`, `REVIEW_PUBLISHED`

## Public review response shape (no PII)

```json
{
  "id": "uuid",
  "rating": 5,
  "comment": "Great stay...",
  "reviewerDisplayName": "Nguyen Van A",
  "createdAt": "2026-06-01T10:00:00Z"
}
```

Hidden reviews **never** appear in public list or aggregates.

## Aggregate recalculation SQL (conceptual)

```sql
UPDATE rooms r SET
  average_rating = sub.avg_rating,
  total_reviews = sub.cnt
FROM (
  SELECT room_id,
         ROUND(AVG(rating)::numeric, 2) AS avg_rating,
         COUNT(*)::int AS cnt
  FROM reviews
  WHERE room_id = :roomId AND status = 'PUBLISHED'
  GROUP BY room_id
) sub
WHERE r.id = :roomId;
-- If cnt=0: set average_rating NULL, total_reviews 0
```

Invoked from `RoomRatingAggregateService` after every review mutation affecting Published set.

## Flyway

```text
V030__reviews_fr14.sql
  - CREATE reviews
  - ALTER rooms ADD average_rating, total_reviews IF NOT EXISTS
  - indexes above
  - optional seed: 1 CHECKED_OUT booking + 1 PUBLISHED review
```

## RBAC Matrix (implementation)

| Action | Customer | Manager | Admin | Guest |
|--------|----------|---------|-------|-------|
| Create | ✓ own CHECKED_OUT booking | — | — | — |
| List own | ✓ | — | — | — |
| Update/delete | ✓ own | — | — | — |
| List moderate | — | ✓ property | ✓ global | — |
| Hide/Show | — | ✓ property | ✓ global | — |
| Public read room reviews | — | — | — | ✓ (Published) |

Employee: no access.

## Integration Points

| FR | Integration |
|----|-------------|
| FR-04 | Booking status CHECKED_OUT; optional `hasReview` on booking detail DTO |
| FR-08 | room_id FK; aggregate columns on rooms |
| FR-02 | reviewerDisplayName from user profile |
| FR-06 | Manager property scope on moderation list/action |
| FR-03 | `GET /rooms/{id}/reviews` + room card `averageRating`/`totalReviews` |

## Booking Detail enrichment (US-6)

Add to `BookingDetailResponse` (FR-04 or FR-14 read):

| Field | Type | Description |
|-------|------|-------------|
| hasReview | boolean | true if review exists for booking |
| reviewId | UUID nullable | link to My Reviews when hasReview |
