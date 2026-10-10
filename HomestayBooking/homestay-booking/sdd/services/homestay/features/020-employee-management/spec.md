# Feature Specification: FR-20 Employee Management

**Feature Branch**: `022-employee-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-20 Employee Management — dựa vào docs (Specification_v2.md § FR-20, §5 User & EmployeePropertyAssignment, §10 acceptance, api-spec-by-screen SCR-39, screen.md, screendesign.md, entity-ui-mapping.md)"

**Phụ thuộc**: FR-01 (User entity, role EMPLOYEE, login block khi SUSPENDED); FR-06 (Property — gán Employee theo Property); FR-02 (Employee tự sửa hồ sơ cá nhân). **Ranh giới**: FR-21 (Housekeeping task assignment); FR-22 (Employee Dashboard); FR-13 (Maintenance task assignment); FR-23 (Room Inspection); Employee **tự** đổi mật khẩu/hồ sơ qua FR-02 — FR-20 cho phép Admin/Manager **quản trị** thông tin và trạng thái tài khoản.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manager xem danh sách Employee theo Property (Priority: P1)

Là **Manager** được gán Property, tôi muốn xem **danh sách nhân viên** thuộc Property đó (tên, email, điện thoại, trạng thái tài khoản, ngày gán), để biết ai đang làm việc tại cơ sở (SCR-39).

**Why this priority**: FR-20 "Xem danh sách Employee theo Property"; nền tảng cho mọi thao tác quản lý nhân sự vận hành.

**Independent Test**: Manager chọn Property được gán → bảng Staff Directory hiển thị chỉ Employee có assignment ACTIVE tại Property đó; Manager Property khác không thấy.

**Acceptance Scenarios**:

1. **Given** Manager đăng nhập và có Property A được gán, **When** mở Employee Management với `propertyId` = A, **Then** chỉ hiển thị user role **Employee** có assignment **ACTIVE** tại Property A.
2. **Given** Property A có 3 Employee active, **When** Manager xem danh sách, **Then** bảng hiển thị Name, Email, Phone, Status badge, Assigned date (assumption: cột theo screendesign SCR-39).
3. **Given** Manager không được gán Property B, **When** cố xem Employee của Property B, **Then** hệ thống từ chối (`UNAUTHORIZED_PROPERTY_ACCESS`).
4. **Given** Property chưa có Employee nào, **When** Manager mở danh sách, **Then** empty state thân thiện + CTA **Assign Employee**.
5. **Given** Customer hoặc Employee, **When** truy cập Employee Management, **Then** từ chối truy cập.

---

### User Story 2 - Admin hoặc Manager gán Employee vào Property (Priority: P1)

Là **Admin** hoặc **Manager**, tôi muốn **gán** một Employee vào Property thông qua **EmployeePropertyAssignment**, để nhân viên có thể nhận tác vụ vận hành tại cơ sở đó (SCR-39 Modal).

**Why this priority**: FR-20 core — "thêm Employee vào Property thông qua EmployeePropertyAssignment".

**Independent Test**: Manager gán Employee chưa có assignment ACTIVE → assignment ACTIVE tạo thành công; Employee chỉ thấy dữ liệu Property được gán.

**Acceptance Scenarios**:

1. **Given** Employee chưa có assignment ACTIVE, **When** Manager gán vào Property A (được phép), **Then** tạo `EmployeePropertyAssignment` status **ACTIVE** với `assignedBy` = Manager hiện tại.
2. **Given** Employee đã có assignment ACTIVE tại Property A, **When** cố gán thêm Property B, **Then** từ chối — mỗi Employee chỉ **một** assignment ACTIVE tại một thời điểm (assumption: phải chuyển/reassign qua US6).
3. **Given** Manager Property A, **When** cố gán Employee vào Property B, **Then** từ chối.
4. **Given** Admin, **When** gán Employee vào bất kỳ Property hợp lệ, **Then** gán thành công.
5. **Given** Employee được gán, **When** Employee đăng nhập, **Then** chỉ truy cập được dữ liệu/tác vụ thuộc Property A (scope downstream FR-21/13/23).

---

### User Story 3 - Admin hoặc Manager tạo tài khoản Employee mới (Priority: P1)

Là **Admin** hoặc **Manager**, tôi muốn **tạo** tài khoản nhân viên mới (role Employee) và gán vào Property trong một luồng, để onboard nhân sự vận hành nhanh.

**Why this priority**: Access matrix Manager/Admin có quyền **Create** trên Users/Employee; thực tế vận hành cần tạo account trước khi gán.

**Independent Test**: Manager tạo Employee với email mới → user role EMPLOYEE xuất hiện trong danh sách Property sau gán; email trùng bị từ chối.

**Acceptance Scenarios**:

1. **Given** Admin hoặc Manager Property A, **When** tạo Employee với fullName, email, phone hợp lệ và chọn Property A, **Then** tạo user role **EMPLOYEE** status **ACTIVE** (hoặc INACTIVE chờ đặt mật khẩu — assumption align FR-01) và assignment ACTIVE tại Property A.
2. **Given** email đã tồn tại (Customer/Manager/Employee khác), **When** tạo, **Then** từ chối với thông báo email trùng.
3. **Given** Manager Property A, **When** tạo Employee gán Property B, **Then** từ chối.
4. **Given** Employee mới được tạo, **When** hoàn tất, **Then** nhân viên nhận hướng dẫn đăng nhập/đặt mật khẩu qua email (assumption: reuse FR-01 invite/reset flow).
5. **Given** trường bắt buộc thiếu (fullName, email), **When** submit, **Then** hiển thị lỗi validation.

---

### User Story 4 - Admin hoặc Manager cập nhật thông tin Employee (Priority: P1)

Là **Admin** hoặc **Manager**, tôi muốn **cập nhật** thông tin liên hệ của Employee (họ tên, số điện thoại) trong phạm vi được phép, để duy trì danh bạ nhân sự chính xác.

**Why this priority**: FR-20 explicit "Cập nhật thông tin Employee".

**Independent Test**: Manager sửa phone Employee thuộc Property được gán → lưu thành công; không sửa được email Employee đang active (assumption: email là định danh đăng nhập — chỉ Admin hoặc không đổi v1).

**Acceptance Scenarios**:

1. **Given** Employee thuộc Property Manager được gán, **When** Manager cập nhật fullName và phone, **Then** thông tin lưu và phản ánh trên danh sách.
2. **Given** Manager Property A, **When** cố sửa Employee thuộc Property B, **Then** từ chối.
3. **Given** Admin, **When** cập nhật Employee bất kỳ Property, **Then** lưu thành công.
4. **Given** Employee, **When** tự sửa hồ sơ qua FR-02, **Then** không xung đột — Manager vẫn có thể cập nhật qua FR-20 (assumption: Manager update ghi đè hoặc merge last-write-wins v1).
5. **Given** cố đổi role Employee sang Manager/Customer qua FR-20, **When** submit, **Then** từ chối — role change ngoài phạm vi FR-20.

---

### User Story 5 - Admin hoặc Manager vô hiệu hóa/kích hoạt Employee (Priority: P1)

Là **Admin** hoặc **Manager**, tôi muốn **đình chỉ** hoặc **kích hoạt lại** tài khoản Employee, để kiểm soát ai được truy cập hệ thống khi nghỉ việc hoặc quay lại.

**Why this priority**: FR-20 "Vô hiệu hóa/kích hoạt tài khoản Employee"; liên kết FR-01 chặn đăng nhập.

**Independent Test**: Manager suspend Employee → Employee không đăng nhập được; activate lại → đăng nhập được.

**Acceptance Scenarios**:

1. **Given** Employee đang Active, **When** Manager (Property được gán) chọn Suspend và xác nhận, **Then** status chuyển **Suspended** và badge cập nhật trên danh sách.
2. **Given** Employee Suspended, **When** Employee cố đăng nhập, **Then** hệ thống từ chối theo FR-01.
3. **Given** Employee Suspended, **When** Admin hoặc Manager Activate, **Then** status chuyển **Active** và đăng nhập lại được.
4. **Given** Employee có tác vụ Housekeeping/Maintenance đang mở, **When** suspend, **Then** vẫn cho phép suspend (assumption: tác vụ hiện tại Manager reassign qua FR-21/13 — không auto-cancel v1).
5. **Given** Manager Property A, **When** cố suspend Employee thuộc Property B, **Then** từ chối.

---

### User Story 6 - Admin chuyển Employee sang Property khác (Priority: P2)

Là **Admin**, tôi muốn **chuyển** Employee từ Property này sang Property khác khi tái phân công, để duy trì quy tắc một assignment ACTIVE duy nhất mà không tạo tài khoản mới.

**Why this priority**: Business constraint một ACTIVE assignment; thao tác tái phân công phổ biến nhưng ít hơn gán mới.

**Independent Test**: Admin reassign Employee từ A → B → assignment A chuyển INACTIVE, assignment B ACTIVE; Employee scope đổi sang Property B.

**Acceptance Scenarios**:

1. **Given** Employee ACTIVE tại Property A, **When** Admin reassign sang Property B, **Then** assignment A → **INACTIVE**, assignment B → **ACTIVE** (lịch sử A được giữ).
2. **Given** Manager (không phải Admin), **When** cố reassign Employee sang Property khác ngoài phạm vi, **Then** từ chối (assumption: Manager chỉ gán Employee **chưa** có ACTIVE assignment hoặc trong cùng Property — chuyển cross-property là Admin-only P2).
3. **Given** reassign thành công, **When** Employee đăng nhập, **Then** chỉ thấy dữ liệu Property B.
4. **Given** Employee có tác vụ đang mở tại Property A, **When** reassign, **Then** hiển thị cảnh báo trước khi xác nhận (assumption: không auto-hoàn thành tác vụ).

---

### Edge Cases

- Employee INACTIVE (chưa hoàn tất onboarding email) — hiển thị trong directory với status INACTIVE; không gán tác vụ cho đến khi Active.
- Gán Employee đã **Suspended** — từ chối hoặc cảnh báo; phải Activate trước (assumption: từ chối gán khi Suspended).
- Property INACTIVE — không cho gán Employee mới (assumption align FR-06).
- Employee tự đăng ký public — không thuộc FR-20; chỉ Admin/Manager tạo role EMPLOYEE.
- Xóa Employee — Admin có quyền Delete trên access matrix; **v1 assumption**: soft-delete/deactivate (Suspend) thay vì hard delete; hard delete Admin-only P2 nếu không có booking/tác vụ liên kết.
- Manager bị thu hồi quyền Property — không còn xem/sửa Employee Property đó.
- Phân trang danh sách lớn — phân trang hoặc search theo tên/email (assumption: search v1 trên SCR-39).
- Audit: gán, chuyển Property, suspend/activate nên được ghi nhận (assumption align FR-17 activity log pattern).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: **Admin** và **Manager** MUST quản lý Employee thông qua **EmployeePropertyAssignment** (SCR-39).
- **FR-002**: Hệ thống MUST cho phép **xem danh sách Employee** theo **Property** — chỉ user role **EMPLOYEE** có assignment liên quan.
- **FR-003**: **Admin** hoặc **Manager** MUST có thể **gán** Employee vào Property — tạo assignment **ACTIVE** với `assignedBy` và `assignedAt`.
- **FR-004**: Mỗi Employee MUST có **tối đa một** assignment **ACTIVE** tại một thời điểm; Employee chỉ thuộc **một** Property duy nhất khi ACTIVE.
- **FR-005**: **Manager** MUST chỉ quản lý Employee trong **Property được gán**; truy cập Property khác MUST bị từ chối.
- **FR-006**: **Admin** MUST quản lý Employee trên **mọi Property** (global scope).
- **FR-007**: **Admin** hoặc **Manager** MUST có thể **tạo** tài khoản user mới với role **EMPLOYEE** và gán Property (trong phạm vi quyền).
- **FR-008**: **Admin** hoặc **Manager** MUST có thể **cập nhật** fullName và phone của Employee trong phạm vi quyền.
- **FR-009**: **Admin** hoặc **Manager** MUST có thể chuyển trạng thái Employee giữa **Active** và **Suspended** với xác nhận.
- **FR-010**: Employee **Suspended** MUST bị **chặn đăng nhập** theo FR-01.
- **FR-011**: Khi **chuyển Property** (reassign), hệ thống MUST đặt assignment cũ thành **INACTIVE** và tạo/kích hoạt assignment mới **ACTIVE** — giữ lịch sử.
- **FR-012**: Hệ thống MUST **từ chối** Customer, Employee (self-mgmt screen), và Guest truy cập chức năng quản lý Employee.
- **FR-013**: FR-20 MUST NOT gán Housekeeping/Maintenance/Inspection tasks — thuộc FR-21/13/23.
- **FR-014**: Thay đổi assignment và trạng thái tài khoản SHOULD được **ghi audit** (ai, khi nào, hành động).

### Key Entities

- **User** (Employee scope): id, fullName, email, phone, avatarUrl, role=EMPLOYEE, status (INACTIVE | ACTIVE | SUSPENDED), createdAt, updatedAt.
- **EmployeePropertyAssignment**: id, employeeId, propertyId, assignedBy, assignedAt, status (ACTIVE | INACTIVE).
- **Property** (read reference): id, name — owned by FR-06; FR-20 chỉ gán Employee theo propertyId hợp lệ.
- **EmployeeSummary** (view): user fields + currentPropertyName, assignedAt, assignmentStatus.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Manager tìm được Employee theo tên/email trong **dưới 30 giây** từ khi mở SCR-39.
- **SC-002**: **100%** Employee Suspended bị từ chối đăng nhập (test matrix email).
- **SC-003**: Gán Employee vào Property hoàn tất trong **dưới 1 phút** (tạo mới + gán hoặc gán existing).
- **SC-004**: **100%** truy cập Employee Management từ role không phải Admin/Manager bị từ chối.
- **SC-005**: **100%** Manager chỉ thấy Employee thuộc Property được gán — không leak cross-property.
- **SC-006**: **100%** Employee có đúng **một** assignment ACTIVE sau mọi thao tác gán/reassign thành công.
- **SC-007**: Thay đổi trạng thái Active/Suspended phản ánh trên danh sách trong **dưới 3 giây** sau xác nhận.

## Assumptions

- **Screen**: SCR-39 Employee Management (Manager actor per screen.md/screendesign); route assumption `/manager/employees` với Property context từ Manager session/selector.
- **Admin**: dùng cùng SCR-39 pattern với **Property picker** toàn hệ thống hoặc route admin tương đương — Admin không bị giới hạn Property.
- **API baseline** (api-spec SCR-39): `GET /manager/employees?propertyId`, `POST /manager/employees/assign` — mở rộng thêm create/update/status/reassign endpoints trong planning; Admin endpoints mirror Manager với global scope.
- **Tạo Employee**: fullName, email, phone bắt buộc; mật khẩu ban đầu qua email invite/reset (FR-01) — không nhập mật khẩu plaintext trên form Manager v1.
- **Email** không đổi qua FR-20 v1 (định danh đăng nhập); đổi email thuộc FR-02/Admin exception ngoài scope.
- **Reassign cross-property**: Admin-only P2; Manager chỉ gán Employee chưa có ACTIVE assignment vào Property của mình.
- **Delete**: v1 ưu tiên Suspend thay hard delete; hard delete Admin-only khi không có ràng buộc dữ liệu — chi tiết planning.
- **Housekeeping/Maintenance assignment** (SCR-40/41) — FR-21/FR-13, không FR-20.
- **Employee Dashboard** (SCR-59) — FR-22, không FR-20.
- Lịch sử assignment INACTIVE được giữ (align ManagerPropertyAssignment pattern FR-06).
- Customer List Manager (figma SCR-55/56) — không thuộc FR-20.
