# Quickstart: FR-11 Billing Management

**Feature**: `specs/011-billing-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-04 bookings table + create booking flow; FR-01 auth (CUSTOMER/MANAGER JWT); FR-06 manager property assignment.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Flyway **V026** (invoices) + **V027** (payments.invoice_id) applied after FR-04 migrations

## Environment

```bash
# No FR-11-specific env beyond standard backend
VITE_API_BASE=http://localhost:8080
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
| SCR-18 Booking Detail | `/customer/bookings/:id` | `GET /bookings/me/{id}` (includes `invoiceBreakdown`) |
| SCR-26 Payment History | `/customer/payments` | `GET /invoices/me` |
| SCR-35 Manager Booking Detail | `/manager/bookings/:id` | `GET /manager/bookings/{id}` (includes `invoiceBreakdown`) |
| SCR-36 Payment Management | `/manager/payments` | `GET /manager/invoices?propertyId=&status=UNPAID` (pending tab) |

## Smoke test: invoice pair on booking create

```bash
BASE=http://localhost:8080/api/v1
CUSTOMER_TOKEN="<customer-jwt>"

# 1. Create booking (FR-04)
BOOKING=$(curl -s -X POST "$BASE/bookings" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roomId":"<room-uuid>","checkInDate":"2026-08-01","checkOutDate":"2026-08-03","guestCount":2}' )

BOOKING_ID=$(echo "$BOOKING" | jq -r '.data.id')
echo "Booking: $BOOKING_ID"

# 2. Verify two invoices exist
curl -s "$BASE/bookings/me/$BOOKING_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.invoiceBreakdown'

# Expected: 2 invoices — DEPOSIT + REMAINING_BALANCE, both UNPAID
# deposit amount = 40% total, remaining = 60% total
```

## Smoke test: customer invoice history

```bash
curl -s "$BASE/invoices/me?page=0&size=10" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.content[] | {type, amount, status}'
```

## Smoke test: manager property-scoped list

```bash
MANAGER_TOKEN="<manager-jwt>"
PROPERTY_ID="<assigned-property-uuid>"

curl -s "$BASE/manager/invoices?propertyId=$PROPERTY_ID&status=UNPAID" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq '.data.content | length'
```

## Smoke test: booking cancel cancels unpaid invoices

```bash
# Cancel pending-deposit booking (FR-04)
curl -s -X PATCH "$BASE/bookings/$BOOKING_ID/cancel" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason":"test cancel"}' | jq

curl -s "$BASE/invoices/me?status=CANCELLED" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.content[] | select(.bookingId=="'"$BOOKING_ID"'")'
```

## Invoice status after payment (FR-12 integration)

After FR-12 implements deposit payment:

```bash
# Customer initiates VNPay deposit → Payment PENDING → invoice PENDING_PAYMENT
# VNPay IPN success → Payment PAID → invoice PAID

curl -s "$BASE/bookings/me/$BOOKING_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.invoiceBreakdown.invoices[] | select(.type=="DEPOSIT")'
```

## Frontend verification

1. Login as Customer → create booking → open Booking Detail → see Deposit/Remaining rows with badges.
2. Open Payment History → see invoice lines (not empty after booking create).
3. Login as Manager → open Booking Detail → Payment Breakdown section populated.
4. Manager Payments tab → Unpaid filter shows bookings with open invoices.

## Troubleshooting

| Issue | Check |
|-------|-------|
| No invoices after booking | `InvoiceIssuanceService` wired in `BookingService.createBooking`; V026 applied |
| Duplicate invoice error | UNIQUE (booking_id, type) — idempotency working |
| paidAmount mismatch | `InvoiceStatusSyncService` updates booking cache on PAID |
| Manager 403 | Manager assigned to booking's property via FR-06 |
