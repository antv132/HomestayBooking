# Template để tạo spec mới

Hướng dẫn tạo feature spec trong Homestay Booking System (Spec-Driven Development).

## Cấu trúc thư mục chuẩn

```text
sdd/services/homestay/features/[NNN]-[feature-slug]/
├── CONTEXT.md                 # Bắt buộc trước khi viết spec (pain + domain)
├── spec.md                    # Feature specification (business, không code)
├── plan.md                    # Sau /speckit-plan
├── research.md                # Phase 0 của plan
├── data-model.md              # Phase 1
├── quickstart.md              # Phase 1
├── contracts/                 # OpenAPI / API contracts
├── tasks.md                   # Sau /speckit-tasks
└── checklists/
    └── requirements.md        # Spec quality checklist
```

`NNN` = số thứ tự 3 chữ số (`024-...`).  
`feature-slug` = kebab-case tiếng Anh ngắn (`booking-no-show`).

## Quy trình (bắt buộc)

1. **Spec Sync** duyệt ý tưởng / FR từ `docs/Specification_v2.md` hoặc backlog.
2. Copy templates từ thư mục này → folder feature mới.
3. Điền **`CONTEXT.md`** trước (chỉ pain + domain — không solution).
4. Đóng Open Questions quan trọng → điền **`spec.md`**.
5. Chạy checklist **`checklists/requirements.md`** — không còn `[NEEDS CLARIFICATION]` blocker.
6. Spec Sync **Approve** → mới `/speckit-plan` → `/speckit-tasks` → implement.
7. Agent phải đọc: root `AGENTS.md`, `CONSTITUTION.md`, `CONTEXT.md`, `spec.md`.

## File template trong thư mục này

| File | Khi dùng |
|------|----------|
| [`_template.md`](./_template.md) | File này — hướng dẫn |
| [`CONTEXT.md`](./CONTEXT.md) | Copy → `CONTEXT.md` |
| [`spec.md`](./spec.md) | Copy → `spec.md` |
| [`checklists/requirements.md`](./checklists/requirements.md) | Copy → `checklists/requirements.md` |

Plan / tasks / data-model: dùng Spec Kit (`.specify/templates/`) hoặc `/speckit-plan`, `/speckit-tasks`.

## Quy tắc nội dung `spec.md`

- Viết cho stakeholder — **không** ngôn ngữ/framework/API cụ thể trong FR.
- User story có Priority (P1…) + Independent Test + Given/When/Then.
- Success Criteria đo được và technology-agnostic.
- Conflict: `spec.md` feature > `AGENTS.md` > `Specification_v2.md`.

## Lệnh nhanh (Cursor)

```text
/speckit-specify   → sinh/điền spec
/speckit-plan      → plan + research + data-model + contracts
/speckit-tasks     → tasks.md
/speckit-clarify   → làm sạch OQ
```

## Checklist tạo folder mới

- [ ] Tạo `sdd/services/homestay/features/[NNN]-[slug]/`
- [ ] Copy `CONTEXT.md`, `spec.md`, `checklists/requirements.md`
- [ ] Điền CONTEXT (mục 1–6)
- [ ] Điền spec (stories, FR, SC, Assumptions)
- [ ] Pass requirements checklist
- [ ] Ghi vào `sdd/sprints/backlog.md` nếu vào sprint
- [ ] Spec Sync approve trước khi code
