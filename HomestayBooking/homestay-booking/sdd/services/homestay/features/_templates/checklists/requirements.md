# Specification Quality Checklist: [FEATURE NAME]

**Purpose**: Validate specification completeness before planning / implementation  
**Created**: [YYYY-MM-DD]  
**Feature**: [spec.md](../spec.md) | [CONTEXT.md](../CONTEXT.md)

## Content Quality

- [ ] No implementation details (languages, frameworks, APIs) in FR/SC
- [ ] Focused on user value and business needs
- [ ] Written for non-technical stakeholders
- [ ] All mandatory sections completed
- [ ] CONTEXT.md completed (Problem → Open Questions)

## Requirement Completeness

- [ ] No blocker `[NEEDS CLARIFICATION]` markers remain (or explicitly deferred in Assumptions)
- [ ] Requirements are testable and unambiguous
- [ ] Success criteria are measurable
- [ ] Success criteria are technology-agnostic
- [ ] All acceptance scenarios are defined (Given/When/Then)
- [ ] Edge cases are identified
- [ ] Scope is clearly bounded (Out of Scope filled if needed)
- [ ] Dependencies and assumptions identified

## Feature Readiness

- [ ] All functional requirements have clear acceptance criteria
- [ ] User scenarios cover primary flows with priorities (P1…)
- [ ] Feature meets measurable outcomes in Success Criteria
- [ ] Spec Sync **Approved** before `/speckit-plan` or coding

## Notes

-
