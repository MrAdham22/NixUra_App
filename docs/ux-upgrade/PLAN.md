# NixUra UX upgrade: log in the moment

**Written:** 2026-11-21 (planning session, no app code changed). **Branch:** `docs/ux-upgrade-plan`.

**Goal.** Help people log a habit when it happens, not rebuild their day in the app at night. Today they log
everything at bedtime and miss the live card animations (water rising per glass, the gold arch per prayer).
Reminders exist to support the habit, not to raise engagement: no guilt, no dark patterns, no spam.

**Phases** (each runs later in its own session; this file is their shared brief):

| Phase | What | Depends on |
|---|---|---|
| A1 | Widget "+" tap latency (feedback within one frame-ish, every tap counts) | none |
| A2 | Launcher App Shortcuts (4) + the 3 new screens they open | none (shares manifest/strings/nav with B) |
| B | Notification foundation: channels, grouping, look per type, permission flow | none |
| C | Good-habit reminders + notification actions ("+1 glass") | B (and the prayer-times answer, see Q1) |
| D | Lock-screen / persistent presence: research only | B, C findings |

---

## Status

| Phase | Status | Branch / PR | Phase doc | Device-tested on Oppo |
|---|---|---|---|---|
| Plan | PR open, waiting for owner review | `docs/ux-upgrade-plan` | this file | n/a |
| A1 | Not started | | `docs/ux-upgrade/A1.md` | no |
| A2 | Not started (blocked on Q4, Q5) | | `docs/ux-upgrade/A2.md` | no |
| B | Not started (blocked on Q3) | | `docs/ux-upgrade/B.md` | no |
| C | Not started (blocked on Q1, Q2, B) | | `docs/ux-upgrade/C.md` | no |
| D | Not started | | `docs/ux-upgrade/D.md` | n/a (research) |

Update this table at the end of every phase.

---

## Open questions for the owner (answer before the phase that needs it)

1. **Prayer times are not calculated anywhere in the app today** (blocks C for prayer). The brief says "prayer
   reminders use the prayer times the app already calculates", but the code has no prayer-time calculation, no
   location access and no calculation method. Prayer is detected by name only (`domain/recognition/PrayerHabitDetector.kt`)
   and the card shows five stars named Fajr…Isha (`Strings.prayerNames`), filled in order by today's count.
   Options:
   - **(a) Recommended:** compute times in pure Kotlin in `commonMain` (standard astronomical formulas, ~200 lines,
     fully unit-testable, no new dependency), from a **city the user picks** (stored per device, no GPS permission)
     and a calculation method (Egyptian General Authority by default for Arabic, Muslim World League otherwise,
     changeable). Offline, works on iOS too.
   - (b) Same, but take the location from the phone (needs `ACCESS_COARSE_LOCATION`, a Play Data-safety update and
     a permission prompt; more accurate when travelling).
   - (c) The user types five times once per habit (no astronomy; times drift through the year, so it is weaker).
   - (d) Add the `adhan` Kotlin library (new dependency; needs the written reason the ground rules ask for).
2. **Where reminder settings live** (C). They are per habit and alarms are per device. Recommended: a new Room
   table `habit_reminders` (Room v9, additive migration, no change to existing rows) that is **synced** with the
   habit, so a new phone keeps the choices; a per-device "pause all reminders" stays unsynced. Alternative: per
   device only (simpler, lost on a new phone). Which do you want?
3. **Live app vs docs** (B, C). The brief says NixUra is live on Google Play; `AGENTS.md` and the README still say
   "the app is not released yet" (and `versionCode = 1`). Which track is it on (internal / closed / production), and
   how many real users? It decides how careful the channel and Room migrations must be. Plan assumes **live**.
