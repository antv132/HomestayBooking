# system-architecture.md — Homestay Booking System Architecture
# Version: 1.0 | Updated: 2026-07-23
# Purpose: High-level service topology & communication patterns
# Related: `sdd/global/security.md`, `sdd/global/data-governance.md`, `sdd/global/api-standards.md`, `AGENTS.md`, `CONSTITUTION.md`

---

## 1. Overview

| | |
|--|--|
| **System** | Homestay / Resort Booking Management |
| **Style** | Modular monolith — SPA + REST API |
| **Clients** | React SPA (Guest, Customer, Employee, Manager, Admin) |
| **Server** | Spring Boot 3 (Java 17) — `com.homestay.*` |
| **Data** | SQL Server + Spring Data JPA / Hibernate |
| **Auth** | JWT (access + refresh), Google OAuth, BCrypt |
| **Workspaces** | `Backend/`, `Frontend/` |

Specs (OPTION A): `sdd/global/` → `sdd/services/homestay/module.md` → `sdd/services/homestay/features/`. Process: `sdd/sprints/`.

---

## 2. Service topology

```text
┌─────────────┐     HTTPS / JSON      ┌──────────────────────┐
│  React SPA  │ ◄───────────────────► │  Spring Boot API     │
│  (Vite)     │   Bearer JWT          │  modular packages    │
└─────────────┘                       └──────────┬───────────┘
       │                                         │
       │ static                                  │ JPA
       ▼                                         ▼
┌─────────────┐                       ┌──────────────────────┐
│ /uploads/** │                       │     SQL Server       │
└─────────────┘                       └──────────▲───────────┘
                                                 │
┌─────────────┐  return / verify                 │
│   VNPay     │ ◄────────────────────────────────┘
└─────────────┘
┌─────────────┐  OAuth
│   Google    │ ◄── auth flows
└─────────────┘
┌─────────────┐  SMTP
│   Email     │ ◄── OTP / contract / notify
└─────────────┘
```

**DEV topology:** Browser `:3000` → Vite proxy → API `:8080` → SQL Server. See `docs/environment-setup.md`.

This is a **single deployable API** (not a microservice mesh). Domain modules are package boundaries inside one process.

---

## 3. Communication patterns

| Pattern | When | How |
|---------|------|-----|
| **Synchronous REST** | Default — UI ↔ API, CRUD, booking/payment commands | JSON over HTTP; prefer `/api/v1/...` |
| **Browser redirect callback** | VNPay payment return | Public `GET /api/payments/vnpay/return` then redirect FE |
| **Scheduled jobs** | Time-based domain events | `@Scheduled` in `job/` (hold timeout; no-show planned) |
| **Async / Outbox (target)** | Email, contract PDF dispatch, notifications | Transactional Outbox → worker (spec); some paths still sync today |
| **Static file serve** | Room images, banners, uploads | Disk `/uploads/**` |

**Not in stack (do not introduce without Constitution amendment):** Kafka, RabbitMQ, GraphQL, separate BFF services.

```text
Sync path:     SPA ──REST──► Controller ──► Service ──► Repository ──► SQL Server
Async target:  Service ──write Outbox──► DB ──► poller/worker ──► Email / PDF / notify
Time path:     Scheduler ──► Service (hold timeout / no-show)
```

---

## 4. Internal layering (API process)

```text
Controller  →  validate, authz, ApiResponse
    ↓
Service     →  business rules, transactions
    ↓
Repository  →  JPA / JPQL
    ↓
Entity      →  snake_case persistence
```

| Layer | Package | Rule |
|-------|---------|------|
| API | `controller` | Thin; no Repository inject |
| DTO | `dto.request` / `dto.response` | Never return Entity |
| Domain | `service` | Business rules live here |
| Persistence | `repository`, `entity` | UUID PKs; index FKs/status/dates |
| Security | `security` | JWT + RBAC — see `security.md` |
| Jobs | `job` | `@Scheduled` only |

---

## 5. Frontend topology

```text
pages/       role screens (public | customer | manager | admin | employee)
layouts/     shells
components/  ui kit + feature widgets
api/         Axios *Api.ts
hooks/       React Query / domain
store/       Zustand (client state)
```

| Concern | Choice |
|---------|--------|
| Server state | TanStack React Query |
| Client state | Zustand |
| HTTP | Axios + JWT interceptors |

---

## 6. Domain modules ↔ specs

| Module | Spec | Responsibility |
|--------|------|----------------|
| Auth | `001-user-auth` | Register, OTP, login, Google, refresh |
| Profile | `002-user-profile` | Profile CRUD |
| Discovery | `003-room-discovery` | Search / room detail |
| Booking & inventory | `004-booking-inventory` | Hold, deposit, lifecycle, cancel, no-show |
| Calendar | `005-availability-calendar` | Day availability |
| Property … Housekeeping | `006`–`023` | See folder list under `sdd/services/homestay/features/` |

Full module table and booking state machine: keep in sync with `004-booking-inventory` and feature CONTEXT files.

---

## 7. Core booking flow (topology impact)

```text
PENDING_DEPOSIT ──deposit──► CONFIRMED ──check-in──► CHECKED_IN ──► … ──► CHECKED_OUT
       │                        │
       │ hold job               │ no-show job (spec)
       ▼                        ▼
   CANCELLED                  NO_SHOW
```

Inventory coordination: pessimistic `Room` lock + date overlap in the **same** API process/DB — not a distributed lock service.

---

## 8. Cross-cutting doc map

| Concern | Document |
|---------|----------|
| AuthN/AuthZ, OWASP, HTTPS | `sdd/global/security.md` |
| PII, retention, ownership | `sdd/global/data-governance.md` |
| REST naming, pagination, errors | `sdd/global/api-standards.md` |
| Project law | `CONSTITUTION.md`, `sdd/global/constitution.md` |
| Module map | `sdd/services/homestay/module.md` |
| Agent coding rules | `AGENTS.md` |

---

## 9. ADR summary

| ADR | Decision |
|-----|----------|
| Style | Modular monolith + REST SPA |
| Auth | JWT over server sessions |
| Persist | JPA on SQL Server |
| Async | Outbox/jobs — not Kafka (unless Constitution amended) |
| Specs | Spec-first under `sdd/services/homestay/features/` |

---

## 10. Related documents

| Doc | Role |
|-----|------|
| `sdd/global/security.md` | Security policies & compliance |
| `sdd/global/data-governance.md` | Data ownership & privacy |
| `sdd/global/api-standards.md` | API design conventions |
| `AGENTS.md` / `CLAUDE.md` | Runtime agent guidance |
| `sdd/services/homestay/features/*/spec.md` | Feature requirements |
