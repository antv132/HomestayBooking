# Feature Specification: FR-01 Authentication

**Feature Branch**: `001-user-auth`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "FR-01 Authentication — dựa vào docs (Specification_v2.md §3 FR-01, §2 Actors, §4 Security, §5 User, §7 Auth Errors, §8 User Management; api-spec-by-screen SCR-02–SCR-06, SCR-12)"

**Nguồn tham chiếu chi tiết**: `CONTEXT.md` (cùng folder); `docs/Specification_v2.md`

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Đăng ký và xác thực email (Priority: P1)

Là **Guest**, tôi muốn đăng ký tài khoản bằng email, họ tên, số điện thoại và mật khẩu, sau đó xác thực email qua mã OTP gửi tới hộp thư, để trở thành **Customer** và sử dụng các chức năng đặt phòng.

**Why this priority**: Không có tài khoản xác thực thì không có Customer; đây là cửa ngõ onboarding chính.

**Independent Test**: Hoàn tất đăng ký + nhập OTP hợp lệ → tài khoản ACTIVE, có thể đăng nhập ở story P2.

**Acceptance Scenarios**:

1. **Given** Guest chưa có tài khoản, **When** gửi thông tin đăng ký hợp lệ, **Then** hệ thống tạo tài khoản chờ xác thực và gửi OTP qua email (không cấp quyền truy cập đầy đủ).
2. **Given** OTP còn hiệu lực, **When** Guest nhập đúng OTP, **Then** email được xác thực, tài khoản chuyển ACTIVE với vai trò Customer.
3. **Given** OTP sai hoặc hết hạn, **When** Guest xác thực, **Then** hệ thống từ chối với thông báo OTP không hợp lệ.
4. **Given** email đã được sử dụng, **When** Guest đăng ký trùng email, **Then** hệ thống từ chối đăng ký.

---

### User Story 2 - Đăng nhập email/mật khẩu (Priority: P1)

Là **Guest** hoặc người dùng đã có tài khoản, tôi muốn đăng nhập bằng email và mật khẩu để truy cập hệ thống theo đúng vai trò (Customer, Employee, Manager, Admin).

**Why this priority**: Luồng đăng nhập cốt lõi cho mọi actor đã có tài khoản local.

**Independent Test**: Đăng nhập với credential đúng → nhận phiên làm việc và định tuyến theo role.

**Acceptance Scenarios**:

1. **Given** tài khoản ACTIVE và email đã xác thực, **When** nhập đúng email/mật khẩu, **Then** đăng nhập thành công và phân quyền đúng role.
2. **Given** email chưa xác thực, **When** đăng nhập, **Then** từ chối với thông báo tài khoản chưa xác thực.
3. **Given** tài khoản SUSPENDED, **When** đăng nhập, **Then** từ chối với thông báo tài khoản bị đình chỉ.
4. **Given** tài khoản chỉ dùng Google (không có mật khẩu local), **When** đăng nhập bằng mật khẩu, **Then** từ chối và hướng dẫn dùng Google.
5. **Given** sai email hoặc mật khẩu, **When** đăng nhập, **Then** thông báo lỗi chung (không tiết lộ field nào sai).

---

### User Story 3 - Đăng nhập Google (Priority: P1)

Là **Guest** hoặc người dùng, tôi muốn đăng nhập nhanh bằng tài khoản Google để không cần tạo mật khẩu riêng.

**Why this priority**: FR-01 yêu cầu Google login; giảm ma sát onboarding.

**Independent Test**: Google login với email đã có local verified → vào hệ thống; email chưa verified → bắt buộc bước xác minh thêm.

**Acceptance Scenarios**:

1. **Given** local account cùng email đã xác thực trước đó, **When** đăng nhập Google, **Then** hệ thống tự động liên kết và đăng nhập thành công.
2. **Given** email Google trùng local nhưng chưa xác thực HOẶC chưa có local verified, **When** đăng nhập Google, **Then** **không** auto-link; yêu cầu xác minh thêm qua OTP email trước khi liên kết (chống account takeover).
3. **Given** xác minh OTP Google link thành công, **When** hoàn tất bước xác minh, **Then** liên kết Google và đăng nhập thành công.
4. **Given** tài khoản SUSPENDED, **When** đăng nhập Google, **Then** từ chối.

---

### User Story 4 - Quản lý phiên: refresh, logout (Priority: P1)

