# Feature Specification: FR-22 Employee Dashboard

**Feature Branch**: `024-employee-dashboard`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-22 Employee Dashboard — dựa vào docs (Specification_v2.md § FR-22, §10 Employee Dashboard acceptance, api-spec-by-screen SCR-59, screen.md, screendesign.md SCR-59, component-library.md touch-friendly, entity-ui-mapping.md §1.10)"

**Phụ thuộc**: FR-01 (auth EMPLOYEE); FR-20 (Employee gán theo Property); FR-21 (HousekeepingTask — assigned tasks); FR-13 (MaintenanceTicket — assigned tickets); FR-23 (RoomInspection — inspections trước check-out); FR-04 (Booking Checked-in → inspection context). **Ranh giới**: FR-22 owns **SCR-59 Employee Dashboard** (read-only aggregate + navigation); **không** cập nhật trạng thái task trên dashboard (SCR-60 Housekeeping, SCR-61 Maintenance, SCR-62 Inspection Hub thuộc FR-21/FR-13/FR-23); Manager/Admin/Customer dashboards ngoài phạm vi.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Employee xem tổng quan KPI và lời chào (Priority: P1)

Là **Employee** đã đăng nhập, tôi muốn mở **Dashboard** và thấy ngay lời chào cá nhân cùng các chỉ số tác vụ đang chờ (housekeeping, maintenance, inspection), để nắm khối lượng công việc trong ngày mà không phải vào từng workspace (SCR-59).

**Why this priority**: FR-22 core "Dashboard tổng quan cho Employee"; KPI row + action cards là điểm vào chính.

**Independent Test**: Employee login → Dashboard hiển thị greeting + 3 KPI counts khớp dữ liệu thực; Employee khác không thấy số liệu của người khác.

**Acceptance Scenarios**:

1. **Given** Employee đăng nhập với tên **Nguyễn Văn A**, **When** mở SCR-59, **Then** hiển thị lời chào **Hello, Nguyễn Văn A** (hoặc tương đương tiếng Việt).
2. **Given** Employee được gán 3 HousekeepingTask status **Pending** hoặc **In Progress**, **When** mở Dashboard, **Then** KPI **Pending housekeeping** = 3.
3. **Given** Employee được gán 2 MaintenanceTicket status **Assigned** hoặc **In Progress**, **When** mở Dashboard, **Then** KPI **Pending maintenance** = 2.
4. **Given** Employee có 1 RoomInspection status **Pending** hoặc **In Progress** được gán, **When** mở Dashboard, **Then** KPI **Pending inspections** = 1 (assumption: card thứ 3 trên SCR-59).
5. **Given** Employee không có tác vụ nào, **When** mở Dashboard, **Then** KPI hiển thị **0**; không lỗi trang.
6. **Given** Manager hoặc Customer, **When** cố truy cập Employee Dashboard, **Then** từ chối hoặc redirect (FR-01).

---

### User Story 2 - Employee xem danh sách tác vụ Housekeeping được gán (Priority: P1)

Là **Employee**, tôi muốn thấy **danh sách tóm tắt** các tác vụ dọn phòng được gán cho mình (phòng, trạng thái, property), để biết cần làm gì trước khi vào workspace chi tiết (SCR-59 → SCR-60).

**Why this priority**: FR-22 explicit "Hiển thị danh sách tác vụ Housekeeping được gán".

**Independent Test**: Employee có 2 task Pending + 1 In Progress → Dashboard list hiển thị 3 mục đúng; click card KPI → điều hướng SCR-60.

**Acceptance Scenarios**:

1. **Given** Employee được gán HousekeepingTask **Pending** và **In Progress**, **When** mở Dashboard, **Then** thấy danh sách tóm tắt (assumption: tối đa 5 mục, ưu tiên Pending trước).
2. **Given** mỗi task trong list, **When** hiển thị, **Then** có **room name/number**, **status**, **property name** (nếu Employee gán nhiều property).
3. **Given** Employee click action card **Housekeeping** hoặc **Xem tất cả**, **When** navigate, **Then** tới **Housekeeping Workspace** (SCR-60).
4. **Given** Employee **không** được gán task nào, **When** mở section, **Then** empty state "Không có tác vụ dọn phòng".
5. **Given** task **Completed** hoặc **Cancelled**, **When** xem list mặc định, **Then** không hiển thị trong danh sách "đang chờ" (assumption: chỉ Pending + In Progress trên preview).

