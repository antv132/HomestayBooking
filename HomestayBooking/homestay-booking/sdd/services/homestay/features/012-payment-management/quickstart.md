# Quickstart: FR-12 Payment Management & Reconciliation

**Feature**: `specs/012-payment-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-04 bookings; FR-11 invoices (V026/V027); FR-01 JWT; VNPay sandbox credentials; optional MailHog for notifications.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Flyway V027 (invoice_id) + V028 (payment extensions) applied
- VNPay sandbox account

## Environment

```bash
VNPAY_TMN_CODE=your_tmn_code
VNPAY_HASH_SECRET=your_hash_secret
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_API_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
VNPAY_RETURN_URL=http://localhost:8080/api/payments/vnpay/return
# VNPAY_IPN_URL=...  # DEFERRED edu (TD-012) — not required for sandbox
APP_RECEIPTS_DIR=./uploads/receipts
```

Vite proxy: `/api/v1` → `http://localhost:8080`

> **Edu (TD-012):** Confirm VNPay via **Return URL** only. Do not configure IPN unless Spec Sync reopens.

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

Use ngrok only if you need a public Return URL from VNPay sandbox. ~~IPN~~ deferred TD-012.

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| Deposit pay | `/customer/payments/:id/pay/deposit` | `POST /payments/vnpay/create-url?type=DEPOSIT` |
| Remaining pay | `/customer/payments/:id/remaining` | `POST /payments/vnpay/create-url?type=REMAINING_BALANCE` |
| SCR-20 Receipt | upload flow | `POST /bookings/{id}/receipts` |
| SCR-26 History | `/customer/payments` | `GET /payments/me` |
| SCR-36 Manager list | `/manager/payments` | `GET /manager/payments?propertyId=` |
| SCR-37 Verify | `/manager/payments/:id/verify` | `PATCH /manager/payments/{id}/verify` |
| SCR-52 Reconciliation | `/admin/reconciliation` (new) | `GET /admin/payments/reconciliation` |

## Smoke test: VNPay deposit

```bash
BASE=http://localhost:8080/api/v1
CUSTOMER_TOKEN="<customer-jwt>"
BOOKING_ID="<pending-deposit-booking-uuid>"

# 1. Create VNPay URL
curl -s -X POST "$BASE/payments/vnpay/create-url?bookingId=$BOOKING_ID&type=DEPOSIT" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq

# 2. Complete payment on VNPay sandbox → browser hits Return URL → Paid
# 3. Verify booking confirmed
curl -s "$BASE/bookings/me/$BOOKING_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.status'
# Expected: CONFIRMED
```

## Smoke test: bank transfer + manager verify

```bash
MANAGER_TOKEN="<manager-jwt>"

# Customer uploads receipt (after creating bank transfer payment)
curl -s -X POST "$BASE/bookings/$BOOKING_ID/receipts" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod":"BANK_TRANSFER","fileUrl":"/uploads/receipts/test.jpg","fileName":"test.jpg","fileSize":12345}' | jq

PAYMENT_ID=$(curl -s "$BASE/manager/payments?propertyId=<property-uuid>&status=PENDING" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq -r '.data.content[0].id')

curl -s -X PATCH "$BASE/manager/payments/$PAYMENT_ID/verify" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"APPROVED","note":"OK"}' | jq
```

## Smoke test: payment history

```bash
curl -s "$BASE/payments/me?page=0&size=10" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.content[] | {type, amount, status}'
```

## Smoke test: admin reconciliation

```bash
ADMIN_TOKEN="<admin-jwt>"

curl -s "$BASE/admin/payments/reconciliation?status=DISCREPANCY" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

## Troubleshooting

| Issue | Check |
|-------|-------|
| Return not received | Check `VNPAY_RETURN_URL` + signature secret; customer must complete redirect |
| Booking not Confirmed after pay | PaymentConfirmationService → DepositConfirmationService wired |
| Approve without receipt fails | Expected — attach PaymentReceipt first |
| Invoice still Unpaid | InvoiceStatusSyncService called from confirmPaid |
| Duplicate Confirmed | orderRef idempotency — check UNIQUE constraint |
