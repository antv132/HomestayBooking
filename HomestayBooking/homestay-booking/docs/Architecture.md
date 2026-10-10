# Architecture — Homestay Booking System

**Project:** Homestay Booking System  
**Type:** Full-stack Web App (SPA + REST API)  
**Updated:** 2026-07-27  

Nguồn ưu tiên khi xung đột: `sdd/services/homestay/features/<feature>/spec.md` > `AGENTS.md` > tài liệu này.

---

## 1. Mục tiêu kiến trúc

- Quản lý đặt phòng homestay end-to-end: tìm phòng → giữ chỗ → cọc/thanh toán → hợp đồng → check-in/out → kiểm tra hư hại → vận hành (HK/MT).
- Tách rõ **client SPA** và **API stateless**; phân quyền theo role.
- Business rules nằm ở **Service**; Controller chỉ validate và ủy thác.
- Inventory an toàn: **pessimistic lock phòng** + kiểm tra overlap ngày (half-open).

---

## 2. Tech stack

| Tầng | Công nghệ |
|------|-----------|
| Frontend | React 18, TypeScript, Vite, React Router, Axios, TanStack React Query, Zustand, Tailwind CSS 3 + `components/ui` (shadcn-style) |
| Backend | Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, Maven |
| Database | SQL Server |
| Auth | JWT (access + refresh), bcrypt, Google OAuth |
| Payment | VNPay (sandbox/prod qua config); tiền còn lại có thể thu **CASH** tại quầy |
| Async / jobs | Outbox pattern + `@Scheduled` (hold timeout, no-show, outbox processor) |
| Realtime | WebSocket (thông báo) |
| Docs API | OpenAPI / Swagger |
| Spec | Spec Kit + `sdd/services/homestay/features/` |

---

## 3. Kiến trúc tổng thể (context)

> PlantUML: [`diagram/system-architecture.puml`](../diagram/system-architecture.puml)  
> (3 trang: Context · Layered Backend · Frontend packages)

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser (SPA)                           │
│   React · Vite · TanStack Query · Zustand · Axios · Tailwind    │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTPS
                                │ Authorization: Bearer <access_token>
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot API (:8080)                      │
│  Security (JWT) → Controller → Service → Repository → Entity    │
│  Jobs (@Scheduled) ──use──► Service                             │
└───────┬───────────────────┬───────────────────┬─────────────────┘
        │                   │                   │
        ▼                   ▼                   ▼
 ┌────────────┐     ┌─────────────┐      ┌────────────┐
 │ SQL Server │     │   VNPay     │      │ Email/SMTP │
 │  (JPA)     │     │ Return URL  │      │ + Outbox   │
 └────────────┘     └─────────────┘      └────────────┘
        │
        ▼
 ┌────────────┐
 │  uploads/  │  (ảnh phòng, CMND, promotions, PDF hợp đồng…)
 └────────────┘
```

**Workspace:**

| Thư mục | Vai trò |
|---------|---------|
| `Frontend/` | SPA |
| `Backend/` | REST API (`com.homestay`) |
| `sdd/` | Spec-as-Code (FR / SCR) |
| `docs/` | SRS narrative, screen, architecture |
| `diagram/` | ERD, workflow, screenflow |

---

## 4. Backend — Layered Architecture

```
HTTP Request
    │
    ▼
