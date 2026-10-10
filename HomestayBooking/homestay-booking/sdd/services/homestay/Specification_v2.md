# Homestay / Resort Booking Management System

## 1. Context & Goal

### Background

Việc quản lý homestay và resort hiện nay chủ yếu được thực hiện thủ công thông qua sổ sách, Excel hoặc các ứng dụng nhắn tin. Điều này gây khó khăn trong việc quản lý phòng, hợp đồng đặt phòng, thanh toán đặt cọc và các yêu cầu bảo trì.

Hệ thống Homestay / Resort Booking Management System được xây dựng nhằm số hóa toàn bộ quy trình quản lý đặt phòng homestay và resort, giúp Admin, Manager, Employee, Customer và khách tham quan làm việc trên một nền tảng tập trung. Hệ thống hỗ trợ nhiều property tại các địa điểm du lịch khác nhau.

### Goals

* Hỗ trợ tìm kiếm và đặt phòng homestay/resort trực tuyến.
* Quản lý hợp đồng đặt phòng (Accommodation Contract).
* Quản lý thanh toán đặt cọc (40%) và thanh toán phần còn lại.
* Tự động tạo và gửi hợp đồng PDF qua email sau khi đặt cọc thành công.
* Hiển thị lịch trống và tình trạng phòng theo thời gian thực.
* Quản lý cấu trúc phân cấp: Property → Floor → Room.
* Phân quyền theo mô hình phân cấp: Admin → Manager → Employee.
* Quản lý quy trình Housekeeping (dọn phòng) sau khi check-out.
* Hỗ trợ phân công Employee cho các tác vụ vận hành (Housekeeping, Maintenance).
* Cung cấp báo cáo và thống kê cho Admin (toàn hệ thống) và Manager (theo Property).
* Hỗ trợ quản trị hệ thống tập trung.

---

## 2. Actors & Roles

### Guest

Người dùng chưa đăng nhập hệ thống.

**Permissions**

* Xem danh sách phòng.
* Xem chi tiết phòng và lịch trống.
* Xem banner khuyến mãi trên trang chủ.
* Xem thống kê nền tảng (số lượng property, phòng, booking).
* Tìm kiếm với các gợi ý tự động (search suggestions).
* Đăng ký tài khoản.
* Đăng nhập bằng email/mật khẩu.
* Đăng nhập bằng tài khoản Google.
* Quên mật khẩu.

### Customer

Khách đã đăng nhập, có thể thực hiện đặt phòng homestay/resort.

**Permissions**

* Xem Dashboard cá nhân thống kê hoạt động.
* Quản lý hồ sơ cá nhân và đổi mật khẩu.
* Gửi yêu cầu đặt phòng (Booking).
* Hủy đặt phòng (trước khi check-in).
* Xem lịch sử đặt phòng.
* Xem hợp đồng đặt phòng (Accommodation Contract).
* Thanh toán đặt cọc và thanh toán phần còn lại (qua VNPay ).
* Gửi, chỉnh sửa và xóa yêu cầu bảo trì.
* Đánh giá phòng sau khi check-out (có thể chỉnh sửa/xóa đánh giá).
* Gửi khiếu nại và theo dõi danh sách khiếu nại cá nhân.
* Nhận thông báo từ hệ thống.
* Tải và xem hợp đồng PDF.

### Admin

Quản trị viên cấp cao nhất của hệ thống.

**Permissions**

* Quản lý tất cả Manager (tạo, cập nhật, kích hoạt/đình chỉ).
* Tạo, chỉnh sửa, kích hoạt/vô hiệu hóa Property.
* Gán Manager vào Property.
* Quản lý cấu hình hệ thống (System Settings).
* Quản lý banner khuyến mãi (Promotions).
* Xem báo cáo toàn hệ thống (Global Reports).
* Quản lý tài khoản Customer (bao gồm kích hoạt/đình chỉ).
* Quản lý và giải quyết khiếu nại.
* Kiểm duyệt nội dung (Review).
* Quản lý phân quyền người dùng.
* Theo dõi hoạt động hệ thống.
* Không thực hiện các tác vụ vận hành (Housekeeping, Maintenance).

### Manager

Người quản lý Property được Admin gán. Manager chỉ truy cập dữ liệu thuộc Property được gán.

**Permissions**

* Quản lý cấu trúc (Structure Management): Floor (chỉ trong Property được gán).
* Quản lý phòng (Room Management) (chỉ trong Property được gán).
* Quản lý đặt phòng (Booking Management) (chỉ trong Property được gán).
* Quản lý hợp đồng đặt phòng (Contract Management) (chỉ trong Property được gán).
* ~~Xác nhận thanh toán chuyển khoản (Payment Verification)~~ — **out of edu scope (TD-011)**; edu chỉ VNPay auto-confirm.
* Tạo và gửi lại hợp đồng PDF.
* Xem báo cáo theo Property (Property Reports).
* Phân công Employee vào Property.
* Quản lý quy trình Housekeeping — gán Employee dọn phòng.
* Quản lý bảo trì (Maintenance) — gán Employee xử lý, xác nhận hoàn thành.
* Phê duyệt bồi thường hư hại phòng (Damage Compensation Approval).
* Không truy cập dữ liệu thuộc Property khác.

### Employee

Nhân viên vận hành thuộc một Property duy nhất. Employee được gán bởi Admin hoặc Manager của Property.

**Permissions**

* Xem danh sách phòng trong Property được gán.
* Xem danh sách tác vụ Housekeeping được gán.
* Xem danh sách tác vụ Maintenance được gán.
* Bắt đầu và hoàn thành dọn phòng (Housekeeping).
* Bắt đầu và hoàn thành bảo trì (Maintenance).
* Cập nhật trạng thái tác vụ.
* Thực hiện kiểm tra phòng trước check-out (Room Inspection).
* Ghi nhận hư hại, chụp ảnh và tạo Damage Report.
* Không quản lý Booking, Payment, Room, Customer, hoặc System Settings.

---

## 3. Functional Requirements

### FR-01 Authentication

* Người dùng có thể đăng ký tài khoản bằng email.
* Hệ thống xác thực email đăng ký thông qua mã OTP.
* Người dùng có thể đăng nhập bằng email và mật khẩu.
* Đăng nhập nhanh Google. **Rule bảo mật**: Hệ thống chỉ tự động liên kết tài khoản Google nếu email đã được xác thực trước đó ở local account, nếu không yêu cầu xác minh thêm (ngăn ngừa account takeover).
* Quản lý phiên làm việc bằng Access Token và Refresh Token, cho phép thu hồi token khi log out.
* Người dùng có thể đặt lại mật khẩu qua email.
* Người dùng có thể đổi mật khẩu (yêu cầu đang đăng nhập).
* Người dùng có thể đăng xuất để kết thúc phiên làm việc an toàn.
* Ngăn chặn đăng nhập đối với các tài khoản đang bị đình chỉ (Suspended).

