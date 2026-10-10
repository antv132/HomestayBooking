# 011 - Billing (Invoice) Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities

### Invoice Class
Represents a bill issued to a customer.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Contains `booking_id`, `customer_id`, `property_id`, `type` (DEPOSIT \| REMAINING_BALANCE), `amount`, `status`, `due_date`, `paid_at`. Unique Constraint: `(booking_id, type)` — exactly 1 DEPOSIT and 1 REMAINING_BALANCE per booking.) |

---

## 2. Controllers

### CustomerInvoiceController Class
Handles invoice viewing for Customers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listInvoices | GET `/api/v1/invoices/me`. Paginated list of my invoices, optional `status` filter. Returns 200 OK. |
| 02 | getInvoiceDetail | GET `/api/v1/invoices/me/{id}`. Detail view, validates ownership. Returns 200 OK. |

### ManagerInvoiceController Class
Handles invoice viewing for Managers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getInvoices | GET `/api/v1/manager/invoices`. Paginated list of invoices for a specific property. Requires `propertyId` param. Returns 200 OK. |

---

## 3. Services

### InvoiceIssuanceService Class
Issues the standard 40/60 invoice pair when a Booking is created.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | issueInvoices | Checks `existsByBooking_IdAndType` before inserting. Sets Due Dates: DEPOSIT is due today. REMAINING is due on `checkInDate`. Logs `INVOICE_ISSUED` activity. Returns void. |

### InvoiceQueryService Class
Handles all read operations for customers and managers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listForCustomer | Retrieves invoices. Enforces `customer_id == currentUser.id`. Returns paginated list. |
| 02 | getDetailForCustomer | Retrieves detail. Enforces `customer_id == currentUser.id`. Returns Invoice detail. |
| 03 | listForManager | Retrieves invoices. Enforces `scopeValidator.validateManagerAccess(manager, propertyId)`. Returns paginated list. |
| 04 | buildBreakdown | Calculates `paidAmount` and `remainingBalance` by summing invoices grouped by status. Used by Booking detail endpoints. Returns summary object. |

### InvoiceStatusSyncService Class
Listens to Payment lifecycle and updates Invoice status.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | syncFromPayment | Transitions Invoice status based on Payment status: `PENDING` -> `PENDING_PAYMENT`, `PAID` -> `PAID`, `FAILED` -> `UNPAID`. Fallback Match: If Payment doesn't have an Invoice ID set yet, matches by Booking ID + Type. Returns void. |
