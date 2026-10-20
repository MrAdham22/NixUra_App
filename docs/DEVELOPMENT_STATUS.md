# NixUra — Development Status

**Last updated:** 2026-10-20 (offline tracking vertical slice — **pending commit**)

## Current phase

**Phase 4 (initial slice)** — Room KMP persistence, domain tracking logic, MVVM habits screen on Android.  
AI, accounts, backend, sync — **not in scope**.

## Implemented (this slice)

- **Persistence:** Room 2.7.2 + KSP 2.3.12, `habits` / `habit_events`, transactional create + initial `TRACKING_START`.
- **Domain:** event-sourced `TrackingStateCalculator`, use cases (create habit, observe tracking, record relapse).
- **Presentation:** `HabitsScreen` + `HabitsViewModel` (live 1s tick), reuses `HabitCard` / streak colors / `formatElapsed`.
- **DI:** Koin (Android `MainActivity`, iOS `MainViewController`).
- **Phase 3 polish retained:** timer formats, milestone stage indicator, card contrast fixes.

## Verification (2026-10-20)

| Command | Result |
|---------|--------|
| `:shared:testAndroidHostTest --rerun-tasks` | **PASS** — 30 tests, 0 failures |
| `:shared:compileAndroidMain` | **PASS** |
| `:androidApp:assembleDebug` | **PASS** |

## Not yet implemented

- Multi-habit UX polish, event edit/delete, archive, onboarding.
- Room schema export file (Gradle warning; `exportSchema = true` but schema dir not emitted to repo yet).
- iOS runtime verification on Windows.

## Git

- Branch: `master`
- Commit/push: see latest agent report after authorized commit.

**Phase 4 full roadmap** (notifications, all MVP use cases) — incremental; not complete in one slice.
