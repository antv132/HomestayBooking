# CONTEXT.md — FR-03 Room Discovery
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/003-room-discovery/spec.md`

## 1. PROBLEM STATEMENT

Guest/Customer không tìm được phòng phù hợp nhanh → bỏ cuộc trước khi đặt.

Pain cụ thể:
- Không lọc được theo địa điểm, loại phòng, giá, ngày, số khách — kết quả nhiễu hoặc trống không giải thích.
- Chi tiết phòng thiếu gallery / tiện ích / giá / capacity → không đủ tin để Book.
- Gõ search không có gợi ý → phải đoán tên property/location.
- Không biết ngày nào còn trống (lịch read-only) trước khi vào booking.
- Trang chủ thiếu featured / banner / stats → Guest không cảm nhận được quy mô nền tảng.
- Phòng property INACTIVE / Maintenance vẫn lộ → đặt nhầm hoặc 404 khó hiểu.

Pain: khám phá & quyết định lưu trú, chưa phải thanh toán (FR-04).

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa |
|-----------|--------|
| Discovery | Luồng public xem/tìm phòng (Guest không cần login) |
| ACTIVE property | Chỉ property ACTIVE mới hiện trong discovery |
| Bookable room | Không Maintenance/OOS; đủ capacity; available trong khoảng ngày |
| Search suggestions | Gợi ý property/location khi gõ ≥2 ký tự |
| SCR-01 / 07 / 08 / 09 | Landing, list, detail, calendar read-only (booked vs available) |
| Pricing display | Giá theo khoảng ngày nếu có rule; không thì giá mặc định phòng |

Ranh giới: lịch 8 trạng thái + khóa thủ công = FR-05; đặt phòng = FR-04.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Guest / Customer | Được lợi: tìm & xem phòng |
| Manager / Admin | Cung cấp dữ liệu Property/Room (FR-06/08) |
| Spec Architect | Chốt filter/calendar boundary với FR-05 |
| Agent Pilot | Implement list/detail/suggest/landing |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Public read APIs; pagination/sort; không N+1 gallery nặng

**Business**
- Guest xem được; Book Now Guest → login (FR-01)
- INACTIVE property / non-bookable rooms loại khỏi kết quả
- Calendar SCR-09 read-only đơn giản (FR-03); full 8-status = FR-05

**Time**
- Phụ thuộc seed/data Property-Room; không block trên auth để xem

## 5. ASSUMPTIONS

1. Dữ liệu Property/Room đã có (FR-06/08 hoặc seed).
2. Khi có check-in/out: loại phòng overlap hold/booking theo FR-04 statuses.
3. Empty state + gợi ý chỉnh filter là đủ UX khi zero results.
4. Featured/stats/promotions lấy từ endpoint/CMS đã định trong screen docs.
5. Customer và Guest cùng quyền xem; khác ở Book Now.

## 6. OPEN QUESTIONS

1. Sort mặc định list: giá tăng hay “mới nhất”?
2. Suggestions: chỉ property/location hay thêm room type?
3. Giá dynamic (PricingRule) bắt buộc Sprint nào?
4. Calendar FR-03 vs FR-05: FE dùng chung component hay tách API?
5. Stats landing: reviews/rating có bắt buộc v1 không?
