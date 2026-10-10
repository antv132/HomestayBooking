# Feature Specification: FR-19 Customer Dashboard

**Feature Branch**: `021-customer-dashboard`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-19 Customer Dashboard — dựa vào docs (Specification_v2.md § FR-19, SCR-15 screen.md/api-spec, screendesign.md, figma SCR-16 Customer Dashboard KPIs, entity-ui-mapping.md, frontend CustomerDashboardPage.tsx, customersApi.ts fetchCustomerDashboard)"

**Phụ thuộc**: FR-01 (auth CUSTOMER); FR-04 (Booking — active/upcoming); FR-12/13 (Payment pending); FR-13 (MaintenanceTicket open count); FR-15 (Notification unread + recent); FR-02 (Customer greeting name). **Ranh giới**: FR-19 owns **SCR-15 Customer Dashboard** composite read + UI wire; **không** CRUD booking/payment/ticket trên dashboard (deep links tới SCR-17/18, payments, maintenance); Damage **Dispute** full flow thuộc **FR-23** — FR-19 MAY hiển thị **cảnh báo** pending dispute P2.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customer xem tổng quan KPI (Priority: P1)

Là **Customer** đã đăng nhập, tôi muốn mở **Dashboard** và thấy ngay các chỉ số quan trọng (booking đang active, thanh toán chờ, ticket bảo trì mở, thông báo chưa đọc), để nắm tình trạng tài khoản mà không phải vào từng màn hình (SCR-15).

**Why this priority**: FR-19 core "Dashboard tổng quan"; KPI row là giá trị chính.

**Independent Test**: Customer login → Dashboard hiển thị 4 KPI khớp dữ liệu thực; Customer khác không thấy KPI của người khác.

**Acceptance Scenarios**:

1. **Given** Customer có 2 booking status **Confirmed** hoặc **Checked-in**, **When** mở Dashboard, **Then** KPI **Active bookings** = 2.
2. **Given** Customer có 1 Payment **Pending**, **When** mở Dashboard, **Then** KPI **Pending payments** = 1 và được nhấn mạnh (assumption: màu cảnh báo khi > 0).
3. **Given** Customer có Maintenance ticket **Open** hoặc **In progress**, **When** mở Dashboard, **Then** **Open tickets** = số ticket đang mở.
4. **Given** Customer có 3 Notification chưa đọc, **When** mở Dashboard, **Then** **Unread notifications** = 3.
5. **Given** Customer mới không có dữ liệu, **When** mở Dashboard, **Then** KPI hiển thị **0** hoặc "—"; không lỗi trang.
6. **Given** Manager hoặc Guest, **When** cố truy cập Customer Dashboard, **Then** từ chối hoặc redirect (FR-01).

---

### User Story 2 - Customer xem sự kiện Check-in/Check-out sắp tới (Priority: P1)

Là **Customer**, tôi muốn thấy **check-in** và **check-out** gần nhất sắp diễn ra (phòng, property, ngày, còn bao nhiêu ngày), để chuẩn bị lưu trú (SCR-15).

**Why this priority**: FR-19 explicit "Hiển thị các sự kiện Check-in/Check-out sắp diễn ra".

**Independent Test**: Customer có booking Confirmed với check-in trong 3 ngày → Dashboard hiển thị event card đúng; click → booking detail.

**Acceptance Scenarios**:

1. **Given** booking **Confirmed** với `checkIn` gần nhất trong tương lai, **When** mở Dashboard, **Then** card **Upcoming check-in** hiển thị room, property, ngày, countdown (e.g. "Còn 2 ngày").
2. **Given** booking **Checked-in**, **When** mở Dashboard, **Then** card **Upcoming check-out** hiển thị `checkOut` gần nhất.
3. **Given** không có booking phù hợp, **When** mở Dashboard, **Then** empty state "Không có lịch sắp tới" trên card tương ứng.
4. **Given** Customer click event card, **When** tương tác, **Then** điều hướng tới **Booking detail** (SCR-18) của booking đó.
5. **Given** check-in trong **≤ 3 ngày**, **When** hiển thị, **Then** có visual cảnh báo (assumption: badge warning per figma).

---

### User Story 3 - Customer xem danh sách booking sắp tới (Priority: P1)

