# 022 - Employee Dashboard: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Domain Overview
The Employee Dashboard provides a mobile-first, unified view for employees to see their assigned tasks across three different domains: Housekeeping, Maintenance, and Room Inspections.

---

## 2. Controllers

### EmployeeDashboardController Class
REST API endpoints for the employee dashboard. Role: `EMPLOYEE`.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getDashboard | GET `/api/v1/employee/dashboard`. Returns the full `EmployeeDashboardResponse` for the main screen. Returns 200 OK. |
| 02 | getKpis | GET `/api/v1/employee/kpis`. Returns a lightweight `EmployeeKpisResponse` containing only the 3 pending counters (used for badges in navigation menus). Returns 200 OK. |

---

## 3. Services

### EmployeeDashboardService Class
Aggregates employee dashboard data.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getDashboard | Checks if the employee has an active `EmployeePropertyAssignment`. If not, returns empty dashboard. Aggregates Housekeeping, Maintenance, and Inspections. Uses `Asia/Ho_Chi_Minh` timezone. Returns `EmployeeDashboardResponse` with pending counters, awaiting list, completed today list, and unified today tasks. |
| 02 | getKpis | Fetches only the raw pending counters for Housekeeping, Maintenance, and Inspections. Returns `EmployeeKpisResponse`. |
