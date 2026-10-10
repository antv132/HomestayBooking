# Feature Specification: FR-03 Room Discovery

**Feature Branch**: `004-room-discovery`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-03 Room Discovery — dựa vào docs (Specification_v2.md §3 FR-03, §2 Guest/Customer, §5 Property/Room/RoomImage, §10 Room Management acceptance, api-spec-by-screen SCR-01/07/08/09, screen.md, screendesign.md)"

**Phụ thuộc**: Dữ liệu Property/Room phải tồn tại (FR-06/FR-08 hoặc seed); FR-01 không bắt buộc để **xem** phòng (Guest); đặt phòng thuộc FR-04.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tìm kiếm và liệt kê phòng (Priority: P1)

Là **Guest hoặc Customer**, tôi muốn xem danh sách phòng và lọc theo property, địa điểm, loại phòng, khoảng giá, ngày check-in/check-out và số khách để tìm phòng phù hợp nhu cầu lưu trú.

**Why this priority**: FR-03 bullet cốt lõi — danh sách phòng + tìm kiếm đa tiêu chí; SCR-07.

**Independent Test**: Mở SCR-07 → áp dụng bộ lọc → danh sách chỉ hiển thị phòng khớp tiêu chí; phân trang hoạt động; trạng thái trống khi không có kết quả.

**Acceptance Scenarios**:

1. **Given** có phòng ACTIVE trong hệ thống, **When** Guest mở trang danh sách phòng không filter, **Then** hiển thị phòng thuộc property ACTIVE với ảnh, giá, loại phòng, property name.
2. **Given** user chọn property + loại phòng + khoảng giá, **When** áp dụng bộ lọc, **Then** chỉ phòng khớp **tất cả** tiêu chí được hiển thị.
3. **Given** user nhập check-in, check-out và số khách, **When** tìm kiếm, **Then** loại phòng không còn trống trong khoảng ngày hoặc không đủ capacity.
4. **Given** check-out ≤ check-in, **When** submit, **Then** thông báo lỗi rõ ràng, không gọi kết quả sai.
5. **Given** không có phòng khớp, **When** xem kết quả, **Then** hiển thị empty state và gợi ý xóa/xiền chỉnh bộ lọc.

---

### User Story 2 - Xem chi tiết phòng (Priority: P1)

Là **Guest hoặc Customer**, tôi muốn xem chi tiết một phòng (gallery ảnh, mô tả, tiện ích, thông tin property, giá, capacity) và xem trước lịch trống rút gọn để quyết định có đặt phòng hay không.

**Why this priority**: FR-03 "Xem chi tiết phòng bao gồm gallery, tiện ích và lịch trống"; SCR-08.

**Independent Test**: Click phòng từ SCR-07 → SCR-08 hiển thị gallery, amenities, giá; nút "Book Now" dẫn login (Guest) hoặc checkout (Customer — FR-04).

**Acceptance Scenarios**:

1. **Given** phòng tồn tại và property ACTIVE, **When** mở chi tiết, **Then** hiển thị room number, room type, mô tả, gallery (ảnh chính + thumbnails), property name/address, giá/đêm, capacity, area.
2. **Given** phòng có tiện ích đã cấu hình, **When** xem chi tiết, **Then** danh sách tiện ích hiển thị đầy đủ.
3. **Given** phòng không tồn tại hoặc property INACTIVE, **When** truy cập URL chi tiết, **Then** thông báo không tìm thấy (404 UX).
4. **Given** Guest chưa đăng nhập, **When** bấm "Book Now", **Then** chuyển hướng đăng nhập (FR-01), giữ ngày/phòng đã chọn nếu có.

---

### User Story 3 - Gợi ý tìm kiếm thông minh (Priority: P1)

Là **Guest hoặc Customer**, tôi muốn nhận gợi ý tự động khi gõ vào thanh tìm kiếm (địa điểm, tên property) để tìm phòng nhanh hơn.

**Why this priority**: FR-03 bullet "search suggestions"; Guest permission §2.