┌───────────────────┐
│ Security          │  JwtAuthFilter · SecurityConfig · role / property scope
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ Controller        │  REST · validation · ResponseEntity<ApiResponse<T>>
│ (dto.request)     │  Không chứa business logic; không gọi Repository
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ Service           │  Nghiệp vụ · map Entity ↔ DTO · transaction
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ Repository        │  Spring Data JPA / JPQL
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ Entity            │  Ánh xạ bảng SQL Server (snake_case)
└───────────────────┘
```

### 4.1 Package (`com.homestay`)

| Package | Trách nhiệm |
|---------|-------------|
| `controller` | Entry REST (`*Controller`) |
| `service` | Business (`*Service`) |
| `repository` | `JpaRepository` |
| `entity` | 29 entities miền |
| `dto` | `*Request` / `*Response` |
| `security` | JWT, filter, property access |
| `config` | CORS, mail, VNPay, WebSocket, upload… |
| `job` | `@Scheduled` background |
| `exception` | `BusinessException`… + `GlobalExceptionHandler` |
| `enums` / `util` | Enum miền, helper thuần |

Chi tiết: [`docs/diagram/package-descriptions-backend.md`](diagram/package-descriptions-backend.md).

### 4.2 API style

- REST: `/api/...`, ưu tiên `/api/v1/...` khi versioned.
- Auth header: `Authorization: Bearer <access_token>`.
- Public (một phần): auth register/login, search/rooms, promotions, VNPay return.
- Role-scoped: `/api/v1/admin/...`, `/manager/...`, `/employee/...`, customer paths tương ứng.

### 4.3 Background jobs

| Job (ví dụ) | Mục đích |
|-------------|----------|
| `BookingHoldTimeoutJob` | Hủy `PENDING_DEPOSIT` hết hold |
| `BookingNoShowJob` | `CONFIRMED` → `NO_SHOW` sau hạn |
| Outbox processor | Gửi email / sinh PDF hợp đồng bất đồng bộ |

---

## 5. Frontend — SPA Architecture

```
App / Router
    │
    ├── layouts/*Layout     (shell theo role)
    ├── pages/[role|public] (màn hình)
    │         │
    │         ├── api/*Api.ts     ← biên duy nhất tới Backend
    │         ├── store (Zustand) ← auth / UI client state
    │         ├── hooks           ← WebSocket, search…
    │         └── components/ui
    └── TanStack Query            ← server state cache
```

| Thư mục | Vai trò |
|---------|---------|
| `pages/` | Màn theo `admin` · `manager` · `employee` · `customer` · `public` |
| `api/` | Axios modules (`bookingApi`, `paymentApi`…) |
| `layouts/` | Nav / sidebar theo role |
| `components/` | UI kit + domain widgets + route guards |
| `store/` | `authStore` (không nhét list server vào đây) |
| `hooks/` / `utils/` | Logic tái sử dụng, pure helpers |

Chi tiết: [`docs/diagram/package-descriptions-frontend.md`](diagram/package-descriptions-frontend.md).

---

## 6. Bảo mật & phân quyền

### 6.1 Auth

- **Access JWT** + **Refresh token** (bảng `refresh_tokens`; revoke khi logout/rotate).
- Password: bcrypt; Google OAuth optional (`google_id`).
- Customer mặc định `INACTIVE` đến khi verify OTP email.

### 6.2 Roles

| Role | Phạm vi chính |
|------|----------------|
| `ADMIN` | Hệ thống, property, gán manager, báo cáo toàn cục, moderation, escalation |
| `MANAGER` | Property được gán: booking, check-in/out, inspection, damage, báo cáo |
| `EMPLOYEE` | HK / MT / dashboard trong property được gán |
| `CUSTOMER` | Đặt phòng, thanh toán, hợp đồng, review, ticket |

Phạm vi property: `ManagerPropertyAssignment` / `EmployeePropertyAssignment` + validator phía Security/Service.

---

## 7. Miền dữ liệu (tóm tắt)

- **29 entities** — ERD: [`diagram/erd-crowsfoot.puml`](../diagram/erd-crowsfoot.puml).
- Spine: `User` → `Booking` → `Payment` / `Invoice` / `Contract` / `Review` / `BookingCheckVerification` → `RoomInspection` → `DamageReport` → `DamageItem`.
- Property tree: `Property` → `Floor` → `Room` (+ `RoomImage`, `RoomStatusBlock`).
- Ops: `HousekeepingTask`, `MaintenanceTicket`, checklist answers.
- Infra: `OutboxEvent`, `Promotion`, `Attachment` (polymorphic `entity_type` + `entity_id`).

### Booking status (canonical)

`PENDING_DEPOSIT` → `CONFIRMED` → `CHECKED_IN` → `PENDING_INSPECTION` → (`PENDING_DAMAGE_PAYMENT`) → `CHECKED_OUT`  
nhánh: `CANCELLED` · `NO_SHOW`

### Payment

| Field | Giá trị |
|-------|---------|
| `type` | `DEPOSIT` · `REMAINING_BALANCE` · `DAMAGE_FEE` |
| `method` | `VNPAY` · `CASH` · `BANK_TRANSFER` |
| Deposit / remaining | 40% / 60% |

---

## 8. Luồng nghiệp vụ cốt lõi

```
Search / chọn phòng
        │
        ▼
  PENDING_DEPOSIT ──(hold timeout)──► CANCELLED
        │ VNPay DEPOSIT OK
        ▼
    CONFIRMED (+ Contract PDF qua Outbox)
        │
        ├──(no-show job)──► NO_SHOW
        │
        ▼ Check-in (CMND + remaining CASH/VNPay)
    CHECKED_IN
        │
        ▼ Check-out → inspection
 PENDING_INSPECTION
        │
        ├── passed ──────────────► CHECKED_OUT
        └── damage fee > 0 ──────► PENDING_DAMAGE_PAYMENT ─pay─► CHECKED_OUT
```

Chi tiết UI: [`User-Manual-Workflows.md`](User-Manual-Workflows.md) · diagram: [`diagram/booking-workflow.puml`](../diagram/booking-workflow.puml).

---

## 9. Tích hợp bên ngoài

| Hệ thống | Cách dùng | Ghi chú |
|----------|-----------|---------|
| **VNPay** | Redirect + **Return URL** xác nhận | IPN deferred (tech debt) |
| **SMTP / Mail** | OTP, thông báo, gửi hợp đồng | Ưu tiên qua Outbox |
| **Google OAuth** | Đăng nhập không password | `google_id` trên `User` |
| **File storage** | Thư mục `uploads/` phục vụ static | Giới hạn upload 10MB |

---

## 10. Quyết định kiến trúc (ADR)

| ID | Quyết định |
|----|------------|
| ADR-001 | JWT access + refresh thay session server |
| ADR-002 | Spring Data JPA / Hibernate trên SQL Server; lock phòng + overlap |
| ADR-003 | React 18 + Vite + TanStack Query + Zustand; UI kit `components/ui` |
| ADR-004 | Enum booking status chuẩn theo feature `004` |
| ADR-005 | Deposit 40% / remaining 60%; contract PDF; Outbox cho async |
| ADR-006 | Spec Kit / cursor-agent; ưu tiên `sdd/.../spec.md` |
| ADR-007 | Online = VNPay; cash remaining tại quầy; confirm VNPay qua Return URL |

---

## 11. Chất lượng & ràng buộc

- Không business logic trong Controller; không expose Entity ra API.
- Không invent booking status ngoài enum canonical.
- Critical path (Auth/Booking/Payment): coverage theo `docs/testing.md` / `testing/coverage-control.md` khi có.
- Secrets chỉ qua env / config local — không commit `.env`.
- Timezone nghiệp vụ vận hành: **Asia/Ho_Chi_Minh** (dashboard / “hôm nay”).

---

## 12. Tài liệu liên quan

| Tài liệu | Nội dung |
|----------|----------|
| [`AGENTS.md`](../AGENTS.md) | Luật agent + stack |
| [`CLAUDE.md`](../CLAUDE.md) | ADR + sprint notes |
| [`diagram/package-descriptions-backend.md`](diagram/package-descriptions-backend.md) | Package BE |
| [`diagram/package-descriptions-frontend.md`](diagram/package-descriptions-frontend.md) | Package FE |
| [`diagram/system-architecture.puml`](../diagram/system-architecture.puml) | Architecture PlantUML (context + layers + FE) |
| [`diagram/erd-crowsfoot.puml`](../diagram/erd-crowsfoot.puml) | ERD Crow’s Foot |
| [`entity-ui-mapping.md`](entity-ui-mapping.md) | Entity ↔ UI |
| [`api-spec-by-screen.md`](api-spec-by-screen.md) | API ↔ màn hình |
| `sdd/services/homestay/features/` | Spec từng feature |