### FR-02 User Profile

* Xem hồ sơ cá nhân.
* Cập nhật hồ sơ cá nhân.
* Quản lý thông tin liên hệ.

### FR-03 Room Discovery

* Hiển thị danh sách phòng theo từng property.
* Cung cấp thanh tìm kiếm với gợi ý thông minh (search suggestions).
* Tìm kiếm phòng theo nhiều tiêu chí (địa điểm, loại phòng, khoảng giá, ngày check-in/check-out, số khách).
* Xem chi tiết phòng bao gồm gallery, tiện ích và lịch trống.
* Xem lịch trống (Availability Calendar) theo từng phòng.
* Hiển thị thống kê tổng quan về nền tảng trên trang chủ.

### FR-04 Booking & Inventory Management

* **Inventory Locking**: Sử dụng khóa pessimistic trên `Room` + kiểm tra overlap khoảng ngày `(RoomId, CheckIn, CheckOut)` trong cùng transaction (SQL Server) để đảm bảo tính atomic, ngăn chặn tuyệt đối Overbooking khi 2 người đặt trùng phòng cùng lúc.
* **Pricing Snapshot**: `TotalAmount` được snapshot và tính toán lúc tạo booking, không bị ảnh hưởng nếu Manager thay đổi giá `PricePerNight` sau đó.
* **Quy trình Booking**: 
  1. Yêu cầu đặt phòng → `Pending Deposit`.
  2. **Hold Timeout**: Nếu sau 10 phút (configurable) khách không thanh toán cọc, Scheduled Job tự động chuyển Booking sang `Cancelled` và nhả phòng (Available).
  3. Thanh toán thành công → Trạng thái `Confirmed`.
  4. Nếu quá 24h từ giờ Check-in mà khách không đến, hệ thống chuyển sang trạng thái `No-show` (Cọc không hoàn).
* Customer thanh toán đặt cọc 40% tổng giá trị đặt phòng qua **VNPay** (xác nhận tự động qua **Return URL**).  
  > **Edu scope (TD-011):** chỉ VNPay. Chuyển khoản + upload biên lai + Manager verify CK — **out of scope**.  
  > **Edu scope (TD-012):** **không** bắt buộc IPN — Return URL đủ sandbox/edu. Chi tiết: `sdd/sprints/tech-debt.md`.
* Khi tiền cọc được xác nhận: hệ thống tự động chuyển trạng thái Booking → Confirmed, tạo Accommodation Contract PDF và gửi qua email (chạy nền thông qua @Async của Spring Boot).
* Customer thanh toán phần còn lại (60%) trước hoặc khi check-in (qua **VNPay**).
* Manager thực hiện quy trình Check-in và Check-out để cập nhật trạng thái thực tế của Booking và phòng.
* Trước khi Check-out: Employee thực hiện Room Inspection. Check-out bị chặn cho đến khi kiểm tra hoàn tất.
* Nếu phát hiện hư hại: Damage Fee được Manager phê duyệt và cộng vào số tiền còn lại.
* Khi Check-out hoàn tất: hệ thống tự động chuyển trạng thái phòng sang Pending Cleaning và tạo HousekeepingTask.
* **Sửa / Hủy Booking**:
  * Chỉnh sửa Booking: Hỗ trợ đổi ngày/phòng cho trạng thái Confirmed (tính toán phụ phí chênh lệch).
  * Hủy Booking (Customer): hoàn theo **tier trên tiền cọc đã trả** (canonical FR-010 / `004`): **≥7 ngày trước check-in → 100% cọc**; **3–7 ngày → 50% cọc**; **&lt;3 ngày → 0%**. Chỉ hủy trước khi check-in. UI: SCR-19. API: `POST /api/v1/bookings/{id}/cancel` (preview nên trả `refundPercent` / `refundAmount`).
  * Hủy Booking (Manager): Manager-initiated cancel (lỗi hệ thống/bất khả kháng) → bắt buộc hoàn **100% cọc**. Chỉ trong Property được gán; chỉ khi status = Pending Deposit hoặc Confirmed (trước check-in). UI: SCR-69 (từ SCR-35). API: `POST /api/v1/manager/bookings/{id}/cancel` với body bắt buộc `{ "cancelReason": "..." }`. Preview: `GET /api/v1/manager/bookings/{id}/cancel/preview`. Postcondition: Booking → Cancelled; Payment cọc đã Paid → Refunded theo chính sách; Room → Available; Contract (nếu có) → Cancelled; gửi Notification cho Customer.

### FR-05 Availability Calendar

* Mỗi phòng hiển thị lịch trống với các trạng thái: Available, Pending Deposit, Reserved, Occupied, Pending Cleaning, Cleaning In Progress, Maintenance, Out Of Service.
* Admin hoặc Manager có thể cập nhật trạng thái phòng thủ công (ngoại trừ chuyển sang Available khi tác vụ Housekeeping chưa hoàn thành).

### FR-06 Property Management

* Admin tạo property mới (homestay/resort).
* Admin chỉnh sửa thông tin property.
* Admin quản lý trạng thái property (Active/Inactive).
* Admin gán Manager vào Property thông qua ManagerPropertyAssignment.
* Mỗi Property phải có một Manager đang hoạt động (ACTIVE).
* Một Manager có thể quản lý nhiều Property.
* Manager xem danh sách và chi tiết Property được gán.

### FR-07 Structure Management

* Quản lý phân cấp: Property → Floor → Room.
* Manager thêm, sửa, xóa Floor trong Property được gán.
* Property Selector và Structure Tree View (Manager chỉ thấy Property được gán).

### FR-08 Room Management

* Manager thêm phòng vào đúng Property/Floor được gán.
* Manager chỉnh sửa thông tin phòng trong Property được gán.
* Manager xóa phòng (chỉ cho phép khi không có đặt phòng nào đang diễn ra).
* Quản lý trạng thái phòng.
* Quản lý hình ảnh phòng (gallery) và thứ tự hiển thị ảnh.
* Lọc danh sách phòng theo Property, Floor dành riêng cho Manager (chỉ Property được gán).
* Employee có thể xem danh sách phòng trong Property được gán.

### FR-09 Customer Management

* Admin xem danh sách Customer với các bộ lọc và tìm kiếm.
* Admin cập nhật trạng thái tài khoản Customer (Active/Suspended).
* Admin xem hồ sơ và lịch sử đặt phòng của từng Customer.

### FR-10 Contract Management

* Tự động tạo Accommodation Contract PDF khi đặt cọc thành công (chạy nền thông qua @Async để không làm chậm API thanh toán).
* Gửi contract qua email cho Customer.
* **Immutable Snapshot**: Contract là tài liệu bất biến lưu trữ nội dung tại thời điểm tạo. Bất kỳ Damage Fee phát sinh thêm sẽ tạo Contract Addendum (Phụ lục hợp đồng).
* Xem danh sách và chi tiết hợp đồng đặt phòng.
* Tải PDF hợp đồng.
* In hợp đồng.
* Manager có thể yêu cầu gửi lại email hợp đồng.

