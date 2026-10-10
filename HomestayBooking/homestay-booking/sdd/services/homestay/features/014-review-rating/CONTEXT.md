# CONTEXT.md — FR-14 Review & Rating
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/014-review-rating/spec.md`

## 1. PROBLEM STATEMENT

Sau lưu trú, khách không có cách tin cậy để đánh giá phòng; người xem phòng thiếu tín hiệu chất lượng; nội dung xấu khó kiểm duyệt theo Property.

Pain cụ thể:
- Booking chưa Checked-out hoặc đã có review vẫn bị cố gửi → đánh giá giả / trùng.
- Customer muốn sửa/xóa đánh giá của mình nhưng sợ mất quyền sở hữu nội dung.
- Guest/Customer xem Room Detail thấy cả review đã ẩn hoặc aggregate sai.
- Manager không ẩn được review xấu trong Property; Admin cần bao quát toàn hệ thống khi leo thang.
- Sau check-out khó tìm form “Write Review” từ Booking Detail.

Không mô tả giải pháp — chỉ pain: thiếu review xác thực gắn booking + moderation + aggregate công khai tin cậy.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| Review | Đánh giá 1–5 sao + comment gắn Booking/Room/Customer |
| Published / Hidden | Công khai vs bị moderation ẩn |
| One review/booking | Tối đa 1 review tồn tại mỗi booking |
| Aggregate | Average + count chỉ từ Published (1 chữ số thập phân) |
| Content Moderation | Manager property-scoped; Admin global (SCR-65 / SCR-56) |
| Write Review CTA | Entry từ Booking Detail khi Checked-out chưa review |

Quy tắc: FR-14 owns Review CRUD/moderation/aggregates; FR-03 consumes Published cho discovery; Complaint thuộc FR-17.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Customer | Tạo/sửa/xóa review của mình sau check-out |
| Guest / Customer | Đọc Published + aggregate trên Room Detail |
| Manager | Hide/Show reviews thuộc Property |
| Admin | Moderate toàn hệ thống |
| Spec Architect / Agent Pilot | Spec + implement `014-review-rating` |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Property-scoped RBAC cho Manager moderation

**Business**
- Chỉ Checked-out mới review; 1 review/booking khi còn tồn tại
- Hidden không vào public list/aggregate; Customer vẫn thấy Hidden của mình
- Manager không vượt Property; Admin không bị property limit

**Time / Process**
- Artifacts: `sdd/services/homestay/features/014-review-rating/`

## 5. ASSUMPTIONS

1. Xóa review → booking được review lại (giải phóng slot).
2. Public list sort mới nhất trước; phân trang.
3. Audit `moderatedBy` / `moderatedAt` khi Hide/Show.
4. Comment có độ dài tối thiểu (validation thân thiện).
5. Review Hidden vẫn cho Customer edit/delete.

## 6. OPEN QUESTIONS

1. Độ dài min/max comment chính thức cho UAT?
2. Sau edit Published — có cần re-moderation không?
3. Tên reviewer hiển thị: full name hay mask?
4. Admin moderation có ghi ActivityLog bắt buộc (FR-17) không?
5. Soft-delete review hay hard-delete khi Customer xóa?