4. **Bug reports and guests** (A2). `AGENTS.md` says guests make no network calls, and `firestore.rules` only allows
   `users/{uid}/sync/{doc}` for a signed-in user. Should a guest's bug report (a) go by email only, with Firestore
   only for signed-in users (recommended, keeps the guest promise), or (b) go to Firestore for everyone (needs
   Firebase anonymous auth or an unauthenticated write rule, both new)?
5. **"Don't delete NixUra!" screen** (A2). What should it offer? Recommended: a calm screen that says what is lost
   (streaks, history; nothing for a guest without backup), offers "Export / back up first" (only if it exists; it does
   not today), "Tell me what's wrong" (opens the bug screen) and "Uninstall anyway" (opens the system app page).
   No guilt wording. Confirm, or describe what you want.
6. **Your phone**: `adb` saw no device in this session. `docs/DEVELOPMENT_STATUS.md` records the owner's phone as
   OPPO CPH2531 on **Android 15** (ColorOS 15). Still correct?

---

## What I found (with file paths)

All shared paths are under `shared/src/commonMain/kotlin/com/adhamelgendy/nixura/`; Android paths under
`androidApp/src/main/kotlin/com/adhamelgendy/nixura/`.

### Build, SDK, tests
- KMP, one shared module `:shared` (domain / data / presentation / di) + `:androidApp` + `iosApp/`.
  `gradle/libs.versions.toml`: **minSdk 24, targetSdk 37, compileSdk 37**; WorkManager 2.10.1 is already a dependency
  (`AndroidBackgroundSync`). `androidApp/build.gradle.kts`: `versionCode = 1`, `versionName = "1.0"`.
- Commands: `gradlew.bat :androidApp:assembleDebug`, `gradlew.bat :shared:testAndroidHostTest`,
  `gradlew.bat :androidApp:installDebug`; Room check `python3 tools/room_migration_check.py`. iOS only on macOS.
- DI: Koin, `di/AppModules.kt` (common) + `PlatformModule.android.kt` / `PlatformModule.ios.kt`, plus late
  `loadKoinModules` in `NixUraApplication.kt`.
- Navigation: own back stack, `presentation/navigation/Navigator.kt` (`sealed interface Route`, `Coach` already
  exists, nothing for help / bug report / "don't delete"). Deep links from widgets: `widgets/WidgetDeepLink.kt`,
  applied in `MainActivity.onCreate`.

### Strings and languages
- All UI text: `presentation/strings/Strings.kt` (English, `open val` in the body) + `ArabicStrings.kt` (override),
  checked by `ArabicCoverageTest`; plurals via `domain/locale/ArabicPlurals`. Android `res/values{,-ar}/strings.xml`
  hold only what Android itself reads (launcher label, widget-picker labels, **notification channel name**).
  App Shortcut labels are read by the launcher, so they also go in `res/values{,-ar}/strings.xml`.
- Notification bodies are built in the domain in the saved language (`NotificationPlanner`, `ContentLanguage`).

### Models and persistence
- `domain/model/GoodHabit.kt`: `dailyTarget`, `weeklyTarget`, `theme: HabitTheme` (WATER, PRAYER, WALK, WORKOUT,
  READING, SLEEP, DEFAULT; `domain/theme/HabitTheme.kt`), `water: WaterGoal?`. **No reminder fields.**
- Logs: `GoodHabitLog(goodHabitId, dayIndex, count)`, one row per day, count only (no time of each tap).
- Room **version 8** (`data/local/NixUraDatabase.kt`, `MIGRATION_1_2`…`MIGRATION_7_8`). Firestore sync of
  habits through `data/sync/FirestoreSyncMapping.kt`; rules `firestore.rules` (`users/{uid}/sync/{docId}` only).
- `domain/model/UserModels.kt` `AppSettings`: `notificationsEnabled` (global switch, default true),
  `dailyCheckInMinutes` (08:30).