### FR-11 Billing Management

* Tạo hóa đơn thanh toán đặt cọc (40%) và phần còn lại (60%).
* Theo dõi trạng thái hóa đơn.

### FR-12 Payment Management & Reconciliation

* Thanh toán đặt cọc (Deposit Payment) qua **VNPay**.
* Thanh toán phần còn lại (Remaining Balance Payment) qua **VNPay**.
* Thanh toán phí bồi thường hư hại (Damage Fee Payment) qua **VNPay** khi flow damage sẵn sàng.
* Khi chuyển VNPay, status Payment là `Pending`; xác nhận **Paid** qua **Return URL**, idempotent. ~~IPN~~ **Deferred edu (TD-012)**.
* **VNPay Reconciliation cron (15 phút) + Admin SCR-52 Sync:** **Deferred edu (TD-010)** — không bắt buộc cho khóa học; reopen khi Spec Sync yêu cầu. Chi tiết: `sdd/services/homestay/features/012-payment-management/spec.md`.
* **Chuyển khoản / PaymentReceipt / Manager verify CK:** **Deferred edu (TD-011)**. **Cash remaining tại quầy check-in:** kept (ADR-007).
* **VNPay IPN:** **Deferred edu (TD-012)** — sandbox/edu chỉ Return URL.

### FR-13 Maintenance Management

* Customer tạo yêu cầu bảo trì (bắt buộc liên kết với một booking hiện tại, hỗ trợ đính kèm hình ảnh minh họa).
* Customer có thể chỉnh sửa hoặc xóa yêu cầu bảo trì khi yêu cầu đó chưa được xử lý (trạng thái Open).
* Manager xem danh sách yêu cầu trong Property được gán và gán Employee xử lý.
* Employee thực hiện bảo trì và cập nhật trạng thái tác vụ.
* Manager xác nhận hoàn thành và ghi chú kết quả giải quyết.
* Quy trình: Open → Assigned → In Progress → Resolved → Closed.
* Theo dõi tiến độ xử lý và tự động thông báo cho Customer khi có cập nhật.

### FR-14 Review & Rating

* Customer đánh giá phòng sau khi quy trình check-out hoàn tất.
* Mỗi Booking chỉ được phép đánh giá tối đa 1 lần.
* Customer có thể xem lại, chỉnh sửa hoặc xóa đánh giá của chính mình.
* Review liên kết với Booking để đảm bảo tính xác thực.
* Manager hoặc Admin có thể kiểm duyệt và ẩn (Hide) các đánh giá không phù hợp.
* Xem đánh giá công khai của phòng.

### FR-15 Notification Engine

* Hệ thống tự động phân loại thông báo.
* Thông báo được xử lý qua luồng độc lập (@Async) để đảm bảo không chặn tiến trình chính. 
* Hỗ trợ push thông báo real-time qua WebSocket cho các sự kiện Booking/Payment và Availability Calendar.
* Gửi thông báo hệ thống và các sự kiện nghiệp vụ quan trọng.
* Thông báo hỗ trợ liên kết trực tiếp để điều hướng nhanh.
* Xem danh sách thông báo và số lượng thông báo chưa đọc.
* Đánh dấu đã đọc cho từng thông báo hoặc toàn bộ danh sách.

### FR-16 Reporting

* Admin: Báo cáo doanh thu toàn hệ thống theo property, tháng, năm.
* Admin: Thống kê tổng số property, floor, room, booking.
* Manager: Báo cáo doanh thu theo Property được gán, tháng, năm.
* Manager: Báo cáo tỷ lệ lấp đầy (Occupancy Rate) theo Property được gán.
* Báo cáo Booking Trend và Revenue Trend.

### FR-17 Administration

* Admin quản lý tài khoản Customer.
* Admin quản lý khiếu nại: Customer có thể tạo khiếu nại, Admin theo dõi và cập nhật quá trình giải quyết (Open → Investigating → Resolved → Closed).
* Admin kiểm duyệt nội dung (Đánh giá).
* Admin quản lý cấu hình hệ thống (System Settings): Cho phép điều chỉnh % đặt cọc, thông tin tài khoản ngân hàng thụ hưởng, email hỗ trợ, tên hệ thống.

### FR-18 Promotion Management

* Admin quản lý danh sách các chương trình/banner khuyến mãi.
* Tạo, sửa, xóa các banner hiển thị trên trang chủ với tiêu đề, hình ảnh, liên kết hành động (Call to Action) và màu sắc.
* Tùy chỉnh thứ tự hiển thị và bật/tắt (Active/Inactive) các banner.

### FR-19 Customer Dashboard

* Cung cấp Dashboard tổng quan cho Customer.
* Hiển thị nhanh số lượng đặt phòng đang hoạt động, yêu cầu bảo trì, thanh toán đang chờ và thông báo chưa đọc.
* Hiển thị các sự kiện Check-in/Check-out sắp diễn ra.
* Danh sách thanh toán và thông báo mới nhất.

### FR-20 Employee Management

* Admin hoặc Manager thêm Employee vào Property thông qua EmployeePropertyAssignment.
* Mỗi Employee chỉ thuộc một Property duy nhất tại một thời điểm (một assignment ACTIVE duy nhất).
* Xem danh sách Employee theo Property.
* Cập nhật thông tin Employee.
* Vô hiệu hóa/kích hoạt tài khoản Employee.
* Manager chỉ quản lý Employee trong Property được gán.

### FR-21 Housekeeping Management

* Hệ thống tự động tạo HousekeepingTask khi Booking chuyển sang trạng thái Checked-out (sau khi Room Inspection hoàn tất và tất cả thanh toán được hoàn thành).
* Trạng thái phòng tự động chuyển sang Pending Cleaning.
* Manager gán Employee thực hiện dọn phòng.
* Employee bắt đầu dọn phòng → Trạng thái phòng chuyển sang Cleaning In Progress.
* Employee hoàn thành dọn phòng → Trạng thái phòng tự động chuyển sang Available.
* Manager không được phép bỏ qua quy trình Housekeeping để chuyển phòng sang Available thủ công.
* Theo dõi lịch sử tác vụ Housekeeping.
* Trạng thái HousekeepingTask: Pending → In Progress → Completed / Cancelled.

### FR-22 Employee Dashboard

* Cung cấp Dashboard tổng quan cho Employee.
* Hiển thị danh sách tác vụ Housekeeping được gán.
* Hiển thị danh sách tác vụ Maintenance được gán.
* Hiển thị tác vụ trong ngày hôm nay.
* Hiển thị tác vụ đã hoàn thành và đang chờ xử lý.
* Hiển thị danh sách Room Inspection được gán.

### FR-23 Room Inspection & Damage Resolution

