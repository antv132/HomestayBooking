# CONTEXT.md — FR-17 Administration
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/017-administration/spec.md`

## 1. PROBLEM STATEMENT

Nền tảng thiếu hub quản trị: khách không gửi/theo dõi khiếu nại được; Admin thiếu cấu hình hệ thống, audit log, và kiểm duyệt nội dung toàn cục.

Pain cụ thể:
- Customer gặp vấn đề lưu trú không có Complaint Open → Investigating → Resolved → Closed.
- Admin không chỉnh được deposit %, tên hệ thống, support email, tài khoản ngân hàng thụ hưởng → lệch FR-04/12.
- Không có Activity Logs read-only để truy vết hành động cấu hình/moderation.
- Review vi phạm cross-property cần Admin Content Moderation (bổ sung FR-14 Manager scope).
- Frontend từng đưa complaint cho Manager — sai actor; Customer Directory Active/Suspend thuộc FR-09, không trùng FR-17.

Không mô tả giải pháp — chỉ pain: thiếu Complaint + System Settings + Activity Logs + Admin moderation hub.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| Complaint | Khiếu nại Customer: subject + description + status lifecycle |
| Open → Investigating → Resolved → Closed | Workflow Admin; Closed terminal |
| SystemSetting | Deposit %, system name, support email, bank info |
| ActivityLog | Audit read-only: user, action, entity, thời gian |
| Content Moderation tab | Admin Hide/Show reviews toàn hệ thống (SCR-56) |
| SCR-54 / SCR-56 | Complaint Mgmt / System Administration hub |

Ranh giới: Promotion = FR-18; Customer Suspend = FR-09; Outbox FAILED monitor = FR-15 (optional tab).

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Customer | Tạo + theo dõi complaint của mình |
| Admin | Resolve complaint, settings, logs, global moderation |
| Manager | Không dùng Admin complaint/settings APIs |
| Spec Architect / Agent Pilot | Spec + implement `017-administration` |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Admin-only cho SCR-54/56; secrets không hard-code trong settings UI payload nhạy cảm nếu có

**Business**
- Deposit % ảnh hưởng booking/payment downstream — validate ngưỡng
- Suspended customer không login (FR-01) → không tạo complaint
- Không duplicate Customer Directory (FR-09)

**Time / Process**
- Artifacts: `sdd/services/homestay/features/017-administration/`

## 5. ASSUMPTIONS

1. Deposit percentage hợp lệ ~10–50%.
2. Closed complaint read-only.
3. Đổi settings ghi ActivityLog.
4. Outbox monitor tab P2 nếu FR-15 sẵn sàng.
5. Manager complaint routes hiện tại phải chuẩn hóa về Admin.

## 6. OPEN QUESTIONS

1. Complaint có bắt buộc gắn bookingId không?
2. Ai nhận notify khi complaint đổi status (FR-15)?
3. Danh sách SystemSetting keys canonical đầy đủ?
4. Retention Activity Logs bao lâu?
5. Admin resolve dispute damage (FR-23) có nằm SCR-56 không?
