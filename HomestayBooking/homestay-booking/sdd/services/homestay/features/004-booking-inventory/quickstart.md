# Quickstart: FR-04 Booking & Inventory Management

**Feature**: `sdd/services/homestay/features/004-booking-inventory` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 auth (Customer + Manager JWT); room available (no overlapping booking / RoomStatusBlock); SQL Server; JDK 17.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server
- VNPay sandbox (Return URL)

## Environment

```bash
# Backend (.env / application.yml)
BOOKING_HOLD_TIMEOUT_MINUTES=10
BOOKING_NO_SHOW_AFTER_HOURS=24
VNPAY_TMN_CODE=
VNPAY_HASH_SECRET=
VNPAY_RETURN_URL=http://localhost:8080/api/payments/vnpay/return
JWT_SECRET=
DB_URL=
DB_USERNAME=
DB_PASSWORD=
```

```bash
# Frontend/.env
VITE_API_URL=http://localhost:8080
```

## Run

```bash
cd Backend && ./mvnw spring-boot:run
# or IDE: com.homestay.HomestayApplication (JAVA_HOME=JDK 17)

cd Frontend && npm install && npm run dev
```

## Screen → Route → API (edu)

| Screen | Route | API |
|--------|-------|-----|
| SCR-16 Checkout | `/customer/bookings/new` | `POST /api/v1/bookings` |
| SCR-17 My Bookings | `/customer/bookings` | `GET /api/v1/bookings/me` |
| SCR-18 Detail | `/customer/bookings/:id` | `GET /api/v1/bookings/me/{id}` |
| SCR-19 Cancel | `/customer/bookings/:id/cancel` | preview + cancel |
| Deposit VNPay | `/customer/bookings/:id/deposit` | create VNPay URL + Return URL |
| SCR-34 Manager list | `/manager/bookings` | `GET /api/v1/manager/bookings` |
| SCR-35 Detail / modify | `/manager/bookings/:id` | get + `PATCH .../modify` |
| SCR-37 Check-in/out | `/manager/bookings/:id/check-in` | `PATCH .../check-in` / `check-out` |

**Deferred:** SCR-20 receipt upload / Manager CK verify (TD-011).

## Smoke (happy path)

1. Customer login → create booking (2+ nights) → status `PENDING_DEPOSIT`, note `holdExpiresAt`.
2. Pay deposit via VNPay sandbox → Return URL → `CONFIRMED`; contract outbox/email.
3. Second customer same room overlapping nights → **409**.
4. Same-day turnover: A `[D, D+2)`, B `[D+2, D+4)` → **both OK** (half-open).
5. Active MAINTENANCE `RoomStatusBlock` overlapping stay → create **409**.
6. Cancel preview at 5 days → `refundPercent=50`; at 7 days → `100`.
7. Manager check-in with `remainingCollected=true` if remaining unpaid → `CHECKED_IN`.
8. Checkout blocked until inspection (+ damage PAID if any) → `CHECKED_OUT` + HK task.

## Verify jobs

- Unpaid hold past `holdExpiresAt` → within ~1–2 job ticks → `CANCELLED`.
- CONFIRMED with `checkInDate` yesterday and now past `atStartOfDay()+24h` → `NO_SHOW`.

## Common failures

| Symptom | Likely cause |
|---------|----------------|
| 409 on create | Overlap booking or RoomStatusBlock |
| Hold cancelled while paying | Finish VNPay before timeout; Return URL must mark PAID |
| Legacy check-in 400 | Use `/api/v1/manager/bookings/{id}/check-in` |
| Checkout conflict | Inspection / unpaid DAMAGE_FEE |
