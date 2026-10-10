# Feature Specification: FR-08 Room Management

**Feature Branch**: `010-room-management`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-08 Room Management — dựa vào docs (Specification_v2.md § FR-08, §5 Room/RoomImage, §10 Room Management acceptance, api-spec-by-screen SCR-29–32, screen.md, screendesign.md, entity-ui-mapping.md §1.3–1.5, frontend RoomListPage, AddRoomPage, EditRoomPage, RoomGalleryPage)"

**Phụ thuộc**: FR-06 (Property scope); FR-07 (Floor hierarchy). **Ranh giới**: FR-05/SCR-33 manual status (Maintenance/OOS/date range, calendar); FR-04 booking lifecycle status transitions; FR-03 Guest discovery read; FR-09 Admin Customer — không thuộc FR-08. Employee SCR-65 read-only list thuộc FR-08 scope.

## Clarifications

### Session 2026-07-23

- Q: Soft-delete vs hard-delete cho Room khi xóa? → A: Soft-delete (`deletedAt`) khi không còn booking active/blocking; ẩn khỏi Manager/Guest list; giữ row để booking history không gãy FK.
- Q: Đổi roomNumber khi đã có booking history? → A: Cho phép đổi bất kỳ lúc nào, miễn unique trong property (booking gắn roomId).
- Q: Max ảnh/room và kích thước file UAT? → A: Tối đa 10 ảnh/room; jpeg/png/webp; tối đa 10MB/file (khớp constitutional upload).
- Q: Status lúc tạo phòng mới? → A: Luôn AVAILABLE khi create; status khác chỉ qua FR-05 SCR-33 hoặc booking/HK lifecycle.
- Q: Đổi floor khi phòng Occupied? → A: Cho phép đổi floor trong cùng property kể cả Occupied; floor khác property từ chối.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manager xem và lọc danh sách phòng (Priority: P1)

Là **Manager**, tôi muốn xem danh sách phòng thuộc Property được gán, tìm kiếm và lọc theo Property, Floor, trạng thái, loại phòng để quản lý inventory nhanh.

**Why this priority**: FR-08 bullet lọc danh sách; SCR-29; nền tảng cho mọi thao tác phòng.

**Independent Test**: Manager mở SCR-29 → chỉ phòng thuộc property được gán; filter floor/status; search theo room number; row actions Edit/Gallery.

**Acceptance Scenarios**:

1. **Given** Manager được gán property P, **When** mở danh sách phòng, **Then** chỉ hiển thị phòng thuộc P (và property khác được gán nếu nhiều).
2. **Given** danh sách phòng, **When** lọc theo Floor và Status, **Then** kết quả khớp filter.
3. **Given** Manager search "101", **When** apply, **Then** chỉ phòng có room number khớp.
4. **Given** bảng phòng, **When** xem, **Then** cột Room Number, Type, Floor, Price, Status badge hiển thị đúng.
5. **Given** Manager truy cập property không được gán qua filter, **When** load, **Then** không có dữ liệu hoặc từ chối.

---

### User Story 2 - Manager tạo phòng mới (Priority: P1)

Là **Manager**, tôi muốn thêm phòng vào đúng Property và Floor được gán với thông tin cơ bản (số phòng, loại, giá, sức chứa, mô tả, tiện ích) để phòng xuất hiện trong discovery và booking.

**Why this priority**: FR-08 "Manager thêm phòng vào đúng Property/Floor"; SCR-30.

**Independent Test**: Manager mở Add Room → chọn floor thuộc assigned property → Create → phòng xuất hiện SCR-29 với status Available mặc định.

**Acceptance Scenarios**:

1. **Given** Manager chọn property P và floor F thuộc P, **When** tạo phòng với room number, room type, price, capacity hợp lệ, **Then** phòng lưu với `propertyId=P`, `floorId=F`, status **Available**.
2. **Given** floor F không thuộc property P, **When** submit, **Then** từ chối validation.
3. **Given** trùng room number trong cùng property, **When** tạo, **Then** từ chối (phân biệt phòng theo property scope).
4. **Given** trường bắt buộc thiếu, **When** submit, **Then** thông báo lỗi rõ ràng.
5. **Given** Manager không được gán property, **When** tạo phòng, **Then** từ chối truy cập.

---

### User Story 3 - Manager chỉnh sửa thông tin phòng (Priority: P1)

Là **Manager**, tôi muốn cập nhật thông tin phòng (floor, loại, giá, sức chứa, mô tả, tiện ích) trong property được gán để duy trì dữ liệu chính xác.

**Why this priority**: FR-08 "Manager chỉnh sửa thông tin phòng"; SCR-31.

**Independent Test**: Edit room → Save → SCR-29 và Guest detail (FR-03) phản ánh thay đổi; không đổi propertyId sang property khác.

**Acceptance Scenarios**:

1. **Given** phòng thuộc property được gán, **When** Manager cập nhật mô tả, giá, amenities, **Then** thay đổi được lưu.
2. **Given** Manager đổi floor, **When** floor mới thuộc cùng property, **Then** cho phép; floor khác property → từ chối.
3. **Given** phòng có booking active hoặc history, **When** Manager đổi `roomNumber` sang giá trị còn trống trong property, **Then** cho phép; trùng số → từ chối.
4. **Given** phòng có booking active, **When** Manager đổi giá, **Then** giá mới áp dụng cho booking **mới** (booking hiện tại giữ snapshot FR-04).
5. **Given** Customer hoặc Employee, **When** cố sửa phòng, **Then** từ chối.

---

### User Story 4 - Manager xóa phòng (Priority: P1)

Là **Manager**, tôi muốn xóa phòng không còn sử dụng khi không có đặt phòng đang diễn ra để dọn dẹp inventory.

**Why this priority**: FR-08 "Manager xóa phòng (chỉ khi không có booking active)"; §10 acceptance.

**Independent Test**: Delete room không booking → thành công; room có booking PENDING_DEPOSIT/CONFIRMED/CHECKED_IN → từ chối.

**Acceptance Scenarios**:

1. **Given** phòng không có booking active/blocking, **When** Manager xóa, **Then** phòng được **soft-delete** (`deletedAt` set); ẩn khỏi SCR-29 và Guest discovery; booking lịch sử vẫn tham chiếu được room.
2. **Given** phòng có booking active (theo FR-04 blocking statuses), **When** Manager xóa, **Then** từ chối với thông báo rõ ràng.
3. **Given** phòng thuộc property không được gán, **When** xóa, **Then** từ chối.

---

### User Story 5 - Manager quản lý gallery ảnh phòng (Priority: P1)

Là **Manager**, tôi muốn upload, xóa, sắp xếp ảnh phòng và đặt ảnh chính để Guest thấy gallery đẹp trên trang chi tiết (FR-03).

**Why this priority**: FR-08 "Quản lý hình ảnh phòng và thứ tự hiển thị"; SCR-32; §5 RoomImage constraint one primary.

**Independent Test**: Manager mở Gallery → upload 3 ảnh → set primary → reorder → Guest detail hiển thị primary first.

**Acceptance Scenarios**:

1. **Given** phòng thuộc property được gán, **When** Manager upload ảnh hợp lệ, **Then** ảnh xuất hiện trong gallery với sort order.
2. **Given** nhiều ảnh, **When** Manager đặt một ảnh làm primary, **Then** chỉ **một** ảnh primary per room (§5 partial unique).
3. **Given** Manager reorder ảnh, **When** lưu, **Then** thứ tự phản ánh trên Guest gallery (FR-03).
4. **Given** Manager xóa ảnh, **When** confirm, **Then** ảnh biến mất; nếu xóa primary thì hệ thống gán primary mới hoặc yêu cầu chọn primary (assumption: auto-promote first remaining).
5. **Given** file không phải jpeg/png/webp hoặc >10MB, hoặc room đã đủ 10 ảnh, **When** upload, **Then** từ chối validation.

---

### User Story 6 - Employee xem danh sách phòng (read-only) (Priority: P2)

Là **Employee**, tôi muốn xem danh sách phòng trong property được gán (read-only) để tham khảo nhanh khi làm việc vận hành.

**Why this priority**: FR-08 bullet Employee; SCR-65; §10 acceptance.

**Independent Test**: Employee login → SCR-65 list → không có Add/Edit/Delete/Gallery actions; chỉ property được gán.

**Acceptance Scenarios**:

1. **Given** Employee được gán property P, **When** xem danh sách phòng, **Then** chỉ phòng thuộc P, read-only.
2. **Given** Employee, **When** cố tạo/sửa/xóa phòng, **Then** từ chối.
3. **Given** Employee không được gán property, **When** truy cập, **Then** từ chối hoặc danh sách rỗng.

---

### Edge Cases

