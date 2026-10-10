# 🖥️ Screen Design Document (Wireframes & UI Mapping)
*Dựa trên Hệ thống Design Tokens: Modern Zen & Premium Hospitality (DESIGN.md)*

---

## 🌐 Section 1 — Guest / Public Screens

# SCR-01 – Landing / Home Page
## Purpose
Trang chủ, thu hút khách hàng, giới thiệu hệ thống và các phòng nổi bật. (Actor: Guest)
## Layout Wireframe
nav-bar (bg-surface-canvas, 80px)
  └── Logo | Nav Links | button-ghost "Log in" | button-primary "Sign up"
Hero Section (bg-primary-base, 600px, bg-image-blend)
  └── text-display-lg "Find Your Zen" | Search Form (Location, Dates, Guests, button-primary "Search")
Featured Properties (bg-surface-canvas, padding space-8)
  └── text-heading-lg "Popular Destinations"
  └── Grid (3 columns)
       └── RoomCard (Image, Title, Price, StatusBadge)
Footer (bg-surface-inverted, text-inverted, padding space-8)
## Components & Tokens
- Navbar: `color-surface-canvas`, `color-text-primary`
- Hero: `color-primary-base`, `text-inverted`
- RoomCard: `color-surface-card`, `radius-lg`, `shadow-md`, `hover:shadow-lg`
- Typography: `text-display-lg`, `text-heading-lg`
- Button: `button-primary`, `button-ghost`
## Navigation & Actions
- Nút "Log in" → Chuyển tới SCR-02
- Submit Search Form → Chuyển tới SCR-07
- Click RoomCard → Chuyển tới SCR-08

# SCR-02 – Login
## Purpose
Đăng nhập vào hệ thống. (Actor: Guest)
## Layout Wireframe
Container (bg-surface-canvas, full-height, flex-center)
  └── Card (bg-surface-card, 400px width, padding space-6)
       └── text-heading-lg "Welcome Back"
       └── Input (Email) | Input (Password)
       └── Link "Forgot Password?"
       └── button-primary "Log In" (full-width)
       └── Text "Don't have an account? Sign up"
## Components & Tokens
- Background: `color-surface-canvas`
- Login Card: `color-surface-card`, `radius-lg`, `shadow-lg`
- Inputs: `border-color-border-base`, `radius-md`, focus: `border-color-primary-base`
- Button: `button-primary`, `radius-md`
## Navigation & Actions
- Nút "Log In" → Xác thực API → Chuyển tới SCR-15 (Customer) hoặc Dashboard tương ứng
- Link "Forgot Password?" → Chuyển tới SCR-05
- Link "Sign up" → Chuyển tới SCR-03

# SCR-03 – Register
## Purpose
Đăng ký tài khoản mới. (Actor: Guest)
## Layout Wireframe
Container (bg-surface-canvas, full-height, flex-center)
  └── Card (bg-surface-card, 450px width, padding space-6)
       └── text-heading-lg "Create an Account"
       └── Input (Full Name) | Input (Phone) | Input (Email) | Input (Password)
       └── button-primary "Sign Up" (full-width)
## Components & Tokens
- Register Card: `color-surface-card`, `radius-lg`, `shadow-lg`
- Inputs: `radius-md`, `border-color-border-base`
- Button: `button-primary`
## Navigation & Actions
- Nút "Sign Up" → Gọi API đăng ký → Chuyển tới SCR-04

# SCR-04 – OTP / Email Verification
## Purpose
Xác thực email đăng ký bằng mã OTP. (Actor: Guest)
## Layout Wireframe
Container (bg-surface-canvas, full-height, flex-center)
  └── Card (bg-surface-card, 400px width, padding space-6)
       └── text-heading-lg "Verify Your Email"
       └── text-body-sm "We sent a code to your email."
       └── 6-digit OTP Input Group
       └── button-primary "Verify"
       └── Link "Resend Code"
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`, `shadow-lg`
- OTP Inputs: `text-heading-md`, text-center, `radius-md`, focus:`border-color-primary-base`
## Navigation & Actions
- Nút "Verify" → Xác thực OTP API → Chuyển tới SCR-02 (Login)

# SCR-05 – Forgot Password
## Purpose
Yêu cầu cấp lại mật khẩu. (Actor: Guest)
## Layout Wireframe
Container (bg-surface-canvas, full-height, flex-center)
  └── Card (bg-surface-card, 400px width, padding space-6)
       └── text-heading-lg "Reset Password"
       └── text-body-sm "Enter your email to receive a reset link."
       └── Input (Email)
       └── button-primary "Send Link"
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`, `shadow-lg`
## Navigation & Actions
- Nút "Send Link" → Gọi API quên mật khẩu → Hiển thị Toast Success.

