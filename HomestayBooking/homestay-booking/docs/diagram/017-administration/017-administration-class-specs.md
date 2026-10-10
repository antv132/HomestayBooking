# 017 - Administration: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Domain Overview
The Administration module manages system-wide configurations, property lifecycles, and user directories (specifically Managers and Customers).

---

## 2. Controllers

### AdminUserController Class
Handles user management for Admins.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listUsers | Lists users with role-specific aggregates. For Managers: Returns number of active property assignments. For Customers: Returns total bookings and total spend. Returns 200 OK. |
| 02 | createManager | Creates a manager account directly with `status = ACTIVE` (bypassing email OTP). Returns 201 Created. |
| 03 | updateManager | Updates manager details and status (`ACTIVE` <-> `INACTIVE`). Returns 200 OK. |
| 04 | updateCustomerStatus | Updates customer status (`ACTIVE` <-> `SUSPENDED`). Suspended users have all their refresh tokens revoked immediately. Returns 200 OK. |

### AdminPropertyController Class
Handles property lifecycle for Admins.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createProperty | Creates a new property with `status = INACTIVE`. Returns 201 Created. |
| 02 | setPropertyStatus | A property CANNOT be set to `ACTIVE` unless it has an active Manager assigned. Returns 200 OK. |
| 03 | assignManager | Only one active manager per property is allowed. Assigning a new manager automatically sets the previous manager's assignment to `INACTIVE`. Returns 200 OK. |

### AdminSettingsController Class
Handles system-wide configurations (SCR-60).
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getSettings | Retrieves global system variables stored in the `SystemSetting` entity. Returns 200 OK. |
| 02 | updateSetting | Updates settings like `DEPOSIT_PERCENTAGE` (constrained 10-50%) and `CANCEL_TIMEOUT_HOURS`. Returns 200 OK. |

---

## 3. Services

### AdminUserService Class
Manages `MANAGER` and `CUSTOMER` users. Does not allow updating `ADMIN` accounts.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listUsers | Fetches paginated users with enrichment data. Returns list. |
| 02 | createManager | Creates user entity with role `MANAGER`. Returns saved user. |
| 03 | updateManager | Updates manager details. Returns updated user. |
| 04 | updateCustomerStatus | Updates status. If `SUSPENDED`, revokes tokens. Returns updated user. |

### AdminPropertyService Class
Manages Homestay properties and their Manager assignments.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | assertCanActivate | Ensures property has an active Manager assigned before transitioning to `ACTIVE`. Throws exception if invalid. Returns void. |
| 02 | assignManager | Creates a historical log in `ManagerPropertyAssignment`. Sets old assignment to `INACTIVE`. Returns void. |

### AdminSettingsService Class
Manages global system variables stored in the `SystemSetting` entity (key-value pairs).
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | updateSetting | Uses upsert logic based on the `key` to save system settings. Returns saved setting. |
