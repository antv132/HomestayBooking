# Screen Authorization Matrix
## Homestay Booking System

**Nguồn:** `docs/screen.md`  
**Ký hiệu:** `✓` = được phép truy cập · `—` = không được phép  

| Screen | Guest | Customer | Manager | Employee | Admin |
|--------|:-----:|:--------:|:-------:|:--------:|:-----:|
| **Guest / Public** | | | | | |
| Landing / Home Page | ✓ | — | — | — | — |
| Login | ✓ | — | — | — | — |
| Register | ✓ | — | — | — | — |
| OTP / Email Verification | ✓ | — | — | — | — |
| Forgot Password | ✓ | — | — | — | — |
| Reset Password | ✓ | — | — | — | — |
| Room Search & Listing | ✓ | ✓ | — | — | — |
| Room Detail | ✓ | ✓ | — | — | — |
| Availability Calendar | ✓ | ✓ | — | — | — |
| **Shared (Authenticated)** | | | | | |
| User Profile | — | ✓ | ✓ | ✓ | ✓ |
| Edit Profile | — | ✓ | ✓ | ✓ | ✓ |
| Change Password | — | ✓ | ✓ | ✓ | ✓ |
| Notification Center | — | ✓ | ✓ | ✓ | ✓ |
| Notification Detail | — | ✓ | ✓ | ✓ | ✓ |
| **Customer** | | | | | |
| Customer Dashboard | — | ✓ | — | — | — |
| Booking Checkout | — | ✓ | — | — | — |
| Booking Management | — | ✓ | — | — | — |
| Booking Detail | — | ✓ | — | — | — |
| Booking Cancellation | — | ✓ | — | — | — |
| Order Review & Payment | — | ✓ | — | — | — |
| My Contract List | — | ✓ | — | — | — |
| Maintenance Ticket List | — | ✓ | — | — | — |
| Create Maintenance Ticket | — | ✓ | — | — | — |
| My Reviews | — | ✓ | — | — | — |
| Review & Rating | — | ✓ | — | — | — |
| Payment History | — | ✓ | — | — | — |
| My Complaint List | — | ✓ | — | — | — |
| Create Complaint | — | ✓ | — | — | — |
| **Manager** | | | | | |
| Manager Dashboard | — | — | ✓ | — | — |
| Structure Management | — | — | ✓ | — | — |
| Room Management List | — | — | ✓ | — | — |
| Add Room | — | — | ✓ | — | — |
| Edit Room | — | — | ✓ | — | — |
| Room Gallery Management | — | — | ✓ | — | — |
| Room Status Management | — | — | ✓ | — | — |
| Booking List | — | — | ✓ | — | — |
| Booking Detail (Manager) | — | — | ✓ | — | — |
| Manager Booking Cancellation | — | — | ✓ | — | — |
| Payment Management | — | — | ✓ | — | — |
| Check-in / Check-out | — | — | ✓ | — | — |
| Contract Management | — | — | ✓ | — | — |
| Employee Management | — | — | ✓ | — | — |
| Housekeeping Tasks | — | — | ✓ | — | — |
| Maintenance Tasks | — | — | ✓ | — | — |
| Inspection Management | — | — | ✓ | — | — |
| Damage Report Management | — | — | ✓ | — | — |
| Property Reports | — | — | ✓ | — | — |
| Housekeeping Schedule | — | — | ✓ | — | — |
| **Admin** | | | | | |
| Admin Dashboard | — | — | — | — | ✓ |
| Property Management | — | — | — | — | ✓ |
| Create Property | — | — | — | — | ✓ |
| Edit Property | — | — | — | — | ✓ |
| Manager Assignment | — | — | — | — | ✓ |
| Manager Directory | — | — | — | — | ✓ |
| Customer Directory | — | — | — | — | ✓ |
| Payment Reconciliation | — | — | — | — | ✓ |
| Damage Escalation | — | — | — | — | ✓ |
| Complaint Management | — | — | — | — | ✓ |
| Global Reports | — | — | — | — | ✓ |
| System Administration | — | — | — | — | ✓ |
| Promotion Management | — | — | — | — | ✓ |
| Add / Edit Promotion | — | — | — | — | ✓ |
| **Employee** | | | | | |
| Employee Dashboard | — | — | — | ✓ | — |
| Housekeeping Workspace | — | — | — | ✓ | — |
| Maintenance Workspace | — | — | — | ✓ | — |
| Room Inspection Hub | — | — | — | ✓ | — |
| Damage Report List | — | — | — | ✓ | — |
| Create Damage Report | — | — | — | ✓ | — |
| Property Room List | — | — | — | ✓ | — |

