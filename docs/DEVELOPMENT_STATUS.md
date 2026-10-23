# NixUra — Development Status

**Last updated:** 2026-10-23 (iOS approved as a full target; Android confirmed running)

## One-paragraph summary

NixUra (Gradle `ReclaimApp`) is a Kotlin Multiplatform + Compose app to quit bad habits, build good ones and count down to events. The offline tracking core was built and verified earlier (Room, MVVM, Koin; 30 tests and `assembleDebug` passed). On 2026-10 the owner approved a large scope expansion and asked to merge a v0-designed UI ("NixUra UI"): new screens, good habits, events with reminders, profile, settings, onboarding and an offline rule-based coach. That work was written without Gradle access, then built by the owner in Android Studio. **The owner reports (2026-10-23) that everything works properly and has been tested on Android** (after two first-run fixes: stale Room database and screen padding). **iOS has never been built or run**; the owner approved iOS as a full target with no restrictions on AI agents touching iOS code. **Remaining work (owner-stated): (1) fine-tune the UI, (2) add a real AI agent, (3) add curated info on many specific bad habits and recognise a habit from however the user typed it, (4) make the app work on iOS.**

## Owner decisions (approved)

- Scope expanded beyond the original MVP: good habits, events + reminders, profile, settings, coach chat. No accounts, no backend, no cloud sync.
- Dark theme first (navy `#1D1A39` background), light theme available in Settings.
- Palette fixed; plum stays `#862549` (the owner first typed `#662549`, then chose the repo value).
- AI coach = **offline rule-based now, real AI later**; owner wants a free AI option (see `docs/AI_COACH_UPGRADE.md`).
- Languages: English only at launch, code structured for more.
- Android and iOS are both targets (approved 2026-10-23): agents may edit iOS code freely. Android was built first. iOS reminders are currently a no-op placeholder that must be replaced with a real `UNUserNotificationCenter` implementation.
- Users can choose the bad-habit card color; card starts almost darkest shade of it and lightens day by day; default = original brand gradient.

## What was done

**Baseline (before merge, verified 2026-10-20):** Room KMP `habits`/`habit_events`, event-sourced `TrackingStateCalculator`, create/observe/relapse use cases, `HabitsViewModel` with a 1 s tick, `HabitCard` + `StreakColorMapper` + `formatElapsed`, Koin DI, 30 tests passing, `assembleDebug` passing.

