# 018 - Promotion Management: Database Queries

## 1. Public Landing Page Queries

### Fetch Active Banners
```sql
-- PromotionRepository.findByIsActiveTrueOrderBySortOrderAscCreatedAtDesc
SELECT p.* 
FROM promotions p
WHERE p.is_active = TRUE
ORDER BY p.sort_order ASC, p.created_at DESC;
-- Used by the frontend landing page carousel.
-- Shows active promotions, prioritizing manual sorting, then newest first.
```

---

## 2. Admin Queries

### List All Promotions (Paginated)
```sql
-- PromotionRepository.findAllByOrderBySortOrderAscCreatedAtDesc
SELECT p.*
FROM promotions p
ORDER BY p.sort_order ASC, p.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Toggle Active Status
```sql
-- AdminPromotionService.patchActive (managed by Spring Data JPA save)
UPDATE promotions
SET is_active = :isActive,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :promotionId;
```

### Insert Promotion
```sql
INSERT INTO promotions (
    id, subtitle, title, description, 
    cta_text, cta_url, color_theme, 
    is_active, sort_order, 
    created_at, updated_at
) VALUES (
    gen_random_uuid(), :subtitle, :title, :description,
    :ctaText, :ctaUrl, :colorTheme,
    :isActive, :sortOrder,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

### Delete Promotion
```sql
DELETE FROM promotions
WHERE id = :promotionId;
```
