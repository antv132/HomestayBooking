# Feature Specification: FR-05 Availability Calendar

**Feature Branch**: `007-availability-calendar`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-05 Availability Calendar — dựa vào docs (Specification_v2.md § FR-05, §5 Room status, §10 acceptance, api-spec-by-screen SCR-09/SCR-33, screen.md, screendesign.md, entity-ui-mapping.md, component-library.md)"

**Phụ thuộc**: Dữ liệu Room/Property (FR-06/FR-08); trạng thái booking (FR-04); chuyển trạng thái dọn phòng (FR-21). **Ranh giới**: FR-03 cung cấp lịch read-only đơn giản cho Guest (SCR-09 — booked vs available cho chọn ngày); FR-05 sở hữu **mô hình trạng thái đầy đủ 8 giá trị**, **tính toán hiển thị theo ngày**, và **cập nhật thủ công Manager/Admin** (SCR-33). Push real-time WebSocket thuộc FR-15 — FR-05 yêu cầu calendar phản ánh thay đổi trong vòng refresh hoặc khi FR-15 có sẵn.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Khách xem lịch phòng với trạng thái đầy đủ (Priority: P1)

Là **Guest hoặc Customer**, tôi muốn xem lịch theo tháng của một phòng với màu/nhãn phân biệt các trạng thái (Available, Pending Deposit, Reserved, Occupied, Pending Cleaning, Cleaning In Progress, Maintenance, Out Of Service) để biết ngày nào có thể đặt phòng.

**Why this priority**: FR-05 bullet cốt lõi — lịch trống với 8 trạng thái; mở rộng SCR-09 so với FR-03 (chỉ booked/available).

**Independent Test**: Mở SCR-09 cho phòng có booking + maintenance → mỗi ngày hiển thị đúng một trạng thái ưu tiên; ngày Available cho phép chọn; ngày không Available không chọn được.

**Acceptance Scenarios**:

1. **Given** phòng có booking Confirmed 15–17/6, **When** xem calendar tháng 6, **Then** ngày 15–16 (hoặc theo quy tắc half-open check-in/check-out) hiển thị **Reserved** hoặc **Occupied** tùy giai đoạn lưu trú; ngày khác **Available** nếu không conflict.
2. **Given** phòng đang **Maintenance** do Manager khóa, **When** Guest xem calendar, **Then** các ngày trong khoảng bảo trì hiển thị **Maintenance** và không chọn được.
3. **Given** phòng **Pending Cleaning** sau checkout, **When** xem calendar, **Then** ngày hiện tại (hoặc ngày áp dụng) hiển thị **Pending Cleaning** / **Cleaning In Progress** — không bookable.
4. **Given** user chọn khoảng ngày chỉ gồm ngày **Available**, **When** xác nhận, **Then** quay chi tiết phòng với check-in/check-out (chuẩn bị FR-04).
5. **Given** user chuyển tháng/năm, **When** load calendar, **Then** cập nhật trạng thái từng ngày cho tháng mới.

---

### User Story 2 - Manager xem lịch phòng trong property (Priority: P1)

Là **Manager**, tôi muốn xem lịch availability của từng phòng (hoặc overview nhiều phòng) thuộc property được gán để nắm tình trạng vận hành và lên kế hoạch.

**Why this priority**: Manager cần context trước khi khóa phòng hoặc xử lý booking; bổ sung SCR-33.

**Independent Test**: Manager chọn property → chọn phòng → calendar hiển thị 8 trạng thái; không thấy phòng property khác.

**Acceptance Scenarios**:

1. **Given** Manager được gán property P, **When** mở lịch phòng thuộc P, **Then** hiển thị calendar với legend 8 trạng thái và tooltip/ngày chi tiết (booking ref optional).
2. **Given** Manager truy cập phòng property không được gán, **When** mở lịch, **Then** từ chối truy cập.
3. **Given** phòng có booking Pending Deposit, **When** xem calendar, **Then** ngày bị giữ hiển thị **Pending Deposit**.

---

### User Story 3 - Manager cập nhật trạng thái phòng thủ công (Priority: P1)

Là **Manager** (hoặc **Admin**), tôi muốn khóa phòng sang Maintenance hoặc Out Of Service (kèm khoảng ngày và lý do) và chỉ chuyển về Available khi quy trình dọn phòng đã hoàn tất, để kiểm soát inventory ngoài luồng booking tự động.

