# Feature Specification: FR-11 Billing Management

**Feature Branch**: `013-billing-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-11 Billing Management — dựa vào docs (Specification_v2.md § FR-11, §5 Booking/Payment, §10 Payment acceptance, FR-04 40/60 snapshot, api-spec-by-screen SCR-18/26/35/36, screen.md, screendesign.md Payment Breakdown, entity-ui-mapping.md §1.8)"

**Phụ thuộc**: FR-04 (Booking snapshot TotalAmount/DepositAmount/RemainingAmount, booking lifecycle); FR-01 (Customer/Manager auth & scope). **Ranh giới**: FR-12 owns payment execution (VNPay, bank transfer verification, receipt upload, reconciliation, Payment/PaymentReceipt records); FR-10 owns contract PDF; FR-23 owns damage-fee billing trigger — FR-11 v1 covers **deposit (40%)** and **remaining balance (60%)** invoices only.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hệ thống tự động tạo hóa đơn cọc và phần còn lại khi đặt phòng (Priority: P1)

Là **hệ thống**, khi Customer tạo booking thành công (trạng thái **Pending Deposit**), tôi muốn tự động phát hành **hai hóa đơn** — một cho **đặt cọc 40%** và một cho **phần còn lại 60%** — dựa trên số tiền đã snapshot trên booking, để mọi nghĩa vụ thanh toán được theo dõi rõ ràng trước khi khách thực hiện thanh toán.

**Why this priority**: FR-11 core — "Tạo hóa đơn thanh toán đặt cọc (40%) và phần còn lại (60%)"; không có hóa đơn thì không thể theo dõi trạng thái billing.

**Independent Test**: Tạo booking mới → hai invoice records tồn tại với amount khớp DepositAmount/RemainingAmount trên booking; deposit invoice trạng thái Unpaid; remaining invoice trạng thái Unpaid; không tạo trùng khi retry.

**Acceptance Scenarios**:

1. **Given** booking mới ở **Pending Deposit** với TotalAmount đã snapshot, **When** booking được lưu, **Then** hệ thống tạo **Invoice Deposit** amount = **40%** TotalAmount và **Invoice Remaining Balance** amount = **60%** TotalAmount.
2. **Given** hai invoice vừa tạo, **When** xem chi tiết, **Then** cả hai có trạng thái **Unpaid**, liên kết bookingId và customerId, amount **không** thay đổi khi Manager sửa giá phòng sau đó.
3. **Given** booking đã có invoice pair, **When** cùng sự kiện tạo booking replay, **Then** **không** tạo invoice trùng (một cặp deposit + remaining per booking).
4. **Given** deposit invoice Unpaid, **When** FR-12 bắt đầu thanh toán cọc (VNPay redirect hoặc upload biên lai), **Then** deposit invoice chuyển **Pending Payment** cho đến khi thanh toán được xác nhận hoặc thất bại.
5. **Given** deposit invoice, **When** thanh toán cọc được xác nhận thành công (FR-12), **Then** deposit invoice → **Paid** và ghi thời điểm hoàn tất.

---

### User Story 2 - Customer xem trạng thái hóa đơn trên booking và lịch sử (Priority: P1)

Là **Customer**, tôi muốn xem breakdown hóa đơn (cọc / phần còn lại), số tiền đã trả và số còn nợ trên chi tiết booking và lịch sử thanh toán để biết mình cần thanh toán gì và khi nào.

**Why this priority**: FR-11 "Theo dõi trạng thái hóa đơn"; SCR-18 hiển thị Total Price, Amount Paid, Remaining Balance; SCR-26 Payment History.

**Independent Test**: Customer mở SCR-18 → thấy hai dòng invoice với status badge; SCR-26 liệt kê các invoice/payment line items thuộc customer; không thấy invoice booking khác.

**Acceptance Scenarios**:

1. **Given** Customer có booking với invoice pair, **When** mở Booking Detail (SCR-18), **Then** hiển thị **Payment Breakdown**: loại hóa đơn (Deposit / Remaining), amount, trạng thái, tổng đã trả và số dư còn lại.
2. **Given** deposit đã Paid và remaining Unpaid, **When** xem SCR-18, **Then** Amount Paid = deposit amount; Remaining Balance = remaining invoice amount; nút hành động thanh toán phần còn lại hiển thị khi booking **Confirmed** (luồng thanh toán thuộc FR-12).
3. **Given** Customer mở Payment History (SCR-26), **When** xem danh sách, **Then** thấy các dòng billing (invoice type, amount, status, ngày phát hành/cập nhật) thuộc tài khoản mình, sắp xếp mới nhất trước.
4. **Given** Customer A, **When** cố xem invoice của booking Customer B, **Then** từ chối truy cập.

---

### User Story 3 - Manager theo dõi trạng thái hóa đơn theo Property (Priority: P1)

Là **Manager**, tôi muốn xem breakdown thanh toán trên chi tiết booking và danh sách giao dịch liên quan trong Property được gán để biết khách nào chưa đóng cọc hoặc chưa trả phần còn lại trước check-in.

**Why this priority**: Manager cần visibility trước check-in; SCR-35 Payment Breakdown; SCR-36 Payment Management liệt kê pending items; CHECKIN_DENIED_UNPAID business rule.

**Independent Test**: Manager mở SCR-35 booking thuộc property được gán → thấy invoice breakdown; filter SCR-36 theo Unpaid/Pending → chỉ booking thuộc property scope.

**Acceptance Scenarios**:

1. **Given** Manager được gán property P, **When** mở Booking Detail (SCR-35), **Then** cột Payment Breakdown hiển thị deposit và remaining invoice với status badge.
2. **Given** Manager mở Payment Management (SCR-36), **When** lọc Pending/Unpaid, **Then** danh sách hiển thị booking/invoice cần xử lý **chỉ** thuộc property P.
3. **Given** remaining invoice vẫn Unpaid trước check-in, **When** Manager xem SCR-35, **Then** breakdown làm rõ số tiền còn thiếu (hỗ trợ quyết định check-in — gate thực thi FR-04/FR-12).
4. **Given** Manager không được gán property, **When** truy cập invoice/booking ngoài scope, **Then** từ chối.

---

### User Story 4 - Đồng bộ trạng thái hóa đơn khi booking thay đổi vòng đời (Priority: P2)

Là **hệ thống**, khi booking bị hủy, hết hạn giữ phòng, hoặc được Manager sửa với chênh lệch giá, tôi muốn cập nhật trạng thái hóa đơn tương ứng để billing luôn phản ánh đúng nghĩa vụ còn hiệu lực.

**Why this priority**: Tránh invoice "Unpaid" trên booking đã Cancelled; align với FR-04 modify/cancel rules.

**Independent Test**: Booking Cancelled → unpaid invoices Cancelled; paid deposit invoice giữ Paid (refund xử lý FR-12); booking modify tăng giá → remaining invoice amount cập nhật hoặc invoice điều chỉnh mới.

**Acceptance Scenarios**:

1. **Given** booking **Cancelled** (timeout, customer cancel, manager cancel), **When** hủy hoàn tất, **Then** mọi invoice **Unpaid/Pending Payment** → **Cancelled**; invoice **Paid** giữ nguyên trạng thái Paid.
2. **Given** booking **Confirmed** được sửa ngày/phòng làm thay đổi TotalAmount (FR-04), **When** modify thành công, **Then** **remaining** invoice amount cập nhật theo RemainingAmount mới; deposit invoice Paid **không** đổi amount (đã snapshot thanh toán).
3. **Given** remaining invoice Unpaid, **When** thanh toán phần còn lại thành công (FR-12), **Then** remaining invoice → **Paid**.

---

### Edge Cases

- Booking **Pending Deposit** hết hold timeout → booking Cancelled; cả hai invoice Unpaid → **Cancelled**.
- Deposit **Paid** nhưng booking **Cancelled** theo chính sách hủy → deposit invoice vẫn **Paid**; refund xử lý FR-12, không thuộc FR-11.
- **Partial payment** không được hỗ trợ v1 — invoice Paid chỉ khi full amount được xác nhận (assumption align FR-04 40/60 split).
- Remaining invoice **due** trước hoặc tại check-in (assumption: dueDate = check-in date); quá hạn hiển thị badge Overdue nhưng không auto-cancel booking v1.
- **Damage fee** invoice — **ngoài phạm vi FR-11 v1**; FR-12/FR-23 tạo khi damage approved.
- Admin **read-only** toàn hệ thống (RBAC) — không yêu cầu Admin UI riêng cho invoice v1.
- Employee — không quyền billing (RBAC `-`).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST **tự động tạo** hai hóa đơn per booking khi booking vào trạng thái **Pending Deposit**: **Deposit (40%)** và **Remaining Balance (60%)**, amount lấy từ snapshot booking tại thời điểm tạo.
- **FR-002**: Hệ thống MUST đảm bảo **một cặp** deposit + remaining invoice **per booking** (idempotent).
- **FR-003**: Invoice amount MUST **immutable** sau khi phát hành, **trừ** remaining invoice khi booking Confirmed được modify làm thay đổi RemainingAmount (FR-004).
- **FR-004**: Khi booking Confirmed modify làm thay đổi RemainingAmount, hệ thống MUST cập nhật **remaining invoice** chưa Paid theo số dư mới; deposit invoice đã Paid MUST NOT đổi amount.
- **FR-005**: Invoice MUST có lifecycle status: **Unpaid**, **Pending Payment**, **Paid**, **Cancelled**; optional display **Overdue** khi quá dueDate và chưa Paid.
- **FR-006**: Hệ thống MUST cập nhật invoice status khi FR-12 xác nhận thanh toán thành công (**Paid**) hoặc bắt đầu/chờ xử lý (**Pending Payment**).
- **FR-007**: Khi booking **Cancelled**, hệ thống MUST chuyển mọi invoice Unpaid/Pending Payment → **Cancelled**.
- **FR-008**: **Customer** MUST xem invoice breakdown trên Booking Detail (SCR-18) và danh sách billing trên Payment History (SCR-26) — chỉ dữ liệu của mình.
- **FR-009**: **Manager** MUST xem invoice breakdown trên Manager Booking Detail (SCR-35) và lọc invoice/payment pending trong Payment Management (SCR-36) — scoped theo **Property được gán**.
- **FR-010**: Hệ thống MUST **từ chối** Customer truy cập invoice không thuộc customerId; Manager truy cập ngoài property scope.
- **FR-011**: Hệ thống MUST ghi **ActivityLog** khi invoice tạo, chuyển Paid, Cancelled, hoặc remaining amount điều chỉnh (INVOICE_ISSUED, INVOICE_PAID, INVOICE_CANCELLED, INVOICE_ADJUSTED).
- **FR-012**: FR-11 MUST **không** xử lý cổng thanh toán, upload biên lai, duyệt chuyển khoản, đối soát VNPay — thuộc FR-12.

### Key Entities

- **Invoice**: id, bookingId, customerId, type (Deposit | Remaining Balance), amount (snapshot), status (Unpaid | Pending Payment | Paid | Cancelled), dueDate, paidAt, createdAt, updatedAt. Một booking có đúng một invoice Deposit và một invoice Remaining Balance (v1).
- **Booking** (read/trigger): totalAmount, depositAmount, remainingAmount, status — FR-04 owns writes; FR-11 listens on create/modify/cancel.
- **Payment** (read/link only): FR-12 owns Payment records; mỗi Payment MUST reference invoiceId khi thanh toán — FR-11 consumes payment-confirmed events to update invoice status.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: **100%** booking Pending Deposit mới có đúng **2 invoice** (deposit + remaining) trong **dưới 5 giây** sau khi booking lưu thành công.
- **SC-002**: **100%** invoice amount khớp snapshot booking (40/60 split) tại thời điểm phát hành — **0** lệch amount trong kiểm thử mẫu.
- **SC-003**: **100%** payment thành công (FR-12) cập nhật invoice tương ứng → **Paid** trong **dưới 10 giây**.
- **SC-004**: Customer xem Payment Breakdown trên SCR-18 hoàn tất trong **dưới 15 giây** trên kết nối bình thường.
- **SC-005**: **100%** truy cập invoice ngoài scope (Customer khác / Manager property khác) bị từ chối.
- **SC-006**: **100%** booking Cancelled có mọi invoice chưa Paid chuyển **Cancelled** đồng bộ với booking cancel.
- **SC-007**: **0** invoice trùng per booking per type (idempotency test).

## Assumptions

- Invoice là **billing document** (nghĩa vụ thanh toán); **Payment** (FR-12) là giao dịch thực thi thanh toán liên kết invoiceId — tách rõ trách nhiệm FR-11 vs FR-12.
- Deposit invoice due ngay khi booking Pending Deposit; remaining invoice dueDate = **check-in date** (có thể thanh toán sớm hơn).
- **Overdue** là trạng thái hiển thị (derived) khi dueDate < today và status chưa Paid — không chặn check-in tự động (check-in gate thuộc FR-04: CHECKIN_DENIED_UNPAID).
- Không có màn hình Invoice riêng v1 — embed trong SCR-18, SCR-26, SCR-35, SCR-36 theo screendesign Payment Breakdown.
- Damage fee invoice, refund invoice — **FR-12**; FR-11 v1 chỉ deposit + remaining.
- Admin read global không có UI billing riêng v1.
- Currency VND; amount là số nguyên VND (assumption align booking pricing).
- Booking modify chỉ điều chỉnh remaining invoice khi deposit chưa Paid hoặc đã Paid — nếu cả hai chưa Paid và TotalAmount đổi, **cả hai** invoice Unpaid cập nhật amount (edge case rare trước deposit pay); nếu deposit đã Paid chỉ remaining cập nhật.
