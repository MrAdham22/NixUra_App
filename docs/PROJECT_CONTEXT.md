# NixUra — Project Context

Canonical product and architecture reference. **Authoritative detail:** [PHASE2_ARCHITECTURE.md](PHASE2_ARCHITECTURE.md). **Current state:** [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md).

## 1. Product

**NixUra** (user-facing) — help users quit or reduce unwanted habits by tracking time since last occurrence, visualizing progress, and (later) AI coaching. Name evokes removing negative patterns and reclaiming control.

**Name:** NixUra everywhere (Gradle project `NixUra`, launcher label, iOS app name). **App id:** `com.adhamelgendy.nixura` for the Android applicationId, the Kotlin packages and the iOS bundle id (owner decision 2026-11-05, replacing `com.example.reclaimapp`). It must not change after publishing. Modules stay `:androidApp` / `:shared`.

## 2. Problem & experience

Users need persistent, honest progress (not a fragile timer), relapse support without shame erasure, and a premium, warm UI. MVP focuses on reliable offline quit tracking with a distinctive visual identity.

## 3. MVP scope

**In:** multi-habit model (UI may emphasize one habit), onboarding (later), dashboard timer, relapse logging, history edit/delete, archive habit, NixUra design system, streak-based habit-card colors.

**Deferred:** AI coach, backend, accounts, cloud sync, reduction goals UX, event tags, notifications/milestones infrastructure, `CoachRepository` stubs.

## 4. Confirmed product decisions

- Android and iOS are both target platforms. Android was built first and runs; iOS is approved work (bring-up + notifications), not yet built or run.
- Offline-first; no accounts in tracking MVP.
- Event history = source of truth.
- Relapse resets **active** period only; history + longest streak remain.
- Tracking start: now or backdated last occurrence (`TRACKING_START` vs `RELAPSE`).
- Continuous elapsed active streak.
- Transactional habit + initial START creation.
- Palette applies **app-wide**; habit-card background additionally follows active streak.

## 5. Palette & design principles

| Token | Hex |
|-------|-----|
| Navy | `#1D1A39` |
| Purple | `#451952` |
| Plum | `#862549` |
| Rose | `#AE445A` |
| Orange | `#F39F5A` |
| Blush | `#E8BCB9` |

Premium, warm, hierarchical; not minimal-empty; not cluttered. Semantic tokens for surfaces, actions, text, states. Error color exception documented in theme (not from palette).

## 6. Habit-card streak colors

- Derived from **active streak duration** via `StreakColorMapper` (presentation).
- Six-stop progression dark → light; provisional thresholds 1d, 3d, 7d, 14d, 30d (configurable).
- Deterministic; survives recompute after edits; resets visually on relapse.
- Text + stage indicator — not color alone.

## 7. Technical architecture

KMP + Compose Multiplatform + Koin + Room KMP (Phase 4, after compatibility gate). **Clean Architecture** in `:shared`, with **MVVM** in the presentation layer:

- **View:** Compose screens/components render state and forward user events.
- **ViewModel:** Exposes observable UI state, invokes domain use cases (no Room/DAO access).
- **Model (domain):** Use cases, entities, and rules — not Android-specific types in shared domain code.

Not every screen must have a ViewModel immediately (e.g. static previews); production flows should use ViewModels as features land.

## 8. Package responsibilities

See [PHASE2_ARCHITECTURE.md](PHASE2_ARCHITECTURE.md).

## 9. Domain (planned / partial)

**Models:** `Habit`, `HabitEvent`, `TrackingState`, enums `GoalType`, `HabitEventType`.

**Repositories (interfaces):** `HabitRepository`, `HabitEventRepository`, `TrackingRepository`, `UserPreferencesRepository`.

**Use cases:** `CreateHabitAndStartTracking`, `RecordRelapse`, `UpdateHabitEvent`, `DeleteHabitEvent`, `ArchiveHabit`, observe dashboard state.

