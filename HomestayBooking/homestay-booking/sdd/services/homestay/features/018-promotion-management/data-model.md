# Data Model: FR-18 Promotion Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Promotion

## Scope

FR-18 **owns** `promotions` table and all read/write paths. **No** join to bookings, payments, or users.

## ERD

```text
Promotion (standalone marketing banner)

Guest ──reads──> Promotion WHERE is_active = true
Admin ──CRUD──> Promotion
```

## Table: promotions

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| title | VARCHAR(200) | NOT NULL | Display heading |
| subtitle | VARCHAR(200) | NOT NULL | Eyebrow text |
| description | TEXT | nullable | Optional body |
| cta_text | VARCHAR(100) | NOT NULL | Button label |
| cta_url | VARCHAR(500) | NOT NULL | Relative or https URL |
| color_theme | VARCHAR(20) | NOT NULL, DEFAULT 'red' | red/blue/green/purple/orange |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Active/Inactive |
| sort_order | INTEGER | NOT NULL, DEFAULT 0 | Display sequence |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `(is_active, sort_order ASC, created_at DESC)` — public list
- `(sort_order)` — admin list

## Validation Rules

| Field | Rule |
|-------|------|
| title | not blank, 2–200 chars |
| subtitle | not blank, 2–200 chars |
| description | max 1000 chars optional |
| cta_text | not blank, 2–100 chars |
| cta_url | not blank; `/...` or `http(s)://...`; no `javascript:` |
| color_theme | one of red, blue, green, purple, orange |
| sort_order | integer ≥ 0 |

## State: is_active

```text
Active (true)   → visible on public homepage
Inactive (false) → admin list only
```

No soft-delete v1 — DELETE removes row permanently (with confirmation UI).

## API DTOs

### PromotionResponse (public + admin)

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| title | string | |
| subtitle | string | |
| description | string? | |
| ctaText | string | |
| ctaUrl | string | |
| colorTheme | string | |
| isActive | boolean | omitted or always true on public endpoint |
| sortOrder | int | |
| createdAt | datetime | |
| updatedAt | datetime | |

### CreatePromotionRequest / UpdatePromotionRequest

Same writable fields: `title`, `subtitle`, `description?`, `ctaText`, `ctaUrl`, `colorTheme`, `isActive`, `sortOrder`.

### PatchPromotionActiveRequest

| Field | Type |
|-------|------|
| isActive | boolean |

## Error Codes

| Code | HTTP | When |
|------|------|------|
| PROMOTION_NOT_FOUND | 404 | Invalid id |
| PROMOTION_VALIDATION_FAILED | 400 | Invalid fields or ctaUrl |
| PROMOTION_ACCESS_DENIED | 403 | Non-Admin CRUD |

## Migration: V034__promotions_fr18.sql

Creates `promotions` + indexes. Optional seed 2–3 demo Active banners for LandingPage dev.

## Seed Example

| title | subtitle | color_theme | sort_order | is_active |
|-------|----------|-------------|------------|-----------|
| Giảm 20% mùa hè | ƯU ĐÃI MÙA HÈ | red | 0 | true |
| Combo cuối tuần | WEEKEND DEAL | blue | 1 | true |

## Cross-FR Dependencies

| FR | Relationship |
|----|--------------|
| FR-01 | JWT ADMIN for admin routes; public permitAll |
| FR-03 | LandingPage consumes public API — layout only |
| FR-17 | Promotion **not** in Administration scope |
