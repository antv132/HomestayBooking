# Tasks: FR-01 Authentication

**Input**: Design documents from `specs/001-user-auth/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/auth-api.yaml, quickstart.md

**Tests**: Không có phase test riêng (spec không yêu cầu TDD). Integration smoke tests trong Phase Polish.

**Organization**: Tasks grouped by user story (US1–US6) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US6 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Scaffold backend và cấu hình môi trường FR-01

- [ ] T001 Create Spring Boot project structure in `Backend/` per plan.md (pom.xml, `com.homestay` package, application.yml)
- [ ] T002 [P] Add dependencies in `Backend/pom.xml`: Spring Web, Security, Data JPA, Validation, SQL Server (mssql-jdbc), Flyway, jjwt, Google API Client, Spring Mail
- [ ] T003 [P] Configure SQL Server + Flyway in `Backend/src/main/resources/application.yml`
- [ ] T004 [P] Add env template `Backend/.env.example` (JWT_SECRET, GOOGLE_CLIENT_ID, SMTP, FRONTEND_BASE_URL)
- [ ] T005 [P] Update Vite proxy to `/api/v1` in `Frontend/vite.config.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core auth infrastructure — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [ ] T006 Create Flyway migration `Backend/src/main/resources/db/migration/V001__users_auth_columns.sql` per data-model.md
- [ ] T007 [P] Create Flyway migration `Backend/src/main/resources/db/migration/V002__refresh_tokens.sql`
- [ ] T008 [P] Create Flyway migration `Backend/src/main/resources/db/migration/V003__otp_verifications.sql`
- [ ] T009 [P] Create Flyway migration `Backend/src/main/resources/db/migration/V004__password_reset_tokens.sql`
- [ ] T010 [P] Create enums in `Backend/src/main/java/com/homestay/enums/`: UserRole, UserStatus, AuthProvider, OtpPurpose
- [ ] T011 [P] Create entity `User.java` in `Backend/src/main/java/com/homestay/entities/User.java`
- [ ] T012 [P] Create entity `RefreshToken.java` in `Backend/src/main/java/com/homestay/entities/RefreshToken.java`
- [ ] T013 [P] Create entity `OtpVerification.java` in `Backend/src/main/java/com/homestay/entities/OtpVerification.java`
- [ ] T014 [P] Create entity `PasswordResetToken.java` in `Backend/src/main/java/com/homestay/entities/PasswordResetToken.java`
- [ ] T015 [P] Create JPA repositories in `Backend/src/main/java/com/homestay/repositories/` for all four entities
- [ ] T016 Create `@PasswordPolicy` validator in `Backend/src/main/java/com/homestay/validations/PasswordPolicy.java` (min 8, letter+digit)
- [ ] T017 Create `JwtProperties.java` + `JwtService.java` in `Backend/src/main/java/com/homestay/security/` (15m access, 7d refresh)
- [ ] T018 Create `TokenService.java` in `Backend/src/main/java/com/homestay/services/TokenService.java` (hash, rotate, revoke all/single)
- [ ] T019 Create `MailService.java` in `Backend/src/main/java/com/homestay/services/MailService.java` (OTP + reset link templates)
- [ ] T020 Create standard `ApiResponse.java` DTO in `Backend/src/main/java/com/homestay/dtos/ApiResponse.java`
- [ ] T021 Create `GlobalExceptionHandler.java` in `Backend/src/main/java/com/homestay/exceptions/GlobalExceptionHandler.java` mapping §7 auth errors
- [ ] T022 Configure `SecurityConfig.java` in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java` (public auth routes, JWT filter skeleton)
- [ ] T023 Migrate `Frontend/src/api/authApi.ts` base paths from `/api/auth/*` to `/api/v1/auth/*`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Đăng ký và xác thực OTP (Priority: P1) 🎯 MVP

**Goal**: Guest đăng ký Customer, nhận OTP email, verify → ACTIVE

**Independent Test**: POST register → email OTP → verify-otp → user ACTIVE; redirect Login (no tokens)

### Implementation

- [ ] T024 [P] [US1] Create DTOs `RegisterRequest.java`, `VerifyOtpRequest.java` in `Backend/src/main/java/com/homestay/dtos/auth/`
- [ ] T025 [US1] Implement `OtpService.java` in `Backend/src/main/java/com/homestay/services/OtpService.java` (generate, hash, validate, resend quota 3/hr)
- [ ] T026 [US1] Implement register + verify-otp in `AuthService.java` (`Backend/src/main/java/com/homestay/services/AuthService.java`)
- [ ] T027 [US1] Add `POST /api/v1/auth/register` and `POST /api/v1/auth/verify-otp` in `AuthController.java`
- [ ] T028 [P] [US1] Add `POST /api/v1/auth/resend-otp` endpoint in `AuthController.java` (if not in contract, align with frontend resendOtp)
- [ ] T029 [P] [US1] Update `RegisterPage.tsx` in `Frontend/src/pages/public/RegisterPage.tsx` for `/api/v1` + password policy UI
- [ ] T030 [US1] Update `VerifyEmailPage.tsx` in `Frontend/src/pages/public/VerifyEmailPage.tsx` (field `otp`, redirect Login on success)

**Checkpoint**: US1 independently testable via curl quickstart §Register

---

## Phase 4: User Story 2 — Đăng nhập email/mật khẩu (Priority: P1)

**Goal**: Login với email/password; RBAC role in response; block unverified/suspended/Google-only

**Independent Test**: ACTIVE verified user login → accessToken + refreshToken + user.role

### Implementation

- [ ] T031 [P] [US2] Create `LoginRequest.java` in `Backend/src/main/java/com/homestay/dtos/auth/LoginRequest.java`
- [ ] T032 [US2] Implement login logic in `AuthService.java` (BCrypt verify, status checks FR-010/011/014)
- [ ] T033 [US2] Add `POST /api/v1/auth/login` in `AuthController.java` returning AuthTokens per contracts/auth-api.yaml
- [ ] T034 [US2] Wire JWT authentication filter in `SecurityConfig.java` for protected routes
- [ ] T035 [US2] Update `LoginPage.tsx` in `Frontend/src/pages/public/LoginPage.tsx` (error messages, role-based redirect)
- [ ] T036 [P] [US2] Extend `Role` type in `Frontend/src/api/authApi.ts` to include ADMIN, EMPLOYEE, MANAGER, CUSTOMER

**Checkpoint**: US2 testable — login after US1 verify

---

## Phase 5: User Story 4 — Quản lý phiên refresh & logout (Priority: P1)

**Goal**: Refresh token rotation; logout revokes token

**Independent Test**: Login → refresh → new tokens; logout → old refresh fails

### Implementation

- [ ] T037 [P] [US4] Create `RefreshTokenRequest.java` in `Backend/src/main/java/com/homestay/dtos/auth/RefreshTokenRequest.java`
- [ ] T038 [US4] Implement refresh + logout in `AuthService.java` using `TokenService.java`
- [ ] T039 [US4] Add `POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout` in `AuthController.java`
- [ ] T040 [US4] Verify `Frontend/src/api/axiosInstance.ts` refresh interceptor uses `/api/v1/auth/refresh` and token rotation

**Checkpoint**: US4 testable with curl quickstart §Refresh/Logout

---

## Phase 6: User Story 3 — Đăng nhập Google (Priority: P1)

**Goal**: Google idToken login; auto-link if verified; else OTP + complete-link

**Independent Test**: Google verified local → tokens; unverified → pendingVerification + OTP flow

### Implementation

- [ ] T041 [P] [US3] Create `GoogleTokenVerifier.java` in `Backend/src/main/java/com/homestay/security/GoogleTokenVerifier.java`
- [ ] T042 [P] [US3] Create DTOs `GoogleAuthRequest.java`, `GoogleCompleteLinkRequest.java` in `Backend/src/main/java/com/homestay/dtos/auth/`
- [ ] T043 [US3] Implement Google login + complete-link in `AuthService.java` (FR-006 anti-takeover rule)
- [ ] T044 [US3] Add `POST /api/v1/auth/google` and `POST /api/v1/auth/google/complete-link` in `AuthController.java`
- [ ] T045 [P] [US3] Add Google Sign-In button + idToken flow in `Frontend/src/pages/public/LoginPage.tsx`
- [ ] T046 [US3] Handle `GOOGLE_LINK` context in `Frontend/src/pages/public/VerifyEmailPage.tsx` calling `google/complete-link`
- [ ] T047 [P] [US3] Add `loginWithGoogle` + `completeGoogleLink` in `Frontend/src/api/authApi.ts`

**Checkpoint**: US3 testable — both auto-link and OTP branches

---

## Phase 7: User Story 5 — Quên và đặt lại mật khẩu (Priority: P2)

**Goal**: Forgot password email; reset via token link; revoke all sessions on reset

**Independent Test**: forgot → email token → reset → login with new password; old refresh invalid

### Implementation

- [ ] T048 [P] [US5] Create DTOs `ForgotPasswordRequest.java`, `ResetPasswordRequest.java` in `Backend/src/main/java/com/homestay/dtos/auth/`
- [ ] T049 [US5] Implement forgot + reset in `AuthService.java` (generic response BR-14, revoke all tokens)
- [ ] T050 [US5] Add `POST /api/v1/auth/forgot-password` and `POST /api/v1/auth/reset-password` in `AuthController.java`
- [ ] T051 [P] [US5] Create/update `ForgotPasswordPage.tsx` in `Frontend/src/pages/public/ForgotPasswordPage.tsx`
- [ ] T052 [US5] Create/update `ResetPasswordPage.tsx` in `Frontend/src/pages/public/ResetPasswordPage.tsx` (query `token`, body `{ token, newPassword }`)
- [ ] T053 [US5] Update `forgotPassword` + `resetPassword` in `Frontend/src/api/authApi.ts` to match contract

**Checkpoint**: US5 testable end-to-end

---

## Phase 8: User Story 6 — Đổi mật khẩu (Priority: P2)

**Goal**: Authenticated change password; revoke ALL sessions; redirect login

**Independent Test**: change-password success → must re-login; old refresh invalid

### Implementation

- [ ] T054 [P] [US6] Create `ChangePasswordRequest.java` in `Backend/src/main/java/com/homestay/dtos/auth/ChangePasswordRequest.java`
- [ ] T055 [US6] Implement change-password in `AuthService.java` (revoke all refresh tokens per clarify session)
- [ ] T056 [US6] Add `PUT /api/v1/auth/change-password` in `AuthController.java`
- [ ] T057 [P] [US6] Create `ChangePasswordPage.tsx` (SCR-12) in `Frontend/src/pages/` + route in `Frontend/src/App.tsx`
- [ ] T058 [US6] Add `changePassword` method in `Frontend/src/api/authApi.ts` + post-success logout redirect

**Checkpoint**: US6 testable — SCR-12 complete

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Security hardening, docs sync, validation

- [ ] T059 [P] Add login/forgot rate limiting filter in `Backend/src/main/java/com/homestay/filters/RateLimitFilter.java` per research.md
- [ ] T060 [P] Add auth integration tests in `Backend/src/test/java/com/homestay/integration/AuthControllerIT.java` (login, OTP, Google pending, logout)
- [ ] T061 Sync auth endpoints into `docs/api-spec-by-screen.md` (google, refresh, logout, change-password)
- [ ] T062 [P] Update `Frontend/src/store/authStore.ts` if role routing needs ADMIN/EMPLOYEE paths
- [ ] T063 Run manual validation per `specs/001-user-auth/quickstart.md` curl + UI flows
- [ ] T064 Security review: no password/token logging; env secrets only; HTTPS note in `Backend/README.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup** → **Phase 2 Foundational** → **User Story Phases 3–8** → **Phase 9 Polish**
- US2 depends on US1 (need verified user to login)
- US4 depends on US2 (need login to test refresh/logout)
- US3 can start after Phase 2 (parallel with US1/US2 if mock Google)
- US5/US6 depend on Phase 2 + US2 (local accounts)

### User Story Dependencies

| Story | Depends on | Independent after |
|-------|------------|-------------------|
| US1 Register+OTP | Phase 2 | Phase 2 |
| US2 Login | US1 (test user) | Phase 2 + US1 |
| US4 Session | US2 | US2 |
| US3 Google | Phase 2 | Phase 2 (OTP branch needs OtpService from US1) |
| US5 Forgot/Reset | US2 | US2 |
| US6 Change pwd | US2 | US2 |

**Recommended order**: US1 → US2 → US4 → US3 → US5 → US6

### Parallel Opportunities

- Phase 1: T002–T005 parallel
- Phase 2: T007–T015 parallel after T006
- After Phase 2: US1 backend (T024–T028) parallel with US1 frontend (T029–T030)
- US3 frontend (T045, T047) parallel with US3 backend after T043

---

## Parallel Example: Phase 2 Foundational

```bash
# Migrations in parallel (after V001):
T007 refresh_tokens.sql
T008 otp_verifications.sql
T009 password_reset_tokens.sql

# Entities in parallel:
T011 User.java | T012 RefreshToken.java | T013 OtpVerification.java | T014 PasswordResetToken.java
```

---

## Parallel Example: User Story 3

```bash
T041 GoogleTokenVerifier.java
T042 Google DTOs
# then sequentially:
T043 AuthService Google logic → T044 Controller → T045/T046/T047 Frontend
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2)

1. Complete Phase 1 + Phase 2
2. Complete Phase 3 (US1) + Phase 4 (US2)
3. **STOP and VALIDATE**: Register → verify → login
4. Demo onboarding flow

### Incremental Delivery

1. Setup + Foundational
2. US1 Register+OTP → MVP onboarding
3. US2 Login + US4 Session → full local auth
4. US3 Google → social login
5. US5 + US6 Password flows → complete FR-01
6. Polish → production-ready

### Suggested MVP Scope

**Phases 1–4 only** (T001–T036): Register, OTP verify, email login — covers FR-001–004 minimum viable auth.

---

## Notes

- Package base: `com.homestay` (CLAUDE.md)
- API envelope: `{ success, message, data|errors[] }` per docs/api-spec-by-screen.md
- Google linking rule: verbatim Specification_v2 FR-01 in `AuthService` US3 logic
- Commit after each phase checkpoint
- Total tasks: **64** (T001–T064)