# SCR-06 – Reset Password
## Purpose
Tạo mật khẩu mới từ link email. (Actor: Guest)
## Layout Wireframe
Container (bg-surface-canvas, full-height, flex-center)
  └── Card (bg-surface-card, 400px width, padding space-6)
       └── text-heading-lg "New Password"
       └── Input (New Password) | Input (Confirm Password)
       └── button-primary "Update Password"
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`, `shadow-lg`
## Navigation & Actions
- Nút "Update Password" → Đổi mật khẩu API → Chuyển tới SCR-02

# SCR-07 – Room Search & Listing
## Purpose
Hiển thị danh sách phòng và kết quả tìm kiếm. (Actor: Guest, Customer)
## Layout Wireframe
nav-bar (color-surface-canvas)
Main Layout (2 columns)
  ├── Sidebar Left (bg-surface-card, 250px)
  │    └── text-heading-md "Filters" | Price Slider | Checkboxes (Amenities) | button-primary "Apply"
  └── Content Right (bg-surface-canvas)
       └── Top Bar: Search Input | Sort Dropdown
       └── Grid (3 columns)
            └── RoomCard | RoomCard | RoomCard ...
       └── Pagination
## Components & Tokens
- Sidebar: `color-surface-card`, `radius-lg`, `border-color-border-subtle`
- RoomCard: `color-surface-card`, `radius-lg`, `shadow-sm`, `hover:shadow-lg`
- Typography: `text-heading-md`, `text-body-sm`
## Navigation & Actions
- Click RoomCard → Chuyển tới SCR-08

# SCR-08 – Room Detail
## Purpose
Xem chi tiết một phòng cụ thể. (Actor: Guest, Customer)
## Layout Wireframe
nav-bar (color-surface-canvas)
Main Content (max-width: 1200px)
  └── Top: Image Gallery Grid (1 large hero image, 4 smaller thumbnails, radius-lg)
  └── Bottom 2-col:
       ├── Left (70%): text-heading-lg Title | text-body-base Description | Amenities List | Reviews summary
       └── Right (30%): Sticky Price Card (bg-surface-card, shadow-lg)
            └── text-heading-lg Price | Dates | button-primary "Book Now"
## Components & Tokens
- Image Gallery: `radius-lg`, overflow-hidden
- Sticky Card: `color-surface-card`, `radius-lg`, `shadow-lg`
- Button: `button-primary` full-width
## Navigation & Actions
- Nút "Book Now" → Yêu cầu login (nếu Guest) hoặc Chuyển tới SCR-16 (nếu Customer)
- Click View Calendar → Chuyển tới SCR-09

# SCR-09 – Availability Calendar
## Purpose
Xem lịch trống full-size của phòng. (Actor: Guest, Customer)
## Layout Wireframe
nav-bar (color-surface-canvas)
Main Content (max-width: 1000px, bg-surface-card, radius-lg, padding space-6)
  └── text-heading-lg "Availability"
  └── Full Calendar Component (Grid of months/days)
       └── Available dates: bg-surface-canvas
       └── Booked dates: bg-border-base, text-secondary, strikethrough
## Components & Tokens
- Calendar Container: `color-surface-card`, `radius-lg`, `shadow-sm`
- Typography: `text-heading-lg`
## Navigation & Actions
- Select Dates → Lưu state → Chuyển về SCR-08

---

## 👤 Section 2 — Shared Screens

# SCR-10 – User Profile
## Purpose
Xem thông tin tài khoản cá nhân. (Actor: All Authenticated)
## Layout Wireframe
Sidebar Layout (chung cho Dashboard các role)
Main Content (bg-surface-canvas)
  └── Card (bg-surface-card, padding space-6, radius-lg)
       └── Avatar (radius-full, 120px)
       └── text-heading-md Name | text-body-sm Email
       └── button-secondary "Edit Profile" | button-ghost "Change Password"
## Components & Tokens
- Profile Card: `color-surface-card`, `radius-lg`, `shadow-sm`
- Buttons: `button-secondary`, `button-ghost`
## Navigation & Actions
- Nút "Edit Profile" → Chuyển tới SCR-11
- Nút "Change Password" → Chuyển tới SCR-12

# SCR-11 – Edit Profile
## Purpose
Cập nhật thông tin cá nhân. (Actor: All Authenticated)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card)
       └── Form: Input (Full Name), Input (Phone), Input (Avatar Upload)
       └── button-primary "Save Changes" | button-ghost "Cancel"
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`
## Navigation & Actions
- Nút "Save Changes" → Gọi API → Toast Success → Quay lại SCR-10

# SCR-12 – Change Password
## Purpose
Đổi mật khẩu tài khoản. (Actor: All Authenticated)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card)
       └── Form: Input (Old Password), Input (New Password), Input (Confirm)
       └── button-primary "Update Password"
## Components & Tokens
- Inputs: `border-color-border-base`, `radius-md`
## Navigation & Actions
- Nút "Update Password" → Gọi API → Toast Success → Quay lại SCR-10

# SCR-13 – Notification Center
## Purpose
Xem danh sách thông báo hệ thống. (Actor: All Authenticated)
## Layout Wireframe
Main Content
  └── text-heading-lg "Notifications"
  └── List of Notification Items (bg-surface-card, border-bottom)
       └── text-heading-sm Title | text-body-sm Time
       └── Unread indicator (dot color-primary-base)
## Components & Tokens
- List Item: `color-surface-card`, `border-color-border-subtle`, hover: `bg-surface-canvas`
## Navigation & Actions
- Click Notification Item → Chuyển tới SCR-14

