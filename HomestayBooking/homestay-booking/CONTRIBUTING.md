# Contributing Guidelines (SDD + ADD)

This project adopts **Spec-Driven & Agent-Driven Development** (SDD + ADD). Before writing any code or prompting an AI agent, you must follow the SDD pipeline.

## The 4-Phase Pipeline

1. **Context Setup**: Ensure you understand the project `CONSTITUTION.md` and the current `CLAUDE.md` context.
2. **Specification (SDD)**: For every feature, write a `SPEC.md` in `sdd/services/homestay/features/<feature>/`. Use EARS Notation (`WHEN`, `THE system SHALL`, etc.). Include Acceptance Criteria and Out of Scope sections.
3. **Agentic Execution (ADD)**:
   - Run AI to generate a `PLAN.md` based on your spec.
   - Break the plan down into `TASKS.md`.
   - Implement tasks atomically using your AI Assistant (Cursor / Claude Code).
4. **Human Review & Iterate**: Always verify tests are green and acceptance criteria are met before merging.

## AI Agent Rules

- Read `AGENTS.md` and `CONSTITUTION.md` before starting any session.
- Never approve agent output that you cannot explain.
- Spec files are the ultimate source of truth. If requirements change, fix the Spec, not the code.
- Avoid "Blind Trust" and "Context Blindness".

For detailed AI capabilities and workflows, refer to the `.cursor/skills/` directory and use the `speckit-*` skills provided.
