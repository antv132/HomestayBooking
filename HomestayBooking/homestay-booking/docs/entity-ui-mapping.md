# 1. Purpose
Tài liệu này ánh xạ các thực thể (Entities) từ Database/Backend (được định nghĩa trong `Specification_v2.md`) với các giao diện người dùng (UI Screens) và các thành phần giao diện (được định nghĩa trong `screen.md` và `screendesign.md`). Mục đích là để đảm bảo sự nhất quán giữa dữ liệu và giao diện, làm rõ các ràng buộc, quyền truy cập, và luồng dữ liệu của Hệ thống Homestay / Resort Booking Management System.

# 2. Mapping Principles
- **Data-Driven UI**: Mỗi hiển thị trên UI phải dựa trên dữ liệu từ các Entity tương ứng.
- **Role-Based Access Control (RBAC)**: Các trường dữ liệu và thao tác (Create/Update/Delete) trên UI được kiểm soát chặt chẽ bởi Role của Actor (Guest, Customer, Manager, Admin, Employee).
- **Status-Driven State**: Trạng thái (Status) của Entity quyết định trực tiếp đến trạng thái hiển thị của các thành phần UI (VD: Nút bấm bị vô hiệu hóa, màu sắc huy hiệu/badge).

# 3. Entity List
Danh sách các thực thể chính trong hệ thống:
1. User
2. Property
3. PricingRule
4. ManagerPropertyAssignment & EmployeePropertyAssignment
5. Floor
6. Room & RoomImage
7. Booking
8. Contract
9. Payment & PaymentReceipt
10. MaintenanceTicket
11. Review
12. Notification
13. Complaint
14. ActivityLog
15. SystemSetting
16. Promotion
17. HousekeepingTask
18. Attachment
19. RoomInspection
20. DamageReport & DamageItem

# 4. Entity UI Mapping

---

### User
- **Description**: Lưu trữ thông tin tài khoản người dùng, phân quyền (Role: ADMIN, MANAGER, EMPLOYEE, CUSTOMER), và trạng thái hoạt động.
- **Mapped Screens**:
  - **SCR-02, SCR-03, SCR-04, SCR-05, SCR-06**: Luồng xác thực (Login, Register, OTP, Forgot/Reset Password).
  - **SCR-10 (User Profile), SCR-11 (Edit Profile), SCR-12 (Change Password)**: Xem và cập nhật thông tin cá nhân.
  - **SCR-39 (Employee Management)**: Manager xem và gán quyền Employee vào Property.
  - **SCR-50 (Manager Directory)**: Admin quản lý tài khoản Manager.
  - **SCR-51 (Customer Directory)**: Admin quản lý tài khoản Customer.
- **UI Components**: Avatar, Profile Card, Table danh sách tài khoản, Form cập nhật thông tin.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | User ID | Text | UUID |
| fullName | Full Name | Text | John Doe |
| email | Email | Text | user@example.com |
| phone | Phone Number | Text | 0987654321 |
| avatarUrl | Avatar | Image (Avatar) | Circular Image, Fallback to Initials |
| role | Role | Chip | CUSTOMER / MANAGER / ADMIN / EMPLOYEE |
| status | Status | StatusBadge | Active / Inactive / Suspended |
| createdAt | Joined Date | Date | dd/MM/yyyy |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-03 | Create (Register) |
| SCR-10 | Read (Profile) |
| SCR-50 | Read (List Managers) |
| SCR-51 | Read (List Customers) |
| SCR-11 | Update (Profile) |
| SCR-12 | Update (Password) |
| SCR-39 | Assign (Employee) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: fullName, email, phone.
- **Filter Options**: role, status.
- **Sort Options**: createdAt (Newest), fullName (A-Z).

#### 4. Table Mapping
- **Table Columns (SCR-50, SCR-51)**: Avatar, Name, Email, Properties Assigned / Total Bookings, Status, Actions.
- **Actions**: View Detail (Drawer), Suspend/Activate.
- **Bulk Actions**: None.
- **Pagination**: 10/20/50 rows per page.
- **Selection**: None.

#### 5. Form Mapping
- **Sections (SCR-11)**: Basic Information (fullName, phone), Avatar Upload.
- **Input Components**: TextInput (fullName, phone), FileUpload (avatarUrl).
- **Required Fields**: fullName, email.
- **Readonly Fields**: email (if verified), role.
- **Hidden Fields**: id, passwordHash.
- **Auto-generated Fields**: createdAt, updatedAt.

#### 6. Action Mapping
- Register
- Login
- Verify Email (OTP)
- Update Profile
- Change Password
- Suspend Account (Admin)
- Activate Account (Admin)
- Assign to Property (Manager/Admin)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| INACTIVE | Gray | Neutral | Verify Email |
| ACTIVE | Green | Success | Login, Update Profile, Book Room |
| SUSPENDED | Red | Danger | View Reason (Login Blocked) |

#### 8. Component Mapping
- ProfileCard (`color-surface-card`, `radius-lg`, `shadow-sm`)
- Avatar (radius-full, 120px)
- StatusBadge
- UserTable (Admin, SCR-50/51)
- AssignEmployeeModal (SCR-39, `bg-surface-card`, `radius-lg`)

#### 9. Responsive Mapping
- **Desktop**: Table view for directories. Sidebar profile navigation.
- **Tablet**: Table allows horizontal scrolling. Drawer takes 50% width.
- **Mobile**: Tables convert to stacked UserCards.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-02 | /api/v1/auth/login | POST |
| SCR-03 | /api/v1/auth/register | POST |
| SCR-04 | /api/v1/auth/verify-otp | POST |
| SCR-05 | /api/v1/auth/forgot-password | POST |
| SCR-06 | /api/v1/auth/reset-password | POST |
| SCR-10 | /api/v1/users/me | GET |
| SCR-11 | /api/v1/users/me | PUT |
| SCR-12 | /api/v1/users/me/password | PUT |
| SCR-50 | /api/v1/admin/managers | GET |
| SCR-51 | /api/v1/admin/customers | GET |

#### 11. Entity Relationship Mapping
- **→ Bookings**: Loaded lazily (Customer).
- **→ Reviews**: Loaded lazily (Customer).
- **→ Properties**: Loaded directly for Managers (ManagerPropertyAssignment).
- **→ EmployeePropertyAssignment**: Loaded for Employees.

#### 12. Derived / Calculated Fields
- `totalBookings` (Admin/Customer View) = Count of associated bookings.
- `totalSpend` (Admin View) = Sum of totalAmount for completed bookings.

#### 13. Empty / Loading / Error States
- **Loading**: Skeleton rows in directory tables.
- **Empty**: "No users found" state with an empty icon.
- **API Error**: Toast error "Failed to load user profile."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| email | Required, Valid Email Format | Invalid email address |
| fullName | Required, Max 200 chars | Name is required and must be under 200 characters |
| phone | Max 20 characters, numeric | Invalid phone number |

#### 15. Permission Mapping
- **Guest**: Can Create (Register).
- **Customer**: Visible (Own profile only). Update (Own profile).
- **Manager**: Visible (Employees in own property). Assign Employee.
- **Employee**: Visible (Own profile).
- **Admin**: Visible (All users). Update (Suspend/Activate/Create Manager).

---

### Property
- **Description**: Chi nhánh (Homestay/Resort) do hệ thống quản lý.
- **Mapped Screens**:
  - **SCR-01 (Landing Page)**: Hiển thị danh sách "Popular Destinations".
  - **SCR-27 (Manager Dashboard)**: Manager xem thông tin tổng quan của Property được gán (Property Selector Dropdown).
  - **SCR-44 (Property Reports)**: Báo cáo thống kê theo Property.
  - **SCR-46 (Property Management), SCR-47 (Create Property), SCR-48 (Edit Property)**: Admin quản lý danh sách chi nhánh, tạo và chỉnh sửa.
  - **SCR-49 (Manager Assignment)**: Admin gán Manager vào Property.
- **UI Components**: RoomCard (Landing Page), PropertySelectorDropdown (Dashboard), PropertyTable.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | Property ID | Text | UUID |
| name | Property Name | Text | Riverside Resort |
| address | Address | Text | 123 Main St |
| description | Description | TextArea | Truncated at 100 chars |
| status | Status | StatusBadge | Active / Inactive |
| createdAt | Created Date | Date | dd/MM/yyyy |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-47 | Create |
| SCR-46 | Read (List) |
| SCR-27 | Read (Dashboard / Selector) |
| SCR-48 | Update |
| SCR-49 | Assign (Manager) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: name, address.
- **Filter Options**: status, assigned manager.
- **Sort Options**: name (A-Z), createdAt.

#### 4. Table Mapping
- **Table Columns (SCR-46)**: Property Name, Location, Manager, Status, Actions.
- **Actions**: Edit (→ SCR-48), Assign Manager (→ SCR-49), Activate/Deactivate.
- **Bulk Actions**: None.
- **Pagination**: 10 rows per page.
- **Selection**: None.

#### 5. Form Mapping
- **Sections (SCR-47)**: Basic Info (Name, Type), Location (Address, Map coordinates), Policies (Check-in time, Penalty %).
- **Input Components**: TextInput (name, address), TextArea (description), Select (status).
- **Required Fields**: name, address.
- **Readonly Fields**: None.
- **Hidden Fields**: id.
- **Auto-generated Fields**: createdAt, updatedAt.

#### 6. Action Mapping
- Create Property (Admin)
- Edit Property (Admin)
- Assign Manager (Admin)
- Activate / Deactivate Property (Admin)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| ACTIVE | Green | Success | Edit, Assign Manager, Deactivate |
| INACTIVE | Gray | Neutral | Edit, Activate |

#### 8. Component Mapping
- RoomCard (`color-surface-card`, `radius-lg`, `shadow-md`, `hover:shadow-lg`) — Landing Page
- PropertySelectorDropdown — Manager Dashboard
- PropertyTable (`bg-surface-card`, `border-color-border-subtle`)

#### 9. Responsive Mapping
- **Desktop**: 3-column grid for Property/Room Cards (SCR-01).
- **Tablet**: 2-column grid.
- **Mobile**: 1-column stacked cards.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-01 | /api/v1/properties/featured | GET |
| SCR-46 | /api/v1/admin/properties | GET |
| SCR-47 | /api/v1/admin/properties | POST |
| SCR-48 | /api/v1/admin/properties/{id} | PUT |
| SCR-49 | /api/v1/admin/properties/{id}/manager | POST |

#### 11. Entity Relationship Mapping
- **→ Floors**: Loaded lazily on Structure Tree.
- **→ Rooms**: Loaded lazily.
- **→ Manager (ManagerPropertyAssignment)**: Displayed directly on Property list.

#### 12. Derived / Calculated Fields
- `totalRooms` = Count of rooms.
- `occupancyRate` = Calculated from current bookings (Occupied / Total).

#### 13. Empty / Loading / Error States
- **Loading**: Skeleton Property Cards.
- **Empty**: "No properties configured."
- **API Error**: "Failed to load properties."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| name | Required, Max 200 chars | Name is required |
| address | Required, Max 500 chars | Address is required |

#### 15. Permission Mapping
- **Guest / Customer**: Readonly (Public Info on Landing Page).
- **Manager**: Readonly (Assigned Property Only — via Selector).
- **Admin**: Visible, Create, Update, Assign.

---

### PricingRule
- **Description**: Cấu hình giá động theo thời gian và loại phòng, ghi đè `pricePerNight` mặc định của Room.
- **Mapped Screens**:
  - **SCR-08 (Room Detail)**: Applied price shown to Guest/Customer.
  - **SCR-16 (Booking Checkout)**: Price used in Booking calculation.
  - **SCR-44 (Property Reports)**: Revenue data reflects actual applied prices.
  - *(No dedicated CRUD screen in current catalog — managed via API.)*
