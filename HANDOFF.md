# Handoff: 2 bug fixes + 5 features (2026-11-17)

Session 1 ran first. **Sessions 2 and 3 run at the same time** (owner decision 2026-11-17), each in its own
worktree and branch cut from `claude/nixora-bugs-features-plan-9a1220`; the lead session merges both back into that
branch. Session 4 starts only after that merge. Each session reads this file first, works only in the files it owns,
and updates its own section ("Status") before it ends.

### Rules while Sessions 2 and 3 run in parallel

- Touch only the files your section owns. If you need a change in a file you do not own, stop and say so in your
  final report instead of editing it.
- New strings go **only between your own markers** in `Strings.kt` and `ArabicStrings.kt`
  ("---- Session 2 ..." / "---- Session 3 ..."), so the two branches merge without conflicts.
- Shared docs (`README.md`, `docs/*.md`, `AGENTS.md`): **do not edit**. Write what changed, how to test it and
  what is verified in **your own Status block in this file**; the lead session copies it into the docs at merge.
- No Room schema change (the v8 columns are all you need). If one seems needed, stop and report.
- `GoodHabitsViewModel` is shared and already has what both need (`GoodHabitUi.theme`, `GoodHabitUi.water`,
  `previewTheme(name)`, `onCreate`/`onUpdate(..., water)`, `onChangeToday(id, ±1)`). Do not edit it; if it lacks
  something, add the logic in a new file of your own (a small helper or use case) and report it.
- Installing on the phone is fine (both builds are Room v8); the phone shows whichever branch was installed last.

## Rules for every session

- Read `AGENTS.md`, `docs/DEVELOPMENT_STATUS.md` (top section) and this file before touching code.
- **Strings:** all UI text goes in `shared/.../presentation/strings/Strings.kt` (English, `open val` in the body)
  **and** in `ArabicStrings.kt` (an override in the same session). The app is Kotlin Multiplatform, so text is
  shared with iOS; Android `res/values/strings.xml` holds only the launcher/widget-picker labels that Android
  itself reads. Arabic plurals: `ArabicPlurals.category(n)` (zero / one / two / few / many / other).
- **Add-only shared files** (any session may append, never rewrite another session's lines): `Strings.kt`,
  `ArabicStrings.kt`, `README.md`, `docs/*.md`, this file.
- Palette stays as in `NixUraPalette.kt` (`#862549`, not `#662549`). Name: **NixUra**.
- Animations: Compose + Canvas only, check `rememberReducedMotion()` and show a static version when it is on.
- Every finished bug fix / feature: its own Conventional Commit, run `:androidApp:assembleDebug` and
  `:shared:testAndroidHostTest`, install on the phone (`:androidApp:installDebug`). iOS is unverified on Windows.
- Theme of a good habit: always read `GoodHabit.theme` (stored) and `HabitThemes.visualFor(theme)`. Never match
  the name again in a screen, never write `if (water)` colour checks in a screen.

---

## Session 1: bugs + shared foundation + shimmer

**Goal:** Bug 1 (starter guide highlights), Bug 2 (Coach mic), the HabitTheme system stored on the habit,
the water-goal data model and math, Feature 1 (Home loading shimmer).

**Owns:** `presentation/guide/*`, `presentation/screen/HomeScreen.kt`, `presentation/screen/CoachScreen.kt`,
`domain/recognition/*`, `domain/theme/*` (new), `domain/model/GoodHabit.kt`, `data/local/**` (entities,
migrations, DB version), `data/mapper/*`, `data/sync/FirestoreSyncMapping.kt`, `presentation/components/Shimmer.kt`
(new), `presentation/goodhabits/GoodHabitsViewModel.kt` (loading + theme), `presentation/strings/ArabicStrings.kt`
(new), `presentation/strings/ArabicPlurals.kt` (new), `androidApp/src/main/AndroidManifest.xml`.

**Must not touch:** `GoodHabitFormScreen.kt`, `GoodHabitDetailScreen.kt`, `MonthCalendar`, good-habit cards.

**Done when:** each guide step's highlight matches its text with and without habits; the Coach has no mic;
habits store a theme (Room v8, old data kept); water habits have a goal in ml; Home shows a skeleton only
after 300 ms of real loading; build + tests pass; phone install done.

**Status (2026-11-17): done, not yet run on a device.**

- Built: `GuideStep`/`guideSteps` (text + target together, `GuideTarget.StartHere`), Coach Send-only + `adjustResize`,
  `domain/theme/HabitTheme.kt` (`HabitTheme`, `HabitThemeDetector`), `domain/theme/WaterGoal.kt` (`WaterGoal`,
  `WaterUnit`), Room v8 (`theme`, `water_goal_ml`, `water_serving_ml`, `water_unit` on `good_habits`; synced in
  `GoodHabitPayload`), `presentation/appearance/HabitThemeVisuals.kt` (`HabitThemes.visualFor`,
  `HabitThemeAnimationKind`), `components/Shimmer.kt` (`Modifier.shimmer`, `SkeletonLine`, `SkeletonCircle`),
  `screen/HomeLoading.kt` (`homePhase`, `rememberShowSkeleton`, `HomeSkeleton` + 4 previews), `ArabicStrings`,
  `ArabicPlurals`.
- Verified: `:androidApp:assembleDebug` OK; `:shared:testAndroidHostTest` 562 OK; `tools/room_migration_check.py`
  OK for the new columns/queries (2 old `events` query warnings from the script, not new).
- Not verified: install on the phone (none connected), the guide and the Coach on a device, Room v7→v8 on a device
  with real data, the Home load time on a phone, iOS.
- `GoodHabitFillStyleDetector` is gone: use `habit.theme.fillStyle`.

**Notes for Session 2:**
- `CreateGoodHabitUseCase` / `UpdateGoodHabitUseCase` already take `water: WaterGoal?`. The form must pass the goal on
  **every save of a water habit, edits included**; `null` means "N cups of 250 ml from dailyTarget" and would reset a
  custom cup size. The use case sets `dailyTarget = goal.servingsPerDay` itself.
- To show the water section live while typing, detect with `HabitThemeDetector.themeFor(name)` off the main thread
  (debounced ~400 ms); saving detects again, so the stored theme always matches the saved name.
- A day's log counts cups of `servingMl`. Changing the cup size re-reads past days with the new size (by design for
  now; tell the owner if the form makes that visible).
