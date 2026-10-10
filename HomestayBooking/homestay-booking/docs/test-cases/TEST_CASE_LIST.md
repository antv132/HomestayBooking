# TEST CASE LIST — Homestay Booking System

| Field | Value |
|---|---|
| **Project Name** | Homestay Booking System |
| **Project Code** | HomestayBooking |
| **Test Environment Setup Description** | See below |

### Test Environment Setup Description

```
1. Server: Backend Spring Boot 3.x (Java 17) – localhost:8080
2. Frontend: React 18 + Vite – localhost:5173
3. Database: Microsoft SQL Server – HomestayManagement (localhost:1433)
4. Web Browser: Google Chrome / Microsoft Edge (latest)
5. Tools: Postman, IntelliJ IDEA / VS Code, Maven, npm
6. Payment: VNPay Sandbox
7. Email: SMTP (.env) for OTP / verification
8. Test accounts: Admin, Manager, Employee, Customer (seeded in DB)
```

> Admin workflow intentionally omitted (team decision). Admin login may still appear inside Auth_Workflow for RBAC smoke only.

| No | Function Name | Sheet Name | Description | Pre-Condition |
|---:|---|---|---|---|
| 1 | User Login / Logout | Auth_Workflow | Đăng nhập theo role, đăng xuất, chặn user Suspended | Hệ thống chạy; DB có tài khoản test từng role |
| 2 | Register & OTP Verify | Auth_Workflow | Đăng ký tài khoản mới và xác thực OTP email | SMTP cấu hình đúng; email test nhận được OTP |
| 3 | Forgot / Reset Password | Auth_Workflow | Yêu cầu reset mật khẩu qua email và đặt mật khẩu mới | Tài khoản ACTIVE đã tồn tại |
| 4 | RBAC Access Control | Auth_Workflow | User sai role không vào được màn hình role khác | Đã đăng nhập từng role |
| 5 | Room Search & Detail | Booking_Workflow | Tìm phòng theo ngày/filter, xem chi tiết và lịch trống | Có Property/Room ACTIVE; có giá và ảnh |
| 6 | Create Booking & Hold | Booking_Workflow | Tạo booking → PENDING_DEPOSIT; hold timeout 10 phút | Customer đã login; phòng trống khoảng ngày chọn |
| 7 | Deposit Payment (VNPay) | Booking_Workflow | Thanh toán cọc 40% VNPay → CONFIRMED + contract | Booking PENDING_DEPOSIT còn trong hold |
| 8 | Customer Cancel & Refund Tier | Cancel_Refund_Workflow | Hủy trước check-in; hoàn cọc theo tier ≥7d / 3–7d / <3d | Booking CONFIRMED; đã thanh toán cọc |
| 9 | Manager Cancel Booking | Cancel_Refund_Workflow | Manager hủy (có cancelReason); hoàn 100% cọc; đúng property | Manager thuộc property của booking |
| 10 | Collect Remaining & Check-in | CheckInOut_Workflow | Thu remaining 60% rồi check-in kèm CMND | Booking CONFIRMED; đúng ngày check-in; staff đúng property |
| 11 | Inspection & Check-out | CheckInOut_Workflow | Check-out → inspection pass/fail; fail thì damage fee | Booking CHECKED_IN |
| 12 | Damage Fee Payment | Payment_Contract_Workflow | Thanh toán DAMAGE_FEE; fee >5M escalate Admin | Booking PENDING_DAMAGE_PAYMENT hoặc escalate |
| 13 | Contract View / PDF | Payment_Contract_Workflow | Xem/tải PDF hợp đồng sau khi cọc confirmed | Booking CONFIRMED; contract đã sinh |
| 14 | Payment History | Payment_Contract_Workflow | Customer xem lịch sử DEPOSIT/REMAINING/DAMAGE | Customer có ≥1 payment PAID |
| 15 | Room & Structure Management | Manager_Ops_Workflow | Manager quản lý Floor/Room trong property | Manager login; được gán property |
| 16 | Availability Calendar (Ops) | Manager_Ops_Workflow | Xem lịch phòng theo property | Có booking/block trong khoảng ngày |
| 17 | Property Scope Isolation | Manager_Ops_Workflow | Không truy cập resource ngoài property được gán | Có ≥2 property; user chỉ gán 1 property |
| 18 | Employee Dashboard | Employee_Ops_Workflow | KPI + todayTasks / awaiting / completedToday | Employee login; có task trong property |
| 19 | Housekeeping Tasks | Employee_Ops_Workflow | Xem/cập nhật task dọn phòng sau check-out | Có HK task sau checkout pass |
| 20 | Maintenance Ticket | Employee_Ops_Workflow | Tạo/xem ticket bảo trì trong scope property | Employee hoặc Customer đúng quyền |

### Workbook sheets (7)

1. `Auth_Workflow`
2. `Booking_Workflow`
3. `Cancel_Refund_Workflow`
4. `CheckInOut_Workflow`
5. `Payment_Contract_Workflow`
6. `Manager_Ops_Workflow`
7. `Employee_Ops_Workflow`

Chi tiết từng sheet: thư mục `docs/test-cases/sheets/`.

### Coverage per sheet

Mỗi workflow sheet có **32 test cases** (tổng **224 TC**). Copy bảng flat cuối mỗi file vào Excel.
