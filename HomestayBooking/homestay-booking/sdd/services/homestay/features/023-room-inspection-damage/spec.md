# Feature Specification: FR-23 Room Inspection & Damage Resolution

**Feature Branch**: `025-room-inspection-damage`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-23 Room Inspection & Damage Resolution — dựa vào docs (Specification_v2.md § FR-23, §5 RoomInspection/DamageReport/DamageItem, §6 RBAC, §10 Room Inspection acceptance, api-spec-by-screen SCR-42/43/53/62/63/64, screen.md, screendesign.md, entity-ui-mapping.md)"

**Phụ thuộc**: FR-01 (auth all roles); FR-04 (booking check-out lifecycle); FR-06 (Manager property scope); FR-08 (Room status); FR-12 (Damage Fee payment); FR-15 (notification/email on damage); FR-20 (Employee property assignment); FR-22 (Employee Dashboard hiển thị inspection — read-only). **Ranh giới**: FR-23 owns **RoomInspection**, **DamageReport**, **DamageItem** + SCR-42/43/53/62/63/64; FR-04 owns check-in/check-out UI nhưng **gate** inspection/payment thuộc FR-23 rules; FR-21 auto Housekeeping **sau** checkout thành công; FR-10 Contract Addendum cho Damage Fee **P2** (v1: cập nhật Remaining Balance booking).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Employee thực hiện kiểm tra phòng trước Check-out (Priority: P1)

Là **Employee** được gán Property, tôi muốn **kiểm tra phòng** cho booking sắp check-out bằng checklist (TV, Minibar, Bed, Bathroom) và đánh dấu **Passed** hoặc **Failed**, để xác định phòng có hư hại trước khi khách rời đi (SCR-62).

**Why this priority**: FR-23 core — Room Inspection là điều kiện bắt buộc trước check-out.

**Independent Test**: Employee mở SCR-62 → chọn phòng check-out hôm nay → hoàn thành checklist → Pass → inspection status **Passed**; booking đủ điều kiện check-out (không có damage).

**Acceptance Scenarios**:

1. **Given** booking **Checked-in** với check-out hôm nay thuộc Property Employee được gán, **When** Employee mở SCR-62, **Then** thấy phòng/booking trong danh sách cần kiểm tra.
2. **Given** Employee mở checklist phòng, **When** đánh dấu tất cả mục OK và chọn **Pass**, **Then** RoomInspection chuyển **Passed** và `inspectedAt` được ghi nhận.
3. **Given** Employee phát hiện vấn đề, **When** chọn **Fail**, **Then** inspection chuyển **Failed With Damage** và hệ thống gợi ý tạo Damage Report (SCR-64).
4. **Given** Employee Property B, **When** cố kiểm tra phòng Property A, **Then** từ chối (`UNAUTHORIZED_PROPERTY_ACCESS`).
5. **Given** inspection đã **Passed**, **When** Employee cố sửa kết quả, **Then** từ chối (assumption: terminal — cần Manager override hiếm, out of scope v1).

---

### User Story 2 - Employee ghi nhận Damage Report và DamageItem (Priority: P1)

Là **Employee**, khi inspection **Failed With Damage**, tôi muốn **tạo Damage Report** kèm danh sách **DamageItem** (tên hư hại, mô tả, chi phí ước tính, ảnh minh họa), để Manager xem xét phê duyệt phí bồi thường (SCR-63/64).

**Why this priority**: FR-23 "Employee ghi nhận DamageItem"; không có damage report thì không có damage fee flow.

**Independent Test**: Sau Fail inspection → Employee tạo report với 2 items + ảnh → report status **Pending Approval**; hiển thị trong SCR-63.

**Acceptance Scenarios**:

1. **Given** RoomInspection **Failed With Damage** chưa có report, **When** Employee submit Damage Report với ít nhất 1 DamageItem (tên + estimated cost), **Then** tạo DamageReport status **Pending Approval** gắn `inspectionId`, `bookingId`.
2. **Given** Employee upload ảnh hợp lệ cho item, **When** submit, **Then** ảnh lưu dưới dạng Attachment liên kết DamageItem/DamageReport.
3. **Given** Employee submit, **When** thành công, **Then** `totalEstimatedCost` = tổng `estimatedCost` các items.
4. **Given** Employee **không** thuộc Property của booking, **When** cố tạo report, **Then** từ chối.
5. **Given** Customer hoặc Manager, **When** cố tạo report thay Employee, **Then** từ chối (RBAC: Employee Create).

---

