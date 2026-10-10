# 010 - Contract Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities

### Contract Class
Represents a legal snapshot of the booking agreement.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Exactly 1 contract per booking. Stores `deposit_amount` and `total_amount` as snapshots. `pdf_url` is written once and immutable.) |

---

## 2. Controllers

### CustomerContractV1Controller Class
Handles contract viewing for Customers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listContracts | GET `/api/v1/customers/me/contracts`. Returns paginated list with `status`, `search`, `sort` filters for the customer. Returns 200 OK. |
| 02 | getContractDetail | GET `/api/v1/customers/me/contracts/{id}`. Detail view for the customer. Returns 200 OK. |

### ContractV1Controller Class
Handles general contract access and PDF streaming.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getMyContracts | GET `/me`. Alias for customer contract list. Returns 200 OK. |
| 02 | getContractByBooking | GET `/booking/{bookingId}`. Lookup by booking ID (CUSTOMER \| MANAGER \| ADMIN). Does not create a contract. Returns 200 OK. |
| 03 | getContract | GET `/{id}`. Detail (CUSTOMER \| MANAGER \| ADMIN). Returns 200 OK. |
| 04 | streamPdf | GET `/{id}/pdf`. Streams PDF file bytes with `Content-Type: application/pdf`. Returns 200 OK. |
| 05 | resendContract | POST `/{id}/resend`. MANAGER only. Enqueues `CONTRACT_RESEND` outbox event. Returns 202 Accepted immediately. |

### ManagerContractV1Controller Class
Handles contract viewing for Managers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getContracts | GET `/`. Contracts scoped to assigned properties. Optional `propertyId` filter (validated against assignment). Returns 200 OK. |

---

## 3. Services

### ContractService Class
Core logic for generating, retrieving, and managing contracts.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | assertCanViewContract | Validates role-based access. Admin always allowed. Manager validated via `scopeValidator`. Customer validated by matching ID. Throws ForbiddenException if invalid. |
| 02 | persistContractPdf | Calls `pdfService.generateContractPdf` to get bytes, validates size, creates directory, writes file, persists `pdf_url` to DB, logs `CONTRACT_GENERATED`. Returns void. |
| 03 | loadStoredPdfStrict | Validates `pdf_url` starts with `/uploads/`. Checks `Files.isRegularFile` - throws `BusinessException` if missing. Returns file bytes. |
| 04 | processContractGenerateFromOutbox | Idempotent worker method. Uses `findByBookingId` + `orElseGet`. Creates Contract, calls `persistContractPdf`, enqueues `CONTRACT_EMAIL_SEND`. Returns void. |
| 05 | processContractEmailSendFromOutbox | Async worker method. Sends email with PDF attachment, updates `sent_at`, logs `CONTRACT_EMAIL_SENT`. Returns void. |
| 06 | processContractResendFromOutbox | Async worker method for resending. Validates scope, does not regenerate PDF, uses existing snapshot, sends email, logs `CONTRACT_RESENT`. Returns void. |
