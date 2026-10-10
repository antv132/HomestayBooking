# Homestay Booking System — User Manual (Workflow Guides)

**Project:** Homestay Booking System  
**Base URL (local):** `http://localhost:3000`  
**Document purpose:** End-user step-by-step guides for each main workflow (screens + actions).

> **Screenshot convention:** Replace each `[Screenshot: …]` with an actual image.  
> Suggested file names: `WF{n}_Step{m}_{Screen}.png` (store in Drive / `docs/user-manual/screenshots/`).

**II. Installation Guides:** see [`Installation-Guides.md`](./Installation-Guides.md) (JDK 17, Docker SQL Server, Backend + Frontend — not NetBeans 8.2 / HashMap).

---

## 2. Authentication Workflow (Register / Login / Forgot Password)

**Purpose:** Allow a new guest to create a Customer account, verify email (OTP), sign in, and recover access if they forget the password. After login, the system routes the user by role (Customer / Manager / Employee / Admin).

### 2.1 Register a new Customer account

| Step | Screen | Action |
|---|---|---|
| 1 | Landing (SCR-01) — `/` | Open the home page. Click **Sign up / Register**. |
| 2 | Register (SCR-03) — `/register` | Enter **Full name**, **Email**, **Password** (meet password rules). Click **Register**. |
| 3 | OTP Verification (SCR-04) — `/verify-otp` | Open email inbox, copy the OTP. Enter OTP on screen. Click **Verify**. |
| 4 | Login (SCR-02) — `/login` | After success, sign in with the verified email + password. |

**Notes**
- OTP is time-limited; request a new code if expired.
- Duplicate email is rejected.

`[Screenshot: WF1_Step2_Register.png]`  
`[Screenshot: WF1_Step3_OTP.png]`

### 2.2 Login

| Step | Screen | Action |
|---|---|---|
| 1 | Login (SCR-02) — `/login` | Enter email + password. Click **Login**. |
| 2 | Role home | System redirects: Customer → `/customer/dashboard` (SCR-15); Manager → `/manager/dashboard` (SCR-27); Employee → `/employee/dashboard` (SCR-59); Admin → admin dashboard. |

`[Screenshot: WF1_Step1_Login.png]`  
`[Screenshot: WF1_Step2_CustomerDashboard.png]`

### 2.3 Forgot / Reset password

| Step | Screen | Action |
|---|---|---|
| 1 | Forgot Password (SCR-05) — `/forgot-password` | Enter account email. Submit. |
| 2 | Email | Open reset link from email (anti-enumeration: UI message stays generic). |
| 3 | Reset Password (SCR-06) — `/reset-password` | Enter new password + confirm. Submit. |
| 4 | Login (SCR-02) | Sign in with the new password. |

`[Screenshot: WF1_ForgotPassword.png]`  
`[Screenshot: WF1_ResetPassword.png]`

---

## 2. Booking & Deposit Payment Workflow

**Purpose:** Let a Customer search a room, create a booking hold (`PENDING_DEPOSIT`), pay **40% deposit via VNPay** within **10 minutes**, then receive a confirmed booking (`CONFIRMED`) and contract PDF.

### 2.1 Search and select a room

| Step | Screen | Action |
|---|---|---|
| 1 | Landing / Search (SCR-01 / SCR-07) — `/` or `/rooms` | Enter check-in / check-out dates, guests, filters. Click **Search**. |
| 2 | Room Listing (SCR-07) — `/rooms` | Browse available rooms. Open a room card. |
| 3 | Room Detail (SCR-08) — `/rooms/:id` | Review photos, amenities, price. Optionally open **Availability Calendar** (SCR-09). |
| 4 | Room Detail | Click **Book now** (requires login as Customer). |

`[Screenshot: WF2_Step1_Search.png]`  
`[Screenshot: WF2_Step3_RoomDetail.png]`

### 2.2 Create booking (checkout form)

| Step | Screen | Action |
|---|---|---|
| 1 | Booking Checkout (SCR-16) — `/customer/bookings/new` (or equivalent) | Confirm dates, guests, special notes. Review price breakdown (total, deposit 40%, remaining 60%). |
| 2 | Submit | Click **Continue / Create booking**. System locks inventory → status **PENDING_DEPOSIT**; hold countdown starts (~10 minutes). |
| 3 | Order Review & Payment (SCR-20) | Review invoice + policy. Watch **hold countdown**. |

