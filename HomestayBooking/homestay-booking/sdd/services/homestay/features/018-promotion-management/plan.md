# Implementation Plan: FR-18 Promotion Management

**Branch**: `020-promotion-management` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/018-promotion-management/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-18, §5 Promotion, §8 Administration), `docs/api-spec-by-screen.md` (SCR-57/58), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.16, frontend `PromotionMgmtPage.tsx`, `managerApi.ts` promotionApi, `publicApi.ts`, `LandingPage.tsx`

**Phụ thuộc**: FR-01 (RBAC Admin; public permitAll); FR-03 (Landing SCR-01 layout — FR-18 owns Promotion data + public endpoint). **Ranh giới**: FR-17 **không** bao gồm Promotion; FR-18 owns banner CRUD + public read; **không** promo code/discount booking v1; **không** image upload v1; Manager `/manager/promotions` **sai actor** → Admin SCR-57/58.

## Summary

Triển khai **FR-18 Promotion Management**: Flyway **V034** bảng `promotions`; REST public `GET /api/v1/public/promotions` (Active only, sortOrder); Admin CRUD `GET/POST/PUT/DELETE /api/v1/admin/promotions` + `PATCH .../active`; frontend migrate `promotionApi` từ `managerApi.ts` → `promotionApi.ts` + `/api/v1/**`; move `PromotionMgmtPage.tsx` → `pages/admin/` với **AdminLayout**; wire `LandingPage` `fetchPromotions` → v1 public API; giữ modal form SCR-58 trên SCR-57 (US5 P2).

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Bean Validation; Vite, Axios, React Router  
**Storage**: SQL Server — `promotions` (V034); optional seed demo banners  
**Testing**: JUnit 5 + Mockito; `PromotionServiceTest` validation + sort; `PromotionControllerIT` RBAC + public active filter  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Public promotions p95 < 500ms; Admin list p95 < 2s  
**Constraints**: Admin-only CRUD; public Active-only; colorTheme palette enum; no discount codes v1  
**Scale/Scope**: ~6 REST endpoints; 5 user stories; SCR-01 + SCR-57/58

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → PromotionService → repository |
| DTO + Bean Validation | PASS | Create/UpdatePromotionRequest, ctaUrl validation |
| Security-first (RBAC) | PASS | Admin CRUD; public read permitAll |
| No secrets in code | PASS | N/A |
| Test coverage ≥80% | PASS | RBAC + active filter IT |
| Standard API envelope | PASS | `{ success, message, data }` |
| Audit log | PASS | Optional PROMOTION_* P2 — omit v1 |
| Pagination | PASS | Admin list paginated optional v1 — return all if <100 |

**Post-design re-check**: PASS — no booking discount integration; FR-03 consumes public API only.

## Project Structure

### Documentation (this feature)

```text
specs/018-promotion-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/promotion-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── PublicPromotionController.java       # GET /public/promotions — Guest SCR-01
│   └── AdminPromotionController.java        # SCR-57/58 CRUD
├── dtos/promotion/
│   ├── PromotionResponse.java
│   ├── CreatePromotionRequest.java
│   ├── UpdatePromotionRequest.java
│   └── PatchPromotionActiveRequest.java
├── entities/
│   └── Promotion.java
├── enums/
│   └── ColorTheme.java
├── repositories/
│   └── PromotionRepository.java
├── services/
│   └── PromotionService.java
└── configs/
    └── SecurityConfig.java                  # permitAll /public/promotions; ADMIN /admin/promotions/**

Backend/src/main/resources/db/migration/
└── V034__promotions_fr18.sql

Backend/src/test/java/com/homestay/
├── unit/PromotionServiceTest.java
└── integration/PromotionControllerIT.java

Frontend/src/
├── api/
│   ├── promotionApi.ts                      # new — extract from managerApi.ts
│   └── publicApi.ts                         # fetchPromotions → /api/v1/public/promotions
├── pages/admin/
│   └── PromotionMgmtPage.tsx                # move from manager/ — SCR-57/58 modal
├── pages/public/
│   └── LandingPage.tsx                      # wire live promotions (exists)
└── App.tsx                                  # /admin/promotions; remove /manager/promotions
```

**Structure Decision**: FR-18 **owns** `promotions` table and all promotion REST. **Consumes** FR-03 for Landing layout only. **Does not** touch FR-04 booking pricing.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | V034 migration + seed demo banners | data-model.md |
| **B** | Entity, repo, DTOs, `PromotionService` | Foundational |
| **C** | `PublicPromotionController` active-only list | US-1 |
| **D** | `AdminPromotionController` CRUD + toggle active | US-2, US-3, US-4 |
| **E** | Frontend `promotionApi.ts` + migrate PromotionMgmtPage to admin | US-2–US-5 |
| **F** | `publicApi.ts` + `LandingPage.tsx` live wire | US-1 |
| **G** | Route/nav cleanup manager → admin | US-5 |
| **H** | Tests + quickstart | SC-001–SC-007 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| Admin API | `/api/manager/promotions` | `/api/v1/admin/promotions` |
| Public API | `/api/public/promotions` | `/api/v1/public/promotions` |
| Actor/layout | ManagerLayout `/manager/promotions` | AdminLayout `/admin/promotions` |
| promotionApi | embedded in `managerApi.ts` | `promotionApi.ts` standalone |
| SCR-58 | modal on same page | keep modal v1 (US5 acceptable) |
| LandingPage | `fetchPromotions` + DEFAULT fallback | live API; fallback when empty |
| Promo code | api-spec `code`, `discountPercent` | **not implemented** — banner fields only |

## Risks

| Risk | Mitigation |
|------|------------|
| api-spec SCR-58 payload mismatch | Contract uses Specification §5 entity fields; document deprecation of code/discount |
| Manager nav still shows promotions | Phase G: remove from ManagerLayout; add AdminLayout link |
| FR-03 Landing breaks on empty API | Keep DEFAULT_PROMOTIONS fallback in LandingPage |
| sortOrder conflicts | Tie-break `createdAt DESC` per spec assumption |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/promotion-api.yaml](./contracts/promotion-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
