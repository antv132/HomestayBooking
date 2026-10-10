# 001 - User Auth: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities

### User Class
Represents a user in the Homestay Booking System (Aggregate Root).
- **Attributes:**
  - `id` (UUID): Primary key.
  - `full_name` (String): Full name of the user.
  - `email` (String): Email address (unique).
  - `phone` (String): Phone number (optional for Google login).
  - `role` (Enum: `CUSTOMER`, `MANAGER`, `ADMIN`, `STAFF`): User's role.
  - `status` (Enum: `ACTIVE`, `INACTIVE`, `BANNED`): Current account status.
  - `password_hash` (String): BCrypt hashed password (null for Google-only users).
  - `email_verified` (Boolean): True if email is verified.
  - `auth_provider` (Enum: `LOCAL`, `GOOGLE`, `LINKED`): Method of authentication.
  - `google_id` (String): Google OAuth subject ID (unique, null if local).
  - `created_at` (Timestamp): Record creation time.
  - `updated_at` (Timestamp): Record last update time.

| No | Method | Description |
|---|---|---|
| - | - | (Entity class, no complex business methods. Standard getters/setters.) |


### OtpVerification Class
Stores OTPs for email verification during registration.
- **Attributes:**
  - `id` (UUID): Primary key.
  - `user_id` (UUID): Foreign key to User (null if email-first verification).
  - `email` (String): Email address the OTP was sent to.
  - `otp_hash` (String): BCrypt hashed OTP.
  - `purpose` (Enum: `REGISTER`, `PASSWORD_RESET`): Reason for OTP.
  - `expires_at` (Timestamp): Expiration time (typically 5-15 mins).
  - `consumed_at` (Timestamp): Time when OTP was successfully used.
- **Behaviors:**
  - `isValid()`: Checks if current time < `expires_at` and `consumed_at` is null.

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | isValid | Checks if current time < `expires_at` and `consumed_at` is null. Returns boolean. |


### RefreshToken Class
Stores refresh tokens for maintaining long-lived sessions securely.
- **Attributes:**
  - `id` (UUID): Primary key.
  - `user_id` (UUID): Foreign key to User.
  - `token_hash` (String): Hashed refresh token to prevent DB leak reuse.
  - `expires_at` (Timestamp): Expiration time (e.g., 7 days).
  - `revoked_at` (Timestamp): Time when token was explicitly revoked.
  - `replaced_by_id` (UUID): ID of the token that replaced this one (for rotation tracking).
  - `user_agent` (String): Browser/Device info of the session.
- **Behaviors:**
  - `isExpired()`: Checks if current time > `expires_at`.
  - `isRevoked()`: Checks if `revoked_at` is not null.

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | isExpired | Checks if current time > `expires_at`. Returns boolean. |
| 02 | isRevoked | Checks if `revoked_at` is not null. Returns boolean indicating if token has been explicitly revoked. |


### PasswordResetToken Class
Stores securely hashed tokens for the "Forgot Password" flow via link.
- **Attributes:**
  - `id` (UUID): Primary key.
  - `user_id` (UUID): Foreign key to User.
  - `token_hash` (String): BCrypt hashed reset token.
  - `expires_at` (Timestamp): Expiration time (typically 15-30 mins).
  - `consumed_at` (Timestamp): Time when token was successfully used.

| No | Method | Description |
|---|---|---|
| - | - | (Entity class, no complex business methods. Standard getters/setters.) |

---

## 2. Controllers