- **UI Components**: StickyPriceCard (shows computed price — SCR-08, SCR-16).

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | Rule ID | Hidden | UUID |
| propertyId | Property | Text | Property Name |
| roomTypeId | Room Type | Text | Suite / Deluxe (Optional) |
| startDate | Start Date | Date | dd/MM/yyyy |
| endDate | End Date | Date | dd/MM/yyyy |
| pricePerNight | Dynamic Price | Currency | 1,200,000 VND |
| priority | Priority | Number | 1 (Higher = applied first) |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| API only | Create / Update / Delete (Manager/Admin) |
| SCR-08 | Read (Applied price shown to Guest/Customer) |
| SCR-16 | Read (Price used in Booking Checkout) |

#### 3. Search / Filter / Sort Mapping
- **Filter Options**: propertyId, roomTypeId, dateRange.
- **Sort Options**: startDate, priority.

#### 4. Table Mapping
- N/A. No dedicated UI table screen in current catalog.

#### 5. Form Mapping
- N/A. Managed via API in current version.
- **Future Input Components**: DateRangePicker, CurrencyInput (pricePerNight), NumberInput (priority).
- **Required Fields**: propertyId, startDate, endDate, pricePerNight, priority.

#### 6. Action Mapping
- Create Pricing Rule (Manager/Admin via API)
- Update Pricing Rule (Manager/Admin via API)
- Delete Pricing Rule (Manager/Admin via API)

#### 7. Status Mapping
- N/A. Active by date range; no explicit status field.

#### 8. Component Mapping
- StickyPriceCard (shows computed price including applied PricingRule — SCR-08, SCR-16).

#### 9. Responsive Mapping
- N/A. No dedicated screen.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| API | /api/v1/managers/properties/{pid}/pricing-rules | GET / POST / PUT / DELETE |

#### 11. Entity Relationship Mapping
- **→ Property**: Belongs to a property.
- **→ Room (by RoomType)**: Optional override by room type.

#### 12. Derived / Calculated Fields
- Applied price at Booking creation = highest-priority matching PricingRule, or Room.pricePerNight as fallback.

#### 13. Empty / Loading / Error States
- No dedicated UI. Price fallback to Room.pricePerNight if no matching rule.

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| startDate | Must be before endDate | Invalid date range |
| pricePerNight | > 0 | Price must be positive |
| priority | Integer >= 1 | Priority must be a positive integer |

#### 15. Permission Mapping
- **Manager**: Create, Read, Update, Delete (Within assigned property).
- **Admin**: Create, Read, Update, Delete (Global).
- **Guest / Customer**: Read only (applied price shown on Room Detail and Checkout).

---

### ManagerPropertyAssignment & EmployeePropertyAssignment
- **Description**: Bảng gán Manager/Employee vào Property; lưu lịch sử gán.
- **Mapped Screens**:
  - **SCR-49 (Manager Assignment)**: Admin gán Manager vào Property.
  - **SCR-39 (Employee Management)**: Manager gán Employee vào Property.
  - **SCR-27 (Manager Dashboard)**: Property Selector chỉ hiển thị Property của Manager ACTIVE.
- **UI Components**: AssignManagerModal, AssignEmployeeModal, PropertySelectorDropdown.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| managerId / employeeId | Name | Text | Manager/Employee Name |
| propertyId | Property | Text | Property Name |
| assignedBy | Assigned By | Text | Admin Name |
| assignedAt | Assigned Date | Date | dd/MM/yyyy |
| status | Status | StatusBadge | Active / Inactive |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-49 | Create / Update (Manager Assignment) |
| SCR-39 | Create / Update (Employee Assignment) |
| SCR-27 | Read (Active assignments for Property Selector) |

#### 3. Search / Filter / Sort Mapping
- **Filter Options**: status (ACTIVE/INACTIVE), propertyId.
- **Sort Options**: assignedAt (Desc).

#### 4. Table Mapping
- **SCR-39 Table Columns**: Name, Role, Phone, Status, Actions.
- **Actions**: Assign Employee (opens Modal), Deactivate.

#### 5. Form Mapping
- **SCR-49 Input Components**: Search Input (Find Manager by Email/Name), List of selected managers.
- **SCR-39 Input Components**: Modal with Search Input (Find Employee by Email/Name).
- **Required Fields**: managerId/employeeId, propertyId.
- **Hidden Fields**: assignedBy (set server-side), assignedAt.

#### 6. Action Mapping
- Assign Manager to Property (Admin)
- Reassign Manager (Admin — creates new ACTIVE, deactivates old)
- Assign Employee to Property (Manager/Admin)
- Remove Employee Assignment (Manager/Admin)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| ACTIVE | Green | Success | View, Deactivate |
| INACTIVE | Gray | Neutral | View (historical record) |

#### 8. Component Mapping
- AssignManagerModal (`bg-surface-card`, `radius-lg` — SCR-49)
- AssignEmployeeModal (`bg-surface-card`, `radius-lg` — SCR-39)
- PropertySelectorDropdown (SCR-27, SCR-28, SCR-29, etc.)

#### 9. Responsive Mapping
- **Mobile**: Assignment modal takes full screen.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-49 | /api/v1/admin/properties/{id}/manager | POST |
| SCR-39 | /api/v1/managers/employees/assign | POST |

#### 11. Entity Relationship Mapping
- **→ User (Manager/Employee)**: Many assignments per user (history kept).
- **→ Property**: Many managers over time; only 1 ACTIVE at a time.

#### 12. Derived / Calculated Fields
- `propertiesAssigned` = Count of ACTIVE Manager assignments (shown in SCR-50).

#### 13. Empty / Loading / Error States
- **Empty (SCR-39)**: "No employees assigned to this property."
- **Error**: "A manager is already assigned to this property."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| managerId | Must be ACTIVE Manager role | Invalid manager account |
| employeeId | Must be ACTIVE Employee role | Invalid employee account |
| propertyId | Must be an existing ACTIVE property | Invalid property |

#### 15. Permission Mapping
- **Admin**: Create, Read, Update (ManagerPropertyAssignment).
- **Manager**: Create, Read, Update (EmployeePropertyAssignment — own property only).
- **Employee**: Read (Own assignment only).

---

### Floor
- **Description**: Tầng hoặc khu vực chứa các phòng trong một Property.
- **Mapped Screens**:
  - **SCR-28 (Structure Management)**: Manager quản lý cấu trúc Property → Floor (hiển thị dưới dạng Tree View).
- **UI Components**: StructureTreeView (Expandable), FloorModal.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| floorNumber | Floor # | Text | Floor 1 |
| description | Description | Text | Sea view wing |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-28 | Create, Read, Update, Delete |

#### 3. Search / Filter / Sort Mapping
- **Sort Options**: floorNumber (Asc).

#### 4. Table Mapping
- N/A. Displayed as a StructureTreeView with indentation lines (`color-border-base`).

#### 5. Form Mapping
- **Input Components**: NumberInput (floorNumber), TextInput (description).
- **Required Fields**: floorNumber.
- **Hidden Fields**: propertyId (passed via context).

#### 6. Action Mapping
- Add Floor (opens Modal)
- Edit Floor (inline or Modal)
- Delete Floor (only if no rooms exist)

#### 7. Status Mapping
- N/A. No status field on Floor.

#### 8. Component Mapping
- StructureTreeView (expandable, `text-body-base`, indentation lines `color-border-base`)
- FloorModal (`bg-surface-card`, `radius-lg`)

#### 9. Responsive Mapping
- **Desktop**: Tree view with expand/collapse icons.
- **Mobile**: Tree view collapses into accordion lists.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-28 | /api/v1/managers/properties/{pid}/floors | GET |
| SCR-28 | /api/v1/managers/properties/{pid}/floors | POST |
| SCR-28 | /api/v1/managers/properties/{pid}/floors/{fid} | PUT / DELETE |

#### 11. Entity Relationship Mapping
- **→ Property**: Parent.
- **→ Rooms**: Children (Displayed nested in Tree View).

#### 12. Derived / Calculated Fields
- `roomCount` = Count of rooms on this floor (shown in Tree node label).

#### 13. Empty / Loading / Error States
- **Empty**: "No floors added yet. Click 'Add Floor' to start."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| floorNumber | Required, Must be unique per Property | Floor number already exists in this property |

#### 15. Permission Mapping
- **Manager**: Create, Read, Update, Delete (Within assigned property).
- **Admin**: Readonly.

---

### Room & RoomImage
- **Description**: Thông tin chi tiết phòng, diện tích, giá, sức chứa, trạng thái và thư viện ảnh.
- **Mapped Screens**:
  - **SCR-07 (Room Search & Listing)**: Guest/Customer tìm kiếm và xem danh sách phòng.
  - **SCR-08 (Room Detail)**: Chi tiết phòng, hiển thị Gallery và Sticky Price Card.
  - **SCR-09 (Availability Calendar)**: Lịch trống của phòng.
  - **SCR-29 (Room Management List), SCR-30 (Add Room), SCR-31 (Edit Room)**: Manager quản lý thông tin phòng.
  - **SCR-32 (Room Gallery Management)**: Manager quản lý thư viện ảnh.
  - **SCR-33 (Room Status Management)**: Manager cập nhật trạng thái phòng thủ công.
  - **SCR-65 (Property Room List)**: Danh sách phòng dành cho Employee tham khảo.
- **UI Components**: RoomCard, ImageGalleryGrid, AvailabilityCalendar, StickyPriceCard, StatusBadge.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| roomNumber | Room No. | Text | 101 |
| roomType | Type | Chip | Deluxe / Suite |
| pricePerNight | Price / Night | Currency | 1,000,000 VND |
| capacity | Guests | Icon + Text | 2 Guests |
| area | Area | Text | 45 m² |
| status | Status | StatusBadge | Available / Occupied / Maintenance |
| images (RoomImage) | Gallery | ImageGalleryGrid | Primary image first |
| isPrimary (RoomImage) | Primary | Icon | Star icon |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-30 | Create (Room) |
| SCR-07 | Read (List) |
| SCR-08 | Read (Detail) |
| SCR-29 | Read (Manager List) |
| SCR-31 | Update (Room Info) |
| SCR-32 | Create/Delete (RoomImage) |
| SCR-33 | Update (Status) |
| SCR-65 | Read (Employee View) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: roomNumber, description.
- **Filter Options**: property, floor, roomType, capacity, status.
- **Sort Options**: pricePerNight, capacity.

#### 4. Table Mapping
- **Table Columns (SCR-29)**: Room Number, Type, Floor, Price, StatusBadge, Actions (Kebab: Edit, Gallery, Status).
- **Actions**: Edit (→ SCR-31), Gallery (→ SCR-32), Status (→ SCR-33).
- **Pagination**: 10/20 rows per page.

#### 5. Form Mapping
- **Sections (SCR-30)**: Room Info (roomNumber, floor, roomType, capacity, area), Pricing (pricePerNight), Amenities Checkboxes, Description (textarea).
- **Input Components**: TextInput (roomNumber), NumberInput (pricePerNight, capacity, area), Select (floor, roomType), Textarea (description), Checkboxes (amenities).
- **Required Fields**: roomNumber, propertyId, floorId, pricePerNight, capacity.
- **Readonly Fields**: status (managed via SCR-33).
- **Hidden Fields**: propertyId.

#### 6. Action Mapping
- Book Room (Customer — from SCR-08)
- Add/Edit Room (Manager)
- Upload Image / Set Primary Image (Manager — SCR-32)
- Set Maintenance / Out of Service / Available (Manager — SCR-33)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| Available | Green | Success | Book, Edit |
| Pending Deposit | Yellow | Warning | View Booking |
| Reserved | Blue | Info | View Booking |
| Occupied | Purple | Info | Trigger Inspection (before Check-out) |
| Pending Cleaning | Orange | Warning | Assign Housekeeper |
| Cleaning In Progress | Blue | Info | View Task |
| Maintenance | Red | Danger | Edit, Change Status |
| Out Of Service | Gray | Neutral | Change Status |

#### 8. Component Mapping
- RoomCard (`color-surface-card`, `radius-lg`, `shadow-sm`, `hover:shadow-lg`)
- StickyPriceCard (`color-surface-card`, `radius-lg`, `shadow-lg` — SCR-08)
- AvailabilityCalendar (`color-surface-card`, `radius-lg`, `shadow-sm` — SCR-09)
- ImageGalleryGrid (`radius-lg`, overflow-hidden — SCR-08, SCR-32)

