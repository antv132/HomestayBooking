# CONTEXT.md — FR-16 Reporting
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/016-reporting/spec.md`

## 1. PROBLEM STATEMENT

Manager/Admin không nắm nhanh sức khỏe vận hành và doanh thu theo Property/toàn hệ thống; phải hỏi nhiều nguồn hoặc xem sai scope.

Pain cụ thể:
- Manager cần KPI (phòng, occupancy, doanh thu, check-in hôm nay, chờ duyệt) theo Property được gán — không được xem Property khác.
- Thiếu báo cáo chi tiết Revenue / Occupancy / Booking Trend theo khoảng ngày, tháng/năm.
- Admin cần KPI global + báo cáo so sánh Property; Manager không được vào Admin reports.
- Employee/Customer không được xem báo cáo nhưng vẫn có thể cố truy cập.
- Cần xuất CSV chia sẻ nội bộ; PDF/Excel nâng cao chưa phải pain chặn MVP.

Không mô tả giải pháp — chỉ pain: thiếu lớp báo cáo read-only tin cậy, đúng RBAC, dựa payment PAID / booking lifecycle.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| KPI Dashboard | SCR-27 Manager / SCR-45 Admin — thẻ tổng quan |
| Revenue | Doanh thu từ Payment PAID hợp lệ (deposit/balance) |
| Occupancy Rate | % room-nights occupied vs available theo kỳ |
| Booking Trend | Đếm booking counted (Confirmed/Checked-in/Checked-out; loại Cancelled/No-show — assumption) |
| Property scope | Manager chỉ Property được gán (FR-06); Admin toàn hệ thống |
| Export CSV | P2 — khớp bảng byPeriod đang hiển thị |

Ranh giới: FR-16 read-only aggregation; không CRUD Booking/Payment; không thay kế toán.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Manager | KPI + reports Property-scoped |
| Admin | KPI + reports global / filter Property |
| Employee / Customer | Không có quyền báo cáo v1 |
| Spec Architect / Agent Pilot | Spec + implement `016-reporting` |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Aggregation timezone Asia/Ho_Chi_Minh; tránh chia occupancy cho 0 phòng

**Business**
- Chỉ Manager/Admin; property RBAC nghiêm ngặt
- Revenue chỉ payment PAID hợp lệ; không invent số kế toán
- Property INACTIVE vẫn tính lịch sử báo cáo

**Time / Process**
- Artifacts: `sdd/services/homestay/features/016-reporting/`

## 5. ASSUMPTIONS

1. Pending approvals KPI = payment + maintenance chờ duyệt.
2. Max date range ~366 ngày hoặc cảnh báo hiệu năng.
3. Manager thấy full historical data của Property (không cắt theo ngày gán).
4. SCR-55 có cùng tabs Occupancy/Trends như SCR-44.
5. CSV client-side đủ P2; PDF/Excel out of scope v1.

## 6. OPEN QUESTIONS

1. Định nghĩa chính thức “booking counted” cho trend?
2. Refunded/cancelled payments loại khỏi revenue thế nào?
3. Occupancy: exclusive check-out day hay inclusive?
4. Nguồn “khách mới” KPI Admin (FR-09) có bắt buộc v1?
5. Cache/refresh interval báo cáo realtime hay on-demand?