Là người dùng đã đăng nhập, tôi muốn duy trì phiên an toàn, làm mới khi hết hạn và đăng xuất để kết thúc phiên trên thiết bị.

**Why this priority**: FR-01 yêu cầu Access/Refresh token và thu hồi khi logout.

**Independent Test**: Login → refresh phiên → logout → refresh token cũ không dùng được.

**Acceptance Scenarios**:

1. **Given** phiên còn hiệu lực, **When** làm mới phiên, **Then** tiếp tục truy cập mà không cần đăng nhập lại.
2. **Given** refresh token đã hết hạn hoặc bị thu hồi, **When** làm mới phiên, **Then** yêu cầu đăng nhập lại.
3. **Given** đang đăng nhập, **When** logout, **Then** refresh token bị thu hồi và phiên kết thúc an toàn.

---

### User Story 5 - Quên và đặt lại mật khẩu (Priority: P2)

Là **Guest** quên mật khẩu, tôi muốn nhận hướng dẫn qua email để đặt mật khẩu mới.

**Why this priority**: Khôi phục truy cập; phụ thuộc P1 (có tài khoản local).

**Independent Test**: Forgot password → email link/token → reset → login bằng mật khẩu mới.

**Acceptance Scenarios**:

1. **Given** email thuộc tài khoản local, **When** yêu cầu quên mật khẩu, **Then** gửi hướng dẫn đặt lại (phản hồi không tiết lộ email có tồn tại hay không).
2. **Given** token/link reset hợp lệ, **When** đặt mật khẩu mới đạt chính sách, **Then** mật khẩu cập nhật và mọi phiên cũ bị kết thúc.
3. **Given** tài khoản Google-only, **When** yêu cầu reset bằng mật khẩu, **Then** không cho phép đặt mật khẩu local qua luồng này.

---

### User Story 6 - Đổi mật khẩu khi đã đăng nhập (Priority: P2)

Là Customer, Employee, Manager hoặc Admin đã đăng nhập, tôi muốn đổi mật khẩu để bảo vệ tài khoản.

**Why this priority**: FR-01 explicit; bảo mật định kỳ.

**Independent Test**: Change password với mật khẩu hiện tại đúng → phải đăng nhập lại; phiên cũ không còn hiệu lực.

**Acceptance Scenarios**:

1. **Given** đang đăng nhập và có mật khẩu local, **When** đổi mật khẩu với current password đúng, **Then** mật khẩu mới có hiệu lực và **tất cả** phiên (kể cả hiện tại) bị kết thúc — user phải đăng nhập lại.
2. **Given** current password sai, **When** đổi mật khẩu, **Then** từ chối.
3. **Given** tài khoản Google-only, **When** đổi mật khẩu, **Then** từ chối.

---

### Edge Cases

- Guest gửi lại OTP quá 3 lần/giờ cho cùng email → từ chối gửi thêm trong cửa sổ thời gian.
- Đăng nhập đồng thời nhiều thiết bị → logout một thiết bị chỉ thu hồi token thiết bị đó (trừ reset/change password thu hồi toàn bộ).
- Admin đình chỉ (SUSPENDED) user đang online → refresh/login tiếp theo bị từ chối.
- Employee/Manager/Admin do Admin tạo ở trạng thái INACTIVE → không đăng nhập cho đến khi kích hoạt.
- Mật khẩu không đạt chính sách (dưới 8 ký tự hoặc thiếu chữ/số) → validation error rõ ràng.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST cho phép Guest đăng ký tài khoản bằng email, họ tên, số điện thoại và mật khẩu.
- **FR-002**: Hệ thống MUST gửi mã OTP qua email để xác thực đăng ký; OTP hết hạn sau 15 phút; tối đa 3 lần gửi lại/giờ/email.
- **FR-003**: Hệ thống MUST kích hoạt tài khoản Customer (ACTIVE, email verified) sau khi OTP đăng ký hợp lệ.
- **FR-004**: Hệ thống MUST cho phép đăng nhập bằng email và mật khẩu cho tài khoản local đã xác thực.
- **FR-005**: Hệ thống MUST hỗ trợ đăng nhập nhanh qua Google.
- **FR-006**: Hệ thống MUST chỉ tự động liên kết Google khi email đã được xác thực trước đó ở local account; nếu không MUST yêu cầu xác minh thêm bằng OTP email (ngăn account takeover) — rule nguyên văn Specification_v2 FR-01.
- **FR-007**: Hệ thống MUST quản lý phiên bằng access token (ngắn hạn) và refresh token (dài hạn), có luân chuyển refresh và thu hồi khi logout.
- **FR-008**: Hệ thống MUST cho phép quên mật khẩu và đặt lại qua email.
- **FR-009**: Hệ thống MUST cho phép đổi mật khẩu khi đang đăng nhập; sau đổi MUST thu hồi mọi phiên và yêu cầu đăng nhập lại.
- **FR-010**: Hệ thống MUST chặn đăng nhập/refresh đối với tài khoản SUSPENDED.
- **FR-011**: Hệ thống MUST từ chối đăng nhập/reset/change mật khẩu cho tài khoản chỉ dùng Google.
- **FR-012**: Hệ thống MUST phân quyền theo role sau auth: Guest, Customer, Employee, Manager, Admin — auth không cấp quyền vượt role.
- **FR-013**: Mật khẩu MUST tối thiểu 8 ký tự, có ít nhất 1 chữ cái và 1 chữ số; lưu trữ an toàn (hash chuẩn công nghiệp).
- **FR-014**: Hệ thống MUST truyền thông lỗi thân thiện theo nhóm: Invalid credentials, Account not verified, Account suspended, Google provider conflict, OTP invalid/expired, Session expired.

