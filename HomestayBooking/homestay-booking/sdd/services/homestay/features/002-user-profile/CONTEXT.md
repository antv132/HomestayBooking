# CONTEXT.md — FR-02 User Profile
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/002-user-profile/spec.md`

## 1. PROBLEM STATEMENT

User đã đăng nhập nhưng không tin tưởng / không cập nhật được thông tin liên hệ của chính mình.

Pain cụ thể:
- Khách / nhân viên không biết hồ sơ hiện tại (tên, SĐT, avatar, role) đang đúng hay sai.
- Muốn sửa họ tên / SĐT / ảnh đại diện nhưng không có luồng Self rõ ràng — hoặc sợ sửa nhầm email/role/status.
- Sau khi lưu, header/menu vẫn hiện tên cũ → phải đăng nhập lại mới “thấy” thay đổi.
- User khác (hoặc Admin qua profile API) không được phép xem/sửa hồ sơ Self của người khác — thiếu Self-scope → rủi ro lộ dữ liệu.

Tránh solution thinking: pain là thiếu quyền Self tin cậy + thông tin liên hệ không đồng bộ UI.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| Self-scope | Chỉ đọc/cập nhật bản ghi user đang đăng nhập (`/users/me`) |
| fullName / phone / avatarUrl | Field được phép sửa qua FR-02 |
| email | Định danh đăng nhập — **read-only** trên profile |
| role / status | Không đổi qua profile; thuộc FR-01 / Admin flows |
| SCR-10 / SCR-11 | Màn xem / sửa hồ sơ |
| avatarUrl (MVP) | Chuỗi URL; upload file server = out of scope FR-02 |

Quy tắc: phụ thuộc FR-01 JWT; SUSPENDED bị từ chối ở auth layer; partial update giữ field không gửi.

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Customer / Employee / Manager / Admin | Được lợi: xem & sửa Self |
| Spec Architect | Chốt FR / OQ (đổi mật khẩu tách FR-01) |
| Quality Gatekeeper | Zero cross-user profile access |
| Agent Pilot | Implement theo `002-user-profile` |

## 4. CONSTRAINTS

**Tech**
- Java 17 + Spring Boot/Security + SQL Server + JPA; React 18 SPA — `CONSTITUTION.md` / `AGENTS.md`
- API: `GET/PUT /api/v1/users/me`; envelope `{ success, message, data }`
- Validation phone/fullName theo §7; không commit secrets

**Business**
- Self-scope only; cấm tự đổi email/role/status
- Đổi mật khẩu / Admin sửa Customer khác = out of scope FR-02

**Time / Process**
- Spec Kit artifacts dưới `sdd/services/homestay/features/002-user-profile/`
- Không implement trước Spec Sync nếu còn gap OQ

## 5. ASSUMPTIONS

1. MVP dùng `avatarUrl` string; chưa bắt buộc upload multipart.
2. Sau update thành công, FE invalidate cache / store để header đồng bộ ngay (không bắt re-login).
3. Mọi role authenticated dùng cùng API Self.
4. Email change (nếu có) là flow riêng có verify — không nằm FR-02.
5. Paths theo `docs/api-spec-by-screen.md` SCR-10/11.

## 6. OPEN QUESTIONS

1. Upload avatar file lên server có vào Sprint gần nhất không, hay giữ URL-only?
2. Phone format chính thức (VN 10 số / E.164) cho UAT?
3. Partial PUT vs PATCH — contract API cuối cùng?
4. Avatar URL lỗi: chỉ initials hay có toast lỗi?
5. Có audit log khi Self cập nhật profile trong v1 không?
