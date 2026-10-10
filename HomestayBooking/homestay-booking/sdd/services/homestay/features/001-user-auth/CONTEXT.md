# CONTEXT.md — FR-01 User Authentication
# Người viết: @team | Ngày: 2026-07-23
# Spec: `sdd/services/homestay/features/001-user-auth/spec.md`

## 1. PROBLEM STATEMENT

Guest không thể trở thành Customer tin cậy nếu không có cách đăng ký/đăng nhập an toàn.

Pain cụ thể:
- Khách muốn đặt phòng nhưng chưa có tài khoản — phải tạo tài khoản và chứng minh sở hữu email trước khi book.
- User quên mật khẩu hoặc muốn đăng nhập nhanh (Google) nhưng sợ bị chiếm tài khoản nếu chỉ trùng email.
- Tài khoản bị đình chỉ (SUSPENDED) vẫn có thể cố đăng nhập — cần bị chặn rõ ràng.
- Phiên đăng nhập hết hạn giữa chừng khiến thao tác đặt phòng/thanh toán bị gián đoạn nếu không refresh được an toàn.
- Đăng xuất trên một thiết bị mà token vẫn dùng được ở nơi khác → rủi ro bảo mật.

Không mô tả giải pháp kỹ thuật ở đây — chỉ pain: không tin cậy được danh tính → không mở được quyền Customer / role đúng.

## 2. DOMAIN KNOWLEDGE

| Thuật ngữ | Nghĩa trong hệ thống này |
|-----------|-------------------------|
| Guest | Chưa đăng nhập; chỉ xem phòng / bắt đầu đăng ký-đăng nhập |
| Customer | Role sau khi đăng ký + xác thực email (hoặc Google link hợp lệ) |
| ACTIVE | Tài khoản được phép đăng nhập |
| SUSPENDED | Tài khoản bị khóa — mọi login (local/Google) bị từ chối |
| OTP | Mã một lần gửi email để verify đăng ký hoặc link Google an toàn |
| Access token / Refresh token | JWT phiên ngắn + token dài để làm mới; logout phải thu hồi refresh |
| Google link | Liên kết tài khoản Google với user local cùng email đã verified — **không** auto-link nếu email local chưa verified (chống takeover) |
| Local-only Google user | Có thể đăng nhập Google nhưng không có password local → không cho login bằng password |

Quy tắc bất thành văn / từ AGENTS + Spec:
- Mật khẩu: BCrypt cost ≥ 12; không lộ “sai email hay sai password” riêng biệt.
- Không cấp full access trước khi email verified (trừ flow được spec cho phép).
- Role quyết định routing sau login (Customer / Employee / Manager / Admin).

## 3. STAKEHOLDERS

| Ai | Vai trò |
|----|---------|
| Guest / Customer | Được lợi: vào hệ thống an toàn để book |
| Employee / Manager / Admin | Cần login đúng role; bị ảnh hưởng nếu auth lỗi |
| Spec Architect | Quyết định FR / clarify OQ |
| Quality Gatekeeper | Duyệt bảo mật (token, Google link, SUSPENDED) |
| Agent Pilot | Implement theo `001-user-auth` |

## 4. CONSTRAINTS (ràng buộc không thể thay đổi)

**Tech**
- Stack: Java 17 + Spring Security + JWT; React 18 SPA — theo `CONSTITUTION.md` / `AGENTS.md`
- SQL Server + JPA; secrets chỉ qua env/config — không commit
- CORS whitelist; không `*` production

**Business**
- Phải hỗ trợ email/password + Google OAuth (FR-01 / Specification_v2)
- Google không được auto-link khi email local chưa verified
- SUSPENDED không login

**Time / Process**
- Không code feature auth mới trước Spec Sync approve (nếu còn gap)
- Spec Kit artifacts nằm dưới `sdd/services/homestay/features/001-user-auth/`

## 5. ASSUMPTIONS (giả định — cần confirm)

1. OTP có TTL cố định đủ dùng cho demo (chi tiết rate limit có thể ở Assumptions của spec — OQ-06).
2. Sau verify OTP đăng ký, hành vi auto-login hay bắt login lại theo default trong Assumptions spec (OQ-05).
3. Một email = một user local; không multi-account cùng email.
4. Refresh token store phía server (hoặc cơ chế thu hồi tương đương) đủ để logout invalidate.
5. Google client IDs đã có trong môi trường DEV của team.

## 6. OPEN QUESTIONS (câu hỏi chưa có câu trả lời)

1. OTP TTL và số lần resend tối đa chính thức cho UAT là bao nhiêu?
2. Sau verify đăng ký: auto-login hay chuyển màn hình login? (OQ-05 trong checklist)
3. Rate limit login/OTP chi tiết (per IP / per email) cho production?
4. Tài khoản Google-only có bắt buộc set password sau không, hay luôn Google-only?
5. Multi-device: logout một thiết bị có revoke toàn bộ refresh tokens không?

---

**Next:** Khi OQ đóng → cập nhật `spec.md` → Spec Sync approve → implement / regression theo `tasks.md`.
