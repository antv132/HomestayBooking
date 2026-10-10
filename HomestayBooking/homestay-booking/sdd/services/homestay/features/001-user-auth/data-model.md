# Data Model: FR-01 Authentication

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 User

## ERD

```text
users 1──* refresh_tokens
users 1──* otp_verifications
users 1──* password_reset_tokens
```

## users (extend Specification_v2 User)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| full_name | VARCHAR(255) | NOT NULL | |
| email | VARCHAR(255) | NOT NULL, UNIQUE | lower-case storage |
| phone | VARCHAR(20) | | |
| avatar_url | VARCHAR(512) | NULL | FR-02 scope |
| role | VARCHAR(20) | NOT NULL | ADMIN, MANAGER, EMPLOYEE, CUSTOMER |
| status | VARCHAR(20) | NOT NULL DEFAULT INACTIVE | INACTIVE, ACTIVE, SUSPENDED |
| password_hash | VARCHAR(255) | NULL | NULL if GOOGLE-only |
| email_verified | BOOLEAN | NOT NULL DEFAULT false | |
| auth_provider | VARCHAR(20) | NOT NULL DEFAULT LOCAL | LOCAL, GOOGLE, LINKED |
| google_id | VARCHAR(255) | NULL, UNIQUE | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**: `UNIQUE (LOWER(email))`; partial unique on `google_id`.

### Auth state transitions

```text
register        → INACTIVE, email_verified=false, role=CUSTOMER
verify OTP      → ACTIVE, email_verified=true
google complete → ACTIVE, email_verified=true (link google_id)
admin suspend   → SUSPENDED (blocks login/refresh)
```

## refresh_tokens

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| user_id | UUID | FK users |
| token_hash | VARCHAR(255) | UNIQUE; SHA-256(raw) |
| expires_at | TIMESTAMPTZ | +7 days |
| revoked_at | TIMESTAMPTZ | NULL |
| replaced_by_id | UUID | NULL; rotation chain |
| user_agent | VARCHAR(512) | optional audit |
| ip_address | VARCHAR(45) | optional audit |
| created_at | TIMESTAMPTZ | |

## otp_verifications

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| user_id | UUID | FK users, nullable pre-register |
| email | VARCHAR(255) | NOT NULL |
| otp_hash | VARCHAR(255) | NOT NULL |
| purpose | VARCHAR(20) | REGISTER, GOOGLE_LINK |
| expires_at | TIMESTAMPTZ | +15 minutes |
| consumed_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | |

**Resend quota**: COUNT rows WHERE email=? AND created_at > now()-interval '1 hour' ≥ 3 → reject.

## password_reset_tokens

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| user_id | UUID | FK users |
| token_hash | VARCHAR(255) | UNIQUE |
| expires_at | TIMESTAMPTZ | +1 hour (plan default) |
| consumed_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | |

## google_link_sessions (optional)

Signed JWT `verificationToken` (15 min) preferred over DB table — payload: `{ googleId, email, fullName }` HMAC-signed.

## Validation (application)

| Field | Rule | Spec |
|-------|------|------|
| password | min 8, letter+digit | FR-013 |
| email | format + unique | FR-001 |
| otp | 6 digits | FR-002 |
| phone | §7 validation | api-spec |

## Flyway migrations (suggested)

1. `V001__users_auth_columns.sql`
2. `V002__refresh_tokens.sql`
3. `V003__otp_verifications.sql`
4. `V004__password_reset_tokens.sql`
