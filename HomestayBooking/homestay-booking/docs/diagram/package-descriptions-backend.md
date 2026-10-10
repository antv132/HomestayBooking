# Package Descriptions — Backend

**Project:** Homestay Booking System  
**Root package:** `com.homestay`  
**Stack:** Java 17 · Spring Boot 3 · Spring Data JPA · SQL Server  

| No | Package | Description |
|----|---------|-------------|
| 01 | Controller | Entry point REST API. Nhận HTTP request, validate đầu vào, ủy thác nghiệp vụ xuống Service, trả `ResponseEntity` / DTO. Không chứa business logic; không truy cập Repository trực tiếp. Ví dụ: `AuthController`, `BookingV1Controller`, `PaymentV1Controller` (~68 controllers). |
| 02 | Service | Tầng nghiệp vụ cốt lõi. Xử lý booking, payment, contract, invoice, housekeeping, maintenance, dashboard…; map Entity ↔ DTO; gọi Repository / Config khi cần. Ví dụ: `BookingService`, `PaymentService`, `ContractService` (~54 `*Service` + helper nội bộ). |
| 03 | Repository | Tầng truy cập dữ liệu. Interface Spring Data JPA (`extends JpaRepository`) — query / persist Entity. Một repository tương ứng một (hoặc nhóm) Entity. Ví dụ: `BookingRepository`, `UserRepository`, `PaymentRepository` (28 repositories). |
| 04 | Entity | Mô hình miền ánh xạ bảng SQL Server (`@Entity`, `@Table`, snake_case). Không expose trực tiếp qua API — chỉ dùng trong Service / Repository. Ví dụ: `Booking`, `Payment`, `Room`, `RoomStatusBlock` (29 entities). |
| 05 | Dto | Đối tượng biên API (Request / Response). Controller và Service dùng DTO để nhận/trả dữ liệu; tách biệt Entity khỏi client. Tổ chức `dto.request` / `dto.response`. Ví dụ: `CreateBookingRequest`, `BookingDetailResponse`. |
| 06 | Security | Xác thực & phân quyền JWT. Filter chain (`JwtAuthFilter`), phát hành token (`JwtTokenProvider`), cấu hình quyền (`SecurityConfig`), kiểm tra phạm vi property (`PropertyAccessValidator`). «access» tới Controller trước khi vào endpoint. |
| 07 | Config | Cấu hình hạ tầng Spring (beans, CORS/static, mail, WebSocket, VNPay, seed/schema patch). Được Service/Controller «use» (vd. `VNPayConfig`); một số class «use» Security / Repository (WS auth, seed). Ví dụ: `WebConfig`, `WebSocketConfig`, `MailConfig`. |
| 08 | Job | Tác vụ nền `@Scheduled`. Xử lý sự kiện theo thời gian (hold timeout, no-show, outbox) bằng cách «use» Service — không nhận HTTP. Ví dụ: `BookingHoldTimeoutJob`, `BookingNoShowJob`, `OutboxProcessorJob`. |
| 09 | Exception | Xử lý lỗi tập trung. Định nghĩa exception nghiệp vụ (`BusinessException`, `ResourceNotFoundException`, `ConflictException`…) và `GlobalExceptionHandler` (`@ControllerAdvice`) map sang HTTP status chuẩn. |
| 10 | Enums | Kiểu liệt kê miền (UPPER_SNAKE). Chuẩn hóa trạng thái / hằng số dùng chung (vd. booking status, invoice/complaint status). Không thêm giá trị ngoài spec. |
| 11 | Util | Tiện ích thuần / helper nội bộ Backend (không chứa business rule lớn). Hỗ trợ các tầng khác khi cần hàm dùng chung. |

---

### Quan hệ gói (tóm tắt)

```
Controller ──«use»──► Service ──«use»──► Repository ──«import»──► Entity
     │                    │                    │
     │                    │                    └──«use»──► Database (SQL Server)
     ├──«use»──► Dto ◄────┘
     └──«use»──► Config ◄── Service
Security ──«access»──► Controller
Security / Config ──«use»──► Repository
Job ──«use»──► Service
```

### Ghi chú đặt tên

| Package | Quy tắc lớp |
|---------|-------------|
| Controller | `*Controller` · tiền tố role / `V1` khi versioned |
| Service | `*Service` |
| Repository | `*Repository` |
| Entity | PascalCase, danh từ số ít |
| Dto | `*Request` / `*Response` |
| Job | `*Job` |
| Security | `Jwt*`, `SecurityConfig`, `*Validator` |
