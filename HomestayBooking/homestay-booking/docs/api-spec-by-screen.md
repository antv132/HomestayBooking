# API Specification by Screen
*API Specification v2.1.0 â€” Homestay / Resort Booking Management System*
*Updated: 2026-06-29 | 65 Screens | 5 Roles: Guest / Customer / Manager / Admin / Employee*

## 1. Overview
This document defines the backend RESTful APIs and WebSocket events mapped directly to the 65 screens in the Homestay / Resort Booking Management System. It ensures that every UI action has a corresponding endpoint, following the principles of Data-Driven UI, Role-Based Access Control (RBAC), and Status-Driven State.

## 2. Authentication & Headers
All protected endpoints require a JWT Access Token.
- **Header:** `Authorization: Bearer <access_token>`
- **Content-Type:** `application/json` (except for file uploads which use `multipart/form-data`)

**Roles:**
- `GUEST`: Unauthenticated user.
- `CUSTOMER`: Authenticated customer.
- `MANAGER`: Property manager (restricted by `PropertyId`).
- `EMPLOYEE`: Operational staff (restricted by `PropertyId`).
- `ADMIN`: Global administrator.

## 3. Error Response Standard
All API responses follow a standard envelope format.

**Success Response (2xx):**
```json
{
  "success": true,
  "message": "Success",
  "data": { ... } // Response payload
}
```

**Error Response (4xx, 5xx):**
```json
{
  "success": false,
  "message": "Error description",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ]
}
```

**Common HTTP Status Codes:**
- `200 OK`: Request succeeded.
- `201 Created`: Resource created successfully.
- `400 Bad Request`: Validation error or business rule violation.
- `401 Unauthorized`: Missing or invalid token.
- `403 Forbidden`: Insufficient permissions (RBAC violation).
- `404 Not Found`: Resource does not exist.
- `409 Conflict`: Business logic conflict (e.g., Room already booked).
- `500 Internal Server Error`: System failure.

---

## 4. Screen-by-Screen API Mapping

### Section 1 â€” Guest / Public Screens (SCR-01 to SCR-09)

#### SCR-01 Landing / Home Page
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/promotions/active`
- **Allowed Roles:** GUEST, CUSTOMER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "title": "20% Off",
        "subtitle": "Weekend Sale",
        "description": "Valid for all rooms...",
        "ctaText": "Book Now",
        "ctaUrl": "/search",
        "colorTheme": "Red"
      }
    ]
  }
  ```
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/properties/featured`
- **Allowed Roles:** GUEST, CUSTOMER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "name": "Riverside Resort",
        "address": "123 Main St",
        "images": ["url1", "url2"]
      }
    ]
  }
  ```

#### SCR-02 Login
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/auth/login`
- **Allowed Roles:** GUEST
- **Request Payload:**
  ```json
  { "email": "user@example.com", "password": "password123" }
  ```
- **Response Data:**
  ```json
  { "accessToken": "jwt...", "refreshToken": "jwt...", "user": { "id": "uuid", "role": "CUSTOMER" } }
  ```

#### SCR-03 Register
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/auth/register`
- **Allowed Roles:** GUEST
- **Request Payload:**
  ```json
  { "email": "user@example.com", "password": "password123", "fullName": "John Doe", "phone": "0987654321" }
  ```
- **Response Data:** `{}`

#### SCR-04 OTP / Email Verification
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/auth/verify-otp`
- **Allowed Roles:** GUEST
- **Request Payload:**
  ```json
  { "email": "user@example.com", "otp": "123456" }
  ```
- **Response Data:** `{}`

#### SCR-05 Forgot Password
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/auth/forgot-password`
- **Allowed Roles:** GUEST
- **Request Payload:**
  ```json
  { "email": "user@example.com" }
  ```
- **Response Data:** `{}`

#### SCR-06 Reset Password
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/auth/reset-password`
- **Allowed Roles:** GUEST
- **Request Payload:**
  ```json
  { "token": "reset_token...", "newPassword": "newPassword123" }
  ```
- **Response Data:** `{}`

#### SCR-07 Room Search & Listing
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/rooms/search`
- **Allowed Roles:** GUEST, CUSTOMER
- **Request Payload:** None (Query Params: `?propertyId=uuid&checkIn=2026-06-10&checkOut=2026-06-12&guests=2&page=0&size=10`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "roomNumber": "101",
        "roomType": "Deluxe",
        "capacity": 2,
        "pricePerNight": 1000000,
        "status": "AVAILABLE",
        "primaryImage": "url"
      }
    ],
    "page": 0, "size": 10, "totalElements": 1, "totalPages": 1
  }
  ```

#### SCR-08 Room Detail
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/rooms/{id}`
- **Allowed Roles:** GUEST, CUSTOMER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "id": "uuid",
    "roomNumber": "101",
    "roomType": "Deluxe",
    "capacity": 2,
    "area": 45,
    "description": "...",
    "pricePerNight": 1000000,
    "status": "AVAILABLE",
    "images": ["url1", "url2"],
    "amenities": ["Wi-Fi", "AC"]
  }
  ```
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/rooms/{id}/reviews`
- **Allowed Roles:** GUEST, CUSTOMER
- **Request Payload:** None (Query: `?page=0&size=5`)
- **Response Data:**
  ```json
  {
    "content": [
      { "id": "uuid", "customerName": "John D.", "rating": 5, "comment": "Great!", "createdAt": "2026-06-25T10:00:00Z" }
    ],
    "averageRating": 5.0,
    "totalReviews": 1
  }
  ```

