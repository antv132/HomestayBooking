# Feature Specification: FR-15 Notification Engine (Event-Driven)

**Feature Branch**: `017-notification-engine`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-15 Notification Engine (Event-Driven) — dựa vào docs (Specification_v2.md § FR-15, §5 Notification/OutboxEvent, entity-ui-mapping.md §1.14, api-spec-by-screen SCR-13/14, screen.md, screendesign.md, figma-generation-prompt.md Notification types, frontend notificationApi.ts, NotificationPages.tsx)"

**Phụ thuộc**: FR-01 (auth — all roles inbox); FR-04 (`outbox_events` table, booking lifecycle events); FR-10 (contract generated — emits/consumes outbox); FR-12 (payment events); FR-13 (maintenance status events); FR-05 (availability calendar — WebSocket scope). **Ranh giới**: FR-15 owns **in-app Notification** delivery, Outbox **notification worker**, WebSocket push, inbox UI (SCR-13/14), unread counts; domain features **emit** events/outbox rows — FR-15 **không** own business logic Booking/Payment/Maintenance; FR-10 owns contract PDF/email worker (separate outbox event type); email SMTP for contract may stay FR-10 — FR-15 delivers in-app + WebSocket for same business milestones.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hệ thống gửi thông báo từ sự kiện nghiệp vụ qua Outbox (Priority: P1)

Là **hệ thống**, khi có sự kiện nghiệp vụ quan trọng (đặt phòng xác nhận, hợp đồng sẵn sàng, thanh toán, bảo trì, v.v.), tôi muốn **phân loại** và **tạo thông báo in-app** cho đúng người nhận qua **Outbox/Message Queue**, để người dùng nhận thông tin đáng tin cậy không mất khi request gốc đã commit.

**Why this priority**: FR-15 core — event-driven delivery; decouples producers from notification UI.

**Independent Test**: Insert outbox row `NOTIFICATION_DISPATCH` with payload → worker processes → `notifications` row created for target userId with correct type/title/content; failed retry logged.

**Acceptance Scenarios**:

1. **Given** domain service commits transaction và ghi **OutboxEvent** loại thông báo, **When** worker chạy, **Then** tạo bản ghi **Notification** cho `userId` đích với `type`, `title`, `content`, `relatedEntityId`, `relatedEntityType`, `isRead=false`.
2. **Given** sự kiện **Booking Confirmed**, **When** worker xử lý, **Then** Customer nhận notification type **Booking Confirmed** với liên kết tới booking.
3. **Given** sự kiện **Payment Pending** (bank transfer), **When** worker xử lý, **Then** Manager được gán property nhận notification yêu cầu xác minh.
4. **Given** sự kiện **Maintenance Status Changed**, **When** worker xử lý, **Then** Customer (ticket owner) nhận notification cập nhật trạng thái.
5. **Given** Outbox xử lý **thất bại**, **When** retry vượt ngưỡng, **Then** đánh dấu **FAILED** và ghi log cho Admin theo dõi (US6).
6. **Given** cùng event replay/idempotent key, **When** worker chạy lại, **Then** không tạo notification trùng lặp.

---

### User Story 2 - Người dùng xem danh sách thông báo và số chưa đọc (Priority: P1)

Là **người dùng đã đăng nhập** (Customer, Employee, Manager, Admin), tôi muốn xem **danh sách thông báo** của mình, lọc theo đã đọc/chưa đọc, và thấy **số lượng chưa đọc**, để theo dõi cập nhật quan trọng (SCR-13).

**Why this priority**: FR-15 explicit inbox list + unread count; shared across all roles.

**Independent Test**: User A login → GET notifications → chỉ của A; unread count matches list; filter Unread works.

**Acceptance Scenarios**:

1. **Given** user có notifications, **When** mở Notification Center (SCR-13), **Then** thấy danh sách `title`, `content` (truncate), `type`, `createdAt`, badge chưa đọc — **chỉ của user hiện tại**.
2. **Given** user có 3 unread, **When** gọi unread count, **Then** trả **3** (hoặc tương đương trên dashboard widget).
3. **Given** filter **Unread**, **When** áp dụng, **Then** chỉ hiển thị `isRead=false`.
4. **Given** user A, **When** cố đọc notification của user B, **Then** từ chối.
5. **Given** danh sách dài, **When** phân trang, **Then** sắp xếp **mới nhất trước** (assumption: 20 per page).

---

### User Story 3 - Người dùng xem chi tiết và đánh dấu đã đọc (Priority: P1)

Là **người dùng đã đăng nhập**, tôi muốn mở **chi tiết** một thông báo, tự động hoặc thủ công **đánh dấu đã đọc**, và **đánh dấu tất cả đã đọc**, để quản lý inbox (SCR-14).

**Why this priority**: FR-15 explicit mark read single + all.

**Independent Test**: Open detail → isRead true; mark-all → all unread become read; unread count → 0.

**Acceptance Scenarios**:

