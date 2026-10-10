# Screen / Function Inventory — Homestay Booking System

**Project:** Homestay Booking System  
**Nguồn:** `docs/screen.md` — **67 SCR trong scope** (bỏ SCR-52, SCR-65). 1 dòng = 1 Screen ID.  
**Template columns:** Screen/Function · Feature · Actor · Screen/Function Description · In Charge · Status · Actual · Updated · Update Details  

> **In Charge:** Việt / Thành / Vân Anh / Huy / Hưng.  
> **Status:** `Done` = đã có trên FE; `To Do` = deferred / chưa làm; `Doing` / `Updated` dùng khi đang sửa.  
> **Actual / Updated:** gắn iteration thực tế (vd. `Sprint2`); `none` nếu chưa có update sau Actual.

---

## Phân công (In Charge)

| Thành viên | SCR |
|---|---|
| **Việt** | 28, 29, 30, 31, 32, 33, 42, 46, 47, 48, 60, 62 |
| **Thành** | 02, 03, 04, 05, 06, 20, 21, 26, 36, 37, 43, 53, 69 |
| **Vân Anh** | 01, 07, 08, 09, 10, 11, 12, 13, 14, 15, 17, 18, 19, 67, 68 |
| **Huy** | 16, 22, 23, 24, 25, 39, 41, 50, 51, 54, 61, 63, 64 |
| **Hưng** | 27, 34, 35, 38, 40, 44, 45, 49, 55, 56, 57, 58, 59, 66 |

**Tổng:** 67 SCR trong inventory. **Đã bỏ:** SCR-52 (Payment Reconciliation / TD-010), SCR-65 (Property Room List).

---

## Flat table (copy sang Excel)