### User Story 3 - Check-out bị chặn cho đến inspection hoàn tất và Damage Fee thanh toán (Priority: P1)

Là **hệ thống**, tôi muốn **chặn check-out** khi Room Inspection chưa hoàn tất hoặc còn Damage Fee chưa thanh toán, và **cho phép check-out** chỉ khi inspection **Passed** (hoặc damage đã approved + paid), để đảm bảo quy trình vận hành (FR-04 integration).

**Why this priority**: FR-23 explicit gate; FR-21 housekeeping phụ thuộc checkout hợp lệ.

**Independent Test**: Booking Checked-in → cố check-out khi inspection Pending → rejected; sau Passed + payments settled → check-out succeeds.

**Acceptance Scenarios**:

1. **Given** booking **Checked-in** chưa có inspection hoặc inspection **Pending/In Progress**, **When** Customer hoặc Manager cố check-out, **Then** từ chối — inspection chưa hoàn tất.
2. **Given** inspection **Passed** và không có damage fee, **When** remaining balance đã thanh toán, **Then** check-out thành công (FR-04).
3. **Given** DamageReport **Approved** với `approvedAmount` > 0, **When** Damage Fee payment chưa **Completed**, **Then** check-out bị chặn.
4. **Given** Damage Fee đã thanh toán đủ, **When** check-out, **Then** booking chuyển **Checked-out**; FR-21 có thể tạo HousekeepingTask (precondition upstream).
5. **Given** inspection **Failed With Damage** nhưng chưa có DamageReport, **When** cố check-out, **Then** từ chối.

---

### User Story 4 - Manager xem inspection và phê duyệt Damage Fee (Priority: P1)

Là **Manager** được gán Property, tôi muốn **xem danh sách Room Inspection** và **phê duyệt Damage Report** với số tiền bồi thường, để áp dụng Segregation of Duties trước khi thu phí khách (SCR-42/43).

**Why this priority**: FR-23 "Manager duyệt Damage Fee"; Manager không tự thu tiền không qua quy trình.

**Independent Test**: Manager mở SCR-43 → xem report + ảnh → approve fee 4M → status Approved; Damage Fee cộng vào Remaining Balance.

**Acceptance Scenarios**:

1. **Given** Manager Property A, **When** mở SCR-42 với `propertyId` = A, **Then** chỉ thấy RoomInspection thuộc Property A.
2. **Given** DamageReport **Pending Approval**, **When** Manager xem SCR-43 drawer, **Then** thấy DamageItems, ảnh, `totalEstimatedCost`.
3. **Given** Manager approve với `fee` ≤ 5.000.000 VNĐ (assumption: configurable threshold default 5M), **When** submit, **Then** report **Approved**, `approvedAmount` lưu, `approvedBy` = Manager; Damage Fee cộng vào Remaining Balance booking.
4. **Given** Manager approve với `fee` > 5.000.000 VNĐ, **When** submit, **Then** report chuyển **Escalated** (assumption: status ESCALATED), `requiresAdminEscalation` = true; **chưa** tạo Damage Fee payment cho đến Admin co-approve.
5. **Given** Manager Property B, **When** cố approve report Property A, **Then** từ chối.

---

### User Story 5 - Admin đồng phê duyệt Damage Fee vượt ngưỡng (Priority: P1)

Là **Admin**, tôi muốn **xem và đồng phê duyệt** các Damage Report bị escalate (> 5M VNĐ), để kiểm soát chéo khoản bồi thường lớn (SCR-53).

**Why this priority**: FR-23 Segregation of Duties — Admin co-approval bắt buộc khi vượt ngưỡng.

**Independent Test**: Report Escalated 6M → Admin co-approve → Approved + Damage Fee vào balance; Customer notified.

**Acceptance Scenarios**:

1. **Given** DamageReport status **Escalated**, **When** Admin mở SCR-53, **Then** thấy danh sách report cần co-approve.
2. **Given** Admin co-approve với `approvedFee`, **When** submit, **Then** report **Approved**, `adminApproverId` lưu; Damage Fee cộng vào Remaining Balance.
3. **Given** Manager đã escalate, **When** Admin chưa co-approve, **Then** Customer không thể thanh toán Damage Fee / check-out.
4. **Given** Admin, **When** cố approve report ≤ 5M chưa escalate, **Then** từ chối hoặc read-only (assumption: Manager-only path for sub-threshold).
5. **Given** co-approve thành công, **When** hoàn tất, **Then** Customer nhận thông báo Damage Fee (FR-15).

---