#### 9. Responsive Mapping
- **Desktop**: Image gallery shows 1 large hero + 4 small thumbnails (SCR-08).
- **Mobile**: Image gallery becomes a swipeable carousel. Calendar stacks vertically.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-07 | /api/v1/rooms/search | GET |
| SCR-08 | /api/v1/rooms/{id} | GET |
| SCR-30 | /api/v1/managers/rooms | POST |
| SCR-31 | /api/v1/managers/rooms/{id} | PUT |
| SCR-32 | /api/v1/managers/rooms/{id}/images | POST / DELETE |
| SCR-33 | /api/v1/managers/rooms/{id}/status | PUT |
| SCR-65 | /api/v1/employees/rooms | GET |

#### 11. Entity Relationship Mapping
- **→ Property & Floor**: Displayed directly on Room card/detail.
- **→ Bookings**: Used to compute Availability Calendar.
- **→ RoomImages**: Gallery (up to N images, 1 isPrimary).
- **→ Reviews**: Loaded lazily on SCR-08.

#### 12. Derived / Calculated Fields
- `isAvailableForDates(start, end)` = Computed from Booking dates.
- `averageRating` = Computed from PUBLISHED Reviews (shown on SCR-08).

#### 13. Empty / Loading / Error States
- **Loading**: Skeleton Room Cards (SCR-07).
- **Empty**: "No rooms match your search criteria."
- **Gallery Empty (SCR-32)**: "No images uploaded yet."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| roomNumber | Unique within Property | Room number already exists |
| pricePerNight | >= 0 | Price must be positive |
| capacity | > 0 | Capacity must be at least 1 |

#### 15. Permission Mapping
- **Guest / Customer**: Readonly (Available rooms — public list and detail).
- **Manager**: Create, Read, Update, Delete (Within assigned property).
- **Employee**: Readonly (SCR-65 — reference only).

---

### Booking
- **Description**: Hợp đồng đặt phòng, lưu ngày Check-in/out, tổng tiền, cọc, trạng thái, và người đặt. Bao gồm cả damageFeeAmount và holdExpiresAt.
- **Mapped Screens**:
  - **SCR-15 (Customer Dashboard)**: Hiển thị "Upcoming Stay".
  - **SCR-16 (Booking Checkout)**: Form nhập thông tin người lưu trú và yêu cầu đặc biệt.
  - **SCR-20 (Order Review & Payment)**: Xem lại tổng chi phí, nội quy và chọn thanh toán VNPay.
  - **SCR-17 (Booking Management), SCR-18 (Booking Detail)**: Customer quản lý và xem chi tiết đặt phòng.
  - **SCR-19 (Booking Cancellation)**: Customer xem chính sách hoàn **cọc theo tier** (≥7d 100% / 3–7d 50% / &lt;3d 0%) và hủy phòng.
  - **SCR-34 (Booking List), SCR-35 (Booking Detail)**: Manager quản lý danh sách và chi tiết, thực hiện Check-in/Check-out; nút Cancel → SCR-69.
  - **SCR-69 (Manager Booking Cancellation)**: Manager hủy booking (bắt buộc lý do); hoàn **100% cọc**.
- **UI Components**: StickyOrderSummary, BookingCard, StatusBadge, HoldCountdownTimer, Action Buttons.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | Booking ID | Text | BK-XXXXXXXX |
| customer.fullName | Guest Name | Text / Link | John Doe |
| room.roomNumber | Room | Link | Room 101 |
| checkInDate | Check-in | Date | dd/MM/yyyy |
| checkOutDate | Check-out | Date | dd/MM/yyyy |
| guestCount | Guests | Text | 2 Adults |
| totalAmount | Total Price | Currency | 1,500,000 VND |
| depositAmount | Deposit (40%) | Currency | 600,000 VND |
| remainingAmount | Remaining | Currency | 900,000 VND |
| damageFeeAmount | Damage Fee | Currency | 0 VND / 500,000 VND |
| status | Status | StatusBadge | Confirmed |
| holdExpiresAt | Payment Deadline | Countdown Timer | 09:45 remaining |
| createdAt | Booking Time | Date | dd/MM/yyyy HH:mm |
| cancelReason | Cancel Reason | Text | Policy / Guest request |
| specialRequests | Special Requests | Text | "Late check-in" |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-16 | Create |
| SCR-20 | Update (Confirm payment) |
| SCR-17 | Read (Customer List) |
| SCR-18 | Read (Customer Detail) |
| SCR-34 | Read (Manager List) |
| SCR-35 | Read (Manager Detail) |
| SCR-19 | Cancel (Customer — deposit refund by tier ≥7d/3–7d/&lt;3d) |
| SCR-69 | Cancel (Manager — refund 100%) |
| SCR-35 | Check-in (Manager) |
| SCR-35 | Check-out (Manager — after Inspection passed) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: Booking ID, Customer Name, Customer Phone.
- **Filter Options**: Property, Status (Tabs: All / Upcoming / Completed / Cancelled), Check-in Date Range.
- **Sort Options**: Check-in Date (Asc/Desc), Created At (Newest/Oldest), Total Amount.

#### 4. Table Mapping
- **Table Columns (SCR-34)**: Booking ID, Guest, Room, Check-in, Check-out, Total, StatusBadge, Actions.
- **Actions**: View Detail (click row → SCR-35).
- **Bulk Actions**: None.
- **Pagination**: 10/20/50 rows per page.

#### 5. Form Mapping
- **Sections (SCR-16)**: Guest Details (guestCount, specialRequests).
- **Sections (SCR-20)**: Policies Review, Payment Method (VNPay).
- **Sections (SCR-69)**: Booking Summary (readonly), Cancel Reason (required), Refund Preview (100% paid amount).
- **Input Components**: TextInput (guestCount, specialRequests), RadioGroup (paymentMethod), Textarea (cancelReason — SCR-69).
- **Required Fields**: checkInDate, checkOutDate, guestCount, paymentMethod; cancelReason (SCR-69).
- **Readonly Fields**: totalAmount, depositAmount (Computed from PricingRule/Room price); paidAmount / refundAmount on SCR-69.
- **Hidden Fields**: customerId, roomId.
- **Auto-generated Fields**: id, createdAt, holdExpiresAt; cancelledBy / cancelledAt (server-side on cancel).

#### 6. Action Mapping
- Book Room (Customer)
- Pay Deposit — VNPay redirect
- Cancel Booking (Customer — SCR-19, deposit refund by tier ≥7d 100% / 3–7d 50% / &lt;3d 0%)
- Cancel Booking (Manager — SCR-69, refund 100% deposit; cancelReason required)
- Check-in Guest (Manager — SCR-35)
- Check-out Guest (Manager — SCR-35, after Room Inspection passed)
- Resend Confirmation Email (Manager — SCR-35)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| Pending Deposit | Yellow | Warning | Pay Deposit (Countdown timer), Cancel |
| Confirmed | Green | Success | Check-in, Cancel (Customer: deposit tier refund; Manager: 100% deposit) |
| Checked-in | Blue | Info | Check-out (requires Inspection) |
| Pending Inspection | Orange | Warning | View/Trigger Inspection (Manager) |
| Pending Damage Payment | Red | Danger | Pay Damage Fee (Customer) |
| Checked-out | Gray | Neutral | Write Review (Customer) |
| Cancelled | Red | Danger | View Detail only |
| No-show | Dark Red | Danger | View Detail only (Deposit forfeited) |

#### 8. Component Mapping
- BookingTable (`bg-surface-card`, `border-color-border-subtle` — SCR-34)
- BookingCard (`color-surface-card`, `border-color-border-subtle`, `radius-lg` — SCR-17)
- StickyOrderSummary (`color-surface-card`, `radius-lg`, `shadow-md` — SCR-20 right panel)
- StatusBadge (`radius-full`, 10% opacity background)
- HoldCountdownTimer (shown when status = Pending Deposit — SCR-18)

#### 9. Responsive Mapping
- **Desktop**: Table view for SCR-34. Sticky summary on right side for SCR-20.
- **Tablet**: Summary card moves below the checkout form. Table horizontal scrolling.
- **Mobile**: Tables convert to stacked BookingCards. Sticky action buttons fixed at bottom.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-16 | /api/v1/bookings | POST |
| SCR-37 | /api/v1/managers/bookings/{id}/check-in | POST |
| SCR-37 | /api/v1/managers/bookings/{id}/check-out | POST |
| SCR-17 | /api/v1/customers/me/bookings | GET |
| SCR-18 | /api/v1/bookings/{id} | GET |
| SCR-19 | /api/v1/bookings/{id}/cancel | POST |
| SCR-69 | /api/v1/manager/bookings/{id}/cancel | POST |
| SCR-69 | /api/v1/manager/bookings/{id}/cancel/preview | GET |
| SCR-34 | /api/v1/managers/properties/{id}/bookings | GET |
| SCR-35 | /api/v1/bookings/{id} | GET |
| SCR-35 | /api/v1/bookings/{id}/check-in | POST |
| SCR-35 | /api/v1/bookings/{id}/check-out | POST |

#### 11. Entity Relationship Mapping
- **→ Customer (User)**: Loaded lazily.
- **→ Room**: Loaded lazily (room number, type shown on list).
- **→ Payment**: List loaded on Detail view.
- **→ Contract**: Linked/Generated via outbox event after deposit confirmed.
- **→ RoomInspection**: Required before Check-out; 1-to-1 per Booking.
- **→ DamageReport**: Linked if inspection found damage.

#### 12. Derived / Calculated Fields
- `remainingAmount` = `totalAmount` - `depositAmount` + `damageFeeAmount` (if any).
- `totalNights` = `checkOutDate` - `checkInDate`.
- `depositAmount` = `totalAmount` * configurable % (default 40%).

#### 13. Empty / Loading / Error States
- **Loading**: Skeleton rows in Table (SCR-34). Skeleton cards in SCR-17.
- **Empty**: "No bookings found matching your criteria."
- **Hold Expired**: Auto-disable Pay button + "Payment window expired" message (SCR-18).

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| checkInDate | >= Today, < checkOutDate | Invalid Check-in Date |
| checkOutDate | > checkInDate | Invalid Check-out Date |
| guestCount | > 0, <= Room Capacity | Exceeds maximum room capacity |
| paymentMethod | Required | Please select a payment method |

#### 15. Permission Mapping
- **Guest**: Hidden (redirected to SCR-02 on Book attempt).
- **Customer**: Visible (Own bookings only). Can Create, Cancel via SCR-19 (status-dependent; deposit refund by tier).
- **Manager**: Visible (Assigned Property). Can Check-in, Check-out, Cancel via SCR-69 (refund 100%), Resend email.
- **Admin**: Readonly (Global view).

---

### Contract
- **Description**: Hợp đồng lưu trú (Immutable PDF) được sinh ra sau khi thanh toán cọc thành công.
- **Mapped Screens**:
  - **SCR-21 (My Contract List)**: Customer xem danh sách hợp đồng.
  - **SCR-38 (Contract Management)**: Manager xem danh sách, gửi lại email.
- **UI Components**: ContractTable, PDFViewerDrawer.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | Contract ID | Text | CT-XXXXXXXX |
| bookingId | Booking Ref | Link | BK-XXXXXXXX |
| generatedAt | Generated Date | Date | dd/MM/yyyy HH:mm |
| sentAt | Email Sent | Date | dd/MM/yyyy HH:mm |
| pdfUrl | Contract PDF | Link / IFrame | View PDF / Download |
| status | Status | StatusBadge | Active / Completed / Cancelled |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| System | Create (Auto-generated by backend @Async after deposit) |
| SCR-21 | Read (Customer List) |
| SCR-38 | Read (Manager List) |
| SCR-38 | Resend Email (Manager) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: bookingId, customer.fullName.
- **Filter Options**: status, checkInDate.
- **Sort Options**: generatedAt (Desc).

