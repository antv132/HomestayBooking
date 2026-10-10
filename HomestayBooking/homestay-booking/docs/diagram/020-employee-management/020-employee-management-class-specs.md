# 020 - Employee Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Domain Overview
The Employee Management module allows Managers (and Admins) to oversee staff (`EMPLOYEE` role) assigned to their Homestays. Unlike Managers (who can manage multiple properties theoretically), an **Employee is strictly assigned to exactly ONE active property at a time**.

---

## 2. Controllers

### ManagerEmployeeV1Controller Class
Handles employee management for Managers. Requires `@PreAuthorize("hasRole('MANAGER')")`.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listEmployees | GET `/api/v1/manager/employees`. Scope-validated list of active employees at a specific property. Returns 200 OK. |
| 02 | listUnassigned | GET `/api/v1/manager/employees/unassigned`. Finds users with `role=EMPLOYEE`, `status=ACTIVE` who have NO `ACTIVE` assignments. Returns 200 OK. |
| 03 | createEmployee | POST `/api/v1/manager/employees`. Creates Employee account directly. Returns 201 Created. |
| 04 | assignEmployee | POST `/api/v1/manager/employees/assign`. Assigns an existing unassigned employee to a property. Returns 200 OK. |
| 05 | updateEmployee | PUT `/api/v1/manager/employees/{id}`. Updates employee details. Returns 200 OK. |
| 06 | updateEmployeeStatus | PATCH `/api/v1/manager/employees/{id}/status`. Toggles the employee's login capability (`ACTIVE` <-> `SUSPENDED`). Returns 200 OK. |

### AdminEmployeeV1Controller Class
Handles employee management for Admins. Requires `@PreAuthorize("hasRole('ADMIN')")`.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listEmployees | GET `/api/v1/admin/employees`. Lists active employees at a specific property (bypasses scope checks). Returns 200 OK. |
| 02 | listUnassigned | GET `/api/v1/admin/employees/unassigned`. Finds unassigned employees. Returns 200 OK. |
| 03 | createEmployee | POST `/api/v1/admin/employees`. Creates Employee account. Returns 201 Created. |
| 04 | assignEmployee | POST `/api/v1/admin/employees/assign`. Assigns an employee to a property. Returns 200 OK. |
| 05 | updateEmployee | PUT `/api/v1/admin/employees/{id}`. Updates employee details. Returns 200 OK. |
| 06 | updateEmployeeStatus | PATCH `/api/v1/admin/employees/{id}/status`. Toggles employee status. Returns 200 OK. |
| 07 | reassignEmployee | PATCH `/api/v1/admin/employees/{id}/reassign`. Exclusive to Admin: Moves an employee from one property to another. Prevents cross-manager conflicts. Returns 200 OK. |

---

## 3. Services

### EmployeeManagementService Class
Handles core logic for employee management. Ensures it only mutates users with `role = EMPLOYEE`. Blocks attempts to update Managers or Customers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createEmployee | Creates the `User` and their `EmployeePropertyAssignment` in one transaction. Generates a random 12-character temporary password and sends the invite email. Returns `EmployeeSummaryResponse`. |
| 02 | assignEmployee | Assigns an existing *unassigned* employee to a property. Returns `EmployeeSummaryResponse`. |
| 03 | reassignEmployee | (Admin Only) Moves an employee from one property to another. Flushes old `ACTIVE` assignment to `INACTIVE` before inserting the new one to maintain 1-to-1 active assignment constraint. Returns `EmployeeSummaryResponse`. |
| 04 | updateEmployeeStatus | Toggles the employee's login capability (`ACTIVE` <-> `SUSPENDED`). Returns `EmployeeSummaryResponse`. |
| 05 | listByProperty | Fetches scope-validated list of active employees at a specific property. Returns paginated list. |
| 06 | listUnassigned | Fetches users with `role=EMPLOYEE`, `status=ACTIVE` who have NO `ACTIVE` assignments. Returns paginated list. |