#### SCR-09 Availability Calendar
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/rooms/{id}/availability`
- **Allowed Roles:** GUEST, CUSTOMER
- **Request Payload:** None (Query Params: `?startDate=2026-06-01&endDate=2026-06-30`)
- **Response Data:**
  ```json
  {
    "bookedDates": ["2026-06-15", "2026-06-16"],
    "maintenanceDates": ["2026-06-20"]
  }
  ```

---

### Section 2 â€” Shared Screens (SCR-10 to SCR-14)

#### SCR-10 User Profile
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/users/me`
- **Allowed Roles:** CUSTOMER, MANAGER, EMPLOYEE, ADMIN
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "id": "uuid",
    "fullName": "John Doe",
    "email": "user@example.com",
    "phone": "0987654321",
    "avatarUrl": "url",
    "role": "CUSTOMER",
    "status": "ACTIVE",
    "createdAt": "2026-06-01T10:00:00Z"
  }
  ```

#### SCR-11 Edit Profile
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/users/me`
- **Allowed Roles:** CUSTOMER, MANAGER, EMPLOYEE, ADMIN
- **Request Payload:**
  ```json
  { "fullName": "John Doe", "phone": "0987654321", "avatarUrl": "url" }
  ```
- **Response Data:** `{}`

#### SCR-12 Change Password
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/users/me/password`
- **Allowed Roles:** CUSTOMER, MANAGER, EMPLOYEE, ADMIN
- **Request Payload:**
  ```json
  { "oldPassword": "OldPass123!", "newPassword": "NewPass456!", "confirmPassword": "NewPass456!" }
  ```
- **Response Data:** `{}`

#### SCR-13 Notification Center
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/notifications`
- **Allowed Roles:** CUSTOMER, MANAGER, EMPLOYEE, ADMIN
- **Request Payload:** None (Query: `?page=0&size=20&isRead=false`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "title": "Booking Confirmed",
        "content": "Your booking...",
        "type": "SUCCESS",
        "isRead": false,
        "relatedEntityId": "uuid",
        "relatedEntityType": "BOOKING",
        "createdAt": "2026-06-25T10:00:00Z"
      }
    ],
    "unreadCount": 1
  }
  ```
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/notifications/read-all`
- **Allowed Roles:** CUSTOMER, MANAGER, EMPLOYEE, ADMIN
- **Request Payload:** `{}`
- **Response Data:** `{}`

#### SCR-14 Notification Detail
**[Call 1 — Get Notification Detail]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/notifications/{id}`
- **Allowed Roles:** CUSTOMER, MANAGER, EMPLOYEE, ADMIN
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "id": "uuid",
    "title": "Booking Confirmed",
    "content": "Your booking for Room 101 from 01/07 to 03/07 has been confirmed.",
    "type": "SUCCESS",
    "isRead": false,
    "relatedEntityId": "uuid",
    "relatedEntityType": "BOOKING",
    "createdAt": "2026-06-25T10:00:00Z"
  }
  ```

**[Call 2 — Mark Notification as Read]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/notifications/{id}/read`
- **Allowed Roles:** CUSTOMER, MANAGER, EMPLOYEE, ADMIN
- **Request Payload:** `{}`
- **Response Data:** `{}`

---
### Section 3 â€” Customer Screens (SCR-15 to SCR-26)

#### SCR-15 Customer Dashboard
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/customers/me/dashboard`
- **Allowed Roles:** CUSTOMER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "upcomingStay": {
      "bookingId": "uuid",
      "roomName": "Room 101",
      "checkInDate": "2026-07-01",
      "daysUntilCheckIn": 2
    },
    "pendingPayments": 1,
    "activeMaintenanceTickets": 0
  }
  ```

#### SCR-16 Booking Checkout
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/bookings`
- **Allowed Roles:** CUSTOMER
- **Request Payload:**
  ```json
  {
    "roomId": "uuid",
    "checkInDate": "2026-07-01",
    "checkOutDate": "2026-07-03",
    "guestCount": 2,
    "specialRequests": "Late check-in",
    "paymentMethod": "VNPAY"
  }
  ```
- **Response Data:**
  ```json
  {
    "bookingId": "uuid",
    "totalAmount": 2000000,
    "depositAmount": 800000,
    "status": "PENDING_DEPOSIT",
    "paymentUrl": "https://sandbox.vnpayment.vn/..."
  }
  ```

#### SCR-17 Booking Management
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/customers/me/bookings`
- **Allowed Roles:** CUSTOMER
- **Request Payload:** None (Query: `?status=UPCOMING&page=0&size=10`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "roomName": "Room 101",
        "checkInDate": "2026-07-01",
        "checkOutDate": "2026-07-03",
        "status": "CONFIRMED",
        "primaryImage": "url"
      }
    ]
  }
  ```

#### SCR-18 Booking Detail
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/bookings/{id}`
- **Allowed Roles:** CUSTOMER, MANAGER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "id": "uuid",
    "customer": { "id": "uuid", "fullName": "John Doe" },
    "room": { "id": "uuid", "roomNumber": "101", "roomType": "Deluxe" },
    "checkInDate": "2026-07-01",
    "checkOutDate": "2026-07-03",
    "guestCount": 2,
    "specialRequests": "Late check-in",
    "totalAmount": 2000000,
    "depositAmount": 800000,
    "remainingAmount": 1200000,
    "damageFeeAmount": 0,
    "status": "CONFIRMED",
    "holdExpiresAt": null,
    "cancelReason": null,
    "createdAt": "2026-06-25T10:00:00Z"
  }
  ```