* Trước khi Check-out, Employee được gán thực hiện kiểm tra phòng (Room Inspection).
* Nếu không có hư hại: Inspection status = PASSED. Booking đủ điều kiện Check-out.
* Nếu phát hiện hư hại: Employee ghi nhận DamageItem.
* **Kiểm soát chéo (Segregation of Duties)**:
  * Manager duyệt Damage Fee. Nếu fee vượt quá 5 triệu VNĐ (configurable), hệ thống yêu cầu Admin đồng phê duyệt.
  * Customer nhận thông báo và có quyền **Dispute** (phản đối) khoản phí trong 24h. Mọi Dispute sẽ được escalate lên Admin.
* **Off-site Collection**: Nếu khách rời đi sớm và từ chối trả Damage Fee, Manager có thể đánh dấu Account của Customer là `Outstanding Debt`, chặn mọi booking tương lai cho đến khi thanh toán xong.
* Check-out bị chặn cho đến khi Room Inspection hoàn tất.
* Employee chỉ được kiểm tra phòng trong Property được gán.
* **Inspection Checklist (có cấu hình)**:
  * Danh mục đồ vật kiểm tra lưu trong `ChecklistItemDefinition` (seed mặc định: TV, AC, Minibar, Bathroom, Beds).
  * Mỗi lần Pass/Fail, Employee gửi kết quả từng mục → lưu `InspectionChecklistAnswer` (1 hàng / 1 mục / 1 inspection).
  * Checklist không còn hardcode UI-only; FE load từ API. Ghi chú tổng hợp vẫn ghi vào `RoomInspection.note`.
  * (Phase sau) Manager CRUD danh mục theo Property — chưa thuộc phiên bản này.

---

## 4. Non-Functional Requirements

### Performance

* Thời gian phản hồi trung bình dưới 3 giây.
* Hỗ trợ tối thiểu 500 người dùng đồng thời.

### Security

* Mật khẩu được mã hóa an toàn (Sử dụng chuẩn mã hóa công nghiệp).
* Sử dụng HTTPS.
* Xác thực phiên làm việc an toàn, phi trạng thái và có cơ chế luân chuyển phiên bảo mật.
* Hỗ trợ xác thực danh tính thông qua các nhà cung cấp uy tín (Google).
* Phân quyền theo vai trò (Guest, Customer, Employee, Manager, Admin) chặt chẽ tại tất cả các điểm truy cập.
* Admin: toàn quyền quản trị hệ thống. Manager: chỉ truy cập Property được gán. Employee: chỉ truy cập Property và tác vụ được gán. Customer: chỉ truy cập dữ liệu cá nhân. Guest: chỉ xem thông tin công khai.
* Đảm bảo cô lập dữ liệu theo Property (Property-level data isolation).
* Xác thực chữ ký dữ liệu để đảm bảo an toàn thanh toán trực tuyến.
* Chống truy cập trái phép.

### Availability

* Hệ thống hoạt động tối thiểu 99% thời gian.

### Scalability

* Có khả năng mở rộng số lượng property, phòng và người dùng.

### Usability

* Giao diện thân thiện.
* Hỗ trợ trên desktop và mobile.

### Maintainability

* Mã nguồn được tổ chức rõ ràng.
* Dễ bảo trì và mở rộng.

---

## 5. Data Model

## User

| Attribute    | Type     | Description            |
| ------------ | -------- | ---------------------- |
| Id           | UUID     | Unique user identifier |
| FullName     | String   | User full name         |
| Email        | String   | Login email            |
| Phone        | String   | Contact number         |
| AvatarUrl    | String   | Profile picture        |
| Role         | Enum     | ADMIN, MANAGER, EMPLOYEE, CUSTOMER |
| Status       | Enum     | INACTIVE, ACTIVE, SUSPENDED |
| CreatedAt    | DateTime | Creation date          |
| UpdatedAt    | DateTime | Last update date       |

---

## Property

| Attribute    | Type     | Description                |
| ------------ | -------- | -------------------------- |
| Id           | UUID     | Unique property identifier |
| Name         | String   | Property name              |
| Address      | String   | Property address           |
| Description  | Text     | Property description       |
| Status       | Enum     | ACTIVE, INACTIVE           |
| CreatedAt    | DateTime | Creation date              |
| UpdatedAt    | DateTime | Last update date           |

### Business Constraints

* Mỗi Property phải có một Manager ACTIVE được gán thông qua ManagerPropertyAssignment.
* Một Manager có thể quản lý nhiều Property.

---

---

## PricingRule (Mới - Cấu hình giá động)

| Attribute     | Type    | Description                  |
| ------------- | ------- | ---------------------------- |
| Id            | UUID    | Rule identifier              |
| PropertyId    | UUID    | Related property             |
| RoomTypeId    | UUID    | Related room type (optional) |
| StartDate     | Date    | Apply from date              |
| EndDate       | Date    | Apply to date                |
| PricePerNight | Decimal | Dynamic price                |
| Priority      | Integer | Priority to override default |

## ManagerPropertyAssignment

| Attribute    | Type     | Description              |
| ------------ | -------- | ------------------------ |
| Id           | UUID     | Assignment identifier    |
| ManagerId    | UUID     | Assigned Manager         |
| PropertyId   | UUID     | Assigned Property        |
| AssignedBy   | UUID     | Admin who assigned       |
| AssignedAt   | DateTime | Assignment date          |
| Status       | Enum     | ACTIVE, INACTIVE         |

### Business Constraints

* Mỗi Property chỉ có một assignment ACTIVE tại một thời điểm.
* Lưu lịch sử gán Manager (INACTIVE assignments được giữ lại).

---

## EmployeePropertyAssignment

| Attribute    | Type     | Description                   |
| ------------ | -------- | ----------------------------- |
| Id           | UUID     | Assignment identifier         |
| EmployeeId   | UUID     | Assigned Employee             |
| PropertyId   | UUID     | Assigned Property             |
| AssignedBy   | UUID     | Admin or Manager who assigned |
| AssignedAt   | DateTime | Assignment date               |
| Status       | Enum     | ACTIVE, INACTIVE              |

### Business Constraints

* Mỗi Employee chỉ có một assignment ACTIVE tại một thời điểm.
* Employee chỉ thuộc một Property duy nhất.

---

## Floor

| Attribute   | Type     | Description            |
| ----------- | -------- | ---------------------- |
| Id          | UUID     | Floor identifier       |
| PropertyId  | UUID     | Related property       |
| FloorNumber | Integer  | Floor number           |
| Description | Text     | Floor description      |
| CreatedAt   | DateTime | Creation date          |
| UpdatedAt   | DateTime | Last update date       |

---

## Room

