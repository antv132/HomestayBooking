# Feature Specification: FR-09 Customer Management

**Feature Branch**: `011-customer-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-09 Customer Management — dựa vào docs (Specification_v2.md § FR-09, §5 User, §10 Administration acceptance, api-spec-by-screen SCR-51, screen.md, screendesign.md, entity-ui-mapping.md § Admin Customer Directory)"

**Phụ thuộc**: FR-01 (User entity, role CUSTOMER, login block khi SUSPENDED); FR-04 (Booking history read). **Ranh giới**: FR-17 (Complaints SCR-54, content moderation, system settings); FR-02 (Customer tự sửa hồ sơ); FR-01 (đăng ký/OTP); Outstanding Debt marking (damage flow §3 FR-04) — không thuộc FR-09; Manager Customer List (figma SCR-55/56) — **out of FR-09 vĩnh viễn** (Admin only; Manager list = feature riêng nếu cần).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Admin xem và lọc danh sách Customer (Priority: P1)

Là **Admin**, tôi muốn xem danh sách tài khoản Customer với tìm kiếm và lọc theo trạng thái để quản trị người dùng nền tảng nhanh chóng.

**Why this priority**: FR-09 bullet đầu tiên; SCR-51 Customer Directory; nền tảng cho mọi thao tác quản lý khách.

**Independent Test**: Admin mở SCR-51 → bảng hiển thị Customer với Name, Email, Total Bookings, Total Spend; filter status; search theo tên/email; phân trang.

**Acceptance Scenarios**:

1. **Given** Admin đăng nhập, **When** mở Customer Directory, **Then** chỉ hiển thị user có role **Customer** (không gồm Admin/Manager/Employee).
2. **Given** danh sách Customer, **When** mở trang không chọn filter status, **Then** hiển thị Customer mọi trạng thái (INACTIVE, ACTIVE, SUSPENDED); **When** lọc theo Active hoặc Suspended (hoặc INACTIVE), **Then** kết quả khớp filter.
3. **Given** Admin search theo email hoặc họ tên, **When** apply, **Then** chỉ Customer khớp từ khóa hiển thị.
4. **Given** bảng Customer, **When** xem, **Then** cột Name, Email, Total Bookings, Total Spend, Status badge hiển thị đúng.
5. **Given** Manager, Employee hoặc Customer, **When** truy cập Customer Directory, **Then** từ chối truy cập.

---

### User Story 2 - Admin cập nhật trạng thái tài khoản Customer (Priority: P1)

Là **Admin**, tôi muốn kích hoạt hoặc đình chỉ (Suspend) tài khoản Customer để kiểm soát ai được sử dụng hệ thống.

**Why this priority**: FR-09 "Admin cập nhật trạng thái tài khoản Customer (Active/Suspended)"; §10 Administration acceptance.

**Independent Test**: Admin suspend Customer → Customer không đăng nhập được (FR-01); Admin activate lại → Customer đăng nhập được.

**Acceptance Scenarios**:

1. **Given** Customer đang Active, **When** Admin chọn Suspend và xác nhận, **Then** trạng thái chuyển **Suspended** và hiển thị trên danh sách.
2. **Given** Customer đang Suspended, **When** Admin chọn Activate, **Then** trạng thái chuyển **Active**.
3. **Given** Customer Suspended, **When** Customer cố đăng nhập (email hoặc Google), **Then** hệ thống từ chối với thông báo tài khoản bị đình chỉ (FR-01).
4. **Given** Customer có booking đang diễn ra, **When** Admin suspend, **Then** vẫn cho phép suspend (chặn đăng nhập mới); booking hiện tại không tự hủy (assumption: vận hành xử lý riêng).
5. **Given** Admin, **When** cố đổi trạng thái user không phải Customer, **Then** từ chối.

---

### User Story 3 - Admin xem hồ sơ và lịch sử đặt phòng (Priority: P1)

Là **Admin**, tôi muốn xem chi tiết hồ sơ Customer và lịch sử đặt phòng khi chọn một khách trong danh sách để hỗ trợ và ra quyết định quản trị.

**Why this priority**: FR-09 "Admin xem hồ sơ và lịch sử đặt phòng của từng Customer"; SCR-51 Drawer.

**Independent Test**: Admin click row Customer → Drawer hiển thị profile (fullName, email, phone, avatar, status, registered date) + bảng booking history (mã booking, ngày, phòng, tổng tiền, trạng thái).

**Acceptance Scenarios**:

1. **Given** Customer có ít nhất một booking, **When** Admin mở chi tiết, **Then** hồ sơ và danh sách booking hiển thị đúng thứ tự mới nhất trước.
2. **Given** Customer chưa có booking, **When** Admin mở chi tiết, **Then** hồ sơ hiển thị; booking history empty-state rõ ràng.
3. **Given** Admin xem chi tiết, **When** xem profile, **Then** chỉ **đọc** thông tin hồ sơ — không sửa fullName/email/phone (Customer tự sửa qua FR-02).
4. **Given** booking thuộc Customer A, **When** Admin xem Customer B, **Then** không lộ booking của A trong drawer B.
5. **Given** Customer INACTIVE (chưa xác thực email FR-01), **When** Admin xem danh sách/chi tiết, **Then** vẫn hiển thị với status INACTIVE; Admin **không** suspend/activate cho đến khi verified (assumption: chỉ Active↔Suspended trên tài khoản đã ACTIVE hoặc từng ACTIVE).

---

### Edge Cases

- Customer INACTIVE (pre-OTP) — hiển thị trong directory; thao tác Suspend/Activate không áp dụng hoặc disabled cho đến khi email verified.
- Admin suspend chính mình nếu có dual role — không áp dụng (Admin role không nằm trong Customer directory).
- Tìm kiếm không có kết quả — empty state thân thiện.
- Customer có Outstanding Debt (damage flow) — hiển thị indicator read-only nếu có flag downstream; **đánh dấu** nợ thuộc FR-04/damage flow, không FR-09.
- Phân trang danh sách lớn (hàng nghìn Customer) — phân trang bắt buộc.
- Refresh token đang hoạt động khi suspend — **revoke tất cả refresh tokens ngay** khi chuyển sang SUSPENDED (Clarifications 2026-07-23); access JWT hết hạn tự nhiên theo TTL; đăng nhập/refresh mới bị chặn.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: **Admin** MUST xem **danh sách Customer** (role CUSTOMER) có **phân trang**, **tìm kiếm** theo họ tên/email, **lọc** theo trạng thái tài khoản (SCR-51). **Default** (không chọn filter status) = hiển thị **tất cả** trạng thái (INACTIVE, ACTIVE, SUSPENDED).
- **FR-002**: Danh sách MUST hiển thị **tổng số booking** và **tổng chi tiêu** (Total Spend) tổng hợp cho mỗi Customer. **Total Spend** = `SUM(Payment.amount)` với `Payment.status = PAID` (mọi loại payment của Customer).
- **FR-003**: **Admin** MUST **cập nhật** trạng thái Customer giữa **Active** và **Suspended** với xác nhận trước khi thực hiện.
- **FR-004**: Customer **Suspended** MUST bị **chặn đăng nhập** và làm mới phiên theo quy tắc FR-01. Khi Admin Suspend, hệ thống MUST **revoke tất cả refresh tokens** của Customer đó ngay; access JWT hết hạn theo TTL (không bắt buộc access-token blacklist v1).
- **FR-005**: **Admin** MUST xem **hồ sơ Customer** (fullName, email, phone, avatar, status, ngày đăng ký) ở chế độ **read-only** (SCR-51 Drawer).
- **FR-006**: **Admin** MUST xem **lịch sử đặt phòng** của Customer (booking id, check-in/out, phòng, tổng tiền, trạng thái booking) từ dữ liệu FR-04.
- **FR-007**: Hệ thống MUST **từ chối** mọi thao tác FR-09 đối với role không phải **Admin**.
- **FR-008**: Hệ thống MUST **từ chối** thay đổi trạng thái user không có role **Customer**.
- **FR-009**: Thay đổi trạng thái MUST được **ghi audit** qua **ActivityLog** (action `USER_SUSPENDED` hoặc `USER_ACTIVATED`): ai (Admin), khi nào, entity User id, details chứa thái cũ → mới.
- **FR-010**: Admin MUST NOT tạo, xóa, hoặc sửa thông tin hồ sơ Customer qua FR-09 (đăng ký FR-01; tự sửa FR-02).

### Key Entities

- **User** (Customer scope): id, fullName, email, phone, avatarUrl, role=CUSTOMER, status (INACTIVE | ACTIVE | SUSPENDED), createdAt, updatedAt.
- **Booking** (read-only): id, customerId, room reference, checkInDate, checkOutDate, totalAmount, status — owned by FR-04; FR-09 chỉ đọc theo customerId.
- **CustomerSummary** (view): user fields + totalBookings, totalSpend aggregates (totalSpend = sum PAID payments).
- **ActivityLog** (audit, FR-009): action `USER_SUSPENDED` | `USER_ACTIVATED`, actor Admin, entity User, details old→new status.
## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Admin tìm được Customer theo email trong **dưới 30 giây** từ khi mở SCR-51.
- **SC-002**: **100%** tài khoản Suspended bị từ chối đăng nhập (test matrix email + Google) và **không** refresh được phiên sau khi Admin Suspend (refresh tokens đã revoke).
- **SC-003**: Admin xem hồ sơ và lịch sử booking của một Customer hoàn tất trong **dưới 5 giây** (≤50 booking/customer).
- **SC-004**: **100%** truy cập Customer Directory từ role không phải Admin bị từ chối.
- **SC-005**: Thay đổi trạng thái Active/Suspended phản ánh trên danh sách trong **dưới 3 giây** sau xác nhận.
- **SC-006**: **100%** hàng trong Customer Directory có role CUSTOMER — không lọt user khác role.

## Assumptions

- **Admin-only** theo Specification_v2 FR-09; Manager Customer List (figma SCR-55/56) **out of FR-09 vĩnh viễn** — nếu cần sau này = feature riêng, không mở rộng scope FR-09 (Clarifications 2026-07-23).
- **SCR-51** = Customer Directory (screen.md, api-spec-by-screen.md) — không nhầm với Contract Detail dùng cùng số ở frontend legacy.
- Admin chỉ toggle **Active ↔ Suspended**; **INACTIVE** là trạng thái pre-verification từ FR-01 — **hiển thị trong default list**, thao tác Suspend/Activate **disabled** cho đến khi verified.
- **Total Spend** = `SUM(Payment.amount)` where `status = PAID` (all payment types) — Clarifications 2026-07-23; chi tiết payment reconciliation thuộc FR-12.
- Suspend **không** tự hủy booking active; chặn đăng nhập và booking **mới**; **revoke refresh tokens ngay** khi Suspend (Clarifications 2026-07-23).
- **Outstanding Debt** flag hiển thị read-only nếu entity có sẵn từ damage flow — Admin không set qua FR-09.
- Complaint management (FR-17 SCR-54), content moderation, system settings — **không** thuộc FR-09.
- Drawer UX trên SCR-51 thay cho dedicated full page — align screendesign.md.

## Clarifications

### Session 2026-07-23

- Q: Total Spend (FR-002) được tính thế nào? → A: Sum `Payment.amount` where `status = PAID` (all payment types: DEPOSIT, REMAINING, DAMAGE_FEE, …).
- Q: Suspend có revoke refresh tokens ngay không? → A: Revoke tất cả refresh tokens ngay khi Suspend (+ chặn login/refresh mới).
- Q: INACTIVE có hiện trong default filter list không? → A: Default = tất cả trạng thái (ACTIVE + SUSPENDED + INACTIVE); filter có thể thu hẹp; Suspend/Activate disabled khi INACTIVE.
- Q: Audit store cho FR-009? → A: Dùng ActivityLog chung với action `USER_SUSPENDED` / `USER_ACTIVATED` (actor, thời điểm, old→new trong details).
- Q: Manager Customer List (SCR-55/56) có thuộc FR-09 không? → A: Out of FR-09 vĩnh viễn (Admin-only); Manager list = feature riêng nếu product yêu cầu sau.
