# Research: FR-18 Promotion Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `PromotionMgmtPage.tsx`, `publicApi.ts`, api-spec SCR-57/58

## 1. Promotion Model vs api-spec Payload

**Decision**: Implement **Specification §5 Promotion** entity fields (`title`, `subtitle`, `description`, `ctaText`, `ctaUrl`, `colorTheme`, `isActive`, `sortOrder`). **Reject** api-spec SCR-58 `code`, `discountPercent`, `attachments` for v1.

**Rationale**: Spec FR-013 no booking discount; frontend `PromotionPayload` already matches entity; api-spec appears legacy/alternate promo-code feature.

**Alternatives considered**: Dual model (banner + promo code) — rejected YAGNI v1.

## 2. Public vs Admin API Split

**Decision**:

| Method | Path | Role | Behavior |
|--------|------|------|----------|
| GET | `/api/v1/public/promotions` | permitAll | `isActive=true`, ORDER BY `sort_order ASC`, `created_at DESC` |
| GET | `/api/v1/admin/promotions` | ADMIN | all rows, same default sort |
| POST | `/api/v1/admin/promotions` | ADMIN | create |
| PUT | `/api/v1/admin/promotions/{id}` | ADMIN | full update |
| PATCH | `/api/v1/admin/promotions/{id}/active` | ADMIN | `{ isActive: boolean }` quick toggle |
| DELETE | `/api/v1/admin/promotions/{id}` | ADMIN | hard delete |

**Rationale**: Clear Guest vs Admin contract; toggle without full PUT for US-4.

**Alternatives considered**: Single `/promotions` with role filter — rejected (leaks inactive to Guest risk).

## 3. colorTheme Palette

**Decision**: Enum `ColorTheme`: `RED`, `BLUE`, `GREEN`, `PURPLE`, `ORANGE` stored lowercase in DB (`red`, `blue`, …) matching frontend `COLOR_OPTIONS`.

**Rationale**: `PromotionMgmtPage.tsx` gradients; invalid theme → default `red` on read.

**Alternatives considered**: Free-text hex color — rejected; spec uses ColorTheme string.

## 4. ctaUrl Validation

**Decision**: Accept:
- Relative paths starting with `/` (e.g. `/search`, `/rooms`)
- Absolute `http://` or `https://` URLs

Reject blank, `javascript:`, other schemes.

**Rationale**: Spec US-3; security baseline for marketing links.

## 5. sortOrder Strategy

**Decision**: Integer `sort_order` NOT NULL DEFAULT 0. Admin sets on create/edit. Public sort: `sort_order ASC`, tie-break `created_at DESC`.

Optional P2: `PATCH /admin/promotions/reorder` bulk — v1 edit form field sufficient.

**Rationale**: Spec US-4; matches frontend form `sortOrder` input.

## 6. SCR-58 UI Pattern

**Decision**: v1 **keep modal** on SCR-57 (`PromotionMgmtPage`) — satisfies US5 acceptance ("modal hoặc dedicated page"). Optional P2 route `/admin/promotions/new`, `/admin/promotions/:id/edit`.

**Rationale**: Existing UI complete with preview; faster delivery.

## 7. Admin Actor & Route Migration

**Decision**: Move page to `pages/admin/PromotionMgmtPage.tsx`, use `AdminLayout`, route `/admin/promotions`, remove Manager route and ManagerLayout nav item.

**Rationale**: screen.md SCR-57 Actor Admin; entity-ui-mapping Admin only for CRUD.

## 8. Landing Page Integration (FR-03)

**Decision**: `publicApi.fetchPromotions()` → `GET /api/v1/public/promotions`. `LandingPage` keeps `DEFAULT_PROMOTIONS` fallback when API returns empty array (SC-007).

**Rationale**: Spec edge case zero Active; FR-03 owns grid layout unchanged.

## 9. Image / Attachments

**Decision**: **No** `image_url` or file upload v1. Visual identity via `colorTheme` gradient only.

**Rationale**: Spec edge case; entity §5 has no image field; frontend has no upload.

## 10. Pagination Admin List

**Decision**: v1 return full list in `data` array (or `{ content: [] }` envelope match api-spec) if count < 100; add `page`/`size` query P2 if needed.

**Rationale**: Low volume marketing banners; api-spec shows `{ content: [...] }` without page params.

## 11. Default on Create

**Decision**: `isActive = true`, `sortOrder = max(sortOrder)+1` or 0 if first, `ctaText` default "Đặt ngay →", `ctaUrl` default `/search`, `colorTheme` default `red`.

**Rationale**: Align `EMPTY` form in `PromotionMgmtPage.tsx`.

## 12. ActivityLog (P2)

**Decision**: Optional `PROMOTION_CREATED`, `PROMOTION_UPDATED`, `PROMOTION_DELETED` via FR-17 `ActivityLogService` — defer v1.

**Rationale**: Spec assumption optional P2.
