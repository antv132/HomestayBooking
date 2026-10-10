# Data Model: FR-02 User Profile

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `specs/001-user-auth/data-model.md`, `docs/Specification_v2.md` §5 User

## Scope

FR-02 **does not** add tables. It reads and partially updates the existing `users` entity from FR-01.

## Entity: User (profile subset)

| Column | Type | FR-02 Access | Notes |
|--------|------|--------------|-------|
| id | UUID | READ | From JWT; never from client |
| full_name | VARCHAR(255) | READ, UPDATE | @NotBlank on update |
| email | VARCHAR(255) | READ only | Login identifier |
| phone | VARCHAR(20) | READ, UPDATE | Optional; VN format if set |
| avatar_url | VARCHAR(512) | READ, UPDATE | Optional HTTPS URL |
| role | VARCHAR(20) | READ only | ADMIN, MANAGER, EMPLOYEE, CUSTOMER |
| status | VARCHAR(20) | READ only | INACTIVE, ACTIVE, SUSPENDED |
| password_hash | VARCHAR(255) | **HIDDEN** | Never in response |
| created_at | TIMESTAMPTZ | READ | "Member since" UI |
| updated_at | TIMESTAMPTZ | READ, AUTO-SET | Set on every profile update |

## Validation Rules

| Field | Rule | Error (§7) |
|-------|------|------------|
| fullName | NOT NULL, not blank after trim, max 255 | Required field missing |
| phone | NULL allowed; if set: `^(\+84\|0)[0-9]{9,10}$` | Invalid phone number |
| avatarUrl | NULL allowed; if set: max 512, must start with `http://` or `https://` | Validation failed |

## Update Semantics

```text
PUT /api/v1/users/me
  → Load user by JWT sub
  → Apply non-null fields from UpdateProfileRequest
  → Ignore email, role, status, password fields
  → Set updated_at = now()
  → Return UserProfileResponse
```

**Partial update**: Omitted JSON fields leave DB value unchanged. Explicit `null` for `phone` or `avatarUrl` clears the field.

## DTOs

### UserProfileResponse

```json
{
  "id": "uuid",
  "fullName": "string",
  "email": "string",
  "phone": "string | null",
  "avatarUrl": "string | null",
  "role": "CUSTOMER | EMPLOYEE | MANAGER | ADMIN",
  "status": "ACTIVE | INACTIVE | SUSPENDED",
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

### UpdateProfileRequest

```json
{
  "fullName": "string (optional but if sent must be non-blank)",
  "phone": "string | null (optional)",
  "avatarUrl": "string | null (optional)"
}
```

## RBAC Matrix (FR-02)

| Role | GET /users/me | PUT /users/me |
|------|---------------|---------------|
| CUSTOMER | Own record | Own record |
| EMPLOYEE | Own record | Own record |
| MANAGER | Own record | Own record |
| ADMIN | Own record | Own record |

**Note**: Manager Admin CRUD on *other* users belongs to FR-09 (Employee/Customer management), not FR-02.

## State Transitions

FR-02 does **not** change `status` or `role`. No profile-specific state machine.

## Audit

- `updated_at` auto-updated on successful PUT (FR-008).
- Optional future: audit_log entry `PROFILE_UPDATED` — out of scope unless global audit FR requires it.