#### SCR-19 Booking Cancellation
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/bookings/{id}/cancel`
- **Allowed Roles:** CUSTOMER
- **Notes:** Refund = **deposit × tier** — ≥7d before check-in → 100%; 3–7d → 50%; &lt;3d → 0% (canonical `004` FR-010). Prefer preview endpoint if available.
- **Request Payload:**
  ```json
  { "reason": "Change of plans" }
  ```
- **Response Data:**
  ```json
  { "refundPercent": 50, "refundAmount": 800000, "status": "CANCELLED" }
  ```

#### SCR-20 Order Review & Payment
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/payments/vnpay`
- **Allowed Roles:** CUSTOMER
- **Notes:** **VNPay only** (TD-011). Hold countdown default **10 minutes**.
- **Request Payload:** `{ "bookingId": "uuid" }`
- **Response Data:** `{ "paymentUrl": "https://sandbox.vnpayment.vn/..." }`

#### SCR-21 My Contract List
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/customers/me/contracts`
- **Allowed Roles:** CUSTOMER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "bookingId": "uuid",
        "roomName": "Room 101",
        "generatedAt": "2026-06-25T10:00:00Z",
        "pdfUrl": "url",
        "status": "ACTIVE"
      }
    ]
  }
  ```

#### SCR-22 Maintenance Ticket List
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/customers/me/maintenance`
- **Allowed Roles:** CUSTOMER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "title": "Broken AC",
        "status": "OPEN",
        "createdAt": "2026-06-25T10:00:00Z"
      }
    ]
  }
  ```

#### SCR-23 Create Maintenance Ticket
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/maintenance`
- **Allowed Roles:** CUSTOMER
- **Request Payload:** (Multipart Form Data)
  `bookingId`: `"uuid"`
  `roomId`: `"uuid"`
  `title`: `"Broken AC"`
  `description`: `"It is leaking water"`
  `photos`: `[file1, file2]`
- **Response Data:** `{}`

#### SCR-24 My Reviews
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/customers/me/reviews`
- **Allowed Roles:** CUSTOMER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "roomName": "Room 101",
        "rating": 5,
        "comment": "Perfect stay",
        "status": "PUBLISHED",
        "createdAt": "2026-06-25T10:00:00Z"
      }
    ]
  }
  ```

#### SCR-25 Review & Rating
**[Call 1 — Submit Review]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/reviews`
- **Allowed Roles:** CUSTOMER
- **Request Payload:**
  ```json
  { "bookingId": "uuid", "roomId": "uuid", "rating": 5, "comment": "Perfect stay" }
  ```
- **Response Data:** `{}`

**[Call 2 — Edit Review]**
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/reviews/{id}`
- **Allowed Roles:** CUSTOMER
- **Request Payload:**
  ```json
  { "rating": 4, "comment": "Updated comment" }
  ```
- **Response Data:** `{}`

**[Call 3 — Delete Review]**
- **HTTP Method:** DELETE
- **Endpoint:** `/api/v1/reviews/{id}`
- **Allowed Roles:** CUSTOMER
- **Request Payload:** None
- **Response Data:** `{}`

#### SCR-26 Payment History
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/customers/me/payments`
- **Allowed Roles:** CUSTOMER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "type": "DEPOSIT",
        "method": "VNPAY",
        "amount": 800000,
        "status": "PAID",
        "paidAt": "2026-06-25T10:00:00Z"
      }
    ]
  }
  ```

---
### Section 4 â€” Manager Screens (SCR-27 to SCR-44)

#### SCR-27 Manager Dashboard
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/dashboard`
- **Allowed Roles:** MANAGER
- **Request Payload:** None (Query: `?propertyId=uuid`)
- **Response Data:**
  ```json
  {
    "totalRooms": 50,
    "occupancyRate": 85,
    "todayCheckIns": 5,
    "pendingApprovals": 2,
    "revenueData": [{ "date": "2026-06-01", "amount": 10000000 }]
  }
  ```

#### SCR-28 Structure Management
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/properties/{propertyId}/floors`
- **Allowed Roles:** MANAGER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      { "id": "uuid", "floorNumber": 1, "description": "Sea view" }
    ]
  }
  ```
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/properties/{propertyId}/floors`
- **Allowed Roles:** MANAGER
- **Request Payload:**
  ```json
  { "floorNumber": 1, "description": "Sea view wing" }
  ```
- **Response Data:** `{}`

#### SCR-29 Room Management List
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/properties/{propertyId}/rooms`
- **Allowed Roles:** MANAGER
- **Request Payload:** None (Query: `?page=0&size=10`)
- **Response Data:**
  ```json
  {
    "content": [
      { "id": "uuid", "roomNumber": "101", "type": "Deluxe", "price": 1000000, "status": "AVAILABLE" }
    ]
  }
  ```

