# NixUra — Project Context

Canonical product and architecture reference. **Authoritative detail:** [PHASE2_ARCHITECTURE.md](PHASE2_ARCHITECTURE.md). **Current state:** [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md).

## 1. Product

**NixUra** (user-facing) — help users quit or reduce unwanted habits by tracking time since last occurrence, visualizing progress, and (later) AI coaching. Name evokes removing negative patterns and reclaiming control.

**Technical identifiers (unchanged):** Gradle `ReclaimApp`, modules `:androidApp` / `:shared`, package `com.example.reclaimapp`.

## 2. Problem & experience

Users need persistent, honest progress (not a fragile timer), relapse support without shame erasure, and a premium, warm UI. MVP focuses on reliable offline quit tracking with a distinctive visual identity.

## 3. MVP scope

**In:** multi-habit model (UI may emphasize one habit), onboarding (later), dashboard timer, relapse logging, history edit/delete, archive habit, NixUra design system, streak-based habit-card colors.

**Deferred:** AI coach, backend, accounts, cloud sync, reduction goals UX, event tags, notifications/milestones infrastructure, `CoachRepository` stubs.

## 4. Confirmed product decisions

- Android-first; iOS in repo for later.
- Offline-first; no accounts in tracking MVP.
- Event history = source of truth.
- Relapse resets **active** period only; history + longest streak remain.
- Tracking start: now or backdated last occurrence (`TRACKING_START` vs `RELAPSE`).
- Continuous elapsed active streak.
- Transactional habit + initial START creation.
- Palette applies **app-wide**; habit-card background additionally follows active streak.

## 5. Palette & design principles

| Token | Hex |
|-------|-----|
| Navy | `#1D1A39` |
| Purple | `#451952` |
| Plum | `#862549` |
| Rose | `#AE445A` |
| Orange | `#F39F5A` |
| Blush | `#E8BCB9` |

Premium, warm, hierarchical; not minimal-empty; not cluttered. Semantic tokens for surfaces, actions, text, states. Error color exception documented in theme (not from palette).

## 6. Habit-card streak colors

- Derived from **active streak duration** via `StreakColorMapper` (presentation).
- Six-stop progression dark → light; provisional thresholds 1d, 3d, 7d, 14d, 30d (configurable).
- Deterministic; survives recompute after edits; resets visually on relapse.
- Text + stage indicator — not color alone.

## 7. Technical architecture

KMP + Compose Multiplatform + Koin + Room KMP (Phase 4, after compatibility gate). Clean Architecture in `:shared`.

## 8. Package responsibilities

See [PHASE2_ARCHITECTURE.md](PHASE2_ARCHITECTURE.md).

## 9. Domain (planned / partial)

**Models:** `Habit`, `HabitEvent`, `TrackingState`, enums `GoalType`, `HabitEventType`.

**Repositories (interfaces):** `HabitRepository`, `HabitEventRepository`, `TrackingRepository`, `UserPreferencesRepository`.

**Use cases:** `CreateHabitAndStartTracking`, `RecordRelapse`, `UpdateHabitEvent`, `DeleteHabitEvent`, `ArchiveHabit`, observe dashboard state.

**Services:** `TrackingStateCalculator`, `HabitEventValidator`, `MilestoneEvaluator` (thresholds TBD).

## 10. Streak & validation

Distinct: current elapsed, completed period lengths, longest streak, milestone eligibility. Validator: one START per active habit lifecycle rules, no future-dated events beyond tolerance.

## 11. Persistence (planned)

Room v1: `habits`, `habit_events`. Schema export; explicit migrations; no destructive fallback in release.

## 12. Live timer

ViewModel combines event-driven Flow + periodic tick; domain/calculator uses `Clock` and persisted anchors.

## 13. AI (future)

Backend proxy; domain use case + data client when specified — not in tracking MVP.

## 14. Testing & compatibility

Unit tests for calculator and color progression; Room tests after Phase 4 gate. Room/KSP/Kotlin/AGP verification required before persistence.

## 15. Open / provisional

- Color stage thresholds (review in Phase 3).
- Onboarding fields, coaching styles, milestone schedule.
- `unarchiveHabit` in MVP?
- License for README.
- DB file naming at Room implementation.
