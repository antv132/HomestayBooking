# Feature Specification: FR-02 User Profile

**Feature Branch**: `003-user-profile`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-02 User Profile — dựa vào docs (Specification_v2.md §3 FR-02, §2 Actors, §5 User, §6 RBAC Users, §7 Validation Errors, api-spec-by-screen SCR-10–SCR-11, screen.md)"

**Phụ thuộc**: FR-01 Authentication (người dùng phải đăng nhập để truy cập hồ sơ)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Xem hồ sơ cá nhân (Priority: P1)

Là **Customer, Employee, Manager hoặc Admin** đã đăng nhập, tôi muốn xem thông tin hồ sơ của chính mình (họ tên, email, số điện thoại, ảnh đại diện, vai trò) để kiểm tra thông tin liên hệ và trạng thái tài khoản.

**Why this priority**: FR-02 bullet đầu tiên; nền tảng cho mọi thao tác profile sau.

**Independent Test**: Đăng nhập → mở SCR-10 → hiển thị đúng dữ liệu user hiện tại; không thấy hồ sơ người khác.

**Acceptance Scenarios**:

1. **Given** user đã đăng nhập ACTIVE, **When** truy cập trang hồ sơ, **Then** hiển thị fullName, email, phone, avatarUrl (hoặc placeholder), role.
2. **Given** user chưa đăng nhập, **When** truy cập hồ sơ, **Then** chuyển hướng đăng nhập (phụ thuộc FR-01).
3. **Given** user SUSPENDED vẫn có token cũ, **When** gọi API hồ sơ, **Then** từ chối truy cập (auth layer FR-01).

---

### User Story 2 - Cập nhật hồ sơ cá nhân (Priority: P1)

Là user đã đăng nhập, tôi muốn chỉnh sửa họ tên, số điện thoại và ảnh đại diện để thông tin liên hệ luôn chính xác.

**Why this priority**: FR-02 "Cập nhật hồ sơ" + "Quản lý thông tin liên hệ" — core value.

**Independent Test**: Sửa fullName/phone trên SCR-11 → lưu → SCR-10 phản ánh thay đổi; email không đổi được.

**Acceptance Scenarios**:

1. **Given** user đã đăng nhập, **When** gửi fullName và phone hợp lệ, **Then** hệ thống cập nhật và trả hồ sơ mới.
2. **Given** user cố sửa email qua form/API, **When** submit, **Then** email **không** thay đổi (email là định danh đăng nhập — read-only).
3. **Given** phone không đúng định dạng, **When** lưu, **Then** validation error (§7 Invalid phone number).
4. **Given** fullName trống, **When** lưu, **Then** validation error (Required field missing).

---

### User Story 3 - Đồng bộ hồ sơ sau cập nhật (Priority: P2)

Là user vừa cập nhật hồ sơ, tôi muốn thấy thông tin mới ngay trên header/menu và các màn hình dùng tên hiển thị mà không cần đăng nhập lại.

**Why this priority**: UX continuity; Customer permission "Quản lý hồ sơ cá nhân".

**Independent Test**: Sau cập nhật hồ sơ → tên trên layout/header phản ánh giá trị mới ngay lập tức.

**Acceptance Scenarios**:

1. **Given** cập nhật fullName thành công, **When** quay lại dashboard, **Then** tên hiển thị mới khớp DB.
2. **Given** cập nhật avatarUrl, **When** xem profile, **Then** ảnh mới hiển thị (hoặc fallback initials nếu URL lỗi).

---

### Edge Cases

- User chỉ cập nhật một field (partial update) → các field khác giữ nguyên.
- avatarUrl null/rỗng → UI hiển thị initials từ fullName.
- Manager/Employee/Admin dùng cùng API `/users/me` — chỉ thấy **Self** scope (§6 RBAC Users: R Self, U Self).
- Không cho phép user tự đổi role hoặc status qua profile API.
- Customer không truy cập được hồ sơ Customer khác (403/404).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST cho phép mọi user đã xác thực (Customer, Employee, Manager, Admin) **xem** hồ sơ **của chính mình**.
- **FR-002**: Hệ thống MUST trả về: id, fullName, email, phone, avatarUrl (và role nếu cần hiển thị UI) khi xem hồ sơ.
- **FR-003**: Hệ thống MUST cho phép user **cập nhật** fullName, phone, avatarUrl trên hồ sơ cá nhân.
- **FR-004**: Hệ thống MUST **không** cho phép user tự sửa email, role, status qua luồng profile (email read-only).
- **FR-005**: Hệ thống MUST validate fullName (bắt buộc, không rỗng) và phone (định dạng hợp lệ) trước khi lưu.
- **FR-006**: Hệ thống MUST áp dụng RBAC Self-scope: API profile chỉ truy cập/cập nhật bản ghi user đang đăng nhập (§6 Users/Employee: R Self, U Self).
- **FR-007**: Hệ thống MUST từ chối truy cập profile khi chưa đăng nhập hoặc token không hợp lệ.
- **FR-008**: Hệ thống MUST ghi nhận thời điểm cập nhật (updatedAt) khi hồ sơ thay đổi.

### Key Entities

- **User** (§5): id, fullName, email, phone, avatarUrl, role, status, createdAt, updatedAt — FR-02 chỉ **đọc** và **cập nhật một phần** (fullName, phone, avatarUrl) bản ghi Self.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 95% user hoàn tất xem hồ sơ trong dưới 3 giây sau khi mở SCR-10.
- **SC-002**: 95% cập nhật hồ sơ hợp lệ hoàn tất trong dưới 5 giây (từ submit đến xác nhận).
- **SC-003**: 100% request profile chỉ trả/cập nhật dữ liệu user đang đăng nhập — zero cross-user profile access.
- **SC-004**: 100% thay đổi email/role/status qua profile API bị từ chối hoặc bỏ qua.
- **SC-005**: Sau cập nhật thành công, thông tin hiển thị trên SCR-10 khớp 100% với dữ liệu lưu trữ.

## Assumptions

- **Out of scope FR-02**: Đổi mật khẩu (FR-01 / SCR-12); Admin xem/sửa hồ sơ Customer khác (FR-09); upload file avatar lên server — MVP dùng **avatarUrl** dạng URL string (api-spec-by-screen SCR-11); upload file có thể bổ sung sau.
- Email thay đổi (nếu cần) thuộc flow riêng có xác thực — không nằm FR-02.
- API paths theo `docs/api-spec-by-screen.md`: `GET /api/v1/users/me`, `PUT /api/v1/users/me`.
- Response envelope: `{ success, message, data }` / `{ success, message, errors[] }`.
- UI: SCR-10 (view), SCR-11 (edit) — Customer, Employee, Manager, Admin (screen.md §2).
- Phụ thuộc FR-01: JWT/session xác định identity cho `/users/me`.

## Clarifications

*(Chưa có session clarify — defaults ở Assumptions.)*