# SCR-14 – Notification Detail
## Purpose
Xem nội dung chi tiết của một thông báo. (Actor: All Authenticated)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6)
       └── text-heading-lg Title
       └── text-caption Time
       └── text-body-base Full message content
       └── button-secondary "Back to Notifications"
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`, `shadow-sm`
## Navigation & Actions
- Nút "Back" → Quay lại SCR-13

---

## 🛎️ Section 3 — Customer Screens

# SCR-15 – Customer Dashboard
## Purpose
Tổng quan các booking sắp tới của khách. (Actor: Customer)
## Layout Wireframe
nav-bar (color-surface-canvas)
Main Content (bg-surface-canvas)
  └── text-heading-lg "Welcome, [Name]"
  └── Grid (2 columns)
       ├── Left: "Upcoming Stay" Card (bg-primary-base, text-inverted, radius-lg)
       │    └── Room Name, Check-in Date, Countdown
       └── Right: Quick Links (My Bookings, My Reviews, Support)
## Components & Tokens
- Highlight Card: `color-primary-base`, `text-inverted`, `radius-lg`, `shadow-md`
- Typography: `text-heading-lg`
## Navigation & Actions
- Click "Upcoming Stay" → Chuyển tới SCR-18

# SCR-16 – Booking Checkout
## Purpose
Form đặt phòng và thanh toán. (Actor: Customer)
## Layout Wireframe
nav-bar (color-surface-canvas)
Main Content (2 columns)
  ├── Left (70%): Form
  │    └── Guest Details | Special Requests | Payment Method (**VNPay only** — TD-011)
  │    └── button-primary "Confirm & Pay Deposit"
  └── Right (30%): Sticky Order Summary
       └── Room Info | Dates | Price Calculation | Total | Deposit Required (40%)
## Components & Tokens
- Summary Card: `color-surface-card`, `radius-lg`, `shadow-md`
- Inputs: `radius-md`, focus: `border-color-primary-base`
- Button: `button-primary`
## Navigation & Actions
- Submit → Gọi API tạo Booking → Chuyển hướng VNPay

# SCR-17 – Booking Management
## Purpose
Danh sách toàn bộ booking đã đặt. (Actor: Customer)
## Layout Wireframe
Main Content
  └── text-heading-lg "My Bookings"
  └── Tabs (All, Upcoming, Completed, Cancelled)
  └── List of Booking Cards (bg-surface-card, flex-row)
       └── Thumbnail | text-heading-md Room | Dates | StatusBadge (color-success/warning) | button-secondary "View Detail"
## Components & Tokens
- Status Badge: `radius-full`, 10% opacity background of semantic color.
- Booking Card: `color-surface-card`, `border-color-border-subtle`, `radius-lg`
## Navigation & Actions
- Click "View Detail" → Chuyển tới SCR-18

# SCR-18 – Booking Detail
## Purpose
Chi tiết một booking, kèm các action tương ứng trạng thái. (Actor: Customer)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6)
       └── text-heading-lg "Booking #12345" | StatusBadge
       └── Divider
       └── Grid: Dates, Guests, Total Price, Amount Paid, Remaining Balance
       └── Action Bar:
            ├── Nếu Pending: button-primary "Pay Remaining"
            ├── Nếu sắp tới: button-danger "Cancel Booking"
            └── Nếu Completed: button-primary "Write Review"
## Components & Tokens
- Action Buttons: `button-primary`, `button-danger`
## Navigation & Actions
- Click "Cancel" → Chuyển tới SCR-19
- Click "Write Review" → Chuyển tới SCR-25

# SCR-19 – Booking Cancellation
## Purpose
Xem chính sách phạt và xác nhận hủy phòng. (Actor: Customer)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6, border-top-danger)
       └── text-heading-lg "Cancel Booking"
       └── Alert Box (bg-danger 10% opacity, text-danger) "Cancellation Policy: refund of deposit by days until check-in — ≥7d 100% / 3–7d 50% / <3d 0%"
       └── text-body-base "Refund Amount: $XXX (= tier % of deposit)"
       └── button-danger "Confirm Cancellation" | button-ghost "Go Back"
## Components & Tokens
- Alert Box: `color-danger` (10% bg, 100% text), `radius-md`
## Navigation & Actions
- Xác nhận hủy → API → Toast Success → Quay lại SCR-17

# SCR-20 – Order Review & Payment
## Purpose
Xem lại hóa đơn tổng kết, chính sách lưu trú và tiến hành thanh toán (Actor: Customer)
## Layout Wireframe
Main Content
  └── 2 Columns (hoặc Stacked trên Mobile):
       ├── Left (60%): Review Booking Details (Guest Info, Dates, Special Requests)
       │    └── Rules & Policies (Chính sách hủy, nội quy)
       └── Right (40%): Order Summary Card (Tổng tiền, Tiền cọc, Thuế phí)
            └── Payment Method Selection (VNPay)
            └── button-primary "Pay via VNPay"
## Components & Tokens
- Summary Card: `bg-surface-card`, `radius-lg`, `shadow-md`
- Button: `button-primary` (chiều rộng 100%)
## Navigation & Actions
- Click "Pay via VNPay" → Redirect sang cổng VNPay


# SCR-21 – My Contract List
## Purpose
Danh sách hợp đồng thuê phòng. (Actor: Customer)
## Layout Wireframe
Main Content
  └── text-heading-lg "My Contracts"
  └── Table (Booking ID, Room, Date, PDF Link)
       └── Drawer Trigger (Click row opens right-drawer with PDF Viewer)
## Components & Tokens
- Table: `bg-surface-card`, `border-color-border-subtle`
- Drawer: `bg-surface-card`, `shadow-lg`, slide-in from right.
## Navigation & Actions
- Click PDF → Mở Drawer xem hợp đồng.

# SCR-22 – Maintenance Ticket List
## Purpose
Danh sách yêu cầu bảo trì đã gửi. (Actor: Customer)
## Layout Wireframe
Main Content
  └── text-heading-lg "Maintenance Requests" | button-primary "Create Request"
  └── Table (Ticket ID, Issue, StatusBadge, Created Date)
## Components & Tokens
- StatusBadge: `color-warning` (Pending), `color-info` (In Progress), `color-success` (Resolved)
## Navigation & Actions
- Nút "Create Request" → Chuyển tới SCR-23

# SCR-23 – Create Maintenance Ticket
## Purpose
Gửi yêu cầu bảo trì mới. (Actor: Customer)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6)
       └── text-heading-lg "Report an Issue"
       └── Input (Issue Title) | Textarea (Description) | Image Upload Area
       └── button-primary "Submit Request"
## Components & Tokens
- Inputs: `radius-md`, `border-color-border-base`
## Navigation & Actions
- Submit → API → Quay lại SCR-22

# SCR-24 – My Reviews
## Purpose
Xem danh sách đánh giá đã viết. (Actor: Customer)
## Layout Wireframe
Main Content
  └── text-heading-lg "My Reviews"
  └── List of Review Cards (bg-surface-card, padding space-4)
       └── Star Rating (text-warning) | text-body-base Comment | text-caption Date
## Components & Tokens
- Review Card: `color-surface-card`, `radius-md`, `border-color-border-subtle`
## Navigation & Actions
- View only.

# SCR-25 – Review & Rating
## Purpose
Viết đánh giá sau khi check-out. (Actor: Customer)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card)
       └── text-heading-lg "Rate Your Stay"
       └── 5-Star interactive component
       └── Textarea "Tell us about your experience"
       └── button-primary "Submit Review"
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`
## Navigation & Actions
- Submit → API → Quay lại SCR-17