**Services:** `TrackingStateCalculator`, `HabitEventValidator`, `MilestoneEvaluator` (thresholds TBD).

## 10. Streak & validation

Distinct: current elapsed, completed period lengths, longest streak, milestone eligibility. Validator: one START per active habit lifecycle rules, no future-dated events beyond tolerance.

## 11. Persistence (planned)

Room v1: `habits`, `habit_events`. Schema export; explicit migrations; no destructive fallback in release.

## 12. Live timer

ViewModel combines event-driven Flow + periodic tick; domain/calculator uses `Clock` and persisted anchors.

**Display format** (Phase 3 preview via `formatElapsed` in presentation): under 1 hour `MM:SS`; from 1 hour until 1 day `HH:MM:SS`; from 1 day `Dd HH:MM:SS` (leading zeros on time fields).

## 13. AI (future)

Backend proxy; domain use case + data client when specified — not in tracking MVP.

## 14. Testing & compatibility

Unit tests for calculator and color progression; Room tests after Phase 4 gate. Room/KSP/Kotlin/AGP verification required before persistence.

## 15. Open / provisional

- Color stage thresholds (review in Phase 3).
- Onboarding fields, coaching styles, milestone schedule.
- `unarchiveHabit` in MVP?
- License for README.
- DB file naming at Room implementation.

---

## Update — October 2026 (supersedes the MVP scope above where they conflict)

The owner approved expanding the app. Current scope:

- **Quit habits** (original core) with user-chosen card colors (`ShadeRamp`), analysis screen, recovery timeline from an offline knowledge base (drawn as a vertical timeline: `RecoveryTimeline` in `BadHabitDetailScreen.kt`), backdated start, edit/archive.
- **Good habits:** daily target counters (`good_habits`, `good_habit_logs` keyed by local day index), streaks, 7/30-day completion, calendar with green done squares, past-day edits.
- **Events:** countdown/count-up, categories, multiple reminder lead times (`events.reminders` = comma-separated millis), Android local notifications.
- **Coach:** offline, rule-based (`CoachEngine`, `HabitKnowledgeBase`) behind `CoachResponder`; separate chats in `coach_conversations` and `coach_messages`, kept on the phone and not synced (owner decision); crisis messages answered locally with a support message; alcohol/gambling carry safety notes. No network.
- **Profile & settings:** name/email/age stored locally in `preferences` (key/value); settings: dark/light, language (English only), notifications, daily check-in time, erase all data; an interactive starter guide once per device (`presentation/guide`), replayable from Settings.
- **Scores (derived, not stored):** "Breaking free" = average progress of active quit habits toward 30 days; "Discipline" = weighted good-habit completion over 7 days; Momentum = average of available scores.

Still **out of scope:** ads. Accounts and cloud sync now exist (see "Accounts and cloud sync" below); online AI and analytics are planned.

