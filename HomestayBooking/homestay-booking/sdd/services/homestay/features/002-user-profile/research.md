# Research: FR-02 User Profile

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `docs/Specification_v2.md`, `docs/api-spec-by-screen.md`, `docs/screen.md`

## 1. Identity Resolution (`/users/me`)

**Decision**: Resolve current user exclusively from JWT `sub` (user UUID) via `@AuthenticationPrincipal UserPrincipal`; never accept user id from path/body.

**Rationale**: FR-006 Self-scope; zero cross-user access (SC-003).

**Alternatives considered**: Path `/users/{id}` — rejected (violates Self-only, enables IDOR).

## 2. Updatable Fields

**Decision**: PUT body accepts optional `fullName`, `phone`, `avatarUrl`. Server **ignores** `email`, `role`, `status`, `password` if sent (strip via DTO whitelist).

**Rationale**: FR-003, FR-004; email is login identifier.

**Alternatives considered**: PATCH partial — rejected (api-spec defines PUT SCR-11).

## 3. PUT Response Body

**Decision**: Return full `UserProfileResponse` in `data` (not empty `{}`).

**Rationale**: Frontend `updateMyProfile()` expects updated profile; enables US-3 authStore sync without extra GET. api-spec SCR-11 shows `{}` — contract documents intentional extension.

**Alternatives considered**: Empty body + client re-fetch GET — rejected (extra round-trip).

## 4. Avatar (MVP)

**Decision**: `avatarUrl` optional HTTPS URL string (max 512 chars). No multipart upload in FR-02.

**Rationale**: spec Assumptions; api-spec SCR-11; UI already has disabled Upload button.

**Alternatives considered**: S3 upload endpoint — deferred to future FR.

## 5. Phone Validation

**Decision**: Vietnamese format — optional field; if present must match `^(\+84|0)[0-9]{9,10}$` (normalize strip spaces/dashes before validate). Bean Validation `@VietnamesePhone`.

**Rationale**: §7 Invalid phone number; matches existing `ProfilePages.tsx` client validation.

**Alternatives considered**: E.164 lib — overkill for VN-only homestay domain.

## 6. fullName Validation

**Decision**: `@NotBlank`, trim, max 255 chars.

**Rationale**: FR-005; §7 Required field missing.

## 7. Authorization

**Decision**: `@PreAuthorize("isAuthenticated()")` on `/users/me/*`; no role-specific checks (all 4 roles equal for Self profile).

**Rationale**: screen.md SCR-10/11 ALL AUTH; RBAC matrix R/U Self for Customer & Employee; Manager/Admin also Self for own profile.

## 8. API Base Path

**Decision**: `/api/v1/users/me` per `docs/api-spec-by-screen.md`.

**Rationale**: Global API v2.1.0 standard; migrate frontend from `/api/users/me`.

## 9. Suspended User Access

**Decision**: FR-01 auth layer rejects SUSPENDED on every authenticated request (including profile) — profile module does not duplicate check if global filter exists; add integration test asserting 403.

**Rationale**: spec US-1 scenario 3; single enforcement point.

## 10. Profile Fields in GET Response

**Decision**: Return `id`, `fullName`, `email`, `phone`, `avatarUrl`, `role`, `status`, `createdAt`, `updatedAt`. Exclude `passwordHash`, `googleId`.

**Rationale**: FR-002; frontend `UserProfile` interface; api-spec minimal fields extended for UI (member since, status badge).

**Alternatives considered**: Minimal api-spec only — rejected (UI needs role/status/createdAt).

## 11. Employee/Admin Routes

**Decision**: Add `/employee/profile`, `/admin/profile` (+ `/edit`) mirroring customer/manager pattern; extend `RoleLayout` in ProfilePages.

**Rationale**: screen.md §2 lists all 4 roles; App.tsx currently missing Employee/Admin routes.