### Notifications today (event reminders, streak milestones, daily check-in)
- Domain: `domain/notification/NotificationPlanner.kt` (pure; plans event reminders, the next two streak milestones
  per quit habit, and a daily check-in for 14 days), `NotificationSyncService.kt` (re-plans on every data change
  **while the app UI runs**; `syncOnce()` is only called by the background `SyncWorker`), `NotificationModels.kt`
  (`PlannedNotification(key, trigger, title, body)`: no type, no channel, no actions), `ReminderCapabilities.kt`,
  `ReminderRules.kt` (event lead-time validation).
- Android: `notifications/AndroidNotificationScheduler.kt` (AlarmManager; plan stored in SharedPreferences
  `nixura_notifications`; max 60 alarms; exact alarms only for `event:` keys and only if allowed),
  `ReminderReceiver.kt` (**one channel `nixura_reminders`**, IMPORTANCE_DEFAULT, name "Reminders & coaching";
  one color `#AE445A`, one small icon, no group, no actions, every notification opens Home),
  `BootReceiver.kt` (re-arms on boot, app update, exact-alarm permission change; **not on time-zone or clock change**),
  `AndroidNotificationPermission.kt` (POST_NOTIFICATIONS on 33+, exact-alarm check and settings shortcuts).
- Manifest declares `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`.
- Permission is asked when the first event reminder is added (`EventFormScreen.kt`) or notifications are turned on
  in Settings (`SettingsScreen.kt`). Never on first launch (good).
- iOS: `PlatformModule.ios.kt` binds `NoOpNotificationScheduler` (no iOS notifications at all yet).

### Widgets (RemoteViews, not Glance)
- `widgets/WidgetProviders.kt`, `WidgetRenderer.kt` (582 lines; draws card backgrounds as **bitmaps**:
  `WidgetCardPainter.gradient / liquid / prayerCard`, prayer stars and badge via Compose drawing code),
  `WidgetUpdater.kt`, `WidgetReceivers.kt`, `WidgetIntents.kt`; shared logic in `domain/widget/*` and
  `domain/usecase/WidgetUseCases.kt`.
- **Tap path** ("+"): `WidgetActionReceiver.onReceive` → `goAsync()` → `IncrementGoodHabitTodayUseCase` →
  `adjustTodayCount` (takes a lock, then `repository.observeGoodHabits().first()`, i.e. **every active habit with its
  full log history**, to read one count) → `setCount` → `WidgetUpdater.refresh(app, widgetId)` which, under one
  `Mutex`, **re-renders every widget of every kind** (touched one first) and re-arms the tick alarm. In parallel,
  `WidgetUpdater.startObserving` sees the DB change and does **another full refresh** 500 ms later.
- Count is clamped to `maxCountPerDay` (prayer = 5): a tap past the cap is ignored by design.

### Likely causes of the 2–4 s delay (to confirm with timing logs in A1, not assumed)
1. **Cold process start**: when the app is not running, the tap first runs `NixUraApplication.onCreate`
   (Facebook SDK init, Koin, Firebase config, widget observers) and opens Room. ColorOS kills background processes
   aggressively, so this is the common case.
2. **Heavy read per tap**: the full habit + log history is loaded to change one number.
3. **Full re-render per tap**: all widgets, with bitmap painting, serialized behind one lock; rapid taps queue N
   full refreshes, plus the observer's extra refresh, so taps 2..N show up late.
4. No visual "received" state between the tap and the new count.

### The habit-card bottom sheet
- Long-press on a good-habit card opens the delete flow's sheet with `SheetAction`s: Edit, Add to Home screen
  (`presentation/screen/GoodHabitsScreen.kt:50`, same list in `HomeScreen.kt:117`), built on
  `presentation/components/NixSheet.kt`. This is where reminder controls go (owner decision).

---

## Recommendations

### A1: widget tap latency (every tap counts)
- **Measure first** on the Oppo: log timestamps at `onReceive`, after the write, after `updateAppWidget`; cold vs warm
  process. Put numbers in `A1.md` before and after.