#### SCR-30 Add Room
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/rooms`
- **Allowed Roles:** MANAGER
- **Request Payload:**
  ```json
  {
    "floorId": "uuid",
    "roomNumber": "101",
    "roomType": "Deluxe",
    "pricePerNight": 1000000,
    "capacity": 2,
    "area": 45,
    "description": "Ocean-facing deluxe room",
    "amenities": ["Wi-Fi", "AC", "Minibar"]
  }
  ```
- **Response Data:** `{}`

#### SCR-31 Edit Room
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/managers/rooms/{id}`
- **Allowed Roles:** MANAGER
- **Request Payload:**
  ```json
  { "roomType": "Deluxe", "pricePerNight": 1200000, "description": "Updated ocean view room" }
  ```
- **Response Data:** `{}`

#### SCR-32 Room Gallery Management
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/rooms/{id}/images`
- **Allowed Roles:** MANAGER
- **Request Payload:** (Multipart Form Data)
  `images`: `[file1, file2]`
- **Response Data:** `{}`
- **HTTP Method:** DELETE
- **Endpoint:** `/api/v1/managers/rooms/{id}/images/{imageId}`
- **Allowed Roles:** MANAGER
- **Request Payload:** None
- **Response Data:** `{}`

#### SCR-33 Room Status Management
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/managers/rooms/{id}/status`
- **Allowed Roles:** MANAGER
- **Request Payload:**
  ```json
  { "status": "MAINTENANCE", "reason": "AC fix" }
  ```
- **Response Data:** `{}`

#### SCR-34 Booking List
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/properties/{propertyId}/bookings`
- **Allowed Roles:** MANAGER
- **Request Payload:** None (Query: `?status=CONFIRMED&page=0&size=10`)
- **Response Data:**
  ```json
  {
    "content": [
      { "id": "uuid", "guestName": "John Doe", "roomNumber": "101", "status": "CONFIRMED" }
    ]
  }
  ```

#### SCR-35 Booking Detail & Check-in/Out
**[Call 1 — Get Booking Detail]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/bookings/{id}`
- **Allowed Roles:** MANAGER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "id": "uuid",
    "customer": { "id": "uuid", "fullName": "John Doe", "phone": "0987654321" },
    "room": { "id": "uuid", "roomNumber": "101", "roomType": "Deluxe" },
    "checkInDate": "2026-07-01",
    "checkOutDate": "2026-07-03",
    "guestCount": 2,
    "specialRequests": "Late check-in",
    "totalAmount": 2000000,
    "depositAmount": 800000,
    "remainingAmount": 1200000,
    "damageFeeAmount": 0,
    "status": "CONFIRMED",
    "holdExpiresAt": null,
    "cancelReason": null,
    "createdAt": "2026-06-25T10:00:00Z"
  }
  ```

**[Call 2 — Check-in Guest]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/bookings/{id}/check-in`
- **Allowed Roles:** MANAGER
- **Request Payload:** `{}`
- **Response Data:** `{}`

**[Call 3 — Check-out Guest]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/bookings/{id}/check-out`
- **Allowed Roles:** MANAGER
- **Request Payload:** `{}`
- **Response Data:** `{}`

#### SCR-36 Payment Management
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/payments`
- **Allowed Roles:** MANAGER
- **Request Payload:** None (Query: `?status=PENDING&page=0&size=10`)
- **Notes:** Edu lists **VNPay** payments (read-only). Bank transfer verify — **TD-011 deferred**.
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "bookingId": "uuid",
        "type": "DEPOSIT",
        "method": "VNPAY",
        "amount": 800000,
        "status": "PENDING",
        "orderRef": "HS-20260625-001",
        "paidAt": null
      }
    ]
  }
  ```

#### SCR-37 Check-in / Check-out Verification
**Notes:** SCR-37 = check-in/out only — **not** Payment Verification (TD-011).
**[Call 1 — Process Check-in]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/bookings/{id}/check-in`
- **Allowed Roles:** MANAGER
- **Request Payload:** (Multipart Form Data)
  `idCardFront`: File (CMND/CCCD mặt trước)
  `idCardBack`: File (mặt sau)
  `depositCollected`: `boolean`
- **Response Data:** `{ "status": "CHECKED_IN" }`

**[Call 2 — Process Check-out]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/bookings/{id}/check-out`
- **Allowed Roles:** MANAGER
- **Request Payload:** `{ "depositRefunded": true }`
- **Response Data:** `{ "status": "CHECKED_OUT" }`

#### SCR-38 Contract Management
**[Call 1 — List Contracts]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/contracts`
- **Allowed Roles:** MANAGER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "bookingId": "uuid",
        "customer": { "fullName": "John Doe" },
        "generatedAt": "2026-06-25T10:00:00Z",
        "sentAt": "2026-06-25T10:05:00Z",
        "pdfUrl": "https://storage.example.com/contract.pdf",
        "status": "ACTIVE"
      }
    ]
  }
  ```

**[Call 2 — Resend Contract Email]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/contracts/{id}/resend`
- **Allowed Roles:** MANAGER
- **Request Payload:** `{}`
- **Response Data:** `{}`

#### SCR-39 Employee Management
**[Call 1 — List Employees in Property]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/properties/{propertyId}/employees`
- **Allowed Roles:** MANAGER
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      { "id": "uuid", "fullName": "Jane Doe", "email": "jane@example.com", "phone": "0912345678", "status": "ACTIVE" }
    ]
  }
  ```

