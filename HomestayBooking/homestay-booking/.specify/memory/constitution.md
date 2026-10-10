<!--
Sync Impact Report
- Version change: (template placeholders) → 1.0.0
- Modified principles: N/A (first fill from root CONSTITUTION.md + AGENTS.md + features hierarchy)
- Added sections: Core Principles (I–V); Tech Stack & Security Constraints; Spec-Driven Workflow & Quality Gates; Governance
- Removed sections: template placeholder tokens only
- Templates requiring updates:
  - .specify/templates/plan-template.md — ✅ Constitution Check gates + sdd/services/homestay/features paths
  - .specify/templates/spec-template.md — ✅ note CONTEXT + sdd/services/homestay/features path
  - .specify/templates/tasks-template.md — ⚠ pending (no principle-driven task type change required)
  - .specify/templates/commands/*.md — N/A (directory absent)
  - Root CONSTITUTION.md — ✅ path refs Option A (docs/ = product SRS; sdd/ = Spec-as-Code)
  - AGENTS.md — ✅ path refs Option A
- Deferred TODOs: Team member names on ratification line (TODO(TEAM_NAMES))
-->

# Homestay Booking System Constitution

## Core Principles

### I. Spec-First Delivery
Feature work MUST begin from an approved feature specification under
`sdd/services/homestay/features/[NNN]-[slug]/` (CONTEXT.md then spec.md). Agents and humans
MUST NOT implement feature code before Monday Spec Sync approval of the
related spec. Conflict resolution MUST follow:
`sdd/services/homestay/features/<feature>/spec.md` > root `AGENTS.md` > `CLAUDE.md` >
`docs/Specification_v2.md`. Rationale: prevents solution-first drift and
keeps Spec Kit (`/speckit-*`) artifacts authoritative.

### II. Immutable Tech Stack
The stack MUST remain: Java 17 + Spring Boot 3.x + Spring Security +
Spring Data JPA + Maven; React 18 + TypeScript + Vite; SQL Server;
JWT access/refresh + BCrypt + Google OAuth; REST (`/api/...`, prefer
`/api/v1/...`); Tailwind CSS 3.x + shared `components/ui`; FE package
manager npm. Alternate languages, NoSQL, GraphQL, Redux, CSS-in-JS as
default, or session-cookie primary auth MUST NOT be introduced without
a unanimous Constitution amendment vote.

### III. Layered Architecture & Clean Boundaries
Backend MUST use Controller → Service → Repository → Entity with
constructor injection. Controllers MUST only validate and delegate;
business rules MUST live in services. APIs MUST return
`ResponseEntity<ApiResponse<T>>` with DTOs — entities MUST NOT leak.
Prefer JPA/JPQL; native SQL ONLY with documented justification and
review. Frontend MUST use functional components, TanStack Query for
server state, Zustand for client state, and TypeScript without `any`
for API payloads. Logging MUST use SLF4J (backend); no
`System.out.println` / production `console.log`.

### IV. Security Non-Negotiable
Passwords MUST use BCrypt with minimum cost 12. Secrets MUST live in
environment/secure config only — never commit JWT secrets, DB
passwords, or VNPay keys. SQL string concatenation for queries is
forbidden. File uploads MUST validate MIME type and size (constitutional
max 10MB unless this principle is amended). Every request body MUST be
validated. Production CORS MUST NOT use wildcard `*`. Manager/Employee
access MUST stay within assigned Property scope. Booking/payment/
check-in-out business rules MUST NOT be bypassed. Canonical booking
statuses MUST NOT be invented outside the enum defined in `AGENTS.md`
and `sdd/services/homestay/features/004-booking-inventory`.

### V. Test Discipline & Definition of Done
New business-critical code (booking, payment, auth) MUST target ≥80%
coverage. Service/business logic MUST have unit tests (JUnit 5 +
Mockito). New API endpoints MUST cover happy and error paths. Existing
tests MUST pass before merge. E2E for critical flows is encouraged.
Tasks MUST meet `AGENTS.md` Definition of Done (build, lint, DTO/API
docs when applicable, no leftover TODOs for the completed scope).

## Tech Stack & Security Constraints

- Workspaces: `Backend`,
  `Frontend`.
- Spec-as-Code layout (OPTION A): `sdd/global/` (Layer 1), 
  `sdd/services/homestay/module.md` (Layer 2), 
  `sdd/services/homestay/features/` (Layer 3, 001–023+) with CONTEXT.md + spec.md;
  templates in `sdd/services/homestay/features/_templates/`. See `sdd/README.md`.
- Sprint process artifacts: `sdd/sprints/` (plan, Spec Sync, Agent Jam,
  backlog, tech-debt, pre-commit checklist).
- Project law mirror: root `CONSTITUTION.md` (Articles 1–7) and 
  `sdd/global/constitution.md` MUST stay semantically aligned with this Spec Kit constitution.
- Runtime agent guidance: root `AGENTS.md`, `CLAUDE.md`.

## Spec-Driven Workflow & Quality Gates

- **Spec Sync (Monday)**: review/approve specs; no feature coding before
  approval.
- **Agent Jam (Wednesday)**: Agent Pilot drives Cursor; load
  `AGENTS.md`, `CLAUDE.md`, this constitution / root `CONSTITUTION.md`,
  and active feature CONTEXT + spec; review agent plan before execution;
  human-led refactor after every 3–5 agent tasks.
- **Demo / Review (Friday)**: demo + tech debt logging + retro.
- **PR**: min 1 approval; max ~400 lines changed (split larger PRs);
  protected `main` / `DEV` — no direct push without exception;
  pre-commit checklist in `sdd/sprints/pre-commit-checklist.md`.
- **Plan Constitution Check** (`.specify/templates/plan-template.md`):
  gates MUST fail the plan if stack, security, layered boundaries, or
  missing approved spec are violated.
- Known gaps (MUST implement properly when scheduled — no workarounds):
  No-show job (FR-008), cancel refund tiers (FR-010), manager modify
  booking (FR-015).

## Governance

This constitution supersedes informal practices for Spec Kit workflows.
Amendments REQUIRE: documented proposal, unanimous team approval,
version bump (MAJOR = remove/redefine principles; MINOR = add/expand;
PATCH = clarify wording), and migration notes for templates/agents.
Compliance: every PR and `/speckit-analyze` run MUST verify alignment
with Core Principles. Complexity beyond standards MUST be justified in
the PR/plan Complexity Tracking section. Runtime development guidance
lives in `AGENTS.md`; Claude-specific memory in `CLAUDE.md`; human
Project Law summary in root `CONSTITUTION.md`.
TODO(TEAM_NAMES): list ratifying members on next amendment.

**Version**: 1.0.0 | **Ratified**: 2026-07-23 | **Last Amended**: 2026-07-23
