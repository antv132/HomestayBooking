# Data Model: FR-17 Administration

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Complaint, SystemSetting, ActivityLog

## Scope

FR-17 **owns**:

- `complaints` — Customer submit + Admin lifecycle
- `system_settings` — platform configuration key-value store
- `activity_logs` — append-only audit (write via `ActivityLogService`; read API Admin)

**Does not own**: `users` (FR-01/09), `reviews` (FR-14), `outbox_events` (FR-15), `promotions` (FR-18).

## ERD

```text
User (CUSTOMER) 1──* Complaint

User (ADMIN) ──updates──> SystemSetting (by key)
User (ADMIN) ──updates──> Complaint.status

ActivityLog *──1 User (actor, nullable for system)

Complaint ──logged──> ActivityLog (COMPLAINT_*)
SystemSetting ──logged──> ActivityLog (SETTINGS_UPDATED)
```

## Table: complaints

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| user_id | UUID | FK users NOT NULL | Submitter (Customer) |
| subject | VARCHAR(200) | NOT NULL | 5–200 chars |
| description | TEXT | NOT NULL | 20–2000 chars |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'OPEN' | OPEN, INVESTIGATING, RESOLVED, CLOSED |
| resolution_notes | TEXT | nullable | Required when status=RESOLVED |
| resolved_at | TIMESTAMPTZ | nullable | Set on RESOLVED |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `(user_id, created_at DESC)` — Customer my complaints
- `(status, created_at DESC)` — Admin filter
- `(created_at DESC)` — Admin list default sort

## Table: system_settings

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| key | VARCHAR(64) | UNIQUE NOT NULL | e.g. DEPOSIT_PERCENTAGE |
| value | TEXT | NOT NULL | Stored as string; typed in service |
| description | VARCHAR(255) | nullable | Admin UI hint |
| updated_by | UUID | FK users nullable | Last Admin editor |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Seed keys** (V033):

| key | default value |
|-----|---------------|
| DEPOSIT_PERCENTAGE | 40 |
| SYSTEM_NAME | Homestay Booking |
| SUPPORT_EMAIL | support@example.com |
| BANK_ACCOUNT_NUMBER | |
| BANK_ACCOUNT_NAME | |
| BANK_NAME | |
| CANCEL_TIMEOUT_HOURS | 24 |

## Table: activity_logs

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| user_id | UUID | FK users nullable | Actor |
| action | VARCHAR(64) | NOT NULL | e.g. COMPLAINT_CREATED |
| entity_type | VARCHAR(64) | nullable | COMPLAINT, SYSTEM_SETTING, REVIEW |
| entity_id | UUID | nullable | Related entity |
| details | TEXT | nullable | Human-readable description |
| ip_address | VARCHAR(45) | nullable | IPv4/IPv6 |
| created_at | TIMESTAMPTZ | NOT NULL | Immutable |

**Indexes**:

- `(created_at DESC)` — default list
- `(action, created_at DESC)` — filter by action
- `(user_id, created_at DESC)` — filter by user

**Retention**: 12 months minimum display; no delete API v1.

## State Transitions: Complaint.status

```text
CREATE → OPEN

OPEN → INVESTIGATING
INVESTIGATING → RESOLVED (resolution_notes required, set resolved_at)
RESOLVED → CLOSED

CLOSED → (terminal)
Any other transition → COMPLAINT_INVALID_STATUS
```

## API DTOs

### ComplaintSummaryResponse

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| subject | string | |
| status | ComplaintStatus | |
| customerName | string | Admin list only |
| createdAt | datetime | |

### ComplaintDetailResponse

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| subject | string | |
| description | string | |
| status | ComplaintStatus | |
| resolutionNotes | string? | |
| resolvedAt | datetime? | |
| customer | { id, fullName, email }? | Admin only |
| createdAt | datetime | |
| updatedAt | datetime | |

### SystemSettingsResponse

| Field | Type | Source key |
|-------|------|------------|
| depositPercentage | int | DEPOSIT_PERCENTAGE |
| systemName | string | SYSTEM_NAME |
| supportEmail | string | SUPPORT_EMAIL |
| bankAccountNumber | string | BANK_ACCOUNT_NUMBER |
| bankAccountName | string | BANK_ACCOUNT_NAME |
| bankName | string | BANK_NAME |
| cancelTimeoutHours | int | CANCEL_TIMEOUT_HOURS |
| updatedAt | datetime | max(updated_at) across keys |

### ActivityLogResponse

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| userId | UUID? | |
| userEmail | string? | join users |
| userFullName | string? | join users |
| action | string | |
| entityType | string? | |
| entityId | UUID? | |
| description | string | maps from details |
| createdAt | datetime | |

## ActivityLog Actions (FR-17)

| Action | Trigger |
|--------|---------|
| COMPLAINT_CREATED | Customer POST complaint |
| COMPLAINT_STATUS_CHANGED | Admin PATCH status |
| SETTINGS_UPDATED | Admin PUT settings |

## Error Codes

| Code | HTTP | When |
|------|------|------|
| COMPLAINT_NOT_FOUND | 404 | Invalid id |
| COMPLAINT_ACCESS_DENIED | 403 | Customer views other's complaint |
| COMPLAINT_INVALID_STATUS | 400 | Illegal transition |
| COMPLAINT_RESOLUTION_REQUIRED | 400 | RESOLVED without notes |
| SETTINGS_VALIDATION_FAILED | 400 | Invalid deposit %, email, etc. |

## Migration: V033__administration_fr17.sql

Creates `complaints`, `system_settings`, `activity_logs` + indexes + seed `system_settings` rows.

## Cross-FR Dependencies

| FR | Relationship |
|----|--------------|
| FR-01 | users FK; JWT roles |
| FR-09 | Customer Directory — nav only |
| FR-14 | Content Moderation tab consumes `/admin/reviews` |
| FR-15 | Optional Outbox tab; optional complaint notification P2 |
| FR-04/12 | Read DEPOSIT_PERCENTAGE from SystemSettingService |