- Property INACTIVE — Manager vẫn quản lý phòng nội bộ; Guest discovery ẩn (FR-03).
- Phòng soft-deleted (`deletedAt` not null) — không hiện SCR-29 / Guest discovery; không tạo booking mới; booking lịch sử vẫn đọc được.
- Phòng status Occupied/Maintenance — vẫn edit metadata (trừ soft-delete khi booking active); manual status change qua SCR-33 (FR-05).
- Upload ảnh trùng URL — cho phép hoặc dedupe (assumption: allow, user manages gallery).
- PricingRule phức tạp (weekend surcharge) — optional v1: core `pricePerNight` on Room; PricingRule entity mở rộng sau (api-spec pricingRule optional).
- Di chuyển phòng sang floor khác khi Occupied / booking future — **cho phép** nếu cùng property (calendar/booking theo roomId); floor khác property → từ chối.
- Đổi roomNumber sau tạo — cho phép nếu unique trong property (Clarifications Session 2026-07-23).
- Admin — read-only trên ma trận quyền §4; không CRUD room qua Admin UI trong FR-08 (Manager only write).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: **Manager** MUST xem **danh sách phòng** thuộc property được gán với phân trang, tìm kiếm, lọc property/floor/status/room type (SCR-29).
- **FR-002**: **Manager** MUST **tạo** phòng gắn đúng **PropertyId** và **FloorId** trong phạm vi được gán (SCR-30).
- **FR-003**: **Manager** MUST **cập nhật** thông tin phòng (floor cùng property — kể cả khi Occupied, room type, area, price, capacity, description, amenities) (SCR-31). MUST từ chối đổi floor sang property khác.
- **FR-004**: **Manager** MUST **soft-delete** phòng chỉ khi **không** có booking **active/blocking** (FR-04 blocking statuses); MUST set `deletedAt` và ẩn phòng khỏi list/discovery; MUST NOT hard-delete row khi còn booking history.
- **FR-005**: Phòng mới MUST mặc định status **Available** trên SCR-30; MUST NOT cho chọn Maintenance/OOS trên form tạo. Chuyển trạng thái vận hành sau đó thuộc FR-05 / booking / HK.
- **FR-006**: **Manager** MUST quản lý **RoomImage** gallery: upload, delete, sort order, **một** primary image per room (SCR-32).
- **FR-007**: Hệ thống MUST **từ chối** trùng **room number** trong cùng **property** (create/update). Manager MAY đổi `roomNumber` bất kỳ lúc nào (kể cả có booking history/active) nếu giá trị mới vẫn unique trong property.
- **FR-008**: Hệ thống MUST **từ chối** mọi thao tác write khi Manager/Employee **không** có quyền property scope (FR-06).
- **FR-009**: **Employee** MUST xem danh sách phòng **read-only** trong property được gán (SCR-65); MUST NOT create/update/delete.
- **FR-010**: Danh sách và detail MUST **hiển thị** trạng thái phòng (8 giá trị §5); **cập nhật thủ công** Maintenance/OOS/calendar thuộc **FR-05** SCR-33 (FR-08 không duplicate).
- **FR-011**: Thay đổi **pricePerNight** MUST NOT alter **TotalAmount** của booking đã snapshot (FR-04).
- **FR-012**: Gallery MUST validate loại file ảnh **jpeg/png/webp** và kích thước tối đa **10MB**/file (constitutional upload cap); MUST từ chối khi room đã có **10** ảnh (max 10 images per room).

### Key Entities

- **Room**: id, propertyId, floorId, roomNumber, roomType, area, pricePerNight, capacity, status, description, amenities (optional tags), deletedAt (null = active; set on soft-delete), createdAt, updatedAt.
- **RoomImage**: id, roomId, imageUrl, isPrimary, sortOrder, createdAt — tối đa **10** ảnh/room.
- **Floor**, **Property**: FK validation; scope via FR-06/07.
- **Booking** (read for delete guard): active status check only — FR-04 owns writes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Manager hoàn tất **tạo phòng mới** (SCR-30) trong **dưới 3 phút**.
- **SC-002**: **100%** xóa phòng có booking active bị từ chối (test matrix FR-04 statuses).
- **SC-003**: **100%** phòng mới tạo có status Available và xuất hiện trong Manager list trong **dưới 5 giây** sau create.
- **SC-004**: Manager lọc danh sách theo floor/status trả kết quả chính xác trong **dưới 3 giây** (≤500 phòng/property).
- **SC-005**: **100%** phòng có đúng **một** ảnh primary sau khi Manager set primary (RoomImage constraint).
- **SC-006**: Employee chỉ thấy phòng property được gán — **0** phòng lọt ngoài scope (security test).
- **SC-007**: **100%** trùng room number trong cùng property bị từ chối.

## Assumptions

- **Manual status management** (Maintenance, Out Of Service, date range, housekeeping gate) thuộc **FR-05** SCR-33 — SCR-29 kebab "Status" điều hướng FR-05, không implement PATCH status trong FR-08.
- **Amenities** lưu dạng tag list (tiện ích) trên Room — align FR-03 discovery display; optional v1 nếu SCR-30 form chưa bắt buộc tất cả amenities.
- **PricingRule** entity (weekend surcharge) — v1 dùng `pricePerNight` trên Room; PricingRule table deferred unless SCR-30 bắt buộc.
- **Upload** ảnh: local `/uploads/rooms/{roomId}/`; validate MIME jpeg/png/webp; max **10MB**/file; max **10** ảnh/room.
- **Admin** không CRUD room trong FR-08 (Manager write only); Admin có thể read qua báo cáo downstream.
- **Soft-delete** room khi không còn booking blocking: set `deletedAt`, ẩn khỏi Manager list / Guest discovery; **không** hard-delete row (giữ FK booking). RoomImage ẩn theo room deleted.
- Đổi `roomNumber` khi có booking history/active — **cho phép** nếu unique trong property (booking theo `roomId`).
- Đổi floor cùng property kể cả Occupied — **cho phép** (calendar/booking theo roomId).
- api-spec field `name` = entity `roomNumber` — map trong plan.
