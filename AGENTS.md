# NixUra — Agent Instructions

NixUra is an offline-first habit app for **Android and iOS** (Kotlin Multiplatform, Compose Multiplatform): quit bad habits (live streak counters, cards that lighten over time), build good habits (daily counters + calendar), count down to events (with reminders), plus an offline rule-based coach. Product name: **NixUra**. Technical ids (Gradle `ReclaimApp`, package `com.example.reclaimapp`) stay unchanged unless explicitly approved.

## Read these first

| File | Role |
|------|------|
| [docs/DEVELOPMENT_STATUS.md](docs/DEVELOPMENT_STATUS.md) | **Start here:** what happened, what is verified vs unverified, what is left |
| [docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) | Product + architecture summary, data model, decisions |
| [docs/PHASE2_ARCHITECTURE.md](docs/PHASE2_ARCHITECTURE.md) | Original architecture (see addendum at the end for what changed) |
| [docs/AI_COACH_UPGRADE.md](docs/AI_COACH_UPGRADE.md) | How to replace the offline coach with a real AI |
| [README.md](README.md) | Developer overview, features, build/test commands |

**Always inspect the repository and git status** before acting. Do not trust chat history alone.

## Current state in one line

The merged UI + features (good habits, events, profile, settings, coach, Android notifications) **are built, run and tested on Android and work properly** (owner-confirmed, 2026-10-23). The owner's remaining work: fine-tune the UI, add a real AI agent, add curated info on many bad habits with recognition of free-typed habit names, and make the app work on iOS (never built or run; iOS notifications not implemented). When asked to continue: check the "What is left" list in DEVELOPMENT_STATUS.md, run what your machine can (`:androidApp:assembleDebug`, `:shared:testAndroidHostTest`; on macOS also the iOS tasks), and never claim a build or test passed unless you ran it. Round 2 added Android home-screen widgets (quit counter, good-habit quick log, events, styled like the in-app cards, plus "Add to Home screen" buttons) in `androidApp/.../widgets/` with shared logic in `domain/widget/`. The earlier widget version was confirmed working on a device by the owner; the card redesign and the pin buttons are **unverified** (type-checked and unit-tested only). Round 2 coach/catalog work from other agents is also **unverified** unless the docs say otherwise. iOS widgets are not implemented (need macOS + WidgetKit + App Group).

## Architecture (mandatory)

- Single module `:shared` with **domain**, **data**, **presentation** packages (+ `di`). Android-only code lives in `:androidApp` (Application, Activity, notifications, home-screen widgets); iOS-only code lives in `shared/src/iosMain` and `iosApp/` (Swift host, Xcode project).
- **Domain:** pure Kotlin — models, use cases, calculators, coach engine, notification planner, repository **interfaces**. No Room, Compose, or network.
- **Data:** Room (DB version 2 with `MIGRATION_1_2`), mappers, repository **implementations**.
- **Presentation:** **MVVM** — Compose UI, ViewModels, `Strings` (all UI text), `Navigator` (no nav library). No business rules in Composables.
- **DI:** Koin (`di/AppModules.kt`; platform modules (Android and iOS) provide DB, `NotificationScheduler`, `NotificationPermissionGateway`; the Android module also registers the widget use cases).
- **Scope:** offline only — no accounts, backend or sync. The coach is offline rule-based behind `CoachResponder`; a real AI needs the owner's approval, a consent toggle and a server-side key (see AI_COACH_UPGRADE.md).

## iOS is a first-class target (approved)

Android **and** iOS are both in scope. Agents may freely read, write, fix and extend iOS code (`shared/src/iosMain`, `iosApp/`, Swift/Xcode project files, Gradle iOS targets, iOS notifications) without asking for extra permission, exactly like Android code. Rules:

- Shared behaviour goes in `commonMain`; platform differences go behind `expect`/`actual` or Koin platform modules (`platformModule()` in `androidMain` and `iosMain`). Keep Android and iOS at feature parity: when you add a platform port (e.g. `NotificationScheduler`), implement **both** sides.
- iOS notifications use `UNUserNotificationCenter` (implement `NotificationScheduler` and `NotificationPermissionGateway` in `iosMain`; the current no-op is a placeholder to replace, not a decision to keep).
- Compose on iOS: `ComposeUIViewController` in `MainViewController.kt`, SwiftUI host in `iosApp/iosApp/ContentView.swift`. Respect safe areas (use `WindowInsets.safeDrawing`, not `safeContentPadding`) and keep the status-bar style in `SystemBars.ios.kt` in sync with the theme.
- Honesty rule still applies: iOS compiles only on macOS with Xcode. If the current machine cannot run `:shared:compileKotlinIosSimulatorArm64`, `:shared:linkDebugFrameworkIosSimulatorArm64`, `iosSimulatorArm64Test` or `xcodebuild`, write the code carefully, say plainly that it is **unverified**, and list the exact commands for the owner to run on a Mac. Never claim an iOS build or test passed unless it ran.
- iOS changes follow the same documentation auto-update rule below.