### User Story 6 - Customer phản đối Damage Fee trong 24 giờ (Priority: P1)

Là **Customer** bị tính Damage Fee, tôi muốn **Dispute** (phản đối) khoản phí trong **24 giờ** sau khi Manager/Admin phê duyệt, để yêu cầu xem xét lại trước khi thanh toán bắt buộc.

**Why this priority**: FR-23 explicit Customer Dispute + escalate Admin; Segregation of Duties rule #2.

**Independent Test**: Report Approved → Customer Dispute trong 24h → status **Disputed** → escalate Admin; check-out vẫn blocked.

**Acceptance Scenarios**:

1. **Given** DamageReport **Approved** thuộc booking của Customer, **When** Customer mở thông báo/dashboard và chọn **Dispute** trong 24h, **Then** report chuyển **Disputed** và escalate Admin.
2. **Given** đã quá 24h kể từ approval, **When** Customer cố Dispute, **Then** từ chối — hết thời hạn phản đối.
3. **Given** report **Disputed**, **When** Customer cố thanh toán Damage Fee, **Then** tạm chặn cho đến Admin xử lý (assumption: Admin resolve dispute out of band hoặc adjust fee — P2 admin resolve UI).
4. **Given** Customer B, **When** cố Dispute report của Customer A, **Then** từ chối.
5. **Given** Dispute thành công, **When** hoàn tất, **Then** Admin nhận thông báo escalate (FR-15).

---

### User Story 7 - Manager đánh dấu Outstanding Debt khi khách từ chối trả phí (Priority: P2)

Là **Manager**, khi khách **rời đi sớm** và **từ chối** thanh toán Damage Fee tại chỗ, tôi muốn đánh dấu tài khoản Customer là **Outstanding Debt**, để chặn booking tương lai cho đến khi thanh toán xong (Off-site Collection).

**Why this priority**: FR-23 Off-site Collection; ít frequent hơn happy path thanh toán tại chỗ.

**Independent Test**: Approved damage + guest refused pay → Manager mark Outstanding Debt → Customer blocked from new booking.

**Acceptance Scenarios**:

1. **Given** DamageReport **Approved** với Damage Fee chưa thanh toán, **When** Manager xác nhận khách từ chối trả và chọn **Mark Outstanding Debt**, **Then** Customer account flag `outstandingDebt` = true (assumption: trên users/customers).
2. **Given** Customer có Outstanding Debt, **When** cố tạo booking mới, **Then** từ chối cho đến khi Damage Fee thanh toán.
3. **Given** Customer thanh toán đủ Damage Fee sau đó, **When** payment Completed, **Then** `outstandingDebt` cleared tự động (assumption).
4. **Given** Employee hoặc Customer, **When** cố mark Outstanding Debt, **Then** từ chối — chỉ Manager Property scope.
5. **Given** report chưa Approved, **When** cố mark debt, **Then** từ chối.

---

### Edge Cases