| Screen/Function | Feature | Actor | Screen/Function Description | In Charge | Status | Actual | Updated | Update Details |
|---|---|---|---|---|---|---|---|---|
| Landing / Home Page | Common | Guest | Trang chủ, giới thiệu hệ thống và phòng nổi bật. | Vân Anh | Done | Sprint2 | none | |
| Login | Auth | Guest | Đăng nhập bằng email + password. | Thành | Done | Sprint2 | none | |
| Register | Auth | Guest | Đăng ký tài khoản mới. | Thành | Done | Sprint2 | none | |
| OTP / Email Verification | Auth | Guest | Xác thực tài khoản bằng OTP gửi qua email. | Thành | Done | Sprint2 | none | |
| Forgot Password | Auth | Guest | Yêu cầu đặt lại mật khẩu qua email. | Thành | Done | Sprint2 | none | |
| Reset Password | Auth | Guest | Tạo mật khẩu mới sau khi xác nhận link reset. | Thành | Done | Sprint2 | none | |
| Room Search & Listing | Room Discovery | Guest, Customer | Gộp List & Search Results; filter parameters. | Vân Anh | Done | Sprint2 | none | |
| Room Detail | Room Discovery | Guest, Customer | Chi tiết phòng: hình ảnh, mô tả, tiện ích. | Vân Anh | Done | Sprint2 | none | |
| Availability Calendar | Room Discovery | Guest, Customer | Lịch full-size chọn ngày trống. | Vân Anh | Done | Sprint2 | none | |
| User Profile | Common | Customer, Employee, Manager, Admin | Xem thông tin hồ sơ cá nhân. | Vân Anh | Done | Sprint2 | none | |
| Edit Profile | Common | Customer, Employee, Manager, Admin | Cập nhật hồ sơ và liên hệ (Dedicated Page). | Vân Anh | Done | Sprint2 | none | |
| Change Password | Common | Customer, Employee, Manager, Admin | Đổi mật khẩu tài khoản (Dedicated Page). | Vân Anh | Done | Sprint2 | none | |
| Notification Center | Common | Customer, Employee, Manager, Admin | Danh sách thông báo. | Vân Anh | Done | Sprint2 | none | |
| Notification Detail | Common | Customer, Employee, Manager, Admin | Chi tiết một thông báo (Dedicated Page). | Vân Anh | Done | Sprint2 | none | |
| Customer Dashboard | Customer Ops | Customer | Tổng quan đặt phòng, check-in/out sắp tới. | Vân Anh | Done | Sprint2 | none | |
| Booking Checkout | Booking | Customer | Form đặt phòng mới (ngày, số khách, giá). | Huy | Done | Sprint2 | none | |
| Booking Management | Booking | Customer | Danh sách tất cả booking của khách. | Vân Anh | Done | Sprint2 | none | |
| Booking Detail | Booking | Customer | Chi tiết 1 booking kèm thanh toán cơ bản. | Vân Anh | Done | Sprint2 | none | |
| Booking Cancellation | Cancel / Refund | Customer | Hủy trước check-in; hoàn cọc theo tier (≥7d 100% / 3–7d 50% / &lt;3d 0%). Dedicated Page. | Vân Anh | Done | Sprint2 | none | |
| Order Review & Payment | Payment | Customer | Checkout Step 2: hóa đơn, chính sách, hold countdown; cọc chỉ VNPay (kèm return result). | Thành | Done | Sprint2 | none | |
| My Contract List | Contract | Customer | Danh sách hợp đồng; PDF Drawer + Contract Detail `/customer/contracts/:id`. | Thành | Done | Sprint2 | none | |
| Maintenance Ticket List | Maintenance | Customer | Danh sách ticket bảo trì đã gửi. | Huy | Done | Sprint2 | none | |
| Create Maintenance Ticket | Maintenance | Customer | Form tạo yêu cầu bảo trì kèm hình (Dedicated Page). | Huy | Done | Sprint2 | none | |
| My Reviews | Review | Customer | Danh sách đánh giá đã viết. | Huy | Done | Sprint2 | none | |
| Review & Rating | Review | Customer | Form viết đánh giá sau check-out (Dedicated Page). | Huy | Done | Sprint2 | none | |
| Payment History | Payment | Customer | Lịch sử dòng tiền Customer (DEPOSIT / REMAINING / DAMAGE_FEE). | Thành | Done | Sprint2 | none | |
| Manager Dashboard | Manager Ops | Manager | KPIs property được gán. | Hưng | Done | Sprint2 | none | |
| Structure Management | Property Structure | Manager | Tree Property → Floor; thêm/sửa tầng bằng Modal. | Việt | Done | Sprint2 | none | |
| Room Management List | Room Inventory | Manager | Danh sách phòng; tìm kiếm, lọc. | Việt | Done | Sprint2 | none | |
| Add Room | Room Inventory | Manager | Form tạo phòng mới (Dedicated Page). | Việt | Done | Sprint2 | none | |
| Edit Room | Room Inventory | Manager | Chỉnh sửa thông tin cơ bản phòng (Dedicated Page). | Việt | Done | Sprint2 | none | |
| Room Gallery Management | Room Inventory | Manager | Quản lý / sắp xếp hình ảnh phòng (Dedicated Page). | Việt | Done | Sprint2 | none | |
| Room Status Management | Room Inventory | Manager | Khóa phòng; trạng thái bảo trì/dọn dẹp theo ngày (Dedicated Page). | Việt | Done | Sprint2 | none | |
| Booking List | Booking Ops | Manager | Danh sách booking thuộc property quản lý. | Hưng | Done | Sprint2 | none | |
| Booking Detail | Booking Ops | Manager | Chi tiết booking, check-in/out, modify; nút Cancel → SCR-69. | Hưng | Done | Sprint2 | none | |
| Payment Management | Payment Ops | Manager | Danh sách giao dịch thanh toán (Table). | Thành | Done | Sprint2 | none | |
| Check-in / Check-out | Check-in / Check-out | Manager | Xác nhận CI/CO; CCCD; remaining 60% PAID (cash desk) trước khi hoàn tất check-in. | Thành | Done | Sprint2 | none | |
| Contract Management | Contract | Manager | Hợp đồng theo property; PDF Drawer; resend email. | Hưng | Done | Sprint2 | none | |
| Employee Management | Staff Ops | Manager | Danh sách NV; gán vào property bằng Modal. | Huy | Done | Sprint2 | none | |
| Housekeeping Tasks | Housekeeping | Manager | Quản lý dọn phòng; gán và cập nhật status qua Drawer. | Hưng | Done | Sprint2 | none | |
| Maintenance Tasks | Maintenance | Manager | Quản lý bảo trì; gán và cập nhật status qua Drawer. | Huy | Done | Sprint2 | none | |
| Inspection Management | Inspection | Manager | Kiểm tra trước check-out; gán/đổi Inspector; xem kết quả. | Việt | Done | Sprint2 | none | |
| Damage Report Management | Damage | Manager | Duyệt báo cáo hư hại; xem ảnh và xác nhận qua Drawer. | Thành | Done | Sprint2 | none | |
| Property Reports | Reports | Manager | Doanh thu / lấp đầy / xu hướng — 1 trang chia Tabs. | Hưng | Done | Sprint2 | none | |
| Admin Dashboard | Admin Ops | Admin | KPIs toàn hệ thống. | Hưng | Done | Sprint2 | none | |
| Property Management | Property Admin | Admin | Danh sách property. | Việt | Done | Sprint2 | none | |
| Create Property | Property Admin | Admin | Form tạo chi nhánh (Dedicated Page). | Việt | Done | Sprint2 | none | |
| Edit Property | Property Admin | Admin | Chỉnh sửa thông tin property (Dedicated Page). | Việt | Done | Sprint2 | none | |
| Manager Assignment | Property Admin | Admin | Chỉ định Manager cho property (Dedicated Page). | Hưng | Done | Sprint2 | none | |
| Manager Directory | User Admin | Admin | Tài khoản Manager toàn hệ thống; chi tiết qua Drawer. | Huy | Done | Sprint2 | none | |
| Customer Directory | User Admin | Admin | Tài khoản Customer; chi tiết qua Drawer. | Huy | Done | Sprint2 | none | |
| Damage Escalation | Damage | Admin | Báo cáo hư hại &gt; 5M; Approve/Reject qua Drawer. | Thành | Done | Sprint2 | none | |
| Complaint Management | Complaint | Admin | Xử lý khiếu nại; cập nhật kết quả qua Drawer. | Huy | Done | Sprint2 | none | |
| Global Reports | Reports | Admin | Báo cáo hệ thống (tabs tương tự Manager). | Hưng | Done | Sprint2 | none | |
| System Administration | System | Admin | Activity Logs / System Settings / Content Moderation — Tabs. | Hưng | Done | Sprint2 | none | |
| Promotion Management | Promotion | Admin | Quản lý banner khuyến mãi. | Hưng | Done | Sprint2 | none | |
| Add / Edit Promotion | Promotion | Admin | Form tạo/sửa banner, thời gian và discount (Dedicated Page). | Hưng | Done | Sprint2 | none | |
| Employee Dashboard | Employee Ops | Employee | Tổng quan nhiệm vụ trong ngày (KPI + todayTasks / awaiting / completedToday). | Hưng | Done | Sprint2 | none | |
| Housekeeping Workspace | Housekeeping | Employee | Tác vụ dọn phòng; hoàn thành qua Inline / Drawer. | Việt | Done | Sprint2 | none | |
| Maintenance Workspace | Maintenance | Employee | Tác vụ bảo trì; cập nhật tiến độ qua Inline / Drawer. | Huy | Done | Sprint2 | none | |
| Room Inspection Hub | Inspection | Employee | Inspection gán + Unassigned (Claim); Pass/Fail chỉ khi đã gán cho mình. | Việt | Done | Sprint2 | none | |
| Damage Report List | Damage | Employee | Danh sách báo cáo hư hại đã ghi nhận. | Huy | Done | Sprint2 | none | |
| Create Damage Report | Damage | Employee | Ghi nhận hư hại, ảnh, tính giá (Dedicated Page, mobile-friendly). | Huy | Done | Sprint2 | none | |
| Housekeeping Schedule | Housekeeping | Manager | Dashboard phân công dọn phòng (view-only hoặc drag-drop nhẹ). | Hưng | Done | Sprint2 | none | |
| My Complaint List | Complaint | Customer | Danh sách khiếu nại cá nhân kèm trạng thái và ghi chú. | Vân Anh | Done | Sprint2 | none | |
| Create Complaint | Complaint | Customer | Form gửi khiếu nại mới (Subject + Description) — Dedicated Page. | Vân Anh | Done | Sprint2 | none | |
| Manager Booking Cancellation | Cancel / Refund | Manager | Hủy booking (Manager): bắt buộc lý do; hoàn 100% cọc. Dedicated Page. | Thành | Done | Sprint2 | none | |

