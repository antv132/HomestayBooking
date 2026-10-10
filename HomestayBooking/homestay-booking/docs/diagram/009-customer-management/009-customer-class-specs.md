# 009 - Customer Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities

### User Class (Customer Role)
Represents a customer in the system.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. `role` is always `CUSTOMER`. Canonical lifecycle: `INACTIVE` (unverified), `ACTIVE` (normal), `SUSPENDED` (Admin-blocked). Managers use `ACTIVE`/`INACTIVE` only.) |

---

## 2. Controllers

### AdminUserController Class
Handles user management for Admins. Authorization: `@PreAuthorize("hasRole('ADMIN')")`.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listCustomers | GET `/api/admin/users?role=CUSTOMER` or `/api/v1/admin/users?role=CUSTOMER`. Paginated directory with `bookingCount` + `totalSpend` enrichment. Returns 200 OK. |
| 02 | getCustomerBookings | GET `/{id}/bookings`. Customer booking history drawer (SCR-51). Returns `AdminCustomerBookingSummaryResponse`. |
| 03 | toggleCustomerStatus | PATCH `/{id}/status`. Toggle `ACTIVE ↔ SUSPENDED`. Logs to `ActivityLog`. Returns 200 OK. |
| 04 | updateCustomer | PUT `/{id}`. General update (Admin Directory); customer path calls `applyCustomerStatus`. Returns 200 OK. |
| 05 | createManager | POST `/managers`. Manager-specific creation. Returns 201 Created. |
| 06 | updateManager | PUT `/managers/{id}`. Manager-specific update. Returns 200 OK. |

### UserController Class
Handles user operations for Managers and Self-service.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getCustomers | GET `/api/users/customers`. Manager views customer list with optional `status` / `search` filters. Returns 200 OK. |
| 02 | getCustomerDetail | GET `/api/users/customers/{id}`. Manager views single customer detail. Returns 200 OK. |
| 03 | setCustomerStatus | PATCH `/api/users/customers/{id}/status`. Manager status toggle. Returns 200 OK. |
| 04 | getProfile | GET `/api/users/me`. Self-service profile read. Returns 200 OK. |
| 05 | updateProfile | PUT `/api/users/me`. Self-service profile update. Returns 200 OK. |
| 06 | changePassword | PATCH `/api/users/me/password`. Self-service password change. Returns 200 OK. |

### UserV1Controller Class
Versioned self-service profile endpoints.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getProfile | GET `/api/v1/users/me`. Returns 200 OK. |
| 02 | updateProfile | PUT `/api/v1/users/me`. Returns 200 OK. |
| 03 | changePassword | PUT `/api/v1/users/me/password`. Uses PUT instead of PATCH. Returns 200 OK. |

### CustomerDashboardV1Controller Class
Handles customer dashboard endpoints. Authorization: `@PreAuthorize("hasRole('CUSTOMER')")`.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getDashboard | GET `/api/v1/customer/dashboard`. Returns composite `CustomerDashboardResponse`: active bookings, upcoming check-ins, recent notifications. Returns 200 OK. |

---

## 3. Services

### AdminUserService Class
Handles Admin-level user logic.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listUsers | Validates role must be `MANAGER` or `CUSTOMER`. Bulk-loads enrichment data (booking counts, spend totals) in batch queries to avoid N+1. Returns paginated list. |
| 02 | applyCustomerStatus | Guards: `INACTIVE` customers cannot be toggled. Only `ACTIVE ↔ SUSPENDED` permitted. On `SUSPENDED`: calls `revokeAllByUser()` to invalidate all sessions. Writes `ActivityLog` entry. Returns updated User. |
| 03 | createManager | Creates `MANAGER` role user directly as `ACTIVE` (bypasses OTP flow). Returns created User. |
| 04 | updateManager | Validates role guard, optional password reset, `ACTIVE/INACTIVE` status only. Returns updated User. |

### UserService Class
Handles general user and manager logic.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | changePassword | Validates current password. Rejects if new == current. Encodes new password with `bcrypt`. Returns void. |
| 02 | getAllCustomers | Manager-facing list; filters by `status` and `search` keyword. Returns paginated list. |
| 03 | getCustomerDetail | Returns extended customer profile including booking summary for Manager view. |

### CustomerDashboardService Class
Aggregates customer dashboard data.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getDashboard | Aggregates data from `BookingRepository` and `PaymentRepository` for the customer's dashboard view. Read-only transaction. Returns `CustomerDashboardResponse`. |