**[Call 2 — Assign Employee to Property]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/employees/assign`
- **Allowed Roles:** MANAGER
- **Request Payload:**
  ```json
  { "employeeId": "uuid" }
  ```
- **Response Data:** `{}`

#### SCR-40 Housekeeping Tasks
**[Call 1 — List Housekeeping Tasks]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/housekeeping`
- **Allowed Roles:** MANAGER
- **Request Payload:** None (Query: `?status=PENDING&page=0&size=20`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "room": { "id": "uuid", "roomNumber": "101" },
        "assignedEmployee": null,
        "status": "PENDING",
        "note": "Deep clean required",
        "createdAt": "2026-06-25T10:00:00Z"
      }
    ]
  }
  ```

**[Call 2 — Assign Employee to Housekeeping Task]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/housekeeping/{id}/assign`
- **Allowed Roles:** MANAGER
- **Request Payload:**
  ```json
  { "employeeId": "uuid" }
  ```
- **Response Data:** `{}`

#### SCR-41 Maintenance Tasks
**[Call 1 — List Maintenance Tickets]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/maintenance`
- **Allowed Roles:** MANAGER
- **Request Payload:** None (Query: `?status=OPEN&page=0&size=20`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "title": "Broken AC",
        "room": { "id": "uuid", "roomNumber": "101" },
        "assignedEmployee": null,
        "status": "OPEN",
        "createdAt": "2026-06-25T10:00:00Z"
      }
    ]
  }
  ```

**[Call 2 — Assign Employee to Maintenance Ticket]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/maintenance/{id}/assign`
- **Allowed Roles:** MANAGER
- **Request Payload:**
  ```json
  { "employeeId": "uuid" }
  ```
- **Response Data:** `{}`

**[Call 3 — Close Maintenance Ticket]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/maintenance/{id}/close`
- **Allowed Roles:** MANAGER
- **Request Payload:**
  ```json
  { "resolutionNote": "Replaced AC capacitor. Unit is fully operational." }
  ```
- **Response Data:** `{}`

#### SCR-42 Inspection Management
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/inspections`
- **Allowed Roles:** MANAGER
- **Request Payload:** None (Query: `?status=PENDING&page=0&size=20`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "room": { "id": "uuid", "roomNumber": "101" },
        "bookingId": "uuid",
        "inspectedBy": { "fullName": "Jane Doe" },
        "status": "PASSED",
        "note": "All items in good condition",
        "inspectedAt": "2026-06-25T11:00:00Z"
      }
    ]
  }
  ```

#### SCR-43 Damage Report Management
**[Call 1 — List Damage Reports]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/damage-reports`
- **Allowed Roles:** MANAGER
- **Request Payload:** None (Query: `?status=PENDING_APPROVAL&page=0&size=20`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "room": { "id": "uuid", "roomNumber": "101" },
        "bookingId": "uuid",
        "totalEstimatedCost": 2500000,
        "approvedAmount": null,
        "status": "PENDING_APPROVAL",
        "requiresAdminEscalation": false,
        "createdBy": { "fullName": "Jane Doe" }
      }
    ]
  }
  ```

**[Call 2 — Approve Damage Report]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/damage-reports/{id}/approve`
- **Allowed Roles:** MANAGER
- **Request Payload:**
  ```json
  { "approvedAmount": 2000000, "note": "Confirmed damage based on evidence" }
  ```
- **Response Data:** `{}`

**[Call 3 — Reject Damage Report]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/managers/damage-reports/{id}/reject`
- **Allowed Roles:** MANAGER
- **Request Payload:**
  ```json
  { "note": "Damage predates this booking" }
  ```
- **Response Data:** `{}`

#### SCR-44 Property Reports
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/managers/properties/{propertyId}/reports`
- **Allowed Roles:** MANAGER
- **Request Payload:** None (Query: `?type=REVENUE&startDate=2026-06-01&endDate=2026-06-30`)
- **Response Data:**
  ```json
  {
    "totalRevenue": 100000000,
    "dataPoints": [{ "date": "2026-06-01", "value": 5000000 }]
  }
  ```

---
### Section 5 â€” Admin Screens (SCR-45 to SCR-58)

#### SCR-45 Admin Dashboard
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/dashboard`
- **Allowed Roles:** ADMIN
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "totalProperties": 5, "totalRooms": 200, "totalUsers": 1500,
    "systemRevenue": 500000000
  }
  ```

#### SCR-46 Property Management
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/properties`
- **Allowed Roles:** ADMIN
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      { "id": "uuid", "name": "Riverside", "managerId": "uuid", "status": "ACTIVE" }
    ]
  }
  ```

#### SCR-47 Add Property
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/admin/properties`
- **Allowed Roles:** ADMIN
- **Request Payload:**
  ```json
  {
    "name": "Riverside Resort",
    "address": "123 Main St, Da Nang",
    "description": "Beachfront resort with modern amenities",
    "checkInTime": "14:00",
    "checkOutTime": "12:00"
  }
  ```
- **Response Data:** `{}`

#### SCR-48 Edit Property
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/admin/properties/{id}`
- **Allowed Roles:** ADMIN
- **Request Payload:**
  ```json
  { "name": "Riverside Resort", "status": "ACTIVE" }
  ```
- **Response Data:** `{}`

#### SCR-49 Manager Assignment
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/admin/properties/{id}/manager`
- **Allowed Roles:** ADMIN
- **Request Payload:**
  ```json
  { "managerId": "uuid" }
  ```
- **Response Data:** `{}`