Là **Customer**, tôi muốn thấy **danh sách booking sắp tới** (status Confirmed, Checked-in, Pending deposit) với link tới chi tiết và thanh toán cọc nếu cần, để hành động nhanh (SCR-15).

**Why this priority**: screendesign "Upcoming Stay"; frontend đã có section "Booking sắp tới".

**Independent Test**: Dashboard list shows upcoming bookings sorted by check-in; PENDING_DEPOSIT shows pay CTA.

**Acceptance Scenarios**:

1. **Given** Customer có nhiều booking upcoming, **When** mở Dashboard, **Then** thấy danh sách (assumption: tối đa 5, sắp theo `checkIn` gần nhất).
2. **Given** booking **Pending deposit**, **When** hiển thị trong list, **Then** có action **Thanh toán cọc** link tới payment flow.
3. **Given** không có booking upcoming, **When** mở Dashboard, **Then** empty state + CTA **Tìm phòng**.
4. **Given** Customer click **Xem tất cả**, **When** navigate, **Then** tới **My Bookings** (SCR-17).

---

### User Story 4 - Customer xem thông báo mới nhất (Priority: P1)

Là **Customer**, tôi muốn thấy **thông báo gần đây** trên Dashboard với trạng thái đã đọc/chưa đọc, để không bỏ lỡ cập nhật quan trọng (SCR-15 sidebar).

**Why this priority**: FR-19 "thông báo chưa đọc" + danh sách mới nhất.

**Independent Test**: Dashboard shows last N notifications; unread styled distinctly; link to notification detail/list.

**Acceptance Scenarios**:

1. **Given** Customer có notifications, **When** mở Dashboard, **Then** thấy tối thiểu **5** thông báo mới nhất (assumption) với title, preview, thời gian tương đối.
2. **Given** notification **chưa đọc**, **When** hiển thị, **Then** visual khác biệt (assumption: highlight/border).
3. **Given** Customer click notification, **When** navigate, **Then** tới chi tiết hoặc danh sách thông báo (FR-15).
4. **Given** không có notification, **When** mở Dashboard, **Then** empty state thân thiện.
5. **Given** Customer click **Tất cả**, **When** navigate, **Then** tới trang Notifications đầy đủ.

---

### User Story 5 - Customer xem thanh toán gần đây (Priority: P1)

Là **Customer**, tôi muốn thấy **danh sách thanh toán mới nhất** (loại, số tiền, trạng thái, ngày) trên Dashboard, để theo dõi giao dịch gần đây (FR-19 + figma SCR-16).

**Why this priority**: FR-19 explicit "Danh sách thanh toán ... mới nhất".

**Independent Test**: Dashboard shows last 3 payments for customer; link to payment history.

**Acceptance Scenarios**:

1. **Given** Customer có payments, **When** mở Dashboard, **Then** thấy **3** thanh toán mới nhất với type, amount, status, ngày.
2. **Given** không có payment, **When** mở Dashboard, **Then** empty state hoặc ẩn section (assumption: empty message).
3. **Given** Customer click **Xem tất cả**, **When** navigate, **Then** tới **Payment History** (SCR-23).
4. **Given** payment **Pending**, **When** hiển thị, **Then** status badge cảnh báo.

---

### User Story 6 - Customer thao tác nhanh và cảnh báo Damage Dispute (Priority: P2)

Là **Customer**, tôi muốn **quick links** (tìm phòng, booking, báo sự cố) và **cảnh báo** khi có khoản Damage Fee đang chờ phản đối (Dispute), để hành động nhanh từ Dashboard (SCR-15 + FR-23 integration).

**Why this priority**: UI đã có quick actions; Damage Dispute là cross-feature P2.

**Independent Test**: Quick links navigate correctly; if pending damage dispute exists, alert banner with Dispute CTA (FR-23).

**Acceptance Scenarios**:

1. **Given** Customer trên Dashboard, **When** xem sidebar quick actions, **Then** thấy links: Tìm phòng, Booking của tôi, Báo cáo sự cố.
2. **Given** Customer có Damage Report chờ **Dispute** trong 24h (FR-23), **When** mở Dashboard, **Then** hiển thị **alert banner** với action **Dispute** (assumption P2 — blocked until FR-23).
3. **Given** không có pending dispute, **When** mở Dashboard, **Then** không hiển thị banner dispute.

---

### Edge Cases

