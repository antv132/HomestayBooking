# AGENTS.md — Project Context for AI Agents
# Version: 1.0 | Updated: 2026-07-23 | Project: Homestay Booking System

## 1. PROJECT OVERVIEW

Name: Homestay Booking System

Type: Full-stack Web App (API + SPA)

Domain: Hospitality / Property Management / Booking

Stage: Development

## 2. TECH STACK (STRICT — do not deviate)

Backend: Java 17 + Spring Boot 3.x + Spring Security + Spring Data JPA + Maven

Frontend: React 18 + TypeScript + Vite + React Router + Axios + TanStack React Query + Zustand

Database: SQL Server

ORM: Spring Data JPA / Hibernate

Auth: JWT (access + refresh) + bcrypt + Google OAuth

Testing: JUnit 5 + Mockito + Postman (Selenium planned)

Styling: Tailwind CSS 3.x + custom UI under `src/components/ui` (shadcn-style)

Docs: OpenAPI / Swagger

## 3. ARCHITECTURE PRINCIPLES

- Follow Layered Architecture: Controller → Service → Repository → Entity
- API style: REST (`/api/...`, prefer `/api/v1/...` when available)
- Error handling: always use centralized `@ControllerAdvice` / GlobalExceptionHandler
- Prefer JPA/JPQL — no raw native SQL unless justified and approved
- No `System.out.println` / `console.log` in production paths — use SLF4J (backend) or structured app logging (frontend)
- Business rules live in Service layer; Controllers validate and delegate only
- DTOs for API boundaries; do not expose entities directly
- Constructor injection; no field injection

## 4. FILE NAMING & STRUCTURE

Java Controllers / Services / Entities: PascalCase (e.g. `BookingService.java`)

Java DTOs: PascalCase request/response (e.g. `CreateBookingRequest.java`, `BookingDetailResponse.java`)

Components / Pages: PascalCase (e.g. `BookingCard.tsx`, `AboutPage.tsx`)

Utilities / hooks: camelCase (e.g. `formatCurrency.ts`, `useBooking.ts`, `roomCalendar.ts`)

API modules: camelCase `*Api.ts` (e.g. `bookingApi.ts`)

API routes: kebab / resource style (e.g. `/api/v1/manager/bookings`, `/api/admin/about`)

DB tables / columns: snake_case (e.g. `bookings`, `check_in_date`, `customer_id`)

Workspace folders in use: `Backend`, `Frontend`

## 5. FORBIDDEN PATTERNS

- NEVER store secrets/passwords in plain text or commit `.env` / `application.yml` secrets (JWT, DB password, VNPay keys)
- NEVER use `any` — use proper TypeScript types
- NEVER skip input validation on API endpoints
- NEVER put business logic in Controllers or access Repository from Controllers
- NEVER use deprecated libraries without team approval
- NEVER delete files in `/data` or `/uploads` without user confirmation
- NEVER bypass booking inventory / payment / check-in-out business rules
- NEVER invent new booking statuses outside the canonical enum

Canonical booking statuses (`sdd/services/homestay/features/004-booking-inventory`):

`PENDING_DEPOSIT` | `CONFIRMED` | `CHECKED_IN` | `PENDING_INSPECTION` | `PENDING_DAMAGE_PAYMENT` | `CHECKED_OUT` | `CANCELLED` | `NO_SHOW`

Key business rules (summary — details in specs):

- Deposit 40% / remaining 60%; no double-booking (room lock + date overlap)
- Hold timeout: unpaid `PENDING_DEPOSIT` cancels (spec default 10 min, configurable)
- No-show: `CONFIRMED` → `NO_SHOW` after 24h past expected check-in; deposit kept; release inventory
- Customer cancel tiers on deposit: ≥7d 100% / 3–7d 50% / <3d 0%; Manager cancel 100%
- Check-in not before check-in date; check-out requires inspection passed
- Damage fee > 5,000,000 VND needs Admin co-approval

When requirements conflict: `sdd/services/homestay/features/<feature>/spec.md` > `AGENTS.md` (this file) > `docs/Specification_v2.md` > older narrative docs.

## 6. DEFINITION OF DONE (per task)

- [ ] Unit tests written and passing (where applicable)
- [ ] Critical path coverage: if Auth/Booking/Payment changed, `mvn -Pcoverage-critical verify` (≥80% LINE on critical set) — `testing/coverage-control.md`
- [ ] No linting errors (ESLint / IDE / SonarLint)
- [ ] API endpoint documented in Swagger/OpenAPI (if API changed)
- [ ] Error cases handled with proper HTTP status codes
- [ ] Business rules and specs respected
- [ ] DTOs / FE clients updated if API changed
- [ ] DB migration updated if schema changed
- [ ] No TODO comments left for the completed scope

## 7. GIT CONVENTIONS

Branch: `feat/[feature-name]` | `fix/[bug-name]` | `hotfix/[bug-name]` | `spec/[feature-name]` | `refactor/[module]`

Commit: `[type]([scope]): [description]`

Example: `feat(auth): add JWT refresh token endpoint`

Types: `feat` | `fix` | `refactor` | `docs` | `test` | `chore`

## 8. CURRENT SPRINT CONTEXT

Sprint: Sprint 2

Focus: Booking workflow, payment processing, room availability, and booking management.

Active specs:

- `sdd/services/homestay/features/004-booking-inventory/spec.md` (primary — hold timeout, no-show, cancel tiers)
- `sdd/services/homestay/features/005-availability-calendar/spec.md`
- `sdd/services/homestay/features/010-contract-management/spec.md`
- `sdd/services/homestay/features/012-payment-management/spec.md`
- Supporting: `docs/Specification_v2.md`, `docs/entity-ui-mapping.md`, `docs/screen.md`

Known gaps (implement properly when tasked — do not invent workarounds):

- Align inventory write-path to half-open + RoomStatusBlock if drift reappears (004 clarify 2026-07-23 — Done in Sprint 2 closeout)
- Deferred edu: TD-010 VNPay reconcile; TD-011 bank transfer; TD-012b VNPay IPN — see `sdd/sprints/tech-debt.md`

Done (do not re-open as gaps): BookingNoShowJob (FR-008); customer cancel tiers (FR-010); Manager modify Confirmed (FR-015); legacy check-in/out blocked (TD-004).

Spec Kit: `.specify/memory/constitution.md` + `.cursor/skills/speckit-*` + `sdd/services/homestay/features/_templates/`