1. **Given** notification chưa đọc thuộc user, **When** mở chi tiết (SCR-14), **Then** hiển thị full `title`, `content`, `type`, `createdAt`; `isRead` → **true**.
2. **Given** notification chưa đọc, **When** đánh dấu đã đọc (không mở detail), **Then** `isRead=true`; unread count giảm 1.
3. **Given** user có nhiều unread, **When** chọn **Mark all as read**, **Then** tất cả notifications của user → `isRead=true`; unread count = 0.
4. **Given** notification đã đọc, **When** mở lại, **Then** vẫn hiển thị nội dung; không lỗi.
5. **Given** user A, **When** cố mark read notification của B, **Then** từ chối.

---

### User Story 4 - Push thông báo real-time qua WebSocket (Priority: P1)

Là **người dùng đang online**, tôi muốn nhận **cập nhật tức thì** trên giao diện khi có sự kiện **Booking**, **Payment**, hoặc **Availability Calendar** thay đổi, để không phải refresh thủ công.

**Why this priority**: FR-15 explicit WebSocket for Booking/Payment/Calendar.

**Independent Test**: Client subscribed with JWT → business event creates notification → WebSocket message received within seconds; unread badge updates.

**Acceptance Scenarios**:

1. **Given** user đã kết nối WebSocket với JWT hợp lệ, **When** notification mới tạo cho user đó, **Then** client nhận message push kèm `notificationId`, `type`, `title` (summary).
2. **Given** sự kiện **Payment Confirmed** cho Customer, **When** notification dispatched, **Then** Customer client nhận push type Payment.
3. **Given** sự kiện **Availability Calendar** thay đổi (FR-05) cho Manager/Employee scope, **When** notification/event broadcast, **Then** subscribed clients trong scope nhận cập nhật (assumption: room/property channel).
4. **Given** WebSocket disconnect, **When** user reconnect, **Then** inbox list vẫn đầy đủ qua REST; không mất notifications.
5. **Given** user không online, **When** event xảy ra, **Then** notification vẫn lưu DB; user thấy khi login sau.

---

### User Story 5 - Điều hướng nhanh từ thông báo (Priority: P2)

Là **người dùng**, khi mở chi tiết thông báo, tôi muốn **nút hành động** hoặc liên kết dẫn thẳng tới màn hình liên quan (booking, contract, payment, maintenance ticket), để xử lý việc ngay.

**Why this priority**: FR-15 "liên kết trực tiếp để điều hướng nhanh"; figma context actions per type.

**Independent Test**: Open BOOKING_CONFIRMED notification → "View Booking" navigates to correct booking detail URL for role.

**Acceptance Scenarios**:

1. **Given** notification type **Booking Confirmed** với `relatedEntityId` booking, **When** user chọn action, **Then** điều hướng tới booking detail phù hợp role (Customer vs Manager path).
2. **Given** type **Contract Generated**, **When** action, **Then** điều hướng contract list/detail.
3. **Given** type **Payment Confirmed**, **When** action, **Then** điều hướng payment history/detail.
4. **Given** type **Maintenance Updated**, **When** action, **Then** điều hướng maintenance ticket detail.
5. **Given** `relatedEntityId` null (SYSTEM), **When** xem detail, **Then** không hiển thị action link — chỉ nội dung.

---

### User Story 6 - Admin giám sát Outbox/notification thất bại (Priority: P2)

Là **Admin**, tôi muốn xem các **OutboxEvent** hoặc notification dispatch **FAILED** trên System Administration (SCR-56), để xử lý sự cố gửi thông báo.

**Why this priority**: entity-ui-mapping Admin monitors OutboxEvent failures; operational reliability.

**Independent Test**: Force worker failure → FAILED outbox row visible to Admin → optional retry action.

**Acceptance Scenarios**:

1. **Given** Admin, **When** mở tab giám sát Outbox/Notifications, **Then** thấy danh sách events **FAILED** với `eventType`, `retryCount`, `createdAt`, lỗi tóm tắt.
2. **Given** event FAILED, **When** Admin trigger **Retry** (assumption v1 manual), **Then** worker xử lý lại; chuyển PROCESSED nếu thành công.
3. **Given** event đã PROCESSED, **When** xem, **Then** read-only audit; không retry bắt buộc.

---

### Edge Cases