- **Optimistic, targeted render**: right after the write, render **only the touched widget** with the new count
  (`partiallyUpdateAppWidget` for the count text / progress where the layout allows; full render for the painted
  background), then refresh the other widgets after a short coalescing delay.
- **Coalesce**: replace "one full refresh per tap" with a conflated refresh (latest state wins), so 5 fast taps write
  5 times but redraw at most twice. Taps are never dropped: every tap is one write (the lock already serializes them).
- **Cheap write**: add a repository/DAO method that reads and updates **one habit's count for one day**
  (`getCount(habitId, day)` + `setCount`) instead of loading every habit's full history. Keep it inside the
  existing use case (no duplicated business logic).
- **Light cold start**: keep `NixUraApplication.onCreate` minimal for receiver-only starts (lazy Facebook init;
  nothing network-bound before the write).
- Optional instant feedback: a pressed/"+1" state on the button via a drawable selector (RemoteViews supports
  state lists), so the finger gets an answer even before the process is up.
- Tests: the new single-count path and the coalescer in `commonTest` (pure parts); device steps in `A1.md`.

### A2: launcher App Shortcuts (exactly 4)
- **Static shortcuts** in `res/xml/shortcuts.xml` (labels in `values` + `values-ar`, short ≤10 chars and long ≤25):
  🥺 Don't delete NixUra! · 🐛 Bugs or issues? · 💡 Need help? · 🧠 Talk to your AI Coach.
  Emoji go in the label text, the icon is a monochrome adaptive shortcut icon in the brand palette
  (launchers drop emoji in some fonts; test on ColorOS). Arabic: «🥺 لا تحذف NixUra!» · «🐛 مشكلة أو خلل؟» ·
  «💡 تحتاج مساعدة؟» · «🧠 تحدّث مع مدربك الذكي».
- Each shortcut is an explicit intent to `MainActivity` with an extra (`nixura.shortcut=…`); a small
  `ShortcutDeepLink` (same pattern as `WidgetDeepLink`) pushes the route. Handle `onNewIntent` too (activity is
  `singleTask`).
- New routes: `Route.KeepNixUra`, `Route.ReportBug`, `Route.Help`; Coach reuses `Route.Coach`.
- **Bug report**: a form (what happened, optional steps, optional screenshot later) → **both** an email intent to
  adhamelgendydev@gmail.com (subject + body prefilled with app version, device model, Android version, language;
  no account id) **and** a Firestore write (see Q4 for guests). Firestore needs a new collection (e.g.
  `bugReports/{autoId}`, create-only, size-limited fields) in `firestore.rules`, tested with
  `tools/firestore-rules-test`, deployed only with the owner's OK. Device info comes through an `expect/actual`
  `DeviceInfo` (iOS side too).
- **Help**: an offline FAQ (logging, widgets, reminders, sync, language, deleting data) with a "Still stuck?" link
  to the bug screen. Text in `Strings` + `ArabicStrings`.
- Report a shortcut use with `ShortcutManagerCompat.reportShortcutUsed` (helps launcher ranking). iOS: Home-screen
  quick actions (`UIApplicationShortcutItem`) for parity, unverified on Windows.

### B: notification foundation
**Channels (small, user-meaningful; IDs are permanent):**

| Channel ID | Name EN / AR | Importance | Used for |
|---|---|---|---|
| `nixura_reminders` (**existing, kept**) | Events / المناسبات (renamed from "Reminders & coaching") | DEFAULT | event reminders |
| `nixura_good_habits` (new) | Good-habit reminders / تذكيرات العادات الجيدة | DEFAULT | water, prayer, other counters (C) |
| `nixura_quit_progress` (new) | Quit progress / تقدّم الإقلاع | LOW (no sound) | streak milestones |
| `nixura_coach` (new) | Coach / المدرب | LOW | daily check-in, coach notes |
| `nixura_app` (new) | App & account / التطبيق والحساب | LOW | sync problems, account, updates |

