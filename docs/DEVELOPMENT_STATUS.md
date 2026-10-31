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

**Mission 1 (2026-10-27, branch `mission-1-settings-gear-topbar`; first version tested by the owner on Android and reported as looking good; the Material-icon dependency change below is not yet synced or built):** top bar now has only logo/name + profile avatar; Settings moved to a gear icon at the top right of the Profile header (`SubScreen(actions = ...)`); new `NixIcon.Gear`, rendered by `NixIconView` with the official Material `Icons.Filled.Settings` (new dependency `org.jetbrains.compose.material:material-icons-core:1.7.3` in `gradle/libs.versions.toml` + `shared/build.gradle.kts`; the version was chosen from memory and could not be resolved offline, so the first Gradle sync is the check); the full-width "Settings" button at the bottom of Profile was removed (it would duplicate the gear); `NixIcon.Sliders` and its draw branch were removed (no remaining uses). Files: `AppShell.kt`, `ProfileScreen.kt`, `NixIcons.kt`. The Material-icon change is not compiled, synced or run by the assistant (no Gradle/network here). Owner to Gradle-sync, then re-run `./gradlew :androidApp:assembleDebug` and `./gradlew :shared:testAndroidHostTest`, then check on device: top bar, gear at 42 dp, Settings opens, back returns to Profile, light and dark themes.

## What is left (priority order)

Android is done and owner-tested. The owner's remaining work is items 1-4; the rest is backlog.

1. **Fine-tune the UI** (spacing, typography, animations, small screens, light theme, contrast).
2. **Real AI agent** behind `CoachResponder` (see `docs/AI_COACH_UPGRADE.md`): consent toggle, offline fallback to the rule-based coach, server-side key, no accounts.
3. **Bad-habit knowledge + free-text understanding:** extend `HabitKnowledgeBase` with curated info for many more habits (effects, withdrawal, recovery timeline, triggers, tips) and make habit recognition robust to however the user typed the name (normalisation, synonyms, typos/fuzzy match, multi-word and multi-language input; fall back to a generic profile and, once the AI exists, to the AI). Use it to personalise the card, analysis screen, coach and milestones. Add unit tests for the matcher.
4. **iOS (approved, unrestricted):** (a) on a Mac run `:shared:linkDebugFrameworkIosSimulatorArm64` and build/run `iosApp` in Xcode, fix errors; (b) implement `NotificationScheduler` + `NotificationPermissionGateway` with `UNUserNotificationCenter` in `iosMain` (cap at 64 pending requests); (c) check status-bar style (`SystemBars.ios.kt`), safe areas and keyboard on a real iPhone; (d) run `:shared:iosSimulatorArm64Test`; (d2) iOS widgets (WidgetKit + App Group; none exist yet, Android widgets are Android-only); (e) before TestFlight: bundle id, signing team, app icon, `Info.plist` strings.

**Backlog (not blocking):** commit `shared/schemas/**/2.json`; accessibility labels; store readiness (new `applicationId`/package, icon, versioning, signing, privacy policy, Play Data-safety form); Arabic/RTL + translated coach content; more tests (use cases, ViewModels with fakes, DAO, UI); edit/delete individual relapses, undo, good-habit reminders, backup/export. (Android widgets: Round 2 - Agent 5; iOS widgets still open.)

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


## Round 2 - Agent 1 (smart recognition)

**Built:** `domain/recognition/*` (normalizer, word matcher, keyword index, good-habit lexicon, polarity classifier, `HabitRecognizer`, `HabitNameHandoff`); `HabitKnowledgeBase` now merges catalogs by id and matches with typo tolerance (`match()` unchanged for callers, new `matchDetailed()`); hints in `BadHabitFormScreen` / `GoodHabitFormScreen` (switch-list banner, "Did you mean" chip, never blocks saving); `AppModules` registers `HabitRecognizer` and `HabitNameHandoff`; Strings section "Agent 1". Stub files `domain/coach/catalog/SubstanceCatalog.kt` and `BehaviorCatalog.kt` (empty) are placeholders for Agents 2/3.