---

### User Story 3 - Employee xem danh sách tác vụ Maintenance được gán (Priority: P1)

Là **Employee**, tôi muốn thấy **danh sách tóm tắt** các yêu cầu bảo trì được Manager gán cho mình (loại sự cố, phòng, trạng thái), để ưu tiên xử lý (SCR-59 → SCR-61).

**Why this priority**: FR-22 explicit "Hiển thị danh sách tác vụ Maintenance được gán".

**Independent Test**: Employee có ticket Assigned → Dashboard list hiển thị; click Maintenance card → SCR-61.

**Acceptance Scenarios**:

1. **Given** Employee được gán MaintenanceTicket status **Assigned** hoặc **In Progress**, **When** mở Dashboard, **Then** thấy danh sách tóm tắt (assumption: tối đa 5 mục).
2. **Given** mỗi ticket, **When** hiển thị, **Then** có **issue type/title**, **room**, **status badge**.
3. **Given** Employee click action card **Maintenance**, **When** navigate, **Then** tới **Maintenance Workspace** (SCR-61).
4. **Given** không có ticket được gán, **When** mở section, **Then** empty state thân thiện.
5. **Given** ticket **Resolved** hoặc **Closed**, **When** xem preview mặc định, **Then** không nằm trong danh sách "đang chờ".

---

### User Story 4 - Employee xem tác vụ trong ngày hôm nay (Priority: P1)

Là **Employee**, tôi muốn thấy **tổng hợp tác vụ liên quan đến hôm nay** (check-out hôm nay cần inspection, housekeeping/maintenance due hôm nay), để tập trung công việc khẩn cấp trong ca làm việc.

**Why this priority**: FR-22 explicit "Hiển thị tác vụ trong ngày hôm nay".

**Independent Test**: Có booking check-out hôm nay với inspection Pending → xuất hiện trong section "Hôm nay"; task tạo hôm qua không xuất hiện (trừ khi vẫn pending và due today).

**Acceptance Scenarios**:

1. **Given** hôm nay theo múi giờ property (assumption: **Asia/Ho_Chi_Minh**), **When** Employee mở Dashboard, **Then** thấy section **Tác vụ hôm nay** gộp housekeeping, maintenance, inspection.
2. **Given** RoomInspection gắn booking có **check-out** hôm nay và status **Pending**, **When** hiển thị, **Then** inspection xuất hiện trong section hôm nay.
3. **Given** HousekeepingTask được gán với `createdAt` hôm nay hoặc room check-out hôm nay, **When** hiển thị, **Then** task xuất hiện trong section hôm nay (assumption: ưu tiên checkout-driven tasks).
4. **Given** không có tác vụ nào thuộc hôm nay, **When** mở section, **Then** empty state "Không có tác vụ hôm nay".
5. **Given** tác vụ hôm nay thuộc loại khác nhau, **When** hiển thị, **Then** có nhãn phân loại (Housekeeping / Maintenance / Inspection).

---

### User Story 5 - Employee xem tác vụ đã hoàn thành và đang chờ xử lý (Priority: P1)

Là **Employee**, tôi muốn thấy **tách biệt** số lượng và danh sách ngắn tác vụ **đang chờ xử lý** versus **đã hoàn thành** (trong ngày hoặc gần đây), để theo dõi tiến độ ca làm việc.

**Why this priority**: FR-22 explicit "Hiển thị tác vụ đã hoàn thành và đang chờ xử lý".

**Independent Test**: Employee hoàn thành 2 housekeeping hôm nay → Dashboard hiển thị Completed count = 2; Pending count khớp task chưa xong.

**Acceptance Scenarios**:

