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
9. After every change (verified or not), update README / DEVELOPMENT_STATUS / PROJECT_CONTEXT as required by the auto-update rule below — automatically.
10. Use [nixura-verification-and-release](nixura-verification-and-release/SKILL.md) for commit/push when appropriate.

## Scope guards

- Offline only: no accounts, backend, sync or online AI without explicit owner approval.
- New UI text goes in `presentation/strings/Strings.kt`.
- Room entity changes need a new migration and a schema check; never use destructive fallback.
- The merged UI/features build and run on Android (owner-confirmed 2026-10-23). **iOS is in scope and unrestricted**: you may implement and fix iOS code (`iosMain`, `iosApp/`, iOS notifications via `UNUserNotificationCenter`) like any other code, keeping Android/iOS parity via `expect`/`actual` or Koin platform modules. iOS only compiles on macOS; if you cannot run it, mark iOS work **unverified** and give the owner the Mac commands (see AGENTS.md).
- Insets: use `WindowInsets.safeDrawing`, never `safeContentPadding()` (it adds gesture-inset gaps).
- The coach is offline behind `CoachResponder`; online AI follows `docs/AI_COACH_UPGRADE.md`.

## Documentation auto-update (mandatory, every time)

After **any** modification, decide whether `README.md` or other docs are now stale and update them **automatically, in the same change, without waiting to be asked**. The full table of "changed X → update Y" is in [AGENTS.md](../../../AGENTS.md) ("Documentation auto-update rule"). At minimum: README for features/architecture/stack/build/test changes; `docs/DEVELOPMENT_STATUS.md` for fixes, bugs found, verification results, risks and the "what is left" list; `docs/PROJECT_CONTEXT.md` for scope/data-model changes. Say which docs you updated in your final reply.
