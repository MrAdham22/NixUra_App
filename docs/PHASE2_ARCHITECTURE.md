# NixUra — Phase 2 Architecture (Approved)

**Status:** Approved  
**Supersedes:** Informal chat-only drafts  
**Summary authority:** [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for day-to-day handoffs; this file for detailed architectural decisions.

## Product

- **NixUra** — quit/unwanted habit change; event-sourced tracking; relapse preserves history.
- Android-first KMP; Compose Multiplatform; offline MVP; no backend/accounts in tracking MVP.

## Module structure

Single `:shared` module:

- **domain** — entities, validators, `TrackingStateCalculator`, use cases, repository interfaces, `Clock`.
- **data** — Room entities/DAOs (Phase 4+), mappers, `@Transaction` writes, preference stores.
- **presentation** — `NixUraTheme`, components, ViewModels, `StreakColorMapper`, screens.

Dependency direction: presentation → domain ← data.

## Tracking model

- Events: `TRACKING_START`, `RELAPSE` with UTC epoch storage.
- **Current elapsed**, **longest streak**, **completed periods**, **milestones** — all **derived**; never authoritative DB columns for streak or card color.
- Continuous elapsed duration (not calendar-day streaks unless product changes).
- Relapse: new active period at event time; history and longest streak preserved.
- Edit/delete events → full recalculation; protect sole `TRACKING_START` on active habits (proposed invariant).
- Habit **archive** only — no cascade delete of events (FK RESTRICT).

## Schema v1 (planned)

`HabitEntity`, `HabitEventEntity` only. Notes optional; no tag table in MVP.

## Transactions

`CreateHabitAndStartTracking` = single DB transaction (habit + initial START).

## Live timer

Persisted `activePeriodStart` + `Clock`; UI ticker in ViewModel; **no** DB writes for display ticks.

## Habit-card colors (presentation)

Hybrid: six palette stops with interpolation within bands. Configurable thresholds (provisional: 24h, 3d, 7d, 14d, 30d). Reset to dark stop on relapse (active elapsed only).

## AI (deferred)

Backend-held credentials; introduce repository/use case when requirements defined — **not** empty stubs in tracking MVP.

## Room KMP gate (before Phase 4 persistence)

Proposed: Room 3.0.3, KSP 2.3.12, bundled SQLite — **must verify** against Kotlin 2.4.20, AGP 9.1.1, current `:shared` plugin setup. On failure: report and propose alternatives; no silent stack changes.

## Testing

Domain streak/edit/multi-habit/timezone; presentation color mapper; Room DAO/migration on Android host when implemented.

## Git / docs process

See [AGENTS.md](../AGENTS.md) and [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md).
