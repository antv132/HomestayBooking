# Implementation Plan: FR-01 Authentication

**Branch**: `001-user-auth` (worktree: `docs/specification-v2-update`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-user-auth/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-01, §2, §4 Security, §5 User, §7, §8), `docs/api-spec-by-screen.md` (SCR-02–06), `docs/screen.md` (SCR-12), `AGENTS.md`

## Summary

Triển khai module **FR-01 Authentication** cho Homestay / Resort Booking System: đăng ký + OTP email, login email/password, Google OAuth2 (rule anti-takeover + OTP link), JWT access/refresh với rotation/revoke, forgot/reset/change password, logout, chặn SUSPENDED. Stack: **React + TypeScript** (frontend có sẵn) + **Spring Boot + SQL Server** (backend cần scaffold). API prefix **`/api/v1/auth/*`**, envelope `{ success, message, data|errors[] }`.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, jjwt, Google API Client (idToken), Spring Mail, BCrypt; Vite, Zustand, Axios, React Router  
**Storage**: SQL Server — `users` (extended), `refresh_tokens`, `otp_verifications`, `password_reset_tokens`  
**Testing**: JUnit 5 + Mockito + `@SpringBootTest`; Vitest + React Testing Library  
**Target Platform**: Web (desktop + mobile responsive)  
**Project Type**: Web application (`Frontend/` + `Backend/`)  
**Performance Goals**: Auth p95 < 500ms; hệ thống ≥500 concurrent users (NFR §4)  
**Constraints**: HTTPS; RBAC 5 roles; không hardcode secrets; Google link rule nguyên văn Specification_v2  
**Scale/Scope**: 10 auth endpoints; 6 user stories; SCR-02–06 + SCR-12

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture (Controller → Service → Repository) | PASS | AGENTS.md |
| DTO + Bean Validation | PASS | Mọi auth request validated |
| Security-first (BCrypt, JWT, RBAC) | PASS | spec FR-001–FR-014 |
| No secrets in code | PASS | Env: `JWT_SECRET`, `GOOGLE_CLIENT_ID`, SMTP |
| Test coverage ≥80% | PASS | Target auth unit + integration |
| Standard API envelope | PASS | api-spec-by-screen.md §1 |

**Post-design re-check**: PASS — entities/contracts align with spec; no unjustified complexity.

## Project Structure

### Documentation (this feature)

```text
specs/001-user-auth/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/auth-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── configs/SecurityConfig.java, JwtProperties.java
├── controllers/AuthController.java
├── dtos/auth/
├── entities/User.java, RefreshToken.java, OtpVerification.java, PasswordResetToken.java
├── enums/UserRole, UserStatus, AuthProvider, OtpPurpose
├── repositories/
├── security/JwtService.java, GoogleTokenVerifier.java
├── services/AuthService.java, OtpService.java, TokenService.java, MailService.java
└── validations/PasswordPolicy.java

Backend/src/test/java/com/homestay/
├── unit/
└── integration/AuthControllerIT.java

Frontend/src/
├── api/authApi.ts              # migrate → /api/v1/auth/*
├── api/axiosInstance.ts        # refresh interceptor (exists)
├── store/authStore.ts          # exists
├── pages/public/LoginPage.tsx, RegisterPage.tsx, VerifyEmailPage.tsx
├── pages/public/ForgotPasswordPage.tsx, ResetPasswordPage.tsx
└── pages/.../ChangePasswordPage.tsx   # SCR-12
```

**Structure Decision**: Web app monorepo — `Frontend/` (existing) + `Backend/` (new module). Package base `com.homestay` per CLAUDE.md.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | DB migrations + entities + password policy | FR-013, data-model.md |
| **B** | Register, verify-otp, resend OTP | US-1, FR-001–003 |
| **C** | Login, refresh, logout | US-2, US-4, FR-004, FR-007, FR-010 |
| **D** | Google auth + complete-link OTP | US-3, FR-005–006 |
| **E** | Forgot/reset/change password | US-5, US-6, FR-008–009, FR-011 |
| **F** | Frontend align + SCR mapping | api-spec-by-screen, screen.md |
| **G** | Tests + rate limit + security hardening | SC-001–006 |

## Frontend Gap Analysis (hiện trạng)

| Item | Hiện tại (`authApi.ts`) | Target (docs) |
|------|-------------------------|---------------|
| Base path | `/api/auth/*` | `/api/v1/auth/*` |
| verify OTP field | `otpCode` | `otp` |
| reset password | `email, otpCode, newPassword` | `token, newPassword` |
| Google complete-link | missing | `POST .../google/complete-link` |
| change-password | missing | `PUT .../change-password` |
| Roles | CUSTOMER, MANAGER | + ADMIN, EMPLOYEE |

## Risks

| Risk | Mitigation |
|------|------------|
| Backend chưa có trong repo | Phase A scaffold Spring Boot |
| api-spec thiếu google/refresh/logout/change | Bổ sung trong `contracts/auth-api.yaml` (FR-01 spec) |
| OQ-05 auto-login sau verify | Default: redirect Login (research.md) |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/auth-api.yaml](./contracts/auth-api.yaml)
- [quickstart.md](./quickstart.md)

## Complexity Tracking

> Không có vi phạm cần biện minh.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
