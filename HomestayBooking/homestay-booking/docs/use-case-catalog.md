# Use Case Catalog — Homestay Booking System

**Source:** `sdd/services/homestay/features/*/spec.md` (User Stories consolidated)  
**Naming:** Verb + Object · Tech-agnostic descriptions  
**Total:** 65 use cases

| ID | Use Case | Feature | Use Case Description |
|----|----------|---------|----------------------|
| UC-01 | Register Customer Account | User Auth | Customer registers an account and verifies email with OTP. |
| UC-02 | Login User Account | User Auth | User signs in with password or Google to open a session. |
| UC-03 | Manage Auth Session | User Auth | User refreshes the access token or logs out of the current session. |
| UC-04 | Recover User Password | User Auth | User resets a forgotten password or changes password while signed in. |
| UC-05 | View Personal Profile | User Profile | User views their personal profile. |
| UC-06 | Update Personal Profile | User Profile | User updates profile data and the system syncs the display. |
| UC-07 | Search Available Rooms | Room Discovery | Guest searches and lists rooms by search criteria. |
| UC-08 | View Room Details | Room Discovery | Guest views room details before booking. |
| UC-09 | Browse Room Discovery | Room Discovery | Guest discovers rooms from the landing page and public availability. |
| UC-10 | Create Booking With Deposit | Booking & Inventory | Customer creates a booking, holds the room, and pays the deposit. |
| UC-11 | Track Customer Bookings | Booking & Inventory | Customer views and tracks the status of their bookings. |
| UC-12 | Perform Check-In Check-Out | Booking & Inventory | Manager performs check-in or check-out per business rules. |
| UC-13 | Cancel Booking With Refund | Booking & Inventory | Customer cancels a booking and receives a deposit refund by tier. |
| UC-14 | Modify Confirmed Booking | Booking & Inventory | Manager modifies a Confirmed booking within allowed rules. |
| UC-15 | Expire Hold And Mark No-Show | Booking & Inventory | System cancels expired holds and marks no-show bookings. |
| UC-16 | View Room Calendar | Availability Calendar | Guest, Manager, or Employee views the room calendar by role and scope. |
| UC-17 | Update Manual Room Status | Availability Calendar | Manager manually updates room status for selected dates. |
| UC-18 | Manage Properties | Property Management | Admin creates and manages property information. |
| UC-19 | Assign Property Manager | Property Management | Admin assigns an ACTIVE Manager to a property. |
| UC-20 | View Assigned Properties | Property Management | Manager views properties assigned to them. |
| UC-21 | Manage Property Floors | Structure Management | Manager adds, updates, or deletes floors in a property. |
| UC-22 | View Property Structure | Structure Management | Manager or Admin views the Property → Floor → Room tree. |
| UC-23 | Manage Rooms | Room Management | Manager creates, updates, or deletes rooms by property. |
| UC-24 | Manage Room Gallery | Room Management | Manager manages room gallery images. |
| UC-25 | View Rooms Read-Only | Room Management | Employee views the room list in read-only mode. |
| UC-26 | Manage Customer Accounts | Customer Management | Admin lists, filters, and updates Customer account status. |
| UC-27 | View Customer History | Customer Management | Admin views a Customer profile and booking history. |
| UC-28 | Generate Booking Contract | Contract Management | System generates and sends a contract or addendum when fees apply. |
| UC-29 | View Booking Contract | Contract Management | Customer or Manager views, downloads, or resends a contract. |
| UC-30 | Generate Booking Invoices | Billing Management | System creates and syncs invoices with the booking lifecycle. |
| UC-31 | View Invoice Status | Billing Management | Customer or Manager views invoice status within their scope. |
| UC-32 | Pay Via VNPay | Payment Management | Customer pays deposit, remaining balance, or damage fee via VNPay. |
| UC-33 | Verify Bank Transfer Payment | Payment Management | Manager verifies a bank transfer payment with a receipt. |
| UC-34 | View Payment History | Payment Management | Customer or Manager views detailed payment history. |
| UC-35 | Submit Maintenance Request | Maintenance Management | Customer submits, edits, or deletes a maintenance ticket while Open. |
| UC-36 | Assign Maintenance Employee | Maintenance Management | Manager reviews tickets and assigns an Employee. |
| UC-37 | Update Maintenance Progress | Maintenance Management | Employee updates progress on an assigned ticket. |
| UC-38 | Close Maintenance Ticket | Maintenance Management | Manager confirms completion and closes the ticket. |
| UC-39 | Manage Own Room Review | Review & Rating | Customer submits, edits, or deletes a review after check-out. |
| UC-40 | View Public Room Reviews | Review & Rating | Guest views public reviews for a room. |
| UC-41 | Moderate Room Reviews | Review & Rating | Manager or Admin moderates and hides inappropriate reviews. |
| UC-42 | Dispatch Business Notifications | Notification Engine | System dispatches notifications from business events via Outbox. |
| UC-43 | Manage User Notifications | Notification Engine | User views, marks as read, and navigates from notifications. |
| UC-44 | Monitor Failed Notifications | Notification Engine | Admin monitors Outbox and failed notification deliveries. |
| UC-45 | View Operational Reports | Reporting | Manager or Admin views KPIs and reports within their scope. |
| UC-46 | Export Internal Report | Reporting | Internal user exports a report for sharing. |
| UC-47 | Manage Customer Complaints | Administration | Customer submits a complaint; Admin resolves complaints. |
| UC-48 | Configure System Settings | Administration | Admin configures system-wide settings. |
| UC-49 | View Activity Logs | Administration | Admin views activity logs with filters. |
| UC-50 | Open Administration Hub | Administration | Admin opens the administration hub to access system functions. |
| UC-51 | View Promotion Banners | Promotion Management | Guest views active promotion banners on the home page. |
| UC-52 | Manage Promotion Banners | Promotion Management | Admin creates, edits, deletes, toggles, and reorders banners. |
| UC-53 | View Customer Dashboard | Customer Dashboard | Customer views KPI overview, bookings, payments, and alerts. |
| UC-54 | Manage Employee Accounts | Employee Management | Admin or Manager creates and updates Employee accounts. |
| UC-55 | Assign Employee Property | Employee Management | Admin or Manager assigns or transfers an Employee to a property. |
| UC-56 | Toggle Employee Status | Employee Management | Admin or Manager deactivates or activates an Employee. |
| UC-57 | Create Housekeeping After Checkout | Housekeeping Management | System auto-creates a housekeeping task after check-out. |
| UC-58 | Manage Housekeeping Tasks | Housekeeping Management | Manager views, assigns, or cancels housekeeping tasks. |
| UC-59 | Execute Housekeeping Task | Housekeeping Management | Employee starts and completes an assigned housekeeping task. |
| UC-60 | View Employee Dashboard | Employee Dashboard | Employee views KPIs, task previews, today’s tasks, and shift progress. |
| UC-61 | Perform Room Inspection | Inspection & Damage | Employee inspects a room before check-out using the checklist. |
| UC-62 | Record Damage Report | Inspection & Damage | Employee records a damage report and damage items. |
| UC-63 | Approve Damage Fee | Inspection & Damage | Manager approves the fee; Admin co-approves when over the threshold. |
| UC-64 | Dispute Damage Fee | Inspection & Damage | Customer disputes a damage fee within the 24-hour window. |
| UC-65 | Enforce Checkout Damage Gate | Inspection & Damage | System blocks check-out until inspection/fee is done or debt is marked. |
