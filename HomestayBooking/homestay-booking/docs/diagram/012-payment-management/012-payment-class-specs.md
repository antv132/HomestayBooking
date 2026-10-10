# 012 - Payment Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities

### Payment Class
Represents a payment transaction.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Contains fields like `booking_id`, `invoice_id`, `type`, `method`, `status`, `amount`, `order_ref`. A booking should only have one `PAID` payment per `type`.) |

---

## 2. Controllers

### PaymentV1Controller Class
Handles customer payment initiation (SCR-20).
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createVnpayPaymentUrl | POST `/api/v1/payments/vnpay`. Creates a VNPay checkout URL for DEPOSIT. Returns `paymentUrl`, `paymentId`, and `orderRef`. |
| 02 | createVnpayPaymentUrlLegacy | POST `/api/v1/payments/vnpay/create-url`. Legacy endpoint that takes `bookingId` + `type`. Returns `paymentUrl`, `paymentId`, and `orderRef`. |

### CustomerPaymentController Class
Handles Webhook/Return from VNPay.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | vnpayReturn | GET `/api/payments/vnpay/return`. Receives synchronous redirect from VNPay Gateway. Verifies signature, then calls `PaymentService.completeVnpayReturn()`. Redirects user to frontend URL with status. |

### ManagerPaymentV1Controller Class
Handles manager payment viewing (SCR-36).
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getPayments | GET `/api/v1/manager/payments`. Lists payments filtered by properties the manager oversees. Uses `ReportPropertyScopeValidator`. Returns 200 OK. |

---

## 3. Services

### PaymentService Class
Core payment lifecycle, VNPay integration, side effects.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createVnpayPaymentUrl | Validates Booking status. Validates hold window. Creates `Payment` entity (`PENDING`). Updates linked `Invoice` to `PENDING_PAYMENT`. Calls `VNPayService.createOrder()` to generate the URL. Returns URL and details. |
| 02 | completeVnpayReturn | Idempotent: Returns immediately if already `PAID`. Marks Payment as `PAID` or `FAILED`. Calls `InvoiceStatusSyncService.syncFromPaymentEntity()`. Applies side effects (e.g. Updates Booking to `CONFIRMED` and triggers Contract generation if DEPOSIT). Returns processing result. |

### VNPayService Class
Handles VNPay specific API communication.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createOrder | Handles hashing (`HmacSHA512`), parameter sorting, and URL generation based on VNPay's API spec. Returns generated URL string. |
