# CONTEXT.md — FR-09 Customer Management
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/009-customer-management/spec.md`

## 1. PROBLEM STATEMENT

Admin không quản trị được tài khoản Customer → khó hỗ trợ, khó đình chỉ lạm dụng, khó xem lịch sử book.

Pain cụ thể:
- Không có directory chỉ role Customer (lẫn Admin/Manager/Employee).
- Không tìm/lọc nhanh theo tên, email, Active/Suspended trên danh sách lớn.
- Không Suspend/Activate có kiểm soát — hoặc Suspend rồi khách vẫn login (lệch FR-01).
- Cần xem hồ sơ + booking history để hỗ trợ nhưng không được sửa hồ sơ giúp khách (FR-02 Self).
- Manager Customer List trên figma dễ nhầm vào FR-09 trong khi Spec v2 là Admin-only.

Pain: quản trị nền tảng Customer, không phải đăng ký OTP hay khiếu nại (FR-17).

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa |
|-----------|--------|
| Customer Directory | SCR-51 — list role CUSTOMER only |
| ACTIVE / SUSPENDED / INACTIVE | INACTIVE = pre-verify FR-01; Admin toggle Active↔Suspended |
| Total Bookings / Total Spend | Aggregate trên list (Spend = SUM Payment PAID) |
| Drawer detail | Profile read-only + booking history |
| Audit | Ghi ai đổi status, cũ→mới |
| Outstanding Debt | Indicator read-only nếu có từ damage flow — không set ở FR-09 |

Phụ thuộc FR-01 block login khi SUSPENDED; booking read từ FR-04.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Admin | Directory, suspend/activate, xem history |
| Customer | Bị ảnh hưởng khi Suspend (không login) |
| Manager | **Out of scope** FR-09 (Spec v2) |
| Spec Architect | Chốt Admin-only vs figma Manager list |
| Agent Pilot | SCR-51 + APIs |

## 4. CONSTRAINTS

**Tech**
- Java 17 Spring Security + SQL Server + React 18 — `CONSTITUTION.md` / `AGENTS.md`
- Pagination bắt buộc; index theo email/status (LESSON-008)
- Không tạo/xóa/sửa profile Customer qua FR-09

**Business**
- Admin-only; từ chối role khác
- Suspend không tự hủy booking active (assumption spec)
- Chỉ thao tác user role CUSTOMER

**Time**
- Align FR-01 trước khi UAT Suspend matrix (email + Google)

## 5. ASSUMPTIONS

1. INACTIVE (chưa verify): hiện trong default list nhưng disable Suspend/Activate (409 nếu BE nhận write).
2. Total Spend = `SUM(Payment.amount)` where `status=PAID` (all types) — Clarifications 2026-07-23; reconciliation chi tiết thuộc FR-12.
3. Suspend: revoke **tất cả** refresh tokens ngay; access JWT hết hạn theo TTL; login/refresh mới bị chặn (FR-01).
4. SCR-51 ≠ Contract Detail legacy cùng số màn hình.
5. Complaint / moderation / settings = FR-17, không FR-09.

## 6. OPEN QUESTIONS

1. Manager Customer List (SCR-55/56) — **đã chốt**: Out of FR-09 vĩnh viễn; feature riêng nếu cần — Clarifications Session 2026-07-23.
2. Suspend revoke refresh tokens — **đã chốt**: Revoke ngay — Clarifications Session 2026-07-23.
3. Total Spend — **đã chốt**: SUM Payment PAID (all types) — Clarifications Session 2026-07-23.
4. INACTIVE default list — **đã chốt**: Hiện tất cả statuses — Clarifications Session 2026-07-23.
5. Audit store — **đã chốt**: ActivityLog `USER_SUSPENDED` / `USER_ACTIVATED` — Clarifications Session 2026-07-23.

*(Không còn open question blocking FR-09.)*
