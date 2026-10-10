# Sprint 2 Plan — [START_DATE] to [END_DATE]
Goal: Complete booking lifecycle hardening — hold timeout alignment, no-show job, cancel refund tiers, and related payment/availability gaps.
Team roles this sprint: Spec Architect: [NAME] | Agent Pilot: [NAME] | Quality Gatekeeper: [NAME] | Integrator: [NAME]

## WEEK 1

### Monday (Spec Sync)
Time: [TIME] | Location: [PLACE/LINK]
Agenda:
- Review specs from backlog:
  - `sdd/services/homestay/features/004-booking-inventory/spec.md` (primary)
  - `sdd/services/homestay/features/005-availability-calendar/spec.md`
  - `sdd/services/homestay/features/010-contract-management/spec.md`
  - `sdd/services/homestay/features/012-payment-management/spec.md`
- Assign spec writing / clarify gaps: [NAME] → FR-008 No-show; [NAME] → FR-010 cancel tiers; [NAME] → FR-015 manager modify
- Approve specs ready for implementation: [list after Spec Sync]
Outcome: Approved specs for Week 1 tasks

### Wednesday (Agent Jam)
Time: [TIME] | Agent Pilot: [NAME] | Screen-share: YES
Tasks for this session:

Task 1: Booking No-show job (FR-008) — Spec: `sdd/services/homestay/features/004-booking-inventory/spec.md`
Est. time: [X] hours | Complexity: Medium
Notes: `CONFIRMED` → `NO_SHOW` after 24h past check-in; deposit kept; release inventory; date-only model needs agreed convention

Task 2: Hold timeout default = 10 min (FR-007) — Spec: `sdd/services/homestay/features/004-booking-inventory/spec.md` (aligned to code 2026-07-23)
Est. time: [X] hours | Complexity: Low
Notes: `application.yml` may still default to 10 min — align to spec

Agent setup: Cursor Agent Mode (+ Spec Kit skills `speckit-*`)
Context files to load: `AGENTS.md`, `CLAUDE.md`, `CONSTITUTION.md`, `sdd/services/homestay/features/004-booking-inventory/spec.md`

### Friday (Demo + Retro)
Demo: [Hold timeout + No-show behavior] on staging URL / local
Tech Debt Review: [Quality Gatekeeper presents findings]
Retro: Keep / Stop / Try

## WEEK 2

### Monday (Spec Sync)
Time: [TIME] | Location: [PLACE/LINK]
Agenda:
- Review Week 1 outcomes vs `sdd/services/homestay/features/004`
- Review / approve: cancel tiers (FR-010), manager modify booking (FR-015), payment reminder/reconcile notes from `sdd/services/homestay/features/012`
- Assign: [NAME] → FR-010; [NAME] → FR-015; [NAME] → availability calendar gaps (`005`)
Outcome: Approved specs for Week 2 tasks

### Wednesday (Agent Jam)
Time: [TIME] | Agent Pilot: [NAME] | Screen-share: YES
Tasks for this session:

Task 1: Customer cancel refund tiers (FR-010) — Spec: `sdd/services/homestay/features/004-booking-inventory/spec.md`
Est. time: [X] hours | Complexity: Medium
Notes: ≥7d 100% / 3–7d 50% / <3d 0% on deposit; replace flat 50% if still in code

Task 2: Manager modify Confirmed booking (FR-015) — Spec: `sdd/services/homestay/features/004-booking-inventory/spec.md`
Est. time: [X] hours | Complexity: High
Notes: change dates/room; recalc snapshot; re-lock inventory; no edit after check-in / cancel / no-show

Agent setup: Cursor Agent Mode
Context files to load: `AGENTS.md`, `CLAUDE.md`, `CONSTITUTION.md`, `sdd/services/homestay/features/004-booking-inventory/spec.md`, `sdd/services/homestay/features/012-payment-management/spec.md`

### Friday (Sprint Review + Retro + Next Sprint Planning)
Demo: [Cancel tiers + manager modify / booking management] on staging URL / local
Tech Debt Review: [Quality Gatekeeper]
Retro: Keep / Stop / Try
Next Sprint Planning: draft Sprint 3 focus (calendar 005 / contract 010 / payment jobs 012)

## SPRINT METRICS (fill at end of sprint)

Features completed: [N] / [planned]
Test coverage delta: [X]% → [Y]%
Tech debt added: [Critical: N, High: N, Medium: N]
Tech debt resolved: [list items]
Agent sessions: [N] sessions, ~[X] hours total
Retro actions from last sprint: [completed? Y/N]

---

### Backlog reference (from AGENTS.md known gaps)

- [ ] `BookingNoShowJob` / FR-008
- [ ] Customer cancel tiers / FR-010
- [ ] Manager modify Confirmed booking / FR-015
- [ ] Prefer manager `/api/v1/...` check-in/out over legacy shortcuts
- [x] Hold timeout config = 10 min (spec + code)
