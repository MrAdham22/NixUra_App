# NixUra — Phase 2 Architecture (Approved)

**Status:** Approved  
**Supersedes:** Informal chat-only drafts  
**Summary authority:** [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for day-to-day handoffs; this file for detailed architectural decisions.

## Product

- **NixUra** — quit/unwanted habit change; event-sourced tracking; relapse preserves history.
- KMP targeting Android and iOS (Android built first); Compose Multiplatform; offline MVP; no backend/accounts in tracking MVP.

## Module structure

Single `:shared` module:

- **domain** — entities, validators, `TrackingStateCalculator`, use cases, repository interfaces, `Clock`.
- **data** — Room entities/DAOs (Phase 4+), mappers, `@Transaction` writes, preference stores.
- **presentation** — **MVVM:** Compose views, ViewModels (UI state/events), `NixUraTheme`, components, `StreakColorMapper`, screens. ViewModels call domain use cases only.

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

Hybrid: six palette stops with interpolation within bands. Configurable thresholds (provisional: **1d, 3d, 7d, 14d, 30d**). Reset to dark stop on relapse (active elapsed only). **Thresholds** (time bounds) and **stage-indicator milestones** are separate concepts; stage semantics may be refined after Phase 3 review.

## AI (deferred)

Backend-held credentials; introduce repository/use case when requirements defined — **not** empty stubs in tracking MVP.

## Room KMP gate (before Phase 4 persistence)

Proposed: Room 3.0.3, KSP 2.3.12, bundled SQLite — **must verify** against Kotlin 2.4.20, AGP 9.1.1, current `:shared` plugin setup. On failure: report and propose alternatives; no silent stack changes.

## Testing

Domain streak/edit/multi-habit/timezone; presentation color mapper; Room DAO/migration on Android host when implemented.

## Git / docs process

See [AGENTS.md](../AGENTS.md) and [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md).

---

## Addendum — October 2026

The Phase 2 architecture still holds (domain / data / presentation, MVVM, Koin, Room KMP). Changes since:

- Room schema is now **v2** with extra tables (good habits, logs, events, coach messages, preferences) and a hand-written migration.
- New domain areas: `time`, `coach`, `notification`; new ports: `NotificationScheduler`, `NotificationPermissionGateway`, `CoachResponder`, `TimeZoneOffsetProvider`.
- Navigation is a small custom `Navigator` (no navigation library).
- `HabitsScreen` was replaced by `BadHabitsScreen`; `AppRoot` (presentation/screen/AppShell.kt) is the root composable.
- "No AI/notifications in MVP" is superseded: an offline coach and Android local notifications exist. Online AI remains out of scope.
- iOS is an approved target: iOS-specific code lives in `shared/src/iosMain` and `iosApp/`; notifications get an iOS implementation of the same ports.
- Android home-screen widgets (Round 2): plain-Kotlin widget logic in `domain/widget` + `WidgetUseCases`, Android rendering in `androidApp/.../widgets` (RemoteViews, Chronometer clock, one inexact refresh alarm), use cases registered in the Android `platformModule`. No Room or navigation-library change; deep links reuse `Navigator`; a `WidgetPinner` port (common) backs the "Add to Home screen" button, with an Android-only implementation. iOS widgets are planned (WidgetKit + App Group, macOS only).
- Cloud sync (Wave 2): pure logic and ports in `domain/sync`, implementations in `data/sync`, wired in `di/AuthModule.kt`. `BackgroundSyncScheduler` is WorkManager on Android (`androidApp/.../sync`) and a no-op on iOS. Room is v3. `AuthorizedApi` is now one shared Koin instance.
- User-review redesign: Room is v4 (`coach_conversations`). Screen transitions live in `navigation/NavTransitions.kt` (`AnimatedContent` keyed by route and stack depth); `AuthHost` takes its route as a parameter so an outgoing screen keeps showing itself; `AuthGate` is the signed-out gate. `presentation/guide` holds the starter guide, whose `guideTarget` modifier only measures while the guide runs. The first-launch onboarding screen was removed. Touch response is one modifier, `components/Motion.kt` `pressable`.
- Round 3: Room is v5 (`MIGRATION_4_5`: money tracker columns on `habits`, source columns on `events`). New packages: `domain/money`, `domain/integration`, `data/integration`, `presentation/deletion`, `presentation/integrations`. New route `Route.Integrations`. Android: `integrations/AndroidGoogleAuthorizer` (play-services-auth, the Authorization API) wired from `MainActivity` for Google's consent screen; `MainActivity` is `singleTask` with a `nixura://integrations` link so the browser can come back after Notion's consent. The habit catalog is warmed on a background thread at start (`di/KoinInit.warmUpInBackground`).
- Firebase accounts (2026-11-08): `data/auth` talks to Firebase Authentication over its REST API (`FirebaseAuthApi` + `KtorFirebaseTransport`, `FirebaseSession` hands out the 1-hour ID token with a single-flight refresh, `FirebaseAuthRepository` implements `AuthRepository`); `AuthRepositoryImpl`, `AuthErrorMapper` and the Devices screen are gone. New ports: `SocialSignIn` (Android `auth/AndroidSocialSignIn`: Credential Manager for Google, Facebook Login; iOS: not available) and `UsageStats` (`domain/analytics`; Android `FirebaseUsageStats`, iOS `NoUsageStats`). Platform modules bind `FirebaseConfig` (Android from `google-services.json` via the google-services plugin, iOS from `GoogleService-Info.plist`). Routes: `Route.Auth.VerifyOtp` became `VerifyEmail`; `Route.Auth.ResetPassword` and `Route.Devices` were removed. `SyncRemote` is bound to `PausedSyncRemote` until the Firestore remote exists. No Room schema change.
- Google import feeds (2026-11-04): new port `DeviceRegion` (Android `AndroidDeviceRegion`, iOS `IosDeviceRegion`, both in the Koin platform modules); `GoogleAuthorizer.forget`; `GoogleCalendarCatalog.holidayCalendar`. `AppRoot` polls linked calendars every 2 minutes while the app is resumed. No Room schema change (categories are stored by name).
