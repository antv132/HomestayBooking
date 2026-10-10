# Feature Specification: FR-12 Payment Management & Reconciliation

**Feature Branch**: `014-payment-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-12 Payment Management & Reconciliation — dựa vào docs (Specification_v2.md § FR-12, §5 Payment/PaymentReceipt, §6 RBAC, §7 Error Handling, §8 Acceptance VNPay, api-spec-by-screen SCR-16/20/26/36/37/52, screen.md, screendesign.md, entity-ui-mapping.md §1.8, frontend paymentApi.ts, PaymentPages.tsx, PaymentMgmtListPage.tsx)"

**Phụ thuộc**: FR-04; FR-11; FR-01; FR-06; FR-10; FR-15. **Ranh giới**: FR-12 owns **VNPay** payment + history. **Edu cuts:** bank transfer/receipts **TD-011**; reconciliation cron/SCR-52 **TD-010**; **VNPay IPN TD-012** (Return URL only). Damage fee (FR-23) pay via VNPay when implemented.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customer thanh toán đặt cọc qua VNPay (Priority: P1)

> **Edu:** Chỉ **VNPay**. Chuyển khoản + biên lai = **TD-011**.

Là **Customer** với booking **Pending Deposit**, tôi muốn thanh toán **40% cọc** qua **VNPay** để giữ chỗ và chuyển booking sang **Confirmed**.

**Why this priority**: FR-12 core — Deposit Payment; FR-04 gate.

**Independent Test**: VNPay deposit success → Payment Paid, booking Confirmed (+ invoice/contract when modules ready).

**Acceptance Scenarios**:

1. **Given** booking Pending Deposit, **When** Customer hoàn tất VNPay, **Then** sau **Return URL** verified → Payment **Paid**; booking → **Confirmed**. ~~IPN~~ **TD-012**.
2. ~~Chuyển khoản + upload biên lai (SCR-20)~~ **DEFERRED TD-011**
3. **Given** VNPay redirect hủy/chưa xong, **When** quay lại, **Then** Payment Failed hoặc Pending; booking vẫn Pending Deposit.
4. **Given** Customer A, **When** cố pay booking của B, **Then** từ chối.
5. **Given** deposit đã Paid, **When** pay lại, **Then** từ chối.

---

### User Story 2 - Customer thanh toán phần còn lại qua VNPay (Priority: P1)

Là **Customer** với booking **Confirmed**, tôi muốn thanh toán **60% còn lại** qua **VNPay**.

**Why this priority**: Remaining Balance; CHECKIN_DENIED_UNPAID.

**Independent Test**: Confirmed → VNPay remaining → Paid.

**Acceptance Scenarios**:

1. **Given** Confirmed + remaining UNPAID, **When** VNPay success, **Then** Remaining **Paid**; `paid_amount` đủ.
2. ~~Chuyển khoản remaining + receipt~~ **DEFERRED TD-011**
3. **Given** chưa Confirmed, **When** pay remaining, **Then** từ chối.
4. **Given** remaining đã Paid, **When** pay lại, **Then** từ chối.

---

### User Story 3 - Manager xác minh chuyển khoản với biên lai (Priority: P1)

> **EDU SCOPE CUT (TD-011):** US3 / SCR-37 / PaymentReceipt bắt buộc / reminder 24h CK = **out of scope**.  
> Manager vẫn xem **lịch sử payment** (US5) theo property — không Approve CK.

Là **Manager**, tôi muốn Approve/Reject chuyển khoản kèm biên lai…

**Why this priority**: ~~FR-12 bank verify~~ — **deferred edu**.

**Independent Test**: **N/A edu**.

**Acceptance Scenarios**: *(all **DEFERRED TD-011**)*

1. ~~List Pending CK scoped by property~~
2. ~~Approve with PaymentReceipt → Paid + Confirmed~~
3. ~~Approve without receipt blocked~~
4. ~~Reject → Failed + notify~~
5. ~~Reminder Pending >24h~~

---

### User Story 4 - VNPay timeout và đối soát tự động / thủ công (Priority: P1)

> **EDU SCOPE CUT (2026-07-23 — Spec Sync):** **Cắt** cron đối soát 15 phút + Admin SCR-52 + manual “Sync from VNPay” khỏi phạm vi khóa học / Sprint hiện tại.  
> **EDU SCOPE CUT (TD-012):** **Cắt VNPay IPN** — sandbox/edu chỉ bắt buộc **Return URL** + verify chữ ký + Paid idempotent.  
> **Vẫn bắt buộc cho VNPay:** create-url + **verify chữ ký** trên **Return URL**; cập nhật Paid **idempotent**.  
> Chi tiết cắt → `sdd/sprints/tech-debt.md` **TD-010**, **TD-012**. Scenarios 1, 3, 4 = **Deferred TD-010**; IPN parts = **Deferred TD-012**.

Là **hệ thống** (và **Admin**), tôi muốn cron đối soát **15 phút** các Payment VNPay **Pending** (mất return/rớt mạng) và Admin xử lý lệch trên SCR-52 để không mất giao dịch khách đã trả tiền.

**Why this priority**: FR-12 explicit VNPay Reconciliation; §8 Acceptance "VNPay Integration"; tránh khách bị trừ tiền hệ thống chưa ghi nhận.

**Independent Test**: Seed Pending VNPay payment → cron query OrderRef → updates Paid if VNPay success; discrepancy → Admin SCR-52 list + manual sync drawer.

**Acceptance Scenarios**:

1. ~~**Given** Payment VNPay status **Pending** với `orderRef`, **When** cron reconciliation chạy (mỗi 15 phút), **Then** hệ thống truy vấn VNPay API và cập nhật **Paid** hoặc **Failed** khớp cổng.~~ **DEFERRED (TD-010)**
2. **Given** **Return URL** callback hợp lệ (signature verified), **When** nhận return, **Then** cập nhật Payment **Paid** idempotent; không double-confirm booking. **(IN SCOPE — edu)** ~~IPN path~~ **DEFERRED (TD-012)**
3. ~~**Given** số tiền VNPay ≠ amount DB, **When** reconciliation phát hiện, **Then** đánh dấu **DISCREPANCY**; Admin thấy trên SCR-52.~~ **DEFERRED (TD-010)**
4. ~~**Given** Admin mở SCR-52, **When** chọn giao dịch lệch và **Sync from VNPay**, **Then** cập nhật trạng thái theo cổng; ghi audit.~~ **DEFERRED (TD-010)**
5. **Given** cùng **return** replay, **When** Payment đã Paid, **Then** không duplicate side-effects (idempotent). **(IN SCOPE — edu)**

---

### User Story 5 - Customer và Manager xem lịch sử thanh toán chi tiết (Priority: P1)

Là **Customer** và **Manager**, tôi muốn xem lịch sử payment với loại, số tiền, phương thức, trạng thái, người xác minh và thời gian để tra cứu minh bạch.

**Why this priority**: FR-12 "Xem lịch sử thanh toán chi tiết kèm trạng thái"; SCR-26, SCR-36.

**Independent Test**: Customer `/payments/me` lists own payments; Manager scoped list (VNPay records; no receipt UX required).

**Acceptance Scenarios**:

1. **Given** Customer có payments, **When** mở Payment History (SCR-26), **Then** danh sách type, amount, method (**VNPay**), status, createdAt, paidAt — chỉ của mình.
2. **Given** Manager, **When** xem payment detail, **Then** hiển thị booking, customer, gateway/status metadata.
3. ~~PaymentReceipt image link~~ **N/A edu (TD-011)** unless legacy data exists.

---

### User Story 6 - Thanh toán phí bồi thường hư hại (Damage Fee Payment) (Priority: P2)

Là **hệ thống**, khi Manager/Admin phê duyệt **Damage Report**, tôi muốn tự động tạo payment obligation (invoice + payment path) cho **Damage Fee** để Customer thanh toán trước checkout hoàn tất.

**Why this priority**: FR-12 explicit Damage Fee Payment; §10 "tự động tạo khi Manager phê duyệt"; phụ thuộc FR-23 damage flow.

**Independent Test**: Damage approved → Customer pay Damage Fee via **VNPay**; booking → Pending Damage Payment until paid.

**Acceptance Scenarios**:

1. **Given** damage fee được phê duyệt (FR-23), **When** approval hoàn tất, **Then** tạo obligation + Customer initiate **VNPay** payment.
2. **Given** Customer thanh toán damage fee VNPay thành công, **When** confirmed, **Then** Payment Paid; booking có thể tiếp tục checkout (FR-04).
3. **Given** FR-23 chưa triển khai, **When** v1 ship, **Then** US6 deferred — stub only.

---

### Edge Cases

- VNPay URL creation / signature verification failure → user-friendly error; Payment không Paid; log for admin.
- Multiple concurrent VNPay attempts same invoice → one active Pending per invoice; new attempt cancels prior Pending or rejects (assumption: one Pending payment per invoice).
- Refund on booking cancel → Payment **Refunded** stub; payout manual v1.
- **Cash remaining tại quầy (check-in)** — **IN SCOPE** (SCR-37 `remainingCollected` → Payment CASH PAID). ~~Cash + receipt upload flow~~ vẫn out (TD-011).
- Employee — no Payment access (RBAC `-`).
- Admin read global payments. *(SCR-52 — TD-010.)*
- ~~Bank info + receipt upload page~~ — **TD-011**.
- Payment amount MUST match expected deposit/remaining snapshot — reject mismatch on create.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Customer MUST thanh toán Deposit (40%) qua **VNPay** khi booking **Pending Deposit**. ~~Bank Transfer + receipt~~ **TD-011**.
- **FR-002**: Customer MUST thanh toán Remaining (60%) qua **VNPay** khi booking **Confirmed**, **hoặc** Manager thu **tiền mặt tại quầy** khi check-in (SCR-37). ~~Bank Transfer~~ **TD-011**.
- **FR-003**: Mỗi Payment SHOULD liên kết invoiceId (FR-11) khi invoice tồn tại; amount MUST khớp số tiền kỳ vọng.
- **FR-004**: VNPay MUST tạo Payment **Pending** với unique **orderRef**; verify **Return URL** trước khi **Paid**. ~~IPN~~ **DEFERRED TD-012**.
- **FR-005**: Deposit **Paid** MUST → booking **Confirmed** (+ invoice/contract side-effects khi có).
- **FR-006**: Remaining **Paid** MUST cập nhật paid balance; MUST NOT re-confirm booking.
- **FR-007**: ~~Bank Transfer + PaymentReceipt (SCR-20)~~ — **DEFERRED TD-011**.
- **FR-008**: ~~Manager Approve/Reject CK (SCR-37) + require receipt~~ — **DEFERRED TD-011**.
- **FR-009**: ~~verifiedBy / Reject note for CK~~ — **DEFERRED TD-011**.
- **FR-010**: ~~Reconciliation cron 15m~~ — **DEFERRED TD-010**.
- **FR-011**: ~~Admin SCR-52 + Sync~~ — **DEFERRED TD-010**.
- **FR-012**: Customer MUST xem payment history (SCR-26); Manager MUST xem list/detail scoped (SCR-36) — **VNPay records**.
- **FR-013**: ~~Pending bank transfer >24h reminder~~ — **DEFERRED TD-011**.
- **FR-014**: Damage Fee Payment MUST auto-create when damage approved (FR-23) — **P2** if damage flow unavailable.
- **FR-015**: Hệ thống MUST ghi **ActivityLog** (PAYMENT_CREATED, PAYMENT_PAID, PAYMENT_FAILED, PAYMENT_VERIFIED, PAYMENT_REJECTED, PAYMENT_RECONCILED).
- **FR-016**: FR-12 MUST **không** tạo Invoice billing documents — thuộc FR-11; FR-12 consumes existing invoices.

### Key Entities

- **Payment**: id, bookingId, customerId, type (Deposit | Remaining | Damage Fee), amount, method (**VNPay** primary; Bank Transfer/Cash enum may remain in DB but **unused in edu UI**), status, orderRef, gateway fields, paidAt, …
- **PaymentReceipt**: ~~required for CK~~ — **out of edu product scope (TD-011)**; entity may linger unused.
- **Invoice** (optional link): FR-11.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: **100%** VNPay **Return URL** hợp lệ → Payment **Paid** trong **dưới 30 giây**.
- **SC-002**: ~~reconciliation cron~~ — **N/A TD-010**.
- **SC-003**: ~~bank transfer Approve → Confirmed~~ — **N/A TD-011**.
- **SC-004**: ~~Approve without receipt~~ — **N/A TD-011**.
- **SC-005**: Customer hoàn tất VNPay deposit (redirect + return) trong **dưới 5 phút** excluding gateway time.
- **SC-006**: **100%** payment history đúng scope.
- **SC-007**: **0** duplicate Confirmed từ replay **Return URL**.
- **SC-008**: ~~CK reminder 24h~~ — **N/A TD-011**.

## Assumptions

- **Edu (2026-07-23):** **Online = VNPay-only** (TD-011). Cắt bank transfer, PaymentReceipt upload, Manager CK verify. **Giữ:** thu remaining **tiền mặt tại quầy** khi check-in (SCR-37).
- **Edu:** Cắt reconciliation cron + SCR-52 (TD-010).
- **Edu (2026-07-23):** Cắt **VNPay IPN** (TD-012) — confirm path = **Return URL** only; đủ cho sandbox test.
- VNPay sandbox credentials từ env.
- **orderRef** unique; one active Pending VNPay attempt per booking/type at a time.
- Agents MUST NOT build SCR-20/37 bank-transfer UX unless Spec Sync reopens TD-011.
- Agents MUST NOT build VNPay IPN unless Spec Sync reopens TD-012.
- Damage Fee US6 **P2** — VNPay when FR-23 ready.
- SCR-26 history = Payment records (VNPay).

## Out of Scope (edu)

- Bank transfer + biên lai (SCR-20), Manager verify CK (SCR-37), CK reminder 24h — **TD-011**
- Reconciliation cron + SCR-52 — **TD-010**
- VNPay IPN server callback — **TD-012**
- Cash payment recording with receipt
