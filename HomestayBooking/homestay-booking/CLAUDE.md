# CLAUDE.md — Claude Code Project Memory
# Đọc @AGENTS.md trước để hiểu full project context
# Updated: 2026-07-23 | Project: Homestay Booking System
# Location: repo root (auto-loaded by Claude Code)

## MANUAL MEMORY (human-maintained)

### Architecture Decisions (ADR)

# ADR-001: JWT (access + refresh) thay vì session server — API stateless, hỗ trợ SPA + role isolation
# ADR-002: Spring Data JPA / Hibernate trên SQL Server — khớp stack môn SWP; inventory dùng pessimistic Room lock + overlap check (không dùng Prisma/TypeORM)
# ADR-003: React 18 + Vite + TanStack Query + Zustand — server state / client state tách rõ; UI kit custom `components/ui` (shadcn-style)
# ADR-004: Booking status enum chuẩn theo sdd/services/homestay/features/004 — PENDING_DEPOSIT → CONFIRMED → CHECKED_IN → … → CHECKED_OUT | CANCELLED | NO_SHOW
# ADR-005: Deposit 40% / remaining 60%; contract PDF sinh sau khi cọc confirmed; ưu tiên Outbox cho email/async khi đã có
# ADR-006: Spec Kit integration = cursor-agent (`.cursor/skills/speckit-*`); nguồn yêu cầu ưu tiên `sdd/services/homestay/features/<feature>/spec.md`
# ADR-007: Online pay = VNPay (TD-011). **Cash remaining tại quầy** khi check-in được giữ (`remainingCollected` → Payment CASH PAID). Không mở lại CK/biên lai. Confirm VNPay qua **Return URL** only (TD-012 — IPN deferred).

### Lessons Learned (từ incidents và code review)

# LESSON-001: Không double-declare export cùng tên (vd. `isRoomBookingBlocked` trong `roomCalendar.ts`) — gây trắng trang Vite
# LESSON-002: FE About CMS (`/api/admin/about`) cần BE endpoint tương ứng — thiếu API → 500; stash/pull DEV có thể làm mất code WIP
# LESSON-003: Hold timeout mặc định = 10 phút (spec + `application.yml`); configurable via `BOOKING_HOLD_TIMEOUT_MINUTES`
# LESSON-004: No-show cần job — đã có `BookingNoShowJob` + `markNoShowExpiredConfirmed` (FR-008 / TD-001 Done)
# LESSON-005: Legacy `/api/bookings/.../check-in|out` đã block → dùng manager `/api/v1/...` (CMND + inspection) (TD-004 Done)
# LESSON-006: Customer cancel MUST theo tier ≥7d/3–7d/<3d trên cọc đã trả — đã align code (FR-010 / TD-002 Done); đừng flat 50%
# LESSON-007: Validate kích thước/type file TRƯỚC khi ghi disk — constitutional max **10MB** (`UploadLimits`, multipart yml)
# LESSON-008: Index / filter theo `property_id`, `room_id`, `booking_id`, `customer_id`, `status`, `check_in_date` — tránh N+1 và full scan list

### Current Sprint Notes

# Sprint 2 focus: Booking workflow, payment, room availability, booking management
# Active specs: sdd/services/homestay/features/004-booking-inventory (primary), 005, 010, 012
# Done (2026-07-23 conflict-fix A–D): TD-001 NoShowJob; TD-002 cancel tiers; TD-004 legacy CI/CO block;
#   TD-005 FR-015 modify Confirmed; TD-006 contract Outbox; TD-007 calendar 8-status; OTP 15m+3/h + Google FR-006; upload 10MB
# Done (2026-07-23 P0): VNPay return DEPOSIT+REMAINING+DAMAGE_FEE; damage fee VNPay + unlock checkout (PENDING_INSPECTION)
# Done (2026-07-23 P1-A): refresh TTL 7d (config); refresh blocks SUSPENDED/INACTIVE; forgot-password anti-enumeration
# Done (2026-07-23 P1-B): RoomStatusBlock date-scoped; HK/overlap gates; half-open calendar; FE days[]; legacy status scoped
# Done (2026-07-23 P1-C): deposit hold countdown (holdExpiresAt); About/Landing copy VNPay + cash desk only
# Deferred (edu Spec Sync — do NOT implement unless mentor reopens): TD-010 VNPay reconcile cron/SCR-52; TD-011 bank transfer + CK verify; TD-012 VNPay IPN (Return URL only)
# Kept (not conflict): cash remaining at check-in desk — ADR-007 / SCR-37 remainingCollected
# Optional polish: modify-booking room picker (dropdown); About CMS BE nếu stash chưa pop
# Deferred optional: customer damage dispute (023)
# Tracker: `sdd/sprints/tech-debt.md`
# Workspace: Backend | Frontend
# Commands FE: `npm run dev` / `npm run build` / `npm run lint` trong Frontend
# Commands BE: Maven / IDE run `com.homestay.HomestayApplication`; test `./mvnw test` (JDK 17: JAVA_HOME)

## PATTERNS TO FOLLOW

# Backend package: com.homestay — controller / service / repository / entity / dto / exception / job / security
# Service pattern: `.../service/[Name]Service.java` (vd. BookingService.java)
# Controller pattern: `.../controller/[Name]Controller.java` — return `ResponseEntity<ApiResponse<T>>`
# Map Entity→DTO trong service / static factory — không bắt buộc lớp Mapper riêng
# Job pattern: `.../job/[Name]Job.java` với `@Scheduled` (vd. BookingHoldTimeoutJob)
# Frontend API: `src/api/[name]Api.ts`
# Frontend pages: `src/pages/[role|public]/[Name]Page.tsx`
# Always create: unit test cạnh service khi thêm logic booking/payment quan trọng (`src/test/java/...`)
# Error handling: BusinessException / ResourceNotFoundException / ConflictException → GlobalExceptionHandler
# Auth header: `Authorization: Bearer <access_token>`
# Never edit: `.env*`, secrets, `node_modules/`, `dist/`, `build/`, `target/`, `coverage/`
# Conflict resolution: sdd/services/homestay/features/<feature>/spec.md > AGENTS.md > CLAUDE.md > narrative docs cũ

## AUTO MEMORY (Claude Code appends here)

# [Claude Code sẽ tự động thêm entries khi bạn làm việc]
