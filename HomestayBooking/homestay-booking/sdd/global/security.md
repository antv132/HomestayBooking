# security.md — Security Policies & Compliance
# Version: 1.0 | Updated: 2026-07-23
# Purpose: Security policies, compliance requirements
# Related: `sdd/global/system-architecture.md`, `sdd/global/data-governance.md`, `CONSTITUTION.md` (Article 3), `sdd/global/constitution.md`, `sdd/services/homestay/features/001-user-auth/`

---

## 1. Scope

Applies to Homestay Booking System (SPA + Spring Boot API + SQL Server + VNPay/Google/SMTP).

**Examples of rules this doc owns:** HTTPS only (production), OWASP Top 10 controls, privacy/GDPR-aligned handling of personal data, secrets, authz.

Implementation details (filter classes, matcher lists) live in code under `com.homestay.security` — this file is the **policy** source of truth.

---

## 2. Transport & session

| Policy | Requirement |
|--------|-------------|
| **HTTPS only (production)** | TLS terminate at reverse proxy / platform; no plaintext API in prod |
| **DEV** | HTTP localhost allowed for local Vite ↔ API |
| **Auth model** | Stateless JWT access + refresh — no cookie session as primary |
| **CSRF** | Disabled for JWT bearer APIs; do not reintroduce cookie-session auth without CSRF plan |
| **CORS** | Explicit origin whitelist; **no** `*` in production |

Header: `Authorization: Bearer <access_token>`.

---

## 3. Authentication & passwords

| Policy | Requirement |
|--------|-------------|
| Password hash | BCrypt **cost ≥ 12** — never plain text, MD5, unsalted SHA |
| Access token | Short-lived (config default 15 min); subject = user UUID |
| Refresh token | Server-stored; revocable on logout / suspend / password reset |
| Google OAuth | No auto-link if local email not verified |
| Account status | `SUSPENDED` → all login paths rejected; filter requires `ACTIVE` |
| Error messages | Do not reveal whether email or password was wrong |

Feature rules: `sdd/services/homestay/features/001-user-auth/`.

---

## 4. Authorization (RBAC + tenancy)

| Role | Access principle |
|------|------------------|
| Guest | Public read only |
| `CUSTOMER` | Own bookings / payments / contracts |
| `EMPLOYEE` | Assigned property operations |
| `MANAGER` | Assigned properties only (`ManagerPropertyAssignment` ACTIVE) |
| `ADMIN` | Cross-property admin APIs |

- Enforce with `@PreAuthorize` **and** service-level ownership / property-scope checks.
- Never trust client-supplied `propertyId` / `customerId` alone.
- NEVER disable auth, skip authz, or bypass booking/payment business rules.

---

## 5. Secrets & configuration

| Secret | Rule |
|--------|------|
| JWT secret, DB password, VNPay keys, Google client secret, SMTP | Env / secure config **only** |
| Git | Never commit `.env`, real keys, or production credentials |
| Logs | Never log passwords, OTP codes, full refresh tokens, or VNPay hash secrets |

---

## 6. OWASP Top 10 — control map

| OWASP | Control in this system |
|-------|------------------------|
| **A01 Broken Access Control** | RBAC + property scope + customer ownership; 401/403 via entry point / `ForbiddenException` |
| **A02 Cryptographic Failures** | BCrypt passwords; HTTPS prod; secrets outside repo; no storing PAN (VNPay hosted) |
| **A03 Injection** | JPA/JPQL only; no string-concatenated SQL; Bean Validation on bodies |
| **A04 Insecure Design** | Spec-first booking/payment rules; hold timeout; deposit rules must not be bypassed |
| **A05 Security Misconfiguration** | Harden CORS/Swagger for prod; no wildcard CORS; default-deny authenticated routes |
| **A06 Vulnerable Components** | Keep Spring Boot / npm deps patched; review Dependabot/advisories in sprint |
| **A07 Identification & Auth Failures** | OTP verify, refresh revoke, SUSPENDED lock, generic login errors |
| **A08 Software & Data Integrity** | VNPay signature verify server-side; no trust of return query alone |
| **A09 Security Logging Failures** | SLF4J authz failures / payment verify outcomes — **without** PII dumps (see data-governance) |
| **A10 SSRF** | No user-controlled arbitrary URL fetch; uploads stored locally with type/size checks |

---

## 7. Privacy / GDPR-aligned requirements

Full GDPR certification is out of course scope; **practices below are mandatory** for personal data:

| Requirement | Policy |
|-------------|--------|
| Lawful use | Collect only data needed for booking, payment, contract, support |
| Access control | PII visible only to authorized roles (customer self, manager of property, admin) |
| Minimization in logs | Mask email/phone in logs; never log password/OTP |
| Retention | Follow `sdd/global/data-governance.md` (e.g. application log retention target **90 days**) |
| User rights (target) | Support account deactivate/suspend; export/delete flows when specified in admin/profile specs |
| Third parties | Google / VNPay / SMTP — share only what the integration requires |

Detail ownership & retention: **`sdd/global/data-governance.md`**.

---

## 8. File uploads

| Policy | Requirement |
|--------|-------------|
| Validate before disk write | MIME/type + size |
| Constitutional max | **10MB** unless Article 3 amended |
| Gap | Resolved Phase C (2026-07-23): multipart + banner capped at **10MB** |
| Public `/uploads/**` | Do not place highly sensitive ID scans without access redesign |

---

## 9. Payments

- Create payment URL: authenticated `CUSTOMER` + booking ownership.
- VNPay return: verify hash with server secret; update payment/booking only after verify.
- Manual bank confirm: Manager/Admin only; audit actor.

---

## 10. Compliance checklist (PR)

- [ ] HTTPS / CORS / Swagger appropriate for target environment
- [ ] No secrets in diff
- [ ] AuthN + AuthZ + ownership/scope on new endpoints
- [ ] Validation on request bodies; no SQL concatenation
- [ ] Upload type/size validated if files involved
- [ ] No PII/secrets in new log statements
- [ ] OWASP-relevant tests for authz failures where critical

---

## 11. Related documents

| Doc | Role |
|-----|------|
| `sdd/global/system-architecture.md` | Topology & communication |
| `sdd/global/data-governance.md` | PII, retention, ownership |
| `sdd/global/api-standards.md` | Error codes & API surface |
| `CONSTITUTION.md` Article 3 | Non-negotiable security law |
| `sdd/services/homestay/features/001-user-auth/` | Auth feature spec |
| `sdd/services/homestay/features/012-payment-management/` | Payment security rules |