#### SCR-50 Manager Directory
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/managers`
- **Allowed Roles:** ADMIN
- **Request Payload:** None (Query: `?status=ACTIVE&page=0&size=10`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "fullName": "Manager A",
        "email": "manager@example.com",
        "phone": "0987654321",
        "status": "ACTIVE",
        "propertiesAssigned": 2
      }
    ]
  }
  ```

#### SCR-51 Customer Directory
**[Call 1 — List Customers]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/customers`
- **Allowed Roles:** ADMIN
- **Request Payload:** None (Query: `?status=ACTIVE&page=0&size=10`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "fullName": "Customer B",
        "email": "customer@example.com",
        "phone": "0912345678",
        "status": "ACTIVE",
        "totalBookings": 5
      }
    ]
  }
  ```

**[Call 2 — Update User Status]**
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/admin/users/{id}/status`
- **Allowed Roles:** ADMIN
- **Request Payload:**
  ```json
  { "status": "SUSPENDED" }
  ```
- **Response Data:** `{}`

#### SCR-52 Payment Reconciliation
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/payments/reconciliation`
- **Allowed Roles:** ADMIN
- **Request Payload:** None (Query: `?status=MISMATCH`)
- **Response Data:**
  ```json
  { "content": [ { "id": "uuid", "systemAmount": 1000, "gatewayAmount": 800, "status": "MISMATCH" } ] }
  ```

#### SCR-53 Damage Escalation
**[Call 1 — List Escalated Damage Reports]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/damage-reports/escalated`
- **Allowed Roles:** ADMIN
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "room": { "id": "uuid", "roomNumber": "101" },
        "totalEstimatedCost": 6000000,
        "approvedAmount": null,
        "status": "PENDING_APPROVAL",
        "requiresAdminEscalation": true,
        "managerNote": "Large-scale damage confirmed",
        "createdBy": { "fullName": "Jane Doe" }
      }
    ]
  }
  ```

**[Call 2 — Admin Co-Approve Damage Report]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/admin/damage-reports/{id}/approve`
- **Allowed Roles:** ADMIN
- **Request Payload:**
  ```json
  { "note": "Admin approval confirmed" }
  ```
- **Response Data:** `{}`

#### SCR-54 Complaint Management
**[Call 1 — List Complaints]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/complaints`
- **Allowed Roles:** ADMIN
- **Request Payload:** None (Query: `?status=OPEN&page=0&size=10`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "customer": { "fullName": "John Doe" },
        "subject": "Dirty room",
        "description": "The bathroom was not cleaned upon check-in.",
        "status": "OPEN",
        "createdAt": "2026-06-25T10:00:00Z"
      }
    ]
  }
  ```

**[Call 2 — Update Complaint Status]**
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/admin/complaints/{id}/status`
- **Allowed Roles:** ADMIN
- **Request Payload:**
  ```json
  { "status": "RESOLVED", "resolutionNotes": "Issued partial refund and apology" }
  ```
- **Response Data:** `{}`

#### SCR-55 Global Reports
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/reports/revenue`
- **Allowed Roles:** ADMIN
- **Request Payload:** None
- **Response Data:**
  ```json
  { "monthlyData": [{ "month": "2026-06", "revenue": 500000000 }] }
  ```

#### SCR-56 System Administration
**[Call 1 — Get Activity Logs (Activity Logs tab)]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/logs`
- **Allowed Roles:** ADMIN
- **Request Payload:** None (Query: `?page=0&size=50`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "user": { "fullName": "Admin User" },
        "action": "BOOKING_CHECKED_IN",
        "entityType": "Booking",
        "entityId": "uuid",
        "createdAt": "2026-06-25T10:00:00Z"
      }
    ]
  }
  ```

**[Call 2 — Get System Settings (Settings tab)]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/settings`
- **Allowed Roles:** ADMIN
- **Request Payload:** None
- **Response Data:**
  ```json
  [
    { "key": "DEPOSIT_PERCENTAGE", "value": "40", "description": "Deposit percentage required at booking", "updatedAt": "2026-06-01T00:00:00Z" },
    { "key": "HOLD_TIMEOUT_MINUTES", "value": "10", "description": "Minutes before unpaid booking expires (default 10; configurable)", "updatedAt": "2026-07-23T00:00:00Z" }
  ]
  ```

**[Call 3 — Update System Setting (Settings tab)]**
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/admin/settings/{key}`
- **Allowed Roles:** ADMIN
- **Request Payload:**
  ```json
  { "value": "50" }
  ```
- **Response Data:** `{}`

**[Call 4 — Get Reviews for Moderation (Content Moderation tab)]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/reviews`
- **Allowed Roles:** ADMIN
- **Request Payload:** None (Query: `?status=PUBLISHED&page=0&size=20`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "customer": { "fullName": "John Doe" },
        "room": { "roomNumber": "101" },
        "rating": 2,
        "comment": "Poor experience",
        "status": "PUBLISHED"
      }
    ]
  }
  ```

**[Call 5 — Update Review Status (Hide/Publish — Content Moderation tab)]**
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/admin/reviews/{id}/status`
- **Allowed Roles:** ADMIN
- **Request Payload:**
  ```json
  { "status": "HIDDEN" }
  ```
- **Response Data:** `{}`

