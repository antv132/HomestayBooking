# 📋 Screen Catalog
## Homestay / Resort Booking Management System

---

## 🗂️ Overview

| #  | Role     | Screen Range              | Total |
|----|----------|---------------------------|-------|
| 1  | Guest    | SCR-01 – SCR-09           | 9     |
| 2  | Shared   | SCR-10 – SCR-14           | 5     |
| 3  | Customer | SCR-15 – SCR-26, SCR-67 – SCR-68 | 14 |
| 4  | Manager  | SCR-27 – SCR-44, SCR-66, SCR-69 | 20 |
| 5  | Admin    | SCR-45 – SCR-58           | 14    |
| 6  | Employee | SCR-59 – SCR-65           | 7     |
|    | **Total**|                           | **69**|

*(SCR-66 tách Housekeeping Schedule khỏi dải 27–44 để SCR-38 dành cho Contract Management)*
*(SCR-67 – SCR-68 bổ sung luồng khiếu nại phía Customer, đánh số nối tiếp để không phá numbering hiện có)*
*(SCR-69 bổ sung Manager Booking Cancellation — Dedicated Page, tương tự SCR-19 phía Customer)*

---

## 🌐 Section 1 — Guest / Public Screens

| Screen ID | Screen Name              | Actor           | Purpose / UI Pattern                                         |
|-----------|--------------------------|-----------------|--------------------------------------------------------------|
| SCR-01    | Landing / Home Page      | Guest           | Trang chủ, giới thiệu hệ thống và phòng nổi bật             |
| SCR-02    | Login                    | Guest           | Đăng nhập bằng email + password                             |
| SCR-03    | Register                 | Guest           | Đăng ký tài khoản mới                                       |
| SCR-04    | OTP / Email Verification | Guest           | Xác thực tài khoản bằng OTP gửi qua email                  |
| SCR-05    | Forgot Password          | Guest           | Yêu cầu đặt lại mật khẩu qua email                         |
| SCR-06    | Reset Password           | Guest           | Tạo mật khẩu mới sau khi xác nhận link reset               |
| SCR-07    | Room Search & Listing    | Guest, Customer | Gộp chung List & Search Results. Dùng filter parameters.   |
| SCR-08    | Room Detail              | Guest, Customer | Xem chi tiết phòng: hình ảnh, mô tả, tiện ích.               |
| SCR-09    | Availability Calendar    | Guest, Customer | Giao diện lịch full-size để khách chọn ngày trống.           |

---

## 👤 Section 2 — Shared Screens (All Authenticated Users)

| Screen ID | Screen Name         | Actor                              | Purpose / UI Pattern                                         |
|-----------|---------------------|------------------------------------|--------------------------------------------------------------|
| SCR-10    | User Profile        | Customer, Employee, Manager, Admin | Xem thông tin hồ sơ cá nhân.                                 |
| SCR-11    | Edit Profile        | Customer, Employee, Manager, Admin | Cập nhật hồ sơ và thông tin liên hệ (Dedicated Page).        |
| SCR-12    | Change Password     | Customer, Employee, Manager, Admin | Đổi mật khẩu tài khoản (Dedicated Page).                     |
| SCR-13    | Notification Center | Customer, Employee, Manager, Admin | Xem danh sách thông báo.                                     |
| SCR-14    | Notification Detail | Customer, Employee, Manager, Admin | Xem nội dung chi tiết của thông báo (Dedicated Page).        |

---

## 🛎️ Section 3 — Customer Screens

| Screen ID | Screen Name             | Purpose / UI Pattern                                                                 |
|-----------|-------------------------|--------------------------------------------------------------------------------------|
| SCR-15    | Customer Dashboard      | Tổng quan đặt phòng, check-in/out sắp tới.                                           |
| SCR-16    | Booking Checkout        | Luồng form đặt phòng mới (Check-in/out, số khách, giá).                              |
| SCR-17    | Booking Management      | Danh sách tất cả booking của khách hàng.                                             |
| SCR-18    | Booking Detail          | Màn hình chi tiết 1 booking (kèm thông tin thanh toán cơ bản).                       |
| SCR-19    | Booking Cancellation    | Hủy booking trước check-in; hoàn **cọc theo tier** (≥7d 100% / 3–7d 50% / &lt;3d 0%). Dedicated Page. |
| SCR-20    | Order Review & Payment  | Checkout Step 2: xem lại hóa đơn, chính sách, đếm ngược hold 10p; thanh toán cọc **chỉ VNPay**. |
| SCR-21    | My Contract List        | Danh sách hợp đồng. PDF qua Drawer; có trang Contract Detail (`/customer/contracts/:id`). |
| SCR-22    | Maintenance Ticket List | Danh sách ticket bảo trì đã gửi.                                                     |
| SCR-23    | Create Maintenance Ticket| Form tạo yêu cầu bảo trì kèm hình ảnh (Dedicated Page).                             |
| SCR-24    | My Reviews              | Danh sách đánh giá đã viết.                                                          |
| SCR-25    | Review & Rating         | Form viết đánh giá sau check-out (Dedicated Page để user tập trung).                 |
| SCR-26    | Payment History         | Xem tổng quan lịch sử dòng tiền của Customer.                                        |
| SCR-67    | My Complaint List       | Danh sách khiếu nại cá nhân kèm trạng thái xử lý và ghi chú giải quyết.              |
| SCR-68    | Create Complaint        | Form gửi khiếu nại mới (Subject + Description) — Dedicated Page.                     |

---

## 🏢 Section 4 — Manager Screens

