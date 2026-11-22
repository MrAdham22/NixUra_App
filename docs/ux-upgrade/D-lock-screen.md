# UX phase D: lock screen and persistent presence (research)

**Written:** 2026-11-22. **Branch:** `docs/lock-screen-research`. **Research only: no app code changed.**
(PLAN.md called this file `D.md`; the owner's brief named it `D-lock-screen.md`.)

**Owner decisions (2026-11-22), none built yet:**
1. The opt-in "Today" notification is **approved**, as an exception to PLAN.md's no-summary rule.
2. Event and coach notifications **follow the phone's lock-screen setting**.
3. The user can **hide a bad habit's name everywhere it shows outside the app** (notifications, widgets, anything
   else), with all other details unchanged (section 4.1).
4. Quit habits' notifications **follow the phone's lock-screen setting** too. This replaces PLAN.md's earlier rule
   "hidden on the lock screen by default"; hiding a name is the user's choice (decision 3).
5. The quit-counter widget **may be placed on the lock screen**, like the other two widgets. The user places it;
   "hide name" applies there too.

**No decisions are open.**

**Question.** Can NixUra show habit progress, the next reminder, the nearest event or a streak on the lock screen,
truthfully, on NixUra's SDK range and on the owner's Oppo Reno 10 5G? What does each mechanism cost, and which one
should be built?

**Ground rules from the brief.** Nothing fake (no "pinned forever" claims Android does not honour), the app is not
released so nothing has to stay compatible, recommend the best design rather than the safest change.

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

### Privacy finding

How Android decides, read in the AOSP source (stock Android; ColorOS not checked):

