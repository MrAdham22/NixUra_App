# NixUra — Development Status

**Last updated:** 2026-11-03 (Round 3: water fill, widget parity, delete with undo, money saved, calendar imports)

## Where we are now (2026-11-11, read this first in a new chat)

- **`master`** (`6ed81b1`) holds everything merged so far: Firebase sign-in, UI polish, the email link page (PR #4), weekly good habits (PR #5, Room v6) and the Notion Worker (PR #6).
- **`feat/ai-coach`** is pushed and **not merged yet; no pull request is open**. It holds the real AI coach, voice notes, the animated progress cards and Room v7. The owner opens the PR at `https://github.com/MrAdham22/NixUra_App/compare/master...feat/ai-coach?expand=1` (the `gh` tool is not installed on the owner's PC, and GitHub is not signed in for agents).
- **Live server:** one Cloudflare Worker, `nixura-notion`, at `https://nixura-notion.nixura-notion-worker.workers.dev` (code in `workers/notion`; Notion link + AI coach). It is deployed from `feat/ai-coach`. Deploy with `npx wrangler deploy` in that folder (Wrangler is logged in on the owner's PC). Secrets set: the Notion ones, `GEMINI_API_KEY`, `GROQ_API_KEY`, `OPENROUTER_API_KEY`. Not set: `MISTRAL_API_KEY` (Mistral's console kept its "create key" button greyed out; probably needs the free plan chosen or a phone number verified).
- **Owner's phone** runs the `feat/ai-coach` debug build (Room v7). A build from an older branch must not be installed over it: Room cannot downgrade.
- **Tested on the phone by the owner (2026-11-11):** AI chat, a voice note, and after the model change a second chat and voice note that Gemini answered with no fallback.
- **Next steps, in order:**
  1. Owner: open and merge the `feat/ai-coach` pull request.
  2. Owner: untick "Update content", "Insert content", "Insert comments" and pick "No user information" on the Notion connection (NixUra only reads).
  3. Mistral key, when its console allows it: `npx wrangler secret put MISTRAL_API_KEY` in `workers/notion`.
  4. Branded emails: needs a domain the owner buys (Firebase refuses the action URL change, see "Branded email link page").
  5. Firestore sync (designed, not built; sync is paused). It must carry `weeklyTarget`.
  6. The owner's to-do list at the end of this file (polish pass, specialised cards, paid plan, on-device model, iOS voice notes).
  7. iOS: nothing of the last weeks is compiled or run on a Mac.
- **Owner's working rules:** explain simply (beginner developer); never put AI names in commits, PRs, code or docs; ask before assuming; after a notable fix add a beginner "interview story" to `D:\Career_development\Claude_app_dev\Interview Talks` (stories 1 to 6 exist); keep replies and tool use short to save tokens.

## Edit voice note items before adding (2026-11-14, branch `claude/sharp-payne-50d044`)

- **Built:** in the "Here's what I understood" sheet (voice notes and chat suggestions) each item has a pencil button. It opens an edit form inside the sheet, built from the add forms' pieces: quit or build chips, the name field, every day / days a week with the stepper, and for events the title, category chips and the date and time picker. "Keep changes" saves the corrected item into the list (and ticks it); the list's "Add" button then saves the corrected values.
- **Rules:** `domain/coach/ai/CoachActionRules` (same as the add forms: a name of at most 80 characters, event title at most 60, 1 to 99 a day, 1 to 7 days a week, a real date, and an event still in the future; an event with no time counts as 9:00). Items that break a rule show "Needs a fix"; tapping "Add" opens the form on the first one instead of adding. This also fixes a hidden gap: the AI could send an event title of 61 to 80 characters, which the event use case then refused.
- **Model change:** `CoachAction.Event` got a `category` (default Event, which is what was saved before). No database change.
- **Verified:** `:shared:testAndroidHostTest` passes (475 tests, new `CoachActionRulesTest` and `ApplyCoachActionsUseCaseTest`) and `:androidApp:assembleDebug` builds.
- **Not verified:** nothing was seen running. The only device attached was an emulator (not the owner's phone) that stopped answering adb, so `installDebug` could not finish. The owner checks on the phone: record a voice note, tap the pencil, change the count, the date and the category, keep, add, and see the corrected habit and event saved. iOS not compiled (no Mac; the code is all in `commonMain`).

## README, screenshots and official app icon (2026-11-14, branch `claude/nixura-repo-enhancement-3a1a93`)

- **App icon:** the owner's artwork (broken purple chain, glowing gold "i") is now the official launcher icon. Android: adaptive icon (`mipmap-*/ic_launcher_foreground.png` at all five densities on `@color/ic_launcher_background` `#110721`), legacy `ic_launcher` / `ic_launcher_round` PNGs for Android 7, and `androidApp/src/main/ic_launcher-playstore.png` (512 px) for the Play listing. The template vectors `drawable-v24/ic_launcher_foreground.xml` and `drawable/ic_launcher_background.xml` were removed. iOS: `app-icon-1024.png` replaced (opaque, iOS rounds it). The in-app leaf brand mark was not changed.
- **README:** new top section for visitors and recruiters (icon, badges, hero banner, what the app does, a screenshot gallery of 18 screens, engineering highlights); the detailed status, features and setup sections below are kept. Screenshots in `docs/screenshots/`, icon and hero in `docs/assets/`.
- **Verified:** `:androidApp:installDebug` built and installed on the Pixel 9 emulator; the new icon shows in the app drawer and on the splash screen. All screenshots were taken from that build with demo data (habits added in the app; past good-habit days and the events were written into the emulator's database to fill the calendars). The quit counter and quick-log widgets were placed with "Add to Home screen" and the events widget from the launcher picker, and all three render. **Not verified:** the icon on a real phone and on iOS (no Mac); the Android 13+ themed (monochrome) icon is not provided, so themed-icon launchers show the full-colour icon.
- **Follow-up (same day): issues noticed while taking screenshots.**
  - **Fixed: stale coach greeting.** A chat's greeting was written once, when the chat was created (often at start-up, before any habits or a name), so the welcome card still said "Hi there. Add a habit you want to leave behind" with three quit habits. Now `EnsureCoachGreetingUseCase` rewrites the greeting every time a chat the user has not written in yet is opened (`CoachRepository.refreshGreeting`, DAO `refreshUnusedGreeting`, which never touches a chat with a user message). Seen on the emulator: "Hi Sam. Add a habit..." became "Hi Sam. You're 3 days clear on Smoking." after adding a habit.
  - **Not an app bug: doubled first letter.** "SSam" in the starter guide's name field did not happen again in two tries (typing right after the tap and after the keyboard was up); it came from the emulator's first keyboard start with `adb shell input text`.
  - **Not a bug: prayer form height.** The gold wash fills the whole screen (mosque at the bottom) once its animation ends; the earlier screenshot was taken mid-animation.
- **In-app brand mark removed (owner decision 2026-11-14):** the leaf tile was first swapped for the new logo, then, at the owner's request, removed altogether. The welcome screen, sign-in and account forms, the top bar and Settings > About now show only the name "NixUra". `BrandMark`, its image resource and the `compose.resources` block in `shared/build.gradle.kts` are gone. The launcher icon is unchanged. The leaf stays as the Profile row icon in the account sheet and as the fallback glyph for unknown build habits. README screenshots retaken.
- **Verified (follow-up):** `:androidApp:assembleDebug` builds; `:shared:testAndroidHostTest` **465 tests, 0 failures**; `tools/room_migration_check.py` validates 83 DAO queries including the new one (its two "SQL ERROR" lines on the events `REMOVED_BY_IMPORT_SQL` queries were already there before this change: the checker cannot substitute that Kotlin constant). Installed on the owner's phone over the existing install (Room v7, data kept). After the brand mark was removed: build passes, 465 tests pass, seen on the emulator (welcome, sign-in, top bar, Settings show the name only) and installed on the owner's phone again (top bar shows "NixUra" alone). iOS not compiled (no Mac).

## Prayer habit card (2026-11-12, branch `claude/islamic-prayer-habit-card-eff051`)

- **Built:** a good habit named like a prayer gets 5 a day automatically, the add form turns gold from the prayer word downward, and the card is a gold Islamic-art card with star buttons, five lighting stars and a burst on every count (see README, "Prayer habit").
- **Verified:** `:shared:testAndroidHostTest` passes (5 new `PrayerHabitDetectorTest` tests included) and `:androidApp:assembleDebug` builds. The debug APK was copied to `D:\Career_development\Claude_app_dev\NixUra-debug.apk`.
- **Not verified:** nothing was seen running. The look and feel of the animations (gold wash, burst, star buttons) and the colour contrast still need the owner's eyes on a phone. iOS: the code is in `commonMain` but was not compiled for iOS (no Mac).
- **Owner feedback round (2026-11-12, after seeing it on the phone):** the shine now loops seamlessly (two bands half a loop apart, each wraps only while outside the card); the gold wash starts at the very top and covers the status and navigation bar areas; richer amber colors; gold "Add habit" button (`NixButtonStyle.Gold`); "Read 10 pages" and "Sleep before midnight" removed from the quick picks; card redesign inspired by the owner's reference picture (mosque silhouette, crescent, golden dawn, mihrab-arch badge, double frame, rays when all five are done). Tests pass and the APK builds. Installed on the owner's phone and checked from screenshots: the gold form covers the whole screen, the gold button, the Build card (mosque in its own space at the bottom, moon at the top centre) and the Home row (big mosque, kept as the owner liked it). The shine loop and the count burst move, so they cannot be judged from a still screenshot; the owner checks them by eye.
- **Second feedback round (2026-11-12):** Home-row mosque a little smaller so it fits the row; the form's gold button is now its own `PrayerGoldButton` (the plain gold `NixButton` style was removed again); the five prayer markers are mihrab arches with a tick instead of stars. Installed on the owner's phone and checked from screenshots (unfinished and all-done card states, Home row, form).
- **Known limits:** English only (Arabic strings are not built yet).

## Prayer widget, category glyphs, 3x1 quit widget name (2026-11-12, branch `claude/prayer-habit-card-widget-1b8b83`, on top of the prayer card branch)

- **Prayer widget:** the good-habit widget of a prayer habit now looks like the in-app prayer card. The card's drawing code in `IslamicArt.kt` was split into plain drawing functions (`PrayerCardScene` + `drawPrayerCard`, `drawPrayerArchBadge`, `drawPrayerStar`, `drawPrayerBead`, `prayerCardColors`) that the card and the widget both call; the widget runs them into bitmaps (`WidgetCardPainter.compose` / `prayerCard`). New layouts `widget_good_prayer` and `widget_good_prayer_narrow` (arch badge, star buttons, row of prayer arches with the five names, week strip, streak row, mosque in the room left at the bottom). Widgets cannot animate: the light band rests where it rests with "remove animations", and there is no burst.
- **Category glyphs:** the random badge icon (picked by habit id) is gone. `domain/recognition/HabitTopic.kt` (`HabitTopicDetector`) maps every quit category of the catalog and every build-habit entry to a kind (smoking, alcohol, hot drink, soft drink, sweets, junk food, phone, gaming, TV, pills, money, gambling, sleep, time, mind, sitting, speech, hands, adult content; water, fitness, movement, learning, prayer, calm, writing, healthy food, morning, self-care, people, music, tidy, eyes), and `presentation/components/TopicIcons.kt` gives each kind its own line icon (25 new path-data icons in `NixIcon`). Unknown names get a shield (quit) or a leaf (build). Cards, the quit detail screen and the widgets use it (widgets draw the icon with the same code into a bitmap).
- **3x1 quit widget name:** the one-row layout broke "smoking" into "smok / ing" beside the counter. `WidgetLayoutPlanner.quitRow` now tries, largest first: badge with the full-size name, a smaller counter, no badge, smaller name sizes, and keeps the first where no word has to break (`titleFitsWhole`). The narrow quit layout shrinks its one-line name the same way.
- **Verified:** `:shared:testAndroidHostTest` passes (new `HabitTopicDetectorTest`, two new planner tests) and `:androidApp:assembleDebug` builds.
- **Installed on the owner's phone (2026-11-12)** and checked from screenshots: the Quit cards show the cigarette (smoking) and burger (junk food) icons, the water card the drop, the prayer card and the Home row are unchanged; data kept.
- **Widgets confirmed by the owner on the phone (2026-11-12):** the prayer widget, the category icons on the widgets and the 3x1 quit widget name look good.
- **Not verified:** iOS not compiled (no Mac).

## One-paragraph summary

NixUra (renamed everywhere on 2026-11-03; app id and code packages `com.adhamelgendy.nixura` since 2026-11-05) is a Kotlin Multiplatform + Compose app to quit bad habits, build good ones and count down to events. The offline tracking core was built and verified earlier (Room, MVVM, Koin; 30 tests and `assembleDebug` passed). On 2026-10 the owner approved a large scope expansion and asked to merge a v0-designed UI ("NixUra UI"): new screens, good habits, events with reminders, profile, settings, onboarding and an offline rule-based coach. That work was written without Gradle access, then built by the owner in Android Studio. **The owner reports (2026-10-23) that everything works properly and has been tested on Android** (after two first-run fixes: stale Room database and screen padding). **iOS has never been built or run**; the owner approved iOS as a full target with no restrictions on AI agents touching iOS code. **Remaining work (owner-stated): (1) fine-tune the UI, (2) add a real AI agent, (3) add curated info on many specific bad habits and recognise a habit from however the user typed it, (4) make the app work on iOS.**

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

**Runtime crash on launch (2026-10-31): `VerifyError ... StringsKt.<clinit>() failed to verify ... expected 10 argument registers, method signature has 11 or more` (classes7.dex).** Cause: `Strings` was a `data class` with 256 constructor parameters and `val EnglishStrings = Strings()` runs in the file-level static initializer; the Kotlin compiler emits a synthetic default-constructor call with ~265 arguments there, D8 encodes that invoke wrongly and the Android verifier rejects the class when `AppRoot` first touches `LocalStrings`. Fix: `Strings` is now `@Immutable open class Strings { open val x: T = ... }` (same 256 property names, same order, so no call site changes; nothing used `copy()` or destructuring). Future languages subclass it and override only what differs. Rule: never put hundreds of strings in a constructor. Files: `presentation/strings/Strings.kt` only. **Not compiled or run by the assistant (no Gradle/kotlinc here)**: owner to Clean Project, rebuild and launch; structural check done (property names/order identical to the old constructor, brackets balanced).

**Mission 1 (2026-10-27, branch `mission-1-settings-gear-topbar`; first version tested by the owner on Android and reported as looking good; the Material-icon dependency change below is not yet synced or built):** top bar now has only logo/name + profile avatar; Settings moved to a gear icon at the top right of the Profile header (`SubScreen(actions = ...)`); new `NixIcon.Gear`, rendered by `NixIconView` with the official Material `Icons.Filled.Settings` (new dependency `org.jetbrains.compose.material:material-icons-core:1.7.3` in `gradle/libs.versions.toml` + `shared/build.gradle.kts`; the version was chosen from memory and could not be resolved offline, so the first Gradle sync is the check); the full-width "Settings" button at the bottom of Profile was removed (it would duplicate the gear); `NixIcon.Sliders` and its draw branch were removed (no remaining uses). Files: `AppShell.kt`, `ProfileScreen.kt`, `NixIcons.kt`. The Material-icon change is not compiled, synced or run by the assistant (no Gradle/network here). Owner to Gradle-sync, then re-run `./gradlew :androidApp:assembleDebug` and `./gradlew :shared:testAndroidHostTest`, then check on device: top bar, gear at 42 dp, Settings opens, back returns to Profile, light and dark themes.

## What is left (priority order)

Android is done and owner-tested. The owner's remaining work is items 0-4; the rest is backlog.

0. **Move to Firebase + Cloudflare (owner decision 2026-11-05), in order:** (a) Firebase sign-in, done on `feat/firebase-auth`, needs a phone test; (b) Firestore sync, designed (see "Firestore sync design"), not built, sync is paused until then; (c) Notion through a Cloudflare Worker: built, deployed and connected to real Notion on the owner's phone (`feat/notion-worker`, 2026-11-11, see "Notion Worker"); (d) the AI coach Worker (item 2); (e) payments for more than about 20 coach messages a day.
0b. **Owner's UI requests (2026-11-08):** items 1-4 (logo buttons, the stacked sign-in screens bug, Profile, Settings) are done on `feat/ui-polish`, see "UI polish". (5) the emails: the branded link page is built (`feat/auth-action-page`, see "Branded email link page"); left: deploy it and set the action URL, set sender name / subject / reply-to in Templates, and later a custom domain (SPF/DKIM).
1. **Fine-tune the UI** (spacing, typography, animations, small screens, light theme, contrast).
2. **AI coach:** built on `feat/ai-coach` (see "AI coach and voice notes"). Left: try it signed in on a phone (real AI answer, a real recording, the review-then-add flow), and the owner adds the API keys.
3. **Bad-habit knowledge + free-text understanding:** extend `HabitKnowledgeBase` with curated info for many more habits (effects, withdrawal, recovery timeline, triggers, tips) and make habit recognition robust to however the user typed the name (normalisation, synonyms, typos/fuzzy match, multi-word and multi-language input; fall back to a generic profile and, once the AI exists, to the AI). Use it to personalise the card, analysis screen, coach and milestones. Add unit tests for the matcher.
4. **iOS (approved, unrestricted):** (a) on a Mac run `:shared:linkDebugFrameworkIosSimulatorArm64` and build/run `iosApp` in Xcode, fix errors; (b) implement `NotificationScheduler` + `NotificationPermissionGateway` with `UNUserNotificationCenter` in `iosMain` (cap at 64 pending requests); (c) check status-bar style (`SystemBars.ios.kt`), safe areas and keyboard on a real iPhone; (d) run `:shared:iosSimulatorArm64Test`; (d2) iOS widgets (WidgetKit + App Group; none exist yet, Android widgets are Android-only); (e) before TestFlight: signing team, `Info.plist` strings (bundle id and the app icon are done; check the icon on a device).

**Backlog (not blocking):** commit `shared/schemas/**/2.json`; accessibility labels; store readiness (versioning, signing, privacy policy, Play Data-safety form); Arabic/RTL + translated coach content; more tests (use cases, ViewModels with fakes, DAO, UI); edit/delete individual relapses, undo, good-habit reminders, backup/export. (Android widgets: Round 2 - Agent 5; iOS widgets still open.)

## Known simplifications / risks

- `MIGRATION_1_2` SQL is hand-written; if Room reports "Migration didn't properly handle", compare with the generated v2 schema JSON.
- ViewModels are Activity-scoped (shared across screens); the nav stack resets after process death.
- Daily-check-in and milestone alarms are re-planned whenever the app is open; if the app is not opened for 14 days daily check-ins stop.
- DST edge cases in `DayCalculator.startOfDayMillis` use a two-step offset guess.
- Coach replies are keyword rules in English; health text is general information, not medical advice.
- The sandbox that wrote the merge could not run Gradle; Android is owner-tested and working; every iOS path is still unverified.
- Google's "app not registered" answer is recognised by the text `UNREGISTERED_ON_API_CONSOLE` in the exception message (Google gives it no status code of its own). If Google changes that text, the app falls back to "Google access wasn't granted"; the `NixUraGoogleAuth` log line still shows the reason.
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

**Widget polish round (written, type-checked by hand, NOT compiled or run on a device):** quit card is centered, no category line, no stage dots/text, no chevron; habit names are bigger (quit 20/18/17 sp, good 20/18/17 sp, event 20/18/17 sp); the H:MM:SS clock has `letterSpacing=0.1` (planner `CLOCK_EM` 4.4 -> 5.2) and the event card puts the day counter closer to the clock (gap 6 dp instead of 12); the event date has no year (`formatDateTime(..., includeYear = false)`, app screens unchanged). Good card: `WidgetLayoutPlanner.estimateTitleLines` estimates the name wrapping so the badge is dropped first, then the buttons move under the name (`controlsBelow`, layout `widget_good_full_narrow`, also used for short narrow widgets), and the name gets 1-3 lines; the row layout (4x2-like) got a streak row (`showStreakRow`) that wins over the progress bar when height is scarce. Layouts regenerated with `tools/gen_widget_layouts.py` (it also deletes the old unused `widget_counter_*`, `widget_good_compact/wide` and `widget_dot`). The `widget_preview_*.png` images still show the old look.

**Widget fix round 2 (written, NOT compiled; layouts checked as well-formed XML):** (1) The earlier good-habit change (compact layouts, 88 dp minimum height) is undone: `widget_good_*` layouts, `widget_good_info.xml` and the good-habit renderer are back to the original. (2) Good-habit priority is now week strip (52) > day-streak row (28) > progress bar (18) in `goodFull` and in the stacked `goodSlim`. (3) Quit and good widgets use `TIGHT_PADDING = 8` instead of 16 (events keep 16), so a 2x2 quit widget keeps its "Clear for" line. (4) HRS/MIN/SEC centering: the ghost ":" text was narrower than the real in-context colon (about 11 px vs 27 px on a test phone), so HRS and MIN sat too far right. `WidgetRenderer.clockCellWidthsPx` now measures the cumulative advances of "H:MM:SS" with a Paint like the Chronometer's (bold, tnum, 0.1 em) and sets the empty ghosts' widths as padding. (5) Earlier in this round: events widget has no reminder count; the Chronometer always prints H:MM:SS (format `0:%s` in the first hour); quit labels are 6.5 sp; `nextChangeAtEpochMillis` also fires at the 10-hour mark. Tests updated: `WidgetLayoutPlannerTest`, `WidgetStateMapperTest`.

**"Add to Home screen":** button (`AddToHomeScreenButton`) on the quit-habit analysis screen, the good-habit analysis screen and the event edit screen. Uses the `WidgetPinner` port (`domain/widget`), implemented on Android by `AndroidWidgetPinner` with `AppWidgetManager.requestPinAppWidget`; the chosen habit/event is stored as a 3-minute "pending pin" and applied by `WidgetConfigActivity` (chooser skipped) or `WidgetPinReceiver`. The launcher owns the placement UI (page/position, resize and move afterwards); an app cannot place a widget silently. If the launcher does not support pinning the screen shows how to add it manually. No binding on iOS: the button is hidden.

**Technology decision:** classic `AppWidgetProvider` + `RemoteViews`, not Glance (no Maven access when written; could not be checked against AGP 9.1.1, compileSdk 37, Kotlin 2.4.20, Compose 1.12.1). Dependencies: `androidApp/build.gradle.kts` has `implementation(libs.compose.ui)` and `implementation(libs.kotlinx.coroutines.core)` (existing catalog entries); `libs.versions.toml` unchanged. Widget text is in the `widget*` members of `presentation/strings/Strings.kt` (the former `WidgetStrings` class was merged into it).

**Refresh strategy:** system `Chronometer` for the clock (no wake-ups); one inexact `AlarmManager.setAndAllowWhileIdle` alarm for the earliest of: counter day rollover, MM:SS/H:MM:SS switch, event passing, local midnight; 500 ms-debounced re-render on data change while the process lives; re-render on boot, app update, time/date/time-zone change. Doze can delay the alarm by minutes.

**Files:** `domain/widget/*` (models, state mapper, nearest-event selector, refresh planner, layout planner, `WidgetPinner`), `domain/usecase/WidgetUseCases.kt`, `presentation/components/AddToHomeScreenButton.kt`, one added line in each of `BadHabitDetailScreen`, `GoodHabitDetailScreen`, `EventFormScreen`, `androidApp/.../widgets/*`, `res/layout/widget_*`, `res/drawable/{widget_*,ic_w_*}`, `res/drawable-nodpi/widget_preview_*`, `res/xml/widget_*_info.xml`, manifest, `MainActivity`, `NixUraApplication`, `PlatformModule.android.kt` (widget use cases), tests in `commonTest/.../domain/widget/` and `.../usecase/WidgetUseCasesTest.kt`, `tools/gen_widget_layouts.py`.

**Verification:** *Owner confirmed on a device (earlier version):* widget numbers and live clock, "+"/"-", choosing a habit or event. *Run by me:* 90 pure-logic tests compiled with plain `kotlinc` 2.2.20 (project uses 2.4.20) and run with a custom reflective runner, outside Gradle/JUnit: **90 passed** (removing the quick-log lock makes the double-tap test fail, checked). Widget Kotlin type-checks against `android.jar` API 34 with stubs for Koin, Compose Color, `ContextCompat`, the generated `R` (ids taken from the real resource files), `Navigator`, `StreakColorMapper`, `NixIcon`, formatting helpers; all XML is well-formed. **NOT run:** `:androidApp:assembleDebug` and `:shared:testAndroidHostTest` for the redesign, AAPT resource linking, the Compose button file (`AddToHomeScreenButton.kt`) and the three screen edits (not compiled), and everything on a device: card look and size behaviour, "Add to Home screen" with real launchers. **iOS widgets: not implemented.**

**Known risks:** the card look is built from tinted shapes via `setColorFilter` / `setImageAlpha` (standard RemoteViews methods, but not seen on a device); launcher behaviour for pinned widgets varies (some skip the config screen, handled by `WidgetPinReceiver`; some cannot pin); Chronometer behaviour in RemoteViews varies by launcher; `DATE_CHANGED` / `TIME_SET` may not reach manifest receivers on Android 8+ (also registered dynamically while the app runs); "+" is read-modify-write under a process-wide lock, so a simultaneous in-app write could in theory be lost; unit-label alignment under the Chronometer is approximate (three equal columns under H:MM:SS); the stage "dot" for the current stage is not larger than the others (RemoteViews cannot resize views before API 31).

**Manual QA:** see the widget checklist in README "Testing". Also: press "Add to Home screen" on each of the three screens and confirm the launcher dialog, the widget shows the right habit/event without a chooser, and it can be moved/resized; try an old or unsupported launcher and confirm the manual-instructions message.

## Wave 1 - Agent 1 (swipe navigation, droplet nav bar, reminder edit/remove)

**Built:** `HorizontalPager` over Home/Quit/Build/Events synced with `Navigator` (settled page only), `DropletNavBar` (draw-phase liquid indicator, haptics, reduced-motion fallback), reminder list with edit / remove + Undo / validation / permission notices in the event edit screen, `EventReminderEditor` + pure `ReminderRules`, `ReminderCapabilities` (notifications + exact alarms), exact alarms for event reminders when allowed. No Room change.

**Verification:** **nothing was compiled or run** (no Gradle, kotlinc, device or network). New unit tests in `commonTest` (`ReminderRulesTest`, `EventReminderEditorTest`, `ReminderPlanningTest`, `MainTabsTest`, `DropletMathTest`) are unverified. All iOS additions are unverified. Details, manual QA and owner decisions (Play exact-alarm policy): `docs/agents/agent-1-handoff.md`.

**Known risks:** pager + Crossfade transition and droplet look need a device; reminder edits in edit mode are saved immediately while other fields need "Save changes".

## Wave 1 - Agent 2 (accounts: backend, login, email OTP, password security)

**Built:** `/backend` (Fastify + PostgreSQL: register/verify/login/refresh/logout/reset/change/delete, Argon2id, hashed OTPs, rotating refresh tokens with reuse detection, rate limits, audit log), client auth (domain/data/presentation `auth`, Ktor, Keystore/Keychain token storage, first-launch gate with "Continue without an account", Settings > Account), `SECURITY.md`, `docs/legal/*`, in-app Privacy/Terms generated by `tools/gen_legal_text.py`. No Room change. Details: [agents/agent-2-handoff.md](agents/agent-2-handoff.md).

**Verification:** **Backend and Kotlin were not compiled or tested** (no network/Gradle/Docker here). Only the dependency-free backend modules (password policy, breach-check request shape, email templates) were executed and passed. Tests are written for the rest (backend 43, client 4 classes) but unrun.

**Known risks:** Ktor/serialization versions vs Kotlin 2.4.20; Compose autofill `ContentType` API; iOS Keychain casts; per-process rate limiter; legal texts are drafts.

## First real build of Wave 1 (2026-11-02, run on Windows)

**Ran:** backend `tsc` and `npm test`; `:shared:testAndroidHostTest`; `:androidApp:assembleDebug`.

**Results:** backend typecheck passes and **43/43 tests pass** (PGlite, no real Postgres or email provider). `:androidApp:assembleDebug` **passes**. `:shared:testAndroidHostTest`: **345/345 pass** after the test updates below. Not run: the app on a device, the backend against real Postgres, anything on iOS.

**Fixed:**
- `shared` did not compile: the comment text `docs/legal/*.md` in `AccountScreens.kt` opened a nested Kotlin comment.
- Backend could not start on any fresh database: `001_accounts.sql` created `schema_migrations`, which `migrate.ts` had already created.
- `ARGON2_TIME_COST=1` was accepted by config but rejected by argon2; the minimum is now 2.
- Emails with surrounding spaces got a 422 because validation ran before trimming.
- `trustProxy` type error in `app.ts`; behaviour is unchanged.
- An uncommitted copy of `androidApp/build.gradle.kts` had replaced the root `build.gradle.kts` in the owner's checkout, which caused "plugin is already on the classpath with an unknown version". Restored from git; not a repo change.

**Out-of-date tests updated to match the code (owner decision):** "Sugary drinks" gives `softdrinks`, "Energy drink" gives `energydrinks`, "Drinking" alone gives `generic` because it is too vague (so it gets no alcohol safety note unless the name says alcohol, beer, etc.), the next smoking milestone after 30 min is 1 h, and event widget padding is 8 dp.

**Removed:** `STRINGS_PASTE_THIS.txt` (leftover paste instructions).

**Owner phone test (2026-11-02):** the owner reports all Wave 1 features and the older features work on Android. Register, email code and login were not part of that test, because no server was running.

**Backend on real PostgreSQL 18 (2026-11-02, local):** migration applied; `/health`, register, login refused before verification, verify, login, wrong password refused, `/v1/me`, token refresh and refresh-replay rejection all behaved correctly over HTTP. The owner then tested sign-in from the app on a phone against this server and reports it works (2026-11-02). `npm run dev` and `npm run migrate` now read `backend/.env`.

**Decision (owner, 2026-11-02):** keep this Fastify + PostgreSQL backend rather than moving to Firebase or Supabase. Hosting is chosen later; development uses local PostgreSQL. Firebase may still be used for analytics and crash reports (Agent 6).

**Small UI changes (2026-11-02):** the password show/hide text is now an eye icon, and the Settings account row shows the signed-in email. Build and tests pass; not yet seen on a device.

**Landing and sign-in redesign (2026-11-02, branch `agent-3/cloud-sync`):** new `LandingScreen` replaces the Welcome screen, with the ash effect (`AshField`, `AshWord`), `AuraBackdrop` and a redesigned `AuthScaffold`. Sign-out and account deletion land on it. Guest mode stays, with a warning that data is not in the cloud and can be lost. Erase all data now deletes habits, events and chats, and keeps profile and settings. 352 tests pass, `installDebug` passed, `room_migration_check.py` passed; the look and the animation are **not yet confirmed on a device**, and the ash code on iOS is unverified.

The owner confirmed the landing and sign-in look on the phone (2026-11-03).

## Wave 2 - Agent 3 (cloud sync)

**Built:** push and pull endpoints, Room v3 (`MIGRATION_2_3`), `SyncEngine`, background sync on Android, sync status in Settings > Account, sign-out that uploads then clears the phone, Erase all data that also reaches the account. Details: [agents/agent-3-handoff.md](agents/agent-3-handoff.md).

**Verification:** backend 55/55, shared 369 tests, build and install pass, migration check passes, and the v2 to v3 migration ran on the owner's phone with real data (all rows kept). **Not yet tested on a device:** actual syncing, two devices, sign-out clearing, erase across devices. `RoomSyncLocalStore` has no automated test. Background sync not run. iOS not compiled.

**Owner test (2026-11-03):** the owner reports sign-in, sync and the rest of the Agent 3 test list work on their phone. A second real device and background sync are still not confirmed.

**Next:** the user-review work below. Real AI (Agent 4) moved to the very last item (owner decision, 2026-11-03).

**Windows note:** if `npm` prints nothing and exits 0, a 0-byte `C:\Windows\system32\npm` is shadowing it; call `C:\Program Files\nodejs\npm.cmd`.

**USB phone link:** `tools/phone_link.ps1` re-runs `adb reverse` whenever a phone connects, using `PORT` from `backend/.env`. Tested by removing the link while the phone stayed plugged in (restored within 6 s) and after a real reconnect of the phone. It must be running, and it does not start by itself after a laptop restart.

## User-review round (2026-11-03)

From a real user's review plus owner notes. No AI features.

**Built:**
- **Sign-in and guest flow:** "Continue as guest" on Landing, sign-in and create-account. The bug where a guest saw the previous session came from a session ending without a clean sign-out, which leaves that account's data on the phone. The guest path now detects it and offers Sign in or Start empty (`SessionManager.hasLeftoverAccountData`, `StartFreshAsGuestUseCase`).
- **Less friction:** the avatar opens an account sheet with one-tap sign-out, or sign-in for guests. Guests also get a banner on the dashboard.
- **Explanations:** Breaking free, Discipline and Momentum open a sheet with the rule and each habit's share (`ScoreCalculator.quitLines`, `disciplineLines`). Streaks and stat tiles respond to taps and explain themselves.
- **Visuals:** gradient habit cards (`StreakColorMapper.cardGradient`; still derived, thresholds unchanged, text color stays readable), green fill on good habits and calendar squares, completion bounce, iOS-style push and modal transitions, touch response everywhere, larger type scale, new dashboard.
- **Custom habits:** a "+ Write my own" pill on the good-habit form.
- **Coach:** one-line first message with highlighted numbers and a welcome card. Separate chats with a history (Room v4, `MIGRATION_3_4`), on the phone only (owner decision).
- **Starter guide:** replaces the old first-launch onboarding, once per device, replay from Settings. Existing users see it once.
- **Investor document:** [FEATURES.md](FEATURES.md).
- **Removed as dead after this work:** `OnboardingScreen`, `MomentumBars`, `weekBars`, `dayCompletion`, and their strings.

**Verification (2026-11-03):**
- Passed: all `:shared:testAndroidHostTest` tests, including new score-line and card-gradient tests; `:androidApp:assembleDebug`; and `tools/room_migration_check.py`.
- On the owner's phone, v3 to v4 kept every row (11 habits, 11 habit events, 5 good habits, 13 logs, 3 events), with no crash.
- Seen working on screen: Landing with "Continue as guest", the guest warning, the starter guide's first three steps on the phone, and the dashboard rings.
- Fixed because of what that testing showed: a cut-off dialog button, dark status-bar icons on the dark theme (now forced at start and re-applied once the window is attached), the guide's dimming not covering the system bars, and the guide asking for a name it already had.
- After reconnecting, the USB phone link came back by itself.
- Second phone pass (2026-11-03): the whole starter guide (8 steps, no name prompt when the name is known), the account sheet, all three score sheets, stat-tile info sheets, gradient quit cards and detail hero, green good-habit fill, push transitions, coach welcome, a coach reply and chat history all work.
- Fixed from that pass:
  - The habit detail's big counter cut off the seconds at 10+ days. `CounterBlocks` now shrinks the digits to fit.
  - Secondary text on green good-habit cards was plum, so it read pink; it is now dimmed ink.
  - Greetings used the full name and wrapped. The dashboard and the coach now use the first name.
  - The guide's bottom-bar spotlight showed a strip of card; `DropletNavBar` now measures the visible pill.
  - Tests and build pass after these fixes, and they were seen fixed on the phone.
- **Not yet seen on a screen:** the light theme, the "+ Write my own" pill, the new-chat button and chat delete.

**Known gaps:** (widgets now match the cards, see Round 3.) The second-device first-sync progress display from Agent 3 is still open.


## Round 3, tasks 1-5 (2026-11-03)

Owner brief: remove the Profile gear, legal links in one row, water fill, widget parity, long-press delete with undo, money saved, Notion/Google Calendar.

**Built:**
- **Profile:** the gear icon is gone; Settings is reached from the avatar's account sheet. This reverses Mission 1's gear, at the owner's request.
- **Account:** Privacy Policy · Terms of Use sit in one centered row (`FlowRow`); at large font sizes the second link wraps instead of clipping.
- **Water fill:** `GoodHabitFillStyleDetector` (`domain/recognition`) recognises water habits with the same matcher as the forms, so typos, synonyms and Arabic work. `GoodHabitStatsCalculator.fillLevel` gives the level (count / target, capped). The reusable `Modifier.liquidFill` (`components/LiquidFill.kt`) rises with a spring and a short splash, keeps a gentle wave while partly full, sweeps a light glint on reaching full and drains on minus or the daily reset. Animation is read only at draw time; with "remove animations" on, the level jumps. It is used on the Build card, the dashboard row and the detail card. Colors: `NixColors.water` / `waterDeep`.
- **Widgets:** the quit widget paints the same diagonal gradient and corner glow as the in-app card. The good-habit widget uses `goodFillColors`, shared with the app, so text, buttons, week squares and streak follow the fill. Water habits get a still picture of the water level, redrawn on every count change (`WidgetCardPainter`). Widgets cannot animate, so the level changes in steps. Widget ↔ app updates already went through Room plus `ObserveWidgetDataChangesUseCase`.
- **Long-press delete:** good habit, quit habit and event cards (lists and dashboard) open a sheet → confirmation naming the item → "“X” deleted · Undo" for 5 s (`PendingDeletions`, an app-wide singleton). The real delete runs only after the undo window (`DeleteItemUseCase`): a habit is marked deleted together with all its history rows (relapses / logged days), an event together with its reminders. Sync carries the delete to the account; reminders and widgets follow the data. If the app is killed inside the window, nothing is deleted.

**Verification:** `:shared:testAndroidHostTest` passes (new `GoodHabitFillStyleTest`, `PendingDeletionsTest`); `:androidApp:assembleDebug` passes, with no new warnings in touched files. On the Pixel 9 emulator, seen working: water rising at 4/10 and full at 10/10; Build card and detail card; long-press → sheet → confirm → Undo bar; undo inside the window keeps the habit; the water widget at 5/8 and at 8/8 after tapping + on the widget.

**Owner decision applied (2026-11-03):** the trash buttons on both habit detail screens and an event's Delete button now use the same delete flow as long-press (a confirmation naming the item, a real delete, 5 s Undo). The archive action is no longer used from the UI. Removed as now unused: `ArchiveHabitUseCase`, `ArchiveGoodHabitUseCase`, `DeleteEventUseCase`, their view-model functions and the old remove/delete strings. The repository `archive` methods stay, because sync and existing archived rows use them. Seen on the emulator: trash → "Delete “Smoking”?" → back to Home with "deleted · Undo" (Undo on this path was tapped after the window on the slow emulator; it is the same code that passed on the lists). Still open: the good-habit form's label reads "What do you want to stop?" (a quit-habit text). The widget picker preview images are static and still show the old flat style.

## Round 3, task 7: money-saved tracker (2026-11-03)

- **Domain:** `domain/money` holds `HabitCost`, `CostUnit`, `CountableHabits`, `MoneySavedCalculator` and `Currencies`. `CountableHabits` decides which quit habits can be counted and priced, using the habit matcher: cigarettes, vaping, pouches, shisha, soft and energy drinks, coffee, tea, alcohol, fast food. Procrastination and social media get no tracker. Savings follow the streak exactly: a relapse starts them again. The default currency is the phone's locale currency when it is in the list (16 currencies), otherwise EGP.
- **Data:** Room **v5** (`MIGRATION_4_5`, additive) adds the `cost_*` columns on `habits`. The same migration adds `source`, `external_id`, `external_updated_at` and `all_day` on `events` (for task 6), plus a unique (`source`, `external_id`) index. The cost and the event source fields travel in the sync payloads; they are optional, so older app versions ignore them.
- **UI:** an optional "Money saved" section on the quit-habit form (create and edit). Cigarettes can be counted per cigarette or per pack, with the pack size. The card shows a compact "EGP 700.02 saved · 140 cigarettes not smoked" line. The detail screen shows the live total, units avoided, daily cost, a bar per clean streak (from the second streak on) and an Edit button. The large quit widget shows the saved line in whole units; it is redrawn when the next whole unit is reached, at most every 15 minutes.
- **Verified:** `MoneySavedCalculatorTest`, `CostInputTest`, all `:shared:testAndroidHostTest`, `assembleDebug`, and `tools/room_migration_check.py` (ALL OK, v4→v5 defaults, import dedupe index). The emulator upgraded a real v4 database to v5. On the emulator: form → card shows EGP 700.02 after 7 days at EGP 100/day; the detail section is correct.
- **Fixed while testing:** the pack size was pre-filled with 1 instead of 20, because the habit was not recognised yet. The good-habit list now computes its rows off the main thread, and the habit catalog is warmed on a background thread at start (`warmUpInBackground`).
- **Not seen yet:** the money line on the quit widget; the per-streak chart (needs a relapse); the light theme.
- **Found, not changed (owner decision):** `tools/gen_widget_layouts.py` deletes resources it does not generate (`widget_counter_*`, `widget_good_compact/wide`, `widget_bg_round`, ...). I restored them after running it. Those 4 layouts look unused.

## Round 3, task 6: Google Calendar and Notion import (2026-11-03)

Agent 5's integration work, built from scratch (nothing existed in code before). Design and owner steps: README, "Linking calendars".

- **Domain** (`domain/integration`): models, `ImportPlanner`, `SyncIntegrationsUseCase` (needs a signed-in session, one run at a time, automatic runs at most every 15 min), `DisconnectIntegrationUseCase`, and the ports `GoogleAuthorizer`, `GoogleCalendarCatalog`, `NotionLink`, `CalendarSource`, `ImportedEventStore` and `IntegrationStateStore`.
  - The planner: one row per (`source`, `external_id`); an edit updates the row; a cancellation deletes it; a full read removes events missing at the source (inside the window only); a user's delete is respected unless the event is edited at the source afterwards; the window runs from yesterday to a year ahead.
- **Data** (`data/integration`):
  - Google Calendar v3 client (read-only): singleEvents, so recurring events are expanded; per-calendar sync tokens; a full re-sync on 410; all-day events become local midnight.
  - Notion client: goes through the backend; incremental by `last_edited_time`, plus a full read every 3 days.
  - Room store (imported text cleaned; reminders and category kept when an import updates the row) and a preference-backed link state (no tokens). `MaintenanceDao.clearAll` removes the link settings on sign-out.
- **Android:** `AndroidGoogleAuthorizer` (play-services-auth **21.3.0**, new dependency, Authorization API, scope `calendar.readonly`, no client id in the code). `MainActivity` hosts Google's consent screen and is now `singleTask`, with the `nixura://integrations` link. `SyncWorker` imports before the cloud sync, and the app imports on resume.
- **iOS:** Google is reported as not available (needs an iOS OAuth client and Google's iOS sign-in); Notion works through the server. **Unverified** (iOS is not built here).
- **UI:** Settings → Integrations (status, last synced, clear errors, calendar picker, Notion database and property picker with a date-property guess, sync now, disconnect with confirmation). Events show a Google or Notion badge. Imported events show "Imported from ..." with their title and date locked.
- **Backend:**
  - `migrations/003_integrations.sql` (`integration_links`, `oauth_states`); `src/integrations/notionService.ts` (OAuth with a single-use state bound to the user, query mapping, time zones via Intl, 429 retry, 401 → link dropped); `tokenCipher.ts` (AES-256-GCM).
  - Routes `/v1/integrations*`. New env vars are in `.env.example`. Migration applied to the local dev database.
- **Verified:** app `ImportPlannerTest` (7) and `SyncIntegrationsUseCaseTest` (8: import, source deletes, guests, revoked access, the interval, disconnect, date parsing, text cleaning, property guess); all `:shared:testAndroidHostTest`; `assembleDebug`; backend `npm run typecheck` and `npm test` (61 pass, 6 new: OAuth round trip, encryption at rest, single-use state, per-user isolation, revoked access, Notion date and time-zone mapping, cipher tampering).
- **Not verified (needs the owner):** a real Google sign-in and calendar fetch (needs the Google Cloud project and an Android OAuth client); a real Notion OAuth (needs the Notion public integration and an https redirect); the Integrations screen on a device (the emulator ran out of memory during the session, see below); background import in WorkManager.
- **Emulator note:** the Pixel 9 emulator runs with `hw.gpu.mode=software` and was swapping (3.5 of 4 GB used), so cold starts took 15-40 s and caused ANR dialogs while taps were queued. A main-thread sampling showed normal startup code, not a hang in the app. An A/B against the pre-round build gave the same frame skips. I did not change the AVD settings.

**Round 3 commits:** `cbd9e4d` (tasks 1-5), `1594791` (task 7), `647b7b1` + `fb8ff51` (task 6). Pushed to `origin/agent-3/cloud-sync` on 2026-11-03 (first push of this branch; no force).

**Phone check (2026-11-03, after round 3):** the owner's Android Studio checkout was still on `agent-2/accounts` (Room v3) while the phone held a v4 database from an earlier install, so the old build crashed with "A migration from 4 to 3 was required" (Room never downgrades). Not a code defect. Fix: fast-forwarded the owner's checkout to `agent-3/cloud-sync` (`ad107cf`) and installed it. On the phone the v4→v5 migration kept all data (user_version 5; 12 habits, 12 habit events, 5 good habits, 14 logs, 3 events) and the app starts. **Lesson:** after an agent installs a newer build, always build from the same branch in Android Studio.

## Google Calendar "access wasn't granted" (2026-11-04)

Owner report: Settings → Integrations → Google Calendar → Connect, pick an account, the picker closes and the app says "Google access wasn't granted."

- **Root cause (observed on the owner's phone, OPPO CPH2531, Android 15, Play services 26.42.31):** Google's server refuses the token because no Android OAuth client matches the app. Logcat, both taps:
  - `W Auth : [GetTokenResponseHandler] Server returned error: This android application is not registered to use OAuth2.0, please confirm the package name and SHA-1 certificate fingerprint match ...`
  - `W Auth.Api.Credentials: [AuthorizationChimeraActivity] Activity finished with error ... [status=UNREGISTERED_ON_API_CONSOLE]`
  - The first `authorize()` call succeeds and the consent screen opens, so the app never saw DEVELOPER_ERROR. The screen then closes with result code 0 and the old code turned that into Denied, discarding the reason. Test users and the scope were never reached.
- **Not code, the owner must do it:** create (or fix) the Android OAuth client in Google Cloud Console, README "What you need to do: Google Calendar", step 4. Package `com.example.reclaimapp` (now `com.adhamelgendy.nixura`, see "App id rename"), debug SHA-1 `9B:AA:D3:58:3F:F2:3A:E5:42:09:62:76:73:71:34:C8:FC:88:C3:F3` (checked with `apksigner` on the built APK and on the APK installed on the phone; same key as `~/.android/debug.keystore`).
- **Code changes:**
  - `AndroidGoogleAuthorizer` logs every failure as `Log.w` under the tag `NixUraGoogleAuth`: the step, Google's status code and name, the message and the consent screen's result code. Never the token or the account.
  - On a result other than RESULT_OK the authorizer still reads the result intent, because Google puts the failure status there.
  - New `GoogleAuthResult.Cancelled` (screen closed without data, or status CANCELED) with the notice `GOOGLE_CANCELLED` and the string `googleCancelled` ("Google Calendar wasn't connected. Tap Connect to try again."). Background syncs treat it like Denied.
  - `UNREGISTERED_ON_API_CONSOLE` (Google sends it as status 8, INTERNAL_ERROR, with the name only in the message) now maps to NotAvailable, so the app says "Google Calendar isn't set up in this build yet" instead of "wasn't granted".
  - `MainActivity` passes the whole `ActivityResult` (code and data) to the authorizer.
- **Verified:** `:shared:testAndroidHostTest` (408 tests, 0 failures) and `:androidApp:assembleDebug`. On the phone with the logging build: `W NixUraGoogleAuth: consent (result code 0) failed: status=8 (INTERNAL_ERROR) message=8: [8] Unknown error [status=UNREGISTERED_ON_API_CONSOLE].`
- **Then (same day):** the owner created the Google Cloud project and the Android client; Connect worked on the phone.

**Follow-up on the phone, same day: what to import, reconnect, categories**

- **Reconnect showed "Connected" with nothing to pick.** After Disconnect (which revokes the token), Google's services kept handing out the revoked cached token, the calendar list got 401 and the picker stayed empty without a message. Fix: `GoogleAuthorizer.forget(token)` (Android: `GoogleAuthUtil.clearToken`), called after revoke and on any 401; the list is asked for once more; if it still fails the link is not saved and the app says it could not sync.
- **A later reconnect spun and stopped:** Google's own services timed out (`getToken() -> NETWORK_ERROR`, "Waited 30 seconds", twice); the app showed "You're offline". Network, not app code.
- **Owner request: four entries instead of a calendar list** (`GoogleFeed`): Events (primary calendar, `eventTypes` default + fromGmail), Birthdays (primary, `eventTypes=birthday`), Tasks (new: Google Tasks API, scope `tasks.readonly`, tasks with a due date as all-day events, done or deleted tasks removed, read in full each sync; if the Tasks API is off the rest still imports) and Holidays for the user's country (`en.<cc>#holiday@group.v.calendar.google.com`, the country from the new `DeviceRegion` port: Android mobile network, then SIM, then region setting; iOS region setting; fallback: the holiday calendar the account follows; off by default). Other calendars are no longer listed.
- **Bug: an entry turned off never came back.** The importer's own removals (entry off, Disconnect) were stored like user deletes, which are remembered. Fix: such rows carry `REMOVED_BY_IMPORT` (-1) in `external_updated_at` and come back when listed again; Disconnect marks every row from the source this way (a new link starts fresh). No schema change.
- **Bug: the last entry turned off stayed.** With nothing picked the sync stopped as "not configured" and removed nothing. Now an empty selection is a full, empty read.
- **New items only arrived on "Sync now".** The app now imports every 2 minutes while it is open (`FOREGROUND_SYNC_EVERY_MILLIS`); background stays at 15 minutes. Google Tasks has no push.
- **Categories:** new `EventCategory.EVENT`, `BIRTHDAY`, `TASK`, `HOLIDAY` (added after `OTHER`, so stored values keep their meaning); imports set them per feed; a category the user picked is kept (the import only fills in rows still on Other). The event form lists Other last (`EventCategory.pickerOrder`). The chips got a hint: "Highlighted items are imported. Tap one to turn it on or off."
- **Notion "You're offline":** setup, not code. Debug builds use `nixuraDebugApiBaseUrl=http://localhost:8081` (owner's `~/.gradle/gradle.properties`); no backend runs there, `backend/.env` does not exist, and a USB phone also needs `adb reverse tcp:8081 tcp:8081`.
- **Verified:** `:shared:testAndroidHostTest` (417 tests, 0 failures; new `GoogleImportTest`, a planner test for rows the importer removed) and `:androidApp:assembleDebug`. On the owner's phone (owner-confirmed): the four entries import; turning entries off and on, and Disconnect + Connect, bring items back; turning all off removes everything; new tasks arrive without "Sync now"; Event, Birthday and Task categories.
- **Not verified:** the Holiday category and Other-last order (built and unit-tested; the phone was unplugged before install); the cancel message; iOS (`IosDeviceRegion` and the `GoogleAuthorizer.forget` stub are not compiled here; on a Mac run `:shared:compileKotlinIosSimulatorArm64`); background import in WorkManager.
- **Emulator note:** the Pixel_10_Pro AVD (Android 37, Play Store image) opened its window off screen, lost its adb link after a quick boot, and froze once on a cold boot with the host GPU. A cold boot with `-gpu swiftshader_indirect` worked; the test then moved to the owner's phone.

## Recovery timeline redesign (2026-11-04)

Owner request: make "How your body & mind recover" on the quit-habit detail screen a real timeline that is good to look at.

- **What changed** (`BadHabitDetailScreen.kt`, private `RecoveryTimeline`, `TimelineStep`, `TimelineNode`, `EarlierStepsToggle`; 9 strings in `Strings`): summary row ("9 of 13 reached" + overall bar); a vertical rail drawn with `drawBehind` (green for reached, filling green→accent toward the next step as time passes, grey ahead); 28 dp nodes (ticked green, pulsing accent ring for the next step, grey for later); each step has an "AFTER 3 DAYS" eyebrow and "Reached" or "in 5 days"; the next step sits in a tinted "Up next" panel with its own progress bar; only the last 2 reached steps show, the rest fold behind a "+N" node. The pulse is still when the phone reduces motion (`rememberReducedMotion`). No shared component changed.
- **Verified:** `:shared:testAndroidHostTest` (417 tests, 0 failures) and `:androidApp:assembleDebug`; on the emulator (Pixel_10_Pro, Android 37) with a smoking habit at 8 days: the rail, nodes, Up next panel and the fold open/close. An independent check of the change against its checklist passed all 8 lines; its 4 findings (reduced motion, `key()` per step, "+N" through `Strings`, no card when a habit has no milestones) are fixed.
- **Not verified:** light theme, the owner's phone, iOS (not compiled here).

## App id rename (2026-11-05)

Owner decision: before registering the app with Firebase, replace the placeholder id `com.example.reclaimapp` with **`com.adhamelgendy.nixura`** everywhere, code packages included.

- **What changed:** Android `applicationId` and `namespace` (`androidApp/build.gradle.kts`), the shared library namespace `com.adhamelgendy.nixura.shared`, every Kotlin package (all source sets moved from `com/example/reclaimapp/` to `com/adhamelgendy/nixura/` with `git mv`), the widget `android:configure` class names, ProGuard keep rules, `tools/gen_legal_text.py`, `tools/room_migration_check.py`, and the iOS `PRODUCT_BUNDLE_IDENTIFIER` (`com.adhamelgendy.nixura`; the template's `$(TEAM_ID)` suffix is gone so the id is fixed). No behaviour changed. The Room database name and schema are unchanged.
- **Verified:** clean `:androidApp:assembleDebug` and `:shared:testAndroidHostTest` (417 tests, 0 failures).
- **Not verified:** iOS (not compiled here). On a Mac run `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:iosSimulatorArm64Test`, then build `iosApp/iosApp.xcodeproj` in Xcode.
- **Owner must do:** phones see a new app, so uninstall the old `com.example.reclaimapp` build (its local data does not carry over). In Google Cloud Console create a new Android OAuth client for package `com.adhamelgendy.nixura` with the same debug SHA-1, or Google Calendar connect fails with `UNREGISTERED_ON_API_CONSOLE`; the old client can then be deleted. Register the Firebase Android app with the new package.

## Firebase accounts (2026-11-08, branch `feat/firebase-auth`)

Owner decision (2026-11-05): accounts move from the Node backend to Firebase Authentication (project `nixura`, Spark plan); sync moves to Firestore next; Notion and the AI coach move to Cloudflare Workers.

- **What changed:** `data/auth` calls Firebase Authentication's REST API from shared code (`FirebaseAuthApi`, `FirebaseSession` for the 1-hour ID token with single-flight refresh, `FirebaseAuthRepository`). Email + password: sign-up mails a **verification link**, and the account is only signed in once it was tapped (the "Check your email" screen checks every 4 s and on "I've confirmed it"); "Forgot password?" mails a **reset link** to Firebase's page. Google sign-in (Credential Manager) and Facebook Login on Android through the new `SocialSignIn` port; iOS offers email + password only. Removed: 6-digit codes, the Reset-password screen, Settings > Account > Devices and "sign out everywhere" (Firebase has no device list; changing the password still ends other sessions). Accounts without a password hide Change password and delete without one. Settings > Privacy has "Share anonymous usage stats" (Firebase Analytics, off by default via the manifest, per device, not synced). Facebook's auto events and advertising id are off; its SDK only starts when a client token is set. `google-services.json` is in `.gitignore`.
- **Cloud sync is paused** (`PausedSyncRemote` reports offline): the old server cannot check Firebase sign-ins. Changes stay queued; signing out warns that they were not uploaded. The Notion link still calls the old server and will fail until it moves to a Worker.
- **Verified:** `:androidApp:assembleDebug` and `:shared:testAndroidHostTest` (428 tests, 0 failures; 21 new for the Firebase flows, token refresh and error mapping). On the emulator (Pixel_10_Pro): the app starts, Login and Create account show "Continue with Google" under an "or" divider, and a reset request for a made-up address reached the Firebase project and showed "Check your email". An independent check of the screens found double error messages, both sign-in paths running at once, a carried-over social error, stale "code" wording, dead OTP UI and a divider read aloud as "or"; all fixed.
- **On the owner's phone (2026-11-08):** Google sign-in and Facebook sign-in **work** (owner-confirmed). First tries failed: Firebase answered `MISSING_REQUEST_URI` because the request JSON skipped default-valued fields (`requestUri`, `returnSecureToken`; this also affected email sign-up and sign-in); fixed with `FirebaseRequestJson` and a test. Facebook needed the `email` permission added to the Meta use case. Creating an account for an email that already had one (here: the Google account) used to pretend it worked and then showed "Your session ended"; it now says the email already has an account (`AuthErrorKind.EmailInUse`), since Firebase's sign-up reveals that anyway. Sign-in failures log Firebase's error code under the `NixUraSignIn` tag (never tokens or emails).
- **Also on the phone:** creating an account with a new email, tapping the verification link and being signed in by the "Check your email" screen works (owner test, no sign-in errors in the log).
- **Not verified:** password reset end to end, Analytics events arriving, iOS (not compiled here: run `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:iosSimulatorArm64Test` on a Mac, add `GoogleService-Info.plist`).
- **Owner must do:** add `nixuraFacebookClientToken=...` to `~/.gradle/gradle.properties` (Meta > App settings > Advanced > Client token); add Facebook testers under App roles while the Meta app is unpublished; register a release SHA-1 (Firebase) and key hash (Meta) before publishing.
- **Known gaps:** NixButton drops its label while loading and NixSwitch has no toggle role for screen readers (older, app-wide). `DeviceInfoProvider` and `HttpSyncRemote` are unused until the Firestore step replaces them.

## Firestore sync design (agreed 2026-11-08, not built)

Owner answers: servers (the AI coach Worker, a future dashboard) will read the data, so it is stored as readable fields; 500 to 1,000 daily users, stay on Spark as long as possible without lowering quality.

- **Layout:** `users/{uid}/sync/{type~id}` with `type`, `updatedAt` (device edit time, decides the winner), `serverTime` (set by Firestore, the pull cursor with the document name as tie-break), `deleted`, and `data` (the item's fields). One query per pull, so an idle sync costs 1 read.
- **Good-habit days are grouped by month** (`goodlog~{habitId}~yyyy-MM`, one field per day with its own timestamp): 12 documents a year per habit instead of 365. This keeps storage near 0.2 MB per user per year (one document per day would fill Spark's 1 GiB in under a year at 1,000 users).
- **Security rules:** a user reaches only `users/{uid}`; an older `updatedAt` cannot overwrite a newer one; timestamps far in the future are refused; `serverTime` must equal the request time; items are capped at 8 KB.
- **Writes** use non-atomic `batchWrite`, so a refused stale item does not block the rest (the same as the old server's per-item "applied").
- **Indexes:** `data` is exempt from indexing; only `type` and `serverTime` are indexed.
- **Deletes** keep a small marker with the content removed. Deleting an account deletes its documents first, then the account; leftovers can be swept later by the Worker.
- **Quota exhausted:** Firestore refuses until midnight Pacific; the app treats it as offline and keeps every change.
- **Budget at 1,000 daily users:** about 30k reads and 15k writes a day (limits 50k / 20k).
- **Rollback:** the remote sits behind `SyncRemote`; switching back to `PausedSyncRemote` is one line, and rules keep a version history in the console.

## UI polish (2026-11-10, branch `feat/ui-polish`)

Owner requests after the Firebase sign-in test.

- **Stacked sign-in screens (bug):** the "Create account" / "Sign in" links pushed a new screen each time, so Back walked through every copy. `Navigator.switchTo` returns to the screen if it is already open and only pushes otherwise (2 tests in `NavigatorTest`).
- **Google and Facebook logo buttons:** two buttons side by side with the providers' own marks (`auth/SocialLogos.kt`, drawn from their published outlines and colours), shaped like the secondary button; the label is kept for screen readers.
- **Profile:** centered identity card, the two scores as tiles side by side, a "Totals" label over the four stat tiles, full-width empty-event card.
- **Settings:** five sections instead of eight (Account, Appearance, Notifications, Privacy and data, About); every row has an accent icon, a title and a muted hint.
- **Verified:** `:androidApp:assembleDebug` and `:shared:testAndroidHostTest` (431 tests, 0 failures); on the emulator (Pixel_10_Pro, dark theme, guest): the Login screen's logo buttons, Settings top to bottom, Profile with no data. An independent check of the screens passed tokens, the logo button, the Settings rows, Profile and `switchTo`; its 5 findings are fixed (the leftover-data dialog's "Sign in" still pushed; the disabled logo button did not dim like the secondary button; a score title could truncate; the Language and Replay rows had no hint; the Account row repeated its section label).
- **Not verified:** the owner's phone, light theme, Profile with real data and a long name, iOS (not compiled here).
- **Second pass (2026-11-10, owner feedback on the phone):** the account sheet comes back when you press Back from Profile or Settings opened from it (`Navigator.pushFromAccountSheet` / `takeAccountSheetReturn`, 2 tests); the sheet no longer has an Account row (it is in Settings); the Account screen is rebuilt in the Settings row style with sections (Cloud sync, Sign-in and security, Legal, Danger zone); buttons never cut their label (`NixButton` and `AuthGradientButton` wrap and grow; `ConfirmDialog` stacks its buttons when the labels are long, which fixes "Sign out anyway"). Verified: build, 433 tests, and on the emulator as a guest (sheet return, the guest Account screen). Not verified: the signed-in Account screen and the stacked dialog on a phone, light theme, iOS.
- **Still open from the same list:** the look of the verification and reset emails (Firebase > Authentication > Templates: sender name, subject, reply-to; later a custom domain and an action page on Firebase Hosting).

## Branded email link page (2026-11-10, branch `feat/auth-action-page`)

Owner request: the email links should open a NixUra page, not Firebase's grey one. Firebase locks the message text of these emails (anti-spam), so only the sender name, subject, reply-to and the link target can change.

- **What changed:** Firebase Hosting config (`firebase.json`, `.firebaserc`, project `nixura`) and `hosting/public/`: `auth/action.html` + `assets/auth-action.js` handle `verifyEmail`, `verifyAndChangeEmail`, `recoverEmail` and `resetPassword` with the Identity Toolkit REST API (password rules mirror `AuthValidation.kt`); a small home page, a 404 page, a favicon, and `assets/nixura.css` with the app's dark palette. No storage, no analytics, the one-time code is removed from the address bar, `continueUrl` is ignored, strict CSP and `Referrer-Policy: no-referrer`.
- **Verified:** served locally with `firebase serve`; every screen checked in a browser with faked Firebase answers (confirmed, expired link, no connection, password form: empty, too short, saving, saved; phone width; 404). The project's API key accepts calls from a web page (Firebase answered `INVALID_OOB_CODE` for a made-up code, not a key error).
- **Deployed (2026-11-11):** live at `https://nixura.web.app/auth/action`; the live page returns its CSP, `no-store` and `no-referrer` headers, and its request reaches the Identity Toolkit (not blocked by the CSP).
- **Blocked (2026-11-11):** the console cannot save the action URL. The request `PATCH identitytoolkit .../v2/projects/nixura/config` with `callbackUri` is refused with `400 EMAIL_TEMPLATE_UPDATE_NOT_ALLOWED`; the same error refuses a new subject. Only the sender name saves (now "NixUra" on the verification and reset emails; the subjects already read "... for NixUra" through the public-facing name). No Firebase documentation for this error was found; community answers point to anti-spam limits on template editing. So the emails still link to Firebase's own page and the hosted page is unused until one of: (a) custom SMTP (Templates > SMTP settings) or a custom domain, then retry; (b) send these emails from our own server (Admin SDK `generateEmailVerificationLink` / `generatePasswordResetLink` in the planned Cloudflare Worker, through an email service), which points the link at our page and frees the text; (c) ask Firebase support with the error code.
- **Not verified:** a real email link end to end.

**History cleanup (2026-11-03, owner request):** commits from `14810ff` onward were rewritten to remove assistant co-author lines and to carry the owner as author (Adham ELgendy). Code is byte-for-byte unchanged (same tree); commit IDs changed and the IDs above were updated. Force-pushed with lease to `master` and `agent-3/cloud-sync`. A local backup of the old history is kept as branch `backup/before-rewrite-2026-11-03`.

## Weekly good habits (2026-11-11, branch `feat/weekly-good-habits`)

Owner request: a good habit that is done a few days a week (e.g. medicine 3 times a week) must not lower the Discipline score on the days it is not due. Owner answers: any days (just a count), week = Sunday to Saturday, a day counts once, never punish early.

- **What changed:** `good_habits.weekly_target` (Room v6, `MIGRATION_5_6`, null = daily), `GoodHabit.weeklyTarget`, sync payload field (optional, old records stay daily). Rules in `domain/calculator/WeeklyGoalCalculator.kt` (14 tests): goal met = 100% (more never goes above); the current week is left out of the score while still reachable, counts done/goal once it cannot be reached; finished weeks count done/goal; the creation week's goal is ceil(goal x days left / 7). `ScoreCalculator` averages only habits that count (a lone new weekly habit gives no score; Profile then says weekly habits count once the goal is met). Stats: streaks and completed weeks in weeks, "this week" and "last 4 weeks" completion. UI: form "How often" (Every day / Days a week, stepper 1-7), card and Home row fill with the week's progress and show "N / M days this week", the strip shows Sunday to Saturday, a done day shows a tick and cannot go above 1 (also the widget and past-day edits). Coach and the daily check-in skip a weekly habit that is done today or whose week is met. Also fixed: the good-habit form's name field asked "What do you want to stop?" (now "What do you want to build?").
- **Verified:** `:shared:testAndroidHostTest` (447 tests, 0 failures) and `:androidApp:assembleDebug`; `tools/room_migration_check.py` (the two "SQL ERROR" lines about `$REMOVED_BY_IMPORT_SQL` are a checker limitation and also appear on master); on the emulator (Pixel_10_Pro) installed over an existing v5 install: the app opened (migration ran), a weekly habit was created on a Wednesday ("0 / 2 days this week"), two taps counted once with a tick, the detail screen showed weeks, days done, this week / last 4 weeks. An independent check of the screens found 7 points; fixed: the widget total said "times", the score tip wording, "Days per week" vs "Days a week", today shown as "Missed" in the day editor (now "Not done yet") and a dead "+" on a done weekly day. Left as they were: the Home row tick rule differs from the card for daily habits with a target above 1 (older), the month calendar starts on Monday while the weekly week starts on Sunday, the form heading style.
- **Not verified:** the home-screen widget with a weekly habit, switching an existing habit between daily and weekly on a device, light theme, iOS (not compiled here: `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:iosSimulatorArm64Test` on a Mac). The Firestore sync design must carry `weeklyTarget` in the good-habit document.

## Notion Worker (2026-11-11, branch `feat/notion-worker`)

Started on step (c) before (b), with the owner's go-ahead: the Notion Worker doesn't depend on Firestore.

- **What:** `workers/notion/`, a Cloudflare Worker (TypeScript, D1, Rate Limiting binding) with the same routes and answers as the Notion part of `backend/`. The app already sent Firebase ID tokens through `AuthorizedApi`, so only its address changed. The Worker checks each token with Google's public keys (`jose`).
- **Live:**
  - Address: `https://nixura-notion.nixura-notion-worker.workers.dev`, on the owner's Cloudflare account. The workers.dev subdomain `nixura-notion-worker` was picked by Wrangler on the first deploy and can be renamed in the dashboard; the app's address would then change too.
  - D1 database `nixura-notion` with migration `0001_notion.sql` applied.
  - Secret `INTEGRATION_TOKEN_KEY` set: 32 random bytes generated straight into Cloudflare, never printed.
  - Secrets `NOTION_CLIENT_ID` and `NOTION_CLIENT_SECRET` set on 2026-11-11 from the owner's Notion OAuth connection, with Redirect URI `https://nixura-notion.nixura-notion-worker.workers.dev/v1/integrations/notion/callback` (README, "What you need to do: Notion").
- **App:**
  - `NIXURA_API_BASE_URL` now defaults to the Worker in debug and release builds. The separate debug override `nixuraDebugApiBaseUrl` is removed: the owner's `~/.gradle/gradle.properties` line `nixuraDebugApiBaseUrl=http://localhost:8081` now does nothing and can be deleted.
  - iOS `Info.plist` and the iOS fallback also point at the Worker.
  - `FirebaseAuthRepository.deleteAccount` removes the Notion link once the password is confirmed and before Firebase deletes the account. The Worker can't learn about deleted Firebase accounts, unlike the old backend's `ON DELETE CASCADE`.
- **Verified:**
  - `npm test` in `workers/notion`: 10 tests, 0 failures. They are ported from the backend's Notion tests, plus Firebase token checks, input checks, limits and reading the backend's cipher format.
  - `npm run typecheck` is clean.
  - `:shared:testAndroidHostTest`: 434 tests, 0 failures, with a new test that deletion erases server data only after the password is checked.
  - `:androidApp:assembleDebug` OK; the generated `BuildConfig` has the Worker address.
  - Live checks with `curl`: `/health` answers `{"ok":true}`; signed-in routes without a valid token answer `401`; unknown routes answer `404`; the callback answers `503 integration_unavailable` until the Notion keys are set; `Cache-Control: no-store` and `nosniff` are present.
- **Verified on the owner's phone (2026-11-11):** the owner created the Notion OAuth connection "NixUra" and set `NOTION_CLIENT_ID` and `NOTION_CLIENT_SECRET`. A build of `feat/weekly-good-habits` pointed at the Worker was installed, and the owner connected Notion from Settings and reported it working. The live D1 database then held one `notion_links` row for the owner's workspace, which also proves a real Firebase ID token passes the Worker's check.
- **Not verified:**
  - Importing events after picking a database was reported by the owner, not checked row by row.
  - Disconnect, and the Notion cleanup on account deletion, against the live Worker.
  - iOS.
- **To do in Notion (owner):** the connection still had Update content, Insert content, Insert comments and "user information including email" ticked on 2026-11-11. NixUra only needs Read content and No user information.
- **Risks:**
  - The rate limits are counted per Cloudflare location, not globally.
  - A user deleted directly in the Firebase console keeps a Notion row until someone removes it by hand.
  - `backend/` still contains the old Notion code: remove it once the Worker is confirmed with real Notion.

## AI coach and voice notes (2026-11-11, branch `feat/ai-coach`)

Owner decisions this round: start the AI now (before Firestore sync); a chain of free AI services, not Workers AI alone; 10 messages and 2 voice notes per user per day for now, lower later with a paid plan; signed-in users only; voice notes show what was understood before adding; the coach sees habits and the next 30 days of events.

- **Worker** (`workers/notion/src/ai`, deployed to `https://nixura-notion.nixura-notion-worker.workers.dev`):
  - Routes `POST /v1/coach/chat`, `POST /v1/coach/voice`, `GET /v1/coach/quota`, all behind the Firebase sign-in check and a 12-a-minute limit per user.
  - Chain `AI_CHAIN` = gemini, groq, cloudflare, mistral, openrouter. A service without its key is skipped; a failure moves to the next; only the service name and status are logged.
  - Voice: Gemini hears the clip directly; otherwise Groq or Cloudflare Whisper turns it into text and any chat model picks out the items.
  - Daily limits in D1 `ai_usage` (migration `0002`), reserved in one statement and refunded when no AI answered.
  - Every model answer is cleaned: unknown action types dropped, names cut, dates and times checked, at most 8 items.
  - Keys set on 2026-11-11: `GEMINI_API_KEY`, `GROQ_API_KEY`, `OPENROUTER_API_KEY`. Not set: `MISTRAL_API_KEY`.
- **App:**
  - Real AI in the coach chat with offline fallback and a notice; consent sheet; "AI coach" switch in Settings.
  - Animated progress cards in the chat (also from the offline coach).
  - Voice notes: mic card on Home and mic button in the coach; review list; adds quit habits (counter starts now), good habits (daily or N days a week) and events (09:00 when no time was said, no reminders).
  - Room v7: `coach_messages.visuals` (additive `MIGRATION_6_7`).
  - HTTP timeout raised from 25 to 45 seconds (a voice note can take the chain that long).
  - Privacy text in the app names the AI services.
- **Verified:**
  - `npm test` in `workers/notion`: 17 tests, 0 failures; `npm run typecheck` clean.
  - `:shared:testAndroidHostTest`: 454 tests, 0 failures (new: the AI client's requests, answers and error cases; the offline card picker). `:androidApp:assembleDebug` OK.
  - `tools/room_migration_check.py`: only its two known `REMOVED_BY_IMPORT` complaints.
  - Emulator (Pixel_10_Pro), installed over a v6 app: the app opens (v6 → v7 ran); the Home mic card; the voice sheet's "needs an account" view; the coach's mic button and guest subtitle; an animated good-habit card under an offline reply.
  - Live Worker: deployed, `ai_usage` exists, the coach routes answer 401 without a sign-in.
- **Verified on the owner's phone (2026-11-11, owner-reported "both worked"):** AI chat and a voice note, signed in, with the Gemini, Groq and OpenRouter keys set (Mistral's console would not create a key; skipped). The Worker's log and `ai_usage` showed 4 chats and 1 voice note answered.
- **Found in that log and changed:** Gemini (`gemini-flash-latest`) failed every time (call errors and 503 "overloaded"), so the default is now `gemini-flash-lite-latest`; Groq answered 400 on some calls in JSON mode, so JSON mode is off for Groq (answers are parsed leniently anyway); the per-call chat timeout went from 9 to 11 seconds, and timeouts are now logged as 408. Re-tested by the owner right after: one chat and one voice note, both answered by Gemini with no fallback in the log.
- **Not verified:**
  - The daily limit being reached, and OpenRouter and Mistral as answering services (the chain never got that far).
  - The model names in `wrangler.jsonc` (`gemini-flash-latest`, `openai/gpt-oss-120b`, `mistral-small-latest`, `openrouter/free`, the two `@cf/...` models) against each provider: a wrong one just makes that service fail over to the next.
  - Arabic voice notes. iOS (shared code not compiled here; the recorder is a placeholder).
- **Risks:**
  - Free quotas are per app, not per user: with many users the chain ends at the offline coach more often.
  - Gemini's free tier terms (data use, no personal information, not for EU/UK/Switzerland users): fine for testing, to be settled before a public launch.
  - The consent is stored on the device only; the Worker cannot check it.

## To-do list (owner, 2026-11-11)

Not started; kept here so they are not lost.

1. **Polish pass over the whole app:** more motion, smoother transitions, small "hooks" that make coming back feel good (in a healthy way).
2. **Specialised habit cards**, like the water card with its water motion: one for prayers, one for smoking, and more ideas of that kind.
3. **Paid plan** for more AI messages and voice notes; then lower the free limits (for example 5 messages and 1 or 2 voice notes).
4. **An AI model that runs on the phone** (free, private, offline); needs a large download and a recent phone.
5. **Voice notes on iOS** (`AVAudioRecorder` + `NSMicrophoneUsageDescription`).
