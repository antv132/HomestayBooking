# Feature Specification: FR-07 Structure Management

**Feature Branch**: `009-structure-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-07 Structure Management — dựa vào docs (Specification_v2.md § FR-07, §5 Floor, §10 Structure Management acceptance, api-spec-by-screen SCR-28, screen.md, screendesign.md, entity-ui-mapping.md §1.3, component-library.md TreeView, frontend StructureTreePage.tsx, floorApi.ts)"

**Phụ thuộc**: FR-06 (Property tồn tại; Manager chỉ thấy Property được gán). **Ranh giới**: FR-08 Room Management (CRUD phòng, gallery, status, lọc danh sách phòng); FR-03 discovery; Employee read-only room list (SCR-65) — không thuộc FR-07. Tree hiển thị **Room** read-only để Manager điều hướng; thêm/sửa/xóa phòng thuộc FR-08.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manager xem cây cấu trúc Property → Floor → Room (Priority: P1)

Là **Manager**, tôi muốn chọn Property được gán và xem cây cấu trúc (tầng và phòng bên dưới) để nắm tổng quan layout chi nhánh trước khi vận hành phòng.

**Why this priority**: FR-07 bullet cốt lõi — Property Selector + Structure Tree View; SCR-28.

**Independent Test**: Manager mở Structure Management → chọn Property P → tree hiển thị floors và rooms (read-only nodes); đổi Property → tree cập nhật; Property không được gán → không có trong selector / 403.

**Acceptance Scenarios**:

1. **Given** Manager được gán property P1, P2, **When** mở SCR-28, **Then** Property Selector chỉ liệt kê P1, P2.
2. **Given** P1 có 2 tầng và phòng trên mỗi tầng, **When** chọn P1, **Then** tree hiển thị phân cấp Property → Floor → Room với nhãn floor number và room number.
3. **Given** phòng có trạng thái vận hành, **When** hiển thị trên tree, **Then** hiển thị chỉ báo trạng thái (màu/nhãn) — không yêu cầu chỉnh sửa status tại FR-07 (FR-08/FR-05).
4. **Given** property chưa có tầng nào, **When** mở tree, **Then** hiển thị trạng thái trống và gợi ý thêm tầng đầu tiên.
5. **Given** Manager truy cập tree của property không được gán (URL trực tiếp), **When** tải dữ liệu, **Then** từ chối truy cập.

---

### User Story 2 - Manager thêm tầng (Floor) (Priority: P1)

Là **Manager**, tôi muốn thêm tầng mới vào Property được gán (số tầng, mô tả) qua form/modal để xây dựng cấu trúc vật lý trước khi thêm phòng (FR-08).

**Why this priority**: FR-07 "Manager thêm Floor"; screendesign SCR-28 modal Add Floor.

**Independent Test**: Manager click Add Floor → nhập floor number + description → Save → tầng xuất hiện trên tree; trùng floor number trong cùng property → từ chối.

**Acceptance Scenarios**:

1. **Given** Manager trên property P được gán, **When** tạo floor với floor number hợp lệ (số nguyên dương) và description, **Then** floor được lưu và hiển thị trên tree.
2. **Given** property đã có floor number 1, **When** tạo floor number 1 trùng, **Then** từ chối với thông báo rõ ràng.
3. **Given** floor number thiếu hoặc không hợp lệ (≤0, không phải số), **When** submit, **Then** từ chối validation.
4. **Given** Manager không được gán property, **When** cố tạo floor, **Then** từ chối truy cập.

---

### User Story 3 - Manager chỉnh sửa tầng (Priority: P1)

Là **Manager**, tôi muốn cập nhật số tầng và mô tả của Floor đã tạo để sửa thông tin cấu trúc khi cần.

**Why this priority**: FR-07 "Manager sửa Floor"; SCR-28 edit floor action.

**Independent Test**: Manager chọn floor trên tree → Edit → đổi description → Save → tree cập nhật; đổi floor number trùng tầng khác → từ chối.

**Acceptance Scenarios**:

1. **Given** floor tồn tại thuộc property được gán, **When** Manager cập nhật description, **Then** thay đổi được lưu.
2. **Given** Manager đổi floor number sang giá trị đã tồn tại trong cùng property, **When** save, **Then** từ chối.
3. **Given** Customer hoặc Employee, **When** cố sửa floor, **Then** từ chối (Manager only write).

---

### User Story 4 - Manager xóa tầng (Priority: P1)

Là **Manager**, tôi muốn xóa tầng không còn sử dụng (khi chưa có phòng) để giữ cấu trúc gọn và chính xác.

**Why this priority**: FR-07 "Manager xóa Floor"; business rule thường gặp — không xóa khi còn phòng.

**Independent Test**: Floor không có room → Delete → biến mất khỏi tree; floor có ≥1 room → Delete bị từ chối.

**Acceptance Scenarios**:

1. **Given** floor không có phòng con, **When** Manager xóa, **Then** floor bị xóa khỏi hệ thống và tree.
2. **Given** floor có ít nhất một phòng, **When** Manager xóa, **Then** từ chối — phải xóa/di chuyển phòng trước (FR-08).
3. **Given** floor có booking active trên phòng con (edge), **When** xóa floor, **Then** vẫn từ chối vì còn phòng (FR-08 owns room delete rules).

---

### User Story 5 - Admin xem cấu trúc read-only (Priority: P2)

Là **Admin**, tôi muốn xem cây cấu trúc Property → Floor → Room trên mọi property (read-only) để hỗ trợ giám sát và KPI, không chỉnh sửa cấu trúc qua Admin UI trong FR-07.

**Why this priority**: Ma trận quyền §4 Admin **R** trên Property/Floor/Room; Admin Dashboard KPIs (Total Floors). Write vẫn thuộc Manager trên property được gán.

**Independent Test**: Admin mở structure view → chọn bất kỳ property → tree read-only; không có nút Add/Edit/Delete floor.

**Acceptance Scenarios**:

1. **Given** Admin đăng nhập, **When** xem structure tree, **Then** Property Selector liệt kê tất cả property (ACTIVE và INACTIVE).
2. **Given** Admin trên tree view, **When** tương tác, **Then** **không** có thao tác tạo/sửa/xóa floor (read-only).
3. **Given** Manager, **When** dùng cùng màn hình SCR-28, **Then** có đầy đủ CRUD floor trên property được gán.

---

### Edge Cases

- Property INACTIVE — Manager được gán vẫn xem và quản lý floor (vận hành nội bộ); Guest không thấy (FR-03).
- Nhiều property cùng tên — selector hiển thị kèm address để phân biệt.
- Floor number không liên tục (1, 3, 5) — cho phép; sắp xếp tree theo floor number tăng dần.
- Tree lớn (nhiều tầng/phòng) — tree có thể collapse/expand theo tầng; performance chấp nhận được với property thông thường (<20 tầng, <200 phòng).
- Đồng thời hai Manager sửa cùng floor — last-write-wins với thông báo lỗi optimistic nếu conflict (assumption: standard concurrency).
- Room nodes trên tree link tới Room Detail (FR-08) — điều hướng optional, không CRUD room tại FR-07.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST hiển thị **cây cấu trúc** Property → Floor → Room cho **Manager** trên property được gán (SCR-28).
- **FR-002**: Hệ thống MUST cung cấp **Property Selector** chỉ gồm property Manager được gán (FR-06 assignment).
- **FR-003**: **Manager** MUST có thể **tạo** Floor với floor number (số nguyên dương) và description trong property được gán.
- **FR-004**: **Manager** MUST có thể **cập nhật** floor number và description của Floor thuộc property được gán.
- **FR-005**: **Manager** MUST có thể **xóa** Floor chỉ khi Floor **không còn** Room con nào.
- **FR-006**: Hệ thống MUST **từ chối** trùng **floor number** trong cùng một Property (create và update).
- **FR-007**: Hệ thống MUST **từ chối** mọi thao tác Floor write khi Manager **không** có assignment ACTIVE cho Property liên quan.
- **FR-008**: Tree MUST hiển thị **Room** dưới Floor **read-only** (room number, trạng thái hiển thị) — nguồn dữ liệu phòng từ FR-08.
- **FR-009**: **Admin** MUST có thể **xem** structure tree trên mọi property **read-only** (không CRUD floor qua Admin trong FR-07).
- **FR-010**: Hệ thống MUST sắp xếp floors theo **floor number** tăng dần trong tree.
- **FR-011**: Validation MUST từ chối floor number ≤ 0 hoặc không phải số nguyên.
- **FR-012**: Sau mọi thay đổi Floor (create/update/delete), tree MUST phản ánh cập nhật **ngay** khi Manager refresh hoặc quay lại màn hình (không yêu cầu real-time push — FR-15 optional).

### Key Entities

- **Property**: container; Manager scope via ManagerPropertyAssignment (FR-06).
- **Floor**: id, propertyId, floorNumber, description, createdAt, updatedAt.
- **Room** (read in tree only): id, floorId, propertyId, roomNumber, status — owned by FR-08 for writes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Manager hoàn tất **thêm tầng đầu tiên** cho property trong **dưới 2 phút** (SCR-28 flow).
- **SC-002**: **100%** floor trùng number trong cùng property bị từ chối (validation test).
- **SC-003**: **100%** xóa floor có phòng con bị từ chối.
- **SC-004**: Manager chỉ thấy property được gán trong selector — **0** property lọt vào selector khi không có assignment (security test).
- **SC-005**: Tree tải cấu trúc property ≤ 200 phòng trong **dưới 3 giây** (user-facing).
- **SC-006**: Admin xem tree mọi property **read-only** — **0** thao tác write floor thành công với role Admin (negative test).

## Assumptions

- **Room CRUD**, gallery, status update, room list filter theo Property/Floor thuộc **FR-08** — FR-07 chỉ hiển thị room nodes trên tree.
- Floor CRUD qua **modal** trên SCR-28 per screendesign (không dedicated Floor Management page bắt buộc v1 — `FloorManagementPage` optional nếu đã có route `/manager/floors`).
- Admin không gán Manager nhưng có quyền **đọc** toàn hệ thống — write floor vẫn do Manager trên property được gán.
- Xóa floor yêu cầu **zero rooms** — không kiểm tra booking trực tiếp tại floor level (rooms block delete).
- Property Selector mặc định chọn property đầu tiên trong danh sách hoặc nhớ lựa chọn gần nhất (session).
- Màn hình chính: **SCR-28 Structure Management**; entity-ui-mapping cũng đề cập TreeView component.
