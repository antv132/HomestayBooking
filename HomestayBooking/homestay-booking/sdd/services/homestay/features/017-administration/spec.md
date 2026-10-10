# Feature Specification: FR-17 Administration

**Feature Branch**: `019-administration`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-17 Administration — dựa vào docs (Specification_v2.md § FR-17, §5 Complaint/SystemSetting/ActivityLog, §8 Administration acceptance, api-spec-by-screen SCR-54/56, screen.md, screendesign.md, entity-ui-mapping.md §1.15, figma SCR-63/64 Content Moderation, frontend complaintsApi.ts, CustomerComplaintPages.tsx, ComplaintListPage.tsx)"

**Phụ thuộc**: FR-01 (auth RBAC Admin; Customer login block khi Suspended — liên quan FR-09); FR-02 (Customer profile read-only trong complaint context); FR-14 (Review moderation data + `ReviewModerationService` — FR-17 owns Admin **Content Moderation** tab SCR-56); FR-15 (Outbox failure monitor tab trong SCR-56 — optional integration); FR-09 (**owns** Customer Directory SCR-51 Active/Suspend — **không** duplicate trong FR-17). **Ranh giới**: FR-17 owns **Complaint** lifecycle (Customer create + Admin resolve), **System Settings**, **Activity Logs** (read), **SCR-56** admin hub tabs; FR-18 Promotion **out of scope**; Manager complaint routes trong frontend hiện tại **sai actor** — chuẩn hóa Admin SCR-54.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customer gửi và theo dõi khiếu nại (Priority: P1)

Là **Customer**, tôi muốn **tạo khiếu nại** (tiêu đề + mô tả) và **xem danh sách khiếu nại** của mình cùng trạng thái xử lý, để được hỗ trợ khi gặp vấn đề trong quá trình lưu trú.

**Why this priority**: FR-17 explicit "Customer có thể tạo khiếu nại"; prerequisite cho Admin resolution flow.

**Independent Test**: Customer submit complaint → appears in My Complaints with OPEN; view detail with status updates after Admin acts.

**Acceptance Scenarios**:

1. **Given** Customer đã đăng nhập, **When** gửi khiếu nại với `subject` và `description` hợp lệ, **Then** tạo Complaint status **Open** gắn Customer; hiển thị trong danh sách của Customer.
2. **Given** Customer có khiếu nại, **When** mở My Complaints, **Then** thấy `subject`, `status`, `createdAt` — **chỉ của mình**.
3. **Given** Customer A, **When** cố xem khiếu nại của Customer B, **Then** từ chối.
4. **Given** `subject` hoặc `description` trống/quá ngắn, **When** submit, **Then** validation error; không tạo complaint.
5. **Given** Customer Suspended (FR-09), **When** cố tạo khiếu nại, **Then** từ chối đăng nhập trước — không tới bước complaint (FR-01).

---

### User Story 2 - Admin quản lý và giải quyết khiếu nại (Priority: P1)

Là **Admin**, tôi muốn xem **danh sách khiếu nại** toàn hệ thống, mở chi tiết, cập nhật trạng thái và ghi **ghi chú giải quyết**, để xử lý phản ánh khách hàng (SCR-54).

**Why this priority**: FR-17 core Admin complaint management; workflow Open → Investigating → Resolved → Closed.

**Independent Test**: Admin list all complaints → open drawer/detail → move OPEN to INVESTIGATING → RESOLVED with notes → CLOSED; Customer sees updated status.

**Acceptance Scenarios**:

1. **Given** Admin, **When** mở Complaint Management (SCR-54), **Then** thấy bảng: mã/id, Customer, Subject, Status, ngày tạo — **toàn hệ thống**.
2. **Given** complaint status **Open**, **When** Admin chuyển **Investigating**, **Then** status cập nhật; Customer thấy trạng thái mới.
3. **Given** complaint **Investigating**, **When** Admin chuyển **Resolved** kèm `resolutionNotes` bắt buộc, **Then** lưu `resolutionNotes`, `resolvedAt`; Customer thấy nội dung giải quyết.
4. **Given** complaint **Resolved**, **When** Admin chuyển **Closed**, **Then** status **Closed**; không chỉnh sửa thêm (read-only hoặc chỉ ghi chú bổ sung — assumption: read-only after Closed).
5. **Given** chuyển trạng thái không hợp lệ (e.g. Closed → Open), **When** Admin cố cập nhật, **Then** từ chối.
6. **Given** Manager hoặc Customer, **When** cố truy cập Admin complaint APIs/UI, **Then** từ chối.

---

### User Story 3 - Admin cấu hình System Settings (Priority: P1)

Là **Admin**, tôi muốn xem và cập nhật **cấu hình hệ thống** (% đặt cọc, tên hệ thống, email hỗ trợ, thông tin tài khoản ngân hàng thụ hưởng), để vận hành nền tảng đúng chính sách kinh doanh (SCR-56 tab System Settings).