**Merge session (2026-10, unverified build):**
- Domain: good habits + daily logs, events + reminder lead times, user profile/settings, coach messages; `DayCalculator`/`CivilCalendar` (own calendar math), `GoodHabitStatsCalculator`, `ScoreCalculator`, `CounterParts`, `CoachEngine` + `HabitKnowledgeBase`, `NotificationPlanner` + `NotificationSyncService`; use cases for all of it.
- Data: Room **v2** + `MIGRATION_1_2` (hand-written SQL that must match Room's generated schema), new entities/DAOs/repositories, `MaintenanceDao.clearAll`.
- Presentation: theme tokens (dark/light), 26 hand-drawn icons, ~15 components, 13 screens, `Navigator`, 6 ViewModels (`HabitsViewModel` extended, others new), `Strings` (English), `ShadeRamp` for custom colors.
- Android: `NixUraApplication`, `MainActivity` (back handling, permission launcher), `AlarmManager` scheduler, `ReminderReceiver`, `BootReceiver`, manifest permissions, dark window theme.
- Removed: old `HabitsScreen` (replaced by `BadHabitsScreen`). Kept: `DesignPreviewScreen`, `HabitCard`, `ProgressStageIndicator`, all tracking logic.
- Docs: README, AGENTS, PROJECT_CONTEXT, this file, `AI_COACH_UPGRADE.md`, PHASE2 addendum, skills updated. Rule added to AGENTS.md and every skill: after any modification, update stale docs automatically.

## Verification state (be honest)

| Check | Result |
|-------|--------|
| 71 pure-logic tests (`domain/time`, `calculator`, `coach`, `notification`, `ShadeRamp`, `StreakProgression`, `formatElapsed`) compiled with `kotlinc` 2.4.20 and run on the JVM outside Gradle | PASS |
| Static check: every `com.example.reclaimapp` import resolves; named arguments of own composables match signatures | PASS |
| Android build + launch, all screens and features, on device/emulator | **PASS per the owner** ("everything tested and working", 2026-10-23); the assistant has no Gradle and did not re-run it |
| Android notifications, Room migration/fresh database, light theme | covered by the owner's Android testing (owner-reported, details not itemised) |
| `:shared:testAndroidHostTest` / `:shared:allTests` as Gradle tasks | not itemised by the owner; the 71 pure-logic tests passed outside Gradle |
| iOS compile/link/run (`:shared:linkDebugFrameworkIosSimulatorArm64`, Xcode run of `iosApp`) | **NOT RUN** (requires macOS + Xcode) |
| iOS notifications | **NOT IMPLEMENTED** (no-op placeholder in `iosMain/di/PlatformModule.ios.kt`) |

**Room audit (2026-10-23):** `python3 tools/room_migration_check.py` rebuilds the v1 schema in SQLite, runs the statements of `MIGRATION_1_2`, and compares every table/column/type/nullability/primary key with the Kotlin entity definitions; it also checks all 25 `@Query` statements execute against the migrated schema, that DAO parameter names match the SQL, that `MaintenanceDao.clearAll` order works with foreign keys ON, and that `INSERT OR REPLACE` upserts the same day once. Result: all OK. Mappers (`encode/decodeReminders`, entity→domain) were compiled against stub Room annotations and checked. This does **not** replace running Room itself: after the first successful build look at the generated `shared/schemas/**/2.json` and compare with the migration SQL. Fixed in the audit: possible duplicate coach greeting (race) and lost settings update (read-modify-write) via mutexes; erase-all now resets navigation.

Dependency removed: `kotlinx-datetime` (its `offsetAt` API was uncertain); the UTC offset now comes from `expect fun systemUtcOffsetMillis` (Android `java.util.TimeZone`, iOS `NSTimeZone`).

Bugs already found by the owner's first build: `Pickers.kt` had a stray broken function (`Function 'padding' must have a body`) — fixed. Expect more of this kind (typos from scripted edits, API signature drift in Material 3 `1.12.0-alpha03`, Room KSP complaints).

**Runtime crash found by the owner's first run (2026-10-23):** `Room cannot verify the data integrity` (expected hash `99e7...`, found `2de0...`) at DB open. Cause: the installed database was created by an earlier dev build that also declared version 2 but with a different schema, so the identity hash differs. Not a code defect; `MIGRATION_1_2` is not involved because the on-disk version is already 2. Fix: uninstall the app / clear its data and reinstall. No code change, destructive fallback intentionally not added (AGENTS rule). Status: resolved by reinstall, **unverified by me** (no Gradle/device here). Lesson: while the schema is still changing before release, uninstall between builds; after release always bump the version + add a migration. Still to do: commit `shared/schemas/**/2.json` once generated.

**Layout bug found on first run (2026-10-23):** content did not fill the screen (large gaps on all sides). Cause: `AppRoot` used `Modifier.safeContentPadding()`, which includes the system *gesture* insets (wide side/bottom margins with gesture navigation). Fix: `windowInsetsPadding(WindowInsets.safeDrawing)` in `AppShell.kt` (status/navigation bars, display cutout, keyboard). Unverified on device by me.

## What is left (priority order)

Android is done and owner-tested. The owner's remaining work is items 1-4; the rest is backlog.

1. **Fine-tune the UI** (spacing, typography, animations, small screens, light theme, contrast).
2. **Real AI agent** behind `CoachResponder` (see `docs/AI_COACH_UPGRADE.md`): consent toggle, offline fallback to the rule-based coach, server-side key, no accounts.
3. **Bad-habit knowledge + free-text understanding:** extend `HabitKnowledgeBase` with curated info for many more habits (effects, withdrawal, recovery timeline, triggers, tips) and make habit recognition robust to however the user typed the name (normalisation, synonyms, typos/fuzzy match, multi-word and multi-language input; fall back to a generic profile and, once the AI exists, to the AI). Use it to personalise the card, analysis screen, coach and milestones. Add unit tests for the matcher.
4. **iOS (approved, unrestricted):** (a) on a Mac run `:shared:linkDebugFrameworkIosSimulatorArm64` and build/run `iosApp` in Xcode, fix errors; (b) implement `NotificationScheduler` + `NotificationPermissionGateway` with `UNUserNotificationCenter` in `iosMain` (cap at 64 pending requests); (c) check status-bar style (`SystemBars.ios.kt`), safe areas and keyboard on a real iPhone; (d) run `:shared:iosSimulatorArm64Test`; (e) before TestFlight: bundle id, signing team, app icon, `Info.plist` strings.

**Backlog (not blocking):** commit `shared/schemas/**/2.json`; accessibility labels; store readiness (new `applicationId`/package, icon, versioning, signing, privacy policy, Play Data-safety form); Arabic/RTL + translated coach content; more tests (use cases, ViewModels with fakes, DAO, UI); edit/delete individual relapses, undo, good-habit reminders, widgets, backup/export.

## Known simplifications / risks

- `MIGRATION_1_2` SQL is hand-written; if Room reports "Migration didn't properly handle", compare with the generated v2 schema JSON.
- ViewModels are Activity-scoped (shared across screens); the nav stack resets after process death.
- Daily-check-in and milestone alarms are re-planned whenever the app is open; if the app is not opened for 14 days daily check-ins stop.
- DST edge cases in `DayCalculator.startOfDayMillis` use a two-step offset guess.
- Coach replies are keyword rules in English; health text is general information, not medical advice.
- The sandbox that wrote the merge could not run Gradle; Android is owner-tested and working; every iOS path is still unverified.
- iOS limits to remember: at most 64 pending local notifications; background alarms are not available like Android `AlarmManager`; a free Apple ID install expires after 7 days (TestFlight needs a paid developer account).

## Git

Branch `master`. The merge is **not committed** (many modified/new files). Suggested commits after the build passes: `feat: merge NixUra UI and add good habits, events, coach, settings`, then `docs: update project documentation`.