**Independent Test**: Gõ ≥2 ký tự trên hero/search bar → dropdown gợi ý property/location → chọn gợi ý → điều hướng SCR-07 với filter tương ứng.

**Acceptance Scenarios**:

1. **Given** user gõ "Đà Nẵng", **When** hệ thống trả gợi ý, **Then** hiển thị property hoặc location khớp partial text.
2. **Given** query rỗng hoặc <2 ký tự, **When** focus search, **Then** không gọi gợi ý hoặc hiển thị gợi ý phổ biến (nếu có).
3. **Given** không có kết quả khớp, **When** gõ từ khóa lạ, **Then** dropdown "Không có gợi ý" — không crash UI.

---

### User Story 4 - Lịch trống phòng (Availability Calendar) (Priority: P2)

Là **Guest hoặc Customer**, tôi muốn xem lịch trống full-size của một phòng theo tháng để biết ngày nào đã được đặt và ngày nào còn trống.

**Why this priority**: FR-03 bullet lịch trống; SCR-09. Read-only cho khách — cập nhật trạng thái phòng thuộc FR-05.

**Independent Test**: Từ SCR-08 → "View Calendar" → SCR-09 hiển thị ngày booked vs available; chọn ngày → quay SCR-08 với dates preserved.

**Acceptance Scenarios**:

1. **Given** phòng có booking trong tháng 6/2026, **When** xem calendar tháng đó, **Then** ngày booked được đánh dấu khác ngày available.
2. **Given** user chuyển tháng/năm, **When** load calendar, **Then** cập nhật booked dates cho tháng mới.
3. **Given** user chọn khoảng ngày available trên calendar, **When** xác nhận, **Then** quay chi tiết phòng với check-in/check-out đã chọn (chuẩn bị FR-04).

---

### User Story 5 - Trang chủ khám phá (Landing) (Priority: P2)

Là **Guest**, tôi muốn xem trang chủ với phòng nổi bật, property nổi bật, banner khuyến mãi và thống kê tổng quan nền tảng để hiểu quy mô hệ thống trước khi tìm kiếm.

**Why this priority**: FR-03 "Hiển thị thống kê tổng quan về nền tảng trên trang chủ"; Guest permissions (banner, stats, featured).

**Independent Test**: Mở SCR-01 → hero search + featured rooms/properties + stats (số property, phòng, booking/reviews) + promotions.

**Acceptance Scenarios**:

1. **Given** hệ thống có phòng featured, **When** Guest mở trang chủ, **Then** hiển thị grid phòng nổi bật với ảnh, giá, property.
2. **Given** có property ACTIVE, **When** xem section properties, **Then** hiển thị tên, địa chỉ, số phòng (hoặc phòng trống).
3. **Given** admin cấu hình promotion ACTIVE, **When** Guest mở trang chủ, **Then** hiển thị banner khuyến mãi.
4. **Given** stats endpoint available, **When** load trang chủ, **Then** hiển thị tổng property, phòng, (và metric liên quan như reviews/rating nếu có).

---

### Edge Cases

