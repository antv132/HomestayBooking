# Quickstart: FR-14 Review & Rating

**Feature**: `specs/014-review-rating` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-04 booking with **CHECKED_OUT** status; FR-01 JWT (Customer, Manager, Admin); FR-06 Manager property assignment; Flyway V030 applied.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: 1 property, 1 room, 1 CHECKED_OUT booking (no review yet), Customer/Manager/Admin tokens

## Environment

```bash
# No special env for reviews v1
```

Vite proxy: `/api/v1` → `http://localhost:8080`

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| SCR-24 My Reviews | `/customer/reviews` | `GET /reviews/me` |
| SCR-25 Review Form | `/customer/reviews/create?bookingId=` | `POST /reviews` |
| Public Room Detail | `/rooms/:id` | `GET /rooms/{id}/reviews` |
| SCR-65 Manager | `/manager/reviews` | `GET /manager/reviews?propertyId=` |
| Admin tab | `/admin/...` Content Moderation | `GET /admin/reviews` |

## Smoke test: create review

```bash
BASE=http://localhost:8080/api/v1
CUSTOMER_TOKEN="<customer-jwt>"
BOOKING_ID="<checked-out-booking-uuid>"

curl -s -X POST "$BASE/reviews" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"bookingId\":\"$BOOKING_ID\",\"rating\":5,\"comment\":\"Excellent stay! The room was clean and staff very helpful.\"}" | jq

# Expected: success, status PUBLISHED

# Duplicate — should 409
curl -s -X POST "$BASE/reviews" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"bookingId\":\"$BOOKING_ID\",\"rating\":4,\"comment\":\"Second attempt should fail validation path.\"}" | jq
```

## Smoke test: my reviews + update + delete

```bash
REVIEW_ID=$(curl -s "$BASE/reviews/me" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq -r '.data.content[0].id')

curl -s -X PUT "$BASE/reviews/$REVIEW_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"rating":4,"comment":"Updated review after thinking more about the stay experience."}' | jq

curl -s -X DELETE "$BASE/reviews/$REVIEW_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq

# After delete — may create review again for same booking (v1 assumption)
```

## Smoke test: public reviews

```bash
ROOM_ID="<room-uuid>"

curl -s "$BASE/rooms/$ROOM_ID/reviews?page=0&size=5" | jq

# Verify only PUBLISHED; no email/phone fields
```

## Smoke test: manager moderation

```bash
MANAGER_TOKEN="<manager-jwt>"
PROPERTY_ID="<property-uuid>"
REVIEW_ID="<published-review-uuid>"

# Hide
curl -s -X PATCH "$BASE/manager/reviews/$REVIEW_ID/status" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"HIDDEN"}' | jq

# Public list should exclude hidden review
curl -s "$BASE/rooms/<room-id>/reviews" | jq '.data.totalElements'

# Show again
curl -s -X PATCH "$BASE/manager/reviews/$REVIEW_ID/status" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"PUBLISHED"}' | jq
```

## Smoke test: eligibility gates

```bash
# Review before CHECKED_OUT — should 400
CONFIRMED_BOOKING_ID="<confirmed-booking-uuid>"
curl -s -X POST "$BASE/reviews" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"bookingId\":\"$CONFIRMED_BOOKING_ID\",\"rating\":5,\"comment\":\"Should fail because not checked out yet.\"}" | jq
```

## Verification checklist

- [ ] Create only when booking CHECKED_OUT
- [ ] One review per booking enforced
- [ ] Public list shows Published only
- [ ] Hide removes from public + lowers aggregate
- [ ] Manager cross-property moderate returns 403
- [ ] Customer edit/delete own only
- [ ] ActivityLog REVIEW_* entries
- [ ] Booking detail shows Write Review CTA when eligible

## Frontend manual test

1. Complete checkout flow (or seed CHECKED_OUT booking)
2. Customer → Booking Detail → Write Review → submit 5 stars
3. Customer → My Reviews → edit comment → save
4. Guest → Room Detail → see review + average rating
5. Manager → Review Management → Hide → verify gone from public
6. Manager → Show → verify back on public
