# Feature Specification: FR-10 Contract Management

**Feature Branch**: `012-contract-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-10 Contract Management — dựa vào docs (Specification_v2.md § FR-10, §5 Contract/OutboxEvent, §10 Contract Management acceptance, api-spec-by-screen SCR-21/SCR-38, screen.md, screendesign.md, entity-ui-mapping.md §1.7, frontend contractApi.ts, ContractMgmtListPage, ContractListPage)"

**Phụ thuộc**: FR-04 (Booking → Confirmed on deposit); FR-12 (deposit payment success); FR-01 (Customer email); FR-06/08 (property/room snapshot data for PDF). **Ranh giới**: FR-11 Billing invoices; FR-12 payment processing/reconciliation; FR-15 notification engine (Outbox infra may be shared but contract content owned here); Damage Fee **Addendum** generation detail coupled to FR-23 damage approval — v1 stub/link only if damage flow not ready.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hệ thống tự động tạo và gửi hợp đồng khi đặt cọc thành công (Priority: P1)

Là **hệ thống**, khi **deposit payment** của booking được xác nhận (VNPay tự động hoặc Manager duyệt chuyển khoản), tôi muốn tạo **Accommodation Contract PDF** bất biến và gửi email cho Customer qua **Outbox pattern** để khách nhận hợp đồng đúng thời điểm xác nhận đặt phòng.

**Why this priority**: FR-10 core — "Tự động tạo PDF khi đặt cọc thành công"; FR-04 side-effect; không có contract thì US2/US3 trống.

**Independent Test**: Booking chuyển Confirmed sau deposit → trong vài phút contract record tồn tại với pdfUrl, status Active, sentAt; Customer nhận email; không tạo trùng khi event replay.

**Acceptance Scenarios**:

1. **Given** booking ở trạng thái cho phép xác nhận deposit, **When** deposit payment thành công, **Then** booking → **Confirmed** (FR-04) và Outbox event `CONTRACT_GENERATE` được enqueue.
2. **Given** Outbox worker xử lý event, **When** generate hoàn tất, **Then** Contract lưu snapshot (bookingId, customerId, roomId, dates, depositAmount, totalAmount, pdfUrl, generatedAt) và status **Active**.
3. **Given** contract vừa tạo, **When** email worker chạy, **Then** Customer nhận email có link/PDF attachment và `sentAt` được cập nhật.
4. **Given** contract đã tồn tại cho booking, **When** cùng deposit-success event replay, **Then** **không** tạo contract trùng (idempotent theo bookingId).
5. **Given** PDF generation thất bại, **When** retry Outbox, **Then** worker retry theo policy; contract không ở trạng thái Active cho đến khi PDF sẵn sàng (assumption: status Pending Generation hoặc retry until success).

---

### User Story 2 - Customer xem, tải và in hợp đồng của mình (Priority: P1)

Là **Customer**, tôi muốn xem danh sách hợp đồng đặt phòng, mở xem PDF và tải/in để lưu trữ pháp lý cá nhân.

**Why this priority**: FR-10 "Xem danh sách và chi tiết"; "Tải PDF"; "In hợp đồng"; SCR-21.

**Independent Test**: Customer mở SCR-21 → chỉ hợp đồng của mình; click row → Drawer PDF viewer; Download và Print hoạt động.

**Acceptance Scenarios**:

1. **Given** Customer có ít nhất một contract Active/Completed, **When** mở My Contracts (SCR-21), **Then** danh sách hiển thị Booking ID, Room, dates, status, link PDF.
2. **Given** Customer chọn một hợp đồng, **When** mở chi tiết/drawer, **Then** hiển thị PDF viewer (iframe hoặc tương đương) read-only.
3. **Given** Customer trên chi tiết, **When** chọn Download, **Then** nhận file PDF gốc đã generate (immutable snapshot).
4. **Given** Customer trên chi tiết, **When** chọn Print, **Then** trình duyệt in nội dung hợp đồng.
5. **Given** Customer A, **When** cố truy cập contract của Customer B, **Then** từ chối.

---

### User Story 3 - Manager xem hợp đồng và gửi lại email (Priority: P1)

Là **Manager**, tôi muốn xem danh sách hợp đồng thuộc Property được gán, xem chi tiết PDF, tải/in và **gửi lại email** cho Customer khi khách không nhận được.

**Why this priority**: FR-10 Manager resend; SCR-38 Contract Management; Manager R trên Contract trong RBAC §6.

**Independent Test**: Manager mở SCR-38 → filter property → xem drawer PDF → Resend email → sentAt cập nhật; không thấy contract property khác.

**Acceptance Scenarios**:

1. **Given** Manager được gán property P, **When** mở Contract Management, **Then** chỉ hợp đồng booking thuộc P hiển thị.
2. **Given** danh sách hợp đồng, **When** xem, **Then** cột Contract ID, Booking ID, Guest, dates, status hiển thị đúng (SCR-38).
3. **Given** Manager mở chi tiết, **When** xem PDF, **Then** cùng immutable snapshot — **không** chỉnh sửa nội dung hợp đồng gốc.
4. **Given** Manager chọn "Send to Guest Email" / Resend, **When** xác nhận, **Then** email queue qua Outbox; `sentAt` cập nhật; **không** regenerate PDF gốc.
5. **Given** Manager property không được gán, **When** truy cập contract thuộc property khác, **Then** từ chối.

---

### User Story 4 - Contract Addendum khi phát sinh Damage Fee (Priority: P2)

Là **hệ thống**, khi Manager phê duyệt **Damage Fee** sau inspection, tôi muốn tạo **Contract Addendum** (phụ lục) riêng thay vì sửa hợp đồng gốc để tuân thủ immutable snapshot.

**Why this priority**: FR-10 explicit Addendum rule; có thể triển khai sau core PDF v1 nếu damage flow (FR-23) chưa sẵn sàng.

**Independent Test**: Damage fee approved → Addendum PDF linked to original contract; original contract pdfUrl unchanged.

**Acceptance Scenarios**:

1. **Given** contract Active cho booking, **When** damage fee được phê duyệt (FR-23/downstream), **Then** hệ thống tạo **Addendum** record linked to parent contractId.
2. **Given** Addendum created, **When** Customer/Manager xem contract detail, **Then** hiển thị danh sách addendum read-only; hợp đồng gốc không đổi.
3. **Given** chưa có damage fee, **When** xem contract, **Then** không có addendum.

---

### Edge Cases

- Booking **Cancelled** sau khi contract Active → contract status → **Cancelled**; PDF gốc vẫn lưu read-only (audit).
- Booking **Checked-out** hoàn tất → contract status → **Completed** (assumption v1 lifecycle).
- Email Customer bounce/fail → Outbox retry; Manager có thể resend thủ công (US3).
- Deposit success nhưng room/property INACTIVE — contract vẫn snapshot data tại thời điểm generate.
- Google-only Customer — email gửi tới email tài khoản.
- In PDF trên mobile — browser print; UX degradation acceptable v1.
- Admin read-only global — có thể xem mọi contract (RBAC §6) nhưng FR-10 không yêu cầu Admin UI riêng (assumption: Admin dùng báo cáo downstream hoặc future screen).
- Employee — không quyền Contract (RBAC `-`).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST **tự động tạo** Accommodation Contract **PDF** khi **deposit payment thành công** (VNPay hoặc Manager approve bank transfer), qua **Outbox pattern**.
- **FR-002**: Hệ thống MUST **gửi email** hợp đồng tới Customer sau khi PDF sẵn sàng; ghi `sentAt`.
- **FR-003**: Contract MUST là **immutable snapshot** — lưu booking/room/customer/amount/dates tại thời điểm tạo; **không** update PDF gốc khi giá phòng thay đổi sau đó.
- **FR-004**: Hệ thống MUST đảm bảo **một** contract gốc **per booking** (idempotent generation).
- **FR-005**: **Customer** MUST xem **danh sách** hợp đồng của mình (SCR-21) với phân trang.
- **FR-006**: **Customer** MUST **xem chi tiết**, **tải PDF**, và **in** hợp đồng thuộc sở hữu mình.
- **FR-007**: **Manager** MUST xem **danh sách** hợp đồng scoped theo **Property được gán** (SCR-38).
- **FR-008**: **Manager** MUST xem chi tiết, **tải**, **in** hợp đồng trong scope.
- **FR-009**: **Manager** MUST có thể **yêu cầu gửi lại email** hợp đồng (resend) — queue Outbox, **không** regenerate PDF gốc.
- **FR-010**: Damage Fee phát sinh MUST tạo **Contract Addendum** riêng — **không** sửa contract gốc (P2 / when damage flow available).
- **FR-011**: Contract status MUST reflect lifecycle: **Active**, **Completed**, **Cancelled** (§5).
- **FR-012**: Hệ thống MUST **từ chối** Customer truy cập contract không thuộc customerId; Manager truy cập ngoài property scope.
- **FR-013**: Resend và generate events MUST ghi **ActivityLog** (CONTRACT_GENERATED, CONTRACT_EMAIL_SENT, CONTRACT_RESENT).

### Key Entities

- **Contract**: id, bookingId, customerId, roomId, checkInDate, checkOutDate, depositAmount, totalAmount, pdfUrl, generatedAt, sentAt, status (Active | Completed | Cancelled), createdAt, updatedAt.
- **ContractAddendum** (P2): id, parentContractId, bookingId, damageFeeAmount, pdfUrl, generatedAt, sentAt — linked to damage approval.
- **OutboxEvent**: eventType (CONTRACT_GENERATE, CONTRACT_EMAIL_SEND, CONTRACT_RESEND), payload JSON, status PENDING/PROCESSED/FAILED — shared infra, FR-10 owns contract payload schema.
- **Booking** (read/trigger): status transition to Confirmed triggers generation — FR-04 owns writes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: **100%** deposit-success bookings nhận contract record trong **dưới 5 phút** (async Outbox SLA v1).
- **SC-002**: **100%** contract emails gửi thành công hoặc retry cho đến max retry (không mất event).
- **SC-003**: Customer mở và tải PDF hợp đồng hoàn tất trong **dưới 30 giây** trên kết nối bình thường.
- **SC-004**: **0** contract trùng per booking (idempotency test).
- **SC-005**: **100%** Manager resend trong scope thành công cập nhật sentAt trong **dưới 3 phút**.
- **SC-006**: **100%** truy cập contract ngoài scope (Customer khác / Manager property khác) bị từ chối.
- **SC-007**: Sau damage approval (when enabled), **100%** addendum tạo mà **0** thay đổi pdfUrl contract gốc.

## Assumptions

- Trigger deposit success align FR-04: VNPay IPN **hoặc** Manager approve bank transfer (SCR-37) — cả hai enqueue `CONTRACT_GENERATE`.
- PDF template v1: tiếng Việt, logo property, snapshot guest/room/dates/amounts — chi tiết layout thuộc plan; spec chỉ yêu cầu immutable content.
- Storage pdfUrl: local `/uploads/contracts/` v1 hoặc object storage — plan decides; spec requires durable URL.
- SCR-21 Customer = `GET /api/v1/contracts/me`; SCR-38 Manager = `GET /api/v1/manager/contracts` per api-spec-by-screen.md (frontend legacy `/api/contracts` migrate in plan).
- **Print** = browser print dialog on PDF viewer — no server-side print.
- Contract **Completed** khi booking Checked-out; **Cancelled** khi booking Cancelled — sync via booking status listener hoặc batch v1.
- Addendum (US4) **deferred** to P2 if FR-23 damage flow not implemented — core MVP = US1–US3.
- Admin Contract read (RBAC) without dedicated Admin UI in FR-10 v1.
- Outbox worker polling interval và max retry theo platform standard (assumption: 15s poll, 5 retries).
