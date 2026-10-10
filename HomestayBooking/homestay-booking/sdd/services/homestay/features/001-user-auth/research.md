# Research: FR-01 Authentication

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `docs/Specification_v2.md`, `docs/api-spec-by-screen.md`

## 1. JWT + Refresh Token

**Decision**: JWT access (15 min) + opaque refresh stored hashed in DB (7 days), rotate on each refresh, revoke on logout/reset/change-password.

**Rationale**: spec FR-007; §4 Security NFR (stateless + rotation); clarify session 2026-06-27.

**Alternatives considered**: Session cookies only — rejected (spec requires access + refresh).

## 2. Password Hashing

**Decision**: BCrypt strength 12 (`BCryptPasswordEncoder`).

**Rationale**: FR-013; AGENTS.md.

## 3. Google OAuth2

**Decision**: Google Identity Services → `idToken` → server verify. Auto-link only if local `emailVerified=true`; else OTP email + `POST /api/v1/auth/google/complete-link`.

**Rationale**: FR-006 nguyên văn Specification_v2 FR-01; clarify Q1.

## 4. OTP

**Decision**: 6-digit numeric; hash at rest; TTL 15 min; max 3 sends/hour/email (REGISTER + GOOGLE_LINK shared quota).

**Rationale**: spec FR-002; clarify Q3.

## 5. Password Policy

**Decision**: Min 8 chars; ≥1 letter + ≥1 digit. Bean Validation custom constraint.

**Rationale**: FR-013; clarify Q2.

## 6. API Paths

**Decision**: `/api/v1/auth/*` per `docs/api-spec-by-screen.md`. Extend contract for endpoints not yet in api-spec: `google`, `google/complete-link`, `refresh`, `logout`, `change-password`.

**Rationale**: Consistency with master API doc; FR-01 full surface.

## 7. Reset Password

**Decision**: Email link `/{frontend}/reset-password?token=...`; body `{ token, newPassword }` per SCR-06 / api-spec.

**Rationale**: Align api-spec; migrate frontend from legacy `email+otpCode`.

## 8. Post-Register Verify (OQ-05)

**Decision**: Redirect to Login after successful verify-otp (no tokens in response).

**Rationale**: Assumption in spec.md; safer UX consistency with change-password re-login.

## 9. Change Password Sessions (Clarified)

**Decision**: Revoke **all** refresh tokens including current; client clears storage → `/login`.

**Rationale**: FR-009; clarify Q5 Option A.

## 10. Rate Limiting (OQ-06 default)

**Decision**: 10 failed logins / 15 min / IP; 5 forgot-password / hour / IP. OTP resend covered by BR-02b.

**Rationale**: Basic abuse protection without blocking legitimate users.

## 11. Error Messages

**Decision**: Map to Specification_v2 §7 Authentication Errors verbatim categories.

**Rationale**: FR-014.

## 12. Email

**Decision**: Spring Mail SMTP; HTML templates for OTP and reset link.

**Rationale**: CLAUDE.md stack.

## 13. RBAC in JWT

**Decision**: Claim `role`: ADMIN | MANAGER | EMPLOYEE | CUSTOMER; Spring `ROLE_*` authorities.

**Rationale**: FR-012; §2 Actors.

## Traceability: Spec FR → Research

| Spec FR | Decision # |
|---------|------------|
| FR-001–003 | 4, 8 |
| FR-004, FR-010–011 | 1, 11 |
| FR-005–006 | 3 |
| FR-007 | 1 |
| FR-008–009 | 7, 9 |
| FR-012 | 13 |
| FR-013 | 2, 5 |
| FR-014 | 11 |