**Notes**
- If deposit is not paid before hold expires → booking becomes **CANCELLED**, room is released.
- Dates use half-open range; timezone **Asia/Ho_Chi_Minh**.

`[Screenshot: WF2_Step2_CheckoutForm.png]`  
`[Screenshot: WF2_Step3_HoldCountdown.png]`

### 2.3 Pay deposit (VNPay)

| Step | Screen | Action |
|---|---|---|
| 1 | Order Review & Payment (SCR-20) | Click **Pay deposit with VNPay**. |
| 2 | VNPay gateway (external) | Complete payment on VNPay sandbox/production. |
| 3 | Return URL result page | System confirms deposit via **Return URL only** → booking **CONFIRMED**. |
| 4 | Booking Detail (SCR-18) | Verify status **CONFIRMED**, deposit paid, contract available. |

`[Screenshot: WF2_VNPay.png]`  
`[Screenshot: WF2_Confirmed_BookingDetail.png]`

---

## 2. Cancel & Refund Workflow

**Purpose:** Allow a Customer to cancel a **CONFIRMED** booking before stay, with deposit refund by tier; allow a Manager to cancel with **100% deposit refund** and a required reason.

### 2.1 Customer cancel

| Step | Screen | Action |
|---|---|---|
| 1 | My Bookings (SCR-17) — `/customer/bookings` | Open a **CONFIRMED** booking. |
| 2 | Booking Detail (SCR-18) | Click **Cancel booking**. |
| 3 | Booking Cancellation (SCR-19) | Review refund preview: **≥7 days = 100%**, **3–7 days = 50%**, **&lt;3 days = 0%** of paid deposit. Confirm cancel. |
| 4 | Booking Detail | Status becomes **CANCELLED**; room released; refund amount shown if applicable. |

`[Screenshot: WF3_CustomerCancel_Preview.png]`  
`[Screenshot: WF3_CancelledStatus.png]`

### 2.2 Manager cancel

| Step | Screen | Action |
|---|---|---|
| 1 | Manager Booking List (SCR-34) — `/manager/bookings` | Open the booking. |
| 2 | Manager Booking Detail (SCR-35) | Click **Cancel**. |
| 3 | Manager Cancellation (SCR-69) | Enter **cancel reason** (required). Submit. Refund = **100% deposit**. |
| 4 | Booking Detail | Status **CANCELLED**; inventory released. |

`[Screenshot: WF3_ManagerCancel.png]`

---

## 2. Check-in / Check-out Workflow

**Purpose:** On stay day, Manager collects remaining payment (if needed), verifies guest ID (CMND/CCCD), checks the guest in; after stay, inspection runs, optional damage fee is paid, then Manager completes check-out.

### 2.1 Pay remaining 60% (before or at check-in)

| Step | Screen | Action |
|---|---|---|
| 1 | Customer Booking Detail (SCR-18) | If remaining unpaid, choose **Pay remaining (VNPay)** **or** pay **cash at desk**. |
| 2 | Manager Check-in (SCR-37) | For cash: mark **remaining collected** → Payment CASH **PAID**. |

**Rule:** Remaining **must be PAID** before check-in completes.

`[Screenshot: WF4_RemainingPayment.png]`

### 2.2 Manager check-in

| Step | Screen | Action |
|---|---|---|
| 1 | Booking List / Detail (SCR-34 / SCR-35) | Open today’s **CONFIRMED** booking. |
| 2 | Check-in / Check-out (SCR-37) | Enter / verify **CMND/CCCD**. Confirm remaining paid. Click **Check-in**. |
| 3 | Result | Status → **CHECKED_IN**. Hand over room key per property process. |

**Notes**
- Check-in is not allowed before the check-in date.
- Legacy check-in URLs are blocked; use Manager `/api/v1/...` screens only.

`[Screenshot: WF4_CheckIn_Form.png]`  
`[Screenshot: WF4_CheckedIn.png]`

### 2.3 Inspection + damage (if any) + check-out