# SCR-26 – Payment History
## Purpose
Xem lịch sử dòng tiền. (Actor: Customer)
## Layout Wireframe
Main Content
  └── text-heading-lg "Payment History"
  └── Table (Date, Transaction ID, Amount, Type, StatusBadge)
## Components & Tokens
- Table: `bg-surface-card`, `text-body-sm`
## Navigation & Actions
- View only.

---

## 🏢 Section 4 — Manager Screens

# SCR-27 – Manager Dashboard
## Purpose
KPIs Property được gán. (Actor: Manager)
## Layout Wireframe
Sidebar Layout (bg-surface-inverted for sidebar)
Main Content (bg-surface-canvas)
  └── Top: text-heading-lg "Dashboard" | Property Selector Dropdown
  └── 4 KPI Cards (Grid)
       └── "Total Rooms", "Occupancy Rate", "Today Check-ins", "Pending Approvals"
  └── 2 Charts (Revenue Line Chart, Booking Source Pie Chart)
## Components & Tokens
- Sidebar: `color-surface-inverted`, `text-inverted`
- KPI Cards: `color-surface-card`, `radius-lg`, `shadow-sm`
## Navigation & Actions
- Click KPI Card → Chuyển tới List tương ứng.

# SCR-28 – Structure Management
## Purpose
Quản lý cây cấu trúc Property → Floor. (Actor: Manager)
## Layout Wireframe
Main Content
  └── text-heading-lg "Structure Tree" | button-primary "Add Floor"
  └── Tree View Component (Expandable)
       └── Floor 1
            └── Room 101 | Room 102
## Components & Tokens
- Tree View: `text-body-base`, indentations with `color-border-base` lines.
- Modal: Click "Add Floor" mở Modal `bg-surface-card`
## Navigation & Actions
- Submit Modal → Gọi API cập nhật Tree.

# SCR-29 – Room Management List
## Purpose
Danh sách phòng thuộc Property. (Actor: Manager)
## Layout Wireframe
Main Content
  └── Top: text-heading-lg "Rooms" | button-primary "Add New Room"
  └── Table (Room Number, Type, Floor, Price, StatusBadge)
       └── Actions (Kebab menu: Edit, Gallery, Status)
## Components & Tokens
- Table: `bg-surface-card`, hover row: `bg-surface-canvas`
- StatusBadge: `color-success` (Clean), `color-danger` (Dirty), `color-warning` (Maintenance)
## Navigation & Actions
- Click "Add New Room" → Chuyển tới SCR-30
- Click Kebab menu -> Edit → Chuyển tới SCR-31
- Click Kebab menu -> Gallery → Chuyển tới SCR-32

