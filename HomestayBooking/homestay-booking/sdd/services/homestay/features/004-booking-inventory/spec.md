# Feature Specification: FR-04 Booking & Inventory Management

**Feature Branch**: `006-booking-inventory`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-04 Booking & Inventory Management — dựa vào docs (Specification_v2.md § FR-04, §5 Booking entity, §2 Customer/Manager permissions, api-spec-by-screen SCR-15–20/34–35, screen.md, screendesign.md, entity-ui-mapping.md)"

**Phụ thuộc**: FR-01 (Customer đăng nhập để đặt phòng); FR-03 (chọn phòng và ngày từ discovery); dữ liệu Room/Property phải tồn tại. **Ranh giới**: tạo/gửi hợp đồng PDF chi tiết (FR-10); đối soát VNPay định kỳ và lịch sử payment đầy đủ (FR-12); quy trình kiểm tra phòng & damage dispute chi tiết (FR-23); thực thi housekeeping (FR-21); dashboard Customer tổng quan (SCR-15) chỉ đọc booking — thuộc phạm vi hiển thị FR-04 khi liên quan booking.

## Clarifications

### Session 2026-07-23

- Q: Inventory date interval — half-open vs inclusive vs split calendar/write? → A: Half-open `[checkIn, checkOut)` (align with 005 calendar; same-day turnover allowed).
- Q: Chặn đặt khi Maintenance/OOS theo ngày — enforce RoomStatusBlock ở đâu? → A: Enforce active `RoomStatusBlock` overlap on create + Manager modify (409/conflict).
- Q: Damage fee vs “số tiền còn lại” (FR-013)? → A: Keep separate `damageFeeAmount` + payment `DAMAGE_FEE`; `remainingAmount` = 60% stay only; UI/API show breakdown; checkout blocked until damage PAID.
- Q: Biên hủy đúng 7 ngày trước check-in — 100% hay 50%? → A: Day 7 = 100%; 50% when `3 ≤ calendar days < 7`; `<3` = 0%.
- Q: Mốc giờ No-show (FR-008) khi chỉ có checkInDate? → A: Deadline = `checkInDate.atStartOfDay()` + 24h (config `no-show-after-hours`).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customer đặt phòng và thanh toán cọc (Priority: P1)

Là **Customer**, tôi muốn gửi yêu cầu đặt phòng với ngày check-in/check-out, số khách, ghi chú đặc biệt và thanh toán **40% tổng giá trị** làm cọc qua **VNPay**, để giữ phòng trong thời gian chờ xác nhận.

**Why this priority**: Luồng cốt lõi tạo doanh thu và khóa tồn kho; SCR-16.

**Independent Test**: Customer chọn phòng + ngày (từ FR-03) → điền form checkout → xác nhận → booking ở trạng thái chờ cọc; tổng tiền và tiền cọc hiển thị đúng; phòng không thể bị đặt trùng bởi khách khác trong khoảng ngày đã chọn.

**Acceptance Scenarios**:

1. **Given** phòng còn trống trong khoảng ngày và Customer đã đăng nhập, **When** submit form đặt phòng hợp lệ, **Then** tạo booking trạng thái **Pending Deposit**, snapshot **TotalAmount** (không đổi khi Manager sửa giá phòng sau này), **DepositAmount = 40%**, **RemainingAmount = 60%**, và giữ phòng trong khoảng ngày.
2. **Given** booking Pending Deposit và Customer chọn thanh toán trực tuyến, **When** thanh toán cọc thành công, **Then** booking chuyển **Confirmed**, hệ thống tự động tạo và gửi **Accommodation Contract** qua email cho Customer.
3. **Given** hai Customer cùng đặt một phòng trùng khoảng ngày gần như đồng thời, **When** hệ thống xử lý, **Then** chỉ **một** booking được chấp nhận; booking còn lại nhận thông báo phòng không còn khả dụng.
4. **Given** check-out ≤ check-in hoặc guestCount vượt capacity phòng, **When** submit, **Then** từ chối với thông báo lỗi rõ ràng, không tạo booking.
5. **Given** phòng không còn available trong khoảng ngày (đã booked, hoặc **RoomStatusBlock** active MAINTENANCE/OUT_OF_SERVICE chồng `[checkIn, checkOut)`, hoặc trạng thái phòng toàn cục chặn đặt), **When** Customer submit, **Then** từ chối đặt phòng.

