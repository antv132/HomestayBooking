# Product / Sprint Backlog

Last updated: 2026-07-23  
Source of truth for requirements: `sdd/services/homestay/features/` (then `docs/Specification_v2.md`)

## Priority P0 — Sprint 2 (booking hardening)

| ID | Item | Spec | Status | Owner |
|----|------|------|--------|-------|
| B-001 | `BookingNoShowJob` — CONFIRMED → NO_SHOW after 24h | 004 FR-008 | Todo | [NAME] |
| B-002 | Hold timeout default 10 min (spec + code aligned) | 004 FR-007 | Done | [NAME] |
| B-003 | Customer cancel refund tiers (≥7d/3–7d/<3d) | 004 FR-010 | Todo | [NAME] |
| B-004 | Manager modify Confirmed booking (dates/room) | 004 FR-015 | Todo | [NAME] |
| B-005 | Prefer manager `/api/v1` check-in/out; deprecate legacy shortcuts | 004 FR-012–014 | Todo | [NAME] |

## Priority P1 — Next sprints

| ID | Item | Spec | Status | Owner |
|----|------|------|--------|-------|
| B-010 | Availability calendar 8 statuses + date-range blocks | 005 | Backlog | |
| B-011 | Contract Outbox + immutable PDF URL | 010 | Backlog | |
| B-012 | VNPay reconciliation job + bank-transfer reminder | 012 | Backlog | |
| B-013 | Invoice / billing layer | 011 + 012 | Backlog | |
| B-014 | About CMS backend API (if FE already shipped) | — / FE | Backlog | |
| B-015 | Reporting: occupancy live + booking trends page | 016 | Backlog | |

## Priority P2 — Later

| ID | Item | Spec | Status |
|----|------|------|--------|
| B-020 | Promotion API contract align (`PATCH .../active`) | 018 | Backlog |
| B-021 | Damage checkout path after fee paid | 004 / 023 | Backlog |
| B-022 | E2E critical path: book → deposit → check-in | testing | Backlog |

## Done (archive)

| ID | Item | Sprint | Notes |
|----|------|--------|-------|
| | | | |

## How to add

1. Link to a `sdd/services/homestay/features/.../spec.md` FR when possible
2. Set Status: `Backlog` | `Ready` | `In Progress` | `Blocked` | `Done`
3. Move to sprint plan only after Monday Spec Sync approval