### Key Entities

- **User**: Người dùng hệ thống — email, họ tên, phone, role (ADMIN/MANAGER/EMPLOYEE/CUSTOMER), status (INACTIVE/ACTIVE/SUSPENDED), trạng thái xác thực email, phương thức auth (local/Google/linked).
- **Session (Refresh Token)**: Phiên làm việc có thể thu hồi, gắn user, có thời hạn và trạng thái revoke.
- **OTP Verification**: Mã xác thực email — mục đích đăng ký hoặc liên kết Google, có thời hạn và trạng thái đã dùng.
- **Password Reset Request**: Yêu cầu đặt lại mật khẩu qua email — token/link một lần, có thời hạn.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 95% người dùng hoàn tất đăng ký + xác thực OTP trong vòng 10 phút (tính từ gửi form đăng ký).
- **SC-002**: 99% yêu cầu đăng nhập hợp lệ (credential đúng, tài khoản ACTIVE) hoàn tất trong dưới 5 giây cảm nhận phía người dùng.
- **SC-003**: 100% tài khoản SUSPENDED bị từ chối login/refresh — không có trường hợp truy cập protected sau suspend.
- **SC-004**: 100% trường hợp Google login không đủ điều kiện auto-link đi qua xác minh OTP bổ sung trước khi liên kết — zero account takeover qua Google email trùng unverified.
- **SC-005**: Sau logout hoặc đổi/reset mật khẩu, 100% refresh token cũ không thể tái sử dụng.
- **SC-006**: Hệ thống phân quyền chính xác theo role cho 100% luồng auth thành công (trace §8 User Management).

## Assumptions

- Self-service register chỉ tạo role **Customer**; Employee, Manager, Admin do Admin tạo (ngoài scope FR-01 tạo tài khoản staff).
- Sau verify OTP đăng ký: chuyển **Login** (không auto-login) — pending OQ-05; có thể điều chỉnh sau clarify.
- Access token TTL 15 phút; refresh token TTL 7 ngày — đã chốt trong clarify session 2026-06-27.
- Google xác minh thêm dùng **OTP email** (không xác nhận password local).
- Rate limiting login/forgot-password: áp dụng mức bảo vệ cơ bản ở giai đoạn triển khai (chi tiết kỹ thuật trong plan).
- **Out of scope FR-01**: FR-02 User Profile, Admin quản lý suspend user (chỉ phản ứng auth), booking/payment/notification.
- Chi tiết triển khai API/UI: `plan.md` / `tasks.md` (cùng folder), `docs/api-spec-by-screen.md` (SCR-02–SCR-06, SCR-12).

## Clarifications

### Session 2026-06-27

- Q: Google xác minh thêm → A: OTP email + complete-link flow
- Q: Chính sách mật khẩu → A: Min 8 + chữ + số
- Q: OTP TTL/resend → A: 15 phút; max 3/giờ/email
- Q: Token TTL → A: Access 15 phút; Refresh 7 ngày
- Q: Change password sessions → A: Revoke tất cả; login lại
