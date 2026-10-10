# Auth_Workflow

| Field | Value |
|---|---|
| **Workflow** | Auth_Workflow |
| **Test requirement** | Kiểm tra đăng ký, OTP (hạn 15 phút, rate limit), login/logout theo role, quên/đổi mật khẩu, Google OAuth (nếu bật), refresh token, RBAC và trạng thái ACTIVE/SUSPENDED/INACTIVE. |
| **Number of TCs** | 32 |

| Testing Round | Passed | Failed | Pending | N/A |
|---|---:|---:|---:|---:|
| Round 1 | 0 | 0 | 32 | 0 |
| Round 2 | 0 | 0 | 32 | 0 |
| Round 3 | 0 | 0 | 32 | 0 |

### Cách điền Excel (quan trọng)

1. Hàng **Scenario A/B/C…**: chỉ ghi tên scenario vào cột **Test Case ID** (hoặc merge cả hàng như template). Các cột còn lại **để trống**. Không đếm vào Number of TCs.
2. Hàng **AUTH_xxx**: điền đủ Description / Procedure / Expected / Pre-conditions; Round = `Pending`.
3. Thứ tự trong sheet phải đúng: Scenario → các TC của scenario đó → Scenario tiếp theo.

---

### Excel copy — paste theo đúng thứ tự hàng