- Every new string: English in `Strings.kt` and Arabic in `ArabicStrings.kt`.

## Session 2: Add/Edit form (Feature 2 + Feature 5 form part)

**Goal:** the theme animations on the Add Good Habit screen (water pour/fill/alive, drain, ripple, optional
tilt, haptics; walk footprints, workout particles, reading pages, sleep stars, prayer rays) as one reusable
`HabitThemeAnimation`, and the water daily-goal section (unit toggle L/cups, goal picker, cup size) that slides
in under the name field, also used when editing.

**Owns:** `presentation/screen/GoodHabitFormScreen.kt`, `presentation/components/themeanim/*` (new),
platform haptics/tilt `expect`/`actual` (new files under `presentation/platform/` in common, android and ios source
sets), any new file under `presentation/goodhabits/form/` (new). The save path is already done in Session 1.

**Must not touch:** detail screen, calendar, cards, Home.

**Depends on:** S1's `HabitTheme`, `HabitThemeDetector`, `WaterGoal`, the v8 columns.

## Session 3: details, calendar, cards (Feature 3 + Feature 5 display part)

**Goal:** calendar days coloured by `HabitThemes.visualFor(theme)` with intensity = count / target (water:
% of the ml goal), a less→more legend, week start by locale (Saturday in Arabic) and RTL grid; water progress
"1.2 / 2 L" / "5 / 8 cups" with +1 cup / +250 ml and undo on the card, the Home row and the details screen.

**Owns:** `presentation/screen/GoodHabitDetailScreen.kt`, `MonthCalendar` (in `presentation/components`),
good-habit card/row composables, `presentation/screen/GoodHabitsScreen.kt`, the `TodayRow` composable in
`HomeScreen.kt` (that function only), `presentation/appearance/HabitThemeVisuals.kt` (to refine shades),
`presentation/appearance/GoodFillColors.kt`, any new file under `presentation/goodhabits/water/` (new).

**Depends on:** S1 foundation only (it does not need Session 2).

## Session 4: Arabic + RTL (Feature 4), last

**Goal:** register `ArabicStrings` in `availableLanguages`, fill every remaining override (screens, dialogs,
errors, notifications, widgets, shortcuts, coach offline replies, timeline info), per-app language
(`AppCompatDelegate.setApplicationLocales` + `locales_config.xml` on Android, per-app language on iOS),
System / English / العربية in Settings applied without restart, RTL check of every screen, locale dates,
digits setting (Western default / Arabic-Indic), Arabic font with safe line height, pseudo-locales in debug.

**Owns:** everything, since it runs last.
