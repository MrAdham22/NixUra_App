# NixUra

**NixUra** helps people quit or reduce unwanted habits by tracking time since the last occurrence, visualizing progress, and (planned) offering personalized coaching. The name combines *nix* (remove) with *aura* — removing negative patterns and reclaiming control.

> **Note:** The Gradle project is still named `ReclaimApp` and the Kotlin package is `com.example.reclaimapp` until a deliberate rename is approved.

## Status

| Area | State |
|------|--------|
| Phase 2 architecture | Approved — see [docs/PHASE2_ARCHITECTURE.md](docs/PHASE2_ARCHITECTURE.md) |
| Phase 3 design system | In review — NixUra theme, habit card, design preview screen |
| Habit tracking / Room persistence | Not started (Phase 4+) |
| AI coach / backend | Deferred |

## Core features

### Implemented

- **Design preview (Android):** Application-wide `NixUraTheme` (Material 3), official palette tokens, `HabitCard` with streak-based background progression, stage indicator, simulated elapsed timer and relapse reset for demo purposes.
- **Streak color logic:** Configurable thresholds (provisional: 1d, 3d, 7d, 14d, 30d) with unit tests for progression segments.

### Planned (MVP)

- Offline quit tracking with Room KMP, multi-habit model, relapse logging, history edit/delete, archive habit, onboarding, production dashboard wired to domain use cases.

### Deferred

- AI coaching, user accounts, cloud sync, reduction-goal UX, event tags, notifications.

## Visual identity

Official six-color palette (app-wide):

| Color | Hex |
|-------|-----|
| Deep navy | `#1D1A39` |
| Deep purple | `#451952` |
| Muted plum | `#862549` |
| Rose | `#AE445A` |
| Warm orange | `#F39F5A` |
| Blush pink | `#E8BCB9` |

Habit-card **backgrounds** additionally interpolate across these stops as the **active streak duration** increases. App chrome uses fixed semantic theme tokens.

## Technology stack

Verified in project configuration:

| Technology | Version |
|------------|---------|
| Kotlin | 2.4.20 |
| Compose Multiplatform | 1.12.1 |
| Android Gradle Plugin | 9.1.1 |
| Material 3 (Compose) | 1.12.0-alpha03 |
| minSdk / compileSdk | 24 / 37 |

**Planned (not yet in Gradle):** Room KMP, Koin — after compatibility gate in Phase 4.

## Architecture

Single `:shared` module with packages:

- **`domain`** — models, use cases, streak calculation, repository interfaces (pure Kotlin).
- **`data`** — Room, mappers, repository implementations (Phase 4+).
- **`presentation`** — Compose UI, theme, ViewModels.

`:androidApp` is the Android entry point. `:iosApp` hosts shared UI for future iOS work.

See [docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) and [AGENTS.md](AGENTS.md) for agent/developer instructions.

## Project structure

```
Habbit_app/
├── AGENTS.md                 # AI agent entry instructions
├── androidApp/               # Android application module
├── docs/
│   ├── PROJECT_CONTEXT.md
│   ├── DEVELOPMENT_STATUS.md
│   └── PHASE2_ARCHITECTURE.md
├── iosApp/                   # iOS Xcode project
├── shared/                   # KMP shared code + UI
│   └── src/commonMain/kotlin/com/example/reclaimapp/
│       ├── presentation/     # theme, components, screens
│       └── App.kt
└── .cursor/skills/           # NixUra workflow skills
```

## Prerequisites

- **JDK 11+** (project uses JVM 11 target)
- **Android Studio** or Android SDK + command-line tools
- **Windows, macOS, or Linux** for Android builds; **macOS + Xcode** required to build/run iOS

## Build and run (Android)

From the repository root:

```bash
# Windows
gradlew.bat :androidApp:assembleDebug

# macOS / Linux
./gradlew :androidApp:assembleDebug
```

Install the debug APK from `androidApp/build/outputs/apk/debug/` or run from Android Studio using the **androidApp** configuration.

The app opens the **NixUra design preview** screen (Phase 3).

## Testing

```bash
gradlew.bat :shared:testAndroidHostTest
gradlew.bat :shared:allTests
```

On Windows, iOS simulator tests are skipped (host limitation).

## Roadmap

1. ~~Phase 2 — Architecture~~ (approved)
2. **Phase 3 — Design system** (current — awaiting product review)
3. Phase 4 — Room KMP compatibility gate + domain/data foundation
4. Phase 5 — End-to-end offline tracking MVP
5. Later — AI coach via backend, sync, iOS release hardening

## Development workflow

- Read [AGENTS.md](AGENTS.md) and [docs/DEVELOPMENT_STATUS.md](docs/DEVELOPMENT_STATUS.md) when starting work.
- Use Conventional Commits; verify builds/tests before commit; push only when safe (see verification skill).
- Invoke Cursor skills under `.cursor/skills/` (e.g. `nixura-project-onboarding`) in new chats.

## License

Not yet selected — no license file is included.
