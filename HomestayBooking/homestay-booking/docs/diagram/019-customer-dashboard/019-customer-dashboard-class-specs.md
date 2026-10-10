# 019 - Customer Dashboard: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Domain Overview
The Customer Dashboard provides a centralized, composite view of the customer's active engagements with the Homestay system. Instead of the frontend making 5-6 separate API calls to `bookings`, `payments`, `maintenance`, and `notifications`, the `CustomerDashboardService` aggregates these into a single highly optimized summary response.

---

## 2. Controllers

### CustomerDashboardV1Controller Class
REST API endpoints for the customer dashboard.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getDashboard | GET `/api/v1/customer/dashboard`. Returns `CustomerDashboardResponse` wrapping all the counters and lists. Controller checks `@PreAuthorize("hasRole('CUSTOMER')")`. Returns 200 OK. |

---

## 3. Services

### CustomerDashboardService Class
Aggregates customer dashboard data.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getDashboard | Enforces a hard check `currentUser.getRole() == User.Role.CUSTOMER` and filters queries by `currentUser.getId()`. Counts active bookings (`CONFIRMED`, `CHECKED_IN`), pending actions (`PENDING_DEPOSIT`, pending payments), open maintenance (`OPEN`, `IN_PROGRESS`), unread notifications. Finds upcoming milestones (nearest check-in/check-out and days until event). Fetches recent lists (top 5 upcoming bookings, top 3 recent payments, top 5 recent notifications). Returns `CustomerDashboardResponse`. |