#### SCR-57 Promotion Management
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/admin/promotions`
- **Allowed Roles:** ADMIN
- **Request Payload:** None
- **Response Data:**
  ```json
  { "content": [ { "id": "uuid", "title": "Summer Sale", "isActive": true } ] }
  ```

#### SCR-58 Add / Edit Promotion
**[Call 1 — Create Promotion]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/admin/promotions`
- **Allowed Roles:** ADMIN
- **Request Payload:**
  ```json
  {
    "title": "Summer Sale",
    "subtitle": "20% off weekends",
    "description": "Valid for all room types from July to August",
    "ctaText": "Book Now",
    "ctaUrl": "/rooms?promo=summer2026",
    "colorTheme": "Blue",
    "sortOrder": 1,
    "isActive": true
  }
  ```
- **Response Data:** `{}`

**[Call 2 — Edit Promotion]**
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/admin/promotions/{id}`
- **Allowed Roles:** ADMIN
- **Request Payload:**
  ```json
  {
    "title": "Summer Sale Extended",
    "subtitle": "25% off weekends",
    "isActive": true
  }
  ```
- **Response Data:** `{}`

---

### Section 6  Employee Screens (SCR-59 to SCR-65)

#### SCR-59 Employee Dashboard
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/employees/me/dashboard`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "pendingHousekeeping": 3,
    "pendingMaintenance": 1,
    "todayInspections": 2
  }
  ```

#### SCR-60 Housekeeping Workspace
**[Call 1 — List My Housekeeping Tasks]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/employees/housekeeping`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:** None (Query: `?status=PENDING`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "room": { "id": "uuid", "roomNumber": "101" },
        "status": "PENDING",
        "note": "Post-checkout clean",
        "createdAt": "2026-06-25T10:00:00Z"
      }
    ]
  }
  ```

**[Call 2 — Start Housekeeping Task]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/employees/housekeeping/{id}/start`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:** `{}`
- **Response Data:** `{}`

**[Call 3 — Finish Housekeeping Task]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/employees/housekeeping/{id}/finish`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:** `{}`
- **Response Data:** `{}`

#### SCR-61 Maintenance Workspace
**[Call 1 — List My Maintenance Tasks]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/employees/maintenance`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:** None (Query: `?status=ASSIGNED`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "title": "Broken AC",
        "room": { "id": "uuid", "roomNumber": "101" },
        "status": "ASSIGNED",
        "createdAt": "2026-06-25T10:00:00Z"
      }
    ]
  }
  ```

**[Call 2 — Update Maintenance Status]**
- **HTTP Method:** PUT
- **Endpoint:** `/api/v1/employees/maintenance/{id}/status`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:**
  ```json
  { "status": "RESOLVED", "note": "Replaced fan motor. AC now operational." }
  ```
- **Response Data:** `{}`

#### SCR-62 Room Inspection Hub
**[Call 1 — List My Assigned Inspections]**
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/employees/inspections`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:** None (Query: `?status=PENDING`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "room": { "id": "uuid", "roomNumber": "101" },
        "bookingId": "uuid",
        "status": "PENDING",
        "inspectedAt": null
      }
    ]
  }
  ```

**[Call 2 — Pass Inspection]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/employees/inspections/{id}/pass`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:**
  ```json
  { "note": "All items in good condition. Room clean." }
  ```
- **Response Data:** `{}`

**[Call 3 — Fail Inspection (Found Damage)]**
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/employees/inspections/{id}/fail`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:**
  ```json
  { "note": "Cracked TV screen and damaged minibar door found." }
  ```
- **Response Data:** `{}`

#### SCR-63 Damage Report List
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/employees/damage-reports`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:** None
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "room": { "id": "uuid", "roomNumber": "101" },
        "totalEstimatedCost": 2500000,
        "status": "PENDING_APPROVAL",
        "createdAt": "2026-06-25T10:00:00Z"
      }
    ]
  }
  ```

#### SCR-64 Create Damage Report
- **HTTP Method:** POST
- **Endpoint:** `/api/v1/employees/damage-reports`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:** (Multipart Form Data)
  `inspectionId`: `"uuid"`
  `items[0].name`: `"TV"`
  `items[0].estimatedCost`: `6000000`
  `photos`: `[file1]`
- **Response Data:** `{}`

#### SCR-65 Property Room List
- **HTTP Method:** GET
- **Endpoint:** `/api/v1/employees/rooms`
- **Allowed Roles:** EMPLOYEE
- **Request Payload:** None (Query: `?status=AVAILABLE`)
- **Response Data:**
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "roomNumber": "101",
        "roomType": "Deluxe",
        "status": "AVAILABLE",
        "capacity": 2,
        "pricePerNight": 1000000
      }
    ]
  }
  ```

---

## 5. Shared DTOs

**PaginationResponse:**
```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 50,
  "totalPages": 5
}
```

**ImageDTO:**
```json
{
  "id": "uuid",
  "url": "https://storage.example.com/image.jpg",
  "type": "GALLERY"
}
```

**BookingStatusEnum:**
| Status Value | Display Label | Description |
|---|---|---|
| `PENDING_DEPOSIT` | Pending Deposit | Booking created, awaiting deposit payment (**10-min** hold default) |
| `CONFIRMED` | Confirmed | Deposit paid and verified, room reserved |
| `CHECKED_IN` | Checked-in | Guest has physically checked in |
| `PENDING_INSPECTION` | Pending Inspection | Check-out requested; awaiting employee room inspection |
| `PENDING_DAMAGE_PAYMENT` | Pending Damage Payment | Damage found; customer must pay damage fee before check-out |
| `CHECKED_OUT` | Checked-out | Guest has departed; booking complete |
| `CANCELLED` | Cancelled | Cancelled by customer/manager/system; deposit refunded **per tier / manager 100%** policy |
| `NO_SHOW` | No-show | Customer never checked in; deposit forfeited |