| Attribute     | Type    | Description            |
| ------------- | ------- | ---------------------- |
| Id            | UUID    | Unique room identifier |
| PropertyId    | UUID    | Related property       |
| FloorId       | UUID    | Related floor          |
| RoomNumber    | String  | Room number            |
| RoomType      | String  | Room category          |
| Area          | Decimal | Room area              |
| PricePerNight | Decimal | Price per night        |
| Capacity      | Integer | Maximum occupancy      |
| Status        | Enum    | Room status            |
| Description   | Text    | Room description       |
| CreatedAt     | DateTime| Creation date          |
| UpdatedAt     | DateTime| Last update date       |

### Values

* Available
* Pending Deposit
* Reserved
* Occupied
* Pending Cleaning
* Cleaning In Progress
* Maintenance
* Out Of Service

---

## RoomImage

| Attribute | Type    | Description      |
| --------- | ------- | ---------------- |
| Id        | UUID    | Image identifier |
| RoomId    | UUID    | Related room     |
| ImageUrl  | String  | Image URL        |
| IsPrimary | Boolean | Main image       |
| SortOrder | Integer | Display order    |
| CreatedAt | DateTime| Creation date    |

*Constraint*: Unique Partial Index `(RoomId)` WHERE `IsPrimary = true` (Chỉ 1 ảnh chính).

---

## Booking

| Attribute      | Type     | Description           |
| -------------- | -------- | --------------------- |
| Id             | UUID     | Booking identifier    |
| CustomerId     | UUID     | Customer              |
| RoomId         | UUID     | Booked room           |
| CheckInDate    | Date     | Check-in date         |
| CheckOutDate   | Date     | Check-out date        |
| GuestCount     | Integer  | Number of guests      |
| TotalAmount    | Decimal  | Snapshot total amount |
| DepositAmount  | Decimal  | Deposit amount (40%)  |
| RemainingAmount| Decimal  | Remaining balance     |
| DamageFeeAmount| Decimal  | Approved damage fee   |
| SpecialRequests| Text     | Special requests note |
| Status         | Enum     | Booking status        |
| HoldExpiresAt  | DateTime | Timeout giữ phòng     |
| RowVersion     | Integer  | Optimistic locking    |
| CancelledBy    | UUID     | Ai hủy phòng          |
| CancelReason   | Text     | Lý do hủy phòng       |
| CancelledAt    | DateTime | Thời gian hủy         |
| CreatedAt      | DateTime | Submission date       |
| UpdatedAt      | DateTime | Last update date      |

### Values

* Pending Deposit
* Confirmed
* Checked-in
* Pending Inspection
* Pending Damage Payment
* Checked-out
* Cancelled
* **No-show**

---

## Contract

| Attribute      | Type    | Description                    |
| -------------- | ------- | ------------------------------ |
| Id             | UUID    | Contract identifier            |
| BookingId      | UUID    | Related booking                |
| CustomerId     | UUID    | Customer                       |
| RoomId         | UUID    | Room                           |
| CheckInDate    | Date    | Check-in date                  |
| CheckOutDate   | Date    | Check-out date                 |
| DepositAmount  | Decimal | Deposit amount                 |
| TotalAmount    | Decimal | Total accommodation amount     |
| PdfUrl         | String  | Generated contract PDF URL     |
| SentAt         | DateTime| Contract email sent timestamp  |
| GeneratedAt    | DateTime| Contract generation timestamp  |
| Status         | Enum    | Contract status                |
| CreatedAt      | DateTime| Creation date                  |
| UpdatedAt      | DateTime| Last update date               |

### Values

* Active
* Completed
* Cancelled

---

## Payment

| Attribute             | Type     | Description                              |
| --------------------- | -------- | ---------------------------------------- |
| Id                    | UUID     | Payment identifier                       |
| BookingId             | UUID     | Related booking                          |
| CustomerId            | UUID     | Related customer                         |
| Type                  | Enum     | Deposit / Remaining Balance / Damage Fee |
| Amount                | Decimal  | Paid amount                              |
| Method                | Enum     | **VNPay** (edu); Bank Transfer / Cash enum có thể tồn tại trong DB nhưng **unused in edu UI** (TD-011) |
| Status                | Enum     | Pending / Paid / Failed / **Refunded**   |
| GatewayTransactionId  | String   | Mã giao dịch cổng thanh toán             |
| OrderRef              | String   | Mã đơn hàng đối soát (Idempotency Key)   |
| GatewayResponseCode   | String   | Mã lỗi từ VNPay                          |
| IpnReceivedAt         | DateTime | Thời gian nhận callback IPN              |
| VerifiedBy            | UUID     | Auditor (Manager)                        |
| VerifiedAt            | DateTime | Verification timestamp                   |
| VerificationNote      | Text     | Approval/Rejection details               |
| PaidAt                | DateTime | Actual payment date                      |
| CreatedAt             | DateTime | Creation date                            |
| UpdatedAt             | DateTime | Last update date                         |

---

## PaymentReceipt

| Attribute  | Type     | Description        |
| ---------- | -------- | ------------------ |
| Id         | UUID     | Receipt identifier |
| PaymentId  | UUID     | Related payment    |
| FileUrl    | String   | Receipt image/file |
| FileName   | String   | Original file name |
| FileSize   | Long     | File size          |
| CreatedAt  | DateTime | Upload date        |

---

---

## Attachment (Mới - Dùng chung thay cho PhotoUrls)

| Attribute  | Type     | Description             |
| ---------- | -------- | ----------------------- |
| Id         | UUID     | Attachment identifier   |
| EntityType | String   | Maintenance / DamageItem|
| EntityId   | UUID     | Related entity ID       |
| FileUrl    | String   | File URL                |
| FileName   | String   | Original file name      |
| FileSize   | Long     | File size in bytes      |
| UploadedAt | DateTime | Upload timestamp        |

## MaintenanceTicket

| Attribute          | Type     | Description              |
| ------------------ | -------- | ------------------------ |
| Id                 | UUID     | Ticket identifier        |
| CustomerId         | UUID     | Request owner            |
| RoomId             | UUID     | Related room             |
| BookingId          | UUID     | Related booking          |
| Title              | String   | Issue title              |
| Description        | Text     | Issue description        |
| AssignedEmployeeId | UUID     | Assigned Employee        |
| AssignedAt         | DateTime | Employee assignment date |
| Status             | Enum     | Ticket status            |
| ResolutionNote     | Text     | Resolution details       |
| VerifiedBy         | UUID     | Manager who verified     |
| VerifiedAt         | DateTime | Verification timestamp   |
| CreatedAt          | DateTime | Creation date            |
| UpdatedAt          | DateTime | Last update date         |

### Values

* Open
* Assigned
* In Progress
* Resolved
* Closed

---

## Review

| Attribute  | Type     | Description        |
| ---------- | -------- | ------------------ |
| Id         | UUID     | Review identifier  |
| CustomerId | UUID     | Reviewer           |
| BookingId  | UUID     | Related booking    |
| RoomId     | UUID     | Reviewed room      |
| Rating     | Integer  | Rating score (1–5) |
| Comment    | Text     | Review content     |
| Status     | Enum     | PUBLISHED, HIDDEN  |
| CreatedAt  | DateTime | Review date        |
| UpdatedAt  | DateTime | Last update date   |

