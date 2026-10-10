# CONTEXT.md — FR-15 Notification Engine
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/015-notification-engine/spec.md`

## 1. PROBLEM STATEMENT

Sự kiện nghiệp vụ (booking, payment, contract, maintenance, calendar) xảy ra nhưng người dùng không nhận tin kịp thời hoặc tin bị mất sau khi request gốc đã commit.

Pain cụ thể:
- Domain service báo thành công nhưng user không có in-app notification / badge unread sai.
- User online vẫn phải F5; không có push real-time cho Booking/Payment/Calendar.
- Inbox lẫn thông báo người khác; không mark-read / mark-all được.
- Deep-link từ thông báo không dẫn đúng màn hình theo role.
- Outbox dispatch FAILED không ai giám sát/retry → mất sự kiện quan trọng.

Không mô tả giải pháp — chỉ pain: thiếu kênh event-driven tin cậy (persist + push + inbox) tách khỏi business logic domain.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| Notification | Bản ghi in-app: type, title, content, relatedEntity*, isRead |
| OutboxEvent | Hàng đợi sau commit; worker tạo Notification (idempotent) |
| NOTIFICATION_DISPATCH | Loại outbox cho in-app (khác email PDF contract FR-10) |
| Unread count | Số `isRead=false` của user hiện tại |
| WebSocket push | Đẩy realtime khi user online + JWT |
| FAILED / Retry | Outbox vượt ngưỡng retry; Admin monitor SCR-56 |

Ranh giới: Domain features **emit**; FR-15 **deliver** in-app + WS; không own Booking/Payment/Maintenance logic.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Customer / Employee / Manager / Admin | Inbox + unread + mark read (mọi role) |
| Domain services (FR-04/10/12/13/05) | Emit outbox / events |
| Admin | Giám sát FAILED + retry (P2) |
| Spec Architect / Agent Pilot | Spec + implement `015-notification-engine` |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- JWT cho REST + WebSocket; không lộ notification cross-user

**Business**
- Idempotent: replay không tạo notification trùng
- FR-10 có thể giữ email/PDF worker riêng; FR-15 in-app cùng milestone
- Calendar push scoped property/room (FR-05)

**Time / Process**
- Artifacts: `sdd/services/homestay/features/015-notification-engine/`

## 5. ASSUMPTIONS

1. Phân trang inbox ~20/page, mới nhất trước.
2. Mở detail → auto mark read.
3. Offline: persist DB; online sau vẫn thấy qua REST.
4. Admin manual Retry cho FAILED (v1).
5. relatedEntity null (SYSTEM) → không action link.

## 6. OPEN QUESTIONS

1. Danh mục `type` notification chuẩn đầy đủ cho UAT?
2. Retry max / backoff / dead-letter policy chính thức?
3. WebSocket channel model: per-user vs property room?
4. Retention / xóa notification cũ (TTL)?
5. Email in-app song song hay chỉ in-app cho FR-15 v1?
