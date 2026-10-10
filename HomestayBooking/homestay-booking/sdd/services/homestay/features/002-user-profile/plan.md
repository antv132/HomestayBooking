# Implementation Plan: FR-02 User Profile

**Branch**: `003-user-profile` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-user-profile/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-02, §2 Actors, §5 User, §6 RBAC, §7 Errors), `docs/api-spec-by-screen.md` (SCR-10–SCR-11), `docs/screen.md` (§2 Shared Screens), `AGENTS.md`

**Phụ thuộc**: FR-01 Authentication (`specs/001-user-auth`) — JWT bearer, User entity, SecurityConfig

## Summary

Triển khai module **FR-02 User Profile**: xem và cập nhật hồ sơ cá nhân (Self-scope) cho mọi user đã xác thực (Customer, Employee, Manager, Admin). Hai endpoint **`GET /api/v1/users/me`** và **`PUT /api/v1/users/me`**; email/role/status read-only. Stack: **React + TypeScript** (pages có sẵn, cần align path + mở rộng role) + **Spring Boot** (UserController, UserService — phụ thuộc FR-01 scaffold). **Out of scope**: đổi mật khẩu (FR-01/SCR-12), admin quản lý customer (FR-09), upload file avatar.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Bean Validation; Vite, Zustand, Axios, React Router  
**Storage**: SQL Server — bảng `users` (đã định nghĩa FR-01); FR-02 chỉ UPDATE `full_name`, `phone`, `avatar_url`, `updated_at`  
**Testing**: JUnit 5 + Mockito + `@SpringBootTest`; Vitest + React Testing Library  
**Target Platform**: Web (desktop + mobile responsive)  
**Project Type**: Web application (`Frontend/` + `Backend/`)  
**Performance Goals**: Profile GET/PUT p95 < 300ms; SC-001/SC-002 (95% < 3s/5s user-facing)  
**Constraints**: RBAC Self-only; không expose `password_hash`; envelope chuẩn api-spec §1  
**Scale/Scope**: 2 endpoints; 3 user stories; SCR-10 + SCR-11; 4 roles

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture (Controller → Service → Repository) | PASS | AGENTS.md |
| DTO + Bean Validation | PASS | UpdateProfileRequest validated |
| Security-first (JWT, RBAC Self) | PASS | FR-006, FR-007 |
| No secrets in code | PASS | N/A for profile |
| Test coverage ≥80% | PASS | Unit + integration UserController |
| Standard API envelope | PASS | api-spec-by-screen.md §1 |

**Post-design re-check**: PASS — 2 endpoints, no new tables, reuses User entity from FR-01.

## Project Structure

### Documentation (this feature)

```text
specs/002-user-profile/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/users-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/UserController.java          # GET/PUT /api/v1/users/me
├── dtos/user/UserProfileResponse.java
├── dtos/user/UpdateProfileRequest.java
├── services/UserProfileService.java
├── mappers/UserMapper.java                  # entity ↔ DTO
└── validations/VietnamesePhone.java         # @Pattern custom constraint

Backend/src/test/java/com/homestay/
├── unit/UserProfileServiceTest.java
└── integration/UserControllerIT.java

Frontend/src/
├── api/usersApi.ts                 # migrate → /api/v1/users/me
├── store/authStore.ts              # updateProfile (exists)
├── pages/customer/ProfilePages.tsx # SCR-10, SCR-11 (exists)
└── App.tsx                         # add Employee/Admin profile routes
```

**Structure Decision**: Web app monorepo — mở rộng backend FR-01; frontend đã có ProfilePages, chỉ cần path alignment và role routing.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | UserProfileService + DTOs + phone validation | FR-005, data-model.md |
| **B** | UserController GET/PUT `/users/me` + Security | FR-001–007, contracts |
| **C** | Frontend: `/api/v1/users/me`, authStore sync | US-3, SCR-10/11 |
| **D** | RoleLayout: Employee + Admin routes/layouts | screen.md §2 |
| **E** | avatarUrl field (URL input, optional MVP) | FR-003, research #4 |
| **F** | Tests + audit updatedAt | FR-008, SC-003–005 |

## Frontend Gap Analysis (hiện trạng)

| Item | Hiện tại | Target (docs) |
|------|----------|---------------|
| API path | `/api/users/me` | `/api/v1/users/me` |
| Roles with profile routes | CUSTOMER, MANAGER | + EMPLOYEE, ADMIN |
| RoleLayout | CustomerLayout / ManagerLayout only | + EmployeeLayout, AdminLayout |
| avatarUrl edit | Upload disabled | URL string input (MVP) or deferred |
| PUT response | expects full profile | Return UserProfileResponse (research #3) |
| Change password | `PATCH /api/users/me/password` | FR-01: `PUT /api/v1/auth/change-password` (out of FR-02) |

## Risks

| Risk | Mitigation |
|------|------------|
| Backend chưa scaffold (FR-01) | Implement UserController sau AuthController; shared User entity |
| api-spec PUT response `{}` | Return full profile — document deviation in contract (research #3) |
| Employee/Admin layouts chưa có | Reuse shared ProfilePages + RoleLayout switch |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/users-api.yaml](./contracts/users-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
