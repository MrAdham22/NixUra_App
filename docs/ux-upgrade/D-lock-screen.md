# UX phase D: lock screen and persistent presence (research)

**Written:** 2026-11-22. **Branch:** `docs/lock-screen-research`. **Research only: no app code changed.**
(PLAN.md called this file `D.md`; the owner's brief named it `D-lock-screen.md`.)

**Question.** Can NixUra show habit progress, the next reminder, the nearest event or a streak on the lock screen,
truthfully, on NixUra's SDK range and on the owner's Oppo Reno 10 5G? What does each mechanism cost, and which one
should be built?

**Ground rules from the brief.** Nothing fake (no "pinned forever" claims Android does not honour), quit habits stay
off the lock screen by default (PLAN.md), the app is not released so nothing has to stay compatible, recommend the
best design rather than the safest change.

---

## 1. The ground we stand on

| Fact | Value | Source |
|---|---|---|
| SDK range | `minSdk 24` (Android 7.0) to `targetSdk 37` / `compileSdk 37` (Android 17) | `gradle/libs.versions.toml` |
| Test phone | OPPO Reno 10 5G (CPH2531), **Android 15, ColorOS 15** | PLAN.md, owner decision 6 |
| ColorOS 16 for this phone? | **Not on OPPO's list.** The ColorOS 16 page lists only the **Reno10 Pro+ 5G** from the Reno10 family (read 2026-11-22). Plan on the Reno 10 5G staying on Android 15. | [oppo.com/en/coloros16](https://www.oppo.com/en/coloros16/) |
| What phase B/C already gives us | One notification layer (`NixNotifications`), five channels, one notification per habit (`good:<habitId>`), `NotificationProgress` (a progress bar), buttons (`+1 glass`, `Prayed ✓`), alarm-clock timing for events and prayer | `B-notifications.md`, `C-reminders.md` |
| What the lock screen shows of NixUra today | `NixNotifications` never calls `setVisibility`, so every notification is `VISIBILITY_PRIVATE` (the default). See the privacy finding below. | `androidApp/.../notifications/NixNotifications.kt` |
| Home-screen widgets | Three widgets, all `android:widgetCategory="home_screen"` | `res/xml/widget_*_info.xml` |

### Privacy finding (fix it whatever else is chosen)

`VISIBILITY_PRIVATE` hides content only when the **user** has set the lock screen to hide sensitive content. The
Android docs: private shows "only basic information, such as the notification's icon and the content title", and
`setPublicVersion()` supplies a replacement ([Create a notification, lock screen visibility][notif-build]).
The **title** is not redacted. On a phone set to "show all notification content" (a common setting), the whole
notification shows. Today that includes the quit-milestone notification ("7 days free of smoking"), and PLAN.md says
quit habits must be hidden on the lock screen by default. **Not checked on the Oppo yet** (the phone was not
connected during this session); the check is in section 6.

---

## 2. The mechanisms compared

Effort is **on top of phase B/C** (S = under a day, M = 2-3 days, L = a week or more).

### 2.1 Normal notifications with lock-screen visibility (`setVisibility` / `setPublicVersion`)

| | |
|---|---|
| **What it shows** | Any NixUra notification that is in the shade when the phone is locked: per type, its full content (`VISIBILITY_PUBLIC`), only a safe substitute (`VISIBILITY_PRIVATE` + `setPublicVersion`, e.g. "NixUra · a milestone reached"), or nothing at all (`VISIBILITY_SECRET`). Channels can set a ceiling (`NotificationChannel.setLockscreenVisibility`). |
| **Android versions** | API 21+, so the whole range (24-37). |
| **ColorOS 15 (Oppo)** | Standard Android setting; ColorOS also has its own lock-screen notification switches. Needs a device check (section 6). |
| **Limits** | The user always wins: "the user always has ultimate control over whether their notifications are visible on the lock screen" ([docs][notif-build]). Only notifications that exist are shown; nothing here is permanent. |
| **Battery** | None beyond posting. |
| **Annoyance** | None added; it only changes what an existing notification reveals. |
| **Play policy** | Nothing to declare. |
| **Effort** | **S.** A pure `LockScreenPrivacy` rule in `domain/notification` (kind + user setting → PUBLIC / PRIVATE-with-public-version / SECRET), applied in `NixNotifications.post`; the public version's text in `Strings` + `ArabicStrings`; unit tests. |

### 2.2 An ongoing "Today" notification (`setOngoing(true)`, no foreground service)

| | |
|---|---|
| **What it shows** | One quiet, always-current line or two: "💧 3 of 8 · 🕌 2 of 5 · Next: Asr 15:12 · Mum's birthday in 2 days". Can carry a ticking countdown to the next reminder (`setUsesChronometer` + `setChronometerCountDown`, drawn by the system, no wake-ups) and the existing `+1 glass` / `Prayed ✓` buttons. Streaks of quit habits only if the user opts in. |
| **Android versions** | API 24+ (countdown chronometer is API 24). Whole range. |
| **Is it really persistent?** | **No, and the doc must not pretend it is.** Since Android 14, `setOngoing(true)` notifications **can be swiped away**; they stay non-dismissable only "when the phone is locked" and against **Clear all** ([Android 14 behaviour changes][a14]). The phone reboots and ColorOS force-stops can also clear it. So: it stays on the lock screen while the phone is locked, the user can remove it when unlocked, and NixUra must respect that (re-post only the next day or when the user turns it back on). |
| **ColorOS 15 (Oppo)** | A posted notification lives in the system, not in our process, so ColorOS killing NixUra does not remove it. A force-stop does. Lock-screen filtering of **silent** notifications differs by OEM (Pixel has "hide silent notifications"); to be safe the channel should be DEFAULT importance with no sound and no vibration rather than LOW. Needs a device check. |
| **Battery** | Near zero if it is **event-driven**: re-post only where the app already writes (app log, widget +/-, notification button, reminder delivered, event change), plus one inexact wake-up at local midnight for the day roll (the widgets already have a refresh alarm and `ReminderRefreshWorker` exists). No polling, no service. |
| **Annoyance** | One row in the shade and an icon in the status bar, all day. This is the real cost: some people will hate it, hence **opt-in only**, with an easy "Hide for today" button. |
| **Play policy** | A plain ongoing notification needs no declaration. **A foreground service to keep it alive would be rejected:** none of the FGS types fits a passive status display; `health` needs body sensors or activity recognition; `specialUse` is reviewed in Play Console ([FGS types][fgs]). So: no service, ever. Play's spam rules also mean no ads or promos in it. |
| **Effort** | **M.** Pure `TodaySummary` builder in `domain` (picks what fits: good-habit counts, next planned reminder from `HabitReminderPlanner`, nearest event from `NearestEventSelector`, quit streaks only with opt-in), a new channel `nixura_today` ("Today on lock screen" / "اليوم على شاشة القفل"), a Settings switch, re-post hooks next to the existing `WidgetUpdater.requestRefresh` calls, delete-intent handling, EN + AR strings, tests. |

### 2.3 Live Updates (promoted ongoing notifications, `ProgressStyle`)

| | |
|---|---|
| **What it shows** | A notification promoted to the top of the shade and the lock screen and shown "as a chip in the status bar" ([Live Updates][live]); `ProgressStyle` adds a segmented progress bar with points and a tracker icon ([ProgressStyle][progress]). |
| **Android versions** | Android 16 (API 36) and newer. Needs the normal permission `POST_PROMOTED_NOTIFICATIONS`, `setRequestPromotedOngoing(true)`, ongoing, a title, no custom views, not colorized, not a group summary, channel not `IMPORTANCE_MIN`; check `canPostPromotedNotifications()` ([Live Updates][live]). |
| **ColorOS** | ColorOS 16 (Android 16) shows Live Updates in Fluid Cloud for more third-party apps (OPPO's page mentions rides, scores, timers). **The Reno 10 5G is not on the ColorOS 16 list**, so on the owner's phone this does nothing. Older ColorOS Fluid Cloud had no public API for third-party apps. |
| **Allowed uses** | Google lists the acceptable cases: "Active navigation, ongoing phone calls, active rideshare tracking, and active food delivery tracking", and the unacceptable ones: "Ads, promotions, chat messages, alerts, upcoming calendar events, and quick access to app features" ([Live Updates][live]). `ProgressStyle` is for "user-initiated start-to-end journeys" ([ProgressStyle][progress]). |
| **Fit for NixUra** | **Poor.** An all-day habit summary is not a user-started journey with an end; the event countdown is literally "upcoming calendar events". It would only fit a future feature the user starts and that ends, e.g. a 10-minute "ride out the craving" timer. Users can demote a Live Update, and "posting unwanted Live Updates might cause users to revoke an app's posting permission" ([Live Updates][live]). |
| **Battery / annoyance** | Same as 2.2, plus a status-bar chip: more visible, more annoying when misused. |
| **Play policy** | No Play declaration is documented for `POST_PROMOTED_NOTIFICATIONS`. The risk is breaking Google's stated criteria (the system or a later policy may demote or block), not a form. Not worth it for a use Google lists as unacceptable. |
| **Effort** | S-M on top of 2.2, but **not recommended** now. |

### 2.4 `MetricStyle` (Android 17)

| | |
|---|---|
| **What it shows** | Up to 3 metrics, each a label, a value and an optional unit; values can be fixed numbers/text/dates/times or a time difference. Shown in the shade, as a Live Update when promoted, and on the always-on display ([MetricStyle][metric]). |
| **Android versions** | Android 17 (API 37) only. NixUra's `compileSdk`/`targetSdk` is already 37, so it compiles. |
| **Fit for NixUra** | **Good, and it is a real native template:** Google names "health and fitness apps, timers" as its use cases ([MetricStyle][metric]). "Water 3/8 · Prayers 2/5 · Next 15:12" is exactly three metrics. Use it as the **look** of the Today notification (2.2) on Android 17, **without** requesting promotion. The docs do not say whether a non-promoted MetricStyle reaches the AOD; treat that as unknown until seen on an Android 17 emulator. |
| **ColorOS** | Needs Android 17; not on the owner's phone. |
| **Battery / annoyance / Play** | Same as 2.2. |
| **Effort** | **S** after 2.2 (an API 37 branch in `NixNotifications` that builds with the platform `Notification.Builder`; the docs do not mention a `NotificationCompat` version). Verify on an API 37 emulator. |

### 2.5 Lock-screen widgets

| | |
|---|---|
| **What it shows** | The existing widgets (good-habit quick log with +/-, events countdown, quit counter) placed by the user on the lock screen. |
| **Android versions** | `WIDGET_CATEGORY_KEYGUARD` exists since API 17 but phones dropped lock-screen widgets in Android 5.0. They came back in AOSP after Android 16 QPR1 (Google: "Lock screen widgets will be available in AOSP for tablets and mobile starting with the release after Android 16 (QPR1)", via [Android Central][ac-lsw]); Pixel phones got them in Android 16 QPR2. API 36 added `WIDGET_CATEGORY_NOT_KEYGUARD` so a widget can refuse the lock screen ([AppWidgetProviderInfo][awpi]); the Glance docs describe `widgetCategory` "home_screen" / "keyguard" ([Glance widget][glance]). Whether a phone offers it is up to the maker. |
| **ColorOS** | **Not on ColorOS 15** (the owner's phone). ColorOS 16 support for third-party lock-screen widgets is not documented by OPPO's page; unknown. |
| **Is it permanent?** | Yes, in the honest sense: the user places it and it stays until they remove it. It is the only mechanism here that is truly "always on the lock screen". |
| **Battery** | Same as today's home-screen widgets (system `Chronometer`, one inexact refresh alarm). |
| **Annoyance** | None: the user chose it. |
| **Privacy** | The +/- buttons work without unlocking; logging a glass of water from a locked phone is harmless. The **quit** widget should get `not_keyguard` (keeps PLAN.md's rule: quit off the lock screen unless the user opts in inside the app, which a widget picker cannot show). |
| **Play policy** | Nothing to declare. |
| **Effort** | **XS-S.** Add `keyguard` to `widget_good_info.xml` and `widget_event_info.xml`, `not_keyguard` to `widget_quit_info.xml`; check the layout planner at lock-screen sizes on a Pixel API 36.1+ emulator. |

### 2.6 Other native mechanisms

| Mechanism | What it would show | Verdict |
|---|---|---|
| **Quick Settings tile** (`TileService`, API 24+; add prompt `requestAddTileService` on Android 13+) | "💧 3 of 8" with a tap to add a glass, reachable from the lock screen's pull-down; `isSecure()` / `unlockAndRun()` decide what needs unlocking ([QS tiles][qs]) | Good **later** add-on for water only. Google: "avoid using tiles that display information, but aren't interactive", max two per app, so it must be a +1 button, not a dashboard. Not a lock-screen display in itself. Effort S. |
| **Alarm clock "next alarm"** (`setAlarmClock`) | The clock / lock screen shows the next alarm time | Already used by phase B for events and prayer within 75 min of the reminder. **Do not** extend it to show habit info: it would claim an alarm that is not an alarm, and Play's exact-alarm rules watch this. |
| **Live wallpaper** (`WallpaperService`) | A streak drawn into the wallpaper | Rejected: replaces the user's wallpaper, ColorOS handles lock-screen wallpapers separately, costs more battery, and feels like a trick. |
| **At a Glance / Smartspace, ColorOS AOD panels** | | No public API for third-party apps. AOD shows notification icons only (except MetricStyle on Android 17, section 2.4). |
| **Foreground service** | A notification that cannot be swiped on old Android | Rejected (no valid FGS type, Play review; section 2.2). |

### 2.7 iOS (for parity; nothing can be built or run on Windows)

| Mechanism | Fit |
|---|---|
| **Lock-screen widgets** (WidgetKit accessory families, iOS 16+) | **The iOS equivalent of 2.5, and better supported than on Android.** Comes with the already-planned iOS widget work (WidgetKit extension + App Group). |
| **Live Activities** (ActivityKit, iOS 16.1+) | Apple limits them to activities with a start and end (they end after at most 8 hours, and can stay on the lock screen a few hours more). Same verdict as Live Updates: only for a future user-started timer, not an all-day summary. Needs a Swift widget extension. |
| **Notifications** (`UNUserNotificationCenter`) | Phase B's iOS scheduler is still a no-op. Lock-screen previews are the user's setting on iOS; there is no `setPublicVersion` equivalent, so quit text must be written neutrally for iOS. |

Apple references: [WidgetKit, lock screen accessories][wk-acc], [ActivityKit][activitykit]. Unverified on this machine.

---

## 3. Summary table

| Mechanism | Android | Oppo Reno 10 5G (Android 15) | Really persistent? | Battery | Annoyance | Play risk | Effort |
|---|---|---|---|---|---|---|---|
| Visibility / public version | 21+ | yes | no (only what's posted) | none | none | none | S |
| Ongoing "Today" notification | 24+ | yes | while locked; swipeable when unlocked (14+) | ~0 (event-driven) | medium (opt-in) | none if no FGS | M |
| Live Update / ProgressStyle | 36+ | no | while promoted; user can demote | ~0 | high if misused | use case not allowed | S-M |
| MetricStyle | 37+ | no | as the notification | ~0 | low | none (not promoted) | S after "Today" |
| Lock-screen widget | Pixel 16 QPR2+, OEM-dependent | no | yes, user-placed | low (existing) | none | none | XS-S |
| QS tile (+1 glass) | 24+ | yes | user-placed, in pull-down | ~0 | none | none | S |
| FGS / alarm-clock abuse / wallpaper | | | | | | rejected | |

---

## 4. Recommendation: a private-by-default "Today" presence

Build these three together; together they are the best honest lock-screen presence Android allows, and the first two
work on the owner's phone today.

1. **Lock-screen privacy rules (do first, S).** Per type, by default:

   | Type | Lock screen default | Public version (what a stranger sees) |
   |---|---|---|
   | Good habit (water, prayer, other) | full content | (not needed) |
   | Event | title hidden | "Event in 2 hours" / "مناسبة بعد ساعتين" |
   | Quit milestone | **nothing** (`VISIBILITY_SECRET`, channel ceiling too) | (none) |
   | Coach | title hidden (it can greet by name) | "A note from your coach" |
   | App / account | title hidden | "NixUra" |
   | Today (item 2) | its own content, built without quit info unless opted in | |

   One Settings row, **Lock screen**: "Show habit progress" (default on) and "Show quit streaks" (default **off**; when
   on, quit lines appear in the Today notification and milestone notifications become PRIVATE with a neutral public
   version, still never the habit name). The rule is a pure, tested function in `domain/notification`.

2. **"Today" notification, opt-in (M).** One quiet ongoing notification on a new DEFAULT-importance, soundless,
   vibration-less channel `nixura_today`, with: good-habit progress for today, the next planned reminder with a
   system-drawn countdown, the nearest event, and the `+1 glass` / `Prayed ✓` button for whatever is next. Re-posted
   only when data changes and once at midnight. If the user swipes it away, it stays away until tomorrow; a
   "Hide today" button does the same. Offered in context (when the user adds a second good habit, or from the habit
   sheet: "Show today's progress on the lock screen"), never at first launch. On Android 17 it uses **MetricStyle**
   (three metrics), not promoted. Copy in EN + AR, digits through the digit setting.

3. **Lock-screen widgets where they exist (XS).** `keyguard` on the good-habit and events widgets, `not_keyguard` on
   the quit widget. No effect on ColorOS 15; real on Pixel-style Android 16 QPR2+ phones.

**Not now:** Live Update promotion (Google lists our use as unacceptable; revisit only if a user-started timer such
as "ride out a craving" is built, which would be a real start-to-end journey), foreground services, alarm-clock
tricks, wallpaper. **Later, optional:** a water QS tile.

### An owner decision this needs

PLAN.md says notifications never duplicate the widget's dashboard ("no 'today's summary' notifications"). Item 2 is
exactly a today's-summary notification. It differs from the nudges that rule was about (opt-in, silent, never
alerts, one row), but it does reverse a written rule, so it needs the owner's yes. Without it, items 1 and 3 still
stand on their own, and the lock screen shows NixUra only when a reminder is due.

### What it would take

| Step | Files (planned) | Size |
|---|---|---|
| Privacy rule + Settings row + public versions | `domain/notification/LockScreenPrivacy.kt` (+ test), `NixNotifications.kt`, channel ceiling in `NotificationChannels.kt`, Settings screen + ViewModel, `Strings` / `ArabicStrings` | S |
| Today builder | `domain/notification/TodaySummary.kt` (+ tests: no quit info without opt-in, nearest event, next reminder, Arabic plurals) | S-M |
| Today channel and posting | `NotificationChannels.kt` (new `nixura_today`, freeze list before release), `NixNotifications.kt` (ongoing, chronometer, actions, delete intent), re-post hooks beside `WidgetUpdater.requestRefresh`, midnight refresh | M |
| MetricStyle branch (API 37) | `NixNotifications.kt` | S |
| Widget categories | `res/xml/widget_*_info.xml` | XS |
| iOS | lock-screen accessory widgets with the planned WidgetKit work; neutral quit wording in the iOS notification scheduler | unverified (Mac) |
| Docs | README features, PROJECT_CONTEXT, DEVELOPMENT_STATUS, B-notifications channel table | S |

About **4-5 days** of work for items 1-3 on Android, tested with `:shared:testAndroidHostTest` and
`:androidApp:assembleDebug`, then on the Oppo (items 1-2), a Pixel API 36.1+ emulator (item 3) and an API 37 emulator
(MetricStyle).

---

## 5. Risks and unknowns

| Unknown | Why it matters | How to settle it |
|---|---|---|
| ColorOS 15 lock-screen filtering of LOW / ongoing notifications | Decides DEFAULT-silent vs LOW for `nixura_today` | Device check below |
| Does ColorOS 15 keep an ongoing notification after the app is swiped from recents? | If not, "Today" disappears until the next data change | Device check below |
| Non-promoted MetricStyle on the AOD | Nice-to-have only | API 37 emulator |
| ColorOS 16 and third-party lock-screen widgets | Other Oppo users, not the test phone | Check on a ColorOS 16 device if one becomes available |
| The Reno 10 5G update list may change | Would bring Live Updates / Fluid Cloud | Re-check OPPO's ColorOS 16 page before building |

## 6. Device checks for the Oppo (before building)

Connect the phone, then:

```powershell
adb shell settings get secure lock_screen_show_notifications
```

```powershell
adb shell settings get secure lock_screen_allow_private_notifications
```

```powershell
adb shell settings get secure lock_screen_show_silent_notifications
```

1. `1` for the second means "show all content": then today's quit milestone shows in full on the lock screen (the
   privacy finding). Confirm by triggering a milestone and locking the phone.
2. Post a LOW-importance notification from the Coach channel (daily check-in), lock the phone, see whether it shows.
3. After building item 2: swipe NixUra away from recents and check the Today notification is still there; reboot and
   check it is re-posted on boot.

---

## Sources

Official Android:
- [notif-build](https://developer.android.com/develop/ui/views/notifications/build-notification): lock-screen visibility, `setPublicVersion`
- [a14](https://developer.android.com/about/versions/14/behavior-changes-all): dismissible ongoing notifications
- [live](https://developer.android.com/develop/ui/views/notifications/live-update): Live Updates requirements and use cases
- [progress](https://developer.android.com/develop/ui/views/notifications/progress-centric): `ProgressStyle`, Android 16
- [metric](https://developer.android.com/develop/ui/views/notifications/metric-style): `MetricStyle`, Android 17
- [awpi](https://developer.android.com/reference/android/appwidget/AppWidgetProviderInfo): `WIDGET_CATEGORY_KEYGUARD`, `WIDGET_CATEGORY_NOT_KEYGUARD` API 36
- [glance](https://developer.android.com/develop/ui/compose/glance/create-app-widget): `widgetCategory` home_screen / keyguard
- [fgs](https://developer.android.com/develop/background-work/services/fgs/service-types): FGS types, `specialUse` Play review
- [qs](https://developer.android.com/develop/ui/views/quicksettings-tiles): Quick Settings tiles

Other:
- [ac-lsw](https://www.androidcentral.com/apps-software/google-is-bringing-widgets-to-phone-lock-screens-with-android-16-qpr1): Google's statement on lock-screen widgets in AOSP; news site, no official developer page found
- OPPO ColorOS 16 eligible devices: https://www.oppo.com/en/coloros16/
- [wk-acc](https://developer.apple.com/documentation/widgetkit/creating-accessory-widgets-and-watch-complications)
- [activitykit](https://developer.apple.com/documentation/activitykit)

[notif-build]: https://developer.android.com/develop/ui/views/notifications/build-notification
[a14]: https://developer.android.com/about/versions/14/behavior-changes-all
[live]: https://developer.android.com/develop/ui/views/notifications/live-update
[progress]: https://developer.android.com/develop/ui/views/notifications/progress-centric
[metric]: https://developer.android.com/develop/ui/views/notifications/metric-style
[awpi]: https://developer.android.com/reference/android/appwidget/AppWidgetProviderInfo
[glance]: https://developer.android.com/develop/ui/compose/glance/create-app-widget
[fgs]: https://developer.android.com/develop/background-work/services/fgs/service-types
[qs]: https://developer.android.com/develop/ui/views/quicksettings-tiles
[ac-lsw]: https://www.androidcentral.com/apps-software/google-is-bringing-widgets-to-phone-lock-screens-with-android-16-qpr1
[wk-acc]: https://developer.apple.com/documentation/widgetkit/creating-accessory-widgets-and-watch-complications
[activitykit]: https://developer.apple.com/documentation/activitykit