- Why keep `nixura_reminders` for events: existing users may have customised it (sound, off); it already carries
  their event reminders. Renaming a channel is allowed, changing its importance is not (only the user can).
  Milestones and the check-in move to the new LOW channels. Prayer gets no separate channel in B; if Q1 leads to
  adhan-like sounds later, that would be its own channel then.
- **Groups**: a channel group per area (`Habits`, `Events`, `Coach & app`) for the settings screen, and a
  notification **group key per type** so several water reminders never stack (water reminders replace each other:
  one notification id per habit, not per reminder).
- **Model change**: `PlannedNotification` gets a `kind` (`EVENT`, `GOOD_HABIT`, `QUIT_MILESTONE`, `COACH`,
  `APP`), optional `habitId`/`eventId` and an optional deep-link target, set by the planner (pure, testable). The
  Android receiver maps kind → channel, color, icon, group, tap target. Old stored plans (no kind) decode as today,
  inferred from the key prefix (`event:`, `milestone:`, `daily:`), so an app update loses nothing.
- **Look per type**: small icon per type (monochrome vectors: drop for water, the gold arch/star for prayer, a
  plant/leaf for other good habits, a broken chain for quit, calendar for events, the brand mark for coach and
  app), accent color per type (water: the water theme's blue from `HabitThemes.visualFor`; prayer: the gold from
  `PrayerPalette`; quit `#AE445A`; events `#862549`; coach and app `#F39F5A`). Tapping opens the right
  screen (habit detail / event / coach), not Home.
- **Permission**: never on first launch. Ask in context (turning on a reminder, the first habit that gets suggested
  reminders) with a one-line reason card before the system dialog; if denied twice, show a card with "Open settings"
  (existing `ReminderCapabilities.openNotificationSettings`). Also show per-channel state: if the user turned a
  channel off, the habit sheet says so and links to that channel's settings.
- **Re-arm on time changes**: `BootReceiver` also handles `TIMEZONE_CHANGED` and `TIME_SET` (widgets already do).
- iOS: implement `NotificationScheduler`/permission with `UNUserNotificationCenter`, categories per kind (thread
  identifier = group). Unverified until run on a Mac.

### C: good-habit reminders + actions

**Strategy per habit type**