---

## CSV (paste vào Excel / Google Sheets)

```csv
Screen/Function,Feature,Actor,Screen/Function Description,In Charge,Status,Actual,Updated,Update Details
Landing / Home Page,Common,Guest,"Trang chủ, giới thiệu hệ thống và phòng nổi bật.",Vân Anh,Done,Sprint2,none,
Login,Auth,Guest,Đăng nhập bằng email + password.,Thành,Done,Sprint2,none,
Register,Auth,Guest,Đăng ký tài khoản mới.,Thành,Done,Sprint2,none,
OTP / Email Verification,Auth,Guest,Xác thực tài khoản bằng OTP gửi qua email.,Thành,Done,Sprint2,none,
Forgot Password,Auth,Guest,Yêu cầu đặt lại mật khẩu qua email.,Thành,Done,Sprint2,none,
Reset Password,Auth,Guest,Tạo mật khẩu mới sau khi xác nhận link reset.,Thành,Done,Sprint2,none,
Room Search & Listing,Room Discovery,"Guest, Customer",Gộp List & Search Results; filter parameters.,Vân Anh,Done,Sprint2,none,
Room Detail,Room Discovery,"Guest, Customer","Chi tiết phòng: hình ảnh, mô tả, tiện ích.",Vân Anh,Done,Sprint2,none,
Availability Calendar,Room Discovery,"Guest, Customer",Lịch full-size chọn ngày trống.,Vân Anh,Done,Sprint2,none,
User Profile,Common,"Customer, Employee, Manager, Admin",Xem thông tin hồ sơ cá nhân.,Vân Anh,Done,Sprint2,none,
Edit Profile,Common,"Customer, Employee, Manager, Admin",Cập nhật hồ sơ và liên hệ (Dedicated Page).,Vân Anh,Done,Sprint2,none,
Change Password,Common,"Customer, Employee, Manager, Admin",Đổi mật khẩu tài khoản (Dedicated Page).,Vân Anh,Done,Sprint2,none,
Notification Center,Common,"Customer, Employee, Manager, Admin",Danh sách thông báo.,Vân Anh,Done,Sprint2,none,
Notification Detail,Common,"Customer, Employee, Manager, Admin",Chi tiết một thông báo (Dedicated Page).,Vân Anh,Done,Sprint2,none,
Customer Dashboard,Customer Ops,Customer,"Tổng quan đặt phòng, check-in/out sắp tới.",Vân Anh,Done,Sprint2,none,
Booking Checkout,Booking,Customer,"Form đặt phòng mới (ngày, số khách, giá).",Huy,Done,Sprint2,none,
Booking Management,Booking,Customer,Danh sách tất cả booking của khách.,Vân Anh,Done,Sprint2,none,
Booking Detail,Booking,Customer,Chi tiết 1 booking kèm thanh toán cơ bản.,Vân Anh,Done,Sprint2,none,
Booking Cancellation,Cancel / Refund,Customer,"Hủy trước check-in; hoàn cọc theo tier (≥7d 100% / 3–7d 50% / <3d 0%). Dedicated Page.",Vân Anh,Done,Sprint2,none,
Order Review & Payment,Payment,Customer,"Checkout Step 2: hóa đơn, chính sách, hold countdown; cọc chỉ VNPay (kèm return result).",Thành,Done,Sprint2,none,
My Contract List,Contract,Customer,Danh sách hợp đồng; PDF Drawer + Contract Detail /customer/contracts/:id.,Thành,Done,Sprint2,none,
Maintenance Ticket List,Maintenance,Customer,Danh sách ticket bảo trì đã gửi.,Huy,Done,Sprint2,none,
Create Maintenance Ticket,Maintenance,Customer,Form tạo yêu cầu bảo trì kèm hình (Dedicated Page).,Huy,Done,Sprint2,none,
My Reviews,Review,Customer,Danh sách đánh giá đã viết.,Huy,Done,Sprint2,none,
Review & Rating,Review,Customer,Form viết đánh giá sau check-out (Dedicated Page).,Huy,Done,Sprint2,none,
Payment History,Payment,Customer,Lịch sử dòng tiền Customer (DEPOSIT / REMAINING / DAMAGE_FEE).,Thành,Done,Sprint2,none,
Manager Dashboard,Manager Ops,Manager,KPIs property được gán.,Hưng,Done,Sprint2,none,
Structure Management,Property Structure,Manager,Tree Property → Floor; thêm/sửa tầng bằng Modal.,Việt,Done,Sprint2,none,
Room Management List,Room Inventory,Manager,"Danh sách phòng; tìm kiếm, lọc.",Việt,Done,Sprint2,none,
Add Room,Room Inventory,Manager,Form tạo phòng mới (Dedicated Page).,Việt,Done,Sprint2,none,
Edit Room,Room Inventory,Manager,Chỉnh sửa thông tin cơ bản phòng (Dedicated Page).,Việt,Done,Sprint2,none,
Room Gallery Management,Room Inventory,Manager,Quản lý / sắp xếp hình ảnh phòng (Dedicated Page).,Việt,Done,Sprint2,none,
Room Status Management,Room Inventory,Manager,Khóa phòng; trạng thái bảo trì/dọn dẹp theo ngày (Dedicated Page).,Việt,Done,Sprint2,none,
Booking List,Booking Ops,Manager,Danh sách booking thuộc property quản lý.,Hưng,Done,Sprint2,none,
Booking Detail,Booking Ops,Manager,"Chi tiết booking, check-in/out, modify; nút Cancel → SCR-69.",Hưng,Done,Sprint2,none,
Payment Management,Payment Ops,Manager,Danh sách giao dịch thanh toán (Table).,Thành,Done,Sprint2,none,
Check-in / Check-out,Check-in / Check-out,Manager,"Xác nhận CI/CO; CCCD; remaining 60% PAID (cash desk) trước khi hoàn tất check-in.",Thành,Done,Sprint2,none,
Contract Management,Contract,Manager,Hợp đồng theo property; PDF Drawer; resend email.,Hưng,Done,Sprint2,none,
Employee Management,Staff Ops,Manager,Danh sách NV; gán vào property bằng Modal.,Huy,Done,Sprint2,none,
Housekeeping Tasks,Housekeeping,Manager,Quản lý dọn phòng; gán và cập nhật status qua Drawer.,Hưng,Done,Sprint2,none,
Maintenance Tasks,Maintenance,Manager,Quản lý bảo trì; gán và cập nhật status qua Drawer.,Huy,Done,Sprint2,none,
Inspection Management,Inspection,Manager,"Kiểm tra trước check-out; gán/đổi Inspector; xem kết quả.",Việt,Done,Sprint2,none,
Damage Report Management,Damage,Manager,Duyệt báo cáo hư hại; xem ảnh và xác nhận qua Drawer.,Thành,Done,Sprint2,none,
Property Reports,Reports,Manager,Doanh thu / lấp đầy / xu hướng — 1 trang chia Tabs.,Hưng,Done,Sprint2,none,
Admin Dashboard,Admin Ops,Admin,KPIs toàn hệ thống.,Hưng,Done,Sprint2,none,
Property Management,Property Admin,Admin,Danh sách property.,Việt,Done,Sprint2,none,
Create Property,Property Admin,Admin,Form tạo chi nhánh (Dedicated Page).,Việt,Done,Sprint2,none,
Edit Property,Property Admin,Admin,Chỉnh sửa thông tin property (Dedicated Page).,Việt,Done,Sprint2,none,
Manager Assignment,Property Admin,Admin,Chỉ định Manager cho property (Dedicated Page).,Hưng,Done,Sprint2,none,
Manager Directory,User Admin,Admin,Tài khoản Manager toàn hệ thống; chi tiết qua Drawer.,Huy,Done,Sprint2,none,
Customer Directory,User Admin,Admin,Tài khoản Customer; chi tiết qua Drawer.,Huy,Done,Sprint2,none,
Damage Escalation,Damage,Admin,Báo cáo hư hại > 5M; Approve/Reject qua Drawer.,Thành,Done,Sprint2,none,
Complaint Management,Complaint,Admin,Xử lý khiếu nại; cập nhật kết quả qua Drawer.,Huy,Done,Sprint2,none,
Global Reports,Reports,Admin,Báo cáo hệ thống (tabs tương tự Manager).,Hưng,Done,Sprint2,none,
System Administration,System,Admin,Activity Logs / System Settings / Content Moderation — Tabs.,Hưng,Done,Sprint2,none,
Promotion Management,Promotion,Admin,Quản lý banner khuyến mãi.,Hưng,Done,Sprint2,none,
Add / Edit Promotion,Promotion,Admin,"Form tạo/sửa banner, thời gian và discount (Dedicated Page).",Hưng,Done,Sprint2,none,
Employee Dashboard,Employee Ops,Employee,Tổng quan nhiệm vụ trong ngày (KPI + todayTasks / awaiting / completedToday).,Hưng,Done,Sprint2,none,
Housekeeping Workspace,Housekeeping,Employee,Tác vụ dọn phòng; hoàn thành qua Inline / Drawer.,Việt,Done,Sprint2,none,
Maintenance Workspace,Maintenance,Employee,Tác vụ bảo trì; cập nhật tiến độ qua Inline / Drawer.,Huy,Done,Sprint2,none,
Room Inspection Hub,Inspection,Employee,Inspection gán + Unassigned (Claim); Pass/Fail chỉ khi đã gán cho mình.,Việt,Done,Sprint2,none,
Damage Report List,Damage,Employee,Danh sách báo cáo hư hại đã ghi nhận.,Huy,Done,Sprint2,none,
Create Damage Report,Damage,Employee,"Ghi nhận hư hại, ảnh, tính giá (Dedicated Page, mobile-friendly).",Huy,Done,Sprint2,none,
Housekeeping Schedule,Housekeeping,Manager,Dashboard phân công dọn phòng (view-only hoặc drag-drop nhẹ).,Hưng,Done,Sprint2,none,
My Complaint List,Complaint,Customer,Danh sách khiếu nại cá nhân kèm trạng thái và ghi chú.,Vân Anh,Done,Sprint2,none,
Create Complaint,Complaint,Customer,Form gửi khiếu nại mới (Subject + Description) — Dedicated Page.,Vân Anh,Done,Sprint2,none,
Manager Booking Cancellation,Cancel / Refund,Manager,Hủy booking (Manager): bắt buộc lý do; hoàn 100% cọc. Dedicated Page.,Thành,Done,Sprint2,none,
```