| Test Case ID | Test Case Description | Test Case Procedure | Expected Results | Pre-conditions | Round 1 | Test date | Tester | Round 2 | Test date | Tester | Round 3 | Test date | Tester | Note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Scenario A — Register validation | | | | | | | | | | | | | | |
| AUTH_001 | Test đăng ký Customer thành công với email mới | 1. Mở Register (SCR-03). 2. Nhập full name, email chưa tồn tại, password hợp lệ. 3. Submit. | Tạo account chờ verify; gửi OTP; chuyển màn OTP. | FE+BE+DB chạy; SMTP OK; email mới | Pending | | | Pending | | | Pending | | | |
| AUTH_002 | Test đăng ký trùng email đã tồn tại | 1. Mở Register. 2. Nhập email đã có trong DB. 3. Submit. | Báo lỗi email đã tồn tại; không tạo user mới. | Email ACTIVE/pending đã có | Pending | | | Pending | | | Pending | | | |
| AUTH_003 | Test đăng ký với email sai định dạng | 1. Nhập email không hợp lệ (vd. abc@). 2. Submit. | Validation lỗi email; không gọi API hoặc API 400. | Form Register mở | Pending | | | Pending | | | Pending | | | |
| AUTH_004 | Test đăng ký password quá ngắn / yếu | 1. Nhập password không đủ rule (quá ngắn). 2. Submit. | Validation lỗi password; không tạo account. | Biết rule password hệ thống | Pending | | | Pending | | | Pending | | | |
| AUTH_005 | Test đăng ký thiếu trường bắt buộc | 1. Bỏ trống full name hoặc password. 2. Submit. | Báo lỗi required fields; không tạo account. | Form Register mở | Pending | | | Pending | | | Pending | | | |
| AUTH_006 | Test đăng ký password và confirm password không khớp | 1. Nhập password ≠ confirm. 2. Submit. | Báo lỗi không khớp; không tạo account. | Form có confirm password | Pending | | | Pending | | | Pending | | | N/A nếu UI không có confirm |
| Scenario B — OTP | | | | | | | | | | | | | | |
| AUTH_007 | Test xác thực OTP đúng → ACTIVE | 1. Đăng ký user mới. 2. Nhập OTP đúng từ email. 3. Confirm. | Account ACTIVE; login được. | AUTH_001; OTP còn hạn ≤15p | Pending | | | Pending | | | Pending | | | |
| AUTH_008 | Test OTP sai bị từ chối | 1. User pending verify. 2. Nhập OTP sai. 3. Confirm. | Báo lỗi; account chưa ACTIVE. | User pending OTP | Pending | | | Pending | | | Pending | | | |
| AUTH_009 | Test OTP hết hạn (>15 phút) | 1. Chờ OTP hết hạn hoặc dùng OTP cũ. 2. Submit. | Báo OTP hết hạn; cần resend. | OTP expired | Pending | | | Pending | | | Pending | | | |
| AUTH_010 | Test resend OTP thành công | 1. Trên màn OTP bấm Resend. 2. Kiểm tra email mới. | OTP mới gửi; OTP cũ không còn dùng được (nếu invalidate). | User pending; SMTP OK | Pending | | | Pending | | | Pending | | | |
| AUTH_011 | Test rate limit OTP (vượt 3 lần/giờ) | 1. Resend/verify OTP liên tục vượt limit. | Bị chặn rate limit; thông báo thử lại sau. | Biết limit 3/h | Pending | | | Pending | | | Pending | | | |
| Scenario C — Login / logout / status | | | | | | | | | | | | | | |
| AUTH_012 | Test login Customer đúng credential → Customer Dashboard | 1. Login email/password Customer ACTIVE. | Vào Customer Dashboard (SCR-15). | Customer ACTIVE | Pending | | | Pending | | | Pending | | | |
| AUTH_013 | Test login Manager → Manager Dashboard | 1. Login Manager ACTIVE. | Vào Manager Dashboard (SCR-27). | Manager ACTIVE + property | Pending | | | Pending | | | Pending | | | |
| AUTH_014 | Test login Employee → Employee Dashboard | 1. Login Employee ACTIVE. | Vào Employee Dashboard (SCR-59). | Employee ACTIVE | Pending | | | Pending | | | Pending | | | |
| AUTH_015 | Test login Admin → Admin Dashboard | 1. Login Admin ACTIVE. | Vào Admin area/dashboard. | Admin ACTIVE | Pending | | | Pending | | | Pending | | | Smoke only |
| AUTH_016 | Test login sai password | 1. Email đúng, password sai. 2. Submit. | Lỗi đăng nhập; không cấp token. | User ACTIVE | Pending | | | Pending | | | Pending | | | |
| AUTH_017 | Test login email không tồn tại | 1. Email chưa đăng ký, password bất kỳ. 2. Submit. | Lỗi đăng nhập (không lộ “email không tồn tại” nếu anti-enum). | Email không có trong DB | Pending | | | Pending | | | Pending | | | |
| AUTH_018 | Test login user SUSPENDED bị chặn | 1. Login user SUSPENDED đúng password. | Bị chặn; thông báo tạm ngưng. | User SUSPENDED | Pending | | | Pending | | | Pending | | | |
| AUTH_019 | Test login user INACTIVE / chưa verify bị chặn | 1. Login user chưa OTP verify. | Bị chặn hoặc yêu cầu verify OTP. | User pending verify | Pending | | | Pending | | | Pending | | | |
| AUTH_020 | Test logout xóa session / token | 1. Login. 2. Logout. 3. Mở lại URL private / gọi API có token cũ. | Về Login; token cũ không dùng được. | Đã login | Pending | | | Pending | | | Pending | | | |
| Scenario D — Forgot / reset / change password | | | | | | | | | | | | | | |
| AUTH_021 | Test Forgot Password gửi email thành công | 1. Mở SCR-05. 2. Nhập email ACTIVE. 3. Submit. | Thông báo đã gửi (anti-enumeration OK); email có link/token reset. | SMTP OK; user ACTIVE | Pending | | | Pending | | | Pending | | | |
| AUTH_022 | Test Forgot Password với email không tồn tại (anti-enum) | 1. Nhập email không có trong DB. 2. Submit. | UI không lộ email có/không; không crash. | Email lạ | Pending | | | Pending | | | Pending | | | |
| AUTH_023 | Test Reset Password thành công bằng token hợp lệ | 1. Mở SCR-06 từ link email. 2. Đặt password mới hợp lệ. 3. Login bằng password mới. | Đổi MK thành công; login OK bằng MK mới; MK cũ không login được. | Token reset còn hạn | Pending | | | Pending | | | Pending | | | |
| AUTH_024 | Test Reset Password với token hết hạn/invalid | 1. Mở link token hết hạn hoặc giả. 2. Submit MK mới. | Báo lỗi token; không đổi MK. | Token invalid/expired | Pending | | | Pending | | | Pending | | | |
| AUTH_025 | Test Change Password khi đã login | 1. Login. 2. Mở SCR-12. 3. Nhập MK cũ đúng + MK mới. 4. Save. | Đổi MK thành công; login lại bằng MK mới. | User ACTIVE đã login | Pending | | | Pending | | | Pending | | | |
| AUTH_026 | Test Change Password sai mật khẩu cũ | 1. SCR-12 nhập MK cũ sai. 2. Submit. | Báo lỗi; MK không đổi. | Đã login | Pending | | | Pending | | | Pending | | | |
| Scenario E — RBAC & session / OAuth | | | | | | | | | | | | | | |
| AUTH_027 | Test Customer mở URL Manager bị redirect/403 | 1. Login Customer. 2. Dán URL `/manager/...`. | Redirect dashboard Customer hoặc 403; không xem data Manager. | Biết URL Manager | Pending | | | Pending | | | Pending | | | |
| AUTH_028 | Test Manager mở URL Admin bị chặn | 1. Login Manager. 2. Dán URL Admin. | Bị chặn/redirect; không vào Admin. | Biết URL Admin | Pending | | | Pending | | | Pending | | | |
| AUTH_029 | Test Employee mở URL Manager bị chặn | 1. Login Employee. 2. Dán URL Manager booking. | 403/redirect; không thao tác Manager. | Biết URL Manager | Pending | | | Pending | | | Pending | | | |
| AUTH_030 | Test API không có Bearer token → 401 | 1. Gọi API private (Postman) không header Authorization. | HTTP 401 Unauthorized. | BE chạy; biết endpoint private | Pending | | | Pending | | | Pending | | | |
| AUTH_031 | Test refresh token gia hạn access token | 1. Login lấy access+refresh. 2. Đợi access gần hết hạn hoặc gọi refresh. 3. Dùng access mới gọi API. | Refresh thành công; API private OK với token mới. | Refresh TTL cấu hình (vd. 7d) | Pending | | | Pending | | | Pending | | | |
| AUTH_032 | Test Google OAuth login (nếu bật) hoặc N/A | 1. Click Login with Google. 2. Chọn account test. 3. Return app. | Tạo/login user Google thành công → đúng dashboard; hoặc N/A nếu môi trường chưa cấu hình OAuth. | Google OAuth credentials (hoặc N/A) | Pending | | | Pending | | | Pending | | | N/A nếu chưa config |
