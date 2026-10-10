# Quickstart: FR-10 Contract Management

**Feature**: `specs/010-contract-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-04 bookings + outbox (V023); deposit → CONFIRMED flow; FR-01 users; SMTP or MailHog for dev.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Flyway V024 applied (after FR-04 V023)
- MailHog optional: `localhost:1025`

## Environment

```bash
APP_CONTRACTS_DIR=./uploads/contracts
SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=1025
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
| SCR-21 My Contracts | `/customer/contracts` | `GET /contracts/me` |
| SCR-21 Detail/PDF | `/customer/contracts/:id` | `GET /contracts/{id}`, `GET /contracts/{id}/pdf` |
| SCR-38 Manager List | `/manager/contracts` | `GET /manager/contracts?propertyId=` |
| SCR-38 Detail/Resend | `/manager/contracts/:id` | `GET /manager/contracts/{id}`, `POST .../resend` |

## Trigger contract generation (via booking)

```bash
BASE=http://localhost:8080/api/v1
CUSTOMER_TOKEN="<customer-jwt>"
MANAGER_TOKEN="<manager-jwt>"
BOOKING_ID="<booking-uuid-after-deposit-confirmed>"

# After FR-04 deposit confirmed → wait for Outbox worker (~15s)
# Verify contract exists by booking
curl -s "$BASE/contracts/booking/$BOOKING_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq

CONTRACT_ID=$(curl -s "$BASE/contracts/booking/$BOOKING_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq -r '.data.id')
```

## curl smoke tests

```bash
# Customer list
curl -s "$BASE/contracts/me?page=0&size=10" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq

# Customer detail
curl -s "$BASE/contracts/$CONTRACT_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq

# Download PDF
curl -s "$BASE/contracts/$CONTRACT_ID/pdf" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -o contract.pdf

# Manager list (scoped property)
PROPERTY_ID="<property-uuid>"
curl -s "$BASE/manager/contracts?propertyId=$PROPERTY_ID" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq

# Manager resend email
curl -s -X POST "$BASE/manager/contracts/$CONTRACT_ID/resend" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}' | jq

# Idempotency: check only one contract per booking
curl -s "$BASE/contracts/booking/$BOOKING_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq '.data.id'

# Cross-customer denied
OTHER_TOKEN="<other-customer-jwt>"
curl -s "$BASE/contracts/$CONTRACT_ID" \
  -H "Authorization: Bearer $OTHER_TOKEN" | jq
```

## Validation checklist

- [ ] Deposit CONFIRMED creates exactly **one** contract per booking
- [ ] Outbox worker processes within **5 minutes** in dev
- [ ] Customer receives email (MailHog UI or logs)
- [ ] `pdfUrl` / PDF download returns immutable snapshot bytes
- [ ] Manager resend updates `sentAt` without changing `pdfUrl`
- [ ] Manager cannot access contract outside assigned property
- [ ] Booking CANCELLED → contract status CANCELLED
- [ ] Booking CHECKED_OUT → contract status COMPLETED
- [ ] ActivityLog CONTRACT_GENERATED / CONTRACT_EMAIL_SENT / CONTRACT_RESENT

## Troubleshooting

| Issue | Fix |
|-------|-----|
| No contract after deposit | Check outbox_events PENDING; FR-04 CONFIRMED transition |
| Outbox FAILED | Check APP_CONTRACTS_DIR writable; PDF lib on classpath |
| Email not sent | Verify SMTP/MailHog; check CONTRACT_EMAIL_SEND outbox |
| Empty manager list | Verify propertyId filter + manager assignment FR-06 |
| 404 on booking contract | Deposit not confirmed yet — expected |