---

### User Story 2 - Customer xem và theo dõi booking (Priority: P1)

Là **Customer**, tôi muốn xem danh sách booking của mình (theo tab trạng thái) và chi tiết từng booking (ngày, giá, số tiền đã trả, còn lại, hành động theo trạng thái) để quản lý chuyến đi.

**Why this priority**: Customer cần theo dõi sau khi đặt; SCR-17, SCR-18; dashboard SCR-15 hiển thị upcoming stay.

**Independent Test**: Customer mở "My Bookings" → thấy booking của chính mình; click chi tiết → thấy breakdown giá và action phù hợp trạng thái (thanh toán phần còn lại, hủy, v.v.).

**Acceptance Scenarios**:

1. **Given** Customer có nhiều booking, **When** mở danh sách booking, **Then** chỉ hiển thị booking thuộc tài khoản đó, có thể lọc theo trạng thái (All, Upcoming, Completed, Cancelled).
2. **Given** booking Confirmed sắp tới, **When** xem chi tiết, **Then** hiển thị TotalAmount, paidAmount, remainingBalance, ngày check-in/check-out, phòng, trạng thái.
3. **Given** booking Pending Deposit chưa thanh toán cọc, **When** xem chi tiết, **Then** hiển thị hành động hoàn tất thanh toán cọc qua **VNPay**.
4. **Given** Customer cố truy cập booking của người khác, **When** mở chi tiết theo ID, **Then** từ chối truy cập (chỉ dữ liệu của chính mình).

---

### User Story 3 - Manager check-in và check-out (Priority: P1)

Là **Manager** của property, tôi muốn xem danh sách booking thuộc property được gán, xem chi tiết, thực hiện check-in khi khách đến và check-out khi khách rời, để phản ánh trạng thái thực tế lưu trú.

**Why this priority**: Vận hành tại chỗ; SCR-34, SCR-35.

**Independent Test**: Manager lọc booking Confirmed → check-in vào ngày đến → trạng thái Checked-in; sau khi kiểm tra phòng hoàn tất → check-out → Checked-out và phòng chuyển chờ dọn.

**Acceptance Scenarios**:

1. **Given** Manager được gán property P, **When** xem danh sách booking, **Then** chỉ thấy booking thuộc phòng của property P; có lọc theo trạng thái và ngày.
2. **Given** booking Confirmed đến ngày check-in, **When** Manager thực hiện check-in, **Then** booking chuyển **Checked-in**; nếu chính sách yêu cầu thu phần còn lại tại check-in, hệ thống xác nhận remaining balance đã thanh toán trước khi cho phép check-in.
3. **Given** booking Checked-in và **Room Inspection** đã hoàn tất (không còn hư hại chưa xử lý), **When** Manager check-out, **Then** booking chuyển **Checked-out**, phòng chuyển **Pending Cleaning**, hệ thống tạo **HousekeepingTask** tự động.
4. **Given** Room Inspection chưa hoàn tất, **When** Manager cố check-out, **Then** chặn check-out với thông báo cần hoàn thành kiểm tra phòng trước.
5. **Given** Manager không được gán property của booking, **When** truy cập chi tiết, **Then** từ chối truy cập.

---

### User Story 4 - Customer hủy booking theo chính sách hoàn tiền (Priority: P2)

Là **Customer**, tôi muốn hủy booking trước ngày check-in và thấy rõ số tiền hoàn lại theo chính sách, để quyết định có hủy hay không.

**Why this priority**: FR-04 chính sách hủy linh hoạt; SCR-19.

**Independent Test**: Mở màn hình hủy → hệ thống hiển thị % hoàn theo số ngày còn lại → xác nhận → booking Cancelled và phòng được nhả.

**Acceptance Scenarios**:

1. **Given** booking Confirmed còn **≥7 ngày** trước check-in, **When** Customer xác nhận hủy, **Then** hoàn **100%** tiền cọc đã trả (theo chính sách), booking **Cancelled**, phòng available lại.
2. **Given** còn **3 đến dưới 7 ngày** trước check-in (`3 ≤ days < 7`), **When** hủy, **Then** hoàn **50%** tiền cọc.
3. **Given** còn **<3 ngày** trước check-in, **When** hủy, **Then** **không hoàn** tiền cọc.
4. **Given** booking đã **Checked-in** hoặc sau check-in, **When** Customer cố hủy, **Then** không cho phép hủy.
5. **Given** booking Pending Deposit chưa thanh toán, **When** hủy hoặc hết thời gian giữ chỗ, **Then** Cancelled, không có khoản hoàn (chưa thu tiền).

---

### User Story 5 - Cọc chuyển khoản và xác minh Manager (Priority: P2)

> **EDU SCOPE CUT (2026-07-23 — Spec Sync / TD-011):** **Cắt** chuyển khoản + upload biên lai (SCR-20) + Manager verify CK (SCR-37) khỏi phạm vi khóa học.  
> **Phương thức thanh toán duy nhất in-scope:** **VNPay** (deposit + remaining). Chi tiết → `sdd/sprints/tech-debt.md` **TD-011**.

Là **Customer**, tôi muốn chọn thanh toán cọc bằng chuyển khoản, upload biên lai, và được thông báo khi Manager xác nhận — thay vì thanh toán trực tuyến tức thì.

**Why this priority**: ~~FR-04 hỗ trợ hai phương thức cọc~~ — **deferred edu**.

**Independent Test**: ~~Transfer → receipt → Manager approve~~ — **N/A edu**.

**Acceptance Scenarios**: *(all **DEFERRED TD-011**)*

1. ~~Customer chọn chuyển khoản → Pending Deposit chờ xác minh + hướng dẫn upload biên lai.~~
2. ~~Upload biên lai → chờ Manager duyệt.~~
3. ~~Manager approve → Confirmed + contract; reject với lý do.~~
4. ~~Quá hold timeout chưa có cọc xác nhận → Cancelled (vẫn covered bởi US7 qua VNPay unpaid).~~

---

### User Story 6 - Manager sửa booking Confirmed (Priority: P2)

Là **Manager**, tôi muốn chỉnh sửa booking đang **Confirmed** (đổi ngày hoặc phòng) khi khách yêu cầu, với tính toán chênh lệch giá rõ ràng.

**Why this priority**: FR-04 "Chỉnh sửa Booking: đổi ngày/phòng cho Confirmed".

**Independent Test**: Manager mở booking Confirmed → đổi ngày/phòng → hệ thống tính chênh lệch → cập nhật snapshot giá và inventory lock mới.

**Acceptance Scenarios**:

1. **Given** booking Confirmed, **When** Manager đổi check-in/check-out sang khoảng ngày phòng còn trống, **Then** cập nhật booking, tính lại TotalAmount snapshot và chênh lệch cần thu/hoàn phần còn lại hoặc cọc nếu có.
2. **Given** booking Confirmed, **When** Manager đổi sang phòng khác cùng property còn trống, **Then** nhả lock phòng cũ, khóa phòng mới, cập nhật giá snapshot.
3. **Given** phòng/ngày mới không available (overlap booking khác hoặc **RoomStatusBlock** active chồng khoảng mới), **When** Manager submit sửa, **Then** từ chối với thông báo conflict.
4. **Given** booking đã Checked-in hoặc Cancelled, **When** cố sửa, **Then** không cho phép chỉnh sửa loại này.

---

### User Story 7 - Vòng đời tự động: giữ chỗ quá hạn và no-show (Priority: P3)

Là **hệ thống**, tôi cần tự động hủy booking không thanh toán cọc đúng hạn và đánh dấu no-show khi khách không đến, để giải phóng tồn kho và áp dụng chính sách cọc.

**Why this priority**: FR-04 Hold Timeout và No-show; giảm overbooking ảo và tồn kho treo.

