# Tasks: FR-02 User Profile

**Input**: Design documents from `specs/002-user-profile/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/users-api.yaml, quickstart.md

**Phụ thuộc FR-01**: JWT auth, `User` entity, `SecurityConfig`, `ApiResponse`, `GlobalExceptionHandler` (`specs/001-user-auth/`)

**Tests**: Không có phase test riêng (spec không yêu cầu TDD). Integration tests trong Phase Polish.

**Organization**: Tasks grouped by user story (US1–US3) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US3 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Contracts**: `specs/002-user-profile/contracts/users-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Xác nhận prerequisite FR-01 và cấu hình sẵn có

- [ ] T001 Verify FR-01 foundation exists: `User.java`, JWT filter, `SecurityConfig.java`, login flow per `specs/001-user-auth/quickstart.md`
- [ ] T002 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts` (shared with FR-01)
- [ ] T003 [P] Add FR-02 contract reference comment in `specs/002-user-profile/contracts/users-api.yaml` linking SCR-10/SCR-11 from `docs/api-spec-by-screen.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: DTOs, validation, service layer — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [ ] T004 [P] Create `UserProfileResponse.java` in `Backend/src/main/java/com/homestay/dtos/user/UserProfileResponse.java` per data-model.md
- [ ] T005 [P] Create `UpdateProfileRequest.java` in `Backend/src/main/java/com/homestay/dtos/user/UpdateProfileRequest.java` (optional fields; ignore email/role/status)
- [ ] T006 [P] Create `@VietnamesePhone` constraint in `Backend/src/main/java/com/homestay/validations/VietnamesePhone.java` + validator (pattern `^(\+84|0)[0-9]{9,10}$`)
- [ ] T007 [P] Create `UserMapper.java` in `Backend/src/main/java/com/homestay/mappers/UserMapper.java` (entity → UserProfileResponse; exclude passwordHash/googleId)
- [ ] T008 Implement `UserProfileService.java` in `Backend/src/main/java/com/homestay/services/UserProfileService.java` with `getMyProfile(UUID userId)` loading by JWT sub only
- [ ] T009 Extend `UserProfileService.java` with `updateMyProfile(UUID userId, UpdateProfileRequest)` — partial update, set `updatedAt`, ignore privileged fields (FR-004, FR-008)
- [ ] T010 Register `/api/v1/users/me` as authenticated-only in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java` (`@PreAuthorize("isAuthenticated()")` on controller)
- [ ] T011 Migrate API paths in `Frontend/src/api/usersApi.ts` from `/api/users/me` to `/api/v1/users/me` (GET + PUT)

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Xem hồ sơ cá nhân (Priority: P1) 🎯 MVP

**Goal**: Authenticated user xem SCR-10 — fullName, email, phone, avatarUrl, role, status

**Independent Test**: Login → GET `/api/v1/users/me` → UI `/customer/profile` hiển thị đúng user hiện tại; 401 khi chưa login

### Implementation

- [ ] T012 [US1] Create `UserController.java` in `Backend/src/main/java/com/homestay/controllers/UserController.java` with `GET /api/v1/users/me` returning `ApiResponse<UserProfileResponse>`
- [ ] T013 [US1] Wire `@AuthenticationPrincipal` (or JWT `sub`) in `UserController.java` — never accept user id from client (FR-006, SC-003)
- [ ] T014 [P] [US1] Map validation errors in `GlobalExceptionHandler.java` for profile DTOs (§7: Required field missing, Invalid phone number)
- [ ] T015 [US1] Update `UserProfilePage` in `Frontend/src/pages/customer/ProfilePages.tsx` — role badge for CUSTOMER, MANAGER, EMPLOYEE, ADMIN
- [ ] T016 [P] [US1] Ensure avatar fallback initials when `avatarUrl` null in `Frontend/src/pages/customer/ProfilePages.tsx` (edge case spec)
- [ ] T017 [US1] Verify protected routes in `Frontend/src/App.tsx` for `/customer/profile` and `/manager/profile` redirect unauthenticated users to login

**Checkpoint**: US1 independently testable via curl quickstart §GET profile + SCR-10 UI

---

## Phase 4: User Story 2 — Cập nhật hồ sơ cá nhân (Priority: P1)

**Goal**: SCR-11 — cập nhật fullName, phone, avatarUrl; email read-only

**Independent Test**: PUT valid payload → DB updated → GET reflects changes; invalid phone/fullName → 400; email unchanged

### Implementation

- [ ] T018 [US2] Add `PUT /api/v1/users/me` in `Backend/src/main/java/com/homestay/controllers/UserController.java` returning full `UserProfileResponse` per contracts/users-api.yaml
- [ ] T019 [US2] Add server-side strip/ignore for `email`, `role`, `status` in `UpdateProfileRequest` deserialization or service layer (FR-004, SC-004)
- [ ] T020 [US2] Normalize phone (strip spaces/dashes) before validation in `UserProfileService.java`
- [ ] T021 [US2] Update `EditProfilePage` in `Frontend/src/pages/customer/ProfilePages.tsx` — email field disabled with hint "Email cannot be changed"
- [ ] T022 [P] [US2] Add optional `avatarUrl` URL input field in `EditProfilePage` in `Frontend/src/pages/customer/ProfilePages.tsx` (MVP URL string per research.md #4)
- [ ] T023 [US2] Display backend validation errors (`errors[]`) on `EditProfilePage` in `Frontend/src/pages/customer/ProfilePages.tsx`
- [ ] T024 [US2] Verify partial update: omitting `phone`/`avatarUrl` in PUT leaves existing values unchanged (integration assertion in service test or manual curl)

**Checkpoint**: US2 testable — edit → save → view profile shows updated data

---

## Phase 5: User Story 3 — Đồng bộ hồ sơ sau cập nhật (Priority: P2)

**Goal**: Header/layout hiển thị fullName và avatarUrl mới ngay sau save — không cần re-login

**Independent Test**: Edit fullName → save → `CustomerLayout`/`ManagerLayout` header shows new name without page reload beyond navigation

### Implementation

- [ ] T025 [US3] Ensure `updateMyProfile` success calls `useAuthStore.updateProfile` with fullName, phone, avatarUrl in `Frontend/src/pages/customer/ProfilePages.tsx` (verify existing wiring)
- [ ] T026 [US3] Extend `updateProfile` in `Frontend/src/store/authStore.ts` to persist all profile fields to sessionStorage consistently
- [ ] T027 [P] [US3] Verify `CustomerLayout.tsx` and `ManagerLayout.tsx` read `fullName`/`avatarUrl` from `useAuthStore` (no stale hardcoded values)
- [ ] T028 [US3] Add profile link in sidebar/nav for both layouts pointing to role-specific profile route (`/customer/profile`, `/manager/profile`)

**Checkpoint**: US3 testable — edit name → dashboard header updates immediately

---

## Phase 6: Multi-Role Profile Access (Cross-Story)

**Purpose**: SCR-10/SCR-11 cho Employee và Admin per `docs/screen.md` §2

- [ ] T029 [P] Create `EmployeeLayout.tsx` in `Frontend/src/layouts/EmployeeLayout.tsx` (minimal shell reusing CustomerLayout nav pattern + profile link)
- [ ] T030 [P] Create `AdminLayout.tsx` in `Frontend/src/layouts/AdminLayout.tsx` (minimal shell + profile link)
- [ ] T031 Extend `RoleLayout` in `Frontend/src/pages/customer/ProfilePages.tsx` to support EMPLOYEE and ADMIN layouts
- [ ] T032 Add profile routes in `Frontend/src/App.tsx`: `/employee/profile`, `/employee/profile/edit`, `/admin/profile`, `/admin/profile/edit` with `ProtectedRoute`
- [ ] T033 [P] Update role badge labels in `UserProfilePage` within `Frontend/src/pages/customer/ProfilePages.tsx` for Employee and Admin display names

**Checkpoint**: All 4 roles can access SCR-10/SCR-11 at role-specific routes

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Tests, security, docs validation

- [ ] T034 [P] Add unit tests in `Backend/src/test/java/com/homestay/unit/UserProfileServiceTest.java` (get self, update fields, ignore email, phone validation, updatedAt)
- [ ] T035 [P] Add integration tests in `Backend/src/test/java/com/homestay/integration/UserControllerIT.java` (GET/PUT auth, 401 unauthenticated, validation 400, suspended 403 if FR-01 filter active)
- [ ] T036 Run manual validation per `specs/002-user-profile/quickstart.md` curl smoke tests
- [ ] T037 [P] Verify `UserProfileResponse` never exposes `passwordHash` or `googleId` in JSON serialization
- [ ] T038 Confirm change-password remains on FR-01 path (`PUT /api/v1/auth/change-password`) — do not implement `PATCH /users/me/password` in FR-02
- [ ] T039 Sync SCR-10/SCR-11 PUT response note (returns full profile) in `docs/api-spec-by-screen.md` if deviating from empty `{}`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup** → **Phase 2 Foundational** → **Phase 3 US1** → **Phase 4 US2** → **Phase 5 US3** → **Phase 6 Multi-Role** → **Phase 7 Polish**
- **FR-01 must be complete** (or minimally: User entity + JWT login) before Phase 2

### User Story Dependencies

| Story | Depends on | Independent after |
|-------|------------|-------------------|
| US1 View profile | Phase 2 + FR-01 JWT | Phase 2 |
| US2 Update profile | US1 (GET endpoint + UserController exists) | US1 backend controller |
| US3 Store sync | US2 (update flow exists) | US2 |
| Multi-Role routes | US1 frontend (ProfilePages) | US1 |

**Recommended order**: US1 → US2 → US3 → Multi-Role → Polish

### Parallel Opportunities

- Phase 1: T002–T003 parallel
- Phase 2: T004–T007 parallel; then T008→T009 sequential; T010–T011 parallel after T008
- US1: T014–T016 parallel after T012
- US2: T022 parallel with T021 after T018
- US3: T027 parallel with T025–T026
- Phase 6: T029–T030 parallel; T033 parallel with T032 after T031
- Polish: T034–T035, T037 parallel

---

## Parallel Example: Phase 2 Foundational

```bash
# DTOs + validation in parallel:
T004 UserProfileResponse.java
T005 UpdateProfileRequest.java
T006 VietnamesePhone.java
T007 UserMapper.java

