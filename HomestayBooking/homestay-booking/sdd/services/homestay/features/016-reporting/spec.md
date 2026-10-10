# Feature Specification: FR-16 Reporting

**Feature Branch**: `018-reporting`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-16 Reporting — dựa vào docs (Specification_v2.md § FR-16, §8 Reporting acceptance, api-spec-by-screen SCR-27/44/45/55, screen.md, screendesign.md, entity-ui-mapping.md §1.17, component-library.md KpiCard/Chart, frontend reportApi.ts, RevenueReportPage.tsx, OccupancyReportPage.tsx, ReportsDashboardPage.tsx)"

**Phụ thuộc**: FR-01 (auth RBAC); FR-06 (Property + Manager assignment scope); FR-07 (Floor counts); FR-08 (Room counts/status); FR-04 (Booking lifecycle); FR-12 (Payment PAID — nguồn doanh thu); FR-09 (Customer counts — optional new customers KPI). **Ranh giới**: FR-16 owns **read-only** báo cáo & KPI aggregation; **không** sở hữu CRUD Property/Booking/Payment; không thay thế hệ thống kế toán (§9 Out of Scope); Employee/Customer **không** có quyền báo cáo v1.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manager xem KPI Dashboard theo Property được gán (Priority: P1)

Là **Manager** được gán một hoặc nhiều Property, tôi muốn xem **tổng quan KPI** (số phòng, tỷ lệ lấp đầy, doanh thu kỳ hiện tại, check-in hôm nay, chờ duyệt) cho Property đang chọn, để nắm tình hình vận hành nhanh khi đăng nhập (SCR-27).

**Why this priority**: FR-16 Manager reporting entry point; SCR-27 wireframe KPI cards + property selector.

**Independent Test**: Manager M gán Property P → chọn P trên dashboard → KPI khớp dữ liệu P; Manager M2 không thấy KPI của P.

**Acceptance Scenarios**:

1. **Given** Manager được gán Property P, **When** mở Manager Dashboard (SCR-27) và chọn P, **Then** hiển thị KPI gồm ít nhất: tổng phòng, tỷ lệ lấp đầy (%), doanh thu kỳ gần nhất, số check-in hôm nay, số mục chờ duyệt (payment/maintenance — assumption: pending approvals count).
2. **Given** Manager được gán nhiều Property, **When** đổi Property selector, **Then** KPI cập nhật theo Property mới.
3. **Given** Manager không được gán Property X, **When** cố lấy KPI của X, **Then** từ chối truy cập.
4. **Given** Property không có booking trong kỳ, **When** xem dashboard, **Then** doanh thu = 0 và occupancy hiển thị hợp lý (0% hoặc N/A với empty state).
5. **Given** Employee hoặc Customer, **When** cố truy cập Manager Dashboard KPI, **Then** từ chối.

---

### User Story 2 - Manager xem báo cáo chi tiết Property (Revenue, Occupancy, Booking Trend) (Priority: P1)

Là **Manager**, tôi muốn xem **báo cáo chi tiết** theo Property được gán với các tab **Doanh thu**, **Tỷ lệ lấp đầy**, và **Xu hướng đặt phòng**, lọc theo **khoảng ngày** và **tháng/năm**, để phân tích hiệu suất kinh doanh (SCR-44).

**Why this priority**: FR-16 core bullets — revenue by property/month/year, occupancy, booking & revenue trends.

**Independent Test**: Manager chọn P, date range 3 tháng → Revenue tab tổng khớp payments PAID; Occupancy tab % hợp lý; Booking Trend chart có điểm theo kỳ.

**Acceptance Scenarios**:

1. **Given** Manager được gán Property P, **When** mở Property Reports tab **Revenue** (SCR-44) với `from`/`to`, **Then** hiển thị tổng doanh thu, breakdown đặt cọc/phần còn lại (assumption: deposit vs balance), doanh thu theo kỳ (tháng/tuần), và bảng tóm tắt.
2. **Given** cùng Property P, **When** chọn tab **Occupancy**, **Then** hiển thị **Occupancy Rate** (%) theo kỳ và tóm tắt phòng occupied/available trong phạm vi Property.
3. **Given** cùng Property P, **When** chọn tab **Booking Trends**, **Then** hiển thị **số lượng booking** theo kỳ (line/bar chart) — đếm booking ở trạng thái counted (assumption: Confirmed, Checked-in, Checked-out; loại Cancelled/No-show).
4. **Given** Manager chọn `groupBy=month` hoặc `week`, **When** áp dụng, **Then** biểu đồ và bảng nhóm theo kỳ tương ứng.
5. **Given** Manager cố xem báo cáo Property ngoài scope, **When** gọi báo cáo, **Then** từ chối.
6. **Given** khoảng ngày không hợp lệ (`to` trước `from`), **When** submit filter, **Then** thông báo lỗi validation.

---