**Independent Test**: Tạo Pending Deposit → không thanh toán sau thời gian cấu hình → Cancelled; Confirmed qua 24h sau giờ check-in mà chưa check-in → No-show, cọc không hoàn.

**Acceptance Scenarios**:

1. **Given** booking Pending Deposit quá **10 phút** (có thể cấu hình) không có cọc xác nhận, **When** scheduled job chạy, **Then** booking → Cancelled, phòng available.
2. **Given** booking Confirmed, **When** quá **24 giờ** kể từ **`checkInDate` 00:00 (local) + configured hours** mà chưa check-in, **Then** booking → **No-show**, tiền cọc **không hoàn**.
3. **Given** Customer thanh toán cọc ngay trước khi job timeout chạy, **When** xác nhận thành công, **Then** booking Confirmed, không bị hủy bởi timeout.

---

### Edge Cases

- Hai khách đặt cùng phòng cùng millisecond → chỉ một thành công (zero overbooking).
- Hai booking cùng phòng: A `[D1, D2)`, B `[D2, D3)` → **không** overlap (same-day turnover hợp lệ theo half-open).
- Create/modify chồng `RoomStatusBlock` active (MAINTENANCE/OOS) trên cùng room → từ chối (không tạo/cập nhật booking).
- Customer thanh toán online nhưng mạng chậm / callback trễ → booking không bị hủy sai nếu thanh toán thực sự thành công *(đối soát cron FR-12 deferred TD-010; IPN deferred TD-012; rely on **Return URL** + hold timeout)*.
- ~~Hủy booking Pending Deposit đang chờ Manager duyệt chuyển khoản~~ — **N/A** (TD-011 VNPay-only).
- Manager hủy booking (lỗi hệ thống/bất khả kháng) → **bắt buộc hoàn 100% cọc**, khác với hủy do Customer.
- Phát hiện hư hại khi inspection → **Damage Fee** được Manager phê duyệt, lưu **`damageFeeAmount`** (tách khỏi `remainingAmount`); check-out chỉ khi inspection xong và `DAMAGE_FEE` đã PAID nếu có (chi tiết dispute FR-23).
- Customer thanh toán **60% còn lại** trước hoặc tại check-in — booking vẫn Confirmed cho đến check-in; remaining balance theo dõi trong chi tiết booking (thu tiền chi tiết FR-11/FR-12).
- Sửa booking Confirmed làm tăng giá → Customer được thông báo số tiền bổ sung cần thanh toán.
- Booking No-show → phòng được giải phóng theo quy trình vận hành; không cho Customer review (FR-14).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST cho phép **Customer** tạo yêu cầu đặt phòng với room, check-in, check-out, guestCount, specialRequests (SCR-16).
- **FR-002**: Hệ thống MUST **khóa tồn kho phòng** theo khoảng ngày khi booking được tạo và MUST **ngăn overbooking** — không hai booking active cùng phòng trùng khoảng ngày. Khoảng khóa MUST là **half-open** `[checkIn, checkOut)` (ngày check-out không chiếm phòng); overlap khi `checkIn < otherCheckOut AND checkOut > otherCheckIn`. Same-day turnover (A check-out ngày D, B check-in ngày D) MUST được phép nếu không còn conflict khác. Create booking và Manager modify MUST cũng **từ chối** khi khoảng yêu cầu chồng **RoomStatusBlock** active (MAINTENANCE / OUT_OF_SERVICE) của phòng đó (FR-05).
- **FR-003**: Hệ thống MUST **snapshot TotalAmount, DepositAmount (40%), RemainingAmount (60%)** tại thời điểm tạo booking; thay đổi giá phòng sau đó MUST NOT ảnh hưởng booking đã tạo.
- **FR-004**: Hệ thống MUST hỗ trợ trạng thái booking: Pending Deposit, Confirmed, Checked-in, Pending Inspection, Pending Damage Payment, Checked-out, Cancelled, No-show.
- **FR-005**: Hệ thống MUST chuyển booking **Pending Deposit → Confirmed** khi cọc 40% được xác nhận qua **VNPay** (**Return URL** verified). ~~IPN~~ **TD-012**. ~~Manager duyệt chuyển khoản~~ — **out of edu scope (TD-011)**.
- **FR-006**: Khi cọc được xác nhận, hệ thống MUST **tự động tạo và gửi Accommodation Contract** qua email cho Customer (nội dung hợp đồng chi tiết thuộc FR-10).
- **FR-007**: Hệ thống MUST tự động **hủy booking Pending Deposit** và nhả phòng nếu không có cọc xác nhận trong thời gian giữ chỗ (**mặc định 10 phút**, có thể cấu hình).
- **FR-008**: Hệ thống MUST chuyển booking **Confirmed → No-show** nếu `now > checkInDate.atStartOfDay().plusHours(N)` mà chưa check-in (**N mặc định 24**, config `app.booking.no-show-after-hours`); tiền cọc không hoàn. Không yêu cầu trường giờ check-in riêng.
- **FR-009**: Customer MUST có thể **xem danh sách và chi tiết** booking của chính mình (SCR-17, SCR-18); MUST NOT xem booking người khác.
- **FR-010**: Customer MUST có thể **hủy booking** trước check-in theo chính sách (ngày lịch đến `checkInDate`): **≥7 ngày** hoàn **100%** cọc; **`3 ≤ days < 7`** hoàn **50%**; **`<3` ngày** không hoàn; MUST NOT hủy sau khi Checked-in.
- **FR-011**: Manager MUST có thể **xem danh sách và chi tiết** booking thuộc property được gán (SCR-34, SCR-35); MUST NOT truy cập property khác.
- **FR-012**: Manager MUST có thể **check-in** booking Confirmed (→ Checked-in) và **check-out** (→ Checked-out) khi điều kiện nghiệp vụ thỏa mãn.
- **FR-013**: Check-out MUST bị **chặn** cho đến khi **Room Inspection** hoàn tất (FR-23). Nếu có Damage Fee đã duyệt, hệ thống MUST lưu riêng **`damageFeeAmount`** và payment type **`DAMAGE_FEE`** (không cộng vào snapshot **`remainingAmount`** 60%); API/UI MUST hiển thị breakdown (`remainingAmount`, `damageFeeAmount`, số còn phải trả); check-out MUST chờ `DAMAGE_FEE` **PAID** (chi tiết dispute FR-23).
- **FR-014**: Khi check-out hoàn tất, hệ thống MUST chuyển phòng sang **Pending Cleaning** và **tạo HousekeepingTask** tự động (thực thi dọn phòng FR-21).
- **FR-015**: Manager MUST có thể **sửa booking Confirmed** — đổi ngày hoặc phòng — với tính toán chênh lệch giá và cập nhật khóa tồn kho; MUST NOT sửa booking Checked-in/Cancelled/No-show; MUST áp dụng cùng rule availability như tạo booking (overlap half-open + RoomStatusBlock).
- **FR-016**: Manager-initiated cancel (lỗi hệ thống/bất khả kháng) MUST **hoàn 100% cọc** và ghi nhận lý do/hình thức hủy.
- **FR-017**: ~~Customer chọn cọc **chuyển khoản** MUST upload biên lai; chờ Manager approve/reject~~ — **DEFERRED / OUT OF EDU SCOPE (TD-011)**. Edu: chỉ **VNPay**.