| Step | Screen | Action |
|---|---|---|
| 1 | Employee Inspection Hub (SCR-62) — `/employee/inspections` | Claim / open assigned inspection. Mark **Pass** or **Fail**. |
| 2a | Pass | Manager proceeds to check-out. |
| 2b | Fail | Employee creates **Damage Report** (SCR-64). Manager reviews (SCR-43). Fee **&gt; 5,000,000 VND** needs **Admin** co-approval (SCR-53). |
| 3 | Customer | Pay damage fee via **VNPay** if required → unlocks checkout (`PENDING_DAMAGE_PAYMENT` → paid). |
| 4 | Manager Check-out (SCR-37) | After inspection passed (and damage paid if any), click **Check-out**. |
| 5 | Result | Status → **CHECKED_OUT**; housekeeping task is triggered. |

`[Screenshot: WF4_Inspection.png]`  
`[Screenshot: WF4_DamageReport.png]`  
`[Screenshot: WF4_CheckOut.png]`

---

## 2. Contract & Payment History Workflow

**Purpose:** After deposit confirmation, the system generates a contract PDF; Customer and Manager can view/resend contracts; Customer can review payment history.

### 2.1 Customer view contract

| Step | Screen | Action |
|---|---|---|
| 1 | My Contracts (SCR-21) — `/customer/contracts` | Open contract list after booking is **CONFIRMED**. |
| 2 | Contract Detail / Drawer | Open PDF preview or download. |

`[Screenshot: WF5_ContractList.png]`  
`[Screenshot: WF5_ContractPDF.png]`

### 2.2 Manager contract management

| Step | Screen | Action |
|---|---|---|
| 1 | Contract Management (SCR-38) — `/manager/contracts` | Filter by property/booking. |
| 2 | Drawer | Preview PDF; optionally **Resend email**. |

`[Screenshot: WF5_ManagerContracts.png]`

### 2.3 Payment history

| Step | Screen | Action |
|---|---|---|
| 1 | Payment History (SCR-26) — `/customer/payments` | Review DEPOSIT / REMAINING / DAMAGE_FEE / refund rows. |
| 2 | Manager Payments (SCR-36) | Manager reviews property payment table. |

`[Screenshot: WF5_PaymentHistory.png]`

---

## 2. Manager Operations Workflow (Rooms & Staff Tasks)

**Purpose:** Manager maintains property structure and rooms, assigns employees, and monitors housekeeping / maintenance / inspections for the assigned property.

### 2.1 Manage rooms

| Step | Screen | Action |
|---|---|---|
| 1 | Structure (SCR-28) — `/manager/structure` | View Property → Floor tree; add/edit floors if needed. |
| 2 | Room List (SCR-29) — `/manager/rooms` | Search/filter rooms. |
| 3 | Add Room (SCR-30) — `/manager/rooms/add` | Create room (type, capacity, price, floor). |
| 4 | Edit / Gallery / Status (SCR-31–33) | Update info, upload images, set date-scoped status blocks (maintenance/unavailable). |
| 5 | Room Calendar — `/manager/rooms/:id/calendar` | Review occupancy calendar. |

`[Screenshot: WF6_RoomList.png]`  
`[Screenshot: WF6_AddRoom.png]`  
`[Screenshot: WF6_RoomCalendar.png]`

### 2.2 Assign staff & monitor ops

| Step | Screen | Action |
|---|---|---|
| 1 | Employee Management (SCR-39) | Assign employees to property. |
| 2 | Housekeeping Tasks (SCR-40) / Schedule (SCR-66) | Assign / track cleaning tasks. |
| 3 | Maintenance Tasks (SCR-41) | Assign / update maintenance tickets. |
| 4 | Inspection Management (SCR-42) | Assign inspector; review results. |
| 5 | Damage Report Management (SCR-43) | Approve/reject damage fees. |

`[Screenshot: WF6_Housekeeping.png]`  
`[Screenshot: WF6_DamageApproval.png]`

---

## 2. Employee Operations Workflow

**Purpose:** Employee uses a read-oriented dashboard, then completes housekeeping, maintenance, room inspection, and damage reporting within property scope. (No booking/payment mutations from dashboard.)

### 2.1 Start the day from dashboard