### User Story 3 - Admin xem KPI Dashboard toàn hệ thống (Priority: P1)

Là **Admin**, tôi muốn xem **KPI toàn hệ thống** gồm tổng doanh thu, số property, phòng, booking, và khách mới, để giám sát sức khỏe nền tảng (SCR-45).

**Why this priority**: FR-16 Admin stats — total property, floor, room, booking; §8 acceptance Admin Dashboard KPIs.

**Independent Test**: Admin login → global KPIs khớp aggregate DB; Manager không truy cập được.

**Acceptance Scenarios**:

1. **Given** Admin, **When** mở Admin Dashboard (SCR-45), **Then** hiển thị KPI: **tổng doanh thu** (toàn hệ thống), **tổng Property**, **tổng Floor**, **tổng Room**, **phòng Available**, **phòng Occupied**, **tổng Booking** (assumption: active + completed counts).
2. **Given** hệ thống có nhiều Property, **When** xem KPI, **Then** số liệu là **tổng hợp toàn hệ thống**, không lọc theo Manager.
3. **Given** Manager hoặc Customer, **When** cố truy cập Admin Dashboard KPI, **Then** từ chối.
4. **Given** không có dữ liệu (hệ thống mới), **When** xem dashboard, **Then** KPI = 0 với empty state thân thiện.

---

### User Story 4 - Admin xem báo cáo toàn hệ thống có lọc Property (Priority: P1)

Là **Admin**, tôi muốn xem **báo cáo doanh thu và xu hướng** toàn hệ thống theo **năm/tháng**, có thể **lọc theo Property**, để so sánh hiệu suất giữa các chi nhánh (SCR-55).

**Why this priority**: FR-16 Admin revenue by property, month, year; SCR-55 mirrors SCR-44 with property filter.

**Independent Test**: Admin chọn year 2026 → monthly revenue chart; filter Property P → chỉ dữ liệu P; không filter → all properties.

**Acceptance Scenarios**:

1. **Given** Admin, **When** mở Global Reports (SCR-55) tab Revenue với `year`, **Then** hiển thị **doanh thu theo tháng** (`monthlyData`) và tổng năm.
2. **Given** Admin, **When** thêm filter `propertyId`, **Then** báo cáo chỉ tính booking/payment thuộc Property đó.
3. **Given** Admin, **When** xem tab Occupancy / Booking Trends (assumption: SCR-55 same tabs as SCR-44), **Then** metrics toàn hệ thống hoặc theo Property filter tương tự Manager scope nhưng Admin có quyền mọi Property.
4. **Given** Property không tồn tại, **When** filter, **Then** trả empty hoặc 404 hợp lý.
5. **Given** Manager, **When** cố gọi Admin global report APIs, **Then** từ chối.

---

### User Story 5 - Xuất báo cáo để chia sẻ nội bộ (Priority: P2)

Là **Manager** hoặc **Admin**, tôi muốn **xuất** dữ liệu báo cáo doanh thu ra file (CSV) sau khi lọc, để chia sẻ với đối tác hoặc lưu trữ nội bộ.

**Why this priority**: Frontend `RevenueReportPage` đã có export UX; tăng giá trị vận hành nhưng không chặn MVP read-only.

**Independent Test**: Manager export revenue CSV sau filter → file chứa các cột period, revenue, bookingCount khớp màn hình.

**Acceptance Scenarios**:

1. **Given** báo cáo doanh thu đang hiển thị, **When** chọn Export CSV, **Then** tải file với header và dữ liệu khớp bảng `byPeriod`.
2. **Given** không có dữ liệu trong kỳ, **When** export, **Then** file chỉ có header hoặc thông báo không có dữ liệu.
3. **Given** Customer, **When** cố export, **Then** từ chối.

---

### Edge Cases

