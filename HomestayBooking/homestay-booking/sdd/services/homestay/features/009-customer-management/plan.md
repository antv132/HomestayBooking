# Implementation Plan: FR-09 Customer Management

**Branch**: `main` (feature dir `009-customer-management`) | **Date**: 2026-07-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `sdd/services/homestay/features/009-customer-management/spec.md`

**Note**: `/speckit-plan` setup script resolved to `008-room-management` on branch `main`; this plan **overrides** to FR-09 after Clarifications Session 2026-07-23.

**Nguồn**: Clarified `spec.md` / `CONTEXT.md`; code as-is `AdminUserController` (`/api/admin/users`), `AdminUserService`, FE `CustomerDirectoryPage` + `_UserDirectoryShared`; FR-01 `AuthService` + `RefreshTokenRepository.revokeAllByUser`.

**Phụ thuộc**: FR-01 (User, SUSPENDED login/refresh block, refresh revoke helper); FR-04 (bookings read); FR-12 (Payment PAID for Total Spend). **Out of scope**: FR-17 Complaints; FR-02 self-profile write; Manager Customer List (SCR-55/56) — permanently out of FR-09.

## Summary

Hoàn thiện **FR-09 Admin Customer Directory (SCR-51)** trên nền code đã có: list CUSTOMER + aggregates (Total Bookings; **Total Spend = SUM Payment PAID**), detail drawer, booking history API. Gap chính: đổi toggle **ACTIVE ↔ SUSPENDED** (không còn ACTIVE ↔ INACTIVE), **revoke refresh tokens** khi Suspend, **ActivityLog** `USER_SUSPENDED` / `USER_ACTIVATED`, FE confirm + load booking history + filter/cột Total Spend. Prefer giữ `AdminUserService` / shared directory FE; migrate path sang `/api/v1/admin/users` khi touch API (optional cùng sprint nếu không phá FE).

## Technical Context

**Language/Version**: Java 17, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA; Vite, Axios, TanStack Query, Zustand, Tailwind  
**Storage**: SQL Server — write `users.status`; read `bookings`, `payments` (PAID); write `activity_logs`; update `refresh_tokens.revoked_at`  
**Testing**: JUnit 5 + Mockito — `AdminUserService` status rules, revoke-on-suspend, aggregates; RBAC smoke via existing controller IT pattern if present  
**Target Platform**: Web (Admin SPA + REST API)  
**Project Type**: `Backend/` + `Frontend/`  
**Performance Goals**: Customer list p95 &lt; 500ms (≤5k customers); detail + ≤50 bookings p95 &lt; 2s  
**Constraints**: ADMIN only; target `role=CUSTOMER`; status write chỉ ACTIVE↔SUSPENDED; INACTIVE listable nhưng Suspend/Activate disabled; no profile mutate; no Manager directory in FR-09  
**Scale/Scope**: Reuse SCR-50/51 directory surface; ~4 customer-facing ops (list/detail/status/bookings); 3 user stories

**Unknowns**: None blocking — Clarifications 2026-07-23 closed Total Spend, revoke, default filter, audit actions, Manager scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*  
*Source: `.specify/memory/constitution.md` v1.0.0*

| Gate | Status | Notes |
|------|--------|-------|
| I. Spec-First | PASS | Clarified `spec.md` + closed OPEN QUESTIONS in `CONTEXT.md` |
| II. Tech Stack | PASS | Java 17 / Spring Boot 3 / SQL Server / React 18 — no stack swap |
| III. Layered Boundaries | PASS | `AdminUserController` → `AdminUserService` → repos; DTOs + `ApiResponse` |
| IV. Security | PASS | `@PreAuthorize` ADMIN; CUSTOMER-only status writes; revoke refresh on Suspend; no secrets |
| V. Tests | PASS | Unit tests for status transitions, INACTIVE 409, revoke call, spend PAID aggregate |
| Complexity | PASS | No violations — reuse existing directory service vs parallel `AdminCustomer*` |

**Post-design re-check**: PASS — Total Spend via Payment PAID; audit via existing `ActivityLog` actions; revoke via existing `RefreshTokenRepository.revokeAllByUser`; INACTIVE gate FE+BE.

