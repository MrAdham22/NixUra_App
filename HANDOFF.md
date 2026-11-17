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

**Status (2026-11-17): done; checked on an emulator, not yet on the owner's phone.**
Branch `claude/s3-calendar-water-d163b7`: `76a6ad1` (calendar), `17ca1a9` (water display), `381f21d` (calendar step shades),
`69f33fb` (owner feedback: Undo and Change goal removed), `affa189` (owner feedback: themed completion bars), `99325a8` (two-decimal litres).

- **Feature 3, coloured calendar:** done days take `HabitThemes.visualFor(theme, colors).accent` (water blue, prayer
  gold, the rest green) in discrete steps: `DayIntensity.level(progress)` = 0 (nothing), 1–4 (part of the target),
  5 (target reached); prayer 1..5 of 5 → 1..5, water 8 cups → 1–2, 3–4, 5–6, 7, 8. Progress per day =
  `GoodHabitUi.dayProgress(count)` (water: `WaterGoal.progress`, weekly: done or not, else count / target).
  On the calendar the partial steps run from `DayIntensity.partialFloor` (the accent dimmed toward black on dark,
  faded toward the empty square on light) up to the accent, so low gold steps never turn grey-brown.
  A "Less ▢▢▢▢▢▢ More" legend replaces Done / Partly / Missed. Week start: `WeekStart.forLocale(lang, rtl)`
  (Sunday; Saturday for Arabic or an RTL layout, via `rememberWeekStart()`); the grid runs right to left in RTL and
  the month arrows are mirrored. The card week strip uses the same steps and `look.doneSquare`.
- **Shades:** `HabitThemes.WaterDark/WaterLight` (`#5BB6EC` / `#2E78C4`) and `GoldDark/GoldLight` (`#F1B451` /
  `#B5761A`, warmer than the old `#E3B24C` / `#B07D1E`, which read mustard). A full day square is ≥ 3:1 against the
  empty one in both themes; day numbers pick their ink by contrast (≥ 3.9:1 on every step). `goodFillColors` water
  done-square now uses the same blue (deeper on light theme); the home-screen widget calls it too and still builds.
- **Feature 5, water display:** card, Home row and details show `WaterProgress.text` ("1.2 / 2 L", "5 / 8 cups"),
  a labelled quick-add (`WaterAddButton`: "+1 cup", or "+250 ml" in litres; both add one serving via
  `onChangeToday(id, +1)`); the existing "−" takes one back. Details: big "1.2 / 2 L", "0.8 L to go" and the
  selected-day panel in the goal's unit; the goal is changed with the pencil (edit form). Water fill level unchanged.
- **Owner feedback (2026-11-17):** the water Undo and the details "Change goal" link were removed with their state,
  strings and the Home row callback (nothing left behind). The details "Last 7 / 30 days" bars take the theme accent
  (`HabitThemes.visualFor(habit.theme, colors).accent`, the colour of a reached calendar day): gold, blue or green.
- **Files:** `appearance/HabitThemeVisuals.kt`, `appearance/GoodFillColors.kt`, `components/HabitComponents.kt`
  (DaySquare, WeekStrip, MonthCalendar, IntensityLegend, GoodHabitCard, `goodProgressText`), new
  `components/CalendarWeek.kt` (next to MonthCalendar; not in the owned list but new, so no merge risk),
  `screen/GoodHabitDetailScreen.kt`, `screen/GoodHabitsScreen.kt` (unchanged), `screen/HomeScreen.kt` (`TodayRow` only), new
  `goodhabits/water/` (`DayProgress.kt`, `WaterProgress.kt`, `WaterQuickAdd.kt`, `WaterAndCalendarPreviews.kt`),
  strings between the Session 3 markers, tests `HabitThemeShadesTest`, `WeekStartTest`, `WaterProgressTest`, and
  one line of Session 1's `HabitThemesTest` (light-theme water accent is now the deeper blue).
- **Previews:** water / prayer / plain calendars and the water card, dark + light, LTR + RTL (RTL uses Arabic strings).
- **Verified:** `:androidApp:assembleDebug` OK and `:shared:testAndroidHostTest` 581 tests OK (2026-11-17), at each
  commit. After the owner feedback, checked on the emulator: no Undo on card / Home / details, no Change goal, and the
  details bars blue on a water habit (100% after reaching today's goal; logs put back to 0 afterwards). The bar on the
  light theme was not looked at on a device; its track is `surfaceRaised`, which a full accent beats by ≥ 3:1
  (`HabitThemeShadesTest`). Not installed on the owner's phone (the lead session installs the merged build). **Checked on the emulator (`emulator-5554`, Android 16):** water card "0 / 2.3 L" with "+300 ml" (a 2.25 L
  goal of 300 ml cups from Session 2's form), quick add 0 → 0.9 L; Home row "+300 ml"; details "0 / 2.3 L",
  "2.3 L to go"; water calendar (1/3/5/7/8/2
  cups → blue steps) and prayer calendar (1..5 → five gold steps) in dark and light theme, legend, card week strip.
  Test prayer habit deleted afterwards; the emulator's "Drink water" (Session 2's test habit) keeps the past days
  logged for this test (11–16 Nov). **Not verified:** RTL on a device (the emulator ignored "force RTL"; covered by
  previews and `WeekStartTest`), previews not rendered in an IDE, iOS (Windows). **Owner's phone:** the build of
  `17ca1a9` was installed at 18:37 (before the shade fix) and the phone was then unplugged; nothing was looked at or
  logged on it. Install the branch again and look at a water and a prayer calendar.
- **Notes for the README / docs:** Build card, Home row and details show water in L or cups with quick add + undo;
  calendar coloured by theme with a less→more legend; week starts Saturday in Arabic. `MonthCalendar` now takes
  `progressOf` + `accent` instead of `target`; `DaySquare` takes a `level`; `WeekStrip` takes `progressOf`.
- **For Session 4:** `rememberWeekStart()` reads only the layout direction; pass the app language to
  `WeekStart.forLocale` once it is selectable. Amounts are formatted with Western digits and a "." decimal.
- **Two-decimal litres (owner, 2026-11-17):** `WaterProgress.formatAmount` shows up to two decimals without
  trailing zeros ("2", "1.2", "1.25", "2.25"). The lead session changes `WaterGoal.goalInUnit` / `drunkInUnit` to
  centilitres at merge; until then this branch alone still shows "2.3 L" for 2.25 L. `WaterProgressTest` uses counts
  that read the same before and after that change.
- **Small finds, not changed:** "Last 7 / 30 days" stayed 0% with past days logged before the habit's
  creation day (existing stats rule, not this session's code).

## Session 4: Arabic + RTL (Feature 4), last

**Goal:** register `ArabicStrings` in `availableLanguages`, fill every remaining override (screens, dialogs,
errors, notifications, widgets, shortcuts, coach offline replies, timeline info), per-app language
(`AppCompatDelegate.setApplicationLocales` + `locales_config.xml` on Android, per-app language on iOS),
System / English / العربية in Settings applied without restart, RTL check of every screen, locale dates,
digits setting (Western default / Arabic-Indic), Arabic font with safe line height, pseudo-locales in debug.

**Owns:** everything, since it runs last.
