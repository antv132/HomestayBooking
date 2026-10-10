# constitution.md — Layer 1 Global Spec
# Version: 1.0 | Updated: 2026-07-23
# Location: `sdd/global/` (OPTION A — Monorepo Spec)

---

## Role

Layer 1 “project law” for Spec-as-Code. Agents and humans MUST follow these constraints before implementing features under `sdd/services/homestay/features/`.

**Canonical mirrors (keep semantically aligned):**

| File | Audience |
|------|----------|
| Root `CONSTITUTION.md` | Human project law (Articles 1–7) |
| `.specify/memory/constitution.md` | Spec Kit /plan Constitution Check |
| **This file** | SDD global catalog entry |

---

## Core principles (summary)

1. **Spec-First** — No feature coding before Spec Sync approval of `CONTEXT.md` + `spec.md`.
2. **Immutable stack** — Java 17 + Spring Boot 3 + JPA + SQL Server; React 18 + Vite; JWT + BCrypt + Google OAuth. No Kafka/GraphQL/session-primary without Constitution amendment.
3. **Layered API** — Controller → Service → Repository; DTOs only; no Entity leak.
4. **Security non-negotiable** — See `sdd/global/security.md` (BCrypt ≥12, secrets in env, no SQL concat, property scope, HTTPS prod).
5. **Test discipline** — ≥80% on booking/payment/auth critical paths; DoD in `AGENTS.md`.

---

## Spec hierarchy (this repo)

| Layer | Path |
|-------|------|
| 1 Global | `sdd/global/` |
| 2 Module | `sdd/services/homestay/module.md` |
| 3 Feature | `sdd/services/homestay/features/[NNN]-[slug]/` |

---

## Related global standards

- `system-architecture.md` — topology & communication
- `security.md` — policies & compliance
- `data-governance.md` — PII, retention, ownership
- `api-standards.md` — REST naming, pagination, errors