### Key Entities

- **Booking**: customer, room, checkIn/checkOut, guestCount, totalAmount (snapshot), depositAmount, remainingAmount (60% stay snapshot — không gồm damage), damageFeeAmount (riêng, từ FR-23), specialRequests, status, holdExpiresAt, cancel metadata (cancelledBy, cancelReason, cancelledAt).
- **Inventory Lock** (logic nghiệp vụ): gắn room + date range **half-open** `[checkIn, checkOut)` với booking active; released khi Cancelled/No-show/Checked-out theo quy tắc.
- **Payment** (liên kết): cọc 40%, phần còn lại 60%, damage fee (`DAMAGE_FEE`) — trạng thái thanh toán chi tiết thuộc FR-11/FR-12; FR-04 yêu cầu xác nhận cọc để Confirmed và gate checkout khi còn damage unpaid.
- **Contract** (liên kết): sinh khi cọc xác nhận; immutable snapshot (FR-10).
- **RoomInspection** (liên kết FR-23): gate trước check-out.
- **HousekeepingTask** (liên kết FR-21): sinh sau check-out.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Customer hoàn tất **đặt phòng và khởi tạo thanh toán cọc VNPay** trong **dưới 5 phút** từ màn hình checkout (không tính thời gian trên cổng VNPay).
- **SC-002**: **0% overbooking** — không có trường hợp hai booking active cùng phòng trùng khoảng ngày trong kiểm thử đồng thời.
- **SC-003**: **100%** booking Pending Deposit quá thời gian giữ chỗ được tự động hủy và nhả phòng trong vòng **5 phút** sau thời hạn.
- **SC-004**: **95%** booking cọc trực tuyến thành công chuyển Confirmed và Customer nhận email hợp đồng trong **10 phút** kể từ thanh toán.
- **SC-005**: Customer xem chính sách hoàn tiền chính xác **100%** các tier (`≥7d` / `3≤d<7` / `<3d`) trước khi xác nhận hủy.
- **SC-006**: Manager check-in/check-out một booking trong **dưới 2 phút** từ màn hình chi tiết khi điều kiện đã đủ.
- **SC-007**: **90%** Customer tìm thấy trạng thái và số tiền còn lại của booking trên màn chi tiết **không cần liên hệ support**.