## 6. WebSocket Events

WebSockets are used to provide real-time updates for critical flows to avoid continuous HTTP polling.
- **Endpoint:** `ws://api.example.com/ws`

### Events:
- **`BOOKING_STATUS_CHANGED`**:
  - Sent to Customer and Manager.
  - Payload: `{ "bookingId": "uuid", "status": "CONFIRMED" }`
- **`PAYMENT_VERIFIED`**:
  - Sent to Customer when Manager approves payment.
  - Payload: `{ "paymentId": "uuid", "status": "PAID" }`
- **`ROOM_AVAILABILITY_UPDATED`**:
  - Broadcasted to all connected clients viewing the calendar.
  - Payload: `{ "roomId": "uuid", "date": "2026-06-15", "status": "OCCUPIED" }`
- **`NEW_MAINTENANCE_TICKET`**:
  - Sent to Manager when Customer creates a ticket.
  - Payload: `{ "ticketId": "uuid", "title": "Broken AC" }`
- **`DAMAGE_REPORT_CREATED`**:
  - Sent to Manager when Employee submits a Damage Report.
  - Payload: `{ "damageReportId": "uuid", "roomNumber": "101", "totalEstimatedCost": 2500000 }`
- **`INSPECTION_COMPLETED`**:
  - Sent to Manager when Employee completes a Room Inspection (Pass or Fail).
  - Payload: `{ "inspectionId": "uuid", "bookingId": "uuid", "status": "PASSED" }`
- **`HOUSEKEEPING_TASK_UPDATED`**:
  - Sent to Manager when Employee updates housekeeping task status.
  - Payload: `{ "taskId": "uuid", "roomNumber": "101", "status": "COMPLETED" }`

## 7. State Transition Rules

### Booking Status Flow:
`PENDING_DEPOSIT` → `CONFIRMED` → `CHECKED_IN` → `PENDING_INSPECTION` → (Optional: `PENDING_DAMAGE_PAYMENT`) → `CHECKED_OUT`
- **Cancel Path:** `PENDING_DEPOSIT` / `CONFIRMED` → `CANCELLED`
- **No-show Path:** `CONFIRMED` → `NO_SHOW` (triggered when customer never checks in by departure date; deposit forfeited)

### Room Status Flow:
`AVAILABLE` → `PENDING_DEPOSIT` (Hold placed) → `RESERVED` (Deposit paid) → `OCCUPIED` (Checked in) → `PENDING_CLEANING` (Checked out) → `CLEANING_IN_PROGRESS` → `AVAILABLE`
- **Maintenance Path:** `AVAILABLE` → `MAINTENANCE` → `AVAILABLE`
- **Out of Service Path:** Any status → `OUT_OF_SERVICE` (Manager override)

### Maintenance Status Flow:
`OPEN` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`

### RoomInspection Status Flow:
`PENDING` → `IN_PROGRESS` → `PASSED` (Booking eligible for Check-out)
`IN_PROGRESS` → `FAILED_WITH_DAMAGE` (Creates DamageReport; Check-out blocked until Damage paid)

### HousekeepingTask Status Flow:
`PENDING` (Auto-created on Booking Check-out) → `IN_PROGRESS` (Employee starts; Room = Cleaning In Progress) → `COMPLETED` (Employee finishes; Room = Available)
- **Cancel Path:** `PENDING` / `IN_PROGRESS` → `CANCELLED` (Manager override)

### DamageReport Status Flow:
`DRAFT` → `PENDING_APPROVAL` → `APPROVED` (Booking → Pending Damage Payment) → `PAID`
`PENDING_APPROVAL` → `DISPUTED` (Customer disputes)
- **Escalation Rule:** If `totalEstimatedCost` > 5,000,000 VND → Manager routes to Admin (SCR-53) for co-approval before `APPROVED`.

## 8. API Change Log
| Version | Date | Author | Changes |
|---------|------------|-------------|---------|
| 2.2.0 | 2026-07-03 | Antigravity | Updated endpoints per entity-ui-mapping.md: fixed housekeeping, maintenance, damage-report, contract, payment, rooms, inspection, user directory paths; added missing calls (SCR-14 GET, SCR-37 GET, SCR-62 3-call expansion, SCR-43/SCR-53 reject/co-approve); enriched response DTOs (damageFeeAmount, holdExpiresAt, cancelReason, specialRequests, requiresAdminEscalation); added SCR-25 PUT/DELETE, SCR-56 tabs; expanded Section 5 with BookingStatusEnum; added 3 WebSocket events; expanded Section 7 with RoomInspection, HousekeepingTask, DamageReport flows and NO_SHOW path. |
| 2.1.0 | 2026-06-29 | Antigravity | Fully restructured to map all 65 screens with RBAC requirements. |
| 2.0.0 | 2026-06-20 | Arch Team | Added Admin & Employee role endpoints. |
| 1.0.0 | 2026-06-01 | Dev Team | Initial API spec for Guest and Customer. |
| 2.3.0 | 2026-07-23 | Docs Sync | Aligned with sdd/004+012: cancel deposit tiers, hold 10m, VNPay-only (TD-011), SCR-37=check-in/out, SCR-52 deferred (TD-010). |
