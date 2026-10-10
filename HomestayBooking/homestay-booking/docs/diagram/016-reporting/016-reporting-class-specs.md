# 016 - Reporting & Analytics: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Domain Overview
The reporting system provides managers and admins with analytics on Homestay performance. Reports are generated dynamically by querying `Payment` and `Booking` records without relying on pre-aggregated data warehouses.

---

## 2. Controllers

### ReportV1Controller Class
Handles report requests for Managers (and Admins). Role: `MANAGER`.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getRevenueReport | Takes `propertyId`, `from`, `to`, `groupBy`. Fetches revenue report from `ReportService`. Returns `RevenueReportResponse` wrapped in `ApiResponse`. |
| 02 | getOccupancyReport | Takes `propertyId`, `from`, `to`, `groupBy`. Fetches occupancy report from `PropertyReportService`. Returns `OccupancyReportResponse` wrapped in `ApiResponse`. |

---

## 3. Services

### ReportService Class
Calculates revenue reports.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getRevenueReport | Calculates total, deposit, and remaining balance revenue from `Payment` table for `COMPLETED` payments. Groups by `month` or `week`. Iterates over raw database rows (`paid_at`, `amount`, `property_id`) and buckets them in memory to build `PeriodRevenue` and `PropertyRevenue` structures. Managers can query specific properties or all assigned properties. Returns `RevenueReportResponse`. |

### PropertyReportService Class
Calculates occupancy and booking trends.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getOccupancyReport | Calculates Occupancy Rate (%) and volume of bookings. Uses `Booking` and `Room` tables for `CONFIRMED`, `CHECKED_IN`, or `CHECKED_OUT` bookings. Initializes time buckets, fetches overlapping bookings, slices into nights, increments occupied counter. `Occupancy Rate = (Occupied Nights / Total Available Nights) * 100`. Returns `OccupancyReportResponse`. |
| 02 | getBookingTrends | Counts bookings based on their `created_at` timestamp. Returns trend data. |

### ReportPropertyScopeValidator Class
Prevents unauthorized access to reports.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | validateManagerAccess | If a Manager requests a report for Property A, this validator ensures there is an active `ManagerPropertyAssignment` linking the Manager to Property A. Throws exception if unauthorized. Returns void. |