### AuthController Class
Exposes REST API endpoints for authentication.
- **Methods:**
  - `POST /api/v1/auth/register` (RegisterRequest) -> 201 Created
  - `POST /api/v1/auth/verify-otp` (VerifyOtpRequest) -> 200 OK (AuthResponse)
  - `POST /api/v1/auth/login` (LoginRequest) -> 200 OK (AuthResponse)
  - `POST /api/v1/auth/google` (GoogleLoginRequest) -> 200 OK (AuthResponse)
  - `POST /api/v1/auth/refresh` (RefreshTokenRequest) -> 200 OK (AuthResponse)
  - `POST /api/v1/auth/logout` (LogoutRequest) -> 204 No Content
  - `POST /api/v1/auth/forgot-password` (ForgotPasswordRequest) -> 200 OK
  - `POST /api/v1/auth/reset-password` (ResetPasswordRequest) -> 200 OK

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | register | POST `/api/v1/auth/register`. Receives `RegisterRequest`, calls `AuthService.registerUser`, returns 201 Created. |
| 02 | verifyOtp | POST `/api/v1/auth/verify-otp`. Receives `VerifyOtpRequest`, calls `AuthService.verifyOtp`, returns 200 OK with `AuthResponse`. |
| 03 | login | POST `/api/v1/auth/login`. Receives `LoginRequest`, calls `AuthService.login`, returns 200 OK with `AuthResponse`. |
| 04 | loginWithGoogle | POST `/api/v1/auth/google`. Receives `GoogleLoginRequest`, calls `AuthService.loginWithGoogle`, returns 200 OK with `AuthResponse`. |
| 05 | refreshToken | POST `/api/v1/auth/refresh`. Receives `RefreshTokenRequest`, calls `AuthService.refreshToken`, returns 200 OK with `AuthResponse`. |
| 06 | logout | POST `/api/v1/auth/logout`. Receives `LogoutRequest`, calls `AuthService.logout`, returns 204 No Content. |
| 07 | forgotPassword | POST `/api/v1/auth/forgot-password`. Receives `ForgotPasswordRequest`, calls `AuthService.requestPasswordReset`, returns 200 OK. |
| 08 | resetPassword | POST `/api/v1/auth/reset-password`. Receives `ResetPasswordRequest`, calls `AuthService.resetPassword`, returns 200 OK. |

---

## 3. Services (Core Logic)

### AuthService Class
Implements the core business logic for user authentication.
- **Methods:**
  - `registerUser(RegisterRequest)`: Validates input, saves user as INACTIVE, triggers OTP email.
  - `verifyOtp(VerifyOtpRequest)`: Validates OTP, activates user, returns tokens.
  - `login(LoginRequest)`: Validates credentials, checks if ACTIVE, returns tokens.
  - `loginWithGoogle(GoogleLoginRequest)`: Verifies Google token. If user exists, generates tokens. If not, creates new user (auth_provider=GOOGLE). Handles linking logic.
  - `refreshToken(String)`: Validates refresh token, implements token rotation, returns new token pair.
  - `logout(String)`: Revokes the specified refresh token.
  - `requestPasswordReset(String email)`: Generates and saves a password reset token, sends email.
  - `resetPassword(ResetPasswordRequest)`: Validates token, hashes new password, updates user, invalidates all active sessions.

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | registerUser | Validates input, saves user as INACTIVE, triggers OTP email. Returns void. |
| 02 | verifyOtp | Validates OTP, activates user, returns tokens. Returns `AuthResponse`. |
| 03 | login | Validates credentials, checks if ACTIVE, returns tokens. Returns `AuthResponse`. |
| 04 | loginWithGoogle | Verifies Google token. If user exists, generates tokens. If not, creates new user (auth_provider=GOOGLE). Handles linking. Returns `AuthResponse`. |
| 05 | refreshToken | Validates refresh token, implements token rotation, returns new token pair. Returns `AuthResponse`. |
| 06 | logout | Revokes the specified refresh token string. Returns void. |
| 07 | requestPasswordReset | Generates and saves a password reset token for the given email, sends email via `EmailService`. Returns void. |
| 08 | resetPassword | Validates token (`ResetPasswordRequest`), hashes new password, updates user, invalidates all active sessions. Returns void. |


### JwtService Class
Handles generation, parsing, and validation of JWTs.
- **Methods:**
  - `generateAccessToken(User)`: Returns short-lived JWT containing User ID and Role.
  - `generateRefreshToken(User)`: Returns long-lived opaque token (or JWT).
  - `validateToken(String)`: Verifies JWT signature and expiration.
  - `extractUserId(String)`: Parses JWT and returns the subject (User ID).

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | generateAccessToken | Takes a `User` entity, returns a short-lived JWT string containing User ID and Role. |
| 02 | generateRefreshToken | Takes a `User` entity, returns a long-lived opaque token (or JWT string). |
| 03 | validateToken | Takes a JWT string, verifies signature and expiration. Returns boolean. |
| 04 | extractUserId | Takes a JWT string, parses it and returns the subject (User ID) as UUID. |


### EmailService Class
Handles asynchronous email delivery.
- **Methods:**
  - `sendOtpEmail(String email, String otpCode)`
  - `sendPasswordResetEmail(String email, String resetLink)`

[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | sendOtpEmail | Takes `email` and `otpCode`. Sends an asynchronous email containing the OTP. Returns void. |
| 02 | sendPasswordResetEmail | Takes `email` and `resetLink`. Sends an asynchronous email containing the password reset link. Returns void. |
