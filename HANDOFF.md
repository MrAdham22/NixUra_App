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

**Status (2026-11-17): done, checked on the owner's phone and on an emulator.** Branch `claude/s2-form-animations-e6598c`.

- Files (all new unless marked):
  - `presentation/components/themeanim/`: `HabitThemeAnimation.kt` (`HabitThemeBackdrop`, `ThemeScene`, the one
    mapping `sceneFor(kind)`, scene clock that pauses when the page is not shown, fade helper), `WaterScene.kt`,
    `FootprintsScene.kt`, `EnergyScene.kt`, `PagesScene.kt`, `NightSkyScene.kt`, `GoldenRaysScene.kt`,
    `ThemeAnimMath.kt` (pure wave/particle math). A new theme = one scene file + one line in `sceneFor`.
  - `presentation/platform/ThemeHaptics.kt` + `.android.kt` + `.ios.kt` (pour tick, "full" confirm);
    `presentation/platform/DeviceTilt.kt` + `.android.kt` (accelerometer at `SENSOR_DELAY_UI`, only while resumed)
    + `.ios.kt` (CoreMotion at 10 Hz).
  - `presentation/goodhabits/form/`: `WaterGoalForm.kt` (pure state: ml goal, unit, cup size, steps, limits),
    `WaterGoalSection.kt` (the card + 4 previews), `ThemeBackdropPreviews.kt` (still frame of every theme, dark/light,
    LTR/RTL).
  - Changed: `presentation/screen/GoodHabitFormScreen.kt`; strings between the Session 2 markers in `Strings.kt` and
    `ArabicStrings.kt` (water goal texts only; the animations have no text).
  - Tests: `commonTest/.../components/themeanim/ThemeAnimMathTest.kt`, `commonTest/.../goodhabits/form/WaterGoalFormTest.kt`.
- How it works: the form calls `vm.previewTheme(name)` 400 ms after typing stops and plays
  `HabitThemes.visualFor(theme, colors).animation` behind the whole form (no name matching in the screen). Water:
  stream from the top centre with a splash → the page fills (two sine layers, 2.4 s) → stream stops, haptic, bubbles,
  one fish (swims the other way in RTL); no light rays (removed on owner feedback); tap = ripple; slight tilt; another name drains it (0.9 s).
  Editing a water habit fills to 15 % + 85 % of today's progress. Prayer keeps `PrayerGoldWash` and gets the rays on
  top. Taps are only observed (initial pass, never consumed). "Remove animations" draws a still frame. Every moving
  value is read in the draw phase, so frames redraw without recomposing.
- Water goal: daily water habit (preview = WATER, not weekly) → the goal card slides in under the name and replaces
  the "Daily target" stepper. Liters/Cups (goal kept in ml; switching never changes it), goal 250 ml–6 L in 250 ml
  steps (litres) or whole cups, cup size 100–1000 ml in 50 ml steps, defaults 2 L / 250 ml / cups. Every save of a water
  habit passes `water = goal` and `dailyTarget = goal.servingsPerDay`. Editing prefills from `GoodHabitUi.water`; if
  the cup size changes, a note says logged days will be counted with the new size (S1's design).
- How to test: Build → + → type "drink water", "watr", "اشرب مياه" (page fills; inputs, chips, steppers and the
  keyboard keep working; tap the water for a ripple); delete the name (water drains); "walk the dog", "workout",
  "read", "sleep early", "pray" for the other themes; switch Liters/Cups and step the goal; turn on "Remove
  animations" (still water). Edit a water habit: goal and cup size come back, page fills only partly.
- Verified: `:androidApp:assembleDebug` OK; `:shared:testAndroidHostTest` 578 tests, 0 failures (16 new).
  `:androidApp:installDebug` on the owner's phone (CPH2531, Android 15) and an emulator (Pixel 9 AVD, Android 17).
  On the phone: "drink water", "watr" and "اشرب مياه" (typed on the Arabic keyboard) fill the page; the field, chips,
  steppers and keyboard stay usable; tap ripple works; left without saving (no data changed). On the emulator: pour,
  fill, drain, footprints, the Liters/Cups toggle and steppers, edit prefill, and save → reopen keeps 2.25 L / 300 ml.
- Not verified: iOS (haptics and tilt `actual`s written but not compiled: run `:shared:compileKotlinIosSimulatorArm64`
  and `:shared:linkDebugFrameworkIosSimulatorArm64` on a Mac); tilt by hand (emulator and phone were flat on a desk);
  a frame-time measurement (looked smooth on the phone, not profiled); the workout/reading/sleep/prayer-rays scenes on
  the phone (checked in previews and code only); "Remove animations" on a device; the Compose previews in the IDE.
- Notes for the README / docs at merge: Features: "Add Good Habit: themed background animations (water fills the
  screen, walk, workout, reading, sleep, prayer) and a daily water goal in litres or cups with a cup size".
  Architecture: `presentation/components/themeanim` (scenes) and `presentation/goodhabits/form` (water goal form);
  new platform ports `ThemeHaptics` and `DeviceTilt` (Android + iOS). No Room change. `GoodHabitsViewModel` unchanged.