- Outbox worker down — events queue PENDING until worker resumes; no data loss after DB commit.
- Duplicate domain event (replay) — idempotent notification create by `(userId, eventType, relatedEntityId, dedupeKey)`.
- User deleted/suspended — skip notification or drop with log (assumption: skip if user inactive).
- Notification content quá dài — truncate in list; full in detail.
- WebSocket auth token expired — reject connection; client falls back to REST polling (assumption v1).
- High volume burst — worker processes in batch; user inbox paginated.
- SYSTEM broadcast — optional all-users notification (assumption: Admin-only trigger, P2 stub).
- Email delivery — **out of scope** FR-15 v1 for contract email (FR-10); in-app + WebSocket only for contract ready milestone.
- Employee/Manager/Admin share same notification model — role-specific routing via event payload `recipientRole` + userId.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST ghi sự kiện thông báo qua **OutboxEvent** (hoặc queue tương đương) **trong cùng transaction** với thay đổi nghiệp vụ gốc (producer features).
- **FR-002**: FR-15 MUST chạy **worker** xử lý Outbox loại notification và tạo bản ghi **Notification** in-app.
- **FR-003**: Mỗi Notification MUST có `userId`, `title`, `content`, `type`, `relatedEntityId`, `relatedEntityType`, `isRead`, `createdAt`.
- **FR-004**: Hệ thống MUST **phân loại** notification theo `type` enum (ít nhất: Booking Confirmed, Contract Generated, Payment Confirmed, Payment Pending Verification, Maintenance Updated, System, Availability Updated).
- **FR-005**: Authenticated user MUST xem danh sách notifications **của mình** phân trang (SCR-13).
- **FR-006**: Authenticated user MUST lấy **unread count** cho badge/dashboard.
- **FR-007**: User MUST đánh dấu **một** notification đã đọc (thủ công hoặc tự động khi mở chi tiết).
- **FR-008**: User MUST **mark all as read** cho toàn bộ notifications của mình.
- **FR-009**: User MUST xem **chi tiết** notification thuộc mình (SCR-14).
- **FR-010**: FR-15 MUST hỗ trợ **WebSocket push** real-time tới user khi notification mới tạo — phạm vi tối thiểu: **Booking**, **Payment**, **Availability Calendar** events.
- **FR-011**: Notification MUST hỗ trợ **deep link** metadata (`relatedEntityType` + `relatedEntityId`) để UI điều hướng (US5).
- **FR-012**: Worker MUST **idempotent** — không duplicate notification cho cùng business event.
- **FR-013**: Outbox MUST hỗ trợ trạng thái **PENDING / PROCESSED / FAILED** và `retryCount` per Specification §5.
- **FR-014**: Admin MUST xem danh sách Outbox/notification dispatch **FAILED** (SCR-56 tab).
- **FR-015**: FR-15 MUST **không** thay thế workers chuyên biệt khác (e.g. FR-10 contract PDF generation) — chỉ consume/produce notification dispatch events.
- **FR-016**: Domain features (FR-04, FR-10, FR-12, FR-13, FR-05) MUST **emit** events — FR-15 owns delivery only.
- **FR-017**: Hệ thống MUST ghi **ActivityLog** cho NOTIFICATION_CREATED, OUTBOX_PROCESSED, OUTBOX_FAILED (assumption).
- **FR-018**: User MUST NOT đọc/mark notifications của user khác (ownership enforcement).

### Key Entities

- **Notification**: id, userId, title, content, type, relatedEntityId, relatedEntityType, isRead, createdAt.
- **OutboxEvent**: id, eventType, payload (JSON), status (PENDING/PROCESSED/FAILED), retryCount, createdAt, processedAt — shared infrastructure; FR-15 owns notification dispatch worker path.
- **NotificationType** (enum): business categories for classification and UI routing.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: **95%** notifications từ Outbox được tạo in-app trong **dưới 30 giây** sau khi business transaction commit.
- **SC-002**: **100%** inbox requests trả đúng scope — user chỉ thấy notifications của mình.
- **SC-003**: **100%** mark-read operations cập nhật unread count chính xác trong **dưới 2 giây**.
- **SC-004**: **90%** online users nhận WebSocket push trong **dưới 5 giây** sau notification create.
- **SC-005**: **0%** duplicate notifications từ replay cùng idempotency key (enforcement test).
- **SC-006**: **100%** FAILED outbox events (sau max retry) visible to Admin monitoring view.
- **SC-007**: User hoàn tất mark-all-read trong **dưới 3 giây** cho inbox ≤ 100 items.
- **SC-008**: **95%** users có thể điều hướng từ notification detail tới entity liên quan trong **1 click** (US5 types supported).

## Assumptions

- `outbox_events` table seeded by FR-04 — FR-15 adds/uses event types: `NOTIFICATION_DISPATCH`, domain-specific aliases (`MAINTENANCE_STATUS_CHANGED`, `PAYMENT_CONFIRMED`, etc.).
- Worker polls pending outbox events on a short interval (assumption: every **15 seconds**).
- Max retry **3** then FAILED; backoff between retries.
- Real-time channel requires authenticated session tied to current user (details in plan phase).
- Inbox supports list, detail, unread count, mark one read, mark all read (align with SCR-13/14).
- Email/SMS **out of scope** v1 — in-app + WebSocket only; contract email remains FR-10.
- Delete notification by user **optional** v1 — not in Specification_v2 FR-15 bullets; omit unless needed (frontend has delete — P2 optional).
- All authenticated roles share notification model; routing determines `userId` recipient per event.
- Availability WebSocket may broadcast lightweight calendar refresh payload without persisting Notification for every cell change (assumption: notify Manager on material status change only; full FR-05 integration in plan).
- Idempotency key in outbox payload: `{ eventType, userId, relatedEntityId, correlationId }`.
