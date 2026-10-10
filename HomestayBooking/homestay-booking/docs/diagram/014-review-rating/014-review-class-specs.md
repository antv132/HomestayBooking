# 014 - Review & Rating Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entity

### Review Class
Represents a customer review for a room.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Contains fields like `booking_id`, `customer_id`, `room_id`, `rating`, `comment`, `status` (PUBLISHED/HIDDEN), `moderated_by`, `moderated_at`. Unique constraint on `booking_id`.) |

---

## 2. Controllers

### CustomerReviewController Class
Handles review operations for Customers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | submitReview | POST `/`. Submit a review. Pre-condition: Booking must be in `CHECKED_OUT` status. Returns 201 Created. |
| 02 | listMyReviews | GET `/`. List my reviews (paginated). Returns 200 OK. |
| 03 | getReview | GET `/{id}`. Get single review. Returns 200 OK. |
| 04 | updateReview | PUT `/{id}`. Edit review rating & comment. Returns 200 OK. |
| 05 | deleteReview | DELETE `/{id}`. Delete review entirely. Returns 204 No Content. |

### ManagerReviewController Class
Handles review operations for Managers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listReviews | GET `/`. List reviews for assigned properties (requires `propertyId`). Returns 200 OK. |
| 02 | moderateReview | PATCH `/{id}/moderate`. Hide/Show a review. Applies scope validation. Returns 200 OK. |

### AdminReviewController Class
Handles review operations for Admins.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listReviewsGlobally | GET `/`. List all reviews globally. Returns 200 OK. |
| 02 | moderateReviewGlobally | PATCH `/{id}/moderate`. Hide/Show any review without scope limits. Returns 200 OK. |

---

## 3. Services & Integrations

### ReviewService Class
Handles core customer review logic.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | handleCustomerAction | Validates rules: ownership, booking status (`CHECKED_OUT`), uniqueness, comment length (20-1000). Triggers `RoomRatingAggregateService.recalculate` on Create/Update/Delete. Logs `REVIEW_CREATED`, `REVIEW_UPDATED`, `REVIEW_DELETED`. Returns processed review. |

### ReviewModerationService Class
Handles manager/admin moderation actions.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | handleModeration | Enforces property scope for Managers. Modifying a review's status triggers `RoomRatingAggregateService.recalculate`. Hidden reviews are ignored by the aggregator. Logs `REVIEW_HIDDEN` or `REVIEW_PUBLISHED`. Returns updated review. |

### RoomRatingAggregateService Class
Maintains denormalized rating aggregates on the Room entity.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | recalculate | Called asynchronously or synchronously whenever a review is added, updated, deleted, or moderated. Only considers reviews with status = `PUBLISHED`. Recalculates `average_rating` and `total_reviews` and updates the `Room` entity to optimize read performance. Returns void. |