## Assumptions

- **Edu (2026-07-23):** Thanh toán **online chỉ VNPay** — cắt chuyển khoản + biên lai (FR-017 / US5). Xem TD-011. **Ngoại lệ giữ:** thu phần còn lại **tiền mặt tại quầy** khi check-in (SCR-37 `remainingCollected`).
- Customer phải đăng nhập (FR-01) để tạo booking; Guest chỉ discovery (FR-03).
- Tỷ lệ cọc **40% / còn lại 60%** cố định theo Specification_v2.
- Thời gian giữ chỗ **mặc định 10 phút** (configurable); khớp `app.booking.hold-timeout-minutes` trong code. api-spec-by-screen nếu ghi 15 phút — ưu tiên **10 phút** theo quyết định Spec Sync 2026-07-23.
- No-show: deadline = **`checkInDate.atStartOfDay()` + 24 giờ** (configurable); cọc không hoàn. Không lưu `expectedCheckInTime` trong phạm vi FR-04.
- Khoảng ngày inventory / overlap: **half-open** `[checkIn, checkOut)` — thống nhất với calendar FR-05; không dùng inclusive `[checkIn, checkOut]` trên write-path.
- Chính sách hủy Customer: **≥7 ngày 100%**, **`3 ≤ days < 7` → 50%**, **`<3` → 0%** tính theo ngày lịch trước check-in (ngày đúng 7 = 100%).
- Thanh toán phần còn lại và damage fee chi tiết (VNPay, invoice) thuộc FR-11/FR-12; FR-04 theo dõi `remainingAmount` (60%) và `damageFeeAmount` riêng; gate check-in khi thu remaining tại quầy; gate check-out khi damage unpaid.
- Room Inspection workflow chi tiết (Employee assign, dispute, Admin escalation) thuộc FR-23; FR-04 chỉ yêu cầu **chặn check-out** until inspection done.
- Contract PDF template và addendum damage thuộc FR-10.
- Tiền tệ và format hiển thị theo locale VND đã dùng trong project docs.
- WebSocket notification cho booking events thuộc FR-15; FR-04 yêu cầu thông báo tối thiểu qua email/in-app khi Confirmed, cancel, no-show.

## Out of Scope (edu — until Spec Sync reopens TD-011)

- Bank transfer deposit/remaining + PaymentReceipt upload (SCR-20)
- Manager Approve/Reject chuyển khoản cho payment flow (không phải check-in cash)
- ~~Cash remaining tại quầy~~ — **IN SCOPE** (product decision 2026-07-23; SCR-37 check-in)