**Why this priority**: FR-05 bullet thứ hai — cập nhật thủ công + cấm Available khi housekeeping chưa xong; SCR-33.

**Independent Test**: Manager set Maintenance 1–5/7 → calendar + discovery loại ngày đó; cố set Available khi HousekeepingTask chưa Completed → từ chối.

**Acceptance Scenarios**:

1. **Given** phòng không có booking active trong khoảng ngày chọn, **When** Manager đặt **Maintenance** với start/end date và reason, **Then** room status/calendar phản ánh Maintenance trong khoảng; không thể đặt phòng mới (FR-04).
2. **Given** phòng có booking overlap khoảng Maintenance, **When** Manager submit, **Then** từ chối với thông báo conflict.
3. **Given** phòng **Pending Cleaning** với HousekeepingTask chưa **Completed**, **When** Manager cố đặt **Available** thủ công, **Then** hệ thống từ chối (FR-05 + FR-21).
4. **Given** HousekeepingTask **Completed**, **When** Manager hoặc hệ thống chuyển **Available**, **Then** calendar cập nhật và phòng bookable trở lại.
5. **Given** Admin thao tác trên bất kỳ property, **When** cập nhật status, **Then** cùng quy tắc như Manager (Admin toàn property).

---

### User Story 4 - Employee xem lịch phòng (read-only) (Priority: P2)

Là **Employee**, tôi muốn xem lịch trạng thái phòng trong property được gán (read-only) để biết phòng nào đang dọn, bảo trì, hoặc có khách.

**Why this priority**: §10 acceptance — Employee xem danh sách/lịch phòng trong property được gán.

**Independent Test**: Employee login → xem calendar phòng thuộc property assignment → không có nút cập nhật status.

**Acceptance Scenarios**:

1. **Given** Employee được gán property P, **When** xem calendar phòng thuộc P, **Then** hiển thị giống Manager nhưng **không** có action cập nhật status.
2. **Given** Employee không được gán property, **When** truy cập, **Then** từ chối.

---

### User Story 5 - Ưu tiên trạng thái khi nhiều nguồn (Priority: P2)

Là **hệ thống**, tôi cần quy tắc rõ ràng khi một ngày có thể map nhiều nguồn (booking + maintenance block + housekeeping) để calendar luôn hiển thị **một** trạng thái nhất quán.

**Why this priority**: Tránh hiển thị mâu thuẫn; §10 "displays correct room statuses".

**Independent Test**: Seed conflict scenarios → API trả đúng một status/day theo priority table.

**Acceptance Scenarios**:

1. **Given** cùng ngày vừa có booking Occupied vừa có maintenance block (edge admin error prevented), **When** tính calendar, **Then** **Occupied** (booking) thắng Maintenance — hoặc maintenance không được tạo nếu booking exists (US3 scenario 2).
2. **Given** ngày checkout vừa xong, room **Pending Cleaning**, **When** hiển thị, **Then** **Pending Cleaning** thay **Available** cho đến khi housekeeping hoàn tất.
3. **Given** booking Pending Deposit, **When** hiển thị ngày hold, **Then** **Pending Deposit** (không hiển thị Available).

---

### Edge Cases