- Customer Suspended (FR-09) — chặn login trước Dashboard (FR-01).
- Booking Cancelled/Checked-out — không tính vào active/upcoming lists.
- Timezone check-in/out — assumption **Asia/Ho_Chi_Minh** cho "hôm nay"/countdown.
- Dashboard load partial failure — hiển thị lỗi thân thiện + retry (assumption).
- KPI `openTickets` — đếm status Open + In progress (assumption align figma).
- `activeBookings` — status **Confirmed** + **Checked-in** only (per figma KPI definition).
- Recent lists scoped **chỉ Customer hiện tại** — không leak data.
- Screen numbering: **SCR-15** (screen.md, api-spec); figma labels SCR-16 — same feature.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Customer MUST xem Dashboard tổng quan sau đăng nhập (SCR-15, route `/customer/dashboard`).
- **FR-002**: Dashboard MUST hiển thị KPI: **active bookings**, **pending payments**, **open maintenance tickets**, **unread notifications**.
- **FR-003**: Dashboard MUST hiển thị **upcoming check-in** và **upcoming check-out** event (nearest future per type).
- **FR-004**: Dashboard MUST hiển thị danh sách **booking sắp tới** với link chi tiết và pay deposit khi Pending deposit.
- **FR-005**: Dashboard MUST hiển thị **thông báo mới nhất** (read/unread distinction).
- **FR-006**: Dashboard MUST hiển thị **thanh toán gần đây** (tối thiểu 3 mục).
- **FR-007**: Tất cả dữ liệu Dashboard MUST **scoped** theo Customer đang đăng nhập.
- **FR-008**: FR-19 MUST **từ chối** truy cập Dashboard cho non-Customer roles (redirect).
- **FR-009**: Dashboard MUST cung cấp quick links tới discovery, My Bookings, tạo maintenance ticket.
- **FR-010**: Dashboard MUST là **read-only aggregate** — không tạo/sửa entity trên màn hình này.
- **FR-011**: Optional P2: hiển thị **Damage Dispute** alert khi FR-23 có pending dispute — link tới dispute flow.
- **FR-012**: Empty states MUST thân thiện với CTA phù hợp (e.g. Tìm phòng).

### Key Entities (read aggregates)

- **Booking** (FR-04): active count, upcoming list, check-in/out events.
- **Payment** (FR-12): pending count, recent list.
- **MaintenanceTicket** (FR-13): open count.
- **Notification** (FR-15): unread count, recent list.
- **DamageReport** (FR-23, P2): pending dispute flag for alert only.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Customer thấy KPI Dashboard trong **dưới 3 giây** sau đăng nhập (95% sessions).
- **SC-002**: **100%** KPI counts khớp với dữ liệu nguồn (booking/payment/ticket/notification) cho Customer đó.
- **SC-003**: Customer mở booking detail từ event card trong **1 click** từ Dashboard.
- **SC-004**: **100%** truy cập Dashboard bởi non-Customer bị chặn/redirect.
- **SC-005**: Dashboard load thành công với zero bookings — empty states, không crash (SC-007 figma alignment).
- **SC-006**: **95%** Customer tìm thấy thông tin check-in sắp tới trong **dưới 10 giây** trên Dashboard.
- **SC-007**: Recent notifications và payments hiển thị trong **dưới 3 giây** cùng payload Dashboard (p95).

## Assumptions

- **Screen ID**: SCR-15 per `screen.md` và `api-spec-by-screen.md`; figma SCR-16 = cùng màn hình.
- **API**: composite `GET /api/v1/customer/dashboard` — migrate từ `/api/customers/dashboard`; align `CustomerDashboardData` shape trong `customersApi.ts`.
- **Active bookings**: status ∈ {Confirmed, Checked-in}.
- **Upcoming bookings**: status ∈ {Pending deposit, Confirmed, Checked-in} với `checkOut` ≥ today; max 5 items.
- **Open tickets**: Maintenance status Open + In progress.
- **Pending payments**: Payment status Pending verification hoặc Pending (align FR-12).
- **Recent notifications**: 5 items, newest first; **recent payments**: 3 items.
- **Damage Dispute banner**: P2 optional — depends FR-23; not blocking FR-19 MVP.
- **Greeting**: dùng `fullName` từ FR-01/02 auth store.
- FR-19 **does not** implement Manager/Admin/Employee dashboards (SCR-27/45/59).