# SCR-30 – Add Room
## Purpose
Tạo phòng mới với form dài. (Actor: Manager)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6)
       └── text-heading-lg "Add New Room"
       └── Form Grid (2 cols): Room Number, Floor, Room Type, Base Price
       └── Amenities Checkboxes (Grid of 4 cols)
       └── text-area (Description)
       └── Action Bar: button-primary "Create Room" | button-ghost "Cancel"
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`, `shadow-sm`
- Inputs: `border-color-border-base`, `radius-md`
## Navigation & Actions
- Create Room → API → Chuyển về SCR-29


# SCR-31 – Edit Room
## Purpose
Chỉnh sửa thông tin cơ bản của phòng. (Actor: Manager)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6)
       └── text-heading-lg "Edit Room 101"
       └── Form Grid (2 cols): Floor, Room Type, Base Price
       └── Amenities Checkboxes (Grid of 4 cols)
       └── text-area (Description)
       └── Action Bar: button-primary "Save Changes" | button-ghost "Cancel"
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`, `shadow-sm`
- Inputs: `border-color-border-base`, `radius-md`
## Navigation & Actions
- Nút "Save Changes" → API → Toast Success → Quay lại SCR-29

# SCR-32 – Room Gallery Management
## Purpose
Quản lý, sắp xếp hình ảnh của phòng. (Actor: Manager)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6)
       └── text-heading-lg "Gallery - Room 101"
       └── Upload Area (bg-surface-canvas, dashed border-color-primary-base)
       └── Grid of Thumbnails (4 cols)
            └── Image (radius-md) | Overlay Delete Button (bg-danger)
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`
- Image Thumbnails: `radius-md`, overflow-hidden
## Navigation & Actions
- Click Delete → Confirm Modal → API → Xóa ảnh

# SCR-33 – Room Status Management
## Purpose
Khóa phòng, cập nhật trạng thái bảo trì/dọn dẹp. (Actor: Manager)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6)
       └── text-heading-lg "Status - Room 101"
       └── Select Input (Clean, Dirty, Maintenance, Out of Order)
       └── Date Picker (Start Date - End Date for Maintenance)
       └── text-area (Reason)
       └── button-primary "Update Status"
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`
## Navigation & Actions
- Submit → API → Quay lại SCR-29

# SCR-34 – Booking List
## Purpose
Danh sách Booking thuộc Property quản lý. (Actor: Manager)
## Layout Wireframe
Main Content
  └── Top: text-heading-lg "Bookings" | Search Input | Filter Dropdown (Status, Date)
  └── Table (Booking ID, Guest, Room, Check-in, Check-out, Total, StatusBadge)
       └── Actions (View Detail)
## Components & Tokens
- Table: `bg-surface-card`, `border-color-border-subtle`
- StatusBadge: `color-warning` (Pending), `color-success` (Confirmed/Completed), `color-danger` (Cancelled)
## Navigation & Actions
- Click "View Detail" → Chuyển tới SCR-35

# SCR-35 – Booking Detail
## Purpose
Xem chi tiết Booking, Check-in/out, Approval. (Actor: Manager)
## Layout Wireframe
Main Content
  └── 2 Columns:
       ├── Left (70%): Card (Guest Info, Dates, Special Requests)
       │    └── Divider
       │    └── Payment Breakdown
       └── Right (30%): Action Card (bg-surface-card)
            └── StatusBadge (Confirmed)
            └── button-primary "Check In Guest" (Nếu đến ngày)
            └── button-secondary "Resend Confirmation"
            └── button-danger "Cancel Booking" (chỉ khi Pending Deposit / Confirmed, trước check-in)
## Components & Tokens
- Action Card: `color-surface-card`, `radius-lg`, `shadow-md`
- Button: `button-primary`, `button-danger`
## Navigation & Actions
- Nút "Check In Guest" → Cập nhật status → reload SCR-35
- Nút "Cancel Booking" → Chuyển tới SCR-69

# SCR-69 – Manager Booking Cancellation
## Purpose
Manager hủy booking thuộc Property được gán; xác nhận hoàn **100%** số tiền đã thanh toán. (Actor: Manager)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6, border-top-danger)
       └── text-heading-lg "Cancel Booking (Manager)"
       └── Alert Box (bg-info 10% opacity) "Manager-initiated cancel → Guest receives 100% refund of deposit"
       └── Booking Summary (ID, Guest, Room, Dates, Paid Amount, Refund Amount = 100%)
       └── Textarea "Cancel Reason" (required — force majeure / system error / other)
       └── button-danger "Confirm Cancellation (Full Refund)" | button-ghost "Go Back"
## Components & Tokens
- Alert Box: `color-info` (10% bg), `radius-md`
- Textarea: required, max 500 chars
## Navigation & Actions
- Xác nhận hủy → `POST /api/v1/manager/bookings/{id}/cancel` → Toast Success → Quay lại SCR-35 (status Cancelled)
- Chỉ cho phép khi status = Pending Deposit hoặc Confirmed (trước check-in)
- On success: Booking → Cancelled; Payments Paid → Refunded; Room → Available; Contract → Cancelled; notify Customer