**Why this priority**: FR-17 explicit System Settings bullets; ảnh hưởng deposit flow FR-04/12.

**Independent Test**: Admin update deposit % → lưu thành công; giá trị mới áp dụng cho booking checkout tiếp theo (read by downstream features).

**Acceptance Scenarios**:

1. **Given** Admin, **When** mở System Settings tab, **Then** hiển thị các mục ít nhất: **deposit percentage**, **system name**, **support email**, **bank account number/name**, **bank name**.
2. **Given** Admin thay đổi deposit percentage hợp lệ (assumption: 10–50%), **When** Save, **Then** lưu `SystemSetting` với `updatedBy`, `updatedAt`; thông báo thành công.
3. **Given** deposit percentage ngoài ngưỡng hoặc email không hợp lệ, **When** Save, **Then** validation error; không lưu.
4. **Given** Manager/Customer, **When** cố truy cập settings, **Then** từ chối.
5. **Given** thay đổi settings, **When** lưu, **Then** ghi **ActivityLog** hành động cấu hình (assumption).

---

### User Story 4 - Admin xem Activity Logs (Priority: P1)

Là **Admin**, tôi muốn xem **nhật ký hoạt động** hệ thống với lọc theo thời gian, hành động và người dùng, để giám sát và kiểm tra (SCR-56 tab Activity Logs).

**Why this priority**: SCR-56 wireframe includes Activity Logs; audit trail for administration.

**Independent Test**: Admin opens Activity Logs → paginated list; filter by date range reduces results; read-only (no delete).

**Acceptance Scenarios**:

1. **Given** Admin, **When** mở Activity Logs tab, **Then** thấy danh sách: user, action, entity type/id, description, thời gian — sắp xếp mới nhất trước.
2. **Given** nhiều log entries, **When** lọc theo khoảng ngày, **Then** chỉ entries trong khoảng hiển thị.
3. **Given** Admin, **When** xem logs, **Then** **read-only** — không sửa/xóa log.
4. **Given** non-Admin, **When** cố truy cập, **Then** từ chối.
5. **Given** không có log, **When** mở tab, **Then** empty state thân thiện.

---

### User Story 5 - Admin kiểm duyệt đánh giá toàn hệ thống (Priority: P1)

Là **Admin**, tôi muốn **kiểm duyệt nội dung đánh giá** (ẩn/hiện review) trên tab **Content Moderation** trong System Administration, để xử lý review vi phạm trên mọi Property (SCR-56).

**Why this priority**: FR-17 "Admin kiểm duyệt nội dung (Đánh giá)"; complements FR-14 Manager property-scoped moderation.

**Independent Test**: Admin opens Content Moderation → sees all reviews → Hide Published review → status Hidden globally; Show restores Published.

**Acceptance Scenarios**:

1. **Given** Admin, **When** mở Content Moderation tab (SCR-56), **Then** thấy danh sách reviews **toàn hệ thống** với filter All/Published/Hidden.
2. **Given** review **Published**, **When** Admin Hide với xác nhận, **Then** status → **Hidden**; không còn công khai (FR-14).
3. **Given** review **Hidden**, **When** Admin Show, **Then** status → **Published**.
4. **Given** Manager, **When** cố dùng Admin global moderation API, **Then** từ chối (Manager dùng FR-14 scoped path).
5. **Given** Admin moderate, **When** thành công, **Then** ghi ActivityLog moderation action.

---

### User Story 6 - Admin hub System Administration (Priority: P2)

Là **Admin**, tôi muốn một **màn hình trung tâm** (SCR-56) với các tab Activity Logs, System Settings, Content Moderation (và giám sát Outbox thất bại từ FR-15 nếu có), để truy cập công cụ quản trị từ một nơi.

**Why this priority**: SCR-56 gộp tabs; navigation UX; P2 vì các tab có thể ship độc lập qua US3–US5.

**Independent Test**: Admin navigates to `/admin/system` → switches tabs without full page reload; each tab loads correct content.

**Acceptance Scenarios**:

1. **Given** Admin, **When** mở System Administration (SCR-56), **Then** thấy tabs: Activity Logs, System Settings, Content Moderation.
2. **Given** Admin trên tab Settings, **When** chuyển sang Activity Logs, **Then** nội dung tab tương ứng hiển thị; không mất session.
3. **Given** FR-15 Outbox monitor available, **When** Admin mở tab Outbox/Notifications (assumption P2), **Then** liên kết tới danh sách FAILED events (read-only + retry per FR-15).
4. **Given** Customer/Manager, **When** cố truy cập SCR-56, **Then** từ chối.

---

### Edge Cases

