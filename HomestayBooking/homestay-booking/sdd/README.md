# sdd/ — Spec-as-Code (OPTION A: Monorepo)

Homestay Booking System stores Spec **in the same Git repo as code** (no separate specs repo / submodule).

Folder `sdd/` ≡ template `.sdd/`.

```text
sdd/
├── global/                         # Layer 1 — shared across the system
│   ├── constitution.md
│   ├── system-architecture.md
│   ├── system-arch.md              # alias
│   ├── security.md
│   ├── data-governance.md
│   └── api-standards.md
├── services/
│   └── homestay/                   # Layer 2 — modular monolith (= one "service")
│       ├── module.md
│       └── features/               # Layer 3 — feature specs (001–023+)
│           ├── _templates/
│           ├── 001-user-auth/
│           └── …
└── sprints/                        # Process artifacts (plan, Spec Sync, retro)
```

**Code (cùng repo):** `Backend/`, `Frontend/`

**Product docs (không nằm trong `sdd/`):** `docs/` — xem `docs/README.md`  
(SRS `Specification_v2.md`, screens, api-spec-by-screen, entity-ui-mapping)

**Conflict resolution:**  
`sdd/services/homestay/features/<feature>/spec.md` > `AGENTS.md` > `CLAUDE.md` > `docs/Specification_v2.md`

**Spec Kit:** `.specify/memory/constitution.md` must stay aligned with root `CONSTITUTION.md` and `sdd/global/constitution.md`.