---

## Notification

| Attribute         | Type     | Description             |
| ----------------- | -------- | ----------------------- |
| Id                | UUID     | Notification identifier |
| UserId            | UUID     | Receiver                |
| Title             | String   | Notification title      |
| Content           | Text     | Notification content    |
| Type              | Enum     | Event category          |
| RelatedEntityId   | UUID     | Link to related record  |
| RelatedEntityType | String   | Target entity type      |
| IsRead            | Boolean  | Read status             |
| CreatedAt         | DateTime | Creation date           |

---

## Complaint

| Attribute       | Type     | Description          |
| --------------- | -------- | -------------------- |
| Id              | UUID     | Complaint identifier |
| UserId          | UUID     | Submitter            |
| Subject         | String   | Complaint title      |
| Description     | Text     | Complaint details    |
| Status          | Enum     | Complaint status     |
| ResolutionNotes | Text     | Resolution details   |
| ResolvedAt      | DateTime | Resolution time      |
| CreatedAt       | DateTime | Creation date        |
| UpdatedAt       | DateTime | Last update date     |

---

---



## ActivityLog

| Attribute  | Type     | Description      |
| ---------- | -------- | ---------------- |
| Id         | UUID     | Log identifier   |
| UserId     | UUID     | Related user     |
| Action     | String   | Performed action |
| EntityType | String   | Entity affected  |
| EntityId   | UUID     | Related entity   |
| Details    | Text     | Additional info  |
| CreatedAt  | DateTime | Action timestamp |

### Tracked Actions

* MANAGER_ASSIGNED
* MANAGER_UNASSIGNED
* EMPLOYEE_ASSIGNED
* EMPLOYEE_UNASSIGNED
* CLEANING_STARTED
* CLEANING_COMPLETED
* ROOM_STATUS_CHANGED
* MAINTENANCE_ASSIGNED
* MAINTENANCE_COMPLETED
* BOOKING_CHECKED_IN
* BOOKING_CHECKED_OUT
* INSPECTION_COMPLETED
* DAMAGE_REPORTED
* DAMAGE_APPROVED

---

## SystemSetting

| Attribute   | Type     | Description                  |
| ----------- | -------- | ---------------------------- |
| Id          | UUID     | Setting identifier           |
| Key         | String   | Configuration key            |
| Value       | Text     | Configuration value          |
| Description | String   | Description of the setting   |
| UpdatedBy   | UUID     | Auditor (Admin)              |
| UpdatedAt   | DateTime | Last update date             |

---

## Promotion

| Attribute   | Type     | Description                                |
| ----------- | -------- | ------------------------------------------ |
| Id          | UUID     | Promotion identifier                       |
| Title       | String   | Banner title                               |
| Subtitle    | String   | Banner subtitle                            |
| Description | Text     | Promotional details                        |
| CtaText     | String   | Call-to-action button text                 |
| CtaUrl      | String   | Action URL                                 |
| ColorTheme  | String   | Display theme color                        |
| IsActive    | Boolean  | Visibility status                          |
| SortOrder   | Integer  | Display sequence                           |
| CreatedAt   | DateTime | Creation date                              |
| UpdatedAt   | DateTime | Last update date                           |

---

## HousekeepingTask

| Attribute          | Type     | Description                  |
| ------------------ | -------- | ---------------------------- |
| Id                 | UUID     | Task identifier              |
| PropertyId         | UUID     | Related Property             |
| RoomId             | UUID     | Related Room                 |
| AssignedEmployeeId | UUID     | Assigned Employee            |
| Status             | Enum     | Task status                  |
| Note               | Text     | Task note                    |
| CreatedAt          | DateTime | Creation date                |
| StartedAt          | DateTime | Cleaning start time          |
| CompletedAt        | DateTime | Cleaning completion time     |
| UpdatedAt          | DateTime | Last update date             |

### Values

* Pending
* In Progress
* Completed
* Cancelled

---

## RoomInspection

| Attribute          | Type     | Description                  |
| ------------------ | -------- | ---------------------------- |
| Id                 | UUID     | Inspection identifier        |
| BookingId          | UUID     | Related Booking              |
| RoomId             | UUID     | Inspected Room               |
| PropertyId         | UUID     | Related Property             |
| InspectedBy        | UUID     | Employee who inspected       |
| Status             | Enum     | Inspection status            |
| Note               | Text     | Inspection note              |
| InspectedAt        | DateTime | Inspection timestamp         |
| CreatedAt          | DateTime | Creation date                |
| UpdatedAt          | DateTime | Last update date             |

### Values

* Pending
* In Progress
* Passed
* Failed With Damage

---

## DamageReport

| Attribute                 | Type     | Description                  |
| ------------------------- | -------- | ---------------------------- |
| Id                        | UUID     | Report identifier            |
| InspectionId              | UUID     | Related RoomInspection       |
| BookingId                 | UUID     | Related Booking              |
| TotalEstimatedCost        | Decimal  | Total estimated damage cost  |
| ApprovedAmount            | Decimal  | Manager-approved amount      |
| ApprovedBy                | UUID     | Manager who approved         |
| ApprovedAt                | DateTime | Approval timestamp           |
| RequiresAdminEscalation   | Boolean  | Indicates if > 5M VND        |
| AdminApproverId           | UUID     | Admin who co-approved        |
| Status                    | Enum     | Report status                |
| Note                      | Text     | Manager note                 |
| CreatedAt                 | DateTime | Creation date                |
| UpdatedAt                 | DateTime | Last update date             |

### Values

* Draft
* Pending Approval
* Approved
* Disputed
* Paid

---

## DamageItem

| Attribute      | Type     | Description                  |
| -------------- | -------- | ---------------------------- |
| Id             | UUID     | Item identifier              |
| DamageReportId | UUID     | Related DamageReport         |
| ItemName       | String   | Damaged item name            |
| Description    | Text     | Damage description           |
| EstimatedCost  | Decimal  | Estimated repair/replace cost|
| CreatedAt      | DateTime | Creation date                |

---

## ChecklistItemDefinition (Inspection checklist catalog)

| Attribute   | Type     | Description                                      |
| ----------- | -------- | ------------------------------------------------ |
| Id          | UUID     | Definition identifier                            |
| Code        | String   | Stable key (tv, ac, minibar, bathroom, beds)     |
| Label       | String   | Display label (VI)                               |
| Icon        | String   | Optional emoji / icon key                        |
| SortOrder   | Integer  | Display order                                    |
| IsActive    | Boolean  | Soft-disable without deleting history            |
| PropertyId  | UUID     | NULL = global default; set = property override   |
| CreatedAt   | DateTime | Creation date                                    |
| UpdatedAt   | DateTime | Last update                                      |

