# Tasks: FR-18 Promotion Management

**Input**: Design documents from `specs/018-promotion-management/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/promotion-api.yaml, quickstart.md

**Phụ thuộc**: FR-01 (JWT RBAC Admin; public permitAll); FR-03 (Landing SCR-01 layout — FR-18 owns Promotion data). **Ranh giới**: FR-17 không bao gồm Promotion; không promo code/discount booking v1; không image upload v1; Manager `/manager/promotions` sai actor.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase H.

**Organization**: Tasks grouped by user story (US1–US5) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US5 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/018-promotion-management/contracts/promotion-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Migration, auth blocker, dev proxy

- [X] T001 Verify FR-01 JWT auth with **ADMIN** role per `specs/001-user-auth/quickstart.md` (blocker)
- [X] T002 Create Flyway `Backend/src/main/resources/db/migration/V034__promotions_fr18.sql` — table `promotions`, indexes, optional seed demo banners per `data-model.md`
- [X] T003 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [X] T004 [P] Add seed INSERT comments in V034 — 2 Active demo banners for LandingPage dev

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entity, repository, DTOs, service skeleton, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T005 [P] Create `ColorTheme.java` enum in `Backend/src/main/java/com/homestay/enums/ColorTheme.java` — red, blue, green, purple, orange
- [X] T006 [P] Create `Promotion.java` entity in `Backend/src/main/java/com/homestay/entities/Promotion.java` per `data-model.md`
- [X] T007 [P] Create `PromotionRepository.java` in `Backend/src/main/java/com/homestay/repositories/PromotionRepository.java` — `findByIsActiveTrueOrderBySortOrderAscCreatedAtDesc`, `findAllByOrderBySortOrderAscCreatedAtDesc`
- [X] T008 [P] Create `PromotionResponse.java` in `Backend/src/main/java/com/homestay/dtos/promotion/PromotionResponse.java`
- [X] T009 [P] Create `CreatePromotionRequest.java` and `UpdatePromotionRequest.java` in `Backend/src/main/java/com/homestay/dtos/promotion/` per `contracts/promotion-api.yaml`
- [X] T010 [P] Create `PatchPromotionActiveRequest.java` in `Backend/src/main/java/com/homestay/dtos/promotion/PatchPromotionActiveRequest.java`
- [X] T011 Create `PromotionService.java` skeleton in `Backend/src/main/java/com/homestay/services/PromotionService.java` — map entity ↔ DTO
- [X] T012 Add `ctaUrl` and `colorTheme` validation helpers in `PromotionService.java` — reject `javascript:`; palette enum
- [X] T013 Register `permitAll` for `GET /api/v1/public/promotions/**` and `hasRole('ADMIN')` for `/api/v1/admin/promotions/**` in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Khách xem banner trên trang chủ (Priority: P1) 🎯 MVP

**Goal**: SCR-01 public Active promotions sorted by sortOrder on LandingPage

**Independent Test**: `GET /public/promotions` returns only Active; homepage renders banners; Inactive excluded

### Implementation

- [X] T014 [US1] Implement `listActivePublic()` — `isActive=true`, ORDER BY `sortOrder ASC`, `createdAt DESC` in `PromotionService.java`
- [X] T015 [US1] Create `PublicPromotionController.java` with `GET /api/v1/public/promotions` in `Backend/src/main/java/com/homestay/controllers/PublicPromotionController.java`
- [X] T016 [P] [US1] Migrate `fetchPromotions()` to `GET /api/v1/public/promotions` in `Frontend/src/api/publicApi.ts`
- [X] T017 [US1] Wire live `fetchPromotions` on `Frontend/src/pages/public/LandingPage.tsx` — keep `DEFAULT_PROMOTIONS` fallback when API empty (SC-007)
- [X] T018 [US1] Verify CTA click navigates to `ctaUrl` on `LandingPage.tsx`

**Checkpoint**: US1 MVP — Guest homepage banners testable via `quickstart.md` public curl

---

## Phase 4: User Story 2 — Admin xem danh sách banner (Priority: P1)

**Goal**: SCR-57 Admin list all promotions (Active + Inactive) with Add button

**Independent Test**: Admin `GET /admin/promotions` returns all; Manager/Customer 403; empty state when no banners

### Implementation

- [X] T019 [US2] Implement `listAllAdmin()` in `PromotionService.java`
- [X] T020 [US2] Create `AdminPromotionController.java` with `GET /api/v1/admin/promotions` returning `{ content: [...] }` in `Backend/src/main/java/com/homestay/controllers/AdminPromotionController.java`
- [X] T021 [P] [US2] Create `promotionApi.ts` with `getAll()` → `/api/v1/admin/promotions` in `Frontend/src/api/promotionApi.ts` — extract from `managerApi.ts`
- [X] T022 [US2] Move `PromotionMgmtPage.tsx` from `Frontend/src/pages/manager/` to `Frontend/src/pages/admin/PromotionMgmtPage.tsx`
- [X] T023 [US2] Replace `ManagerLayout` with `AdminLayout` (create `AdminLayout.tsx` if missing) on `PromotionMgmtPage.tsx`
- [X] T024 [US2] Wire admin list grid/cards to `promotionApi.getAll()` on `PromotionMgmtPage.tsx`
- [X] T025 [US2] Register `/admin/promotions` with `ProtectedRoute role="ADMIN"` in `Frontend/src/App.tsx`
- [X] T026 [US2] Add empty state + **Add Promotion** button on `PromotionMgmtPage.tsx` per SCR-57

**Checkpoint**: US2 testable — Admin SCR-57 list

---

## Phase 5: User Story 3 — Admin tạo và chỉnh sửa banner (Priority: P1)

**Goal**: SCR-58 create/edit via modal with preview and validation

**Independent Test**: Admin POST creates banner → appears on list and homepage if Active; PUT updates fields

### Implementation

- [X] T027 [US3] Implement `createPromotion(request)` with defaults (`isActive=true`, `sortOrder`) in `PromotionService.java`
- [X] T028 [US3] Implement `updatePromotion(id, request)` in `PromotionService.java`
- [X] T029 [US3] Add `POST /api/v1/admin/promotions` and `PUT /api/v1/admin/promotions/{id}` in `AdminPromotionController.java`
- [X] T030 [P] [US3] Add `create()` and `update()` to `Frontend/src/api/promotionApi.ts`
- [X] T031 [US3] Wire modal **create** flow (`openNew` → `handleSave`) to `promotionApi.create` on `PromotionMgmtPage.tsx`
- [X] T032 [US3] Wire modal **edit** flow (`openEdit` → `handleSave`) to `promotionApi.update` on `PromotionMgmtPage.tsx`
- [X] T033 [US3] Show validation errors for required `title`, `subtitle`, `ctaText`, `ctaUrl` on `PromotionMgmtPage.tsx`
- [X] T034 [US3] Verify `BannerPreview` component reflects form state on `PromotionMgmtPage.tsx`

**Checkpoint**: US3 testable — create/edit banner end-to-end

---

## Phase 6: User Story 4 — Admin xóa, bật/tắt và sắp xếp (Priority: P1)

**Goal**: Delete, toggle Active/Inactive, sortOrder control

**Independent Test**: Inactive hidden from public; sortOrder change reflects on homepage; delete removes everywhere

### Implementation

- [X] T035 [US4] Implement `deletePromotion(id)` in `PromotionService.java`
- [X] T036 [US4] Implement `patchActive(id, isActive)` in `PromotionService.java`
- [X] T037 [US4] Add `DELETE /api/v1/admin/promotions/{id}` and `PATCH /api/v1/admin/promotions/{id}/active` in `AdminPromotionController.java`
- [X] T038 [P] [US4] Add `delete()` and `patchActive()` to `Frontend/src/api/promotionApi.ts`
- [X] T039 [US4] Wire delete confirmation + `promotionApi.delete` on `PromotionMgmtPage.tsx`
- [X] T040 [US4] Add quick **Active/Inactive** toggle per card calling `patchActive` on `PromotionMgmtPage.tsx`
- [X] T041 [US4] Expose `sortOrder` input in modal form and display on list cards on `PromotionMgmtPage.tsx`

**Checkpoint**: US4 testable — toggle + sort + delete per `quickstart.md`

---

## Phase 7: User Story 5 — Admin điều hướng SCR-57 ↔ SCR-58 (Priority: P2)

**Goal**: List → modal form → save/cancel → back to list; admin nav; remove Manager routes

**Independent Test**: Add opens modal; Save closes and refreshes list; Cancel discards; `/manager/promotions` removed

### Implementation

- [X] T042 [US5] Ensure modal open/close resets `editing` state on `PromotionMgmtPage.tsx`
- [X] T043 [US5] Add **Promotions** nav link to `/admin/promotions` in `Frontend/src/layouts/AdminLayout.tsx`
- [X] T044 [US5] Remove `/manager/promotions` route from `Frontend/src/App.tsx`
- [X] T045 [P] [US5] Remove **Banner khuyến mãi** nav item from `Frontend/src/layouts/ManagerLayout.tsx`
- [X] T046 [P] [US5] Remove `promotionApi` exports from `Frontend/src/api/managerApi.ts` — update imports to `promotionApi.ts`

**Checkpoint**: US5 testable — admin-only navigation complete

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Tests, quickstart validation, cleanup

- [X] T047 [P] Unit test `ctaUrl` validation and `sortOrder` tie-break in `Backend/src/test/java/com/homestay/unit/PromotionServiceTest.java`
- [X] T048 Integration test public active-only + Admin CRUD RBAC in `Backend/src/test/java/com/homestay/integration/PromotionControllerIT.java`
- [X] T049 [P] Assert Inactive promotion absent from `GET /public/promotions` in `PromotionControllerIT.java`
- [X] T050 [P] Assert Manager/Customer 403 on `POST /admin/promotions` in `PromotionControllerIT.java`
- [X] T051 Run curl smoke tests in `specs/018-promotion-management/quickstart.md` and fix gaps
- [X] T052 [P] Verify `LandingPage.tsx` graceful fallback when zero Active promotions (SC-007)
- [X] T053 [P] Verify admin route `/admin/promotions` and no `/manager/promotions` in `Frontend/src/App.tsx`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-01 auth (blocker)
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP** public read + LandingPage
- **US2 (P1)**: After Foundational; shares `PromotionService` with US1
- **US3 (P1)**: After US2 (list page exists for create/edit refresh)
- **US4 (P1)**: After US3 (shared AdminPromotionController + modal)
- **US5 (P2)**: After US2–US4 (page and APIs stable)
- **Polish (Phase 8)**: After desired user stories complete

### User Story Dependencies

```text
Foundational → US1 (public + Landing) — MVP
            → US2 (admin list)
            → US3 (create/edit) → US4 (delete/toggle/sort)
            → US5 (nav + route cleanup)
```

### Parallel Opportunities

**Phase 1**: T003, T004 [P]  
**Phase 2** (after T011): T005–T010 all [P]  
**US1**: T016 parallel with T014–T015  
**US2**: T021 parallel with T019–T020  
**US3**: T030 parallel with T027–T029  
**US4**: T038 parallel with T035–T037  
**US5**: T045, T046 [P]  
**Polish**: T047, T049, T050, T052, T053 all [P]

### Parallel Example: User Story 1

```bash
T014–T015 Backend public list + controller
T016 publicApi.ts migration (parallel when contract stable)
T017–T018 LandingPage wire + CTA verify
```

### Parallel Example: User Story 3

```bash
T027–T029 PromotionService create/update + controller POST/PUT
T030 promotionApi.ts create/update methods
T031–T034 Modal flows + validation + preview
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T004)
2. Complete Phase 2: Foundational (T005–T013)
3. Complete Phase 3: User Story 1 (T014–T018)
4. **STOP and VALIDATE**: `GET /public/promotions` + homepage banners per `quickstart.md`
5. Demo SCR-01 LandingPage with live promotions

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Public homepage banners (MVP)
3. US2 → Admin SCR-57 list
4. US3 → Create/edit modal SCR-58
5. US4 → Delete, toggle, sortOrder
6. US5 → Admin nav + remove Manager routes
7. Polish → Tests + quickstart

### Parallel Team Strategy

With multiple developers after Foundational:

- **Developer A**: US1 (public API + LandingPage)
- **Developer B**: US2 + US3 (admin list + create/edit)
- **Developer C**: US4 + US5 (lifecycle + nav cleanup)

---

## Notes

- api-spec `code`, `discountPercent`, `attachments` — **not implemented** v1
- Image upload out of scope — `colorTheme` gradient only
- FR-03 owns Landing layout; FR-18 owns data only
- `AdminLayout.tsx` may need creation if FR-17 not yet implemented — create minimal admin shell for promotions


## Implementation notes (2026-07-23)

- Package layout uses `entity/controller/service/repository/dto` (not entities/controllers).
- T002/T004: skipped Flyway � project uses JPA `ddl-auto`; `promotions` table already exists.
- Admin CRUD in `AdminPromotionService`; public read in `PromotionService`.
- Manager `/api/manager/promotions` and FE route removed; Admin `/api/v1/admin/promotions` (+ legacy `/api/admin/promotions`).
- T048?T050: `AdminPromotionRbacTest` (401), `AdminPromotionControllerTest` (DELETE 204), `AdminPromotionServiceTest` (http/https CTA), `PublicPromotionControllerTest`; `@PreAuthorize` on Admin controller for Manager/Customer.