# SCR-36 – Payment Management
## Purpose
Danh sách Payment (VNPay) thuộc Property được gán. (Actor: Manager)
## Layout Wireframe
Main Content
  └── text-heading-lg "Payments"
  └── Tabs (All, Pending, Paid, Failed, Refunded)
  └── Table (Date, Transaction ID, Booking ID, Amount, Method=VNPay, StatusBadge)
## Components & Tokens
- Table: `bg-surface-card`
## Navigation & Actions
- Click row → View details in Drawer (Read-only)
- **Edu (TD-011):** không có Approve/Reject biên lai chuyển khoản. Payment Verification CK — deferred.

# SCR-37 – Check-in / Check-out
## Purpose
Xác nhận Check-in/out; CCCD; chìa khóa; Remaining 60% phải PAID trước khi hoàn tất Check-in. (Actor: Manager)
## Layout Wireframe
Main Content
  └── text-heading-lg "Check-in / Check-out"
  └── Booking summary + guest ID verification + remaining balance status
  └── Action Bar: button-primary "Confirm Check-in" | button-primary "Confirm Check-out"
## Navigation & Actions
- Check-in blocked if remaining unpaid
- Check-out blocked if Room Inspection incomplete
- **Note:** SCR-37 **không** phải Payment Verification (đó là TD-011 deferred).

# SCR-38 – Contract Management
## Purpose
Danh sách hợp đồng thuê phòng. (Actor: Manager)
## Layout Wireframe
Main Content
  └── text-heading-lg "Contracts"
  └── Table (Contract ID, Booking ID, Guest, Date, Status)
       └── Drawer Trigger "View PDF"
## Components & Tokens
- Table: `bg-surface-card`
- Drawer: Slide-in từ bên phải, chứa iframe PDF.
## Navigation & Actions
- Click "View PDF" → Mở Drawer
- Nút trong Drawer "Send to Guest Email" → Gửi email API

# SCR-39 – Employee Management
## Purpose
Danh sách nhân viên. Gán nhân viên vào Property. (Actor: Manager)
## Layout Wireframe
Main Content
  └── text-heading-lg "Staff Directory" | button-primary "Assign Employee"
  └── Table (Name, Role, Phone, Status)
## Components & Tokens
- Nút "Assign Employee" mở Modal `bg-surface-card`, `radius-lg`
## Navigation & Actions
- Submit Modal → Gọi API gán quyền → Reload table

# SCR-40 – Housekeeping Tasks
## Purpose
Quản lý dọn phòng. (Actor: Manager)
## Layout Wireframe
Main Content
  └── text-heading-lg "Housekeeping" | button-primary "Create Task"
  └── Board/List (To Do, In Progress, Done)
       └── Task Card (Room 101, text-caption "Assigned to: Jane")
## Components & Tokens
- Task Card: `bg-surface-card`, `border-color-border-base`, `radius-md`
## Navigation & Actions
- Click Task Card → Mở Drawer chỉnh sửa/gán nhân viên.

# SCR-41 – Maintenance Tasks
## Purpose
Quản lý bảo trì. (Actor: Manager)
## Layout Wireframe
Main Content
  └── text-heading-lg "Maintenance Requests"
  └── Table (Room, Issue, Reporter, Assigned To, StatusBadge)
## Components & Tokens
- StatusBadge: `color-danger` (Urgent), `color-warning` (Normal)
## Navigation & Actions
- Click Row → Mở Drawer xem ảnh hư hại và gán kỹ thuật viên.

# SCR-42 – Inspection Management
## Purpose
Quản lý kiểm tra phòng trước Check-out. (Actor: Manager)
## Layout Wireframe
Main Content
  └── text-heading-lg "Room Inspections"
  └── Table (Room, Booking, Inspector, Result: Passed/Failed)
## Components & Tokens
- Result Text: `color-success` (Passed), `color-danger` (Failed)
## Navigation & Actions
- Click Row → Mở Drawer xem chi tiết log kiểm tra; **Gán / đổi Inspector** (Employee thuộc Property).

# SCR-43 – Damage Report Management
## Purpose
Duyệt báo cáo hư hại. (Actor: Manager)
## Layout Wireframe
Main Content
  └── text-heading-lg "Damage Reports"
  └── Table (Room, Items Damaged, Est. Cost, StatusBadge)
## Components & Tokens
- StatusBadge: `color-warning` (Pending Review), `color-info` (Escalated to Admin)
## Navigation & Actions
- Click Row → Mở Drawer xem ảnh, Approve bồi thường (Nếu > 5M, nút sẽ thành "Escalate to Admin")

# SCR-44 – Property Reports
## Purpose
Báo cáo thống kê của Property. (Actor: Manager)
## Layout Wireframe
Main Content
  └── Tabs: [Revenue] [Occupancy] [Booking Trends]
  └── Content (Phụ thuộc tab):
       └── Date Range Picker
       └── Large Line Chart (Chart.js)
       └── Data Table Summary
## Components & Tokens
- Tabs: `text-heading-md`, active tab có border-bottom `color-primary-base`
- Chart Container: `bg-surface-card`, `radius-lg`, `padding space-6`
## Navigation & Actions
- View only.

---

## 🔑 Section 5 — Admin Screens

