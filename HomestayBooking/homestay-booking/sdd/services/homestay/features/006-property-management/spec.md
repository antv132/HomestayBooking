# Feature Specification: FR-06 Property Management

**Feature Branch**: `008-property-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-06 Property Management — dựa vào docs (Specification_v2.md § FR-06, §5 Property/ManagerPropertyAssignment, §10 Admin Management acceptance, api-spec-by-screen SCR-46–49, screen.md, screendesign.md, entity-ui-mapping.md)"

**Phụ thuộc**: FR-01 (Admin/Manager auth, RBAC); User/Manager accounts tồn tại. **Ranh giới**: FR-07 Structure (Floor tree); FR-08 Room CRUD; EmployeePropertyAssignment (gán Employee — API riêng, không thuộc FR-06); PricingRule cấu hình giá (entity §5 — có thể seed sau); Guest discovery filter INACTIVE property thuộc FR-03 side-effect.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Admin tạo và quản lý Property (Priority: P1)

Là **Admin**, tôi muốn tạo property mới (homestay/resort), chỉnh sửa thông tin, và bật/tắt trạng thái Active/Inactive để quản lý danh mục chi nhánh trên toàn hệ thống.

**Why this priority**: FR-06 bullets cốt lõi — Admin CRUD + status; SCR-46, SCR-47, SCR-48.

**Independent Test**: Admin mở Property Management → Create → property xuất hiện trong list; Edit → cập nhật name/address; Set Inactive → status đổi, property không còn hiển thị cho khách (FR-03).

**Acceptance Scenarios**:

1. **Given** Admin đã đăng nhập, **When** tạo property với name, address, description hợp lệ, **Then** property được lưu và hiển thị trong danh sách SCR-46.
2. **Given** property tồn tại, **When** Admin chỉnh sửa thông tin, **Then** thay đổi được lưu và phản ánh trên detail.
3. **Given** property ACTIVE, **When** Admin chuyển sang INACTIVE, **Then** status cập nhật; property và phòng thuộc property không còn xuất hiện trong discovery khách (FR-03).
4. **Given** trường bắt buộc thiếu (name hoặc address), **When** submit form, **Then** từ chối với thông báo lỗi rõ ràng.
5. **Given** Manager hoặc Customer, **When** cố tạo/sửa property qua Admin flow, **Then** từ chối truy cập.

---

### User Story 2 - Admin gán Manager cho Property (Priority: P1)

Là **Admin**, tôi muốn chỉ định Manager quản lý từng property và thay đổi Manager khi cần, với lịch sử gán được lưu, để mỗi chi nhánh luôn có người chịu trách nhiệm vận hành.

**Why this priority**: FR-06 ManagerPropertyAssignment; SCR-49; ràng buộc nghiệp vụ §5.

**Independent Test**: Admin mở Assign Manager → chọn Manager ACTIVE → Save → list hiển thị Manager name; đổi Manager → assignment cũ INACTIVE, assignment mới ACTIVE.

**Acceptance Scenarios**:

1. **Given** property chưa có Manager ACTIVE, **When** Admin gán Manager M, **Then** tạo ManagerPropertyAssignment ACTIVE; Manager M thấy property trong danh sách được gán.
2. **Given** property đã có Manager A ACTIVE, **When** Admin gán Manager B, **Then** assignment A → INACTIVE, assignment B → ACTIVE; chỉ **một** ACTIVE tại một thời điểm.
3. **Given** Manager user không tồn tại hoặc không role MANAGER, **When** Admin gán, **Then** từ chối.
4. **Given** assignment thay đổi, **When** lưu, **Then** ghi nhận AssignedBy (Admin), AssignedAt; assignment cũ vẫn tồn tại INACTIVE (lịch sử).
5. **Given** một Manager M, **When** được gán nhiều property, **Then** M quản lý tất cả property đó (FR-06: Manager có thể quản lý nhiều Property).

---

### User Story 3 - Manager xem Property được gán (Priority: P1)

Là **Manager**, tôi muốn xem danh sách và chi tiết các property được Admin gán cho mình để vận hành Structure, Room, Booking trong phạm vi đó.

**Why this priority**: FR-06 "Manager xem danh sách và chi tiết Property được gán"; SCR-27 context, manager property pages.

**Independent Test**: Manager login → chỉ thấy assigned properties; click detail → name, address, status, stats summary; không thấy property khác.

**Acceptance Scenarios**:

1. **Given** Manager được gán property P1, P2, **When** mở danh sách property, **Then** chỉ hiển thị P1, P2 (ACTIVE assignment).
2. **Given** Manager mở chi tiết property được gán, **When** xem, **Then** hiển thị thông tin cơ bản và tóm tắt vận hành (số tầng/phòng nếu có — read-only từ FR-07/08 khi có dữ liệu).
3. **Given** Manager truy cập property không được gán, **When** mở URL trực tiếp, **Then** từ chối (403).
4. **Given** assignment chuyển sang Manager khác, **When** Manager cũ refresh, **Then** property biến mất khỏi danh sách.

---

### User Story 4 - Ràng buộc Property phải có Manager ACTIVE (Priority: P2)

Là **hệ thống**, tôi cần đảm bảo mỗi property **ACTIVE** luôn có đúng một ManagerPropertyAssignment ACTIVE, và không cho phép trạng thái không hợp lệ.

**Why this priority**: FR-06 + §5 Business Constraints + §10 acceptance.

**Independent Test**: Cố kích hoạt ACTIVE property không có Manager → từ chối hoặc bắt buộc gán Manager trước; property ACTIVE luôn có managerId trong admin list.

**Acceptance Scenarios**:

1. **Given** property mới tạo, **When** Admin set ACTIVE mà chưa gán Manager, **Then** từ chối hoặc yêu cầu gán Manager trong cùng flow (wizard).
2. **Given** property ACTIVE với Manager M, **When** Admin thu hồi assignment (unassign) mà không gán thay thế, **Then** từ chối — property ACTIVE bắt buộc có Manager.
3. **Given** property INACTIVE, **When** chưa có Manager, **Then** cho phép (property chưa vận hành).

---

### User Story 5 - Admin tra cứu danh sách Property (Priority: P2)

Là **Admin**, tôi muốn xem bảng tất cả property với tên, địa điểm, Manager hiện tại, trạng thái, và tìm kiếm/lọc để quản trị nhanh.

**Why this priority**: SCR-46 table UX; vận hành multi-property.

**Independent Test**: Admin list paginated; filter ACTIVE/INACTIVE; search by name/address; row actions → Edit, Assign Manager.

**Acceptance Scenarios**:

1. **Given** nhiều property trong hệ thống, **When** Admin mở SCR-46, **Then** danh sách phân trang với cột Name, Location/Address, Manager name (hoặc "Unassigned"), Status.
2. **Given** Admin search "Đà Nẵng", **When** filter, **Then** chỉ property khớp name/address.
3. **Given** Admin lọc status INACTIVE, **When** apply, **Then** chỉ property INACTIVE.

---

### Edge Cases

- Trùng tên property — cho phép (phân biệt bằng id/address); hiển thị address trong list để tránh nhầm.
- Manager bị SUSPENDED — property vẫn ACTIVE nhưng cảnh báo Admin cần reassign (assumption: assignment vẫn ACTIVE until Admin changes; login Manager suspended blocked by FR-01).
- Xóa property — **out of scope** FR-06 (chỉ INACTIVE); hard delete nếu có room/booking thuộc FR-07/08/04.
- Admin tự gán mình làm Manager — không hợp lệ nếu user role ADMIN (chỉ user role MANAGER).
- Property INACTIVE vẫn visible cho Admin/assigned Manager read-only để audit.
- Tạo property + gán Manager trong một transaction — nếu gán fail thì property vẫn tạo ở INACTIVE draft (assumption: default INACTIVE on create until manager assigned).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: **Admin** MUST có thể **tạo** property mới với name, address, description (SCR-47).
- **FR-002**: **Admin** MUST có thể **chỉnh sửa** thông tin property (SCR-48).
- **FR-003**: **Admin** MUST có thể đặt property status **ACTIVE** hoặc **INACTIVE**.
- **FR-004**: **Admin** MUST có thể **xem danh sách** tất cả property với phân trang, tìm kiếm, lọc status (SCR-46).
- **FR-005**: **Admin** MUST có thể **gán Manager** cho property qua ManagerPropertyAssignment (SCR-49).
- **FR-006**: Hệ thống MUST đảm bảo **tối đa một** ManagerPropertyAssignment **ACTIVE** per property tại mọi thời điểm.
- **FR-007**: Hệ thống MUST **lưu lịch sử** assignment (INACTIVE records không xóa).
- **FR-008**: Property status **ACTIVE** MUST NOT tồn tại **không có** Manager assignment ACTIVE (FR-004 US4).
- **FR-009**: **Manager** MUST xem **danh sách và chi tiết** chỉ các property có assignment ACTIVE cho chính Manager đó.
- **FR-010**: **Manager** MUST NOT tạo/sửa/deactivate property toàn hệ thống (Admin only).
- **FR-011**: Property **INACTIVE** MUST NOT hiển thị trong **Guest/Customer discovery** (phối hợp FR-03).
- **FR-012**: Gán Manager MUST chỉ chấp nhận user có role **MANAGER** và status **ACTIVE**.

### Key Entities

- **Property**: id, name, address, description, status (ACTIVE/INACTIVE), createdAt, updatedAt.
- **ManagerPropertyAssignment**: id, managerId, propertyId, assignedBy, assignedAt, status (ACTIVE/INACTIVE).
- **User** (Manager): referenced for assignment; role MANAGER.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Admin hoàn tất **tạo property + gán Manager** trong **dưới 5 phút** (SCR-47 → SCR-49 flow).
- **SC-002**: **100%** property ACTIVE trong hệ thống có đúng **một** Manager assignment ACTIVE (audit query).
- **SC-003**: Manager chỉ thấy property được gán — **0** property lọt vào list/detail khi không có assignment ACTIVE (security test).
- **SC-004**: Admin tìm property theo tên/địa chỉ trả kết quả chính xác trong **dưới 3 giây** (user-facing, list ≤ 500 properties).
- **SC-005**: **100%** thao tác gán Manager mới deactivate assignment cũ và tạo bản ghi lịch sử INACTIVE.
- **SC-006**: Sau khi property INACTIVE, **100%** truy cập discovery khách không hiển thị property đó (FR-03 integration test).

## Assumptions

- Property mới tạo **mặc định INACTIVE** cho đến khi Admin gán Manager và kích hoạt ACTIVE (tránh property ACTIVE không Manager).
- Trường SCR-47 mở rộng (property type, map coordinates, check-in policy, penalty %) **optional v1** — core fields theo §5 Property entity; mở rộng trong plan nếu screendesign bắt buộc.
- Admin quản lý property qua màn hình quản trị dành riêng (SCR-46–49); Manager xem property qua màn hình vận hành được gán. Chi tiết endpoint sẽ được định nghĩa ở giai đoạn plan.
- Manager **không** xóa property; Admin **không** hard-delete property có room/booking — chỉ INACTIVE.
- EmployeePropertyAssignment là feature riêng (Manager/Admin gán Employee) — không thuộc FR-06.
- Location trong api-spec = address trong entity §5.