- Property INACTIVE → phòng thuộc property **không** hiển thị trong discovery.
- Phòng trạng thái Maintenance / Out Of Service → loại khỏi kết quả tìm kiếm khách (chỉ phòng bookable).
- Khi có check-in/check-out: phòng đang Pending Deposit/Reserved/Occupied trong khoảng ngày → không hiển thị là available.
- Gallery rỗng → placeholder image + initials/ icon.
- Giá động (PricingRule) vs giá mặc định: hiển thị giá áp dụng cho khoảng ngày search nếu có; ngược lại giá mặc định phòng.
- Customer và Guest có cùng quyền **xem** discovery; khác biệt chỉ ở "Book Now" (Customer → FR-04).
- Pagination: page vượt totalPages → về trang cuối hoặc empty.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST cho phép Guest và Customer **xem danh sách phòng** thuộc property ACTIVE (SCR-07).
- **FR-002**: Hệ thống MUST hỗ trợ lọc/tìm theo: property, địa điểm/location, loại phòng, khoảng giá, check-in, check-out, số khách.
- **FR-003**: Hệ thống MUST loại phòng không đáp ứng capacity hoặc không available trong khoảng ngày khi user cung cấp dates.
- **FR-004**: Hệ thống MUST cung cấp **gợi ý tìm kiếm** (property name, location) khi user gõ trên thanh search.
- **FR-005**: Hệ thống MUST cho phép xem **chi tiết phòng** gồm gallery, mô tả, tiện ích, thông tin property, giá, capacity (SCR-08).
- **FR-006**: Hệ thống MUST hiển thị **lịch trống read-only** theo phòng theo tháng/năm (SCR-09).
- **FR-007**: Hệ thống MUST hiển thị trang chủ với phòng/property nổi bật, banner khuyến mãi ACTIVE, và **thống kê tổng quan nền tảng** (SCR-01).
- **FR-008**: Hệ thống MUST phân trang danh sách phòng và hỗ trợ sắp xếp (giá, mới nhất).
- **FR-009**: Hệ thống MUST ẩn phòng/property INACTIVE hoặc không bookable khỏi discovery.
- **FR-010**: Hệ thống MUST validate check-in/check-out (check-out sau check-in, không quá khứ cho check-in).

### Key Entities

- **Property** (§5): id, name, address, description, status — discovery chỉ ACTIVE.
- **Room** (§5): id, propertyId, floorId, roomNumber, roomType, area, pricePerNight, capacity, status, description — lọc theo status bookable.
- **RoomImage** (§5): gallery với IsPrimary, SortOrder.
- **Booking** (§5, read-only): dùng để tính booked dates / availability — không tạo booking trong FR-03.
- **Promotion** (landing): banner khuyến mãi ACTIVE trên SCR-01.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 95% truy vấn tìm phòng (có filter) hoàn tất hiển thị kết quả trong dưới 3 giây.
- **SC-002**: 95% user mở chi tiết phòng thấy gallery + thông tin cơ bản trong dưới 2 giây.
- **SC-003**: 90% user tìm được ít nhất 1 phòng phù hợp khi dùng ≥1 filter trên catalog có ≥10 phòng.
- **SC-004**: Gợi ý search trả kết quả trong dưới 500ms cảm nhận (debounced) với query hợp lệ.
- **SC-005**: Lịch trống phản ánh đúng 100% ngày đã booked so với booking ACTIVE trong tháng hiển thị.
- **SC-006**: 100% phòng/property INACTIVE bị loại khỏi kết quả discovery công khai.

## Assumptions

- **Out of scope FR-03**: Tạo/sửa phòng (FR-08), quản lý property (FR-06), đặt phòng/thanh toán (FR-04), Manager cập nhật trạng thái lịch (FR-05), đánh giá/review chi tiết (FR riêng).
- **Tiện ích (amenities)**: Hiển thị từ thuộc tính/danh sách tiện ích gắn với Room (cấu hình khi Manager tạo phòng FR-08); nếu chưa có entity riêng, dùng danh sách tag chuẩn (WiFi, AC, TV, …).
- **API surfaces** (theo `docs/api-spec-by-screen.md`, mở rộng từ frontend hiện có): `GET /rooms`, `GET /rooms/{id}`, `GET /rooms/{id}/availability`, `GET /properties/featured`, `GET /promotions/active`; gợi ý + stats: `/public/search-suggestions`, `/public/stats`, `/rooms/featured` — chuẩn hóa `/api/v1/*` khi implement.
- **Giá hiển thị**: Ưu tiên PricingRule nếu có cho khoảng ngày search; fallback `pricePerNight` Room.
- **Actors**: Guest + Customer cho SCR-01, 07, 08, 09; không yêu cầu Employee/Manager/Admin.
- **Room status bookable** cho discovery: Available (và có thể hiển thị trạng thái badge nhưng chỉ Available bookable khi dates clear).

## Clarifications

*(Chưa có session clarify — defaults ở Assumptions.)*