#### 4. Table Mapping
- **Table Columns (SCR-38, SCR-21)**: Contract ID, Booking ID, Guest, Date, Status, Actions.
- **Actions**: View PDF (opens Drawer with iframe), Resend Email (Manager only).

#### 5. Form Mapping
- N/A. Contracts are automatically generated by the backend (Outbox Pattern / @Async).

#### 6. Action Mapping
- View PDF (Customer, Manager)
- Download PDF (Customer, Manager)
- Resend Email (Manager — SCR-38)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| Active | Green | Success | View, Download, Resend Email |
| Completed | Gray | Neutral | View, Download |
| Cancelled | Red | Danger | View only |

#### 8. Component Mapping
- ContractTable (`bg-surface-card` — SCR-38, SCR-21)
- PDFViewerDrawer (Drawer slide-in from right, contains iframe — `bg-surface-card`, `shadow-lg`)

#### 9. Responsive Mapping
- **Desktop**: List on left, Drawer with PDF on right.
- **Mobile**: Drawer takes full screen; fallback to direct PDF download link if iframe fails.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-21 | /api/v1/customers/me/contracts | GET |
| SCR-38 | /api/v1/managers/contracts | GET |
| SCR-38 | /api/v1/contracts/{id}/resend | POST |

#### 11. Entity Relationship Mapping
- **→ Booking**: 1-to-1 (immutable snapshot of booking data).
- **→ Customer**: Loaded lazily.

#### 12. Derived / Calculated Fields
- None. Immutable record.

#### 13. Empty / Loading / Error States
- **Loading**: Spinner inside PDF iframe.
- **Empty**: "No contracts generated yet."
- **PDF Error**: "Unable to load PDF. Please download directly."

#### 14. Validation Mapping
- N/A. PDF generation is handled entirely by backend.

#### 15. Permission Mapping
- **Customer**: Readonly (Own contracts only).
- **Manager**: Readonly, Resend Email (Assigned property contracts).
- **Admin**: Readonly.

---

### Payment & PaymentReceipt
- **Description**: Thông tin thanh toán (Deposit, Remaining Balance, Damage Fee). **Edu: VNPay only (TD-011)**. Bank Transfer / Cash / PaymentReceipt — deferred.
- **Mapped Screens**:
  - **SCR-26 (Payment History)**: Customer xem lịch sử thanh toán VNPay.
  - **SCR-36 (Payment Management)**: Manager xem danh sách payment (read-only) theo Property.
  - **SCR-37 (Check-in / Check-out)**: **Không** phải Payment Verification — xem Booking section endpoints.
  - **SCR-52 (Payment Reconciliation)**: Admin đối soát VNPay — **Deferred edu (TD-010)**.
- **UI Components**: PaymentTable, DiscrepancyTable (SCR-52 deferred).

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | Transaction ID | Text | TXN-XXXXXXXX |
| type | Type | Chip | DEPOSIT / REMAINING_BALANCE / DAMAGE_FEE |
| method | Method | Text | **VNPAY** (edu); BANK_TRANSFER / CASH unused in UI |
| amount | Amount | Currency | 600,000 VND |
| status | Status | StatusBadge | Pending / Paid / Failed / Refunded |
| paidAt | Paid Date | Date | dd/MM/yyyy HH:mm |
| orderRef | Order Ref | Text | VNPay order reference |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-20 | Create (VNPay redirect) |
| SCR-26 | Read (Customer History) |
| SCR-36 | Read (Manager List) |
| SCR-52 | Reconcile (Admin — **TD-010 deferred**) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: id, bookingId, customer.fullName.
- **Filter Options**: type, method, status (Tabs: All / Pending / Paid / Failed / Refunded).
- **Sort Options**: createdAt (Desc).

#### 4. Table Mapping
- **Table Columns (SCR-36)**: Date, Transaction ID, Booking ID, Amount, Method, StatusBadge.
- **Actions**: View detail (read-only). ~~Verify bank transfer → SCR-37~~ removed (TD-011; SCR-37 = check-in/out).

#### 5. Form Mapping
- **Hidden Fields**: paymentId.

#### 6. Action Mapping
- ~~Approve / Reject Payment (Manager CK verify)~~ — **TD-011 deferred**
- Reconcile VNPay Payment (Admin — SCR-52) — **TD-010 deferred**
- Refund Payment (system / Admin per cancel policy)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| Pending | Yellow | Warning | Wait VNPay **Return URL**; retry pay if failed (IPN N/A edu TD-012) |
| Paid | Green | Success | View |
| Failed | Red | Danger | View, Retry Payment (VNPay) |
| Refunded | Blue | Info | View |

#### 8. Component Mapping
- PaymentTable (`bg-surface-card` — SCR-36)
- DiscrepancyTable (`bg-surface-card` — SCR-52 deferred)
- ReconciliationDrawer (manual sync — SCR-52 deferred)

#### 9. Responsive Mapping
- **Desktop / Mobile**: Table + detail Drawer (no receipt split panel in edu).

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-26 | /api/v1/customers/me/payments | GET |
| SCR-36 | /api/v1/managers/payments | GET |
| SCR-37 | /api/v1/managers/bookings/{id}/check-in | POST |
| SCR-37 | /api/v1/managers/bookings/{id}/check-out | POST |
| SCR-52 | /api/v1/admin/payments/reconciliation | GET (deferred TD-010) |

#### 11. Entity Relationship Mapping
- **→ Booking**: Parent; one Booking has multiple Payments (Deposit, Remaining, Damage Fee).
- **→ PaymentReceipt**: ~~Bank Transfer~~ — **N/A edu (TD-011)**.

#### 12. Derived / Calculated Fields
- `totalPaid` = Sum of Paid payments per Booking.
- `isFullyPaid` = totalPaid >= totalAmount + damageFeeAmount.

#### 13. Empty / Loading / Error States
- **Loading**: Skeleton Table.
- **Empty (SCR-26)**: "No payment history found."
- **API Error**: Toast "Failed to load payments."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| amount | Must match expected deposit/remaining snapshot | Amount mismatch |
| method | Edu: VNPAY only | Unsupported payment method |

#### 15. Permission Mapping
- **Customer**: Create (VNPay pay attempt), Read (Own payments).
- **Manager**: Read (Assigned property payments). ~~Verify CK~~ deferred TD-011.
- **Admin**: Read. Reconcile (SCR-52) — **deferred TD-010**.

---

### MaintenanceTicket
- **Description**: Yêu cầu bảo trì, sửa chữa từ khách hàng, gán cho Employee. Phải liên kết với một booking hiện tại. Quy trình: Open → Assigned → In Progress → Resolved → Closed.
- **Mapped Screens**:
  - **SCR-22 (Maintenance Ticket List), SCR-23 (Create Maintenance Ticket)**: Customer xem và tạo yêu cầu.
  - **SCR-41 (Maintenance Tasks)**: Manager xem danh sách và gán Employee (via Drawer).
  - **SCR-61 (Maintenance Workspace)**: Employee xem và cập nhật trạng thái bảo trì.
- **UI Components**: StatusBadge, MaintenanceTable, MaintenanceDrawer, MobileListItem.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | Ticket ID | Text | MT-XXXXXXXX |
| title | Issue | Text | Broken AC |
| description | Description | Text | Full issue details |
| room.roomNumber | Room | Link | Room 101 |
| assignedEmployee.fullName | Assigned To | Text | Jane D. |
| status | Status | StatusBadge | Open / Assigned / In Progress / Resolved / Closed |
| resolutionNote | Resolution | Text | Fixed by replacing part |
| attachments | Photos | AttachmentThumbnailGrid | Up to 5 thumbnails |
| createdAt | Submitted | Date | dd/MM/yyyy |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-23 | Create |
| SCR-22 | Read (Customer own list) |
| SCR-41 | Read (Manager list), Assign Employee (via Drawer) |
| SCR-61 | Read (Employee assigned list), Update Status |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: title, id.
- **Filter Options**: status, room.
- **Sort Options**: createdAt (Desc).

#### 4. Table Mapping
- **Table Columns (SCR-41)**: Room, Issue, Reporter, Assigned To, StatusBadge, Actions.
- **Actions**: Click Row → Drawer (view attachments, assign employee, update status).
- **SCR-22 Columns**: Ticket ID, Issue, StatusBadge, Created Date.

#### 5. Form Mapping
- **Sections (SCR-23)**: Issue Details (title, description), Evidence (image uploads).
- **Input Components**: TextInput (title), Textarea (description), MultiFileUpload (attachments).
- **Required Fields**: title, roomId, bookingId.
- **Hidden Fields**: customerId, bookingId (from active booking context).

#### 6. Action Mapping
- Create Request (Customer — SCR-23)
- Edit / Delete Request (Customer — only when status = Open)
- Assign Employee (Manager — via Drawer in SCR-41)
- Start Work (Employee — SCR-61, changes to In Progress)
- Finish Work (Employee — SCR-61, changes to Resolved)
- Verify & Close Ticket (Manager — SCR-41)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| Open | Red | Danger | Assign (Manager), Edit/Delete (Customer) |
| Assigned | Blue | Info | Start Work (Employee) |
| In Progress | Blue | Info | Finish Work (Employee) |
| Resolved | Green | Success | Verify & Close (Manager) |
| Closed | Gray | Neutral | View only |

#### 8. Component Mapping
- MaintenanceTable (`bg-surface-card` — SCR-41)
- MaintenanceDrawer (slide-in from right, shows photos + assignment — SCR-41)
- MobileListItem (`bg-surface-card`, `border-color-border-subtle` — SCR-61)
- AttachmentThumbnailGrid (SCR-41 Drawer, SCR-23)

#### 9. Responsive Mapping
- **Desktop**: Table + Drawer layout (SCR-41).
- **Employee Workspace Mobile (SCR-61)**: Large Start/Finish buttons (touch-friendly, min-height 48px). Swipe gestures.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-23 | /api/v1/maintenance | POST |
| SCR-22 | /api/v1/customers/me/maintenance | GET |
| SCR-41 | /api/v1/managers/maintenance | GET |
| SCR-41 | /api/v1/managers/maintenance/{id}/assign | POST |
| SCR-61 | /api/v1/employees/maintenance | GET |
| SCR-61 | /api/v1/employees/maintenance/{id}/status | PUT |

#### 11. Entity Relationship Mapping
- **→ Room**: Target location.
- **→ Customer (User)**: Submitter.
- **→ Employee (User)**: Assignee.
- **→ Booking**: Required linkage.
- **→ Attachments**: Polymorphic photo list (EntityType = "Maintenance").

#### 12. Derived / Calculated Fields
- `timeToResolve` = `resolvedAt` - `createdAt`.

#### 13. Empty / Loading / Error States
- **Empty (SCR-22)**: "No maintenance requests submitted."
- **Empty (SCR-61)**: "No maintenance tasks assigned to you."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| title | Required, Max 200 chars | Title is required |
| roomId | Required, from active booking | Please select a room from your current booking |
| attachments | Max 5 files, < 5MB each | Maximum 5 photos allowed, each under 5MB |

#### 15. Permission Mapping
- **Customer**: Create, Read (Own). Edit/Delete (Own, status = Open only).
- **Manager**: Read, Assign, Verify, Close (Assigned property).
- **Employee**: Read (Assigned only), Update Status.

---

### Review
- **Description**: Đánh giá và nhận xét của khách hàng sau khi Check-out. Liên kết 1-to-1 với Booking.
- **Mapped Screens**:
  - **SCR-24 (My Reviews)**: Customer xem các đánh giá mình đã viết.
  - **SCR-25 (Review & Rating)**: Customer viết đánh giá.
  - **SCR-08 (Room Detail)**: Hiển thị các đánh giá public.
  - **SCR-56 (System Administration — Content Moderation tab)**: Admin ẩn/publish đánh giá vi phạm.