## Visual identity

Palette: `#1D1A39` `#451952` `#862549` `#AE445A` `#F39F5A` `#E8BCB9`. Dark theme is default (background = navy). Semantic tokens: `presentation/theme/NixColors.kt`.

Quit-habit card background follows the **active streak duration** (derived, never stored): default brand gradient via `StreakColorMapper`, or a user-chosen color via `ShadeRamp` (starts ~30% brightness, ends light). Thresholds 0/1/3/7/14/30 days in `StreakColorProgressionConfig`. Keep the counter logic (`TrackingStateCalculator`, 1 s tick in `HabitsViewModel`) intact.

## Coding

- SOLID, Clean Architecture, small focused changes. No speculative abstractions.
- UI text goes through `presentation/strings/Strings.kt`; no hard-coded literals in screens.
- Do not rename technical identifiers or change major dependency versions silently.
- Room entity changes require a new migration + schema check.
- Prefer files written with a real compile: this repo has no CI yet.

## Testing & verification

- Pure logic has tests in `shared/src/commonTest` (day math, coach, planner, scores, shade ramp, counters, tracking, streak colors).
- Run relevant Gradle tasks before claiming success; report failures honestly.
- Android: `:androidApp:assembleDebug`, `:shared:testAndroidHostTest`. iOS (macOS only): `:shared:iosSimulatorArm64Test`, `:shared:linkDebugFrameworkIosSimulatorArm64`, then build/run `iosApp/iosApp.xcodeproj` in Xcode.
- Room schema changes: the app is not released yet, so after a schema change at the same version, uninstall the app on devices/simulators before running (see README troubleshooting); never add destructive fallback.

## Git & GitHub

After **verified** scoped work: Conventional Commits, inspect diff for secrets/debug, update `docs/DEVELOPMENT_STATUS.md`, commit, push to configured remote when safe. **No force-push**, no secrets. Report commit hash and actual push status.

## Documentation auto-update rule (mandatory)

After **any** modification — code, build files, database, UI text, bug fix, new file, removed file — check whether documentation is now out of date and **update it in the same change, without being asked**:

| If you changed... | Update... |
|---|---|
| Features, screens, behaviour | `README.md` (Features table), `docs/PROJECT_CONTEXT.md` |
| Architecture, packages, DI, navigation | `README.md` (Architecture, structure), `docs/PROJECT_CONTEXT.md`, `docs/PHASE2_ARCHITECTURE.md` addendum |
| iOS code, Xcode project, iOS notifications | `README.md` (Status, iOS section), `docs/DEVELOPMENT_STATUS.md` (iOS verification state), `docs/PROJECT_CONTEXT.md` |
| Room entities/DAOs/migrations | `README.md`, `docs/PROJECT_CONTEXT.md` (persistence), `docs/DEVELOPMENT_STATUS.md`; re-run `python3 tools/room_migration_check.py` and update it if entities changed |
| Dependencies, versions, build/test commands | `README.md` (stack, build, testing) |
| Bugs found or fixed, test/build results, new risks | `docs/DEVELOPMENT_STATUS.md` (what was done, verification state, what is left, known risks) |
| Workflow or rules | `AGENTS.md` and the matching skill in `.cursor/skills/` |
| AI/coach behaviour | `docs/AI_COACH_UPGRADE.md` |

Keep the docs truthful: never record a build or test as passing unless it ran; mark unverified work as unverified. Mention in your final reply which docs you updated.

## README and docs

Keep README accurate: implemented vs planned only; no fabricated versions or test results. When state changes, update DEVELOPMENT_STATUS.md first (see the auto-update rule above).

## Ambiguity

If product or architecture is unclear, stop and ask. Do not expand scope without approval. If chat contradicts docs, surface the conflict — do not silently overwrite documentation.

## Project skills (`.cursor/skills/`)

Invoke explicitly in a new chat:

- `nixura-project-onboarding` — recover context
- `nixura-feature-development` — implement within authorized scope
- `nixura-verification-and-release` — verify, commit, push
- `nixura-context-handoff` — update status before long handoff
