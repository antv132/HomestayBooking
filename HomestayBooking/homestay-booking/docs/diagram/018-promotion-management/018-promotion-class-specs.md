# 018 - Promotion Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entity

### Promotion Class
Represents a promotion or banner displayed on the landing page.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Contains `subtitle`, `title`, `description`, `ctaText`, `ctaUrl`, `colorTheme`, `isActive`, `sortOrder`.) |

---

## 2. Controllers

### AdminPromotionController Class
Handles promotion management for Admins.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listPromotions | GET `/`. List all promotions (active and inactive) with pagination for the admin dashboard. Ordered by `sortOrder` ASC, then `createdAt` DESC. Returns 200 OK. |
| 02 | createPromotion | POST `/`. Create a new promotion. Returns 201 Created. |
| 03 | updatePromotion | PUT `/{id}`. Update all fields of an existing promotion. Returns 200 OK. |
| 04 | toggleActive | PATCH `/{id}/active`. Quick toggle for `isActive` status. Returns 200 OK. |
| 05 | deletePromotion | DELETE `/{id}`. Permanently delete a promotion. Returns 204 No Content. |

### PublicPromotionController Class
Handles public promotion requests.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getActivePromotions | GET `/active`. Used by the landing page (frontend carousel). Returns only `isActive = true` promotions, sorted by `sortOrder` ASC, then `createdAt` DESC. Returns 200 OK. |

---

## 3. Services

### AdminPromotionService Class
Handles the admin CRUD operations.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | validatePromotion | Uses `PromotionValidators` to ensure: `ctaUrl` is properly formatted (e.g., must start with `/` or `http`), `colorTheme` is one of the allowed presets. Returns void. |
| 02 | createPromotion | Validates and saves a new promotion. Returns saved promotion. |
| 03 | updatePromotion | Validates and updates promotion details. Returns updated promotion. |

### PromotionService Class
Dedicated lightweight service for public reads.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getActivePromotions | Fetches active promotions to supply the landing page carousel. Returns list of promotions. |