- **UI Components**: StarRatingInput, ReviewCardList, ReviewFormCard.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | Review ID | Hidden | UUID |
| customer.fullName | Reviewer | Text | John D. |
| rating | Rating | StarRating | 5/5 Stars (`text-warning`) |
| comment | Comment | Text | "Great room..." |
| status | Status | Badge | Published / Hidden |
| createdAt | Date | Date | dd/MM/yyyy |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-25 | Create |
| SCR-08 | Read (Public List — PUBLISHED only) |
| SCR-24 | Read (Customer own list) |
| SCR-25 | Update / Delete (Customer own) |
| SCR-56 | Update (Hide/Publish — Admin) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: comment (Admin moderation).
- **Filter Options**: rating (1–5 stars), room, status.
- **Sort Options**: createdAt (Desc — Newest First).

#### 4. Table Mapping
- **Table Columns (Admin Moderation — SCR-56 tab)**: Room, Customer, Rating, Comment, Status, Actions.
- **Actions**: Hide / Publish.

#### 5. Form Mapping
- **Sections (SCR-25)**: Rating (StarRatingInput), Comment (Textarea).
- **Input Components**: StarRatingInput (interactive 5-star, `text-warning`), Textarea (comment).
- **Required Fields**: rating.
- **Hidden Fields**: bookingId, roomId, customerId.

#### 6. Action Mapping
- Submit Review (Customer — once per booking, status = Checked-out)
- Edit Review (Customer — own review)
- Delete Review (Customer — own review)
- Hide Review (Admin/Manager)
- Publish Review (Admin/Manager)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| PUBLISHED | Green | Success | Hide (Admin/Manager) |
| HIDDEN | Gray | Neutral | Publish (Admin/Manager) |

#### 8. Component Mapping
- StarRatingInput (interactive — SCR-25)
- StarRatingDisplay (readonly — SCR-08, SCR-24)
- ReviewCardList (`color-surface-card`, `radius-md`, `border-color-border-subtle` — SCR-08, SCR-24)
- ReviewFormCard (`color-surface-card`, `radius-lg` — SCR-25)

#### 9. Responsive Mapping
- **Mobile**: Star rating fits mobile width, full-width textarea, submit button spans full width.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-08 | /api/v1/rooms/{id}/reviews | GET |
| SCR-24 | /api/v1/customers/me/reviews | GET |
| SCR-25 | /api/v1/reviews | POST |
| SCR-25 | /api/v1/reviews/{id} | PUT / DELETE |
| SCR-56 | /api/v1/admin/reviews/{id}/status | PUT |

#### 11. Entity Relationship Mapping
- **→ Booking**: 1-to-1 (must be status Checked-out; one review per booking).
- **→ Room**: Target of review; `averageRating` computed from PUBLISHED reviews.

#### 12. Derived / Calculated Fields
- `averageRating` on Room = Average of all PUBLISHED review ratings.

#### 13. Empty / Loading / Error States
- **Empty (SCR-08)**: "No reviews yet. Be the first to stay!"
- **Empty (SCR-24)**: "You haven't written any reviews yet."
- **Business Error**: "You can only write a review after check-out is complete."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| rating | 1 to 5 | Please provide a rating (1–5 stars) |
| bookingId | Must have status Checked-out | You can only review after check-out |
| bookingId | Not already reviewed | You have already submitted a review for this booking |

#### 15. Permission Mapping
- **Guest**: Read (PUBLISHED only — SCR-08).
- **Customer**: Create (Checked-out, once per booking), Read own, Edit own, Delete own.
- **Manager**: Read, Hide/Publish.
- **Admin**: Read, Hide/Publish (SCR-56).

---

### Notification
- **Description**: Thông báo hệ thống gửi đến người dùng (WebSocket real-time + persistent list). Hỗ trợ liên kết điều hướng nhanh.
- **Mapped Screens**:
  - **SCR-13 (Notification Center)**: Trang danh sách thông báo.
  - **SCR-14 (Notification Detail)**: Chi tiết một thông báo.
- **UI Components**: NotificationBell (Header), Unread Badge, NotificationListItem.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| title | Title | Bold Text (`text-heading-sm`) | Booking Confirmed |
| content | Message | Text (`text-body-sm`) | Your booking for Room 101... |
| type | Type Icon | Icon | Bell / Check / Warning |
| isRead | Unread Dot | Colored Dot | `color-primary-base` dot if false |
| createdAt | Time | Relative Time | 2 hours ago |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| System | Create (Backend @Async only) |
| SCR-13 | Read (List) |
| SCR-14 | Read (Detail) |
| SCR-13 | Update (Mark as Read / Mark All as Read) |

#### 3. Search / Filter / Sort Mapping
- **Filter Options**: isRead (All / Unread).
- **Sort Options**: createdAt (Always Desc).

#### 4. Table Mapping
- N/A. Displayed as a ListView of NotificationListItems.

#### 5. Form Mapping
- N/A. System-generated; no user creation.

#### 6. Action Mapping
- Click Notification (navigates to related entity via relatedEntityId/relatedEntityType)
- Mark as Read (individual)
- Mark All as Read

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| Unread (isRead=false) | Dot | `color-primary-base` | Click to mark read |
| Read (isRead=true) | None | N/A | View |

#### 8. Component Mapping
- NotificationBell (Header — shows unreadCount badge)
- NotificationDropdownMenu (Desktop header dropdown)
- NotificationListItem (`color-surface-card`, `border-color-border-subtle`, hover: `bg-surface-canvas`)

#### 9. Responsive Mapping
- **Desktop**: Dropdown from Header Bell icon.
- **Mobile**: Full-page Notification Center (SCR-13).

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-13 | /api/v1/notifications | GET |
| SCR-13 | /api/v1/notifications/read-all | POST |
| SCR-14 | /api/v1/notifications/{id} | GET |
| SCR-14 | /api/v1/notifications/{id}/read | POST |

#### 11. Entity Relationship Mapping
- **→ User**: Recipient (userId).
- **→ Related Entity**: Dynamic link via `relatedEntityId` + `relatedEntityType`.

#### 12. Derived / Calculated Fields
- `unreadCount` = Count of `isRead == false` for current user (shown on Bell badge).

#### 13. Empty / Loading / Error States
- **Empty**: "You're all caught up! No new notifications."

#### 14. Validation Mapping
- N/A.

#### 15. Permission Mapping
- **All Authenticated Users**: Read, Update (Own notifications only).

---

### Complaint
- **Description**: Khiếu nại, phản hồi tiêu cực từ Guest hoặc Customer. Quy trình: Open → Investigating → Resolved → Closed.
- **Mapped Screens**:
  - **SCR-54 (Complaint Management)**: Admin/Manager xem danh sách, theo dõi và giải quyết (via Drawer).
  - **SCR-67 (My Complaint List)**: Customer xem danh sách khiếu nại cá nhân kèm trạng thái và resolution notes.
  - **SCR-68 (Create Complaint)**: Customer gửi khiếu nại mới (subject + description).
- **UI Components**: ComplaintTable, ComplaintDrawer (Admin/Manager); ComplaintCardList, ComplaintForm (Customer).

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | ID | Text | CMP-XXXX |
| userId / customer.fullName | Submitter | Text | John Doe / Guest |
| subject | Subject | Text | Dirty Room |
| description | Details | Text | Full complaint details |
| status | Status | StatusBadge | Open / Investigating / Resolved / Closed |
| resolutionNotes | Resolution | Text | "Issue addressed by..." |
| createdAt | Submitted | Date | dd/MM/yyyy |
| resolvedAt | Resolved At | Date | dd/MM/yyyy |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-68 | Create (Customer portal) |
| SCR-67 | Read (Customer — own complaints list) |
| SCR-54 | Read (Admin/Manager List & Detail via Drawer) |
| SCR-54 | Update (Status: Investigate / Resolve / Close) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: subject, customer.fullName.
- **Filter Options**: status.
- **Sort Options**: createdAt (Desc).

#### 4. Table Mapping
- **Table Columns (SCR-54)**: Ticket ID, Customer, Subject, StatusBadge, Submitted Date, Actions.
- **Actions**: Click Row → Drawer (view details, update status).

#### 5. Form Mapping
- **Sections (SCR-54 Resolve Drawer)**: Resolution Notes, Status Select.
- **Input Components**: Textarea (resolutionNotes), StatusSelect.
- **Required Fields**: resolutionNotes (when Resolving).

#### 6. Action Mapping
- Submit Complaint (Customer/Guest)
- Mark Investigating (Admin)
- Resolve Complaint (Admin)
- Close Complaint (Admin)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| Open | Red | Danger | Mark Investigating (Admin) |
| Investigating | Yellow | Warning | Resolve (Admin) |
| Resolved | Green | Success | Close (Admin) |
| Closed | Gray | Neutral | View only |

#### 8. Component Mapping
- ComplaintTable (`bg-surface-card` — SCR-54)
- ComplaintDrawer (slide-in from right, details + resolution form — SCR-54)

#### 9. Responsive Mapping
- **Desktop**: Table + Drawer split screen (SCR-54).
- **Mobile**: Drawer takes full screen.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-68 | /api/complaints | POST |
| SCR-67 | /api/complaints | GET (own list) |
| SCR-54 | /api/manager/complaints | GET |
| SCR-54 | /api/manager/complaints/{id} | GET |
| SCR-54 | /api/complaints/{id}/status | PATCH |

#### 11. Entity Relationship Mapping
- **→ User (Customer)**: Optional (null if submitted by Guest without account).

#### 12. Derived / Calculated Fields
- None.

#### 13. Empty / Loading / Error States
- **Empty**: "No complaints. Great job!"

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| subject | Required, Max 200 chars | Please enter a subject |
| description | Required | Please describe your complaint |

#### 15. Permission Mapping
- **Customer**: Create (SCR-68), Read own list (SCR-67).
- **Admin / Manager**: Read, Update (status transitions — SCR-54).

---

### ActivityLog
- **Description**: Nhật ký hoạt động người dùng — tự động ghi lại các hành động quan trọng trong hệ thống.
- **Mapped Screens**:
  - **SCR-56 (System Administration — Activity Logs tab)**: Admin xem Activity Logs.
- **UI Components**: ActivityLogTable.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| user.fullName | User | Text | Admin Name |
| action | Action | Text | BOOKING_CHECKED_IN |
| entityType | Entity | Text | Booking |
| entityId | Entity Ref | Link | BK-XXXXXXXX |
| details | Details | Text | Additional context |
| createdAt | Time | Date | dd/MM/yyyy HH:mm |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| System | Create (Auto on tracked actions) |
| SCR-56 | Read (Admin — Activity Logs tab) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: user.fullName, action.
- **Filter Options**: action type, date range.
- **Sort Options**: createdAt (Desc).

#### 4. Table Mapping
- **Table Columns (SCR-56)**: Time, User, Action, Entity, Details.
- **Actions**: None (readonly).

#### 5. Form Mapping
- N/A. Auto-generated by system.

#### 6. Action Mapping
- View Log (Admin)

#### 7. Status Mapping
- N/A.

#### 8. Component Mapping
- ActivityLogTable (`bg-surface-card` — SCR-56, Activity Logs tab)

#### 9. Responsive Mapping
- **Desktop**: Tabbed layout (SCR-56), horizontal scroll for table.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-56 | /api/v1/admin/logs | GET |

#### 11. Entity Relationship Mapping
- **→ User**: Actor who performed the action.
- **→ Related Entity**: Any entity referenced by entityType + entityId.

#### 12. Derived / Calculated Fields
- None.

#### 13. Empty / Loading / Error States
- **Empty**: "No recent activity."

#### 14. Validation Mapping
- N/A.

#### 15. Permission Mapping
- **Admin**: Read only (SCR-56 — Activity Logs tab).

---

### SystemSetting
- **Description**: Cấu hình hệ thống chung (deposit %, hold timeout, bank account info, penalty rates).
- **Mapped Screens**:
  - **SCR-56 (System Administration — System Settings tab)**: Admin xem và thay đổi System Settings.
