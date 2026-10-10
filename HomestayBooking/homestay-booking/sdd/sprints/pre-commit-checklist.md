# Pre-Commit Checklist (Playbook Ch.16.6)

Use before every commit/PR — especially agent-generated code.

## Must pass

- [ ] Read related `sdd/services/homestay/features/<feature>/spec.md` (and `AGENTS.md` if business logic)
- [ ] Compiles / builds (`mvn` or `npm run build` as applicable)
- [ ] No new lint / Sonar blockers
- [ ] No secrets in diff (JWT, DB password, VNPay keys, `.env`)
- [ ] DTOs used; no Entity leaked from Controller
- [ ] Validation on request body
- [ ] Error paths return proper HTTP status
- [ ] Business rules not bypassed (inventory, payment, check-in/out)
- [ ] Unit tests added/updated for service logic changed
- [ ] If Auth/Booking/Payment/VNPay touched: `mvn -Pcoverage-critical verify` (critical LINE ≥80%) — see `testing/coverage-control.md`
- [ ] Existing tests still pass
- [ ] No leftover TODO for this task scope
- [ ] PR ≤ ~400 lines (else split) — `CONSTITUTION.md` Art.4
- [ ] Author can explain the change to another teammate

## Booking / payment extra

- [ ] Status transitions only use canonical enum
- [ ] No double-booking (lock + overlap) preserved
- [ ] Refund / deposit math matches FR (tiers if FR-010)

## Agent-specific

- [ ] Human reviewed agent plan before run
- [ ] Diff reviewed line-by-line for unexpected files
- [ ] After 3–5 agent tasks: human-led refactor pass scheduled

## Sign-off

PR: [link]  
Reviewer: [NAME]  
Date: [DATE]
