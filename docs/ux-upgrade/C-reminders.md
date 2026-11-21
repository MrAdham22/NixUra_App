# UX phase C: good-habit reminders and notification buttons

**Date:** 2026-11-21. **Branch:** `feature/habit-reminders` (from `master` after phase B, PR #19).
**Brief:** `docs/ux-upgrade/PLAN.md` (section C) and `docs/ux-upgrade/B-notifications.md` (the API this phase uses).

**Goal.** Help people log a good habit at the moment it happens. A reminder comes at the right time for that habit
(prayer time, behind on water, the time you picked), carries one button that logs it without opening the app, and
never nags: it is skipped once the habit is done, capped per day, silent at night, and easy to pause or turn off.

## Owner decisions used in this phase

1. **Prayer times come from the phone's location** (PLAN.md decision 1, confirmed again for this phase on 2026-11-21).
   The phase brief said "the prayer times the app already calculates", but the app had no prayer-time calculation.
   The owner chose to build it here: city-level location, read once, kept on the phone, times computed offline.
2. **Suggested on for time-based habits** (prayer, water), off but offered for the rest. Controls live in the habit
   card's long-press sheet.
3. **The app is not released** (PLAN.md decision 3): the design does not need to stay compatible with earlier builds.

## What was built

### Planning (shared, pure, tested)
`shared/.../domain/reminder/`:

| File | What |
|---|---|
| `HabitReminder.kt` | `HabitReminderSettings` (stored on the habit), `ReminderStrategy`, `HabitReminderRules` (defaults, quiet hours, caps, water pace) |
| `HabitReminderPlanner.kt` | turns habits + today's logs + this phone's state into `PlannedNotification`s for today and tomorrow |
| `PrayerTimes.kt` | `PrayerTimeCalculator` (offline, Egyptian General Authority method), `GeoPoint`, `Prayer` |
| `HabitReminderCopy.kt` | the wording interface; implemented by `Strings` / `ArabicStrings` |
| `ReminderActionHandler.kt` | runs a notification button through the widget's use case, once per press |
| `ReminderDeviceState.kt` | this phone's location, pauses, snoozes; `ReminderDeviceRepository` |
| `DeviceLocation.kt` | `DeviceLocationGateway` port (Android: `AndroidDeviceLocation`; iOS: `UnavailableDeviceLocation`) |

`NotificationSyncService` adds the reminder plan to events, milestones and the check-in, so everything still goes
through phase B's one scheduler, receiver and `NixNotifications` layer. No notification is built anywhere else.

### Strategy per habit (PLAN.md, as built)

| Habit | Default | When | Cap/day | Skip |
|---|---|---|---|---|
| Prayer (theme PRAYER, 5 a day) | on | 10 min after each prayer starts, for every prayer not marked yet | 5 | that prayer is marked (today's count ≥ its number) |
| Water (theme WATER, daily) | on | every 2 h inside the window (default 09:00-21:00): 11, 13, 15, 17, 19, 21 | 6 | on or ahead of pace (`goal × elapsed share of the window`, rounded down), or the goal is met |
| Other daily habits | off | the chosen time (default 19:00); a second one 3 h later when the target is above 1 | 2 | today's target is met |
| Weekly habits | off | the chosen time on days the week's goal is still reachable | 1 | done today, or the week's goal is met or out of reach |

Across all good habits, per local day: **at most 8**, **never two within 30 min** (prayer and snoozes win, then a
chosen time, then water), **quiet hours 22:00-08:00** (Fajr is exempt, and so is a snooze the user asked for). A
"Tahajjud" prayer habit with 1 a day is a daily habit with a chosen time, not prayer times.

Each habit has **one notification on screen** (`postKey = good:<habitId>`): a new reminder replaces the one showing.
Each moment has its own alarm key (`good:<habitId>:<day>:<slot>`), so phase B's delivery records keep working.

The plan is re-built on every change: in the app (as before), and now also after a widget "+" and after a
notification button (`syncOnce()`), after each delivered good-habit reminder, by a twice-daily WorkManager job,
after an app update, a time-zone change and a clock change. A glass logged anywhere cancels or re-words the
pending water reminder.

### Scheduling: AlarmManager or WorkManager, and why

| What | Mechanism | Why | Precision |
|---|---|---|---|
| Prayer reminders | AlarmManager through phase B's scheduler (`NotificationTiming.ON_TIME`) | tied to a moment, like an event | with "Alarms & reminders" allowed: phase B's hybrid timing (exact; an alarm clock within 75 min, measured on time on the Oppo). Without it: inexact, up to ~1 h late on ColorOS |
| Water, daily and weekly reminders | AlarmManager `setWindow`, 10-minute window (`NotificationTiming.FLEXIBLE`) | a nudge; a few minutes either way is fine | inside 10 min of the planned time while the phone is awake or in light sleep. In deep Doze (phone still and screen off for a long time) Android holds it until the next maintenance window or the screen turns on. Delivered more than 45 min late = dropped, never shown as "you missed it" |
| Daily re-plan (safety net) | WorkManager periodic, 12 h (`ReminderRefreshWorker`) | the time does not matter; survives reboots, the OS batches it | anywhere in the day |
| Re-plan after a log / a button / a delivery | `syncOnce()` in the receiver (`goAsync`) | must happen right away, the app may be closed | immediate |

- **Why not WorkManager for the reminders themselves:** WorkManager has no "at 13:10" (one-off work only has a
  minimum delay, periodic work is at least 15 min apart with a flex window) and ColorOS defers it further. Alarms are
  the tool for "at a time of day".
- **Exact alarms and Google Play:** no new permission. `SCHEDULE_EXACT_ALARM` was already declared for events (it is
  user-granted; denied by default on Android 14+ for new installs). Prayer uses it only when the user allowed it.
  Water and other habits never need it: `setWindow` is an inexact API. **`USE_EXACT_ALARM` is not used** (Play
  restricts it to alarm-clock and calendar apps). Flexible reminders do not use alarm clocks, so the status-bar
  alarm icon never shows for them.
- **Measured on the Oppo (2026-11-21, after install):** `dumpsys alarm` lists the water reminders with
  `window=+10m0s0ms` (ColorOS kept the 10-minute window; it widens ordinary inexact alarms to 1 h).
- **Reboot / app update / time / time zone / DST:** reboot restores the stored plan (phase B); an app update, a
  time-zone change and a manual clock change **re-plan** (`BootReceiver`). DST needs no broadcast: the planner turns
  local wall-clock times into instants for each day with that day's offset (`DayCalculator.epochMillisOfLocal`,
  never "+24 h"), tested on Egypt's DST day.
- **Process death:** nothing runs in the background between reminders. Alarms live in the OS; the plan is stored
  (phase B); the receivers start the process cold, do their work in `goAsync` and finish.

### ColorOS (Oppo) and other aggressive phones: what the user may need to allow
1. **Notifications** for NixUra (asked in context; the sheet links to system settings when blocked or when the
   "Good-habit reminders" type is off).
2. **Alarms & reminders** (Settings → Apps → NixUra) for on-time prayer reminders. Not needed for water and others.
3. **Battery: "Allow background activity" / no restriction**, and **Auto-launch** on (Settings → Battery → App battery
   management → NixUra on ColorOS 15). Without it ColorOS may delay alarms and can block the re-plan job. Reminders
   themselves still fire (alarms are kept), at worst late.
4. **Location** (city level) only for prayer habits, once.

### Buttons on the notification (no app opening)

| Habit | Buttons | What it runs |
|---|---|---|
| Water | **+1 glass · 250 ml** (uses the habit's cup size), **Snooze 1 h** | `IncrementGoodHabitTodayUseCase`, the widget's "+" |
| Prayer | **Prayed ✓**, **Snooze 15 min** | `MarkGoodHabitTodayAtLeastUseCase(N)`: same lock and write path as the widget; count ≥ N, the prayer card's own rule |
| Other daily / weekly | **Done ✓**, **Snooze 1 h** | `IncrementGoodHabitTodayUseCase` |

- `ReminderActionReceiver` (non-exported, `goAsync`) → `ReminderActionHandler`. Afterwards the notification is
  **updated in place** without sound (`setOnlyAlertOnce`) to "5 of 8 glasses today" with Android's own progress bar,
  and **removes itself after 4 s** (`setTimeoutAfter`); then widgets redraw and reminders re-plan.
- **A press counts once.** Each button carries a token (`<alarm key>@<time>:<button>`). The handler claims it before
  writing (stored in the local `preferences` table) and releases it if the write fails, so a double tap or a
  re-delivered broadcast after process death is ignored, and a failed one can be retried. "Prayed" is idempotent
  on its own too (count ≥ N). Tested in `ReminderActionHandlerTest`.
- **Snooze** stores "show again at" for that habit on this phone; the planner drops that habit's regular moments
  before it and adds one reminder at that time (even in quiet hours: the user asked for it). Logging clears it.
- **Progress** only through `NotificationCompat.setProgress` ("3 / 8" glasses, prayers marked). No fake animation:
  tapping the notification opens the habit (phase B's tap target), where the real card animation plays.

### Controls (the habit card's long-press sheet)
- New row between **Edit** and **Add to Home screen**: "🔔 Reminders: On · every 2 h" (or "Off", "On · at prayer
  times", "On · 7:00 PM", "Paused until Sat", "Needs your location"). It opens the reminders sheet
  (`presentation/goodhabits/reminders/HabitRemindersSheet.kt`): a "Remind me" switch with one line on how it works,
  the water window (From / Until) or the time, the quiet-hours note, "Pause for today" / "Pause for 1 week" (or
  "Resume reminders"), and for prayer the location card. Notices with a link when notifications are blocked or the
  Good-habit reminders type is switched off (phase B's capabilities).
- **Create form:** a prayer or water habit shows one card "Reminders on, change anytime" with a switch; turning it
  off stores "off". The card offers **Allow notifications** (the card is the reason) and, for prayer, **Use my
  location**.
- **Settings → Notifications:** "Pause habit reminders" for today or 1 week, on this phone only (events, milestones
  and the check-in keep coming). The app-wide notifications switch also stops habit reminders.
- Turning reminders on asks for the notification permission in context (phase B's `rememberNotificationAsk`, new
  reason line).

### Location for prayer times
`ACCESS_COARSE_LOCATION` only, asked only from a prayer habit's reminder controls (sheet or create form), never at
launch. One reading from the network provider (no GPS, no Google Play services, no new dependency), rounded to two
decimals (~1 km) before it is stored in the local `preferences` table (`device.reminders.location`), which is not
part of the synced settings. Denied: the prayer habit still works, its reminders stay off, and the sheet says why with
"Use my location" (asks again while Android allows) or "Open settings". "Update location" after travelling.
Prayer times are recomputed for each day of the plan (daily, and after a location or time-zone change).

### Storage (Room v9)
Five nullable columns on `good_habits`: `reminder_enabled`, `reminder_time`, `reminder_window_start`,
`reminder_window_end`, `reminder_paused_until` (`MIGRATION_8_9`, additive; null = "not chosen", the defaults apply).
They sync with the habit (Firestore `good_habit` payload, 15 of the 24 allowed fields; rules unchanged).

**Deviation from PLAN.md:** the plan suggested a separate `habit_reminders` table. A habit has exactly one reminder
setting, so columns on the habit row are simpler and safer: they sync, archive and delete with the habit, need no
join, no second sync type and no orphan cleanup. Per-phone state (location, snoozes, pressed buttons, the phone-wide
pause) is in the `preferences` table and never synced.

### Wording (EN + AR, in `Strings` / `ArabicStrings`)
Specific to each habit, varied (water has three titles, themed habits their own), never guilt. Examples:

| Type | English | Arabic |
|---|---|---|
| Water | 💧 **Time for a glass** · 3 of 8 so far today. Tap +1 when you drink it. | 💧 **وقت كوب ماء** · 3 من 8 حتى الآن اليوم. اضغط ‎+1 عندما تشربه. |
| Water, last glass | 💧 **Water break** · One more glass and today's water is done. | 💧 **استراحة ماء** · كوب واحد بعد ويكتمل ماء اليوم. |
| Fajr | 🌙 **Fajr has started** · Mark it here once you've prayed. | 🌙 **دخل وقت الفجر** · سجّلها هنا بعد أن تصلّي. |
| Maghrib | 🌅 **Maghrib time** · 3 of 5 marked today. Mark Maghrib here once you've prayed. | 🌅 **حان وقت المغرب** · سجّلت 3 صلوات اليوم. سجّل المغرب هنا بعد أن تصلّي. |
| Reading | 📖 **Your reading time** · Reading: 0 of 2 today. | 📖 **وقت القراءة** · Reading: ‏0 من 2 اليوم. |
| Weekly | 🌱 **Time for Gym** · Gym: 1 of 3 days this week. | 🌱 **حان وقت Gym** · Gym: ‏1 من 3 أيام هذا الأسبوع. |
| After "+1" | 5 of 8 glasses today (progress bar), then gone | شربت 5 من 8 اليوم |

The `ArabicCoverageTest` checks every reminder function and the Arabic plural forms ("أجّل ساعة", "أجّل 15 دقيقة",
"صلاتين"). Owner review of the final copy is welcome.

## Tests (`:shared:testAndroidHostTest`, 730 tests, 0 failures)

| Test | Covers |
|---|---|
| `PrayerTimeCalculatorTest` (6) | Cairo winter and summer, Mecca, London against published timetables (±2 min); high-latitude summer; order across a year |
| `HabitReminderPlannerTest` (20) | next trigger per strategy, water pace and last-glass wording, custom window, skip-if-logged, prayer offset and marked prayers, Fajr quiet-hours exemption, Isha after 22:00 dropped, no location = no prayer reminders, daily cap 8, 30-min gap and priority, snooze (also in quiet hours), habit and phone pause, two-day horizon, time zones, Egypt's DST day |
| `HabitReminderRulesTest` (7) | default-on rules (prayer and water on, the rest off, weekly water off), user choice wins, strategy for a 1-a-day prayer habit, quiet hours across midnight, water pace, broken window fallback |
| `ReminderActionHandlerTest` (7) | "+1 glass" once per press (re-delivery ignored), two reminders = two glasses, "Prayed" at-least and idempotent, failed write can be retried, snooze stored and cleared by a log, old press records pruned, location round trip |
| `ArabicCoverageTest` (+1, extended) | every reminder text translated, Arabic plurals |
| `tools/room_migration_check.py` | v9 columns match the entity; old rows keep null ("not chosen"). Still reports the 2 old `$REMOVED_BY_IMPORT_SQL` query errors (script limitation, not new) |

`:androidApp:assembleDebug` OK. Installed on the owner's Oppo Reno 10 5G (Android 15) over the phase B build
(v8 → v9 migration ran, app opened, no crash in logcat).

## Device test steps (Oppo Reno 10 5G)

Useful commands while testing:
```
adb shell dumpsys alarm | grep -B2 -A6 "com.adhamelgendy.nixura"     # armed alarms: origWhen, window
adb logcat -s NixUraReminder                                           # "deliver good:… late=…ms", "action … -> …"
adb shell cmd deviceidle force-idle                                    # simulate deep sleep (Doze)
adb shell cmd deviceidle unforce
```

1. **Install** the build. Open NixUra once. Settings → Apps → NixUra → Notifications: "Good-habit reminders" is on.
2. **Water, default on:** create "Drink water" (8 glasses). The form shows "Reminders on, change anytime" (switch on).
   Long-press the card: the sheet shows "Reminders: On · every 2 h". Open it: From 9:00 AM, Until 9:00 PM.
3. **Water timing:** in the sheet set From = now − 1 h 50 min, Until = now + 4 h. Within ~10 min of the next even
   step a notification comes: drop icon, water blue, "Time for a glass · 0 of 8…", progress bar, buttons
   "+1 glass · 250 ml" and "Snooze 1 h". `dumpsys alarm` shows it with `window=+10m`. No alarm icon in the status bar.
4. **+1 from the notification (app closed):** swipe NixUra away from recents, then press "+1 glass". The notification
   turns into "1 of 8 glasses today" with the bar, makes no sound, and disappears after ~4 s. The water widget (if
   placed) shows 1. Open the app: the card shows 1 glass.
5. **Once per press:** press "+1 glass" twice very fast on a new reminder: the count goes up by 1.
6. **Skip if logged:** log enough glasses in the app to be on pace; the next water alarm disappears from `dumpsys
   alarm` (re-planned). Reach the goal: no more water reminders today.
7. **Snooze:** press "Snooze 1 h": "OK, again in 1 h." then it goes; it comes back an hour later.
8. **Prayer, location:** create "Pray" (5 a day). The form card offers "Use my location": tap it, Android asks for
   approximate location, allow. Long-press the card → "Reminders: On · at prayer times", sheet says "Prayer times follow
   your saved location". Deny instead on a second try: the sheet says the reminders stay off and offers the button again.
9. **Prayer timing:** compare with a local timetable: a reminder ~10 min after each prayer time (gold arch icon,
   "Prayed ✓", "Snooze 15 min"). With "Alarms & reminders" allowed it comes on the minute. Press "Prayed ✓": "Asr
   marked · 3 of 5 today", the card's stars fill; the next prayer's reminder still comes, the marked one never.
10. **Quiet hours:** a daily habit with reminder time 23:00: the sheet warns it is in quiet hours; nothing comes. Fajr
    (before 08:00) still comes for the prayer habit.
11. **Pause:** "Pause for today" on a habit: the row says "Paused until <tomorrow>", nothing today, back tomorrow.
    Settings → Notifications → "Pause habit reminders" → For today: no habit reminder today (events still come).
12. **Default off:** create "Read" (daily): long-press → "Reminders: Off". Turn it on, set 2 min from now: reading
    reminder, leaf icon, "📖 Your reading time", "Done ✓".
13. **Reboot:** with reminders armed, restart the phone; `dumpsys alarm` lists them again; they fire.
14. **Time zone / clock / DST:** Settings → Date & time → turn off automatic, change the time zone: the next water
    reminder is re-planned to the same local time (check `origWhen`). Set the clock forward past a reminder: it fires
    (within 45 min) or is dropped if older.
15. **Doze:** with a water reminder ~15 min away, lock the phone and run `cmd deviceidle force-idle`; it is held; run
    `unforce` (or turn the screen on): it arrives then, or is dropped if more than 45 min late.
16. **Arabic:** switch to العربية: notification titles, bodies and buttons are in Arabic ("‎+1 كوب · 250 مل", "صلّيت ✓",
    "أجّل ساعة"); the sheet reads right to left.

## How later phases should use this

- Plan good-habit reminders only in `HabitReminderPlanner` (pure); post only through `NixNotifications`
  (`NixNotificationRequest` now also takes `progress`, `onlyAlertOnce`, `timeoutAfterMillis`, `deleteIntent`).
- A new button type: add it to `NotificationActionType`, handle it in `ReminderActionHandler` through an existing use
  case, add its icon in `ReminderActions`.
- Phase D (lock screen): `NotificationProgress` and the one-notification-per-habit key are the hooks for a progress-
  style or ongoing notification; quit habits must stay off the lock screen.

## Limits and follow-ups

- **iOS:** not implemented. iOS still has no notification scheduler (phase B left the no-op), and location for prayer
  times is `UnavailableDeviceLocation` (the sheet shows "Needs your location" and the note, without a button). Needs a Mac:
  `UNUserNotificationCenter` scheduler + categories for the buttons, `CLLocationManager` "when in use". Then run
  `./gradlew :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosSimulatorArm64` and build `iosApp` in Xcode.
  The shared code added here compiles for Android; iOS compilation is **unverified** on this Windows machine.
- **Not built from PLAN.md:** "auto-quiet" (offer "Fewer reminders?" after 3 dismissed days without logging) needs a
  dismissal record and an agreed meaning of "fewer"; left for a follow-up.
- **Isha after 22:00** is dropped by the quiet-hours rule as written in PLAN.md (only Fajr is exempt). In summer at high
  latitudes that removes Isha; exempting all prayers is a one-line change if the owner prefers it.
- Notification text uses Western digits (as phase B's notifications do); Settings → Digits is not applied to it yet.
- The water window and the time are per habit and synced; quiet hours are fixed at 22:00-08:00 (no setting yet).
- **Merged 2026-11-22** on the owner's go-ahead. Per-step device results were not reported, so the steps above stay the
  checklist for the next device pass (install + `dumpsys alarm` were checked by the build session).