- **UI Components**: SettingsTable, Inline Edit Input.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| key | Setting | Text | DEPOSIT_PERCENTAGE |
| value | Value | TextInput (editable inline) | 40 |
| description | Description | Text | "Deposit amount required..." |
| updatedAt | Last Updated | Date | dd/MM/yyyy HH:mm |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-56 | Read (Settings tab), Update (inline) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: key.
- **Sort Options**: key (A-Z).

#### 4. Table Mapping
- **Table Columns (SCR-56)**: Setting Name, Value, Description, Last Updated, Actions.
- **Actions**: Edit Setting (inline).

#### 5. Form Mapping
- **Input Components (SCR-56)**: Inline TextInput (value), Save button per row.
- **Required Fields**: value.
- **Readonly Fields**: key, description (informational).

#### 6. Action Mapping
- Update Setting (Admin)

#### 7. Status Mapping
- N/A.

#### 8. Component Mapping
- SettingsTable (`bg-surface-card` — SCR-56, System Settings tab)

#### 9. Responsive Mapping
- **Desktop**: Tabs for switching between Activity Logs, Settings, Content Moderation.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-56 | /api/v1/admin/settings | GET |
| SCR-56 | /api/v1/admin/settings/{key} | PUT |

#### 11. Entity Relationship Mapping
- **→ ActivityLog**: Each setting update is tracked in ActivityLog.

#### 12. Derived / Calculated Fields
- None.

#### 13. Empty / Loading / Error States
- **Empty**: "No settings configured."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| value (DEPOSIT_PERCENTAGE) | 1–100, numeric | Invalid percentage value |
| value (general) | Format depends on setting key | Invalid format for this setting |

#### 15. Permission Mapping
- **Admin**: Read, Update only.

---

### Promotion
- **Description**: Quản lý banner khuyến mãi hiển thị ngoài trang chủ (Landing Page).
- **Mapped Screens**:
  - **SCR-01 (Landing Page)**: Hiển thị Hero Banner Carousel cho Guest.
  - **SCR-57 (Promotion Management)**: Admin quản lý danh sách banner.
  - **SCR-58 (Add / Edit Promotion)**: Admin tạo và chỉnh sửa banner.
- **UI Components**: HeroBannerCarousel, PromotionGrid, PromotionForm.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | ID | Hidden | UUID |
| subtitle | Subtitle | Text | "Weekend Sale" |
| title | Title | Heading | "20% Off" |
| description | Description | Text | "Valid for all rooms..." |
| ctaText | Button Text | Button | "Book Now →" |
| ctaUrl | Action URL | Link | /rooms?promo=xxx |
| colorTheme | Theme | ColorBox | Red / Blue / Green |
| isActive | Active | Switch/Badge | On / Off |
| sortOrder | Order | Text | 1, 2, 3 |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-58 | Create / Update |
| SCR-57 | Read (Admin Grid) |
| SCR-01 | Read (Public — Active only, ordered by sortOrder) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: title.
- **Filter Options**: isActive.
- **Sort Options**: sortOrder (Asc).

#### 4. Table Mapping
- **SCR-57 Display**: Grid of Promo Cards (`bg-surface-card`, `radius-lg`, `shadow-sm`).
- **Actions**: Edit (→ SCR-58), Toggle Active.

#### 5. Form Mapping
- **Sections (SCR-58)**: Promotion Details.
- **Input Components**: TextInput (title, subtitle, ctaText, ctaUrl), Textarea (description), Select (colorTheme), NumberInput (sortOrder), Switch (isActive), ImageUpload (banner image).
- **Required Fields**: title, subtitle, ctaText, ctaUrl, colorTheme.
- **Hidden Fields**: id.

#### 6. Action Mapping
- Add Promotion (Admin — SCR-58)
- Edit Promotion (Admin — SCR-58)
- Toggle Active (Admin — inline in SCR-57)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| isActive: true | Green | Success | Toggle Off, Edit |
| isActive: false | Gray | Neutral | Toggle On, Edit |

#### 8. Component Mapping
- HeroBannerCarousel (SCR-01 — public display)
- PromotionGrid (`bg-surface-card`, `radius-lg`, `shadow-sm` — SCR-57)
- PromotionForm (`color-surface-card` — SCR-58)

#### 9. Responsive Mapping
- **Mobile**: Hero Banner text scales down, CTA button spans full width.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-01 | /api/v1/promotions/active | GET |
| SCR-57 | /api/v1/admin/promotions | GET |
| SCR-58 | /api/v1/admin/promotions | POST |
| SCR-58 | /api/v1/admin/promotions/{id} | PUT |
| SCR-57 | /api/v1/admin/promotions/{id} | DELETE |

#### 11. Entity Relationship Mapping
- None. Standalone entity.

#### 12. Derived / Calculated Fields
- None.

#### 13. Empty / Loading / Error States
- **Empty (SCR-01)**: Fallback to default static hero section.
- **Empty (SCR-57)**: "No promotions created."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| title | Required, Max 200 chars | Title is required |
| ctaUrl | Required, valid URL | Please enter a valid URL |
| sortOrder | Integer >= 1 | Order must be a positive number |

#### 15. Permission Mapping
- **Guest / Customer**: Read (Active only — SCR-01).
- **Admin**: Create, Read, Update, Delete.

---

### HousekeepingTask
- **Description**: Công việc dọn phòng, sinh tự động sau khi Booking → Checked-out (và Room Inspection hoàn tất). Quy trình: Pending → In Progress → Completed / Cancelled.
- **Mapped Screens**:
  - **SCR-40 (Housekeeping Tasks)**: Manager gán Employee, xem board trạng thái (via Drawer).
  - **SCR-60 (Housekeeping Workspace)**: Employee nhận việc và đánh dấu hoàn thành.
- **UI Components**: HousekeepingBoardCard, HousekeepingDrawer, MobileListItem.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| room.roomNumber | Room | Text | 101 |
| assignedEmployee.fullName | Maid | Text | Jane D. |
| status | Status | StatusBadge | Pending / In Progress / Completed / Cancelled |
| note | Note | Text | Special instruction |
| startedAt | Started | Time | 14:00 |
| completedAt | Completed | Time | 14:45 |
| createdAt | Created | Date | dd/MM/yyyy |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| System | Create (Auto after Checked-out) |
| SCR-40 | Read (Manager Board), Assign Employee |
| SCR-60 | Read (Employee List), Update Status (Start/Finish) |

#### 3. Search / Filter / Sort Mapping
- **Filter Options**: status, room, assigned employee.
- **Sort Options**: createdAt (Desc), priority.

#### 4. Table Mapping
- **SCR-40 Display**: Board (To Do / In Progress / Done) with Task Cards (`bg-surface-card`, `border-color-border-base`, `radius-md`).
- **Actions**: Click Task Card → Drawer (assign employee, view details).

#### 5. Form Mapping
- **Input Components (Drawer SCR-40)**: Select Employee (dropdown).
- **Input Components (SCR-60)**: Single action button per task (Start / Finish).
- **Required Fields**: employeeId (when assigning).

#### 6. Action Mapping
- Assign Task to Employee (Manager — SCR-40)
- Start Cleaning (Employee — SCR-60; Room → Cleaning In Progress)
- Finish Cleaning (Employee — SCR-60; Room → Available)
- Cancel Task (Manager)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| Pending | Yellow | Warning | Assign Employee (Manager), Start (Employee) |
| In Progress | Blue | Info | Finish (Employee) |
| Completed | Green | Success | View only |
| Cancelled | Gray | Neutral | View only |

#### 8. Component Mapping
- HousekeepingBoardCard (`bg-surface-card`, `border-color-border-base`, `radius-md` — SCR-40)
- HousekeepingDrawer (assign + details — SCR-40)
- MobileListItem (`bg-surface-card`, `border-color-border-subtle`, `padding space-4` — SCR-60)

#### 9. Responsive Mapping
- **Desktop**: Kanban board layout (SCR-40).
- **Employee Mobile (SCR-60)**: List view. Swipe right to mark "In Progress", swipe left to mark "Done". Large Start/Finish buttons (min-height 48px).

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-40 | /api/v1/managers/housekeeping | GET |
| SCR-40 | /api/v1/managers/housekeeping/{id}/assign | POST |
| SCR-60 | /api/v1/employees/housekeeping | GET |
| SCR-60 | /api/v1/employees/housekeeping/{id}/start | POST |
| SCR-60 | /api/v1/employees/housekeeping/{id}/finish | POST |

#### 11. Entity Relationship Mapping
- **→ Room**: Target (status updated automatically on Start/Finish).
- **→ Employee (User)**: Assignee.
- **→ Booking**: Triggered by Booking checkout.

#### 12. Derived / Calculated Fields
- `duration` = `completedAt` - `startedAt`.

#### 13. Empty / Loading / Error States
- **Empty (SCR-60)**: "No rooms assigned to clean."
- **Empty (SCR-40)**: "No housekeeping tasks today."

#### 14. Validation Mapping
- None. Auto-generated; assignment requires valid Employee from same property.

#### 15. Permission Mapping
- **Manager**: Read, Update (Assign Employee, Cancel Task).
- **Employee**: Read (Assigned only), Update (Start/Finish).

---

### Attachment
- **Description**: Tệp đính kèm dùng chung (polymorphic) cho MaintenanceTicket và DamageItem. Thay thế trường photoUrls trước đây.
- **Mapped Screens**:
  - **SCR-23 (Create Maintenance Ticket)**: Customer upload ảnh minh họa.
  - **SCR-41 (Maintenance Tasks)**: Manager xem ảnh trong Drawer.
  - **SCR-64 (Create Damage Report)**: Employee upload ảnh hư hại (camera native).
  - **SCR-43 (Damage Report Management)**: Manager xem ảnh trong Drawer.
- **UI Components**: AttachmentThumbnailGrid, DamageEvidenceGallery, CameraUploadButton.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | Attachment ID | Hidden | UUID |
| entityType | Context | Text | Maintenance / DamageItem |
| fileUrl | File | Image Thumbnail | Clickable → full preview |
| fileName | File Name | Text | photo1.jpg |
| fileSize | File Size | Text | 2.3 MB |
| uploadedAt | Uploaded | Date | dd/MM/yyyy HH:mm |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-23 | Create (Upload with Maintenance Ticket) |
| SCR-64 | Create (Upload with Damage Report) |
| SCR-41 | Read (View in Drawer) |
| SCR-43 | Read (View in Drawer) |

#### 3. Search / Filter / Sort Mapping
- N/A. Always loaded with parent entity.

#### 4. Table Mapping
- N/A. Displayed as ThumbnailGrid or Gallery.

#### 5. Form Mapping
- **Input Components**: MultiFileUpload (`dashed border-color-border-base`, drag & drop), Camera capture (HTML5 `input capture` — SCR-64).
- **Required Fields**: At least 1 file when creating Damage Report.
- **Constraints**: Max 5 files, < 5MB each, JPG/PNG/PDF.

#### 6. Action Mapping
- Upload Attachment (Customer/Employee)
- Delete Attachment (Customer — own, before submission)
- View Full Preview (Manager/Employee via Gallery)

#### 7. Status Mapping
- N/A.

#### 8. Component Mapping
- AttachmentThumbnailGrid (`radius-md`, overflow-hidden, delete overlay on hover)
- DamageEvidenceGallery (fullscreen preview — SCR-43 Drawer, SCR-63)
- CameraUploadButton (HTML5 `input capture`, touch-friendly min-height 48px — SCR-64)

#### 9. Responsive Mapping
- **Mobile (SCR-64)**: Camera integration for direct photo capture via device camera.
- **Desktop**: Drag & drop upload area.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-23 | /api/v1/attachments (entityType=Maintenance) | POST |
| SCR-64 | /api/v1/attachments (entityType=DamageItem) | POST |

#### 11. Entity Relationship Mapping
- **→ MaintenanceTicket**: Polymorphic (entityType=Maintenance).
- **→ DamageItem**: Polymorphic (entityType=DamageItem).

#### 12. Derived / Calculated Fields
- None.

#### 13. Empty / Loading / Error States
- **Empty**: "No photos attached."
- **Upload Error**: "File too large" / "Invalid file type."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| file | JPG/PNG/PDF, < 5MB | Invalid file type or exceeds 5MB |
| count | Max 5 files per entity | Maximum 5 attachments allowed |

