# 004 - Booking & Inventory: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities & Enums

### BookingStatus Enum
Defines the state machine and inventory locking rules.
- **Locks Inventory (Blocking):** `PENDING_DEPOSIT`, `CONFIRMED`, `CHECKED_IN`, `PENDING_INSPECTION`, `PENDING_DAMAGE_PAYMENT`.
- **Terminal (Releases Inventory):** `CHECKED_OUT`, `CANCELLED`, `NO_SHOW`.

| No | Method | Description |
|---|---|---|
| - | - | (Enum class. Blocking states: `PENDING_DEPOSIT`, `CONFIRMED`, `CHECKED_IN`, `PENDING_INSPECTION`, `PENDING_DAMAGE_PAYMENT`. Terminal states: `CHECKED_OUT`, `CANCELLED`, `NO_SHOW`.) |


### Booking Class
Snapshot of the agreement.
- `totalAmount`: Frozen at creation.
- `depositAmount`: 40% of total.
- `remainingAmount`: 60% of total.
- `damageFeeAmount`: Default 0, populated post-inspection if damage occurs (FR-23).

| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Contains `totalAmount` frozen at creation, `depositAmount` as 40% of total, `remainingAmount` as 60%, and `damageFeeAmount` populated post-inspection.) |

---

## 2. Controllers

### BookingController Class
Handles customer booking operations.
- **Methods:**
  - `createBooking(CreateBookingRequest)`: Initializes booking, locks inventory, returns VNPay payment URL.
  - `getMyBookings()`: Returns list of user's bookings.
  - `getBookingDetail(UUID)`: Includes financial breakdown and permitted actions.
  - `cancelBooking(UUID)`: Applies dynamic cancellation policy (0%, 50%, 100% refund).

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createBooking | Takes `CreateBookingRequest`. Initializes booking, locks inventory, returns 201 Created with `CreateBookingResponse` containing VNPay payment URL. |
| 02 | getMyBookings | Retrieves a paginated list of the current user's bookings. Returns 200 OK. |
| 03 | getBookingDetail | Takes `UUID`. Retrieves booking detail including financial breakdown and permitted actions. Returns 200 OK. |
| 04 | cancelBooking | Takes `UUID`. Applies dynamic cancellation policy (0%, 50%, 100% refund). Transitions booking to `CANCELLED`. Returns 200 OK. |


### ManagerBookingController Class
Handles manager booking operations.
- **Methods:**
  - `getPropertyBookings(UUID propertyId)`: List bookings for an assigned property.
  - `checkIn(UUID)`: Transitions `CONFIRMED` -> `CHECKED_IN`.
  - `checkOut(UUID)`: Transitions `PENDING_INSPECTION` or `PENDING_DAMAGE_PAYMENT` -> `CHECKED_OUT`.
  - `modifyBooking(UUID, ModifyRequest)`: Re-calculates price and inventory for date/room swaps on `CONFIRMED` bookings.

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getPropertyBookings | Takes `UUID propertyId`. Lists bookings for an assigned property. Returns 200 OK. |
| 02 | checkIn | Takes `UUID`. Transitions booking from `CONFIRMED` to `CHECKED_IN`. Returns 200 OK. |
| 03 | checkOut | Takes `UUID`. Transitions booking from `PENDING_INSPECTION` or `PENDING_DAMAGE_PAYMENT` to `CHECKED_OUT`. Returns 200 OK. |
| 04 | modifyBooking | Takes `UUID, ModifyRequest`. Re-calculates price and inventory for date/room swaps on `CONFIRMED` bookings. Returns 200 OK. |

---

## 3. Services

### BookingService Class
Core logic for booking creation and management.
- **Methods:**
  - `createBooking`: Uses pessimistic write locks (`findByIdForUpdate`). Validates date overlap using half-open intervals `[checkIn, checkOut)`. Also checks against `RoomStatusBlock` (maintenance).
  - `processVNPayCallback`: When payment succeeds, updates Booking to `CONFIRMED`, and creates an `OutboxEvent` (type: `CONTRACT_GENERATE_REQUESTED`) to trigger PDF generation (FR-10).

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createBooking | Uses pessimistic write locks (`findByIdForUpdate`). Validates date overlap using half-open intervals `[checkIn, checkOut)`. Also checks against `RoomStatusBlock` (maintenance). Returns initialized booking with payment URL. |
| 02 | processVNPayCallback | When payment succeeds, updates Booking to `CONFIRMED`, and creates an `OutboxEvent` (type: `CONTRACT_GENERATE_REQUESTED`) to trigger PDF generation (FR-10). |


### BookingCronService Class
Scheduled jobs for background booking tasks.
- **Methods:**
  - `expireHoldTimeouts`: Runs every minute. Finds `PENDING_DEPOSIT` where `holdExpiresAt < now` (default 10 mins). Cancels booking and fails pending deposit payment.
  - `markNoShows`: Runs hourly. Finds `CONFIRMED` bookings where current time is > 24 hours past the start of `checkInDate`. Sets status to `NO_SHOW`. Deposit is forfeited.

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | expireHoldTimeouts | Runs every minute. Finds `PENDING_DEPOSIT` where `holdExpiresAt < now` (default 10 mins). Cancels booking and fails pending deposit payment. |
| 02 | markNoShows | Runs hourly. Finds `CONFIRMED` bookings where current time is > 24 hours past the start of `checkInDate`. Sets status to `NO_SHOW`. Deposit is forfeited. |


### ManagerBookingService Class
Core logic for manager-specific operations on bookings.
- **Methods:**
  - `checkOut`: Strictly gated. Must ensure `RoomInspection` is `PASSED`. If a `DAMAGE_FEE` exists, it must be `PAID`. On success, automatically inserts a `HousekeepingTask`.

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | checkOut | Strictly gated. Must ensure `RoomInspection` is `PASSED`. If a `DAMAGE_FEE` exists, it must be `PAID`. On success, transitions to `CHECKED_OUT` and automatically inserts a `HousekeepingTask`. |

---

## 4. DTOs

- **`CreateBookingResponse`**: Contains `bookingId`, `status`, `totalAmount`, `depositAmount`, `remainingAmount`, and crucially `paymentUrl` (VNPay).
- **`CancellationPreview`**: Used before confirming cancel. Calculates `refundPercent` based on `daysUntilCheckIn` (>=7: 100%, 3-6: 50%, <3: 0%).
