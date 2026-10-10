# Quickstart: FR-18 Promotion Management

**Feature**: `specs/018-promotion-management` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 JWT (Admin); V034 migration applied; optional seed banners.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: Admin user; optional demo promotions from V034

## Environment

```bash
# No feature-specific env vars
```

Vite proxy: `/api/v1` → `http://localhost:8080`

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| SCR-01 Homepage banners | `/` (LandingPage) | `GET /public/promotions` |
| SCR-57 Promotion Management | `/admin/promotions` | `GET /admin/promotions` |
| SCR-58 Create/Edit (modal v1) | `/admin/promotions` (modal) | `POST/PUT /admin/promotions` |

## Smoke test: Public active promotions (no auth)

```bash
BASE=http://localhost:8080/api/v1

curl -s "$BASE/public/promotions" | jq

# Expected: success true, data[] only isActive=true items, sorted by sortOrder
```

## Smoke test: Admin create banner

```bash
ADMIN_TOKEN="<admin-jwt>"

curl -s -X POST "$BASE/admin/promotions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Giảm 20% mùa hè",
    "subtitle": "ƯU ĐÃI MÙA HÈ",
    "description": "Đặt phòng trước 30/6 để nhận ưu đãi.",
    "ctaText": "Đặt ngay →",
    "ctaUrl": "/search",
    "colorTheme": "red",
    "isActive": true,
    "sortOrder": 0
  }' | jq

# Expected: success true, data.id present
```

## Smoke test: Admin list all (includes inactive)

```bash
curl -s "$BASE/admin/promotions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.data.content | length'
```

## Smoke test: Toggle inactive — hidden from public

```bash
PROMO_ID="<uuid-from-create>"

curl -s -X PATCH "$BASE/admin/promotions/$PROMO_ID/active" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"isActive": false}' | jq

curl -s "$BASE/public/promotions" | jq '[.data[].id] | index("'"$PROMO_ID"'")'
# Expected: null (not in public list)
```

## Smoke test: Update sort order

```bash
curl -s -X PUT "$BASE/admin/promotions/$PROMO_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Giảm 20% mùa hè",
    "subtitle": "ƯU ĐÃI MÙA HÈ",
    "ctaText": "Đặt ngay →",
    "ctaUrl": "/search",
    "colorTheme": "red",
    "isActive": true,
    "sortOrder": 5
  }' | jq '.data.sortOrder'
# Expected: 5
```

## Smoke test: RBAC denial

```bash
MANAGER_TOKEN="<manager-jwt>"
CUSTOMER_TOKEN="<customer-jwt>"

curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/admin/promotions" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
# Expected: 403

curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$BASE/admin/promotions" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"x","subtitle":"y","ctaText":"z","ctaUrl":"/","colorTheme":"red"}'
# Expected: 403
```

## Smoke test: Delete promotion

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X DELETE "$BASE/admin/promotions/$PROMO_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# Expected: 204
```

## Manual UI checklist

- [ ] Homepage shows Active banners from API (or fallback when empty)
- [ ] Inactive banner not on homepage but visible in Admin list
- [ ] Admin `/admin/promotions` — create via modal with live preview
- [ ] Edit updates homepage within reload
- [ ] Delete removes banner from list and homepage
- [ ] CTA navigates to `ctaUrl`
- [ ] Manager cannot access `/admin/promotions`
- [ ] `/manager/promotions` removed or redirects

## Troubleshooting

| Issue | Check |
|-------|-------|
| Empty homepage | V034 seed or create Active promotion; fallback DEFAULT_PROMOTIONS in LandingPage |
| 403 on admin | JWT role must be ADMIN |
| Validation 400 | title, subtitle, ctaText, ctaUrl required; ctaUrl must be `/path` or https |
| Wrong sort on homepage | Verify sortOrder ASC; tie-break createdAt DESC |
