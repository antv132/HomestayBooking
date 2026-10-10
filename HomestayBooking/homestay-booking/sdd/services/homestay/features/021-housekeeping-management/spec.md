# Feature Specification: FR-21 Housekeeping Management

**Feature Branch**: `023-housekeeping-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-21 Housekeeping Management — dựa vào docs (Specification_v2.md § FR-21, §5 HousekeepingTask, §10 Housekeeping acceptance, api-spec-by-screen SCR-40/SCR-60, screen.md, screendesign.md, entity-ui-mapping.md §1.10)"

**Phụ thuộc**: FR-04 (Booking Checked-out trigger); FR-23 (Room Inspection + payments complete trước check-out); FR-12 (thanh toán hoàn tất); FR-08 (Room status); FR-20 (Employee gán theo Property); FR-06 (Manager property scope). **Ranh giới**: FR-22 Employee Dashboard **hiển thị** danh sách task nhưng FR-21 owns **HousekeepingTask** data + SCR-40/SCR-60 workflows; FR-13 Maintenance; Manager check-in/check-out booking thuộc FR-04.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hệ thống tự động tạo tác vụ dọn phòng sau Check-out (Priority: P1)

Là **hệ thống**, khi Booking chuyển **Checked-out** (sau Room Inspection hoàn tất và mọi thanh toán đã xong), tôi muốn **tự động tạo HousekeepingTask** và chuyển phòng sang **Pending Cleaning**, để quy trình dọn phòng bắt đầu mà không cần Manager tạo thủ công.

**Why this priority**: FR-21 core automation — nền tảng toàn bộ housekeeping flow.

**Independent Test**: Booking đủ điều kiện check-out → sau khi Checked-out, tồn tại task status **Pending** cho room; room status = **Pending Cleaning**; không tạo trùng task cho cùng checkout.

**Acceptance Scenarios**:

1. **Given** Booking đã **Checked-out** hợp lệ (inspection Passed, payments settled), **When** sự kiện hoàn tất, **Then** tạo **một** HousekeepingTask status **Pending** gắn `propertyId`, `roomId`, chưa gán Employee.
2. **Given** task vừa tạo, **When** kiểm tra phòng, **Then** trạng thái phòng chuyển **Pending Cleaning** (không Available).
3. **Given** check-out chưa đủ điều kiện (inspection chưa xong hoặc còn nợ), **When** cố check-out, **Then** không tạo HousekeepingTask (FR-23/FR-12 — ngoài FR-21 nhưng là precondition).
4. **Given** đã có task Pending cho room từ checkout vừa rồi, **When** hệ thống xử lý lại, **Then** không tạo task trùng (assumption: idempotent theo booking/room checkout event).
5. **Given** Manager xem SCR-40 sau checkout, **When** refresh, **Then** thấy task mới trong cột **To Do / Pending**.

---

### User Story 2 - Manager xem và quản lý danh sách tác vụ dọn phòng (Priority: P1)

Là **Manager** được gán Property, tôi muốn xem **danh sách/bảng** HousekeepingTask theo Property (Pending, In Progress, Completed) với thông tin phòng và nhân viên được gán, để điều phối dọn phòng (SCR-40).

**Why this priority**: FR-21 "Theo dõi lịch sử tác vụ"; SCR-40 board/list là màn hình quản lý chính.

**Independent Test**: Manager mở Housekeeping Tasks → thấy task theo property; filter/status columns; không thấy property khác.

**Acceptance Scenarios**:

1. **Given** Manager Property A, **When** mở SCR-40 với `propertyId` = A, **Then** chỉ thấy HousekeepingTask thuộc Property A.
2. **Given** có task ở các trạng thái Pending, In Progress, Completed, **When** xem board, **Then** task hiển thị đúng cột/trạng thái với room number và assignee (nếu có).
3. **Given** Manager Property B, **When** cố truy cập task Property A, **Then** từ chối (`UNAUTHORIZED_PROPERTY_ACCESS`).
4. **Given** không có task, **When** mở SCR-40, **Then** empty state thân thiện.
5. **Given** **Admin**, **When** xem housekeeping, **Then** chỉ **đọc** danh sách (assumption: access matrix Admin R-only).

---

### User Story 3 - Manager gán Employee thực hiện dọn phòng (Priority: P1)

Là **Manager**, tôi muốn **gán** Employee thuộc cùng Property vào HousekeepingTask (hoặc tạo task thủ công cho phòng cần dọn), để nhân viên biết ai phụ trách (SCR-40 Drawer/Modal).

**Why this priority**: FR-21 "Manager gán Employee thực hiện dọn phòng".

**Independent Test**: Manager gán Employee Property A vào task Pending → `assignedEmployeeId` lưu; Employee khác Property bị từ chối.

**Acceptance Scenarios**:

1. **Given** task **Pending** chưa gán, **When** Manager chọn Employee thuộc Property A, **Then** gán thành công và hiển thị tên trên task card.
2. **Given** Employee thuộc Property B, **When** Manager Property A cố gán, **Then** từ chối — task chỉ gán Employee **cùng Property**.
3. **Given** Manager, **When** tạo task thủ công cho `roomId` + `assigneeId` (assumption: phòng đang Pending Cleaning), **Then** task tạo với status Pending hoặc đã gán tùy payload.
4. **Given** task **Completed** hoặc **Cancelled**, **When** cố đổi assignee, **Then** từ chối (assumption: chỉ Pending/In Progress cho phép reassign).
5. **Given** Employee được gán, **When** Employee mở SCR-60, **Then** thấy task trong danh sách được gán cho mình.

---

### User Story 4 - Employee bắt đầu dọn phòng (Priority: P1)

Là **Employee** được gán, tôi muốn **bắt đầu** tác vụ dọn phòng từ workspace, để cập nhật tiến độ và báo phòng đang được dọn (SCR-60).

**Why this priority**: FR-21 "Employee bắt đầu dọn phòng → phòng Cleaning In Progress".

**Independent Test**: Employee tap **Start** trên task được gán → task In Progress; room Cleaning In Progress; `startedAt` ghi nhận.

**Acceptance Scenarios**:

1. **Given** Employee được gán task **Pending**, **When** chọn **Start / In Progress**, **Then** task chuyển **In Progress** và `startedAt` được set.
2. **Given** task chuyển In Progress, **When** kiểm tra phòng, **Then** room status chuyển **Cleaning In Progress**.
3. **Given** Employee **không** được gán task, **When** cố cập nhật, **Then** từ chối.
4. **Given** Employee Property A, **When** xem SCR-60, **Then** chỉ thấy task được gán cho mình thuộc Property A.
5. **Given** task đã **In Progress** bởi Employee khác, **When** Employee khác cố start, **Then** từ chối (assumption: một assignee active tại một thời điểm).

---

### User Story 5 - Employee hoàn thành dọn phòng và phòng sẵn sàng (Priority: P1)

Là **Employee** đang dọn phòng, tôi muốn **đánh dấu hoàn thành** tác vụ, để phòng chuyển **Available** cho booking mới (SCR-60).

**Why this priority**: FR-21 "Employee hoàn thành → phòng Available"; đóng vòng lifecycle.

**Independent Test**: Employee **Finish** task In Progress → task Completed; room Available; không bypass được bởi Manager.

**Acceptance Scenarios**:

1. **Given** task **In Progress**, **When** Employee chọn **Finish / Completed**, **Then** task chuyển **Completed** và `completedAt` được set.
2. **Given** task Completed, **When** kiểm tra phòng, **Then** room status tự động chuyển **Available**.
3. **Given** task **Pending** (chưa start), **When** cố Complete trực tiếp, **Then** từ chối — phải qua In Progress (assumption: strict transition).
4. **Given** Manager cố đặt phòng **Available** thủ công khi task chưa Completed, **When** submit, **Then** hệ thống từ chối (FR-21 + FR-05).
5. **Given** task Completed, **When** Manager xem SCR-40, **Then** task nằm cột **Done** và vẫn xem được trong lịch sử.

---

### User Story 6 - Manager hủy tác vụ hoặc xem lịch sử (Priority: P2)

Là **Manager**, tôi muốn **hủy** tác vụ housekeeping khi không còn cần thiết (ví dụ booking reversal hiếm) và **xem lịch sử** task đã hoàn thành/hủy, để audit vận hành.

**Why this priority**: FR-21 status **Cancelled** + "theo dõi lịch sử"; ít frequent hơn happy path.

**Independent Test**: Manager cancel Pending task → status Cancelled; room handling per rule; completed tasks remain in history filter.

**Acceptance Scenarios**:

1. **Given** task **Pending** hoặc **In Progress**, **When** Manager hủy với lý do, **Then** task chuyển **Cancelled** và ghi `note`.
2. **Given** task **Cancelled** khi room đang Cleaning In Progress, **When** hủy, **Then** room chuyển trạng thái an toàn (assumption: về **Pending Cleaning** hoặc **Maintenance** — Manager quyết định follow-up thủ công FR-08).
3. **Given** Manager filter **Completed** trong 30 ngày, **When** xem, **Then** thấy lịch sử với startedAt/completedAt.
4. **Given** task **Completed**, **When** cố hủy, **Then** từ chối.
5. **Given** Customer hoặc Guest, **When** truy cập housekeeping management, **Then** từ chối.

---

### Edge Cases

- Checkout tạo task nhưng chưa có Employee nào tại Property — task Pending unassigned; Manager gán sau.
- Nhiều phòng checkout cùng lúc — mỗi room một task riêng.
- Employee Suspended khi đang có task In Progress — assumption: task vẫn active; Manager reassign (FR-20).
- Room **Maintenance** / **Out Of Service** — không auto Available khi complete; assumption: complete vẫn set Available trừ khi room flag Maintenance (Manager set sau).
- Task note — Employee/Manager có thể thêm ghi chú khi complete (assumption: optional `note` field).
- Timezone `startedAt`/`completedAt` — assumption **Asia/Ho_Chi_Minh**.
- Idempotent auto-create — không duplicate task per checkout event.
- Admin read-only — không gán/hủy task qua FR-21.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST **tự động tạo** HousekeepingTask status **Pending** khi Booking **Checked-out** đủ điều kiện (inspection + payments complete).
- **FR-002**: Khi task auto-create, phòng liên quan MUST chuyển **Pending Cleaning**.
- **FR-003**: **Manager** MUST xem danh sách HousekeepingTask theo **Property** được gán (SCR-40).
- **FR-004**: **Manager** MUST **gán** Employee vào task — Employee MUST thuộc **cùng Property**.
- **FR-005**: **Manager** MAY tạo task housekeeping **thủ công** cho phòng cần dọn (trong Property được gán).
- **FR-006**: **Employee** MUST xem và cập nhật **chỉ** task được gán cho mình (SCR-60).
- **FR-007**: Employee bắt đầu dọn (**In Progress**) MUST chuyển phòng sang **Cleaning In Progress**.
- **FR-008**: Employee hoàn thành (**Completed**) MUST chuyển phòng sang **Available**.
- **FR-009**: **Manager** MUST NOT chuyển phòng sang **Available** thủ công khi task housekeeping liên quan chưa **Completed** (enforce FR-05/FR-08).
- **FR-010**: Task status MUST tuân thủ: **Pending → In Progress → Completed**; **Cancelled** từ Pending/In Progress (Manager).
- **FR-011**: Hệ thống MUST **từ chối** gán Employee khác Property; Employee cập nhật task không được gán.
- **FR-012**: **Admin** MUST chỉ **xem** housekeeping (read-only) — không gán/hủy qua FR-21.
- **FR-013**: Hệ thống MUST lưu **lịch sử** task (`createdAt`, `startedAt`, `completedAt`) cho audit.
- **FR-014**: FR-21 MUST NOT thay thế FR-22 Dashboard — chỉ cung cấp dữ liệu task cho downstream.

### Key Entities

- **HousekeepingTask**: id, propertyId, roomId, assignedEmployeeId (optional), status (Pending | In Progress | Completed | Cancelled), note, createdAt, startedAt, completedAt, updatedAt.
- **Room** (FR-08): status transitions Pending Cleaning ↔ Cleaning In Progress ↔ Available driven by housekeeping.
- **Booking** (FR-04): Checked-out event triggers task creation (read/trigger only).
- **Employee** (FR-20): assignee must have ACTIVE assignment at property.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Sau check-out hợp lệ, Manager thấy task Pending mới trong **dưới 30 giây** trên SCR-40.
- **SC-002**: **100%** task auto-created gắn đúng `roomId` và `propertyId` của booking vừa check-out.
- **SC-003**: Employee hoàn thành dọn phòng và phòng Available trong **dưới 1 phút** thao tác trên SCR-60.
- **SC-004**: **100%** lần Manager cố bypass Available khi task chưa Completed bị từ chối.
- **SC-005**: **100%** gán Employee sai Property bị từ chối.
- **SC-006**: **100%** Employee chỉ cập nhật task được gán cho chính họ.
- **SC-007**: Manager tìm task theo phòng/trạng thái trong **dưới 30 giây** trên SCR-40.

## Assumptions

- **Screens**: Manager **SCR-40** Housekeeping Tasks (`/manager/housekeeping`); Employee **SCR-60** Housekeeping Workspace (`/employee/housekeeping`) — per `screen.md` (không nhầm figma SCR-40 Room Detail).
- **API baseline**: `GET/POST /manager/housekeeping-tasks`; `GET/PATCH /employee/housekeeping-tasks/{id}/status` — mở rộng assign/cancel/history trong planning.
- **Auto-create hook**: FR-04 check-out service gọi `HousekeepingTaskService.onBookingCheckedOut(bookingId)` — FR-21 owns implementation.
- **Preconditions**: Room Inspection Passed + all payments settled (FR-23, FR-12) trước Checked-out — owned upstream.
- **Manual task create**: cho phòng đã Pending Cleaning hoặc operational need — Manager Property-scoped.
- **Cancelled task + room**: về Pending Cleaning để Manager xử lý tiếp (assumption v1).
- **Strict transitions**: Pending → In Progress → Completed; không skip In Progress v1.
- **Admin**: read-only list only; Manager owns C/R/U on tasks.
- **FR-22 Dashboard**: consumes task counts/lists — not implemented in FR-21 UI beyond SCR-60.
- **Maintenance / Inspection** flows — FR-13, FR-23; FR-21 chỉ nhận trigger post-checkout.
