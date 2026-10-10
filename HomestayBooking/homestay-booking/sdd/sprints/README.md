# Sprints — Homestay Booking System

Thư mục quản lý sprint theo Spec-Driven / Agent-Driven Development.

## Cấu trúc

| File | Mục đích |
|------|----------|
| [backlog.md](./backlog.md) | Backlog sản phẩm / gap vs specs |
| [tech-debt.md](./tech-debt.md) | Tech debt tracker |
| [team-roles.md](./team-roles.md) | Vai trò sprint (Spec Architect, Agent Pilot, …) |
| [pre-commit-checklist.md](./pre-commit-checklist.md) | Checklist trước commit/PR (playbook Ch.16.6) |
| [calendar.md](./calendar.md) | Lịch Spec Sync / Agent Jam / Demo |
| `sprint-N-plan.md` | Kế hoạch sprint N |
| `sprint-N-retro.md` | Retro + metrics sau sprint |
| `sprint-N-agent-jam.md` | Ghi chú Agent Jam trong sprint |
| [_templates/](./_templates/) | Template trống copy cho sprint mới |

## Sprint hiện tại

**Sprint 2** — [sprint-2-plan.md](./sprint-2-plan.md)

Focus: Booking lifecycle (hold timeout, no-show, cancel tiers, FR-015).

## Quy tắc nhanh

1. Monday Spec Sync → duyệt spec **trước** khi code (`CONSTITUTION.md` Art.7)
2. Agent Jam → load `AGENTS.md`, `CLAUDE.md`, `CONSTITUTION.md` + spec liên quan
3. Mọi PR agent phải qua [pre-commit-checklist.md](./pre-commit-checklist.md)
4. Spec priority: `sdd/services/homestay/features/<feature>/spec.md` > `AGENTS.md` > narrative docs

## Tạo sprint mới

```text
1. Copy _templates/sprint-N-plan.md → sprint-3-plan.md
2. Copy _templates/sprint-N-retro.md → sprint-3-retro.md
3. Copy _templates/agent-jam-notes.md → sprint-3-agent-jam.md
4. Cập nhật "Sprint hiện tại" trong README này + AGENTS.md §8
```