| What the app sets | Phone set to "show all content" | Phone set to "hide sensitive content" |
|---|---|---|
| `VISIBILITY_PUBLIC` | full content | full content |
| `VISIBILITY_PRIVATE` (today's default) | **full content** | header only (app name, time): no title, no text |
| `VISIBILITY_PRIVATE` + `setPublicVersion` | **full content: the public version is not used** | the public version |
| `VISIBILITY_SECRET` | not shown | not shown |

- The [guide][notif-build] says private still shows the content title. The platform code does not:
  `Notification.Builder.makePublicContentView` drops title and text when there is no public version ([source][src-notif]).
- Redaction of a private notification depends on the user's setting ([source][src-redact]), so a public version
  **cannot** guarantee that a name is hidden. Only `VISIBILITY_SECRET`, or text with no private detail, can
  ([source][src-hide]).
- An app cannot set a lock-screen ceiling on its channels: `setLockscreenVisibility` is "Only modifiable by the
  system and notification ranker", and Android resets the app's value ([source][src-prefs]).

So today, on a phone set to show all content, the quit-milestone notification ("7 days clear: Smoking") shows in
full. The Oppo is set to hide sensitive content, and there the details were hidden (tested 2026-11-22, section 6).
The owner accepted this on 2026-11-22 (decision 4): what is
missing is the option to hide the name (4.1) and a neutral public version for phones that hide sensitive content.

---

## 2. The mechanisms compared

Effort is **on top of phase B/C** (S = under a day, M = 2-3 days, L = a week or more).

### 2.1 Normal notifications with lock-screen visibility (`setVisibility` / `setPublicVersion`)

| | |
|---|---|
| **What it shows** | Any NixUra notification that is in the shade when the phone is locked: full content (`VISIBILITY_PUBLIC`), full content or a substitute depending on the phone's setting (`VISIBILITY_PRIVATE` + `setPublicVersion`), or nothing (`VISIBILITY_SECRET`). Set per notification; apps cannot set it per channel (see the privacy finding). |
| **Android versions** | API 21+, so the whole range (24-37). |
| **ColorOS 15 (Oppo)** | Standard Android setting; ColorOS also has its own lock-screen notification switches. Needs a device check (section 6). |
| **Limits** | The user always wins: "the user always has ultimate control over whether their notifications are visible on the lock screen" ([docs][notif-build]). Only notifications that exist are shown; nothing here is permanent. |
| **Battery** | None beyond posting. |
| **Annoyance** | None added; it only changes what an existing notification reveals. |
| **Play policy** | Nothing to declare. |
| **Effort** | **S.** A pure `LockScreenPrivacy` rule in `domain/notification` (kind → PUBLIC / PRIVATE-with-public-version), applied in `NixNotifications.post`; the public version's text in `Strings` + `ArabicStrings`; unit tests. |

### 2.2 An ongoing "Today" notification (`setOngoing(true)`, no foreground service)

| | |
|---|---|
| **What it shows** | One quiet, always-current line or two: "💧 3 of 8 · 🕌 2 of 5 · Next: Asr 15:12 · Event in 2 days". Can carry a ticking countdown to the next reminder (`setUsesChronometer` + `setChronometerCountDown`, drawn by the system, no wake-ups) and the existing `+1 glass` / `Prayed ✓` buttons. Streaks of quit habits only if the user opts in. |
| **Android versions** | API 24+ (countdown chronometer is API 24). Whole range. |
| **Is it really persistent?** | **No, and the doc must not pretend it is.** Since Android 14, `setOngoing(true)` notifications **can be swiped away**; they stay non-dismissable only "when the phone is locked" and against **Clear all** ([Android 14 behaviour changes][a14]). The phone reboots and ColorOS force-stops can also clear it. So: it stays on the lock screen while the phone is locked, the user can remove it when unlocked, and NixUra must respect that (re-post only the next day or when the user turns it back on). |
| **ColorOS 15 (Oppo)** | A posted notification lives in the system, not in our process, so on stock Android killing the process does not remove it (a force-stop does). Not tested on ColorOS. Lock-screen filtering of **silent** notifications differs by OEM (Pixel has "hide silent notifications"); to be safe the channel should be DEFAULT importance with no sound and no vibration rather than LOW. Needs a device check. |
| **Battery** | Near zero if it is **event-driven**: re-post only where the app already writes (app log, widget +/-, notification button, reminder delivered, event change), plus one inexact wake-up at local midnight for the day roll (the widgets already have a refresh alarm and `ReminderRefreshWorker` exists). No polling, no service. |
| **Annoyance** | One row in the shade and an icon in the status bar, all day. This is the real cost: some people will hate it, hence **opt-in only**, with an easy "Hide for today" button. |
| **Play policy** | A plain ongoing notification needs no declaration. **A foreground service to keep it alive would very likely be rejected** (our reading of the docs): none of the FGS types fits a passive status display; `health` needs body sensors or activity recognition; `specialUse` is reviewed in Play Console ([FGS types][fgs]). So: no service, ever. Play's spam rules also mean no ads or promos in it. |
| **Effort** | **M.** Pure `TodaySummary` builder in `domain` (picks what fits: good-habit counts, next planned reminder from `HabitReminderPlanner`, timing only, never the name, of the nearest event from `NearestEventSelector`, quit streaks only with opt-in), a new channel `nixura_today` ("Today on lock screen" / "اليوم على شاشة القفل"), a Settings switch, re-post hooks next to the existing `WidgetUpdater.requestRefresh` calls, delete-intent handling, EN + AR strings, tests. |

### 2.3 Live Updates (promoted ongoing notifications, `ProgressStyle`)

| | |
|---|---|
| **What it shows** | A notification promoted to the top of the shade and the lock screen and shown "as a chip in the status bar" ([Live Updates][live]); `ProgressStyle` adds a segmented progress bar with points and a tracker icon ([ProgressStyle][progress]). |
| **Android versions** | Android 16 (API 36) and newer. Needs the normal permission `POST_PROMOTED_NOTIFICATIONS`, `setRequestPromotedOngoing(true)`, ongoing, a title, no custom views, not colorized, not a group summary, channel not `IMPORTANCE_MIN`; check `canPostPromotedNotifications()` ([Live Updates][live]). |
| **ColorOS** | ColorOS 16 (Android 16) is reported by the press to show third-party Live Updates in Fluid Cloud; OPPO's page mentions rides, scores and timers but does not say third-party apps are supported. **The Reno 10 5G is not on the ColorOS 16 list**, so on the owner's phone this does nothing. No public Fluid Cloud API for older ColorOS was found. |
| **Allowed uses** | Google lists the acceptable cases: "Active navigation, ongoing phone calls, active rideshare tracking, and active food delivery tracking", and the unacceptable ones: "Ads, promotions, chat messages, alerts, upcoming calendar events, and quick access to app features" ([Live Updates][live]). `ProgressStyle` is for "user-initiated start-to-end journeys" ([ProgressStyle][progress]). |
| **Fit for NixUra** | **Poor.** An all-day habit summary is not a user-started journey with an end; the event countdown is literally "upcoming calendar events". It would only fit a future feature the user starts and that ends, e.g. a 10-minute "ride out the craving" timer. Users can demote a Live Update, and "posting unwanted Live Updates might cause users to revoke an app's posting permission" ([Live Updates][live]). |
| **Battery / annoyance** | Same as 2.2, plus a status-bar chip: more visible, more annoying when misused. |
| **Play policy** | No Play declaration is documented for `POST_PROMOTED_NOTIFICATIONS`. The risk is breaking Google's stated criteria (the system or a later policy may demote or block), not a form. Not worth it for a use Google lists as unacceptable. |
| **Effort** | S-M on top of 2.2, but **not recommended** now. |

### 2.4 `MetricStyle` (Android 17)

| | |
|---|---|
| **What it shows** | Up to 3 metrics, each a label, a value and an optional unit; values can be fixed numbers/text/dates/times or a time difference. Shown in the shade, as a Live Update when promoted, and on the always-on display ([MetricStyle][metric]). |
| **Android versions** | Android 17 (API 37) only. NixUra's `compileSdk`/`targetSdk` is already 37 and the class is in the installed android-37.0 SDK (not compiled in the app yet). |
| **Fit for NixUra** | **Good, and it is a real native template:** Google names "health and fitness apps, timers" as its use cases ([MetricStyle][metric]). "Water 3/8 · Prayers 2/5 · Next 15:12" is exactly three metrics. Use it as the **look** of the Today notification (2.2) on Android 17, **without** requesting promotion. The docs do not say whether a non-promoted MetricStyle reaches the AOD; treat that as unknown until seen on an Android 17 emulator. |
| **ColorOS** | Needs Android 17; not on the owner's phone. |
| **Battery / annoyance / Play** | Same as 2.2. |
| **Effort** | **S** after 2.2 (an API 37 branch in `NixNotifications` that builds with the platform `Notification.Builder`; the docs do not mention a `NotificationCompat` version). Verify on an API 37 emulator. |

### 2.5 Lock-screen widgets

| | |
|---|---|
| **What it shows** | The existing widgets (good-habit quick log with +/-, events countdown, quit counter) placed by the user on the lock screen. |
| **Android versions** | `WIDGET_CATEGORY_KEYGUARD` exists since API 17 but phones dropped lock-screen widgets in Android 5.0. They came back in AOSP after Android 16 QPR1 (Google: "Lock screen widgets will be available in AOSP for tablets and mobile starting with the release after Android 16 (QPR1)", via [Android Central][ac-lsw]); Pixel phones got them in Android 16 QPR2. API 36 added `WIDGET_CATEGORY_NOT_KEYGUARD` so a widget can refuse the lock screen ([AppWidgetProviderInfo][awpi]); the Glance docs describe `widgetCategory` "home_screen" / "keyguard" ([Glance widget][glance]), though that page still carries the older note that from Android 5.0 only `home_screen` is valid. Whether a phone offers it is up to the maker. |
| **ColorOS** | **Not on ColorOS 15** (the owner's phone). ColorOS 16 support for third-party lock-screen widgets is not documented by OPPO's page; unknown. |
| **Is it permanent?** | Yes, in the honest sense: the user places it and it stays until they remove it. It is the only mechanism here that is truly "always on the lock screen". |
| **Battery** | Same as today's home-screen widgets (system `Chronometer`, one inexact refresh alarm). |
| **Annoyance** | None: the user chose it. |
| **Privacy** | The good-habit +/- buttons may work without unlocking (not tested); logging a glass of water from a locked phone is harmless. The **quit** widget is allowed too (owner decision 5): the user places it, and "hide name" (4.1) masks it there. A widget does not follow the phone's notification setting. |
| **Play policy** | Nothing to declare. |
| **Effort** | **XS-S.** Set `widgetCategory="home_screen\|keyguard"` in all three `widget_*_info.xml`; check the layout planner at lock-screen sizes on a Pixel API 36.1+ emulator. |

### 2.6 Other native mechanisms

| Mechanism | What it would show | Verdict |
|---|---|---|
| **Quick Settings tile** (`TileService`, API 24+; add prompt `requestAddTileService` on Android 13+) | "💧 3 of 8" with a tap to add a glass, reachable from the lock screen's pull-down; `isSecure()` / `unlockAndRun()` decide what needs unlocking ([QS tiles][qs]) | Good **later** add-on for water only. Google: "avoid using tiles that display information, but aren't interactive", max two per app, so it must be a +1 button, not a dashboard. Not a lock-screen display in itself. Effort S. |
| **Alarm clock "next alarm"** (`setAlarmClock`) | The clock / lock screen shows the next alarm time | Already used by phase B for events and prayer within 75 min of the reminder. **Do not** extend it to show habit info: it would claim an alarm that is not an alarm, and Play's exact-alarm rules watch this. |
| **Live wallpaper** (`WallpaperService`) | A streak drawn into the wallpaper | Rejected: replaces the user's wallpaper, ColorOS may handle lock-screen wallpapers separately (not checked), costs more battery, and feels like a trick. |
| **At a Glance / Smartspace, ColorOS AOD panels** | | No public API for third-party apps was found. What the ColorOS AOD shows of notifications was not checked. |
| **Foreground service** | A notification that cannot be swiped on old Android | Rejected (no valid FGS type, Play review; section 2.2). |

### 2.7 iOS (for parity; nothing can be built or run on Windows)

| Mechanism | Fit |
|---|---|
| **Lock-screen widgets** (WidgetKit accessory families, iOS 16+) | **The iOS equivalent of 2.5, and better supported than on Android.** Comes with the already-planned iOS widget work (WidgetKit extension + App Group). |
| **Live Activities** (ActivityKit, iOS 16.1+) | Apple limits them to activities with a start and end (they end after at most 8 hours, and can stay on the lock screen a few hours more). Same verdict as Live Updates: only for a future user-started timer, not an all-day summary. Needs a Swift widget extension. |
| **Notifications** (`UNUserNotificationCenter`) | Phase B's iOS scheduler is still a no-op. Lock-screen previews are the user's setting on iOS; a category's hidden-preview placeholder applies only when the user hides previews, so quit notifications need that placeholder, plus the masked text of 4.1 when the user hid the name. |

Apple references: [WidgetKit, lock screen accessories][wk-acc], [ActivityKit][activitykit]. Unverified on this machine.

---

## 3. Summary table

| Mechanism | Android | Oppo Reno 10 5G (Android 15) | Really persistent? | Battery | Annoyance | Play risk | Effort |
|---|---|---|---|---|---|---|---|
| Visibility / public version | 21+ | expected, untested | no (only what's posted) | none | none | none | S |
| Ongoing "Today" notification | 24+ | expected, untested | while locked; swipeable when unlocked (14+) | ~0 (event-driven) | medium (opt-in) | none if no FGS | M |
| Live Update / ProgressStyle | 36+ | no | while promoted; user can demote | ~0 | high if misused | use case not allowed | S-M |
| MetricStyle | 37+ | no | as the notification | ~0 | low | none (not promoted) | S after "Today" |
| Lock-screen widget | Pixel 16 QPR2+, OEM-dependent | no | yes, user-placed | low (existing) | none | none | XS-S |
| QS tile (+1 glass) | 24+ | expected, untested | user-placed, in pull-down | ~0 | none | none | S |
| FGS / alarm-clock abuse / wallpaper | | | | | | rejected | |

---

## 4. Recommendation: an opt-in "Today" presence

Build these three together, plus 4.1 (the owner's hide-name option). They are the best honest lock-screen presence
Android allows, and the first two should work on the owner's Android 15 phone (untested on ColorOS).

1. **Lock-screen privacy rules (do first, S).** Per type, by default:

   | Type | Visibility | What a stranger sees |
   |---|---|---|
   | Good habit (water, prayer, other) | PUBLIC | full content |
   | Quit milestone | PRIVATE + neutral public version | follows the phone's setting: the milestone (without the name if hidden, 4.1), or "A milestone reached" / "وصلت إلى محطة جديدة" |
   | Today (item 2) | PUBLIC | its own content, written to be safe: never an event name or a quit-habit name |
   | App / account | PRIVATE | follows the phone's setting |
   | Event | PRIVATE + neutral public version | follows the phone's setting: the name, or "Event in 2 hours" |
   | Coach | PRIVATE + neutral public version | follows the phone's setting: the note, or "A note from your coach" |

   One opt-in, **"Show quit streaks on Today"** (default **off**): adds a streak with no habit name to the Today
   notification. It is separate from "hide name" (4.1): Today never shows a quit habit's name. The rule is a pure,
   tested function in `domain/notification`.

2. **"Today" notification, opt-in (M).** One quiet ongoing notification on a new DEFAULT-importance, soundless,
   vibration-less channel `nixura_today`, with: good-habit progress for today, the next planned reminder with a
   system-drawn countdown, the nearest event's timing ("Event in 2 days", never its name), and the `+1 glass` /
   `Prayed ✓` button for whatever is next. Re-posted only when data changes and once at midnight. If the user swipes it away, it stays away until tomorrow; a
   "Hide today" button does the same. Offered in context (when the user adds a second good habit, or from the habit
   sheet: "Show today's progress on the lock screen"), never at first launch. On Android 17 it uses **MetricStyle**
   (three metrics), not promoted. Copy in EN + AR, digits through the digit setting.

3. **Lock-screen widgets where they exist (XS).** `home_screen|keyguard` on all three widgets, the quit counter
   included. No effect on ColorOS 15; real on Pixel-style Android 16 QPR2+ phones.

**Not now:** Live Update promotion (Google lists our use as unacceptable; revisit only if a user-started timer such
as "ride out a craving" is built, which would be a real start-to-end journey), foreground services, alarm-clock
tricks, wallpaper. **Later, optional:** a water QS tile.

### Owner decisions

**Decided 2026-11-22: "Today" is approved.** PLAN.md said notifications never duplicate the widget's dashboard
("no 'today's summary' notifications"). Today is the one exception: opt-in, silent, never alerts, one row.

**Decided 2026-11-22: event and coach notifications follow the phone's setting** (PRIVATE + neutral public version).
A phone set to show all content shows the event's name; a phone set to hide sensitive content shows "Event in 2
hours" / "A note from your coach". (The alternative, always hiding them, could make reminders easy to miss.)

**Decided 2026-11-22: the user can hide a bad habit's name outside the app.** Design in 4.1.

**Decided 2026-11-22: quit habits follow the phone's setting**, replacing PLAN.md's "hidden on the lock screen by
default". (The alternative was to keep milestones off the lock screen and never name a quit habit in the check-in.)

| Phone's lock-screen setting | Milestone | Daily check-in | With "hide name" on (4.1) |
|---|---|---|---|
| Show all content | full text, with the name | full text, can name a quit habit | the same, without the name |
| Hide sensitive content | "A milestone reached" | "A note from your coach" | the same |
| No notifications on the lock screen | nothing | nothing | nothing |

Both use quiet (LOW) channels, so a phone set to hide silent notifications on the lock screen shows neither.
Consequence: on a show-all phone a quit habit's name is visible until the user turns on "hide name" for it.

**Decided 2026-11-22: the quit-counter widget may go on the lock screen.** It appears there only if the user places
it, and "hide name" masks it there too. (The alternative was `not_keyguard`, home screen only.)

### 4.1 Hide a bad habit's name outside the app

**The option.** One switch per bad habit, **"Hide name outside the app"** / "إخفاء الاسم خارج التطبيق", off by
default, in the add/edit form and in the card's long-press sheet. Inside the app nothing changes. Everything else
stays: days, live clock, longest streak, money saved, colour, buttons, tap target.

**Removing the name alone is not enough.** Two other things identify the habit today: the milestone's body comes
from the habit's category ("Nicotine level dropping", "A day gambling-free") and the widget's badge is the category's
icon (a cigarette for smoking). Both become neutral when the name is hidden.

| Surface | Today | With the name hidden |
|---|---|---|
| Milestone title | "7 days clear: Smoking" | "7 days clear" / "صامد منذ 7 أيام" |
| Milestone body | category text that can name the habit | the existing generic line: "You reached 7 days. Keep going..." |
| Daily check-in | "Smoking: 7 days clear. (encouragement)" | "7 days clear. (encouragement)"; the encouragement is already generic |
| Quit widget, name | "Smoking" | no name: the existing "Clear for" / "صامد منذ" label takes its place, once |
| Quit widget, badge | category icon | the neutral shield (`HabitTopic.OTHER_QUIT`) |
| Today notification | never names a quit habit | the same |
| Recent-apps preview | a picture of the open screen, names included | Android 13+: no preview while any habit has a hidden name (`Activity.setRecentsScreenshotEnabled(false)`) |
| iOS notifications, widgets, app switcher | not built | the same masked text; cover the screen when the app goes inactive (unverified, needs a Mac) |

**Already safe:** the channel and bundle titles ("Quit progress"), app shortcuts and the widget picker's preview are
fixed text or pictures. Watches and other devices that mirror notifications receive the masked text.

**Limits to tell the user about, not to hide:**
- The broken-chain icon and "Quit progress" still say that *a* habit is being quit, not which one.
- Android's own notification history keeps notifications that were shown before the switch was turned on.
- On Android 12 and older the recent-apps preview can only be blocked with `FLAG_SECURE`, which also blocks
  screenshots, so it is not used.
- The widget's "choose a habit" screen shows real names: it is NixUra's own screen, and the user needs them to choose.
- Several hidden habits look alike on widgets; their colours and day counts tell them apart.
- It hides what is **displayed**. The name still syncs to the user's account, and the AI coach still sends habit names
  for users who agreed to it (`AiCoachClient`).

**How to build it (about 1.5-2 days):**
1. `Habit.hideNameOutside`: Room v10 (`MIGRATION_9_10`, one column on `habits`, default 0), mapper, DAO, schema check.
2. Sync it with the habit (`HabitPayload`, default false), so a second phone does not expose the name. The Firestore
   rules accept it as they are (`data` allows up to 24 fields).
3. One pure rule in `domain` applied at the two exits, `NotificationPlanner` and `WidgetStateMapper`, so renderers
   and receivers never get the real name. Match the category before masking, never from the masked name.
4. On a change: re-plan (stored plans and armed alarms carry the old text), remove that habit's posted milestone and
   any posted check-in, redraw its widgets at once.
5. Tests: for a hidden habit no planned notification or widget state contains the name, a category milestone or a
   category icon, in English and Arabic.

### What it would take

| Step | Files (planned) | Size |
|---|---|---|
| Privacy rule + quit-streak opt-in + public versions | `domain/notification/LockScreenPrivacy.kt` (+ test), `NixNotifications.kt`, Settings screen + ViewModel, `Strings` / `ArabicStrings` | S |
| Today builder | `domain/notification/TodaySummary.kt` (+ tests: no quit info without opt-in, nearest event, next reminder, Arabic plurals) | S-M |
| Today channel and posting | `NotificationChannels.kt` (new `nixura_today`, freeze list before release), `NixNotifications.kt` (ongoing, chronometer, actions, delete intent), re-post hooks beside `WidgetUpdater.requestRefresh`, midnight refresh | M |
| MetricStyle branch (API 37) | `NixNotifications.kt` | S |
| Widget categories | `res/xml/widget_*_info.xml` | XS |
| Hide a bad habit's name outside the app (4.1) | `Habit`, Room v10, sync payload, form + sheet, `NotificationPlanner`, `WidgetStateMapper`, `WidgetRenderer`, `MainActivity`, strings | M (1.5-2 days) |
| iOS | lock-screen accessory widgets with the planned WidgetKit work; hidden-preview placeholder and masked names (4.1) in the iOS notification scheduler | unverified (Mac) |
| Docs | README features, PROJECT_CONTEXT, DEVELOPMENT_STATUS, B-notifications channel table | S |

About **6-7 days** on Android (4-5 for items 1-3, 1.5-2 for hiding names), tested with `:shared:testAndroidHostTest` and
`:androidApp:assembleDebug`, then on the Oppo (items 1-2 and hidden names), a Pixel API 36.1+ emulator (item 3) and an API 37 emulator
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

## 6. Device checks for the Oppo

**Results, 2026-11-22 (Oppo Reno 10 5G, Android 15, ColorOS V15.0.0):**

| Check | Result |
|---|---|
| `lock_screen_show_notifications` | `1`: notifications show on the lock screen |
| `lock_screen_allow_private_notifications` | `0`: sensitive content is hidden |
| `lock_screen_show_silent_notifications` | not set |
| Daily check-in at 16:55, phone locked (owner's test) | **passed:** posted at 16:55:00, showed on the lock screen with its details hidden |
| Channel importance | Quit progress, Coach and App are at DEFAULT on this phone although created LOW (`mOriginalImp=2`, importance locked as a settings change). So this phone **cannot** show how silent channels behave: test that on a clean install. |
| "Show all content", a milestone, an ongoing notification | not tested |

To repeat the checks, connect the phone, then:

```powershell
adb shell settings get secure lock_screen_show_notifications
```

```powershell
adb shell settings get secure lock_screen_allow_private_notifications
```

```powershell
adb shell settings get secure lock_screen_show_silent_notifications
```

1. `1` for the second means "show all content": then today's quit milestone should show in full on the lock
   screen (the privacy finding). Confirm by triggering a milestone and locking the phone.
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
- [recents](https://developer.android.com/reference/android/app/Activity#setRecentsScreenshotEnabled(boolean)): hiding the recent-apps preview, API 33

Android 15 source:
- [src-notif](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/core/java/android/app/Notification.java): the default redacted view (`makePublicContentView`)
- [src-redact](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/packages/SystemUI/src/com/android/systemui/statusbar/NotificationLockscreenUserManagerImpl.java): when redaction applies (`needsRedaction`)
- [src-hide](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/packages/SystemUI/src/com/android/systemui/statusbar/notification/interruption/KeyguardNotificationVisibilityProvider.kt): what is hidden on the lock screen
- [src-prefs](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/services/core/java/com/android/server/notification/PreferencesHelper.java): fields apps cannot set on a channel

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
[src-notif]: https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/core/java/android/app/Notification.java
[src-redact]: https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/packages/SystemUI/src/com/android/systemui/statusbar/NotificationLockscreenUserManagerImpl.java
[src-hide]: https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/packages/SystemUI/src/com/android/systemui/statusbar/notification/interruption/KeyguardNotificationVisibilityProvider.kt
[src-prefs]: https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/services/core/java/com/android/server/notification/PreferencesHelper.java