# SCR-45 – Admin Dashboard
## Purpose
KPIs toàn hệ thống. (Actor: Admin)
## Layout Wireframe
Sidebar Layout (bg-surface-inverted)
Main Content
  └── 4 KPI Cards (Total Revenue, Active Properties, Total Bookings, New Customers)
  └── Global Map / Bar Chart
## Components & Tokens
- KPI Cards: `color-surface-card`, `radius-lg`
## Navigation & Actions
- View only.

# SCR-46 – Property Management
## Purpose
Danh sách Property toàn hệ thống. (Actor: Admin)
## Layout Wireframe
Main Content
  └── text-heading-lg "Properties" | button-primary "Create Property"
  └── Table (Name, Location, Manager, Status)
## Components & Tokens
- Table: `bg-surface-card`, `border-color-border-subtle`
## Navigation & Actions
- Click "Create Property" → Chuyển tới SCR-47
- Click Row -> Edit → Chuyển tới SCR-48

# SCR-47 – Create Property
## Purpose
Form tạo chi nhánh mới. (Actor: Admin)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-8)
       └── text-heading-lg "New Property"
       └── Section 1: Basic Info (Name, Type)
       └── Section 2: Location (Address, Map coordinates)
       └── Section 3: Policies (Check-in time, Penalty %)
       └── button-primary "Create Property"
## Components & Tokens
- Sections: margin-bottom `space-8`, text-heading-md
- Button: `button-primary`, `radius-md`
## Navigation & Actions
- Submit → API → Quay lại SCR-46

# SCR-48 – Edit Property
## Purpose
Chỉnh sửa thông tin Property. (Actor: Admin)
## Layout Wireframe
Tương tự SCR-47 nhưng pre-fill data.
## Components & Tokens
Tương tự SCR-47
## Navigation & Actions
- Submit → API → Quay lại SCR-46

# SCR-49 – Manager Assignment
## Purpose
Chỉ định Manager quản lý Property. (Actor: Admin)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6)
       └── text-heading-lg "Assign Manager to [Property Name]"
       └── Search Input (Find Manager by Email/Name)
       └── List of selected managers
       └── button-primary "Save Assignments"
## Components & Tokens
- Card: `color-surface-card`, `radius-lg`
## Navigation & Actions
- Nút "Save" → API → Quay lại SCR-46

# SCR-50 – Manager Directory
## Purpose
Quản lý tài khoản Manager. (Actor: Admin)
## Layout Wireframe
Main Content
  └── text-heading-lg "Managers" | button-primary "Create Manager"
  └── Table (Name, Email, Properties Assigned, Status)
## Components & Tokens
- Nút "Create Manager" mở Modal.
## Navigation & Actions
- Click Row → Mở Drawer xem chi tiết hồ sơ.

# SCR-51 – Customer Directory
## Purpose
Quản lý tài khoản Customer. (Actor: Admin)
## Layout Wireframe
Main Content
  └── text-heading-lg "Customers"
  └── Table (Name, Email, Total Bookings, Total Spend)
## Components & Tokens
- Table: `bg-surface-card`
## Navigation & Actions
- Click Row → Mở Drawer xem lịch sử booking của khách.

# SCR-52 – Payment Reconciliation
## Purpose
**Deferred edu (TD-010)** — Giao dịch lệch/timeout VNPay. (Actor: Admin). Không bắt buộc khóa học.
## Layout Wireframe
Main Content
  └── text-heading-lg "VNPay Discrepancies"
  └── Alert (bg-danger 10%, text-danger) "Có 3 giao dịch cần đối soát"
  └── Table (Txn ID, VNPay Amount, DB Amount, Diff)
## Components & Tokens
- Alert Box: `color-danger` opacity 10%
## Navigation & Actions
- Click Row → Mở Drawer đối soát thủ công (Sync from VNPay API) — chỉ khi Spec Sync reopen TD-010.

# SCR-53 – Damage Escalation
## Purpose
Báo cáo hư hại > 5M cần duyệt. (Actor: Admin)
## Layout Wireframe
Main Content
  └── text-heading-lg "Escalated Damages"
  └── Table (Property, Room, Est. Cost, Manager Note)
## Components & Tokens
- Table: `bg-surface-card`
## Navigation & Actions
- Click Row → Mở Drawer xem ảnh. Nút "Approve" (button-success).

# SCR-54 – Complaint Management
## Purpose
Xử lý khiếu nại của khách. (Actor: Admin)
## Layout Wireframe
Main Content
  └── text-heading-lg "Complaints"
  └── Table (Ticket ID, Customer, Subject, StatusBadge)
## Components & Tokens
- Table: `bg-surface-card`
## Navigation & Actions
- Click Row → Mở Drawer chat/reply khiếu nại.

# SCR-55 – Global Reports
## Purpose
Báo cáo doanh thu toàn hệ thống. (Actor: Admin)
## Layout Wireframe
Giống SCR-44 nhưng có thêm Filter theo Property.
## Components & Tokens
- Chart Container: `bg-surface-card`, `radius-lg`
## Navigation & Actions
- View only.

# SCR-56 – System Administration
## Purpose
Cấu hình hệ thống (Activity Logs, Settings). (Actor: Admin)
## Layout Wireframe
Main Content
  └── Tabs: [Activity Logs] [System Settings] [Content Moderation]
  └── Nội dung Tab Settings:
       └── Input (Deposit %, Default Time-out, Penalty Rate)
       └── button-primary "Save Settings"