| Screen ID | Screen Name             | Purpose / UI Pattern                                                                 |
|-----------|-------------------------|--------------------------------------------------------------------------------------|
| SCR-27    | Manager Dashboard       | KPIs Property được gán.                                                              |
| SCR-28    | Structure Management    | Xem dạng Tree (Property → Floor). Thêm/Sửa tầng bằng Modal.                          |
| SCR-29    | Room Management List    | Danh sách phòng. Tìm kiếm, lọc.                                                      |
| SCR-30    | Add Room                | Form tạo phòng mới với cấu hình phức tạp (Dedicated Page).                           |
| SCR-31    | Edit Room               | Chỉnh sửa thông tin cơ bản của phòng (Dedicated Page).                               |
| SCR-32    | Room Gallery Management | Quản lý, sắp xếp hình ảnh của phòng (Dedicated Page).                                |
| SCR-33    | Room Status Management  | Khóa phòng, cập nhật trạng thái bảo trì/dọn dẹp (Dedicated Page).                    |
| SCR-34    | Booking List            | Danh sách Booking thuộc Property quản lý.                                            |
| SCR-35    | Booking Detail          | Xem chi tiết Booking, Check-in/out, Approval; nút Cancel → SCR-69.                   |
| SCR-69    | Manager Booking Cancellation | Hủy booking (Manager): bắt buộc lý do; hoàn **100% cọc**.                         |
| SCR-36    | Payment Management      | Xem danh sách giao dịch (Table).                                                     |
| SCR-37    | Check-in / Check-out    | Xác nhận Check-in/out; CCCD; chìa khóa; **Remaining 60% phải PAID** trước khi hoàn tất Check-in. |
| SCR-38    | Contract Management     | Danh sách hợp đồng theo Property. Xem PDF Drawer; Resend email.                      |
| SCR-39    | Employee Management     | Danh sách nhân viên. Gán nhân viên vào Property bằng Modal.                          |
| SCR-40    | Housekeeping Tasks      | Quản lý dọn phòng. Gán và cập nhật trạng thái qua Drawer.                            |
| SCR-41    | Maintenance Tasks       | Quản lý bảo trì. Gán và cập nhật trạng thái qua Drawer.                              |
|  SCR-42   | Inspection Management   | Kiểm tra trước Check-out. Gán / đổi Inspector qua Drawer; xem kết quả.               |
| SCR-43    | Damage Report Management| Duyệt báo cáo hư hại. Xem hình ảnh và xác nhận qua Drawer.                           |
| SCR-44    | Property Reports        | Gộp Doanh thu, Tỷ lệ lấp đầy, Xu hướng thành 1 trang chia theo Tabs.                 |
| SCR-66    | Housekeeping Schedule   | Dashboard phân công dọn phòng (View-only hoặc Drag-drop nhẹ).                        |

---

## 🔑 Section 5 — Admin Screens

| Screen ID | Screen Name             | Purpose / UI Pattern                                                                 |
|-----------|-------------------------|--------------------------------------------------------------------------------------|
| SCR-45    | Admin Dashboard         | KPIs toàn hệ thống.                                                                  |
| SCR-46    | Property Management     | Danh sách Property.                                                                  |
| SCR-47    | Create Property         | Form tạo chi nhánh dài (địa chỉ, map, facilities, contact) -> Dedicated Page.        |
| SCR-48    | Edit Property           | Chỉnh sửa thông tin Property (Dedicated Page).                                       |
| SCR-49    | Manager Assignment      | Chỉ định Manager quản lý Property (Dedicated Page).                                  |
| SCR-50    | Manager Directory       | Quản lý tài khoản Manager toàn hệ thống. Xem chi tiết qua Drawer.                    |
| SCR-51    | Customer Directory      | Quản lý tài khoản Customer. Xem chi tiết qua Drawer.                                 |
| SCR-52    | Payment Reconciliation  | **Deferred edu (TD-010)** — VNPay discrepancy / Sync. Không bắt buộc khóa học.       |
| SCR-53    | Damage Escalation       | Báo cáo hư hại > 5M cần duyệt. Xem note và Approve/Reject qua Drawer.                |
| SCR-54    | Complaint Management    | Xử lý khiếu nại. Cập nhật kết quả qua Drawer.                                        |
| SCR-55    | Global Reports          | Báo cáo doanh thu, lấp đầy, xu hướng... chia thành các Tabs (tương tự Manager).      |
| SCR-56    | System Administration   | Gộp Activity Logs, System Settings, Content Moderation thành Tabs.                   |
| SCR-57    | Promotion Management    | Quản lý banner khuyến mãi.                                                           |
| SCR-58    | Add / Edit Promotion    | Form tạo banner, thiết lập thời gian và discount (Dedicated Page).                   |

---

## 👷 Section 6 — Employee Screens

| Screen ID | Screen Name             | Purpose / UI Pattern                                                                 |
|-----------|-------------------------|--------------------------------------------------------------------------------------|
| SCR-59    | Employee Dashboard      | Tổng quan nhanh nhiệm vụ trong ngày.                                                 |
| SCR-60    | Housekeeping Workspace  | Danh sách tác vụ dọn phòng. Đánh dấu hoàn thành qua Inline Action / Drawer.          |
| SCR-61    | Maintenance Workspace   | Danh sách tác vụ bảo trì. Cập nhật tiến độ qua Inline Action / Drawer.               |
| SCR-62    | Room Inspection Hub     | Inspection được gán + Unassigned (Claim). Pass/Fail chỉ khi đã gán cho mình.         |
| SCR-63    | Damage Report List      | Danh sách các báo cáo hư hại đã ghi nhận.                                            |
| SCR-64    | Create Damage Report    | Ghi nhận hư hại, chụp ảnh, tính giá (Dedicated Page tiện thao tác trên mobile).      |
| SCR-65    | Property Room List      | Danh sách phòng để tham khảo thông tin nhanh.                                        |