| Step | Screen | Action |
|---|---|---|
| 1 | Login as Employee | Redirect to Employee Dashboard (SCR-59) — `/employee/dashboard`. |
| 2 | Dashboard | Review KPI cards, **today tasks**, **awaiting**, **completed today**. Click a task preview to open the workspace. |

`[Screenshot: WF7_EmployeeDashboard.png]`

### 2.2 Housekeeping

| Step | Screen | Action |
|---|---|---|
| 1 | Housekeeping Workspace (SCR-60) — `/employee/housekeeping` | Open assigned cleaning tasks. |
| 2 | Task action | Start task → **IN_PROGRESS**; finish → **COMPLETED**. |

`[Screenshot: WF7_Housekeeping.png]`

### 2.3 Maintenance

| Step | Screen | Action |
|---|---|---|
| 1 | Maintenance Workspace (SCR-61) — `/employee/maintenance` | Open tickets. |
| 2 | Update progress | Update status / notes until resolved per Manager assignment. |

`[Screenshot: WF7_Maintenance.png]`

### 2.4 Inspection & damage report

| Step | Screen | Action |
|---|---|---|
| 1 | Room Inspection Hub (SCR-62) — `/employee/inspections` | Claim unassigned or open assigned inspection. |
| 2 | Result | Mark **Pass** or **Fail** (only if assigned to you). |
| 3 | Damage List / Create (SCR-63 / SCR-64) — `/employee/damage`, `/employee/damage/create` | If fail: create damage report with photos and estimated fee. |

`[Screenshot: WF7_Inspection.png]`  
`[Screenshot: WF7_CreateDamage.png]`

---

## 2. No-show Handling Workflow (System + Manager view)

**Purpose:** If a guest does not arrive, the system automatically marks **CONFIRMED** bookings as **NO_SHOW** after **24 hours** past expected check-in, keeps the deposit, and releases the room.

| Step | Actor | Action |
|---|---|---|
| 1 | System job | `BookingNoShowJob` runs on schedule (timezone Asia/Ho_Chi_Minh). |
| 2 | System | Eligible booking: status **CONFIRMED**, check-in date + 24h passed, guest not checked in → **NO_SHOW**. |
| 3 | Manager | Open Booking Detail (SCR-35): verify **NO_SHOW**, deposit kept, room available again. |
| 4 | Customer | Booking Detail (SCR-18) shows **NO_SHOW** (no check-in possible). |

`[Screenshot: WF8_NoShow_BookingDetail.png]`

---

## Quick reference — Booking status lifecycle

```
PENDING_DEPOSIT → CONFIRMED → CHECKED_IN → PENDING_INSPECTION
       ↓              ↓                           ↓
   CANCELLED       NO_SHOW              PENDING_DAMAGE_PAYMENT (optional)
       ↑              ↑                           ↓
   (hold timeout / cancel)                    CHECKED_OUT
```

| Status | Meaning |
|---|---|
| PENDING_DEPOSIT | Hold room; await 40% VNPay within ~10 minutes |
| CONFIRMED | Deposit paid; contract generated |
| CHECKED_IN | Guest arrived; remaining paid |
| PENDING_INSPECTION | Pre-checkout inspection |
| PENDING_DAMAGE_PAYMENT | Damage fee unpaid |
| CHECKED_OUT | Stay completed; housekeeping triggered |
| CANCELLED | Cancelled by customer/manager or hold timeout |
| NO_SHOW | Guest did not arrive after 24h |

---

## Evidence checklist (for submission)

| Workflow | Min. screenshots to attach |
|---|---|
| 1. Authentication | Register, OTP, Login, Forgot password |
| 2. Booking & Deposit | Search, Checkout, Hold countdown, VNPay return, CONFIRMED detail |
| 3. Cancel & Refund | Customer cancel preview, CANCELLED; Manager cancel reason |
| 4. Check-in / Check-out | Remaining pay, Check-in, Inspection, Check-out |
| 5. Contract & Payment | Contract PDF, Payment history |
| 6. Manager Ops | Room list, Add room, Housekeeping assign |
| 7. Employee Ops | Dashboard, HK task, Inspection, Damage create |
| 8. No-show | Booking detail NO_SHOW |

---

*End of workflow guides — replace all `[Screenshot: …]` placeholders before final PDF export.*
