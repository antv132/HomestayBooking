# module.md — Homestay (modular monolith)
# Version: 1.0 | Updated: 2026-07-24
# Layer 2 — Service / module Spec
# Code: `Backend/` + `Frontend/`
# Project CONTEXT (pain, domain, stakeholders): `CONTEXT.md` (cùng thư mục)

---

## 1. Module identity

| | |
|--|--|
| **Name** | `homestay` |
| **Style** | Modular monolith (single Spring Boot API + React SPA) |
| **Not** | Separate deployable microservices (`order-service`, `payment-service`, …) |
| **Features** | `sdd/services/homestay/features/` |
| **Global constraints** | `sdd/global/*` |

Domain “services” (auth, booking, payment, …) are **packages** inside one API process — see `sdd/global/system-architecture.md`.

---

## 2. Responsibilities

- Guest discovery → Customer booking/payment/contract
- Manager / Employee property-scoped operations
- Admin cross-property administration
- Integrations: VNPay, Google OAuth, SMTP, local `/uploads`

---

## 3. Feature catalog (Layer 3)

| ID | Folder | Domain |
|----|--------|--------|
| 001 | `features/001-user-auth` | Auth |
| 002 | `features/002-user-profile` | Profile |
| 003 | `features/003-room-discovery` | Discovery |
| 004 | `features/004-booking-inventory` | Booking & inventory |
| 005 | `features/005-availability-calendar` | Calendar |
| 006 | `features/006-property-management` | Property |
| 007 | `features/007-structure-management` | Floors / structure |
| 008 | `features/008-room-management` | Rooms |
| 009 | `features/009-customer-management` | Customers |
| 010 | `features/010-contract-management` | Contracts |
| 011 | `features/011-billing-management` | Billing |
| 012 | `features/012-payment-management` | Payments |
| 013 | `features/013-maintenance-management` | Maintenance |
| 014 | `features/014-review-rating` | Reviews |
| 015 | `features/015-notification-engine` | Notifications |
| 016 | `features/016-reporting` | Reporting |
| 017 | `features/017-administration` | Admin |
| 018 | `features/018-promotion-management` | Promotions |
| 019 | `features/019-customer-dashboard` | Customer dashboard |
| 020 | `features/020-employee-management` | Employees |
| 021 | `features/021-housekeeping-management` | Housekeeping |
| 022 | `features/022-employee-dashboard` | Employee dashboard |
| 023 | `features/023-room-inspection-damage` | Inspection & damage |

Templates: `features/_templates/`.

---

## 4. Boundaries

- **Inbound:** HTTPS REST from SPA; VNPay return; Google OAuth
- **Outbound:** SQL Server, SMTP, VNPay, disk uploads
- **Authz:** RBAC + property scope — `sdd/global/security.md`
- **Data:** Ownership/retention — `sdd/global/data-governance.md`
- **API shape:** `sdd/global/api-standards.md`

---

## 5. How to add a feature

1. Copy `features/_templates/` → `features/NNN-slug/`
2. Fill `CONTEXT.md` then `spec.md`
3. Spec Sync approve → plan/tasks → implement in Backend/Frontend workspaces
4. Keep paths in PR description for traceability (spec + code cùng repo)