Persistence: Room **v5** — tables `habits` (+`color_argb`, + the money tracker's `cost_*` columns), `habit_events`, `good_habits`, `good_habit_logs` (unique `good_habit_id`+`day_index`), `events` (+`source`, `external_id`, `external_updated_at`, `all_day`; unique `source`+`external_id`), `coach_conversations`, `coach_messages` (+`conversation_id`), `preferences`. The first five also have `sync_id`, `updated_at`, `deleted_at` and `dirty`. `MIGRATION_1_2` … `MIGRATION_4_5` are hand-written. No destructive fallback.

## Accounts and cloud sync

Accounts are optional and live on **Firebase Authentication** (project `nixura`, Spark plan; owner decision 2026-11-05, replacing the Node backend's accounts). Ways in: email + password with a verification link (the account is only signed in after the link is tapped) and a reset link (both open the NixUra page `hosting/public/auth/action.html` on `nixura.web.app` once the console's action URL points there), Google and Facebook (Android; iOS email only for now). The app calls Firebase's REST API from shared code; tokens stay in `SecureStore`. Signed out, the app shows `LandingScreen` ("Let's Nix the bad Aura"); Get Started opens sign-in. "Continue without an account" warns first, and guests make no network calls, except anonymous usage stats if they turn them on in Settings > Privacy (Firebase Analytics, off by default, Android only). **Cloud sync is paused** (`PausedSyncRemote`) until it moves to Firestore; the agreed layout is in `docs/DEVELOPMENT_STATUS.md` ("Firestore sync design"). Signed in, `SyncEngine` keeps Room and the server in step: newest edit per item wins, deletes are soft, and local `Long` ids stay while `sync_id` identifies a row across devices. Sign-out uploads, then clears the phone. Erase all data removes habits, events and chats, also from the account when signed in, and keeps profile and settings. See `docs/agents/agent-2-handoff.md` and `docs/agents/agent-3-handoff.md`.

Time: local "day index" = days since 1970-01-01 in the device time zone (`DayCalculator`, offset from the platform: `java.util.TimeZone` on Android, `NSTimeZone` on iOS).

Notifications: `NotificationPlanner` (pure) builds event reminders + next two streak milestones per habit + 14 daily check-ins; `NotificationSyncService` re-plans while the app is open; Android `AndroidNotificationScheduler` arms `AlarmManager` (inexact) and `BootReceiver` restores after reboot. iOS: currently a no-op placeholder; the plan is a `UNUserNotificationCenter`-based `NotificationScheduler`/`NotificationPermissionGateway` in `iosMain` (calendar/time-interval triggers; iOS allows at most 64 pending local notifications, so the planner output must be capped there).

Navigation chrome: the top bar (tab screens only) shows the logo + "NixUra" on the left and the profile avatar on the right. Settings is no longer in the top bar: it is opened by a gear icon (`NixIcon.Gear`, rendered with the official Material `Icons.Filled.Settings`) in the top-right `actions` slot of the Profile screen header (`SubScreen`), and back from Settings returns to Profile. `NixIcon.Sliders` was removed.

Coach is full-screen: `Route.Coach` is still a `Route.Tab`, but `MainContent()` in `AppShell.kt` hides both `TopBar` and `BottomNav` for it (`showChrome = isTab && route != Route.Coach`). `CoachScreen` has its own compact header (back -> `navigator.back()` -> Home, title, mode line, clear chat). The mode line is the isolated `CoachModeLine()` composable (currently the static offline text) so the AI mode label can replace it. The "today's focus" strip at the top opens a list of active quit habits; the pick is kept in `CoachViewModel`, sent with each message (`SendCoachMessageUseCase(focusHabitId)` -> `CoachSnapshot.focusHabitId`) and `CoachEngine` answers about that habit (an explicitly mentioned habit still wins).

Display format for counters: days → hours → minutes → seconds blocks from the largest non-zero unit (`CounterParts`); the older `formatElapsed` string format is still used in a few places.

Concurrency notes: `EnsureCoachGreetingUseCase` and `UpdateSettingsUseCase` use a `Mutex` (the first prevents a duplicate greeting when the view model and screen both trigger it; the second serializes settings read-modify-write). Persistence changes are checked with `tools/room_migration_check.py`.


## Smart habit recognition (Round 2 - Agent 1)

`domain/recognition` turns free text into a `HabitRecognition` (normalized text, `HabitPolarity` BAD/GOOD/UNKNOWN + confidence, matched `HabitCategory?` + confidence, optional spelling correction). Pipeline: `HabitTextNormalizer` (lowercase, accent folding, Arabic normalization, repeated-letter collapse, intent-wrapper and filler stripping) -> `HabitMatcher` (word-boundary token matching: exact, inflection stem, Damerau-Levenshtein with length-based tolerance, dropped-vowel skeleton, weak generic keywords scored down, neutral phrases such as "sweet potato" masked) -> `PolarityClassifier` (catalog match = BAD, bundled EN/AR good-habit lexicon = GOOD, conflicting evidence = UNKNOWN). `HabitKnowledgeBase.match()` delegates to it; `categories` = built-in + `SubstanceCatalog` + `BehaviorCatalog` merged by id (later overrides earlier). The quit and build forms show non-blocking hints; the name is carried between forms by `HabitNameHandoff` (Koin singleton). No Room changes.

## Knowledge catalog A: substances, food and drink (Round 2 - Agent 2)

`domain/coach/catalog/SubstanceCatalog` (`object SubstanceCatalog { val categories: List<HabitCategory> }`, built from helper files `Substance*.kt`) holds 20 habits as pure data using the existing `HabitCategory` / `BodyMilestone` classes. `HabitKnowledgeBase.categories` merges it by id, so a category sharing an id with a built-in one (`nicotine, caffeine, sugar, alcohol, junkfood`) overrides it. New ids: `vaping, shisha, smokeless, energydrinks, tea, softdrinks, sugarindrinks, latenighteating, overeating, salt, friedfood, cannabis, painkillers, sleepingpills, stimulants`. Risky substances carry `safetyNote`s; no dosing or sourcing content. Content is mostly unverified general knowledge (flagged `UNVERIFIED` in KDoc). Branch `agent-2-substance-catalog`, not merged yet.

## Knowledge catalog B: behavioural, screen, mental and body-reflex habits (Round 2 - Agent 3)

`domain/coach/catalog/BehaviorCatalog` (`object BehaviorCatalog { val categories: List<HabitCategory> }`, built from the internal helper objects in `BehaviorScreens.kt`, `BehaviorSleepMind.kt`, `BehaviorBodyReflex.kt`, `BehaviorSocialMoney.kt`) holds 28 habits as pure data using the existing `HabitCategory` / `BodyMilestone` classes. `HabitKnowledgeBase.categories` merges it by id after `SubstanceCatalog`. It re-declares (enriches) the legacy ids `screens` (kept as the broad fallback), `nails` (now nail biting only), `sleep` (label "Staying up late"), `procrastination` and `spending` (now impulse and online shopping). New ids: `socialmedia, doomscrolling, shortvideo, gaming, bingewatching, phonechecking, latenightphone, texting, oversleeping, overthinking, negativeselftalk, comparison, sitting, skinpicking, hairpulling, lipbiting, knucklecracking, teethgrinding, gambling, swearing, gossiping, anger, explicitcontent`. Gambling was split out of the legacy `spending`, so `match("betting")` now returns `gambling`. Timelines start at 20 min to 4 h and are dense in the first 48 h; they are typical ranges, not measurements. Habits where help may be advisable carry a `safetyNote` that points to a doctor, counsellor, dentist or support service and never diagnoses. `explicitcontent` is neutral and clinical (its keywords contain the terms people type; no explicit wording in the text). Branch `agent-3-behavior-catalog`, not merged yet.

## Offline coach upgrade and stop-timeline feature (Round 2 - Agent 4)

`domain/coach/DurationParser` reads durations from free text; `StopTimelineExplainer.explain(habitName, elapsed)` turns any habit's milestones into a `StopTimelineAnswer` (headline, what is happening, already happened, next + time to next, craving tips, safety note, `isGenericProfile`). `CoachEngine` exposes it as `explainStop` and answers the WHAT_HAPPENS intent in chat (habit from the message, else the picked habit, else the only active habit, else it asks which; no duration: next two milestones). Follow-ups use `CoachSnapshot.recentUserTexts`. Intent matching is typo-tolerant via `CoachText`. UI: explorer card on the quit-habit analysis screen and a quick-prompt chip in the coach. Unverified (not compiled), see `DEVELOPMENT_STATUS.md`. Branch `agent-4-coach-timeline`.

## Android home-screen widgets (Round 2 - Agent 5)

`domain/widget/` holds plain-Kotlin widget states (`QuitWidgetState`, `GoodWidgetState` with the 7-day strip and streak, `EventWidgetState`), `NearestEventSelector` (next upcoming event, else most recent past, else none), `WidgetStateMapper` (reuses the in-app calculators), `WidgetRefreshPlanner` (next re-render = earliest of local midnight and the states' change instants) and `WidgetLayoutPlanner` (size -> layout family and which optional rows fit; counter numerals sized so they always fit). All widget text is in the `widget*` members of `presentation/strings/Strings.kt`. `WidgetPinner` is the port behind the "Add to Home screen" button (Android implementation `AndroidWidgetPinner`; no iOS binding, the button hides). `domain/usecase/WidgetUseCases.kt` has `Increment/DecrementGoodHabitTodayUseCase` (local day, clamped 0..`MAX_DAILY_COUNT`, process-wide mutex), `LoadWidgetStateUseCase` and `ObserveWidgetDataChangesUseCase`, registered in the Android `platformModule`. `androidApp/.../widgets/` has the providers, `WidgetRenderer` (RemoteViews styled like `BadHabitCard` / `GoodHabitCard` / `EventCard`), `WidgetUpdater`, receivers (quick log, system, pin), `WidgetConfigActivity` and `WidgetDeepLink`. Per-widget bindings (`appWidgetId -> habit/event id`) and the short-lived pending pin live in SharedPreferences `nixura_widgets`; no Room change, no new permissions. iOS widgets are not implemented (need macOS, WidgetKit, App Group).

## Round 3 (2026-11-03): water fill, widget parity, delete with undo, money saved, calendar imports

- **Water fill:** `domain/recognition/GoodHabitFillStyle` (WATER or PLAIN, from the habit-name matcher) and `GoodHabitStatsCalculator.fillLevel`. `presentation/components/LiquidFill.kt` is a reusable liquid modifier for any measurable habit; `presentation/appearance/GoodFillColors.kt` gives the card colors to both the app and the widget.
- **Widgets:** `androidApp/.../widgets/WidgetCardPainter.kt` paints the quit gradient and the water level as bitmaps, because RemoteViews cannot draw gradients or animate. The quit widget shows the money saved.
- **Delete with undo:** `domain/usecase/DeleteItemUseCase` (a habit and its history, or an event, marked deleted so sync carries it). `presentation/deletion/PendingDeletions` (an app-wide undo window: lists hide the item at once, the delete happens after 5 s) and `DeleteFlow` (sheet, confirmation, Undo bar).
- **Money saved:** `domain/money` holds `HabitCost`, `CountableHabits` (which quit habits can be counted and priced), `MoneySavedCalculator` (follows the streak) and `Currencies`. Stored in `habits.cost_*` and synced in the habit payload.
- **Calendar imports:** `domain/integration` holds the models, `ImportPlanner` (dedupe, edits, cancellations, deletions, user deletes), `SyncIntegrationsUseCase` and `DisconnectIntegrationUseCase`, and the ports `GoogleAuthorizer`, `GoogleCalendarCatalog` and `NotionLink`. `data/integration` holds the Google Calendar v3 + Google Tasks v1 client (`GoogleCalendarSource`, feeds Events / Birthdays / Tasks / Holidays as `GoogleFeed`, the country from the `DeviceRegion` port), the Notion backend client and the stores. Imports get a category per feed (`EventCategory.EVENT`, `BIRTHDAY`, `TASK`, `HOLIDAY`; Notion stays `OTHER`); pickers show Other last (`EventCategory.pickerOrder`). Rows the importer removes itself carry `REMOVED_BY_IMPORT` in `external_updated_at` so they can come back; the user's own deletes are remembered. Backend: `backend/src/integrations/` (Notion OAuth and queries, AES-256-GCM token storage). Imported events need an account; link settings are per device and cleared on sign-out. Setup steps: README, "Linking calendars".
