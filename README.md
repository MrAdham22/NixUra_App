# NixUra

**NixUra** (Android and iOS) helps people break bad habits, build good ones and look forward to important moments. It tracks the exact time (days, hours, minutes, seconds) since the last slip, shows progress with cards that lighten as a streak grows, tracks daily good habits on a calendar, counts down to life events with reminders, and includes an on-device AI-style coach.

> **Naming:** the product is **NixUra**. The Gradle project is still `ReclaimApp`, the Kotlin package is `com.example.reclaimapp`, and the Android launcher label is still "ReclaimApp" until a deliberate rename is approved. Change `applicationId`/package before publishing to a store.

## Status (read this first)

| Area | State |
|------|-------|
| Offline quit tracking (Room, MVVM, Koin) | Built and **verified earlier** (30 tests, `assembleDebug` passed on the author's machine before the UI merge) |
| NixUra UI merge + all features below | **Built, run and tested on Android — working properly (owner-confirmed).** First-run bugs fixed: stale Room database crash, screen padding from gesture insets. The Android app is considered feature-complete for the current scope. |
| Pure-logic unit tests (day math, coach, planner, scores, shade ramp, counters) | 71 tests compiled with plain `kotlinc` 2.4.20 and **passed** outside Gradle. They also live in `shared/src/commonTest`. |
| Room v2 schema + `MIGRATION_1_2` | Audited with `tools/room_migration_check.py` (real SQLite: migration vs entities, all 25 queries, erase order) — **OK**. Works on Android (owner-tested). |
| AI coach | Offline rule-based coach ships and works. A **real AI agent is the next planned feature** (see roadmap); guide in [docs/AI_COACH_UPGRADE.md](docs/AI_COACH_UPGRADE.md) |
| iOS | **Approved target, not yet built or run.** Shared UI/logic is written for it (Compose host in `iosApp`, `iosMain` actuals). Reminders are a no-op placeholder until a `UNUserNotificationCenter` implementation is added. Needs macOS + Xcode to verify. |

If you are an AI assistant or new developer: read [AGENTS.md](AGENTS.md), then [docs/DEVELOPMENT_STATUS.md](docs/DEVELOPMENT_STATUS.md) (what happened, what is unverified, what is left).

## Features

All screens use the NixUra palette, dark theme by default (navy `#1D1A39` background), with a light theme switch in Settings.

| Screen | What it does |
|--------|--------------|
| Onboarding | Welcome + optional name (first launch only) |
| Home | Greeting, coach "today's focus", momentum score + 7-day bars, previews of quit habits, good habits and the next event |
| Quit (bad habits) | Cards with live days/hours/minutes/seconds. Card starts dark and lightens over 0/1/3/7/14/30 days. Pick your own card color (starts ~30% brightness of it, ends in a light tint) or use the default brand gradient. |
| Quit habit analysis | Big counter, progress to next shade, stats (current/best streak, relapses, tracking since), coach note, body & mind recovery timeline, relapse history, edit / remove / record relapse |
| Add/Edit quit habit | Name (the coach "understands" it by keyword category), color picker + shade preview, backdated start ("last time was...") |
| Build (good habits) | Daily target counters (+/-), 7-day strip, streak, green squares |
| Good habit analysis | Month calendar (green = done, light green = partial), tap any past day to correct it, 7/30-day completion, best streak, totals |
| Events | Live countdown cards (count-up after the date), multiple reminders per event (preset + custom lead times) |
| AI Coach | **Full-screen** (top bar and bottom nav hidden; compact header with back arrow, mode line and clear-chat; system back returns to Home). Offline chat (persisted), "today's focus" strip you tap to pick which quit habit to talk about (the coach then uses that habit's data), quick prompts, craving/relapse/progress/body/plan replies, crisis safety reply |
| Profile | Opened from the avatar in the top bar. Name, email, age (local only), "Breaking free" and "Discipline" scores, totals, nearest event countdown. Gear icon at the top right opens Settings |
| Settings | Reached from the gear icon on the Profile screen (back returns to Profile). Dark/Light, language (English only for now), notifications + daily check-in time, erase all data, about |

Notifications: event reminders, streak milestones and a daily check-in. **Android:** implemented with `AlarmManager`, restored after reboot. **iOS:** not implemented yet (planned with `UNUserNotificationCenter`).

## Roadmap (what remains)

Everything above is working on Android. The owner's remaining work, in no fixed order:

1. **Fine-tune the UI.** Polish spacing, typography, animations, small-screen and light-theme details across all screens.
2. **A real AI agent in the app.** Replace/augment the offline `RuleBasedCoachResponder` with an actual AI behind the existing `CoachResponder` interface, with a consent toggle, an offline fallback and a server-side key (design in [docs/AI_COACH_UPGRADE.md](docs/AI_COACH_UPGRADE.md)).
3. **Richer knowledge about specific bad habits, understood from free text.** Add curated info (effects, withdrawal, recovery timeline, tips, triggers) for many more habits, and recognise a habit however the user typed it (synonyms, typos, different phrasing, multi-word names, other languages) so each habit feels personalised: the habit card, analysis screen, coach replies and milestones adapt to it. Today the coach matches the name by simple keyword category (`HabitKnowledgeBase`); this item generalises that.
4. **Make the app work on iOS.** Build and run `iosApp` on a Mac, fix first-run errors, implement iOS notifications, verify layout/status bar on a real iPhone, then TestFlight.

Smaller backlog (not blocking): Arabic/RTL and translated content, accessibility labels, store readiness (application id/package, icon, privacy policy), more tests, widgets/backup. Details: [docs/DEVELOPMENT_STATUS.md](docs/DEVELOPMENT_STATUS.md).

## Visual identity

| Color | Hex |
|-------|-----|
| Deep navy | `#1D1A39` |
| Deep purple | `#451952` |
| Muted plum | `#862549` |
| Rose | `#AE445A` |
| Warm orange | `#F39F5A` |
| Blush pink | `#E8BCB9` |

Semantic tokens (dark/light) are in `presentation/theme/NixColors.kt`. UI was adapted from a v0 design ("NixUra UI", a Next.js mock-up) into Compose; icons are drawn in code (`components/NixIcons.kt`); the settings gear is the official Material "Settings" icon from `material-icons-core`. Icon libraries are allowed.

## Technology stack

| Technology | Version |
|------------|---------|
| Kotlin | 2.4.20 |
| Compose Multiplatform | 1.12.1 |
| Android Gradle Plugin | 9.1.1 |
| Material 3 (Compose) | 1.12.0-alpha03 |
| Room KMP / KSP | 2.7.2 / 2.3.12 |
| Koin | 4.0.1 |
| Material icons core (`org.jetbrains.compose.material:material-icons-core`, used for the settings gear) | 1.7.3 |
| minSdk / compileSdk | 24 / 37 |

## Architecture

Single `:shared` module, Clean Architecture + MVVM:

- **`domain`** (pure Kotlin): `model`, `calculator` (streaks, scores, counters, good-habit stats), `time` (calendar math), `coach` (offline coach + knowledge base), `notification` (planner, scheduler interfaces), `repository` interfaces, `usecase`.
- **`data`**: Room entities/DAOs/migration (`data/local`), mappers, repository implementations.
- **`presentation`**: `screen`, `components`, `theme`, `strings` (all UI text), `navigation` (tiny `Navigator`, no nav library), ViewModels per feature.
- **`di`**: Koin `appModule()`; platform modules provide the database, `NotificationScheduler` and `NotificationPermissionGateway`.
- **`:androidApp`**: `NixUraApplication` (starts Koin), `MainActivity`, `notifications/` (alarm scheduler, receivers, permission bridge).
- **iOS**: `shared/src/iosMain` (`MainViewController`, database builder, `platformModule()`, UTC offset, system bars) and `iosApp/` (SwiftUI `ContentView` hosting the Compose view controller; framework name `Shared`).

Database: Room **version 2** (`MIGRATION_1_2` in `data/local/Migrations.kt`). Tables: `habits`, `habit_events`, `good_habits`, `good_habit_logs`, `events`, `coach_messages`, `preferences`.

The coach is behind `CoachResponder` (`RuleBasedCoachResponder` today). A real AI is a second implementation; see the upgrade guide.

## Project structure

```
Habbit_app/
├── AGENTS.md                      # entry instructions for AI agents
├── androidApp/                    # Android app: Application, MainActivity, notifications, manifest
├── docs/
│   ├── PROJECT_CONTEXT.md         # product + architecture reference
│   ├── DEVELOPMENT_STATUS.md      # what happened, what is verified, what is left
│   ├── PHASE2_ARCHITECTURE.md     # original architecture (see addendum at the end)
│   └── AI_COACH_UPGRADE.md        # how to make the coach a real AI (free options)
├── iosApp/                        # iOS app: Xcode project + SwiftUI host that embeds the shared Compose UI
├── tools/room_migration_check.py  # validates MIGRATION_1_2 + DAO SQL against the entities (python3, no deps)
├── shared/src/{commonMain,androidMain,iosMain,commonTest}/kotlin/com/example/reclaimapp/
└── .cursor/skills/                # workflow skills (onboarding, feature dev, verification, handoff)
```

## Build and run

```bash
# Windows
gradlew.bat :androidApp:assembleDebug
# macOS / Linux
./gradlew :androidApp:assembleDebug
```

Prerequisites (Android): JDK 11+, Android Studio or Android SDK.

### iOS (macOS only)

Prerequisites: a Mac, Xcode, and the Kotlin Multiplatform tooling (Android Studio with the KMP plugin or IntelliJ is optional; Gradle works from the terminal).

```bash
# compile and link the shared framework for the simulator
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
# unit tests on the iOS simulator
./gradlew :shared:iosSimulatorArm64Test
```

Then open `iosApp/iosApp.xcodeproj` in Xcode, pick a simulator or a connected iPhone, set your signing team under Signing & Capabilities, and press Run. Xcode's build phase calls Gradle to build the `Shared` framework. Sharing with testers: TestFlight needs a paid Apple Developer account; a free Apple ID can install on your own connected device but the app expires after 7 days. iOS has **not been built yet**, so expect some first-run fixes.

### Troubleshooting: "Room cannot verify the data integrity"

`IllegalStateException: Room cannot verify the data integrity ... Expected identity hash: X, found: Y` means the
database file already on the device/emulator was created by an earlier build whose v2 schema differed from the
current one (the schema changed while the version stayed 2 during development). It is not a compile error.
Fix: uninstall the app (or Settings > Apps > ReclaimApp > Storage > Clear data, or `adb uninstall com.example.reclaimapp`)
and run again. Destructive fallback is deliberately **not** enabled, so release data is never wiped silently.
Once the app ships, any schema change must bump the version and add a migration.

### Troubleshooting: content does not fill the screen

If screens show large gaps on all sides, the root layout is reserving system *gesture* insets. `AppRoot` (`presentation/screen/AppShell.kt`) must use `windowInsetsPadding(WindowInsets.safeDrawing)`, not `safeContentPadding()`.

## Testing

```bash
gradlew.bat :shared:testAndroidHostTest
gradlew.bat :shared:allTests
```

Run these first after pulling the merged UI. After changing any Room entity, DAO or migration also run `python3 tools/room_migration_check.py` (edit it if you add tables). iOS simulator tests only run on macOS (they are skipped on Windows/Linux).
Manual QA checklist: onboarding; add quit habit with/without color and backdated start; watch counter and shade; relapse; add good habit and tick days; past-day edit; add event with reminders and check the notification; coach chat; theme switch; erase data; upgrade from a v1 database.

## Development workflow

- Android and iOS are both supported targets; AI agents and developers may change iOS code freely (see "iOS is a first-class target" in [AGENTS.md](AGENTS.md)). Anything not compiled on the machine used must be marked unverified.

- Read [AGENTS.md](AGENTS.md) and [docs/DEVELOPMENT_STATUS.md](docs/DEVELOPMENT_STATUS.md) when starting work.
- Conventional Commits; verify builds/tests before committing; never claim a build passed unless it ran.
- Skills in `.cursor/skills/` (e.g. `nixura-project-onboarding`) can be invoked in new chats.
- **Docs are kept current automatically:** after any change, agents (and developers) update this README and `docs/` as needed — see "Documentation auto-update rule" in [AGENTS.md](AGENTS.md).

## License

Not yet selected — no license file is included.
