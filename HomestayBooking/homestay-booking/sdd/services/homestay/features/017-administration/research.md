# Research: FR-17 Administration

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `complaintsApi.ts`, api-spec SCR-54/56, Specification_v2 §5

## 1. Complaint Status Machine

**Decision**: Strict linear transitions only:

```text
OPEN → INVESTIGATING → RESOLVED → CLOSED
```

- `RESOLVED` requires `resolutionNotes` (min 10 chars)
- `CLOSED` is terminal — no reopen v1
- `resolvedAt` set when entering RESOLVED
- Invalid transitions return `COMPLAINT_INVALID_STATUS`

**Rationale**: Spec FR-003, US-2 acceptance scenarios; matches frontend `ComplaintStatus` enum.

**Alternatives considered**: Allow OPEN → RESOLVED skip — rejected (spec explicit workflow).

## 2. Admin Complaint API Shape

**Decision**: Extend minimal api-spec beyond `PATCH .../resolve`:

| Method | Path | Role | Notes |
|--------|------|------|-------|
| POST | `/api/v1/complaints` | CUSTOMER | Create |
| GET | `/api/v1/complaints` | CUSTOMER | Own list (paginated) |
| GET | `/api/v1/complaints/{id}` | CUSTOMER | Own detail only |
| GET | `/api/v1/admin/complaints` | ADMIN | SCR-54 list |
| GET | `/api/v1/admin/complaints/{id}` | ADMIN | Detail + customer info |
| PATCH | `/api/v1/admin/complaints/{id}/status` | ADMIN | `{ status, resolutionNotes? }` |

Deprecate frontend `/api/manager/complaints` and `/api/complaints/{id}/status`.

**Rationale**: Full workflow; api-spec `resolve` maps to `status: RESOLVED` + `resolutionNotes`.

**Alternatives considered**: Separate endpoints per transition — single PATCH sufficient.

## 3. System Settings Storage

**Decision**: Key-value table `system_settings` with unique `key`; service exposes typed `SystemSettingsResponse` DTO aggregating keys:

| Key | Type | Default | Validation |
|-----|------|---------|------------|
| DEPOSIT_PERCENTAGE | int | 40 | 10–50 |
| SYSTEM_NAME | string | "Homestay Booking" | 2–100 chars |
| SUPPORT_EMAIL | string | support@example.com | @Email |
| BANK_ACCOUNT_NUMBER | string | "" | digits/hyphens, max 32 |
| BANK_ACCOUNT_NAME | string | "" | max 200 |
| BANK_NAME | string | "" | max 200 |
| CANCEL_TIMEOUT_HOURS | int | 24 | 1–168 (P2, api-spec SCR-56) |

PUT accepts partial object; upsert each provided field; log `SETTINGS_UPDATED`.

**Rationale**: Specification_v2 SystemSetting entity; figma SCR-56 fields; api-spec `depositPercentage`, `cancelTimeoutHours`.

**Alternatives considered**: Single JSON blob column — rejected (harder audit per-key).

## 4. ActivityLog — Write vs Read Ownership

**Decision**: FR-17 **creates** `activity_logs` table + `ActivityLogService.log(userId, action, entityType, entityId, details, ip)` for **all features** to call. FR-17 **owns** Admin read API `GET /api/v1/admin/activity-logs` with filters.

New actions for FR-17:

- `COMPLAINT_CREATED`
- `COMPLAINT_STATUS_CHANGED`
- `SETTINGS_UPDATED`

Existing actions from other FR specs append over time (REVIEW_*, INVOICE_*, etc.).

**Rationale**: Spec FR-016; many FR plans reference ActivityLog writes but no read API yet.

**Alternatives considered**: Each FR owns log table — rejected (single audit stream).

## 5. ActivityLog Schema Alignment

**Decision**: Map Specification_v2 `Details` → column `details` TEXT; add optional `ip_address` VARCHAR(45). No `description` column — use `details` for human-readable text. Response DTO exposes `description` as alias of `details` for UI.

Index: `(created_at DESC)`, `(action)`, `(user_id, created_at DESC)`.

**Rationale**: Align §5 entity; support SCR-56 search/filter.

## 6. Customer Complaint Authorization

**Decision**: `ComplaintRepository` queries scoped by `userId = currentUser.id` for Customer endpoints. Admin bypasses scope. No Manager access to complaints v1.

**Rationale**: Spec FR-010/FR-011; SCR-54 Actor Admin only.

## 7. Frontend Route & Layout Correction

**Decision**:

- Move complaint pages from `pages/manager/` to `pages/admin/`
- Routes: `/admin/complaints`, `/admin/complaints/:id`, `/admin/system` (SCR-56 tabs)
- Use `AdminLayout` (create if missing, mirror ManagerLayout nav for admin)
- Customer: keep `/customer/complaints`, `/customer/complaints/create`

**Rationale**: screen.md SCR-54 Actor Admin; current `/manager/complaints` incorrect.

## 8. Content Moderation Tab (FR-14)

**Decision**: SCR-56 tab **does not** duplicate Review APIs — frontend `ContentModerationTab.tsx` calls FR-14 `GET/PATCH /api/v1/admin/reviews/**`. FR-17 task only wires tab + routing.

**Rationale**: Spec boundary FR-009; FR-14 contract already defines admin endpoints.

## 9. FR-09 Customer Directory Boundary

**Decision**: FR-17 plan includes **navigation link** from Admin sidebar to `/admin/customers` (FR-09 SCR-51) but **zero** implementation of customer status APIs in FR-17 tasks.

**Rationale**: Spec FR-013; avoids duplicate with `specs/009-customer-management`.

## 10. Notification on Status Change (P2)

**Decision**: v1 **no** automatic email on complaint status change. Optional P2: publish `COMPLAINT_STATUS_CHANGED` outbox event for FR-15.

**Rationale**: Spec assumption — optional P2.

## 11. Deposit Percentage Consumers

**Decision**: `SystemSettingService.getInt("DEPOSIT_PERCENTAGE")` injected into FR-04 booking checkout and FR-12 payment amount calculation. FR-17 seeds default; consumers read at runtime (new bookings only per spec edge case).

**Rationale**: Spec edge case — in-flight bookings keep snapshot at creation (FR-04 responsibility to store `depositPercentageAtBooking` optional P2).

## 12. Validation Rules

**Decision**:

- `subject`: 5–200 chars, not blank
- `description`: 20–2000 chars, not blank
- `resolutionNotes`: required when `status = RESOLVED`, 10–2000 chars

**Rationale**: Spec assumptions; align maintenance ticket patterns.
