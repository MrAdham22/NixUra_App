---
name: nixura-feature-development
description: Implements NixUra features within the authorized phase using Clean Architecture. Use when building or extending NixUra functionality after context is loaded.
disable-model-invocation: true
---

# NixUra feature development

Does **not** grant product approval — follow user authorization and [DEVELOPMENT_STATUS.md](../../../docs/DEVELOPMENT_STATUS.md).

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md) and relevant sections of [PROJECT_CONTEXT.md](../../../docs/PROJECT_CONTEXT.md).
2. Confirm the feature is within the **currently authorized phase**.
3. Review existing code in the affected packages before editing.
4. Ask the user about materially ambiguous requirements.
5. For substantial tasks, propose a short implementation plan first.
6. Respect domain / data / presentation boundaries; no Room in domain; no business logic in Composables.
7. Avoid speculative abstractions and unnecessary dependencies.
8. Implement incrementally; add tests for business rules and presentation mappers.
9. After verification, update [DEVELOPMENT_STATUS.md](../../../docs/DEVELOPMENT_STATUS.md) and README if needed.
10. Use [nixura-verification-and-release](nixura-verification-and-release/SKILL.md) for commit/push when appropriate.

## Phase guards

- **Phase 3:** theme and UI only — no Room Gradle changes.
- **Phase 4+:** Room only after compatibility gate passes and is authorized.