1. **Given** Employee có task đang chờ (Pending/In Progress/Assigned), **When** mở Dashboard, **Then** section **Đang chờ** hiển thị tổng count và preview list (assumption: tối đa 3 mỗi loại).
2. **Given** Employee đã hoàn thành task hôm nay (Housekeeping **Completed**, Maintenance **Resolved**, Inspection **Passed**), **When** mở Dashboard, **Then** section **Đã hoàn thành hôm nay** hiển thị count và preview.
3. **Given** Employee hoàn thành task **hôm qua**, **When** xem "Đã hoàn thành hôm nay", **Then** không tính vào count hôm nay (assumption: scope "today" only cho completed section).
4. **Given** Employee muốn xem lịch sử đầy đủ, **When** click link trên section, **Then** điều hướng tới workspace tương ứng với filter phù hợp (SCR-60/61/62).
5. **Given** không có task hoàn thành hôm nay, **When** mở section, **Then** hiển thị **0** hoặc empty message.

---

### User Story 6 - Employee xem danh sách Room Inspection được gán (Priority: P1)

Là **Employee**, tôi muốn thấy **danh sách kiểm tra phòng** được gán hoặc cần tôi thực hiện (booking sắp check-out, status Pending/In Progress), để ưu tiên inspection trước khi khách rời đi (SCR-59 → SCR-62).

**Why this priority**: FR-22 explicit "Hiển thị danh sách Room Inspection được gán"; FR-23 upstream.

**Independent Test**: Inspection Pending gán cho Employee → hiển thị trên Dashboard; click Inspection card → SCR-62.

**Acceptance Scenarios**:

1. **Given** RoomInspection status **Pending** hoặc **In Progress** với `inspectedBy` = Employee hoặc được gán cho Employee (assumption: align FR-23 assignment rule), **When** mở Dashboard, **Then** thấy list tóm tắt (assumption: tối đa 5).
2. **Given** mỗi inspection, **When** hiển thị, **Then** có **room**, **booking reference** hoặc **check-out date**, **status**.
3. **Given** Employee click action card **Inspections**, **When** navigate, **Then** tới **Room Inspection Hub** (SCR-62).
4. **Given** inspection **Passed** hoặc **Failed With Damage**, **When** xem preview mặc định, **Then** không nằm trong danh sách "cần làm" (có thể xuất hiện trong "đã hoàn thành hôm nay" nếu hoàn thành hôm nay).
5. **Given** Employee Property A, **When** xem inspections, **Then** chỉ thấy inspection thuộc Property được gán (FR-20/FR-23).

---

### Edge Cases

- Employee **Suspended** — chặn login trước Dashboard (FR-01).
- Employee gán **nhiều Property** — mọi list/KPI scoped theo tất cả property được gán; hiển thị property name khi > 1 property.
- Employee **không** được gán property — empty dashboard với hướng dẫn liên hệ Manager (assumption).
- Timezone **Asia/Ho_Chi_Minh** cho định nghĩa "hôm nay".
- Dashboard load partial failure — thông báo lỗi thân thiện + retry (assumption).
- **100%** dữ liệu scoped theo Employee đăng nhập — không leak task của Employee khác.
- Mobile-first: action cards touch-friendly (min-height 100px per component-library).
- Task counts trên KPI MUST khớp preview lists (không mismatch).
- FR-22 **read-only** — không Start/Finish/Pass trên SCR-59; thao tác trên SCR-60/61/62.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Employee MUST xem Dashboard tổng quan sau đăng nhập (SCR-59).
- **FR-002**: Dashboard MUST hiển thị lời chào cá nhân dựa trên tên Employee.
- **FR-003**: Dashboard MUST hiển thị KPI: **pending housekeeping**, **pending maintenance**, **pending inspections** (đếm tác vụ chưa hoàn thành được gán cho Employee).
- **FR-004**: Dashboard MUST hiển thị **3 action cards** lớn (Housekeeping, Maintenance, Inspections) với số pending và điều hướng tới workspace tương ứng.
- **FR-005**: Dashboard MUST hiển thị danh sách tóm tắt **HousekeepingTask được gán** (Pending + In Progress).
- **FR-006**: Dashboard MUST hiển thị danh sách tóm tắt **MaintenanceTicket được gán** (Assigned + In Progress).
- **FR-007**: Dashboard MUST hiển thị section **Tác vụ hôm nay** gộp các loại task liên quan ngày hiện tại.
- **FR-008**: Dashboard MUST tách biệt **đang chờ xử lý** và **đã hoàn thành hôm nay** với count và preview list.
- **FR-009**: Dashboard MUST hiển thị danh sách tóm tắt **RoomInspection được gán** (Pending + In Progress).
- **FR-010**: Tất cả dữ liệu Dashboard MUST **scoped** theo Employee đang đăng nhập và Property được gán.
- **FR-011**: FR-22 MUST **từ chối** truy cập Dashboard cho non-Employee roles.
- **FR-012**: Dashboard MUST là **read-only aggregate** — không cập nhật trạng thái task/inspection trên màn hình này.
- **FR-013**: Empty states MUST thân thiện khi không có dữ liệu từng section.
- **FR-014**: Dashboard MUST hỗ trợ **mobile-first** layout với thành phần touch-friendly.