**Files:** see `git show 7e7a315 --stat`. Tests: `commonTest/.../domain/recognition/*`, `domain/coach/HabitKnowledgeBaseTest.kt` (about 360 table cases, merge-by-id with fake catalogs, a 1,000-call timing check).

**Verification:** NOT compiled and NOT run with Gradle or kotlinc (neither was available in this environment). The matching logic was mirrored line by line in Python; all 362 category/polarity table cases passed in that mirror (which proves the algorithm and expectations, not the Kotlin). The Kotlin tests, the Compose changes and the merge step are **unverified**; run `:shared:testAndroidHostTest` and `:androidApp:assembleDebug`.

**Known risks:** compile errors are possible in unverified Kotlin; fuzzy matching can still collide with real words not in `HabitDictionary.commonWords` (add them there); Arabizi support was not built (stretch); the good-habit lexicon (~60 entries) is hand-written and will need growth; `HabitNameValidator` was left unchanged.

## Round 2 - Agent 2 (knowledge catalog A: substances, food and drink)

**Built:** `SubstanceCatalog.categories`, 20 curated habits with dense "what happens after N hours/days" timelines, many EN + Egyptian-Arabic keywords, craving tips and safety notes: cigarettes (`nicotine`), vaping, shisha, chewing tobacco/snus, coffee (`caffeine`), energy drinks, heavy tea, soda/sugary drinks, sugar in tea/coffee, sweets (`sugar`), fast food (`junkfood`), late-night eating, overeating, excess salt, fried food, `alcohol`, cannabis, tramadol/opioid painkillers, sleeping pills/sedatives, stimulants. The legacy ids `nicotine, caffeine, sugar, alcohol, junkfood` are re-declared (enriched) and rely on the merge-by-id rule in `HabitKnowledgeBase` (Agent 1). Data only: no UI, no Room, no dependency changes.

**Safety:** alcohol, painkillers, sleeping pills and stimulants always carry a `safetyNote` (alcohol, painkillers and sleeping pills warn against stopping suddenly). No dosing, tapering, sourcing or usage information anywhere.

**Files** (package `domain/coach/catalog`): `SubstanceCatalog.kt` (replaces Agent 1's empty stub), `SubstanceNicotine.kt`, `SubstanceDrinks.kt`, `SubstanceFood.kt`, `SubstanceDrugs.kt`; test `commonTest/.../domain/coach/catalog/SubstanceCatalogTest.kt` (17 integrity tests). Branch `agent-2-substance-catalog`, commit `9358cfe`; **not merged yet**.

**Verification:** the Kotlin and `SubstanceCatalogTest` were **NOT compiled or run** (no Gradle or kotlinc available): unverified. A Python script that parses the Kotlin source and applies the same rules as the test found no violations (unique slug ids, >= 8 keywords, no duplicate keywords within or across categories, >= 6 ascending milestones, 3-5 tips, safety notes, Arabic keywords, no dosing wording); that checks the data rules only, not compilation. Content: the nicotine early timeline and the cannabis withdrawal timeline were checked against NHS and NIDA-based sources (online, 2026-10-28). **Everything else is written from general knowledge and marked `// UNVERIFIED ... needs review` in the KDoc**; a clinician should review the alcohol, painkiller, sleeping-pill and stimulant text first.

**Known risks:** broad single-word keywords (`tea`, `salt`, `hash`, `meth`, `crack`) depend on word-boundary matching and on how Agent 1's matcher scores weak/generic keywords; the matcher has not been run against this catalog (table cases for the new keywords are not yet in Agent 1's tests); Arabic and Franco-Arabic keywords need native-speaker review; the `stimulants` category also lists ecstasy/MDMA keywords.

## Round 2 - Agent 3 (knowledge catalog B: behavioural, screen, mental and body-reflex habits)