- Khoảng ngày rất dài (1+ năm) — giới hạn tối đa (assumption: 366 ngày) hoặc cảnh báo hiệu năng.
- Payment PAID sau khi booking Cancelled — revenue chỉ tính payment PAID hợp lệ (assumption: exclude refunded/cancelled booking payments).
- Property INACTIVE — vẫn tính trong báo cáo lịch sử; không hiển thị trong discovery (FR-06).
- Múi giờ — aggregation theo **Asia/Ho_Chi_Minh** (assumption, đồng bộ booking).
- Manager mới được gán Property — chỉ thấy dữ liệu từ thời điểm có assignment (assumption v1: full historical data of property, không filter theo assignment date).
- Concurrent bookings overlapping — occupancy tính theo **room-nights** occupied vs available.
- Zero rooms in property — occupancy 0%; tránh chia cho 0.
- PDF/Excel export nâng cao — **out of scope** v1 (CSV client-side đủ P2).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Manager MUST xem KPI Dashboard (SCR-27) **chỉ** cho Property được gán (FR-06 scope).
- **FR-002**: Manager MUST xem báo cáo **doanh thu** theo Property, **tháng/năm** (và khoảng ngày tùy chọn) trên SCR-44.
- **FR-003**: Manager MUST xem báo cáo **Occupancy Rate** theo Property được gán trên SCR-44.
- **FR-004**: Manager MUST xem **Booking Trend** (số booking theo kỳ) theo Property được gán.
- **FR-005**: Admin MUST xem KPI toàn hệ thống: **tổng Property, Floor, Room, Booking** và phòng Available/Occupied (SCR-45).
- **FR-006**: Admin MUST xem **doanh thu toàn hệ thống** theo Property, tháng, năm (SCR-55).
- **FR-007**: Admin MUST có thể **lọc** báo cáo global theo Property (SCR-55).
- **FR-008**: Doanh thu MUST tính từ **thanh toán đã hoàn tất (PAID)** gắn booking (FR-12), gồm deposit và remaining balance.
- **FR-009**: Occupancy Rate MUST tính theo công thức **occupied room-nights / available room-nights × 100** trong kỳ đã chọn.
- **FR-010**: Booking Trend MUST đếm booking theo trạng thái counted (Confirmed, Checked-in, Checked-out) theo kỳ.
- **FR-011**: FR-16 MUST **từ chối** truy cập báo cáo ngoài RBAC (Manager cross-property, Customer, Employee, Guest).
- **FR-012**: Báo cáo MUST hỗ trợ **Revenue Trend** (doanh thu theo kỳ) — đồng bộ với tab Revenue / chart trên SCR-44 và SCR-55.
- **FR-013**: FR-16 MUST **read-only** — không thay đổi dữ liệu nguồn khi xem báo cáo.
- **FR-014**: Manager Property selector MUST chỉ liệt kê Property được gán.
- **FR-015**: Admin global reports MUST aggregate chính xác khi **không** filter property (toàn hệ thống).
- **FR-016**: Manager/Admin MAY xuất CSV doanh thu từ dữ liệu đã tải (P2 — client-side).

### Key Entities *(read aggregates — không tạo bảng báo cáo riêng v1)*

- **ReportPeriod**: kỳ báo cáo (ngày/tuần/tháng/năm) — derived, không persist.
- **RevenueSummary**: tổng doanh thu, deposit/balance split, byPeriod[], byProperty[].
- **OccupancySummary**: occupancyRate %, occupiedRooms, totalRooms, byPeriod[].
- **BookingTrendSummary**: bookingCount by period.
- **PropertyKpis**: occupancyRate, revenue, pendingCheckIns, pendingApprovals (Manager dashboard).
- **GlobalKpis**: totalRevenue, totalProperties, totalFloors, totalRooms, availableRooms, occupiedRooms, totalBookings, newCustomers (optional).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Manager xem KPI Dashboard cho Property được gán trong **dưới 3 giây** (95% requests).
- **SC-002**: **100%** báo cáo Manager bị chặn khi truy cập Property ngoài scope.
- **SC-003**: Doanh thu báo cáo **khớp 100%** với tổng payment PAID trong cùng kỳ (validation test với seed data).
- **SC-004**: Occupancy Rate sai số **≤ 1%** so với tính tay room-nights trên dataset mẫu.
- **SC-005**: Admin Global KPIs phản ánh đúng count Property/Floor/Room/Booking so với DB.
- **SC-006**: **90%** Manager hoàn tất xem báo cáo Revenue + Occupancy + Booking Trend trong **dưới 2 phút** (filter + đọc chart).
- **SC-007**: Báo cáo theo tháng/năm trả kết quả trong **dưới 5 giây** cho range ≤ 12 tháng.
- **SC-008**: CSV export (P2) khớp dữ liệu hiển thị trên màn hình **100%** các dòng.

## Assumptions

- FR-16 **không** tạo bảng `reports` riêng v1 — query aggregate trực tiếp từ bookings, payments, rooms, properties, floors.
- Revenue = sum `payments.amount` WHERE `status = PAID` trong kỳ, gắn booking thuộc property scope.
- Deposit vs balance split theo `payment.type` (DEPOSIT / REMAINING_BALANCE) từ FR-12.
- Manager routes frontend: `/manager/reports/*` maps SCR-44 tabs; SCR-27 Manager Dashboard là entry KPI riêng.
- Admin routes: `/admin/dashboard` (SCR-45), `/admin/reports` (SCR-55) — có thể chưa có route; tạo trong implement phase.
- `groupBy`: `month` (default) | `week` per frontend `reportApi.ts`.
- Booking counted statuses: CONFIRMED, CHECKED_IN, CHECKED_OUT (exclude PENDING_DEPOSIT, CANCELLED, NO_SHOW).
- New Customers KPI on Admin dashboard: count registrations trong 30 ngày (assumption P2 nice-to-have on SCR-45).
- Caching report queries optional P2 — không bắt buộc v1.
- Hệ thống kế toán, PDF báo cáo chính thức, đa tiền tệ — **out of scope** (§9).
