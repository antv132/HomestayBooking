# CONTEXT.md — FR-18 Promotion Management
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/018-promotion-management/spec.md`

## 1. PROBLEM STATEMENT

Trang chủ thiếu banner khuyến mãi đang Active; Admin không quản được nội dung marketing (CRUD, bật/tắt, thứ tự); quyền quản lý từng gắn nhầm Manager.

Pain cụ thể:
- Guest vào Landing không thấy ưu đãi / CTA điều hướng; banner Inactive vẫn lộ.
- Admin cần tạo/sửa banner (title, subtitle, CTA, màu) nhưng không có SCR-57/58 rõ.
- Không bật/tắt Active-Inactive hoặc đổi sortOrder → thứ tự trang chủ loạn.
- Kỳ vọng mã giảm giá/discount vào checkout gây nhầm — v1 không phải pain giải bằng coupon.
- Manager routes `/manager/promotions` sai actor.

Không mô tả giải pháp — chỉ pain: thiếu banner marketing Admin-owned + public Active read, không phải hệ thống discount booking.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| Promotion | Banner marketing: title, subtitle, description?, ctaText, ctaUrl, colorTheme |
| Active / Inactive | Chỉ Active hiện trang chủ; Inactive vẫn trong Admin list |
| sortOrder | Thứ tự tăng dần; tie-break createdAt mới hơn trước |
| colorTheme | Gradient/theme UI — v1 không upload ảnh banner |
| Public read | Guest không cần auth để xem Active |
| Out of scope v1 | discountPercent, coupon code, A/B, schedule, image upload |

Ranh giới: FR-17 không gồm Promotion; FR-03 owns layout Landing; FR-18 owns data Promotion.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Guest / mọi role | Xem banner Active trên trang chủ |
| Admin | CRUD, Active/Inactive, sortOrder (SCR-57/58) |
| Manager / Customer | Không quản lý promotion |
| Spec Architect / Agent Pilot | Spec + implement `018-promotion-management` |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Public GET Active; Admin CRUD authenticated

**Business**
- Admin-only quản trị; không áp dụng giảm giá booking v1
- ctaUrl: relative path hoặc http(s); route chết → 404 khi click (chấp nhận v1)
- Không marketing automation (§9)

**Time / Process**
- Artifacts: `sdd/services/homestay/features/018-promotion-management/`

## 5. ASSUMPTIONS

1. Default tạo mới = Active (hoặc theo checkbox Admin).
2. Form có preview trước Save.
3. SCR-58 là page hoặc modal đều chấp nhận v1.
4. colorTheme ngoài palette → fallback default.
5. Xóa = hard delete vĩnh viễn.

## 6. OPEN QUESTIONS

1. Palette `colorTheme` chính thức gồm những giá trị nào?
2. Có giới hạn số banner Active đồng thời không?
3. Image banner có lên P2 sau demo không?
4. Localization đa ngôn ngữ banner?
5. Audit log khi Admin đổi promotion (FR-17 ActivityLog)?