#### 15. Permission Mapping
- **Customer**: Upload (for Maintenance), View own.
- **Employee**: Upload (for Damage Report), View (assigned tasks).
- **Manager**: Read (via Drawer in SCR-41, SCR-43).

---

### RoomInspection
- **Description**: Kiểm tra phòng trước Check-out do Employee thực hiện. Check-out bị chặn cho đến khi Inspection hoàn tất. Quy trình: Pending → In Progress → Passed / Failed With Damage.
- **Mapped Screens**:
  - **SCR-42 (Inspection Management)**: Manager xem danh sách inspections, kết quả (via Drawer).
  - **SCR-62 (Room Inspection Hub)**: Employee thực hiện kiểm tra phòng (Checklist Drawer/Modal).
- **UI Components**: InspectionTable, InspectionDrawer, ChecklistModal, ResultBadge.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | Inspection ID | Text | INS-XXXXXXXX |
| room.roomNumber | Room | Text | 101 |
| bookingId | Booking | Link | BK-XXXXXXXX |
| inspectedBy.fullName | Inspector | Text | Employee Name |
| status | Result | ResultBadge | Pending / In Progress / Passed / Failed With Damage |
| note | Note | Text | Inspection notes |
| inspectedAt | Inspected At | Date | dd/MM/yyyy HH:mm |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| System | Create (Auto when Booking → Pending Inspection) |
| SCR-42 | Read (Manager list + detail via Drawer) |
| SCR-42 | Assign / Reassign Inspector (Manager) |
| SCR-62 | Read (Employee assigned list), Update (Perform inspection) |

#### 3. Search / Filter / Sort Mapping
- **Filter Options**: status (Pending / Passed / Failed With Damage), room.
- **Sort Options**: createdAt (Desc), inspectedAt.

#### 4. Table Mapping
- **Table Columns (SCR-42)**: Room, Booking, Inspector, Result, Inspected At, Actions.
- **Result Display**: `color-success` (Passed), `color-danger` (Failed With Damage).
- **Actions**: Click Row → Drawer (view inspection log, damage items if any).

#### 5. Form Mapping
- **Input Components (SCR-62 Checklist)**: Toggle checkboxes load từ `GET /api/v1/employees/inspections/checklist-items` (`ChecklistItemDefinition`); submit `answers[{ itemId, passed }]` → lưu `InspectionChecklistAnswer`. Textarea (note), Pass/Fail buttons.
- **Required Fields**: status result (Pass or Fail).

#### 6. Action Mapping
- Start Inspection (Employee — SCR-62)
- Pass Inspection (Employee — `button-success` "Pass"; Booking eligible for Check-out)
- Fail Inspection (Employee — `button-danger` "Fail"; prompts Damage Report → SCR-64)
- Assign / Reassign Inspector (Manager — SCR-42 Drawer)
- View Inspection Log (Manager — SCR-42 Drawer)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| Pending | Yellow | Warning | Start Inspection (Employee) |
| In Progress | Blue | Info | Pass / Fail (Employee) |
| Passed | Green | Success | Proceed with Check-out (unblocked) |
| Failed With Damage | Red | Danger | Create Damage Report (→ SCR-64) |

#### 8. Component Mapping
- InspectionTable (`bg-surface-card` — SCR-42)
- InspectionDrawer (detail log — SCR-42, slide-in from right)
- ChecklistModal (`bg-surface-card`, `radius-lg`, Checkbox items `radius-sm` — SCR-62)
- ResultBadge (`color-success` / `color-danger`)

#### 9. Responsive Mapping
- **Desktop**: Table + Drawer layout (SCR-42).
- **Mobile (SCR-62)**: Full-screen checklist. Pass/Fail buttons large and touch-friendly (min-height 48px).

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-62 | /api/v1/employees/inspections | GET |
| SCR-62 | /api/v1/employees/inspections/checklist-items | GET |
| SCR-62 | /api/v1/employees/inspections/{id}/pass | POST |
| SCR-62 | /api/v1/employees/inspections/{id}/fail | POST |
| SCR-42 | /api/v1/manager/inspections | GET |
| SCR-42 | /api/v1/manager/inspections/{id}/assign | PATCH |
| SCR-42 | /api/v1/manager/inspections/{id}/checklist-answers | GET |

#### 11. Entity Relationship Mapping
- **→ Booking**: 1-to-1 (one RoomInspection per Booking).
- **→ Room**: Target room being inspected.
- **→ Employee (User)**: Inspector (inspectedBy).
- **→ DamageReport**: Created if status = Failed With Damage.
- **→ InspectionChecklistAnswer**: Kết quả từng mục checklist (qua ChecklistItemDefinition).

#### 12. Derived / Calculated Fields
- `inspectionDuration` = `inspectedAt` - `createdAt`.

#### 13. Empty / Loading / Error States
- **Empty (SCR-62)**: "No rooms ready for inspection."
- **Business Error**: "Check-out is blocked until Room Inspection is completed."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| status | Must reach Passed or Failed With Damage | Please complete the inspection |
| note | Required when Failing | Please describe the damage found |

#### 15. Permission Mapping
- **Employee**: Read (Assigned inspections only), Update (Perform — own property).
- **Manager**: Read, Assign/Reassign Inspector (Assigned property — SCR-42).
- **Admin**: Readonly.

---

### DamageReport & DamageItem
- **Description**: Ghi nhận hư hại phòng sau khi Room Inspection = Failed With Damage. Quy trình: Draft → Pending Approval → Approved / Disputed → Paid. Nếu fee > 5M VNĐ, yêu cầu Admin đồng phê duyệt.
- **Mapped Screens**:
  - **SCR-64 (Create Damage Report)**: Employee tạo báo cáo hư hại kèm DamageItems và ảnh.
  - **SCR-63 (Damage Report List)**: Employee xem danh sách báo cáo đã tạo.
  - **SCR-43 (Damage Report Management)**: Manager duyệt báo cáo bồi thường (via Drawer).
  - **SCR-53 (Damage Escalation)**: Admin duyệt bồi thường lớn > 5M VNĐ.
- **UI Components**: DamageReportTable, DamageEvidenceGallery, DynamicItemForm, DamageApprovalDrawer.

#### 1. Display Mapping
| Entity Field | UI Label | UI Component | Display Format |
| ------------ | -------- | ------------ | -------------- |
| id | Report ID | Text | DR-XXXXXXXX |
| room.roomNumber | Room | Text | 101 |
| bookingId | Booking | Link | BK-XXXXXXXX |
| totalEstimatedCost | Estimated Cost | Currency | 2,500,000 VND |
| approvedAmount | Approved Amount | Currency | 2,000,000 VND |
| status | Status | StatusBadge | Draft / Pending Approval / Approved / Disputed / Paid |
| createdBy.fullName | Inspector | Text | Employee Name |
| approvedBy.fullName | Approved By | Text | Manager Name |
| requiresAdminEscalation | Escalated | Badge | > 5M (Admin required) |
| note | Manager Note | Text | Approval/rejection notes |
| — DamageItem — | | | |
| itemName | Item | Text | Broken TV |
| description | Description | Text | Screen cracked |
| estimatedCost | Est. Cost | Currency | 1,500,000 VND |

#### 2. CRUD Mapping
| Screen | Operation |
| ------ | --------- |
| SCR-64 | Create (Employee) |
| SCR-63 | Read (Employee own list) |
| SCR-43 | Read (Manager list), Update (Approve/Reject) |
| SCR-53 | Read (Admin escalated list), Update (Co-Approve > 5M) |

#### 3. Search / Filter / Sort Mapping
- **Searchable Fields**: id, bookingId.
- **Filter Options**: status, room, requiresAdminEscalation.
- **Sort Options**: createdAt (Desc), totalEstimatedCost.

#### 4. Table Mapping
- **Table Columns (SCR-43)**: Room, Booking, Est. Cost, Inspector, StatusBadge, Actions.
- **Table Columns (SCR-53)**: Property, Room, Est. Cost, Manager Note, StatusBadge, Actions.
- **Actions (SCR-43)**: Click Row → Drawer (view images, DamageItems, Approve/Reject).
- **Actions (SCR-53)**: Click Row → Drawer (view images, Admin Co-Approve `button-success`).
- **StatusBadge (SCR-43)**: `color-warning` (Pending Review), `color-info` (Escalated to Admin).

#### 5. Form Mapping
- **Sections (SCR-64)**: Room Selection, Damage Items List (dynamic), Evidence Photos, Note.
- **Input Components**: Select (roomId), DynamicList (itemName, description, estimatedCost per item), MultiFileUpload/Camera (attachments), Textarea (note). "Add Another Item" ghost button.
- **Required Fields**: roomId (from booking), at least 1 DamageItem, photos.
- **Hidden Fields**: inspectionId, bookingId.

#### 6. Action Mapping
- Create Damage Report (Employee — SCR-64)
- Approve Report (Manager — SCR-43 Drawer; if > 5M: button changes to "Escalate to Admin")
- Reject Report (Manager — SCR-43 Drawer)
- Co-Approve Report (Admin — SCR-53 Drawer `button-success` "Approve")
- Dispute Damage Fee (Customer — via notification → Booking Detail)

#### 7. Status Mapping
| Backend Status | Badge | Color | Allowed Actions |
| -------------- | ----- | ----- | --------------- |
| Draft | Gray | Neutral | Submit (Employee) |
| Pending Approval | Yellow | Warning | Approve/Reject (Manager); Escalate if > 5M |
| Approved | Green | Success | Pay Damage Fee (Customer) |
| Disputed | Orange | Warning | Admin review |
| Paid | Green | Success | View only |

#### 8. Component Mapping
- DamageReportTable (`bg-surface-card` — SCR-43, SCR-53)
- DamageApprovalDrawer (slide-in from right, images + items + approve/reject — SCR-43)
- AdminEscalationDrawer (`button-success` "Approve" — SCR-53)
- DamageEvidenceGallery (fullscreen preview)
- DynamicItemForm (add/remove damage items dynamically — SCR-64)
- CameraUploadButton (HTML5 native capture, touch-friendly — SCR-64)

#### 9. Responsive Mapping
- **Desktop**: Table + Drawer for Manager/Admin review.
- **Employee Mobile (SCR-64)**: Optimized for 1-hand mobile operation. Camera via HTML5 `input capture`. FAB (+) from SCR-63 → SCR-64.

#### 10. Endpoint Mapping
| Screen | Endpoint | Method |
| ------ | -------- | ------ |
| SCR-64 | /api/v1/employees/damage-reports | POST |
| SCR-63 | /api/v1/employees/damage-reports | GET |
| SCR-43 | /api/v1/managers/damage-reports | GET |
| SCR-43 | /api/v1/managers/damage-reports/{id}/approve | POST |
| SCR-43 | /api/v1/managers/damage-reports/{id}/reject | POST |
| SCR-53 | /api/v1/admin/damage-reports/escalated | GET |
| SCR-53 | /api/v1/admin/damage-reports/{id}/approve | POST |

#### 11. Entity Relationship Mapping
- **→ RoomInspection**: Parent (1-to-1 per Failed With Damage inspection).
- **→ DamageItems**: List of individual damaged items (1-to-many).
- **→ Attachments**: Evidence photos via polymorphic Attachment (entityType=DamageItem).
- **→ Room / Booking**: Context of the damage.
- **→ User (Manager)**: ApprovedBy.
- **→ User (Admin)**: AdminApproverId (if escalated).

#### 12. Derived / Calculated Fields
- `totalEstimatedCost` = Sum of all DamageItem.estimatedCost.
- `requiresAdminEscalation` = totalEstimatedCost > 5,000,000 VND (configurable via SystemSetting).

#### 13. Empty / Loading / Error States
- **Empty (SCR-63)**: "No damage reports submitted."
- **Empty (SCR-43)**: "No damage reports awaiting approval."

