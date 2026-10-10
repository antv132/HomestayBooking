# Tech Debt Tracker

Last updated: 2026-07-23  
Severity: Critical | High | Medium | Low

| ID | Severity | Description | Area | Introduced | Target sprint | Status |
|----|----------|-------------|------|------------|---------------|--------|
| TD-001 | High | No-show job missing — room stays locked if guest never arrives | Booking | Pre-Sprint 2 | Sprint 2 | Done |
| TD-002 | High | Customer cancel flat 50% vs tiered FR-010 | Booking | Legacy | Sprint 2 | Done |
| TD-003 | Medium | Hold timeout default 10 min — specs aligned to code (was 30 in older docs) | Booking/config | Config | Sprint 2 | Done |
| TD-004 | Medium | Legacy check-in/out bypasses inspection / HK | Booking API | Legacy | Sprint 2–3 | Done |
| TD-005 | Medium | Manager modify Confirmed booking (FR-015) missing | Booking | Gap | Sprint 2 | Done |
| TD-006 | Medium | Contract generation sync; Outbox not used | Contract | Gap | Sprint 3+ | Done |
| TD-007 | Medium | Calendar API incomplete vs 8-status model | Availability | Gap | Sprint 3+ | Done |
| TD-008 | Low | Agent stubs under `docs/` removed — canonical `AGENTS.md` / `CLAUDE.md` at root (Option A) | Docs | 2026-07-23 | — | Done |
| TD-009 | Low | Spec Kit `.specify/memory/constitution.md` filled v1.0.0 — keep aligned with root `CONSTITUTION.md` | Docs | 2026-07-23 | — | Done |
| TD-010 | Medium | **Edu scope cut:** VNPay reconciliation cron (15m) + Admin SCR-52 + Sync from VNPay (012 FR-010/011). Keep **Return URL** verify + idempotent Paid. Reopen only if mentor rubric requires. | Payment | 2026-07-23 Spec Sync | Post-edu / optional | Deferred |
| TD-011 | Medium | **Edu scope cut:** Online payment = **VNPay only** — drop bank transfer + biên lai + Manager CK verify. **Exception (kept 2026-07-23):** cash remaining tại quầy check-in (`remainingCollected` / `Payment.Method.CASH`) — SCR-37. Canonical SCR: SCR-20 = Order Review VNPay; SCR-37 = Check-in/out (not CK verify). Agents must not build CK UX unless Spec Sync reopens. | Payment/Booking | 2026-07-23 Spec Sync | Post-edu / optional | Deferred |
| TD-012 | High | FE Vitest/lint scripts wired in `package.json` (`lint`, `test`/`test:run`/`test:coverage`); smoke Vitest only — FE coverage gate still deferred; e2e Playwright still off | Frontend/CI | 2026-07-23 | Sprint 2–3 | Done |
| TD-013 | High | BE critical path JaCoCo (Auth/Booking/Payment) below 80% — Coverage Agents + `mvn -Pcoverage-critical verify`; CI currently continue-on-error until green | Testing | 2026-07-23 | Sprint 2 | Done |
| TD-012b | Medium | **Edu scope cut:** VNPay **IPN** server callback — not required for sandbox/edu. Canonical confirm path = **Return URL** only (`/api/payments/vnpay/return`). Agents must not build IPN unless mentor/rubric reopens. *(Renamed from duplicate TD-012 to avoid clash with FE scripts debt.)* | Payment | 2026-07-23 Spec Sync | Post-edu / optional | Deferred |

## Rules

- New debt from Agent Jam must be logged the same Friday Demo
- Critical debt blocks release of related feature
- Resolving debt = PR link + Status `Done`