### Business Constraints

* `Code` unique trong cùng scope (global hoặc per Property).
* Seed mặc định 5 mục global khi hệ thống khởi động nếu bảng trống.

---

## InspectionChecklistAnswer

| Attribute              | Type     | Description                          |
| ---------------------- | -------- | ------------------------------------ |
| Id                     | UUID     | Answer identifier                    |
| InspectionId           | UUID     | Related RoomInspection               |
| ChecklistItemId        | UUID     | Related ChecklistItemDefinition      |
| Passed                 | Boolean  | true = OK, false = FAIL              |
| CreatedAt              | DateTime | Creation date                        |

### Business Constraints

* Unique `(InspectionId, ChecklistItemId)`.
* Ghi khi Employee Pass/Fail.

---

## 6. Roles & Permissions Matrix (RBAC)

Dưới đây là ma trận phân quyền chi tiết các Entity cốt lõi (C=Create, R=Read, U=Update, D=Delete). Giới hạn dữ liệu phụ thuộc vào cột **Scope**.

| Entity | Customer (Customer Scope) | Employee (Property Scope) | Manager (Property Scope) | Admin (Global Scope) |
|---|---|---|---|---|
| **Property/Floor/Room** | R | R | C, R, U, D | C, R, U, D |
| **Booking** | C, R, U (Hủy/Sửa) | R | R, U | R |
| **Payment** | R (Pay) | - | R, U (Verify) | R |
| **Housekeeping** | - | R, U (Status) | C, R, U | R |
| **Maintenance** | C, R, U | R, U (Status) | R, U (Verify) | R |
| **Damage Report** | R (Dispute) | C, R | R, U (Approve) | R, U (Escalated Approve) |
| **Contract** | R | - | R | R |
| **Users / Employee** | R (Self) | R (Self) | C, R, U (Employee only) | C, R, U, D |

**Luật kiểm soát chéo (Segregation of Duties):**
1. Manager **không được phép** thanh toán thay khách nếu không đính kèm PaymentReceipt hợp lệ.
2. Manager phê duyệt Damage Fee **phải** cho phép Customer thời gian phản hồi (Dispute). Mọi Dispute sẽ được leo thang lên Admin.

---

## 7. Error Handling

* `BOOKING_HELD_TIMEOUT`: Hết thời gian giữ phòng do không thanh toán đặt cọc.
* `ROOM_ALREADY_BOOKED`: Xung đột phòng (Overbooking phát hiện bởi DB Lock).
* `UNAUTHORIZED_PROPERTY_ACCESS`: Manager hoặc Employee cố gắng truy cập/chỉnh sửa dữ liệu của Property không được gán.
* `CHECKIN_DENIED_UNPAID`: Manager từ chối Check-in do khách chưa thanh toán đủ phần còn lại.
* `PAYMENT_RECONCILIATION_MISMATCH`: Khớp lệch số liệu khi đối soát với VNPay.

### Authentication Errors

* Invalid email or password.
* Account not verified.
* Account suspended.
* Account uses an external provider (Google) and cannot use password login.
* OTP expired or invalid.
* Session expired or invalid.

### Validation Errors

* Required field missing.
* Invalid email format.
* Invalid phone number.
* Invalid date range.
* Check-in date must be before check-out date.
* Check-in date cannot be in the past.
* Guest count exceeds room capacity.
* Selected dates are not available.

### Business Errors

* Room is not available for selected dates.
* Room has overlapping booking for requested date range.
* Booking already confirmed.
* Deposit payment already completed.
* Duplicate booking for same room and date range.
* Contract already generated.
* Booking cannot be cancelled after check-in has started.
* Customer-initiated cancellation refunds deposit by tier (≥7d 100% / 3–7d 50% / &lt;3d 0%); Manager-initiated cancellation refunds 100% of deposit.
* cancelReason is required for Manager-initiated cancellation.
* Manager cannot cancel a booking outside assigned property.
* ~~Only PENDING bank-transfer payments can be verified~~ — **N/A edu (TD-011 VNPay-only)**.
* Review can only be submitted after Booking status is Checked-out.
* Booking already reviewed.
* Cannot delete room with active bookings.
* Maintenance ticket requires an active booking.
* Only OPEN maintenance tickets can be edited or deleted.
* Manager không được truy cập Property không được gán.
* Employee không được truy cập Property không thuộc quyền.
* Employee không được thực hiện tác vụ không được gán.
* Room không thể chuyển sang Available khi tác vụ Housekeeping chưa hoàn thành.
* Housekeeping task chỉ được gán cho Employee thuộc cùng Property.
* Maintenance ticket chỉ được gán cho Employee thuộc cùng Property.
* Manager không thể gán Employee thuộc Property khác.
* Mỗi Employee chỉ có một assignment ACTIVE tại một thời điểm.
* Mỗi Property chỉ có một Manager assignment ACTIVE tại một thời điểm.
* Check-out không thể thực hiện khi Room Inspection chưa hoàn tất.
* Check-out không thể thực hiện khi còn khoản thanh toán chưa hoàn thành (bao gồm Damage Fee).
* Damage Fee chỉ được tạo khi Manager phê duyệt Damage Report.
* Employee chỉ được kiểm tra phòng trong Property được gán.
* Mỗi Booking chỉ có một RoomInspection duy nhất.

### System Errors

* Database connection failure.
* File upload failure.
* PDF generation failure.
* Email sending failure.
* Payment processing failure (e.g., VNPay URL creation or signature verification failed).
* Unexpected server error.

### Error Response

* Display user-friendly messages.
* Log technical errors for administrators.
* Prevent system crash.

---

## 8. Acceptance Criteria

* **Booking Creation**: Hệ thống không bao giờ cho phép 2 Customer đặt cùng 1 phòng, cùng 1 ngày (Zero Overbooking).
* **VNPay Integration**: Hệ thống có khả năng tự động khôi phục và đối soát trạng thái đơn hàng bị lỗi IPN qua Cron Job.
* **Damage Flow**: Customer nhận được Email cảnh báo khi Damage Report được tạo và có quyền bấm "Dispute" trên giao diện Customer Dashboard.

### User Management

* Users can register and verify accounts successfully.
* Users can login using email/password or Google authentication.
* Users can login and logout successfully.
* Hệ thống phân quyền chính xác theo vai trò (Admin, Manager, Employee, Customer, Guest).

### Admin Management

* Admin có thể tạo, cập nhật, kích hoạt/đình chỉ tài khoản Manager.
* Admin có thể gán Manager vào Property thông qua ManagerPropertyAssignment.
* Mỗi Property chỉ có một Manager assignment ACTIVE tại một thời điểm.
* Admin có thể tạo, chỉnh sửa, kích hoạt/vô hiệu hóa Property.

### Room Management

