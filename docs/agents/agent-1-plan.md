# Agent 1 plan: swipe navigation, droplet nav bar, reminder edit/remove

Written before coding, from `NixUra_Agent_Tasks.md` section 5 and the repo docs (README, AGENTS, PROJECT_CONTEXT).

## What the code looked like
- Single `:shared` KMP module, Compose Multiplatform. Navigation is a tiny `Navigator` (back stack of `Route`s, no nav library); `AppShell.kt` cross-fades between routes and draws the top bar and a hand-made bottom bar.
- Bottom bar has five tabs: Home, Quit, Build, Events, Coach. The Coach is a tab in the bar but renders full-screen (no chrome).
- Event reminders are a list of lead times stored on the event (`events.reminders`, CSV). `NotificationSyncService` observes the database and calls `NotificationScheduler.replaceAll(plan)`; the Android scheduler cancels everything it armed before (stored plan) and arms the new plan. So reminder consistency = "change the stored list, the plan follows".
- Room schema is **not** touched by this task (Agent 3 owns it).

## Files
Created: `presentation/navigation/MainTabs.kt`, `presentation/components/{DropletNavBar,DropletMath}.kt`, `presentation/platform/ReducedMotion.kt` (+ android/ios actuals), `presentation/screen/EventRemindersSection.kt`, `domain/notification/{ReminderRules,ReminderCapabilities}.kt`, tests (see handoff).
Modified: `AppShell.kt` (pager host), `EventFormScreen.kt` (reminders section, undo snackbar), `EventsViewModel.kt`, `EventUseCases.kt` (+`EventReminderEditor`), `Strings.kt` (additive block), `AppModules.kt` (2 lines), both `PlatformModule` files (1 line each), Android `AndroidNotificationPermission`, `AndroidNotificationScheduler`, `BootReceiver`, `AndroidManifest.xml`.

## Approach
1. **Pager.** `HorizontalPager` over Home/Quit/Build/Events. `Navigator` stays the source of truth (Home's "See all", widget deep links and back keep working). navigator -> pager via `animateScrollToPage`; pager -> navigator on `settledPage` only. Coach and all pushed routes stay normal destinations.
2. **Droplet.** One tapered path drawn in the draw phase. Leading edge = `currentPage + currentPageOffsetFraction` (finger-following); tail = under-damped spring chasing it (stretch, contract, overshoot). No recomposition per frame, one reused `Path`, no per-frame allocations.
3. **Reminders.** Editing an existing event persists each reminder change at once through `EventReminderEditor` (validated by pure `ReminderRules`, serialized with a mutex); the existing sync service then cancels and re-arms. New events keep a draft list saved with the event.
4. **Permissions.** `ReminderCapabilities` reports blocked notifications / missing exact-alarm permission; the UI explains and opens the right system screen. Event reminders use exact alarms only when allowed, else the old inexact alarm.

## Risks
- Nothing can be compiled in this environment (no Gradle/kotlinc/network): everything is unverified until the owner builds.
- Pager + `Crossfade` interplay and the droplet's look need a device.
- `SCHEDULE_EXACT_ALARM` has a Play policy angle (see handoff).

## Testing
Pure-logic unit tests in `commonTest` (rules, editor use case with a fake repository, planner key contract, tab mapping + back behaviour, droplet math). No Compose UI test infrastructure exists in the repo (no test dependencies for it), so the UI test for edit + delete + undo is a manual checklist in the handoff.