| Habit | Default | Timing | Cap/day | Skip rule |
|---|---|---|---|---|
| Prayer (theme PRAYER, daily 5) | ON (suggested at create, see Q1) | ~10 min after each prayer's start time (adhan time) for the **next unlogged** prayer, never before it starts | 5 | skip if that prayer (count ≥ index) is already logged; one notification that updates, never stacks |
| Water (theme WATER) | ON (suggested) | every 2 h inside a waking window (default 09:00–21:00), only while behind pace (`expected = goal × elapsed share of the window`) | 6 | skip if on or ahead of pace or the goal is met; stop for the day when met |
| Other daily counters (walk, reading, workout…) | OFF, offered | one time the user picks (default: the habit's usual logging hour if we learn it later; for now 19:00) | 1 (2 if target > 1, second at +3 h) | skip if today's target is met |
| Weekly habits | OFF, offered | one time on the remaining days, only when the week is still reachable | 1 | skip if today already done or the week's goal is met |

- **Quiet hours**: global default 22:00–08:00 (Fajr is exempt only for prayer habits when the user keeps the
  reminder on). Respect Android Do Not Disturb (we never use alarm-style categories).
- **No spam guard**: across all good habits at most 8 reminders a day and none within 30 min of another; a missed
  reminder is never re-sent later as "you missed it".
- **Auto-quiet**: if a habit's reminders are dismissed 3 days in a row without logging, the sheet next time offers
  "Fewer reminders?" (never turns them off silently, never shames).
- **Planning stays in the domain**: a new pure `GoodHabitReminderPlanner` (or a section of `NotificationPlanner`)
  computes the next fire times from habit, logs, settings and prayer times. Because it depends on logs, the plan is
  re-built on every log, **including logs made from a widget or a notification action while the UI is closed**: the
  widget receiver and the action receiver call `NotificationSyncService.syncOnce()` after writing (today only the
  sync worker does). Horizon: today + tomorrow (water/prayer change daily), so the 60-alarm cap is never hit.
- **Inexact alarms** for good-habit reminders (`setAndAllowWhileIdle`, a few minutes late is fine). Prayer may use
  exact only if the user already granted exact alarms for events. No new exact-alarm usage that Play would
  question.

**Bottom-sheet controls** (long-press sheet, existing `SheetAction` list): one new row between Edit and Add to Home
screen: "🔔 Reminders: On · every 2 h" (or "Off"). Tapping opens a small sheet: on/off switch, the schedule
(water window, prayer offset, custom time), "Pause for today", "Pause for 1 week". Paused state shows on the row
("Paused until Sat"). Global "Pause all reminders" stays in Settings next to the existing notifications switch.
At create time, prayer and water habits show one line in the form: "Reminders on — change anytime" with a switch.

**Actions (which habits, which actions)**

| Habit | Actions | Behaviour |
|---|---|---|
| Water | **+1 glass** (label uses the serving size, "+1 glass · 250 ml"), Snooze 1 h | write through `IncrementGoodHabitTodayUseCase`; notification updates in place to "5 of 8 glasses" then dismisses after 4 s; widgets refresh |
| Prayer | **Prayed ✓** (marks the named prayer), Snooze 15 min | marks that prayer only; if done for the day, cancels the rest |
| Other daily counters | **Done ✓** (+1), Snooze 1 h | same increment path |
| Events, quit milestones, coach | none (tap opens the screen) | a quit habit never gets a "relapse" action from a notification |

Actions run in a non-exported `BroadcastReceiver` with `goAsync()` (same pattern as the widget receiver), reuse the
existing use cases, and call `WidgetUpdater.requestRefresh` + `syncOnce()`. No "Open app to see your streak!" lures.

**Example wording** (tone: warm, short, specific, never guilt; no "don't break your streak", no "you missed")

| Type | English | Arabic |
|---|---|---|
| Water, behind pace | 💧 **Time for a glass** · 3 of 8 so far today. Tap +1 when you drink it. | 💧 **وقت كوب ماء** · ٣ من ٨ حتى الآن اليوم. اضغط +1 عندما تشربه. |
| Water, last of the day | 💧 **One more glass** and today's water is done. | 💧 **كوب واحد بعد** ويكتمل ماء اليوم. |
| Prayer, Fajr | 🌙 **Fajr has started** · Mark it here once you've prayed. | 🌙 **دخل وقت الفجر** · سجّلها هنا بعد أن تصلّي. |
| Prayer, Maghrib | 🌅 **Maghrib time** · 3 prayers marked today. | 🌅 **حان وقت المغرب** · سجّلت ٣ صلوات اليوم. |
| Reading (custom time) | 📖 **Your reading time** · 10 pages tonight? | 📖 **وقت القراءة** · ١٠ صفحات الليلة؟ |
| Walk | 🚶 **A short walk?** · Your goal today: 1 walk. | 🚶 **مشية قصيرة؟** · هدفك اليوم: مشية واحدة. |
| Event (existing, kept) | **Mum's birthday** · Starts in 2 hours. | **عيد ميلاد أمي** · تبدأ بعد ساعتين. |
| Quit milestone | ⛓️ **7 days free of smoking** · A full week. That counts. | ⛓️ **٧ أيام بلا تدخين** · أسبوع كامل، وهذا إنجاز. |
| Coach check-in | ✨ **Morning, Sara** · 2 of 4 habits on track this week. | ✨ **صباح الخير يا سارة** · عادتان من ٤ على المسار هذا الأسبوع. |
| App / sync | **Sync paused** · We'll try again when you're online. | **المزامنة متوقفة** · سنحاول مجدداً عند الاتصال. |

Arabic counts go through `ArabicPlurals`; digits follow the user's digit setting. Final copy is reviewed by the
owner in the C session.

### How widgets and notifications complement each other
- **Widget = always there, user-initiated**: glanceable progress and logging when the user decides to look.
- **Notification = the right moment, app-initiated, rare**: one nudge when the behaviour is due (prayer time, behind
  on water), with one action. It never duplicates the widget's dashboard (no "today's summary" notifications).
- Shared state: both write through the same use cases; each write refreshes widgets and re-plans reminders, so a
  glass logged on the widget cancels the pending water reminder, and "+1" from a notification moves the widget.
- If a habit has a widget on the home screen, its reminder default stays the same (the widget does not prove the
  user looks at it), but the reminders sheet can say "You also have this on your Home screen".

### D: lock screen (research only)
Questions to answer in `D.md`, no code: Android lock-screen widgets (Android 16+ on some devices, not ColorOS 15),
ongoing notification with progress (water) vs Play policy for ongoing notifications, `setPublicVersion` / visibility
for private habits (quit habits must be hidden on the lock screen by default), Live Updates / progress-style
notifications on Android 16, ColorOS "Fluid Cloud" support for third-party apps, iOS Live Activities (needs
ActivityKit + Swift target). Recommendation and cost for each.

---

## Risks

| Risk | Detail | Mitigation |
|---|---|---|
| Exact alarms / Play policy | `SCHEDULE_EXACT_ALARM` is declared. Play restricts `USE_EXACT_ALARM` to alarm/calendar apps; `SCHEDULE_EXACT_ALARM` is user-granted and denied by default on Android 14+ for new installs. | Good-habit reminders use inexact alarms. Keep exact only for events when the user allowed it. Never add `USE_EXACT_ALARM`. Check the Play declaration form before release. |
| Android 13+ notification permission | Denied = nothing shows; asking at launch hurts trust. | Ask in context with a reason card; show a fix card when denied; check per-channel enabled state. |
| Reboot / update | Alarms are cleared. | Already re-armed by `BootReceiver`; C extends the stored plan with kind and habit id (backward-compatible decode). |
| Time zone / clock / DST | `BootReceiver` does not listen to `TIMEZONE_CHANGED`/`TIME_SET`; prayer times and water windows are local-time. | Re-plan on those broadcasts; plan with local dates (`DayCalculator`), never "+24 h"; tests across DST days (Egypt observes DST since 2023). |
| ColorOS background killing | Oppo kills background processes and may block auto-start; receivers still run but cold; alarms may be delayed in deep sleep. | Inexact alarms are fine for nudges; never rely on a long-running service; make the cold path fast (A1); Help screen explains "Allow auto-launch / no battery restriction" for NixUra with ColorOS steps. |
| Existing users' channel | `nixura_reminders` exists on every install with the user's settings. | Keep the ID; only rename it. New channels start fresh. Never delete it. Test the update path: install the current Play build, set the channel to silent, update, check it stays silent. |
| Existing data | Reminder settings need storage. | Additive Room v9 migration (new table only), migration test + `room_migration_check.py`; Firestore payload stays backward-compatible (old apps ignore unknown fields). |
| Notification actions after process death | Action receiver starts cold like the widget. | Same `goAsync` pattern, same fast path as A1, idempotent per tap. |
| Spam / trust | Too many nudges = users block the whole app. | Daily cap, quiet hours, skip-when-done, pause, auto-quiet offer, LOW importance for non-time-critical types. |
| iOS parity | No iOS notifications exist; nothing can be verified on Windows. | Implement both sides per phase, mark iOS unverified, list Mac commands in each phase doc. |

---

## Dependency map

```
A1 (widgets) ─────────────┐
                          ├──> C (reminders + actions) ──> D (research)
B (notification base) ────┘
A2 (shortcuts + screens)  independent, but shares files with B and C
Q1 (prayer times) ──> C (prayer part only; water/others can ship first)
```

- A1, A2 and B can run in parallel **only** if they keep to their own files and append-only markers in shared files.
- C needs B merged (channels, kinds, receiver) and benefits from A1 (fast cold path reused by the action receiver).

**Files more than one phase touches** (append-only, own `// ---- Phase X ----` markers):

| File | A1 | A2 | B | C |
|---|---|---|---|---|
| `androidApp/src/main/AndroidManifest.xml` | | shortcuts meta-data | receivers / intent filters | action receiver |
| `androidApp/src/main/res/values{,-ar}/strings.xml` | | shortcut labels | channel names | |
| `presentation/strings/Strings.kt` + `ArabicStrings.kt` | | screen text | permission cards | reminder sheet + notification text |
| `di/AppModules.kt`, `PlatformModule.{android,ios}.kt` | maybe | `DeviceInfo`, bug-report repo | scheduler changes | reminder repo/planner |
| `NixUraApplication.kt` | cold-start trim | | channel creation | |
| `MainActivity.kt` | | shortcut intent | notification tap routing | |
| `presentation/navigation/Navigator.kt` | | 3 routes | | |
| `domain/notification/*` | | | `kind` in model | reminder planner |
| `notifications/*` (Android) | | | channels, receiver | actions |
| `widgets/WidgetReceivers.kt`, `WidgetUpdater.kt` | yes | | | call `syncOnce()` after a write |
| `domain/usecase/WidgetUseCases.kt` (+ DAO) | single-count path | | | reused by actions |
| `data/local/*` (Room) | DAO query only | | | Room v9 |
| `firestore.rules` | | bug reports | | reminder sync (if Q2 = synced) |
| `GoodHabitsScreen.kt`, `HomeScreen.kt` (sheet actions) | | | | reminders row |
| Docs (`README.md`, `docs/*.md`, `AGENTS.md`) | each phase updates them per the AGENTS.md table |

Merge order if run in parallel: A1 → B → A2 → C, each rebased on master before merge.

---

## GROUND RULES

- Read README, CLAUDE.md and docs/ux-upgrade/PLAN.md before touching code.
- Treat my examples as direction, not spec. Inspect first, then use senior judgment.
- Clean Architecture, SOLID, existing KMP boundaries: shared logic in commonMain, Android APIs in the Android layer. Reuse existing models and use cases. No duplicated business logic.
- No new dependencies without a written reason. Don't rewrite working systems. Don't redesign unrelated screens.
- The app is live: never break existing users' data, settings or notification channels. Channel IDs are permanent once created.
- All user-facing strings go in resources in EN and AR. Check RTL layouts.
- Add tests for important logic. Run the build and tests before claiming anything works.
- For behavior only a device can show, give me numbered test steps for my Oppo Reno 10 5G.
- Git: branch from the latest main, open a PR with `gh` that covers what changed, why, limitations and verification steps. Before merging, rebase on main and re-run build + tests. Squash-merge ONLY after checks are green AND I confirm the device test. Then delete the branch.
- At the end, write `docs/ux-upgrade/<phase>.md` and update the status table in PLAN.md, so the next session needs nothing from this conversation.
- If a product decision isn't covered here or in PLAN.md, ask me. Don't guess.

**Repo notes that sit next to these rules** (from `AGENTS.md`, no `CLAUDE.md` exists; `AGENTS.md` plays that role):
"main" is `master`. "Resources" for UI text means `Strings.kt` + `ArabicStrings.kt` (shared with iOS); only text
Android itself reads (channel names, shortcut labels) goes in `res/values{,-ar}/strings.xml`. Draw text with
`presentation/components/Text`. iOS changes ship alongside Android and are marked unverified on Windows. No AI or
assistant names in commits, PRs, code or docs.