#### 14. Validation Mapping
| Field | Validation | Error Message |
| ----- | ---------- | ------------- |
| damageItems | At least 1 item required | Please add at least one damaged item |
| estimatedCost | > 0 per item | Cost must be positive |
| photos | At least 1 photo required | Please attach evidence photos |

#### 15. Permission Mapping
- **Employee**: Create (own), Read (own — SCR-63).
- **Manager**: Read (assigned property — SCR-43), Update (Approve/Escalate).
- **Admin**: Read (Escalated — SCR-53), Update (Co-Approve large fees).
- **Customer**: Read (notified), Dispute (within 24h).

---

# 5. Shared Components
Các UI component tái sử dụng trên nhiều Entity để tạo sự đồng nhất:
- **StatusBadge**: Sử dụng chung cho Status của Room, Booking, Payment, Maintenance, Housekeeping, RoomInspection, DamageReport. Các màu sắc đồng nhất theo Semantic Tokens (`color-success` = green, `color-warning` = yellow, `color-danger` = red, `color-info` = blue, `radius-full`, 10% opacity background).
- **Data Table**: Hiển thị danh sách, tìm kiếm, phân trang và Lọc (Property, Status, Date) — dùng cho Admin và Manager (`bg-surface-card`, `border-color-border-subtle`).
- **Drawer Component**: Dùng để hiển thị chi tiết (Contract PDF, Maintenance Detail, Damage Report, Inspection Log, Customer Details) trượt ra từ bên phải màn hình mà không cần chuyển trang (`bg-surface-card`, `shadow-lg`, slide-in from right).
- **Modal Component**: Dùng cho các tác vụ ngắn (Gán Employee, thêm Floor, Confirm Cancellation).
- **PropertySelectorDropdown**: Hiển thị trong Manager Dashboard (SCR-27) và tất cả màn hình Manager — chỉ chứa các Property ACTIVE được gán.
- **ImageGalleryGrid**: Dùng cho Room Gallery (SCR-08, SCR-32) và Damage Evidence (SCR-43 Drawer, SCR-63).
- **FileUpload / CameraCapture**: Dùng cho Maintenance Photos (SCR-23), Damage Report Photos (SCR-64). Mobile: HTML5 `input capture` để dùng camera thiết bị.

# 6. Validation Mapping
Các quy tắc kiểm tra tính hợp lệ trên UI trước khi gửi xuống API:
- **Booking**: Customer không thể Booking các ngày đã ở trạng thái Occupied, Reserved, Maintenance, Pending Cleaning, Cleaning In Progress trên Lịch (Availability Calendar — SCR-09).
- **Payment (VNPay)**: Amount must match expected deposit/remaining snapshot. ~~Bank Transfer receipt upload~~ — **N/A edu (TD-011)**.
- **Housekeeping**: Manager không thể thao tác chuyển phòng từ `Pending Cleaning` sang `Available` trên UI (SCR-33) nếu Task Housekeeping của phòng đó chưa ở trạng thái Completed.
- **Room Inspection**: Chức năng Check-out trên màn hình quản lý (SCR-35) bị Disable nếu quy trình Room Inspection của Employee chưa hoàn tất với kết quả (Status = Passed).
- **Maintenance Edit/Delete**: Chỉ cho phép khi status = Open; các trạng thái khác → Edit/Delete button bị ẩn hoặc disabled.
- **Review**: Nút "Write Review" trên SCR-18 chỉ hiện khi Booking status = Checked-out, và chưa có review cho booking đó.

# 7. Permission Mapping
Kiểm soát hiển thị giao diện theo RBAC:
- **Guest**: Chỉ được phép xem SCR-01 đến SCR-09. Ẩn nút "Book Now" thay bằng "Login to Book".
- **Customer**: Chỉ được phép truy cập Dashboard cá nhân (SCR-15), quản lý Booking của mình (SCR-16–SCR-26). Giao diện ẩn tất cả các menu thuộc vùng quản trị của Manager và Admin.
- **Manager**: Property Selector trên Dashboard (SCR-27) chỉ chứa các Property được gán (ManagerPropertyAssignment ACTIVE). Cây cấu trúc (SCR-28), danh sách phòng (SCR-29) tự động filter dữ liệu theo Property đang quản lý. Truy cập SCR-27 đến SCR-44, SCR-66, SCR-69.
- **Employee**: Chỉ truy cập SCR-59 đến SCR-65, giao diện không hiển thị các menu liên quan đến Booking, Payment, Customer. Employee Dashboard (SCR-59) hiển thị 3 loại tác vụ: Housekeeping, Maintenance, Inspections.
- **Admin**: Quản lý toàn bộ Master Data (SCR-45 → SCR-58), gán Manager, nhưng giao diện không hiển thị các tác vụ vận hành dọn phòng hay bảo trì.

# 8. API Mapping
(Tổng quan các nhóm API gắn kết với UI)
- **Auth API**: Gắn với luồng xác thực SCR-02 → SCR-06 (Login, Register, OTP, Forgot/Reset Password). Hỗ trợ Google OAuth redirect.
- **Public API**: Lấy danh sách Property/Room nổi bật (SCR-01), tìm kiếm phòng (SCR-07, SCR-08), Lấy lịch trống (SCR-09), Banner Promotions (SCR-01). No authentication required.
- **Customer API**: My Bookings, My Payments (VNPay), Create/Edit/Delete Maintenance Ticket, Submit/Edit/Delete Review, My Contracts, My Complaints, Customer Dashboard stats. ~~Upload Receipt~~ deferred TD-011.
- **Manager API**: CRUD cấu trúc/phòng, Check-in/Out (SCR-37, after Inspection), **Cancel Booking** (`POST /api/v1/manager/bookings/{id}/cancel` — SCR-69, refund **100% deposit**), Assign Employee Task (Housekeeping/Maintenance), Approve Damage Report (or Escalate), View Inspections (SCR-42), Contract Resend (SCR-38), Property Reports (SCR-44). ~~Approve/Reject Payment CK~~ deferred TD-011.
- **Employee API**: Cập nhật Status các Task (Housekeeping, Maintenance), Perform Room Inspection Pass/Fail (SCR-62), Create Damage Report (SCR-64), View assigned Room List (SCR-65).
- **Admin API**: CRUD Property, CRUD User Status (Managers/Customers), Global Reporting (SCR-55), Cập nhật System Settings, Xử lý khiếu nại (SCR-54), Approve Escalated Damage (SCR-53), ~~VNPay Reconciliation (SCR-52)~~ deferred TD-010, Manage Promotions (SCR-57, SCR-58).

# 9. UI State Mapping
Ánh xạ các trạng thái nghiệp vụ lên trạng thái UI:
- **Booking State `Pending Deposit`**: UI chi tiết booking (SCR-18) hiển thị countdown thanh toán (**Hold Timeout 10 phút**, configurable). Nếu hết giờ, tự động vô hiệu hóa nút thanh toán và hiển thị "Hết hạn thanh toán".
- **Payment State `Pending` (VNPay)**: Chờ **Return URL**; Customer có thể retry nếu Failed. ~~IPN~~ **TD-012**. ~~Pending Verification + Upload Receipt~~ — **N/A edu (TD-011)**.
- **Booking Cancel (Manager)**: Nút "Cancel Booking" trên SCR-35 chỉ hiện khi status = Pending Deposit hoặc Confirmed. SCR-69 hiển thị Refund Preview = **100% deposit**; Confirm gọi Manager cancel API.
- **Booking Cancel (Customer)**: SCR-19 hiển thị tier refund trên **cọc** (≥7d 100% / 3–7d 50% / &lt;3d 0%).
- **Room State `Pending Cleaning` / `Maintenance` / `Out Of Service`**: Trên Calendar (SCR-09) và Room List (SCR-29), các ngày này được tô màu xám/vàng đặc trưng và khóa thuộc tính onClick để chặn thao tác đặt phòng.
- **Booking State `Pending Inspection`**: Nút "Check-out" trên SCR-35 bị disabled với tooltip "Waiting for room inspection".
- **Booking State `Pending Damage Payment`**: Customer thấy badge đỏ trên SCR-18 với nút "Pay Damage Fee". Số tiền cộng thêm damageFeeAmount vào remainingAmount.
- **DamageReport `requiresAdminEscalation = true`**: Nút Manager "Approve" trên SCR-43 Drawer đổi thành "Escalate to Admin" (`color-info`).

# 10. Navigation Mapping
Luồng di chuyển (User flow) liên kết giữa các màn hình:
- **Guest Flow**: Trang chủ (SCR-01) → Tìm kiếm (SCR-07) → Xem chi tiết phòng (SCR-08) → Click "Book Now" → Chuyển hướng sang đăng nhập (SCR-02).
- **Booking Flow**: Chi tiết phòng (SCR-08) → Checkout Form (SCR-16) → Order Review (SCR-20) → Cổng Gateway VNPay → Redirect trả về trang chi tiết booking (SCR-18) với trạng thái cập nhật (Confirmed/Failed).
- **Manager Flow**: Click Booking Detail (SCR-35) → Bấm nút Check-in/Check-out nhảy sang trang Check-in/Check-out Verification (SCR-37) → Xác nhận CMND / remaining paid → Hoàn tất.
- **Manager Cancel Flow**: SCR-35 → "Cancel Booking" → SCR-69 (nhập cancelReason, xem refund **100% deposit**) → `POST /api/v1/manager/bookings/{id}/cancel` → Booking Cancelled + deposit Payment Refunded + Room Available → notify Customer → quay SCR-35.
- **Maintenance Flow**: Khách tạo yêu cầu (SCR-23) → Manager nhận thông báo và thấy ở list (SCR-41) → Manager gán Employee (Drawer) → Employee nhận việc ở Workspace (SCR-61) và hoàn thành → Trạng thái phía Customer tự động cập nhật ở list yêu cầu (SCR-22).
- **Inspection & Damage Flow**: Booking → Pending Inspection → Employee thực hiện kiểm tra (SCR-62) → Pass → Check-out unblocked (SCR-35) → HousekeepingTask auto-created → Room = Pending Cleaning. **Nếu Fail** → Employee tạo Damage Report (SCR-64) → Manager duyệt (SCR-43) [Escalate to Admin if > 5M (SCR-53)] → Customer notified → Customer pays Damage Fee → Check-out completes.
- **Housekeeping Flow**: After Checkout → HousekeepingTask auto-created (Room = Pending Cleaning) → Manager assigns Employee (SCR-40) → Employee starts (SCR-60) → Room = Cleaning In Progress → Employee finishes → Room = Available.
- **Contract Flow**: Booking Confirmed → Backend @Async generates PDF → Email sent to Customer → Customer views in SCR-21 → Manager can resend from SCR-38.

# 11. Design Notes
- Ứng dụng thống nhất hệ thống **Design Tokens** (Modern Zen & Premium Hospitality): `color-primary-base`, `color-surface-card`, `color-surface-canvas`, `color-surface-inverted`, `radius-lg`, `radius-md`, `shadow-sm`, `shadow-md`, `shadow-lg`, semantic colors (`color-success`, `color-warning`, `color-danger`, `color-info`).
- Trải nghiệm di động (Mobile-first) đặc biệt quan trọng ở các luồng Guest tham khảo, Customer Booking, và đặc biệt là các màn hình workspace của Employee (SCR-59 tới SCR-65).
- Các thông báo hệ thống như Alert, Toast Success/Error phải hiển thị nhất quán trên toàn bộ các screen bằng các màu sắc quy định sẵn (`color-success`, `color-danger`, `color-warning`). Mỗi form cần có trạng thái loading indicator khi call API.
- Sidebar cho Manager/Admin (SCR-27 onward) sử dụng `color-surface-inverted` với `text-inverted`. Sidebar cho Employee (SCR-59 onward) cũng dùng `color-surface-inverted` với mobile-first layout.
- **Property-level data isolation**: Tất cả API calls của Manager tự động được filter theo Property được gán — không cần Manager tự chọn Property trong mỗi API call (đã được xử lý qua JWT context). Property Selector Dropdown trên UI chỉ dùng để thay đổi context khi Manager quản lý nhiều Property.
