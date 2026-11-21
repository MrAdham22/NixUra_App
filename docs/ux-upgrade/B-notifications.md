# UX phase B: notification foundation

**Date:** 2026-11-21. **Branch:** `feature/notification-foundation`. **Brief:** `docs/ux-upgrade/PLAN.md` (section B).

**Goal.** You can tell a NixUra notification and its type (event, good habit, quit support, coach, app) before you
read the text. Notifications stay useful, never spammy, and can be dismissed as usual.

## Owner decisions taken in this phase (2026-11-21)

1. **Tapping an event reminder opens that event** (Events tab → the event's screen), as PLAN.md says. Before, every
   notification opened Home. The text of event reminders did not change (timing: see "Timing" below).
2. **Streak milestones and the daily check-in move to their own quiet channels now** (Quit progress, Coach), as
   PLAN.md says. Before, they shared the events channel and made a sound.

## What was built

### One notification layer (Android)
`androidApp/.../notifications/NixNotifications.kt` is the **only** place that builds an Android notification or
creates channels. It owns:

| Part | Where |
|---|---|
| Channels and channel groups (created at app start, names follow the app language) | `NixNotifications.ensureChannels` |
| Look per type: small icon, accent colour, category, priority | `NixNotificationStyle.kt` (`NixNotificationStyles.of(kind, theme)`) |
| Grouping: one group key per type, summary posted once 2+ of that type are showing | `NixNotifications.post` |
| Tap target: the screen the notification is about | `NotificationDeepLink.kt` (applied in `MainActivity.onCreate` / `onNewIntent`) |
| Per-channel state and the system channel screen | `NixNotifications.channelEnabled`, `channelSettingsIntent` |

The pure parts live in shared code so they are unit-tested and iOS can mirror them:
`shared/.../domain/notification/NotificationKind.kt` (the five types, plus reading the type from an old key),
`NotificationChannels.kt` (channel catalog, type → channel, group keys), `NotificationAccess.kt` (permission state).

`ReminderReceiver` now only turns the alarm's extras into a `NixNotificationRequest` and calls `post`.

### Channels (IDs are permanent once released)

| Channel ID | Name EN / AR | Importance | Group | Used for |
|---|---|---|---|---|
| `nixura_reminders` (**kept**) | Events / المناسبات (was "Reminders & coaching") | DEFAULT | Events | event reminders |
| `nixura_good_habits` (new) | Good-habit reminders / تذكيرات العادات الجيدة | DEFAULT | Habits | phase C (water, prayer, counters) |
| `nixura_quit_progress` (new) | Quit progress / تقدّم الإقلاع | LOW (silent) | Habits | streak milestones |
| `nixura_coach` (new) | Coach / المدرب | LOW (silent) | Coach & app | daily check-in, coach notes |
| `nixura_app` (new) | App & account / التطبيق والحساب | LOW (silent) | Coach & app | sync problems, account (nothing posts here yet) |

- **Why the old ID is kept for events:** it already carried event reminders, so a phone that has the app keeps the
  user's sound/silence choice for it. Only the name changed (allowed; importance cannot change after creation).
- **Nothing retired.** `NotificationChannels.RETIRED_CHANNEL_IDS` is empty; if a channel is ever retired, add its ID
  there and `ensureChannels` deletes it.
- Only time-bound types (events, good-habit moments) make sound. A test keeps it that way.
- Prayer has no channel of its own. If adhan-style sounds are ever wanted, that would be a new channel then.
- Freeze this list before the first release.

### Look per type

| Type | Small icon | Accent | Category | Tap opens |
|---|---|---|---|---|
| Event | calendar | `#862549` | `event` | Events tab → that event |
| Good habit (default) | leaf | `#E8BCB9` | `reminder` | Build tab → habit detail |
| Good habit, theme WATER | drop | `#5BB6EC` (water card blue) | `reminder` | same |
| Good habit, theme PRAYER | arch + star | `#F1B451` (prayer gold) | `reminder` | same |
| Quit milestone | broken chain | `#AE445A` | `status` | Quit tab → habit detail |
| Coach | sparkle | `#F39F5A` | `recommendation` | Coach tab |
| App | NixUra leaf | `#F39F5A` | `status` | Home |

Icons: `res/drawable/ic_notif_*.xml` (white strokes; Android tints them).

### Model change (backward-compatible)
`PlannedNotification` gained `kind` (defaults to what the key prefix says), `targetId` and `theme`. The planner sets
them. `AndroidNotificationScheduler` stores and reads them; a plan stored before phase B (no kind) is read by key
prefix (`event:`, `milestone:`, `daily:`, `good:`), and alarms armed before the update still post correctly
(`ReminderReceiver` falls back to the key in the alarm's data URI). Notification ids are still `key.hashCode()`.

### Permission (Android 13+ POST_NOTIFICATIONS)
- **Never at launch.** Asked in context only: when the user adds an event reminder, or turns reminders on in
  Settings. (Phase C adds: when reminders are turned on for a good habit.)
- **Reason first.** `rememberNotificationAsk()` (`presentation/components/NotificationAsk.kt`) shows a one-line
  reason card (Allow / Not now) and only then the system dialog.
- **Denied:** after one denial Android still shows its dialog, so the app asks again (with the reason) next time.
  After the second denial Android stays silent: the app shows, once per screen, "Notifications are blocked" with
  **Open settings**. The user's action (adding the reminder, the switch) always goes through anyway.
- The decision is a pure function, `NotificationAccessPolicy.resolve` (tested), fed by
  `AndroidNotificationPermission.access()` with: SDK level, permission state, the app-wide system switch, "asked
  before" (stored in `nixura_notification_permission` prefs) and `shouldShowRequestPermissionRationale`.
- Known edge: closing the system dialog by tapping outside it is not a denial, but looks like one to Android's API.
  The app then offers system settings, which still works.

### In-app controls (only what Android lacks)
- **Settings → Notifications:** the existing switch and check-in time stay (Android has neither). New row
  "Notification types" → **Open settings** (the app's page in system settings, where each channel can be silenced).
  When notifications are blocked, the same row says so in the warm colour.
- **Event form:** if the Events channel is switched off in system settings, a notice "Event reminders are turned
  off in system settings" with **Turn on in settings** opens that channel's own screen.
- No per-type toggles in the app: they would duplicate Android's channel screen.

### Timing (fixed after the first device test, 2026-11-21)
- **What went wrong:** on the Oppo an event's "at time" reminder came ~2-3 min late, the check-in ~2 min late, and a
  "10 min before" reminder never came.
- **Cause 1, ColorOS widens alarms.** `dumpsys alarm` showed NixUra's `setExactAndAllowWhileIdle` alarms accepted as
  exact (`exactAllowReason=permission`) yet given a delivery window of ~75% of the time left (3 m 14 s for an alarm
  4 min ahead; inexact ones get 1 h). Only alarm clocks keep `window=0`.
  Alarm clocks show the status-bar alarm icon and "next alarm" on the clock / lock screen. `USE_EXACT_ALARM` was
  tried and does not help (ColorOS still gave a 1 h window; Google Calendar is exempt by package, not permission).
  **Fix, hybrid timing (owner decision 2026-11-21):** when exact alarms are allowed, a reminder at most
  `NotificationArming.ALARM_CLOCK_LEAD_MILLIS` (75 min) away is armed with `AlarmManager.setAlarmClock` (the "show"
  intent opens what the reminder is about). A farther one is an ordinary exact alarm (on time on most phones), plus
  **one** hand-off wake-up (`ReminderReceiver.ACTION_HAND_OFF`) at 75 min before the soonest of them. The wake-up
  re-arms the stored plan, so that reminder becomes an alarm clock in time; arming the same PendingIntent replaces
  the old alarm, so a reminder never has two. Even with ColorOS's widening (75% of the time left, at most 1 h) the
  wake-up arrives by 15 min before the reminder (tested in `NotificationArmingTest`). The alarm icon therefore shows
  only 15-75 min before a reminder. If the wake-up is lost, the ordinary exact alarm still fires (at worst late).
  Without the exact-alarm permission everything is inexact (up to 1 h late).
- **Cause 2, a re-plan cancelled late alarms.** Each re-plan (every app start and data change) cancelled all alarms and
  re-armed only future ones, so an alarm whose time had passed but that the OS had not delivered yet was lost.
  **Fix:** the planner keeps items up to `NotificationArming.LATE_GRACE_MILLIS` (1 h) late; the pure
  `NotificationArming.select` (tested) re-arms such an item only if it is still planned, was armed before at that
  same time, and was not delivered (`ReminderReceiver` records deliveries via `markDelivered`). It then fires at once.
- **Measured after the fix** (Oppo, screen off): check-in due 19:38:00 posted 19:38:00.04; event reminders due
  19:39:00 and 19:40:00 posted at .02 s.

### Re-arming
`BootReceiver` also listens to `TIMEZONE_CHANGED` and `TIME_SET` and **re-plans** (`NotificationSyncService.syncOnce`),
since stored times were computed for the old local time. Reboot / update / exact-alarm changes still re-arm the stored plan.

## How phase C should use this API

1. **Plan in the domain.** Emit `PlannedNotification(key = "good:<habitId>", kind = GOOD_HABIT, targetId = habitId,
   theme = habit.theme, …)`. Keep **one key per habit** so a new reminder replaces the old one (never stacks).
   The scheduler, receiver and tap target already handle `GOOD_HABIT`; water and prayer already get their icon/colour.
2. **Actions** ("+1 glass", "Prayed ✓", Snooze): build `NotificationCompat.Action`s whose PendingIntents target a new
   **non-exported** action receiver, and pass them in `NixNotificationRequest.actions`. To carry them from the alarm,
   either extend `ReminderReceiver` to build them for `GOOD_HABIT` or add a small action factory next to it. Do not
   call `NotificationCompat.Builder` anywhere else.
3. **Update in place** after an action: call `NixNotifications.post` again with the same key and the new text
   ("5 of 8 glasses"); `NixNotifications.cancel(context, key)` to remove it.
4. **Ask for permission** with `rememberNotificationAsk().ensure(reason)` when reminders are switched on for a habit
   (add an EN + AR reason string). Show the channel state with `ReminderCapabilities.channelEnabled(GOOD_HABIT)` and
   link with `openChannelSettings(GOOD_HABIT)`.
5. **Timing is handled for you:** the scheduler applies the hybrid timing above to every kind, and
   `NotificationArming` keeps late-but-undelivered ones. Keep one key per habit moment so the delivery record matches.
6. After a log from a widget or an action while the UI is closed, call `NotificationSyncService.syncOnce()`.

## Verification

- `:androidApp:assembleDebug`: OK.
- `:shared:testAndroidHostTest`: **689 tests, 0 failures** (new: `NotificationChannelsTest` 9, `NotificationAccessPolicyTest` 7,
  `NotificationArmingTest` 13, 3 planner tests for late items).
- The 50 ms measurement above was taken with alarm clocks for every reminder. The hybrid hand-off on the phone is
  **not measured yet** (the phone was disconnected); see DEVELOPMENT_STATUS.md.
- Installed on the owner's Oppo Reno 10 5G (Android 15). `adb shell dumpsys notification` there shows the five
  channels with the right importance and groups, and `nixura_reminders` renamed to "Events".
- **Not verified yet:** what each notification looks like on the phone, taps, the permission flow (the Oppo already
  had the permission granted), grouping, the time-zone re-plan. iOS: nothing changed there (still a no-op scheduler).

## Device test steps (Oppo Reno 10 5G)

1. Install the build, open NixUra once. Settings → Apps → NixUra → Notifications: you see three sections
   (Events, Habits, Coach & app) and five types; Events / Good-habit reminders allow sound, the others are silent.
2. **Event:** add an event 15 minutes ahead with the "10 min" reminder. Lock the phone. When it fires: calendar icon,
   wine colour, sound as usual. Tap it: NixUra opens on that event.
3. **Two events:** add two events with reminders that fire close together. When both are showing, they bundle under
   one "Events" group.
4. **Milestone:** add a quit habit that started 23 h 55 min ago (backdated start). At 1 day: broken-chain icon, rose
   colour, **no sound**. Tap: the quit habit's detail opens.
5. **Check-in:** Settings → daily check-in → set it 2 minutes ahead. Sparkle icon, amber, no sound. Tap: Coach opens.
6. **Permission:** uninstall, reinstall, open the app: no permission dialog. Add an event reminder: a NixUra card
   "Allow notifications?" appears first; tap **Not now** (no system dialog). Add another reminder → card → **Allow** →
   system dialog → **Don't allow**. Add another → card → Allow → **Don't allow** again. Add another: now
   "Notifications are blocked" with **Open settings**, which opens NixUra's notification page.
7. **Channel off:** in system settings switch off only "Events". Open an event that has reminders: the notice
   "Event reminders are turned off in system settings" shows; **Turn on in settings** opens that channel.
8. **Settings row:** Settings → Notifications → "Notification types" → Open settings opens NixUra's page.
9. **Arabic:** switch the app to العربية, reopen; the channel names in system settings are in Arabic.
10. **Time zone:** with a check-in scheduled, change the phone's time zone; the check-in still fires at its local time.

## Limits and follow-ups

- iOS notifications are still not implemented (PLAN.md lists a `UNUserNotificationCenter` scheduler; needs a Mac).
  The shared catalog (`NotificationChannels.groupKeyFor` → `threadIdentifier`) is ready for it.
- Nothing posts to the App channel yet (sync problems could, later).
- Channel names follow the app language at the next app start (when `ensureChannels` runs).