- Phòng **Out Of Service** dài hạn — toàn bộ ngày trong range hiển thị Out Of Service; không bookable.
- Booking **Cancelled** / **No-show** — ngày được giải phóng → Available (trừ khi housekeeping/maintenance khác).
- Timezone: ngày calendar theo **Asia/Ho_Chi_Minh** (property local) — half-open [check-in, check-out).
- Guest không thấy thông tin nhạy cảm (tên khách) trên calendar — chỉ trạng thái/màu.
- Manager đặt Maintenance không end date — coi là open-ended until cleared (assumption: bắt buộc end date — see Assumptions).
- Lịch tháng không có dữ liệu — empty month vẫn render grid, all Available nếu room operational.
- Concurrent: Manager update status + booking create cùng lúc — FR-04 inventory lock thắng; calendar eventual consistency ≤ refresh interval.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST hiển thị **Availability Calendar theo từng phòng** theo tháng/năm với **8 trạng thái**: Available, Pending Deposit, Reserved, Occupied, Pending Cleaning, Cleaning In Progress, Maintenance, Out Of Service.
- **FR-002**: Hệ thống MUST cho phép **Guest và Customer** xem calendar read-only (SCR-09) và chọn chỉ các ngày **bookable** (chủ yếu Available; có thể chọn range toàn Available).
- **FR-003**: Hệ thống MUST cho phép **Manager** xem calendar phòng thuộc **property được gán** only.
- **FR-004**: Hệ thống MUST cho phép **Admin** xem và cập nhật calendar/status trên mọi property.
- **FR-005**: Hệ thống MUST cho phép **Manager và Admin** cập nhật trạng thái phòng thủ công sang **Maintenance** hoặc **Out Of Service** với khoảng ngày và lý do (SCR-33).
- **FR-006**: Hệ thống MUST **từ chối** chuyển phòng sang **Available** thủ công khi **HousekeepingTask** liên quan chưa **Completed** (FR-21).
- **FR-007**: Hệ thống MUST **từ chối** khóa Maintenance/Out Of Service nếu **booking active** overlap khoảng ngày.
- **FR-008**: Calendar MUST phản ánh trạng thái từ **booking lifecycle** (FR-04): Pending Deposit, Reserved (Confirmed pre check-in), Occupied (Checked-in).
- **FR-009**: Calendar MUST phản ánh trạng thái **housekeeping** (FR-21): Pending Cleaning, Cleaning In Progress.
- **FR-010**: Hệ thống MUST áp dụng **quy tắc ưu tiên** khi xác định trạng thái hiển thị mỗi ngày (documented in Assumptions) — một ngày một trạng thái.
- **FR-011**: **Employee** MUST xem calendar read-only trong property được gán; MUST NOT cập nhật status.
- **FR-012**: Khi status thay đổi (booking, housekeeping, manual), calendar MUST cập nhật cho user trong **≤ 30 giây** (refresh hoặc real-time khi FR-15 sẵn sàng).

### Key Entities

- **Room**: `status` enum 8 giá trị; operational state hiện tại.
- **RoomStatusBlock** (logic): khoảng ngày Maintenance/Out Of Service do Manager tạo — manual block trên calendar.
- **Booking**: nguồn Pending Deposit, Reserved, Occupied theo overlap ngày (FR-04).
- **HousekeepingTask**: nguồn Pending Cleaning, Cleaning In Progress (FR-21).
- **CalendarDayStatus** (derived): `{ date, status, bookable }` — không persist, tính khi query.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: **100%** ngày trong tháng test hiển thị đúng 1 trong 8 trạng thái so với nguồn dữ liệu (booking + room + housekeeping + manual block).
- **SC-002**: Guest chọn ngày và quay checkout trong **dưới 1 phút** từ SCR-09 (US1 flow).
- **SC-003**: Manager cập nhật Maintenance block trong **dưới 2 phút** từ SCR-33.
- **SC-004**: **0%** trường hợp Manager set Available thành công khi housekeeping chưa Completed (test 100% reject).
- **SC-005**: **100%** manual Maintenance blocks bị từ chối khi overlap booking active.
- **SC-006**: Calendar refresh sau thay đổi booking/status trong **≤ 30 giây** (manual refresh hoặc push).

## Assumptions

- **Status priority** (cao → thấp) cho một ngày: Occupied > Pending Deposit > Reserved > Cleaning In Progress > Pending Cleaning > Maintenance > Out Of Service > Available. Booking states override manual block khi overlap không được phép tạo.
- **Reserved** map từ booking **Confirmed** (chưa check-in); **Occupied** map từ **Checked-in** và các trạng thái in-stay trước checkout.
- **Half-open stay range**: check-in inclusive, check-out exclusive (đêm lưu trú chuẩn khách sạn).
- Manual Maintenance/Out Of Service **bắt buộc** có start date; end date bắt buộc (không open-ended) — Manager phải chọn range hoàn chỉnh trên SCR-33.
- Guest calendar **ẩn** chi tiết booking (chỉ màu + label trạng thái); Manager calendar có thể xem booking id/link.
- FR-03 endpoint `GET /rooms/{id}/availability?month&year` được **mở rộng** bởi FR-05 trả `{ days: [{ date, status, bookable }] }` thay vì chỉ `bookedDates[]` — backward compatible layer optional trong plan.
- Admin = Manager rules + all properties scope.
- Employee assignment to property qua cùng cơ chế ManagerPropertyAssignment / EmployeePropertyAssignment (FR-06) — chi tiết HR thuộc FR-06.