- Một booking chỉ có **một** RoomInspection duy nhất (Specification §8).
- Inspection tạo khi booking request check-out (FR-04 **Pending Inspection**) — FR-23 owns inspection entity lifecycle.
- Employee chưa được gán `inspectedBy` — assumption: Employee tự nhận task khi bắt đầu inspection (status In Progress) hoặc Manager assign trên SCR-42 P2.
- Ảnh DamageItem: giới hạn định dạng/dung lượng align FR-13 attachment rules.
- Damage Fee payment: FR-12 owns payment gateway; FR-23 owns fee amount trên booking.
- Contract Addendum (FR-10) cho damage — **P2**; v1 cập nhật `damageFeeAmount` / Remaining Balance trên booking.
- Timezone **Asia/Ho_Chi_Minh** cho dispute 24h window.
- Concurrent approve/dispute — optimistic locking hoặc status guard.
- Inspection **Passed** không tạo DamageReport.
- Customer Suspended — chặn trước Dispute (FR-01).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Employee MUST thực hiện Room Inspection trước check-out cho booking Checked-in thuộc Property được gán (SCR-62).
- **FR-002**: RoomInspection MUST hỗ trợ status: **Pending**, **In Progress**, **Passed**, **Failed With Damage**.
- **FR-003**: Employee MUST có thể đánh dấu inspection **Passed** hoặc **Failed With Damage** qua checklist.
- **FR-004**: Employee MUST tạo DamageReport với một hoặc nhiều **DamageItem** khi có hư hại (SCR-64).
- **FR-005**: DamageItem MUST gồm tên, mô tả, chi phí ước tính; MAY có ảnh đính kèm.
- **FR-006**: Hệ thống MUST **chặn check-out** khi Room Inspection chưa hoàn tất (chưa Passed và chưa xử lý xong damage nếu Failed).
- **FR-007**: Manager MUST xem danh sách Room Inspection theo Property (SCR-42).
- **FR-008**: Manager MUST phê duyệt DamageReport với `approvedAmount` (SCR-43).
- **FR-009**: Nếu `approvedAmount` > ngưỡng cấu hình (mặc định **5.000.000 VNĐ**), hệ thống MUST escalate và yêu cầu **Admin co-approve** trước khi áp dụng phí.
- **FR-010**: Admin MUST đồng phê duyệt Damage Report escalated (SCR-53).
- **FR-011**: Sau khi Approved, Damage Fee MUST được cộng vào **Remaining Balance** của Booking.
- **FR-012**: Check-out MUST hoàn tất chỉ khi mọi thanh toán bao gồm **Damage Fee** đã xong (FR-12 integration).
- **FR-013**: Customer MUST có quyền **Dispute** Damage Fee trong **24 giờ** sau approval; Dispute MUST escalate lên Admin.
- **FR-014**: Manager MUST có thể đánh dấu Customer **Outstanding Debt** khi khách từ chối trả Damage Fee (Off-site Collection) — P2.
- **FR-015**: Outstanding Debt MUST chặn Customer tạo booking mới cho đến khi thanh toán xong.
- **FR-016**: Mọi thao tác MUST tuân **property scope** (Manager/Employee) và **booking ownership** (Customer).
- **FR-017**: Customer/Employee MUST nhận thông báo tại các mốc: damage report created, approved, disputed (FR-15 integration).

### Key Entities

- **RoomInspection**: booking, room, property, inspectedBy (Employee), status, checklist/note, inspectedAt.
- **DamageReport**: inspection, booking, totalEstimatedCost, approvedAmount, approvedBy, requiresAdminEscalation, adminApproverId, status (Draft, Pending Approval, Approved, Escalated, Disputed, Paid).
- **DamageItem**: report, itemName, description, estimatedCost; attachments.
- **Booking** (FR-04): damageFeeAmount, remaining balance, check-out gate.
- **Payment** (FR-12): Damage Fee payment type.
- **User/Customer** (FR-01/09): outstandingDebt flag (P2).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Employee hoàn thành Room Inspection (Pass) trong **dưới 5 phút** trên mobile (95% sessions).
- **SC-002**: **100%** check-out attempts khi inspection chưa Passed bị chặn với thông báo rõ ràng.
- **SC-003**: **100%** Damage Fee > 5M VNĐ yêu cầu Admin co-approve trước khi áp dụng vào balance.
- **SC-004**: Customer có thể submit Dispute trong 24h trong **dưới 2 phút** từ thông báo.
- **SC-005**: Sau approval + payment, check-out thành công và trigger FR-21 housekeeping trong **100%** happy-path test cases.
- **SC-006**: **100%** truy cập inspection/damage ngoài property scope bị từ chối.
- **SC-007**: Manager phê duyệt damage report và xem ảnh trong **dưới 3 phút** (p95).

## Assumptions

- **Screens**: SCR-62/63/64 (Employee), SCR-42/43 (Manager), SCR-53 (Admin).
- **Routes** (assumption): `/employee/inspections`, `/employee/damage-reports`, `/employee/damage-reports/new`, `/manager/inspections`, `/manager/damage-reports`, `/admin/damage-escalation`.
- **API paths** align `api-spec-by-screen.md`: employee room-inspections, damage-reports; manager room-inspections, damage-reports approve; admin co-approve.
- **Escalation threshold**: 5.000.000 VNĐ configurable via system settings (FR-17) — default hardcoded v1 nếu settings chưa có.
- **Dispute window**: 24h từ `approvedAt` timestamp, timezone Asia/Ho_Chi_Minh.
- **Inspection creation**: auto when booking enters check-out request / Pending Inspection state (FR-04); one inspection per booking.
- **DamageReport status Escalated**: maps api-spec ESCALATED; awaiting Admin co-approve.
- **Admin dispute resolution UI**: v1 minimal — Admin notified; fee adjustment manual P2; Dispute blocks payment until resolved.
- **FR-10 Contract Addendum**: P2 — v1 updates booking balance only.
- **Notifications/email**: FR-15 emits events; FR-23 owns business triggers.
- **Outstanding Debt**: boolean on customer profile; cleared on Damage Fee payment Completed.
