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

- Android and iOS are both target platforms. Android was built first and runs; iOS is approved work (bring-up + notifications), not yet built or run.
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

KMP + Compose Multiplatform + Koin + Room KMP (Phase 4, after compatibility gate). **Clean Architecture** in `:shared`, with **MVVM** in the presentation layer:

- **View:** Compose screens/components render state and forward user events.
- **ViewModel:** Exposes observable UI state, invokes domain use cases (no Room/DAO access).
- **Model (domain):** Use cases, entities, and rules — not Android-specific types in shared domain code.

Not every screen must have a ViewModel immediately (e.g. static previews); production flows should use ViewModels as features land.

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

**Display format** (Phase 3 preview via `formatElapsed` in presentation): under 1 hour `MM:SS`; from 1 hour until 1 day `HH:MM:SS`; from 1 day `Dd HH:MM:SS` (leading zeros on time fields).

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

---

## Update — October 2026 (supersedes the MVP scope above where they conflict)

The owner approved expanding the app. Current scope:

- **Quit habits** (original core) with user-chosen card colors (`ShadeRamp`), analysis screen, recovery timeline from an offline knowledge base, backdated start, edit/archive.
- **Good habits:** daily target counters (`good_habits`, `good_habit_logs` keyed by local day index), streaks, 7/30-day completion, calendar with green done squares, past-day edits.
- **Events:** countdown/count-up, categories, multiple reminder lead times (`events.reminders` = comma-separated millis), Android local notifications.
- **Coach:** offline, rule-based (`CoachEngine`, `HabitKnowledgeBase`) behind `CoachResponder`; chat persisted in `coach_messages`; crisis messages answered locally with a support message; alcohol/gambling carry safety notes. No network.
- **Profile & settings:** name/email/age stored locally in `preferences` (key/value); settings: dark/light, language (English only), notifications, daily check-in time, erase all data; first-run onboarding.
- **Scores (derived, not stored):** "Breaking free" = average progress of active quit habits toward 30 days; "Discipline" = weighted good-habit completion over 7 days; Momentum = average of available scores.

Still **out of scope:** accounts, backend, cloud sync, online AI, ads/analytics.

Persistence: Room **v2** — tables `habits` (+`color_argb`), `habit_events`, `good_habits`, `good_habit_logs` (unique `good_habit_id`+`day_index`), `events`, `coach_messages`, `preferences`. `MIGRATION_1_2` is hand-written. No destructive fallback.

Time: local "day index" = days since 1970-01-01 in the device time zone (`DayCalculator`, offset from the platform: `java.util.TimeZone` on Android, `NSTimeZone` on iOS).

Notifications: `NotificationPlanner` (pure) builds event reminders + next two streak milestones per habit + 14 daily check-ins; `NotificationSyncService` re-plans while the app is open; Android `AndroidNotificationScheduler` arms `AlarmManager` (inexact) and `BootReceiver` restores after reboot. iOS: currently a no-op placeholder; the plan is a `UNUserNotificationCenter`-based `NotificationScheduler`/`NotificationPermissionGateway` in `iosMain` (calendar/time-interval triggers; iOS allows at most 64 pending local notifications, so the planner output must be capped there).

Navigation chrome: the top bar (tab screens only) shows the logo + "NixUra" on the left and the profile avatar on the right. Settings is no longer in the top bar: it is opened by a gear icon (`NixIcon.Gear`, rendered with the official Material `Icons.Filled.Settings`) in the top-right `actions` slot of the Profile screen header (`SubScreen`), and back from Settings returns to Profile. `NixIcon.Sliders` was removed.

Display format for counters: days → hours → minutes → seconds blocks from the largest non-zero unit (`CounterParts`); the older `formatElapsed` string format is still used in a few places.

Concurrency notes: `EnsureCoachGreetingUseCase` and `UpdateSettingsUseCase` use a `Mutex` (the first prevents a duplicate greeting when the view model and screen both trigger it; the second serializes settings read-modify-write). Persistence changes are checked with `tools/room_migration_check.py`.
