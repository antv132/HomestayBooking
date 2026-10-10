# CONTEXT.md — FR-19 Customer Dashboard
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/019-customer-dashboard/spec.md`

## 1. PROBLEM STATEMENT

Customer đăng nhập phải mở nhiều màn hình mới biết booking active, thanh toán chờ, ticket bảo trì, thông báo chưa đọc và lịch check-in/out sắp tới.

Pain cụ thể:
- Không có một SCR-15 tổng quan KPI tin cậy, đúng dữ liệu của chính mình.
- Thiếu upcoming check-in/check-out + countdown; gần ngày check-in dễ bỏ sót.
- Booking sắp tới / Pending deposit không có CTA thanh toán nhanh.
- Thông báo và thanh toán gần đây nằm rải; deep link dễ sai.
- Damage Dispute (FR-23) cần cảnh báo trên dashboard nhưng full flow không thuộc FR-19.

Không mô tả giải pháp — chỉ pain: thiếu composite read-only dashboard + navigation, không CRUD domain trên dashboard.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| Customer Dashboard (SCR-15) | Composite KPI + sections + quick links |
| Active bookings KPI | Confirmed + Checked-in |
| Pending payments / Open tickets / Unread | Đếm từ FR-12/13/15 |
| Upcoming events | Check-in/out gần nhất + countdown |
| Upcoming stays list | Confirmed / Checked-in / Pending deposit (max ~5) |
| Damage dispute alert | P2 banner → FR-23; không own dispute logic |

Ranh giới: Deep links tới SCR-17/18, payments, maintenance, notifications; không CRUD trên dashboard.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Customer | Người dùng chính của SCR-15 |
| Manager / Guest | Không truy cập Customer Dashboard |
| Upstream FR-04/12/13/15/02/23 | Nguồn dữ liệu / alert |
| Spec Architect / Agent Pilot | Spec + implement `019-customer-dashboard` |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Auth CUSTOMER only; không lộ dữ liệu cross-customer

**Business**
- Read-only aggregate; mọi hành động nghiệp vụ qua deep link feature gốc
- Dispute banner phụ thuộc FR-23 sẵn sàng

**Time / Process**
- Artifacts: `sdd/services/homestay/features/019-customer-dashboard/`

## 5. ASSUMPTIONS

1. Pending payments KPI nhấn mạnh khi > 0.
2. Check-in ≤ 3 ngày có badge warning.
3. Recent notifications ~5; recent payments ~3.
4. Empty KPI = 0 hoặc "—" không crash.
5. Open tickets đếm Open + In Progress (và tương đương đang mở).

## 6. OPEN QUESTIONS

1. Exact set status cho “Open tickets” KPI?
2. Timezone countdown: property vs Asia/Ho_Chi_Minh?
3. Max items mỗi section chính thức?
4. Single API aggregate vs nhiều query FE?
5. Dispute banner ship cùng sprint FR-23 hay stub?