### Key Entities (read aggregates)

- **HousekeepingTask** (FR-21): assigned tasks, status Pending/In Progress/Completed; room, property.
- **MaintenanceTicket** (FR-13): assigned tickets, status Assigned/In Progress/Resolved; issue type, room.
- **RoomInspection** (FR-23): assigned inspections, status Pending/In Progress/Passed/Failed; booking check-out date, room.
- **EmployeePropertyAssignment** (FR-20): scope filter cho mọi aggregate.
- **Booking** (FR-04): check-out date context cho inspection "hôm nay".

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Employee thấy KPI Dashboard trong **dưới 3 giây** sau đăng nhập (95% sessions).
- **SC-002**: **100%** KPI counts khớp với số task thực tế được gán cho Employee đó (housekeeping + maintenance + inspection).
- **SC-003**: Employee mở workspace tương ứng từ action card trong **1 tap/click** từ Dashboard.
- **SC-004**: **100%** truy cập Dashboard bởi non-Employee bị chặn hoặc redirect.
- **SC-005**: Dashboard load thành công khi Employee không có tác vụ — empty states, không crash.
- **SC-006**: **95%** Employee xác định được tác vụ ưu tiên hôm nay trong **dưới 10 giây** trên Dashboard.
- **SC-007**: Section "hôm nay" và KPI preview tải trong **dưới 3 giây** cùng payload Dashboard (p95).

## Assumptions

- **Screen ID**: SCR-59 per `screen.md`, `screendesign.md`, `api-spec-by-screen.md`; route `/employee/dashboard` (assumption align Customer `/customer/dashboard`).
- **API**: composite read endpoint (assumption: mở rộng `GET /api/v1/employee/kpis` hoặc `GET /api/v1/employee/dashboard`) trả KPI + preview lists một round-trip — chi tiết kỹ thuật thuộc planning phase.
- **Pending housekeeping**: HousekeepingTask WHERE `assignedEmployeeId` = current AND `status IN (PENDING, IN_PROGRESS)`.
- **Pending maintenance**: MaintenanceTicket WHERE `assignedEmployeeId` = current AND `status IN (ASSIGNED, IN_PROGRESS)`.
- **Pending inspections**: RoomInspection WHERE assignee/inspectedBy = current AND `status IN (PENDING, IN_PROGRESS)`.
- **Preview list limits**: tối đa **5** mục mỗi loại trên Dashboard; full list trên SCR-60/61/62.
- **Completed today**: Housekeeping **Completed**, Maintenance **Resolved**, Inspection **Passed** với completion/inspected timestamp trong ngày hôm nay.
- **Today's tasks**: booking `checkOut` = today OR task `createdAt` = today OR explicit due date = today (assumption: ưu tiên checkout-driven).
- **Greeting**: dùng `fullName` từ FR-01 auth profile.
- **No new domain tables**: FR-22 read-only aggregate; không tạo bảng dashboard riêng v1.
- FR-22 **does not** implement task status updates (owned by FR-21, FR-13, FR-23).
