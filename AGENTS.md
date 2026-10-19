# NixUra — Agent Instructions

NixUra is an offline-first habit-quitting app (Kotlin Multiplatform, Compose Multiplatform). Product name: **NixUra**. Technical ids (Gradle `ReclaimApp`, package `com.example.reclaimapp`) stay unchanged unless explicitly approved.

## Read these first

| File | Role |
|------|------|
| [docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) | Canonical product + architecture summary |
| [docs/DEVELOPMENT_STATUS.md](docs/DEVELOPMENT_STATUS.md) | Current phase, tasks, verification, git state |
| [docs/PHASE2_ARCHITECTURE.md](docs/PHASE2_ARCHITECTURE.md) | Approved detailed architecture |
| [README.md](README.md) | Public developer overview |

**Always inspect the repository and git status** before acting. Do not trust chat history alone.

## Architecture (mandatory)

- Single module `:shared` with **domain**, **data**, **presentation** packages.
- **Domain:** pure Kotlin — models, use cases, calculators, repository **interfaces**. No Room, Compose, or network.
- **Data:** Room (when implemented), mappers, repository **implementations**, transactions.
- **Presentation:** Compose, ViewModels, theme, `StreakColorMapper`. No business rules in Composables.
- **DI:** Koin.
- **MVP:** offline tracking only — no accounts, backend, AI, or sync until authorized.

## Visual identity

Official palette (app-wide tokens, not habit-card-only):

`#1D1A39` `#451952` `#862549` `#AE445A` `#F39F5A` `#E8BCB9`

Habit-card **background** progresses dark→light from **active streak duration** (derived, not stored in DB). App chrome uses semantic theme tokens; only the card background follows streak progression.

Provisional thresholds: 1d, 3d, 7d, 14d, 30d — configurable via `StreakColorProgressionConfig`.

## Coding

- SOLID, Clean Architecture, small focused changes.
- No speculative abstractions (e.g. empty AI interfaces before AI phase).
- No Gradle/Room until compatibility gate passes and phase is authorized.
- Do not rename technical identifiers or change major dependency versions silently.

## Testing & verification

- Domain: streak math, validation, fixed clock.
- Presentation: streak color progression logic.
- Run relevant Gradle tasks before claiming success; report failures honestly.

## Git & GitHub

After **verified** scoped work: Conventional Commits, inspect diff for secrets/debug, update `docs/DEVELOPMENT_STATUS.md`, commit, push to configured remote when safe. **No force-push**, no secrets, respect branch protection. Report commit hash and actual push status.

## README

Keep [README.md](README.md) accurate: implemented vs planned only; no fabricated versions or test results.

## Ambiguity

If product or architecture is unclear, stop and ask. Do not expand MVP without approval. If chat contradicts approved docs, surface the conflict — do not silently overwrite documentation.

## Project skills (`.cursor/skills/`)

Invoke explicitly in a new chat:

- `nixura-project-onboarding` — recover context
- `nixura-feature-development` — implement within authorized phase
- `nixura-verification-and-release` — verify, commit, push
- `nixura-context-handoff` — update status before long handoff
