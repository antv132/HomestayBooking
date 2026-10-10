# docs/ — Product & engineering narrative docs

**Convention A (this repo):** keep product/UI/SRS documents here. Do **not** move them into `sdd/`.

| Location | Role |
|----------|------|
| Repo root (`AGENTS.md`, `CLAUDE.md`, `CONSTITUTION.md`) | Agent / team law |
| `docs/` | Product SRS, screens, API-by-screen, entity↔UI mapping |
| `sdd/` | Spec-as-Code: global rules, feature specs (`services/homestay/features/`), sprints |
| `Backend/` | Spring Boot API (canonical backend workspace) |
| `Frontend/` | React SPA (canonical frontend workspace) |

## Contents

| File | Role |
|------|------|
| [Architecture.md](Architecture.md) | System architecture (stack, layers, auth, integrations, ADR) |
| [Specification_v2.md](Specification_v2.md) | Narrative SRS / FR overview (secondary to feature specs) |
| [use-case-catalog.md](use-case-catalog.md) | Use case catalog (65 UCs) |
| [use-case-specifications.md](use-case-specifications.md) | Use case specs (Primary/Secondary Actors, flows…) |
| [screen.md](screen.md) | **SCR ID master** + screen catalog |
| [screendesign.md](screendesign.md) | Per-screen layout notes |
| [entity-ui-mapping.md](entity-ui-mapping.md) | Entity ↔ UI mapping |
| [api-spec-by-screen.md](api-spec-by-screen.md) | API ↔ screen mapping |
| [uat-checklist.md](uat-checklist.md) | Manual UAT (aligned to sdd tiers / edu cuts) |
| [environment-setup.md](environment-setup.md) | Local setup |
| [testing.md](testing.md) | Testing notes |
| [DESIGN.md](DESIGN.md) / [component-library.md](component-library.md) | Design system |
| [figma-generation-prompt.md](figma-generation-prompt.md) | Figma prompt (may contain legacy SCR numbers — follow `screen.md`) |

## Conflict resolution

`sdd/services/homestay/features/<feature>/spec.md` > `AGENTS.md` > `CLAUDE.md` > `docs/Specification_v2.md`

Feature work: `sdd/services/homestay/features/[NNN]-[slug]/` — not under `docs/`.

## Docs ↔ sdd sync (2026-07-23)

| Rule | Source of truth | Docs expectation |
|------|-----------------|------------------|
| Customer cancel refund | `004` FR-010 | Deposit tiers ≥7d 100% / 3–7d 50% / &lt;3d 0% |
| Manager cancel refund | `004` FR-016 | 100% **deposit** |
| Hold timeout | `004` + TD-003 | Default **10 minutes** |
| Payment methods (edu) | `012` + TD-011 | **VNPay only**; bank transfer / receipt / CK verify deferred |
| VNPay confirm path (edu) | `012` + TD-012 | **Return URL only**; IPN **deferred** |
| VNPay reconciliation UI | `012` + TD-010 | SCR-52 **deferred** |
| SCR-19 / SCR-20 / SCR-37 | `screen.md` | Cancel / Order+VNPay / Check-in-out |

See also: [sdd/](../sdd/)

## Root doc folders (2026-07-23)

| Folder | Index |
|--------|--------|
| [`testing/`](../testing/) | Strategy, test plan, cases, regression |
| [`deployment/`](../deployment/) | Docker, CI/CD, environments |
| [`prompts/`](../prompts/) | Agent prompts (backend, frontend, testing, review, bugfix) |

These complement `docs/` and `sdd/` — link to canonical sources; feature truth stays in `sdd/.../spec.md`.