- Complaint trùng subject từ cùng Customer — cho phép nhiều complaint độc lập (assumption v1).
- Admin resolve không nhập resolutionNotes — bắt buộc khi chuyển Resolved (assumption).
- System Settings thay đổi deposit % khi có booking Pending Deposit — áp dụng cho booking **mới**; booking đang chờ giữ % tại thời điểm tạo (assumption).
- Activity log volume lớn — phân trang bắt buộc; retention theo chuẩn ngành (assumption: 12 tháng, không xóa v1).
- Customer complaint khi chưa có booking — vẫn cho phép gửi (assumption: general platform complaint).
- SMTP settings trong figma — **out of scope** FR-17 v1 nếu FR-10 owns email config; chỉ business-facing settings per FR-17 bullets.
- Promotion/banner — **FR-18**, không thuộc FR-17.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Customer MUST tạo Complaint với `subject` và `description` khi đã đăng nhập.
- **FR-002**: Customer MUST xem danh sách và chi tiết Complaint **của mình**.
- **FR-003**: Complaint MUST có lifecycle: **Open → Investigating → Resolved → Closed**.
- **FR-004**: Admin MUST xem danh sách Complaint **toàn hệ thống** phân trang (SCR-54).
- **FR-005**: Admin MUST cập nhật trạng thái Complaint theo workflow hợp lệ và ghi `resolutionNotes` khi **Resolved**.
- **FR-006**: Admin MUST cấu hình **deposit percentage**, **system name**, **support email**, **bank account** (number, name, bank name) qua System Settings.
- **FR-007**: System Settings changes MUST ghi nhận `updatedBy` (Admin) và `updatedAt`.
- **FR-008**: Admin MUST xem Activity Logs **read-only** với lọc theo ngày (SCR-56).
- **FR-009**: Admin MUST kiểm duyệt Review **toàn hệ thống** (Hide/Show) trên Content Moderation tab — dữ liệu do FR-14 own.
- **FR-010**: FR-17 MUST **từ chối** truy cập Admin functions cho non-Admin roles.
- **FR-011**: Customer MUST NOT xem/sửa Complaint của Customer khác.
- **FR-012**: SCR-56 MUST gộp tabs Activity Logs, System Settings, Content Moderation (hub).
- **FR-013**: Quản lý tài khoản Customer (Active/Suspend, SCR-51) thuộc **FR-09** — FR-17 **không** implement duplicate; Admin hub MAY link tới Customer Directory.
- **FR-014**: Promotion Management thuộc **FR-18** — out of scope FR-17.
- **FR-015**: Complaint status **Closed** MUST là trạng thái kết thúc — không reopen v1 (assumption).
- **FR-016**: Hệ thống MUST ghi ActivityLog cho settings update và complaint status changes (assumption).

### Key Entities

- **Complaint**: id, userId (Customer), subject, description, status (Open/Investigating/Resolved/Closed), resolutionNotes, resolvedAt, createdAt, updatedAt.
- **SystemSetting**: id, key, value, description, updatedBy, updatedAt — keys: DEPOSIT_PERCENTAGE, SYSTEM_NAME, SUPPORT_EMAIL, BANK_ACCOUNT_NUMBER, BANK_ACCOUNT_NAME, BANK_NAME (assumption v1).
- **ActivityLog**: id, userId, action, entityType, entityId, description, ipAddress, createdAt — read-only for Admin.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Customer hoàn tất gửi khiếu nại trong **dưới 2 phút** (95% sessions).
- **SC-002**: Admin xem danh sách complaint và mở chi tiết trong **dưới 3 giây** (95% requests).
- **SC-003**: **100%** chuyển trạng thái complaint không hợp lệ bị từ chối.
- **SC-004**: **100%** thay đổi System Settings bởi Admin được persist và hiển thị lại chính xác sau reload.
- **SC-005**: **100%** truy cập Admin administration APIs bởi Customer/Manager bị chặn.
- **SC-006**: Admin hoàn tất luồng Investigating → Resolved → Closed trong **dưới 5 phút** (90% cases).
- **SC-007**: Activity Logs filter theo 30 ngày trả kết quả trong **dưới 5 giây** (p95).
- **SC-008**: Admin global review moderation cập nhật trạng thái review trong **dưới 2 giây** (đồng bộ FR-14 public display).

## Assumptions

- FR-09 **owns** Customer Directory (SCR-51) — FR-17 bullet "quản lý tài khoản Customer" satisfied via FR-09 integration, not re-implemented.
- FR-14 **owns** Review entity và moderation service — FR-17 owns Admin UI tab only.
- FR-15 **owns** Outbox FAILED monitor — optional tab trong SCR-56 hub P2.
- Customer complaint UI: `/customer/complaints` (exists); Admin complaint: chuẩn hóa từ `/manager/complaints` → `/admin/complaints` SCR-54.
- `resolutionNotes` min 10 chars when status → Resolved (align maintenance/complaint patterns).
- `subject` min 5, max 200 chars; `description` min 20, max 2000 chars (assumption).
- Deposit percentage default **40%**; valid range 10–50%.
- cancelTimeoutHours có trong api-spec SCR-56 — include as optional SystemSetting key P2.
- Complaint không có file đính kèm v1 — text only.
- Email notification to Customer on status change — **optional P2** (FR-15); in-app optional.