- Round 2 (owner feedback, 2026-11-17): water light rays removed (`8484d12`); the walk footprints redesigned
  (`108a9fe`): bare footprints walk an S path from the open bottom of the page, behind the form, out past the title;
  one print every 34 dp, left/right feet either side of the path and turned to its heading; each presses in, holds,
  fades; stronger on both themes (0.55 dark / 0.65 light); mirrored in RTL; whole still trail with "remove animations".
  The water summary line now uses the deeper blue on the light theme (`43b76be`). Verified: assembleDebug OK,
  testAndroidHostTest 580 tests / 0 failures; on the emulator only (not the phone, by the lead's request): water without
  rays (dark and light), footprints in dark and light. RTL footprints checked in code and previews only (the app stays
  LTR on a device until Session 4 turns on the Arabic layout).
- Round 3 (owner feedback on the footprints, 2026-11-17, `b837c9e`): the trail walks **down** the page, toes first,
  from the top end corner beside the title, behind the form, out through the open bottom. Two walkers: the second is the
  mirror image and starts 5.5 s later, so the trails cross. Each print presses in, stays 2.6 s and fades over 2.6 s
  (about 6 pairs per walker on screen). Left and right feet are mirror-image sole outlines (arch and big toe on the inner
  side) turned 9° outwards. Still frame = one moment of both walks; RTL mirror and draw-phase reads unchanged.
  Verified: assembleDebug OK, testAndroidHostTest 582 / 0 failures; on the emulator (dark theme): direction, several
  pairs, two lanes, distinct left/right prints. Not on the phone (lead installs the merged build).

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

**Status (2026-11-17): done on the emulator; not yet on the owner's phone (it was not connected).** Branch `claude/cranky-goodall-94eb7c`
(reset onto `388ab00` of the plan branch first: the worktree had been cut from an older commit). Commits: `889882c` (switching, RTL,
font, digits), `30d069b` (all strings, offline coach, notifications), `6dd01d3` (٪ and RTL previews), `a798b7f` (Arabic
catalog), `b3e0e31` (LocaleManager fix, typed-name digits, notification test), then the docs commit.

- **Language switching:** `AppSettings.languageCode` = `system` (new default) / `en` / `ar`, synced as before; `digitStyle` is new and per
  device (`settings.digits`, no schema change). `domain/locale/AppLocales.resolve` picks the language; `AppShell` provides strings,
  `LayoutDirection` and `LocalAppLocale` at the root, so a switch applies at once. Android: `AndroidAppLocaleGateway` uses
  `LocaleManager` on 13+ (AppCompat's `setApplicationLocales` does nothing without an AppCompatActivity, and MainActivity is a
  ComponentActivity: checked on the emulator), `res/xml/locales_config.xml`, `android:localeConfig`, `configChanges="locale|layoutDirection"`;
  a language chosen in the phone's settings is taken back on resume. Before Android 13 the app still switches (its own setting),
  the system just has no record. iOS: "System default" follows `NSLocale.preferredLanguages` (also the iOS per-app language, offered
  through `CFBundleLocalizations` en/ar in Info.plist); unverified.
- **Text:** every `Strings` member has an Arabic override (`ArabicCoverageTest`). Domain error messages are mapped in
  `ArabicStrings.errorText`. The offline coach (`CoachEngine`, `StopTimelineExplainer`), notification texts and the habit catalog follow
  `ContentLanguage`. **Catalog: everything is translated** (48 categories + the general profile: labels, why it matters, every
  milestone title and detail, craving tips, safety notes) in `domain/coach/catalog/ar/*`, keyed by id in the English milestone order;
  matching is untouched. Still English: the legal drafts (Privacy Policy, Terms: a note says so, they need a lawyer's version), the
  AI coach's replies (they come from the online model in whatever language the user writes), habit names the user typed, and
  `DesignPreviewScreen` (debug).
- **RTL:** start/end everywhere; `NixIconView` mirrors `Back`/`Chevron` (`autoMirror = false` where a chevron is rotated); Session 3's
  manual calendar mirror removed (it would have doubled); `rememberWeekStart()` now reads the app language; widgets set their root
  direction; the legal text keeps LTR inside the RTL screen.
- **Numbers, dates, font:** dates and times are built by `Strings.dateText / clockText / dateTimeText / monthYear` (Arabic:
  «الثلاثاء، 20 أكتوبر 2026 · 8:30 م»); `presentation/components/Text` (used everywhere instead of Material's) draws digits as chosen
  (٫ decimal, ٪ percent; emails, links and digits glued to Latin letters stay as typed) and gives Arabic IBM Plex Sans Arabic, zero
  letter spacing and ≥ 1.5× line height. The widget Chronometer keeps the device's digits. Arabic "+300 مل" uses a left-to-right mark.
- **Previews:** `presentation/locale/LocalePreviews.kt` (Home parts, detail stats and calendar; Arabic dark/light, Arabic-Indic digits,
  English). The screens read ViewModels from Koin, so their building blocks are previewed. Compiled, not rendered in an IDE.
- **Verified:** assembleDebug OK; testAndroidHostTest **623 tests, 0 failures** (new: `AppLocaleTest`, `ArabicCoverageTest`,
  `ArabicCatalogTest`, an Arabic `NotificationPlannerTest`). On `emulator-5554` (Android 16): everything listed under "Done when"
  except the phone-only items below, dark and light, both digit styles; pseudo-locales ar-XB → Arabic, en-XA → English. Test quit habit
  "S4test smoking" deleted; the emulator was left in English, dark, Western digits.
- **Not verified:** the owner's phone (install with `:androidApp:installDebug` and switch Settings → Language → العربية); Firebase
  sign-in/sync after a switch (needs an account; the code path is unchanged apart from the synced `languageCode` value `system`);
  widgets on a home screen and a fired notification in Arabic (code + tests); iOS (Mac commands in DEVELOPMENT_STATUS).
- **Arabic to review (owner):** «الإقلاع / البناء» as tab names; «صامد منذ» for "Clear for"; «تعثّر / تعثّرت» used for relapse and "I
  slipped" everywhere; «رغبة قوية؟» for "Craving?"; «طاب يومك» for "Good afternoon"; «لنمحُ الهالة السلبية» (owner asked for an Arabic word instead of "Aura", 2026-11-18; «الهالة السلبية» blows away like "bad Aura") for the tagline "Let's Nix the
  bad Aura"; the short time units «3ي 20س» / «5س 12د» and the stop-explorer chips «1س…1ش»; «رسالة
  اليوم» for the daily check-in; «الزخم / التحرّر / الانضباط» for the three scores; «ارتقِ / تطلّع / تحرّر» eyebrows; the weekday letters
  «ن ث ر خ ج س ح»; Egyptian-style month names (يناير…ديسمبر) instead of the Levantine ones; «اللّي» (shisha mouthpiece, Egyptian
  word) in a shisha tip; the medical safety notes in `ArabicDrugTexts.kt` (worth a second reader).
- **Follow-ups:** an Arabic legal text; a pseudo-locale view of the shared `Strings` (Android pseudo-locales only touch `res` strings);
  digit setting for the widget Chronometer; let iOS write the in-app choice to the iOS per-app language.