# Then service (sequential):
T008 getMyProfile → T009 updateMyProfile

# Config + frontend API in parallel:
T010 SecurityConfig.java
T011 usersApi.ts
```

---

## Parallel Example: User Story 2

```bash
# Backend first:
T018 PUT endpoint → T019 ignore privileged fields → T020 phone normalize

# Frontend in parallel after T018:
T021 email readonly UI
T022 avatarUrl URL input
T023 validation error display
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2)

1. Complete Phase 1 + Phase 2 (requires FR-01 JWT + User entity)
2. Complete Phase 3 (US1) — view profile SCR-10
3. Complete Phase 4 (US2) — edit profile SCR-11
4. **STOP and VALIDATE**: Login → view → edit fullName/phone → view confirms change
5. Demo for Customer + Manager roles

### Incremental Delivery

1. Setup + Foundational → profile service ready
2. US1 View → SCR-10 for Customer/Manager (MVP read)
3. US2 Update → SCR-11 edit flow (MVP write)
4. US3 Sync → header UX polish
5. Multi-Role → Employee/Admin routes
6. Polish → tests + docs

### Suggested MVP Scope

**Phases 1–4** (T001–T024): View + update profile for Customer and Manager — covers FR-001–005 minimum viable profile.

---

## Notes

- Package base: `com.homestay` (AGENTS.md)
- API envelope: `{ success, message, data|errors[] }` per `docs/api-spec-by-screen.md` §1
- Self-scope only: identity from JWT `sub` — no `/users/{id}` in FR-02
- Out of scope: SCR-12 change password (FR-01), admin customer mgmt (FR-09), avatar file upload
- Commit after each phase checkpoint
- Total tasks: **39** (T001–T039)
