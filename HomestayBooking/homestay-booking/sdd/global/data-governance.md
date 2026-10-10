# data-governance.md — Data Ownership, Retention & Privacy
# Version: 1.0 | Updated: 2026-07-23
# Purpose: Data ownership, retention, privacy rules
# Related: `sdd/global/security.md`, `sdd/global/system-architecture.md`, `sdd/global/api-standards.md`, `CONSTITUTION.md`

---

## 1. Scope

Defines who owns which data, how long it is kept, and how personal data (PII) is handled in the Homestay Booking System.

**Examples of rules this doc owns:** PII masking in logs/UI, application log retention (**90 days**), ownership of booking vs property data, export/delete expectations.

Authoritative product fields still come from feature specs under `sdd/services/homestay/features/`.

---

## 2. Data classification

| Class | Examples | Handling |
|-------|----------|----------|
| **Public** | Room name, price, property description, active promotions | Public GET OK |
| **Internal** | Occupancy reports, maintenance tickets, housekeeping tasks | Role + property scope |
| **PII** | Email, phone, full name, profile photo | Minimize; mask in logs; RBAC |
| **Sensitive PII / docs** | ID images, payment receipts, contracts PDF | Strict role; prefer not world-readable long-term |
| **Secrets** | Password hash, refresh token, JWT secret, VNPay keys | Never in logs/API responses; env only |
| **Financial** | Payment amounts, deposit %, VNPay refs | Customer own + manager/admin; verify integrity |

---

## 3. Data ownership

| Domain | System of record | Owner (business) | Technical steward |
|--------|------------------|------------------|-------------------|
| User account / profile | `users` (+ related) | End user (self) | Auth / Profile modules (`001`, `002`) |
| Property / floors / rooms | property structure tables | Admin creates; Manager operates assigned | `006`–`008` |
| Booking | `bookings` | Customer (subject); Manager (property ops) | `004` |
| Payment | `payments` | Finance ops + Customer (own) | `012` |
| Contract | `contracts` + PDF files | Legal/ops; Customer receives copy | `010` |
| Inspection / damage | inspection & damage tables | Manager / Employee of property; Admin if escalated | `023` |
| Notifications | notification store | System; recipient user | `015` |
| Uploads | `/uploads/**` | Same as parent entity | Upload services |
| Application logs | log files / host | Platform / Admin | Ops |

**Rule:** Managers and Employees may only access **assigned property** data. Customers may only access **their own** bookings/payments/contracts unless an Admin tool explicitly allows otherwise.

---

## 4. PII inventory (minimum)

| Field / artifact | Location | Notes |
|------------------|----------|-------|
| `email`, `fullName`, `phone` | `users` | Unique email; login identity |
| `password_hash` | `users` | BCrypt only — never return in API |
| Google `sub` | `users` | External id; treat as sensitive identifier |
| OTP codes | user / OTP fields | Short TTL; never log cleartext |
| Refresh tokens | `refresh_tokens` (or equiv.) | Revocable; never log full value |
| Booking guest contact | booking / profile snapshots | May duplicate PII — protect as PII |
| Contract PDF | disk / storage | Contains personal + commercial terms |
| ID / receipt images | `/uploads` | Higher sensitivity |

APIs MUST use DTOs — **never** expose `passwordHash`, raw tokens, or internal secrets.

---

## 5. Privacy rules

| Rule | Requirement |
|------|-------------|
| **Minimization** | Collect only fields needed for FR in approved specs |
| **Purpose limitation** | Use PII for booking, payment, contract, support, security — not for unrelated analytics without decision |
| **Access** | Enforce RBAC + ownership/scope (see `security.md`) |
| **PII masking (logs)** | Log `u***@domain.com`, last-4 phone, or user UUID — not full email/phone/name dumps |
| **PII masking (UI admin lists)** | Prefer partial mask for phone/email in dense tables when full value not required |
| **Export** | When Admin/Customer export is specified, include only authorized fields |
| **Deletion / anonymize** | Prefer soft-delete or anonymize PII when account removal is specified; retain financial records as required by payment/booking audit |

Align with GDPR-oriented practices in `security.md` §7 even if formal GDPR certification is N/A for the course.

---

## 6. Retention

| Data | Retention target | Notes |
|------|------------------|-------|
| **Application / access logs** | **90 days** then delete or rotate off hot storage | Security/ops default |
| OTP / short codes | Minutes (config TTL) then unusable | Do not keep cleartext history |
| Refresh tokens | Until expiry or revoke; purge expired rows periodically | |
| Bookings / payments / contracts | Retain for business + dispute window (default: life of system unless Admin purge policy defined) | Financial audit |
| Uploaded images (rooms/banners) | While parent entity active | Delete orphans when entity deleted |
| Highly sensitive uploads (ID) | Prefer shortest period after check-in/out complete | Tighten when implementing |
| Soft-deleted users | Per admin policy; anonymize PII when hard-removing | |

If a feature needs a different retention, document it in that feature’s `spec.md` Assumptions and update this table.

---

## 7. Cross-border & processors

| Processor | Data shared | Constraint |
|-----------|-------------|------------|
| Google OAuth | Email / profile claims for login | Link rules in `001-user-auth` |
| VNPay | Amount, order ref — **no** card PAN stored by us | Verify callbacks server-side |
| SMTP provider | Email address + message body (OTP/contract) | TLS to provider; no password in mail |

---

## 8. Engineering checklist

- [ ] New PII fields classified and listed in this inventory (or feature CONTEXT)
- [ ] DTOs omit secrets/hashes
- [ ] New logs mask PII
- [ ] Property/customer scope enforced for reads
- [ ] Retention noted if different from defaults
- [ ] Upload path sensitivity reviewed (`/uploads` public GET)

---

## 9. Related documents

| Doc | Role |
|-----|------|
| `sdd/global/security.md` | Access control & OWASP |
| `sdd/global/system-architecture.md` | Where data is stored |
| `sdd/global/api-standards.md` | What APIs may return |
| `docs/entity-ui-mapping.md` | Entity ↔ screens |
| `sdd/services/homestay/features/002-user-profile/` | Profile data rules |
| `sdd/services/homestay/features/009-customer-management/` | Admin customer directory |