* Managers can create and update rooms with correct Floor hierarchy (chỉ trong Property được gán).
* Managers can delete rooms that do not have active bookings.
* Guests can search and view rooms successfully với hỗ trợ gợi ý tìm kiếm.
* Availability Calendar displays correct room statuses: Available, Pending Deposit, Reserved, Occupied, Pending Cleaning, Cleaning In Progress, Maintenance, Out Of Service.
* Employee có thể xem danh sách phòng trong Property được gán.

### Booking

| Attribute      | Type     | Description           |
| -------------- | -------- | --------------------- |
| Id             | UUID     | Booking identifier    |
| CustomerId     | UUID     | Customer              |
| RoomId         | UUID     | Booked room           |
| CheckInDate    | Date     | Check-in date         |
| CheckOutDate   | Date     | Check-out date        |
| GuestCount     | Integer  | Number of guests      |
| TotalAmount    | Decimal  | Snapshot total amount |
| DepositAmount  | Decimal  | Deposit amount (40%)  |
| RemainingAmount| Decimal  | Remaining balance     |
| DamageFeeAmount| Decimal  | Approved damage fee   |
| SpecialRequests| Text     | Special requests note |
| Status         | Enum     | Booking status        |
| HoldExpiresAt  | DateTime | Timeout giữ phòng     |
| RowVersion     | Integer  | Optimistic locking    |
| CancelledBy    | UUID     | Ai hủy phòng          |
| CancelReason   | Text     | Lý do hủy phòng       |
| CancelledAt    | DateTime | Thời gian hủy         |
| CreatedAt      | DateTime | Submission date       |
| UpdatedAt      | DateTime | Last update date      |

### Values

* Pending Deposit
* Confirmed
* Checked-in
* Pending Inspection
* Pending Damage Payment
* Checked-out
* Cancelled
* **No-show**

---

## Contract Management

* Accommodation Contracts are generated automatically after deposit payment.
* Contracts can be viewed, downloaded as PDF, printed, and resent via email.

### Payment

* Deposit payments, remaining balance payments, và Damage Fee payments được ghi nhận chính xác.
* Lịch sử thanh toán được lưu vết chi tiết bao gồm người xác nhận và thời gian xác nhận.
* Payment status is tracked and updated accurately.
* Damage Fee Payment được tự động tạo khi Manager phê duyệt Damage Report.

### Structure Management

* Managers can manage Property → Floor → Room hierarchy (chỉ trong Property được gán).
* Property Selector and Structure Tree View work correctly (Manager chỉ thấy Property được gán).

### Employee Management

* Admin hoặc Manager có thể gán Employee vào Property thông qua EmployeePropertyAssignment.
* Mỗi Employee chỉ có một assignment ACTIVE tại một thời điểm.
* Employee chỉ xem được dữ liệu thuộc Property được gán.
* Manager chỉ quản lý Employee trong Property được gán.

### Housekeeping

* Khi Booking chuyển sang Checked-out (sau Room Inspection và thanh toán hoàn tất), hệ thống tự động tạo HousekeepingTask và chuyển trạng thái phòng sang Pending Cleaning.
* Manager gán Employee thực hiện dọn phòng.
* Employee bắt đầu dọn phòng → Trạng thái phòng chuyển sang Cleaning In Progress.
* Employee hoàn thành dọn phòng → Trạng thái phòng tự động chuyển sang Available.
* Room không thể chuyển sang Available trước khi Housekeeping hoàn thành.
* Housekeeping task chỉ được gán cho Employee thuộc cùng Property.

### Maintenance

* Maintenance requests (kèm hình ảnh và thông tin booking) can be created by Customers.
* Customer có thể chỉnh sửa hoặc xóa yêu cầu bảo trì khi yêu cầu chưa được xử lý (trạng thái Open).
* Manager gán Employee xử lý yêu cầu bảo trì.
* Employee thực hiện bảo trì và cập nhật trạng thái.
* Manager xác nhận hoàn thành bảo trì.
* Quy trình: Open → Assigned → In Progress → Resolved → Closed.

### Review and Feedback

* Customers can leave one review per completed booking.
* Customers có thể chỉnh sửa hoặc xóa đánh giá của chính mình.
* Admin hoặc Manager can moderate and hide inappropriate reviews.
* Customers có thể tạo khiếu nại và theo dõi tiến độ giải quyết.

### Employee Dashboard

* Employee Dashboard hiển thị danh sách tác vụ Housekeeping được gán.
* Employee Dashboard hiển thị danh sách tác vụ Maintenance được gán.
* Employee Dashboard hiển thị tác vụ trong ngày hôm nay.
* Employee Dashboard hiển thị tác vụ đã hoàn thành và đang chờ xử lý.
* Employee Dashboard hiển thị danh sách Room Inspection được gán.

### Room Inspection

* Customer không thể Check-out trước khi Room Inspection hoàn tất.
* Employee chỉ được kiểm tra phòng trong Property được gán.
* Nếu phát hiện hư hại: Employee ghi nhận DamageItem và hệ thống tạo DamageReport.
* Manager phê duyệt tất cả bồi thường hư hại (Approved Amount).
* Damage Fee được tự động cộng vào Remaining Balance của Booking.
* Check-out chỉ hoàn tất khi tất cả thanh toán (bao gồm Damage Fee) được hoàn thành.
* Room chuyển sang Pending Cleaning chỉ sau khi Check-out thành công.

### Reporting

* Admin Dashboard displays correct KPIs: Total Properties, Total Floors, Total Rooms, Available Rooms, Occupied Rooms.
* Admin: Reports display accurate Revenue Report by period and property (toàn hệ thống).
* Manager: Reports display accurate Revenue Report by period (chỉ Property được gán).

### Administration

* Admin can manage user accounts (Active/Suspended) and complaints.
* Admin can configure system settings (deposit percentage, banking information).
* Admin can manage promotional banners hiển thị trên trang chủ.

### Property-level Authorization

* Manager chỉ truy cập dữ liệu thuộc Property được gán.
* Employee chỉ truy cập dữ liệu thuộc Property được gán.
* Customer chỉ truy cập dữ liệu Booking cá nhân.
* Guest chỉ xem thông tin công khai.

---

## 9. Out of Scope

Các chức năng sau không nằm trong phạm vi của phiên bản hiện tại:

* Tích hợp chữ ký số cho hợp đồng.
* Tích hợp cổng thanh toán quốc tế (ngoài cổng thanh toán nội địa VNPay đã hỗ trợ).
* Ứng dụng mobile native (Android/iOS).
* AI đề xuất phòng homestay/resort.
* Chat trực tiếp giữa Customer và Manager.
* Tích hợp bản đồ thời gian thực.
* Hệ thống kế toán doanh nghiệp.
* Hỗ trợ đa ngôn ngữ.
* Hệ thống marketing tự động (ngoài quản lý banner cơ bản).
* Quản lý nhiều chi nhánh doanh nghiệp lớn.