## Project Structure

### Documentation (this feature)

```text
sdd/services/homestay/features/009-customer-management/
├── CONTEXT.md
├── spec.md
├── plan.md                 # this file
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/customer-api.yaml
├── checklists/
└── tasks.md                # /speckit-tasks (next — not created here)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controller/AdminUserController.java          # SCR-50/51 — keep; add PATCH status or harden PUT
├── service/AdminUserService.java                # ACTIVE↔SUSPENDED + revoke + ActivityLog
├── service/ActivityLogService.java              # USER_SUSPENDED / USER_ACTIVATED
├── repository/PaymentRepository.java            # sumPaidAmount* (already FR-09-aligned)
├── repository/RefreshTokenRepository.java       # revokeAllByUser
├── repository/BookingRepository.java            # count + findByCustomerIdWithDetails
├── dto/request/AdminUpdateUserRequest.java      # or UpdateCustomerStatusRequest
└── dto/response/AdminUserResponse.java          # totalBookings, totalSpend

Backend/src/test/java/com/homestay/
└── service/AdminUserServiceCustomerTest.java    # status + revoke + aggregates

Frontend/src/
├── api/adminApi.ts                              # users list/update/bookings
├── pages/admin/CustomerDirectoryPage.tsx        # role=CUSTOMER
└── pages/admin/_UserDirectoryShared.tsx         # SUSPENDED UX, Total Spend col, history, confirm
```

**Structure Decision**: Extend **existing** Admin User Directory (SCR-50/51 shared), not greenfield `AdminCustomerController`. Contracts document customer ops; path may remain `/api/admin/users` or move to `/api/v1/admin/users` in implementation tasks.

## Complexity Tracking

> No constitution violations requiring justification.

## Implementation Phases (design → tasks)

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | BE: `updateUser` / dedicated status method — ACTIVE↔SUSPENDED only; reject INACTIVE → 409; reject non-CUSTOMER | FR-003, FR-008 |
| **B** | On SUSPENDED: `refreshTokenRepository.revokeAllByUser`; rely on FR-01 login/refresh block | FR-004, SC-002 |
| **C** | ActivityLog `USER_SUSPENDED` / `USER_ACTIVATED` via `ActivityLogService` | FR-009 |
| **D** | Confirm Total Spend = Payment PAID (already in service); list column + filter SUSPENDED | FR-001, FR-002 |
| **E** | FE: confirm dialog; disable actions for INACTIVE; load `.../bookings` in drawer; Total Spend column | US-1–3 |
| **F** | Optional: `/api/v1/admin/users` alias + OpenAPI; deprecate INACTIVE toggle wording | contracts |
| **G** | Unit tests + quickstart curl matrix (suspend → refresh fails) | SC-001–SC-006 |

## Gap Analysis (code vs clarified spec)

| Item | Hiện tại | Target |
|------|----------|--------|
| Status toggle | ACTIVE ↔ **INACTIVE** | ACTIVE ↔ **SUSPENDED** |
| Suspend side-effect | Status only | + **revoke all refresh tokens** |
| Audit | None on directory update | `USER_SUSPENDED` / `USER_ACTIVATED` |
| Total Spend | Payment PAID ✓ | Keep; show in FE table column |
| Default list | All statuses (no filter) ✓ | Keep |
| INACTIVE actions | Still toggled to INACTIVE | Disable Suspend/Activate |
| Booking history API | Exists | FE drawer must call it |
| Confirm before status | Missing / weak | ConfirmationDialog required |
| Manager list | Out of FR-09 | Do not add |

## Risks

| Risk | Mitigation |
|------|------------|
| Shared `updateUser` also used for Manager | Branch by role: Customer → ACTIVE/SUSPENDED; Manager keep existing ACTIVE/INACTIVE rules (FR-06) |
| Access JWT still valid after Suspend | Accept TTL expiry (clarified); refresh revoked immediately |
| FE still sends INACTIVE | BE reject; FE map Suspend→SUSPENDED |
| Dual path `/api/admin` vs `/api/v1` | Prefer one in tasks; update `adminApi.ts` + SecurityConfig together |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/customer-api.yaml](./contracts/customer-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md` (then `/speckit-implement`).