---

## Notes

| Ghi chú | Chi tiết |
|---------|----------|
| Guest auth screens | SCR-01–06: chỉ Guest. User đã login → redirect dashboard theo role. |
| Discovery | SCR-07–09: Guest + Customer. |
| Shared | SCR-10–14: mọi role đã authenticate; không Guest. |
| Role isolation | Sai role → 403 / redirect (không vào màn role khác). |
| SCR-52 | Payment Reconciliation — Admin; deferred edu (TD-010). |
| SCR-65 | Property Room List — Employee; có thể ngoài scope inventory FE. |
| Manager scope | Màn Manager chỉ data thuộc property được gán. |
| Employee scope | Màn Employee chỉ task/property được gán. |

---

## SCR ID ↔ Screen (tham chiếu)

| SCR | Screen | Authorized roles |
|-----|--------|------------------|
| SCR-01 | Landing / Home Page | Guest |
| SCR-02 | Login | Guest |
| SCR-03 | Register | Guest |
| SCR-04 | OTP / Email Verification | Guest |
| SCR-05 | Forgot Password | Guest |
| SCR-06 | Reset Password | Guest |
| SCR-07 | Room Search & Listing | Guest, Customer |
| SCR-08 | Room Detail | Guest, Customer |
| SCR-09 | Availability Calendar | Guest, Customer |
| SCR-10 | User Profile | Customer, Manager, Employee, Admin |
| SCR-11 | Edit Profile | Customer, Manager, Employee, Admin |
| SCR-12 | Change Password | Customer, Manager, Employee, Admin |
| SCR-13 | Notification Center | Customer, Manager, Employee, Admin |
| SCR-14 | Notification Detail | Customer, Manager, Employee, Admin |
| SCR-15 | Customer Dashboard | Customer |
| SCR-16 | Booking Checkout | Customer |
| SCR-17 | Booking Management | Customer |
| SCR-18 | Booking Detail | Customer |
| SCR-19 | Booking Cancellation | Customer |
| SCR-20 | Order Review & Payment | Customer |
| SCR-21 | My Contract List | Customer |
| SCR-22 | Maintenance Ticket List | Customer |
| SCR-23 | Create Maintenance Ticket | Customer |
| SCR-24 | My Reviews | Customer |
| SCR-25 | Review & Rating | Customer |
| SCR-26 | Payment History | Customer |
| SCR-27 | Manager Dashboard | Manager |
| SCR-28 | Structure Management | Manager |
| SCR-29 | Room Management List | Manager |
| SCR-30 | Add Room | Manager |
| SCR-31 | Edit Room | Manager |
| SCR-32 | Room Gallery Management | Manager |
| SCR-33 | Room Status Management | Manager |
| SCR-34 | Booking List | Manager |
| SCR-35 | Booking Detail (Manager) | Manager |
| SCR-36 | Payment Management | Manager |
| SCR-37 | Check-in / Check-out | Manager |
| SCR-38 | Contract Management | Manager |
| SCR-39 | Employee Management | Manager |
| SCR-40 | Housekeeping Tasks | Manager |
| SCR-41 | Maintenance Tasks | Manager |
| SCR-42 | Inspection Management | Manager |
| SCR-43 | Damage Report Management | Manager |
| SCR-44 | Property Reports | Manager |
| SCR-45 | Admin Dashboard | Admin |
| SCR-46 | Property Management | Admin |
| SCR-47 | Create Property | Admin |
| SCR-48 | Edit Property | Admin |
| SCR-49 | Manager Assignment | Admin |
| SCR-50 | Manager Directory | Admin |
| SCR-51 | Customer Directory | Admin |
| SCR-52 | Payment Reconciliation | Admin |
| SCR-53 | Damage Escalation | Admin |
| SCR-54 | Complaint Management | Admin |
| SCR-55 | Global Reports | Admin |
| SCR-56 | System Administration | Admin |
| SCR-57 | Promotion Management | Admin |
| SCR-58 | Add / Edit Promotion | Admin |
| SCR-59 | Employee Dashboard | Employee |
| SCR-60 | Housekeeping Workspace | Employee |
| SCR-61 | Maintenance Workspace | Employee |
| SCR-62 | Room Inspection Hub | Employee |
| SCR-63 | Damage Report List | Employee |
| SCR-64 | Create Damage Report | Employee |
| SCR-65 | Property Room List | Employee |
| SCR-66 | Housekeeping Schedule | Manager |
| SCR-67 | My Complaint List | Customer |
| SCR-68 | Create Complaint | Customer |
| SCR-69 | Manager Booking Cancellation | Manager |
