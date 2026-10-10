# CONSTITUTION.md — Project Law
# Ratified: 2026-07-23 | Team: HomestayBooking — fill member names | Version: 1.0
# RULE: Any change to this document requires unanimous team approval.

## ARTICLE 1 — TECH STACK (immutable)

Runtime / Backend: Java 17 + Spring Boot 3.x + Spring Security + Spring Data JPA — NO alternate backend language without team vote

Build: Maven — stick with Maven (no Gradle switch without vote)

Database: SQL Server — NO NoSQL without team vote

Frontend: React 18 + TypeScript + Vite — NO class components; functional components only

State: TanStack React Query (server) + Zustand (client) — do not add Redux without vote

Styling: Tailwind CSS 3.x + shared UI under `src/components/ui` — NO CSS-in-JS; avoid one-off CSS unless Tailwind is insufficient

Package manager (FE): npm — pick one, stick with it

Auth: JWT (access + refresh) + BCrypt + Google OAuth — NO session-cookie auth as primary

API: REST (`/api/...`, prefer `/api/v1/...`) — NO GraphQL without team vote

Docs / Specs (OPTION A monorepo): `sdd/global/` (Layer 1) + `sdd/services/homestay/` (Layer 2–3 features) + root `AGENTS.md` / `CLAUDE.md` — Spec Kit via `.cursor/skills/speckit-*`; Spec Kit law: `.specify/memory/constitution.md` (must stay aligned with this file and `sdd/global/constitution.md`)

## ARTICLE 2 — CODING STANDARDS

Language (FE): TypeScript strict mode (`noImplicitAny: true` / equivalent project tsconfig)

Language (BE): Java 17; layered Controller → Service → Repository → Entity; constructor injection only

Formatter: project defaults (IDE format on save / Prettier if configured) — no style bikeshedding in PR

Linter: ESLint (FE) + IDE/SonarLint (BE) — fix warnings before merge when CI enforces

Max function length: 40 lines (refactor if longer)

Max file length: 300 lines (split if longer; legacy exceptions must shrink over time)

Comments: explain WHY not WHAT. Remove TODO before merge.

API responses: `ResponseEntity<ApiResponse<T>>`; DTOs on every endpoint; no Entity leak

Business logic: Service layer only — Controllers validate and delegate

Logging: SLF4J (BE); no `System.out.println` / stray `console.log` in production paths

## ARTICLE 3 — SECURITY POLICIES (non-negotiable)

- Passwords: BCrypt with min cost **12** — NEVER plain text, MD5, or SHA without salt
- Secrets: environment variables / secure config ONLY — never commit JWT secret, DB password, VNPay keys
- SQL: JPA/JPQL ONLY — zero tolerance for string-concatenated queries; native SQL only with documented justification + review
- File uploads: validate MIME type + size (constitutional max **10MB** unless this Article is amended; tighten any 50MB paths toward this rule)
- Input validation: Bean Validation / explicit checks on every request body; TypeScript types on FE — never `any` for API payloads
- CORS: whitelist localhost/dev origins in development; **no wildcard `*` in production**
- RBAC + property scope: Manager/Employee MUST NOT access data outside assigned Property
- NEVER disable authentication, skip authorization, or bypass booking/payment business rules

## ARTICLE 4 — GIT WORKFLOW

Main / protected branches: `main` and `DEV` — no direct push without team policy exception

Branch naming: `feat/` | `fix/` | `hotfix/` | `spec/` | `refactor/` | `chore/`

Commit format: `[type]([scope]): [description]` (max 72 chars)

Example: `feat(booking): add hold timeout job`

PR rules: min 1 approval (Quality Gatekeeper / teammate) before merge to `DEV`/`main`

PR size: max **400 lines** changed (split larger PRs)

Do not force-push protected branches; do not commit secrets

## ARTICLE 5 — TESTING REQUIREMENTS

Minimum coverage: **80%** for all new business-critical code (booking, payment, auth)

Required: unit tests for service/business logic (JUnit 5 + Mockito)

Required: API validation for new endpoints (happy + error path — Postman/integration tests)

E2E: optional but encouraged for critical flows (book → deposit → check-in)

No merge if existing tests break

## ARTICLE 6 — AI AGENT RULES

- Read `AGENTS.md` (repo root) before starting any session; Claude Code also reads `CLAUDE.md`
- Prefer Spec Kit skills (`.cursor/skills/speckit-*`) for spec → plan → tasks → implement
- Review agent plan BEFORE approving execution
- Human-Led Refactoring after every 3–5 agent tasks
- All agent-generated code must pass the Pre-Commit Checklist (team playbook Ch.16.6)
- Never approve agent output you cannot explain to another team member
- Spec priority: `sdd/services/homestay/features/<feature>/spec.md` > `AGENTS.md` > `CLAUDE.md` > `docs/Specification_v2.md`
- Agents MUST NOT invent booking statuses outside the canonical enum in `AGENTS.md` / `sdd/services/homestay/features/004-booking-inventory`

## ARTICLE 7 — REVIEW PROCESS

Code review: synchronous on Thursday Agent Jam sessions (or team-agreed weekly slot)

Spec review: Monday Spec Sync — **no feature code before related spec approval**

Architecture / stack changes: require Constitution amendment vote (unanimous)

Emergency hotfix: allowed with 1 approval + post-mortem required within 48 hours

Known gaps vs specs (track; do not paper over): No-show job (FR-008), cancel refund tiers (FR-010), manager modify booking (FR-015) — implement properly when scheduled