## Components & Tokens
- Tabs: `text-heading-md`
## Navigation & Actions
- Save Settings → API.

# SCR-57 – Promotion Management
## Purpose
Quản lý banner khuyến mãi. (Actor: Admin)
## Layout Wireframe
Main Content
  └── text-heading-lg "Promotions" | button-primary "Add Promotion"
  └── Grid of Promo Cards (Image, Code, Discount %, Status)
## Components & Tokens
- Promo Card: `bg-surface-card`, `radius-lg`, `shadow-sm`
## Navigation & Actions
- Click "Add Promotion" → Chuyển tới SCR-58

# SCR-58 – Add / Edit Promotion
## Purpose
Form tạo banner/mã giảm giá. (Actor: Admin)
## Layout Wireframe
Main Content
  └── Card (bg-surface-card, padding space-6)
       └── Input (Promo Code) | Input (Discount %)
       └── Date Range (Start - End)
       └── Image Upload (Banner)
       └── button-primary "Save Promotion"
## Components & Tokens
- Card: `color-surface-card`
## Navigation & Actions
- Submit → API → Quay lại SCR-57

---

## 👷 Section 6 — Employee Screens

# SCR-59 – Employee Dashboard
## Purpose
Tổng quan nhiệm vụ trong ngày. (Actor: Employee)
## Layout Wireframe
nav-bar (color-surface-inverted) (Mobile-first layout)
Main Content
  └── text-heading-lg "Hello, [Name]"
  └── 3 Large Action Cards (Housekeeping, Maintenance, Inspections)
       └── Icon | Title | "X pending tasks"
## Components & Tokens
- Action Card: `bg-surface-card`, `radius-lg`, `shadow-md`, touch-friendly (min-height 100px)
## Navigation & Actions
- Click Card → Chuyển tới danh sách tương ứng.

# SCR-60 – Housekeeping Workspace
## Purpose
Danh sách tác vụ dọn phòng. (Actor: Employee)
## Layout Wireframe
Main Content
  └── List of Rooms (Room 101, Room 102)
  └── Each item: Swipe right to mark as "In Progress", Swipe left to mark as "Done".
  └── Hoặc nút button-primary "Start" / button-success "Finish"
## Components & Tokens
- Mobile List Item: `bg-surface-card`, `border-color-border-subtle`, padding space-4
## Navigation & Actions
- Cập nhật trạng thái → API.

# SCR-61 – Maintenance Workspace
## Purpose
Danh sách tác vụ bảo trì. (Actor: Employee)
## Layout Wireframe
Tương tự SCR-60. Kèm theo Modal để ghi chú vật tư đã thay thế.
## Components & Tokens
- Tương tự SCR-60
## Navigation & Actions
- Cập nhật trạng thái → API.

# SCR-62 – Room Inspection Hub
## Purpose
Bảng kiểm tra phòng trước Check-out. (Actor: Employee)
## Layout Wireframe
Main Content
  └── List of Rooms ready for checkout
  └── Click room → Mở Drawer/Modal Checklist (TV, Minibar, Bed, Bathroom)
       └── Toggle Checkboxes cho từng item.
       └── button-success "Pass" | button-danger "Fail"
## Components & Tokens
- Checklist Item: `text-body-base`, Checkbox `radius-sm`
## Navigation & Actions
- Nút "Fail" → Gợi ý tạo Damage Report (Chuyển tới SCR-64)
- Nút "Pass" → Cập nhật trạng thái phòng.

# SCR-63 – Damage Report List
## Purpose
Danh sách báo cáo hư hại đã ghi nhận. (Actor: Employee)
## Layout Wireframe
Main Content
  └── text-heading-lg "My Reports"
  └── List Items (Room, Date, Status)
## Components & Tokens
- List Item: `bg-surface-card`
## Navigation & Actions
- Click nút "+" FAB (Floating Action Button) → Chuyển tới SCR-64

# SCR-64 – Create Damage Report
## Purpose
Ghi nhận hư hại, chụp ảnh, tính giá. (Actor: Employee)
## Layout Wireframe
Main Content (Mobile optimized)
  └── Input (Select Room)
  └── Dynamic List of Damaged Items:
       └── [Item Name] | [Estimated Cost] | [Photo Upload Button]
  └── button-ghost "Add Another Item"
  └── text-area (Note)
  └── button-primary "Submit Report"
## Components & Tokens
- FAB/Buttons: touch-friendly, min-height 48px.
- Photo Upload: Mở camera native của thiết bị (thông qua HTML5 input capture).
## Navigation & Actions
- Submit → API → Quay lại SCR-63

# SCR-65 – Property Room List
## Purpose
Danh sách phòng để tham khảo. (Actor: Employee)
## Layout Wireframe
Main Content
  └── Search Bar (Room Number)
  └── Simple List of Rooms with Status Badge (Clean, Dirty, Occupied)
## Components & Tokens
- StatusBadge: `color-success`, `color-danger`, `color-warning` 10% opacity.
## Navigation & Actions
- View only.