**Built:** `BehaviorCatalog.categories`, 28 curated habits with dense "what happens after N hours/days" timelines (6 to 9 milestones each, first one at 20 min to 4 h), 15 to 28 EN + Egyptian-Arabic (some Franco-Arabic) keywords each, 4 craving tips and, where relevant, a safety note. Enriched legacy ids: `screens` (broad fallback), `nails` (nail biting only), `sleep` ("Staying up late"), `procrastination`, `spending` (impulse and online shopping). New ids: `socialmedia, doomscrolling, shortvideo, gaming, bingewatching, phonechecking, latenightphone, texting, oversleeping, overthinking, negativeselftalk, comparison, sitting, skinpicking, hairpulling, lipbiting, knucklecracking, teethgrinding, gambling, swearing, gossiping, anger, explicitcontent`. Data only: no UI, no Room, no dependency changes.

**Safety:** gambling, spending, explicit content, overthinking, negative self-talk, skin picking, hair pulling, anger, gaming, nails, lip biting, knuckle cracking, teeth grinding, sleep and oversleeping carry a `safetyNote` pointing to a doctor, counsellor, dentist or support service (overthinking, negative self-talk and gambling also mention the local emergency number or crisis line). No diagnosis, no shaming wording, no explicit text.

**Files** (package `domain/coach/catalog`): `BehaviorCatalog.kt` (replaces Agent 1's empty stub), `BehaviorScreens.kt`, `BehaviorSleepMind.kt`, `BehaviorBodyReflex.kt`, `BehaviorSocialMoney.kt`; test `commonTest/.../domain/coach/catalog/BehaviorCatalogTest.kt` (17 integrity tests). Branch `agent-3-behavior-catalog`, commit `b1af628`; **not merged yet**.

**Verification:** the Kotlin and `BehaviorCatalogTest` were **NOT compiled or run** (no Gradle or kotlinc available): unverified. A Python script that parses the Kotlin source and applies the same rules as the test found no violations (unique slug ids, >= 8 keywords, no duplicate keywords within or across the 28 categories, >= 6 ascending milestones, first milestone <= 4 h, >= 3 milestones in 48 h, 3-5 tips, safety notes present where required, Arabic keywords, no generic single-word keywords, safety notes never diagnose); that checks the data rules only, not compilation, and it did not compare against Agent 2's keywords (their source was not available). Content: the hair pulling and skin picking text (when to see a GP, CBT, diary, replacing the action) and the nail growth rate were checked against NHS/NIDirect pages online; **everything else is written from general knowledge and marked `// UNVERIFIED ... needs review` in the KDoc**. A clinician should review anger, gambling, explicit content, overthinking and negative self-talk first.

**Known risks:** `match("betting")` now resolves to `gambling` instead of `spending`, so `CoachEngineTest` line 41 (`assertEquals("spending", kb.match("Betting").id)`) must be updated (owned by Agent 4); keywords were not compared with Agent 2's list and the matcher has not been run against this catalog (a cross-catalog duplicate check script is listed in the merge notes); short or common keywords (`scroll`, `scrolling`, `insta`, `rage`, `gossip`, `trich`, `netflix`, `porn`) depend on word-boundary matching and on Agent 1's weak-keyword scoring; Arabic and Franco-Arabic keywords need native-speaker review; milestone text for behavioural habits is experience-based, not clinical measurement.

## Round 2 - Agent 4 (offline coach upgrade + "what happens if I stop for N hours/days")

**Built:** `DurationParser` (EN/AR/mixed durations: "6h", "half a day", "a day and a half", "2 days 3 hours", ranges (lower bound), Arabic words and digits, unit typos; rejects 0, negatives, > 10 years, seconds; returns the matched span), `StopTimelineExplainer` + `StopTimelineAnswer` (works with any category incl. `generic`; neutral wording; early-phase text before the first milestone; safety note always passed on in full), `CoachEngine` upgrade (new WHAT_HAPPENS intent incl. "how long until cravings stop" and Arabic; typo-tolerant intent matching through the private helper `domain/coach/CoachText.kt`; habit found by name, typo or category word; follow-ups like "and after a week?" via new `CoachSnapshot.recentUserTexts`, filled by `SendCoachMessageUseCase` from the existing repository, no interface change; small talk; reply variants by seed; sensitive habits (`explicitcontent`) shown by label, never by the typed words; crisis reply kept and extended with typo and extra phrases), UI: "What happens if I stop for..." explorer on the quit-habit analysis screen (chips 1h to 1mo, custom input, defaults to the live streak) and a quick-prompt chip on the coach screen that pre-fills the question. Strings in the `Agent 4` section of `Strings.kt`.

**Files:** `domain/coach/{DurationParser,StopTimelineExplainer,CoachText,CoachEngine}.kt`, `domain/usecase/CoachUseCases.kt`, `presentation/screen/{BadHabitDetailScreen,CoachScreen}.kt`, `presentation/strings/Strings.kt`; tests `DurationParserTest` (about 130 cases), `StopTimelineExplainerTest`, extended `CoachEngineTest` (also `Betting` now accepts `spending` or `gambling`). Branch `agent-4-coach-timeline`.

**Verification:** **nothing was compiled or run** (no Gradle, kotlinc or network in this session): all code and tests are **unverified**; expected values were traced by hand only. First thing to run: `./gradlew :shared:testAndroidHostTest`, then `:androidApp:assembleDebug` and open a quit habit.

**Known risks:** `CoachText` duplicates a little of Agent 1's matcher (unify later); engine tests that resolve a habit from words rely on `knowledge.matchDetailed` returning null for filler like "what happens if i stop for"; a bare "m" means minutes, month needs "mo"/"month"; ranges use the lower bound; the explorer text from the explainer is English domain text (not in `Strings`), like the existing coach replies; Arabic phrase lists need native review; follow-ups use only the last 6 user messages.

## Round 2 - Agent 5 (Android home-screen widgets + "Add to Home screen")

Branch `agent-5-widgets` (earlier commits e10193d, 0c3214a; the card redesign and pin feature are newer and not committed in the delivery zip).

**Built:** three resizable Android widgets. *Quit counter*, *Good habit quick log* ("+" / "-" record without opening the app) and *Events* (nearest upcoming or a chosen event, count-up after the date). Per-widget choice via `WidgetConfigActivity` (SharedPreferences, no Room change); taps open the habit / Events through `MainActivity` extras and the existing `Navigator` API. Numbers reuse `TrackingStateCalculator`, `GoodHabitStatsCalculator`, `StreakColorMapper`, `DayCalculator`. Follows the app theme.

**Card redesign (latest):** widgets now mirror `BadHabitCard`, `GoodHabitCard` and `EventCard` (same colors and corner radii, glyph badge, DAYS/HRS/MIN/SEC counter blocks, stage dots, longest streak, progress bar, 7-day strip, streak row, category/date/reminder pill). Drawn with white tintable shapes and vector icons copied from `NixIcons.kt`; layouts are generated by `tools/gen_widget_layouts.py` (11 layouts: bad/good/event x full/row/slim, a narrow good variant, and a message card). `WidgetLayoutPlanner` (pure, tested) chooses the variant from the widget size and which optional rows fit: titles are never hidden, nothing overlaps, counter numerals are sized in dp so they always fit the width. The Chronometer prints MM:SS in the first hour and H:MM:SS after, so the unit-label row follows it, and the refresh alarm also fires at that switch.

**"Add to Home screen":** button (`AddToHomeScreenButton`) on the quit-habit analysis screen, the good-habit analysis screen and the event edit screen. Uses the `WidgetPinner` port (`domain/widget`), implemented on Android by `AndroidWidgetPinner` with `AppWidgetManager.requestPinAppWidget`; the chosen habit/event is stored as a 3-minute "pending pin" and applied by `WidgetConfigActivity` (chooser skipped) or `WidgetPinReceiver`. The launcher owns the placement UI (page/position, resize and move afterwards); an app cannot place a widget silently. If the launcher does not support pinning the screen shows how to add it manually. No binding on iOS: the button is hidden.

**Technology decision:** classic `AppWidgetProvider` + `RemoteViews`, not Glance (no Maven access when written; could not be checked against AGP 9.1.1, compileSdk 37, Kotlin 2.4.20, Compose 1.12.1). Dependencies: `androidApp/build.gradle.kts` has `implementation(libs.compose.ui)` and `implementation(libs.kotlinx.coroutines.core)` (existing catalog entries); `libs.versions.toml` unchanged. Widget text is in `domain/widget/WidgetStrings.kt`; `Strings.kt` is not required (a previously pasted widget block there is unused and can be deleted).

**Refresh strategy:** system `Chronometer` for the clock (no wake-ups); one inexact `AlarmManager.setAndAllowWhileIdle` alarm for the earliest of: counter day rollover, MM:SS/H:MM:SS switch, event passing, local midnight; 500 ms-debounced re-render on data change while the process lives; re-render on boot, app update, time/date/time-zone change. Doze can delay the alarm by minutes.

**Files:** `domain/widget/*` (models, state mapper, nearest-event selector, refresh planner, layout planner, `WidgetStrings`, `WidgetPinner`), `domain/usecase/WidgetUseCases.kt`, `presentation/components/AddToHomeScreenButton.kt`, one added line in each of `BadHabitDetailScreen`, `GoodHabitDetailScreen`, `EventFormScreen`, `androidApp/.../widgets/*`, `res/layout/widget_*`, `res/drawable/{widget_*,ic_w_*}`, `res/drawable-nodpi/widget_preview_*`, `res/xml/widget_*_info.xml`, manifest, `MainActivity`, `NixUraApplication`, `PlatformModule.android.kt` (widget use cases), tests in `commonTest/.../domain/widget/` and `.../usecase/WidgetUseCasesTest.kt`, `tools/gen_widget_layouts.py`.

**Verification:** *Owner confirmed on a device (earlier version):* widget numbers and live clock, "+"/"-", choosing a habit or event. *Run by me:* 90 pure-logic tests compiled with plain `kotlinc` 2.2.20 (project uses 2.4.20) and run with a custom reflective runner, outside Gradle/JUnit: **90 passed** (removing the quick-log lock makes the double-tap test fail, checked). Widget Kotlin type-checks against `android.jar` API 34 with stubs for Koin, Compose Color, `ContextCompat`, the generated `R` (ids taken from the real resource files), `Navigator`, `StreakColorMapper`, `NixIcon`, formatting helpers; all XML is well-formed. **NOT run:** `:androidApp:assembleDebug` and `:shared:testAndroidHostTest` for the redesign, AAPT resource linking, the Compose button file (`AddToHomeScreenButton.kt`) and the three screen edits (not compiled), and everything on a device: card look and size behaviour, "Add to Home screen" with real launchers. **iOS widgets: not implemented.**

**Known risks:** the card look is built from tinted shapes via `setColorFilter` / `setImageAlpha` (standard RemoteViews methods, but not seen on a device); launcher behaviour for pinned widgets varies (some skip the config screen, handled by `WidgetPinReceiver`; some cannot pin); Chronometer behaviour in RemoteViews varies by launcher; `DATE_CHANGED` / `TIME_SET` may not reach manifest receivers on Android 8+ (also registered dynamically while the app runs); "+" is read-modify-write under a process-wide lock, so a simultaneous in-app write could in theory be lost; unit-label alignment under the Chronometer is approximate (three equal columns under H:MM:SS); the stage "dot" for the current stage is not larger than the others (RemoteViews cannot resize views before API 31).

**Manual QA:** see the widget checklist in README "Testing". Also: press "Add to Home screen" on each of the three screens and confirm the launcher dialog, the widget shows the right habit/event without a chooser, and it can be moved/resized; try an old or unsupported launcher and confirm the manual-instructions message.
