# A1: widget "+" tap latency

**Date:** 2026-11-21. **Branch:** `perf/widget-tap-latency`. **Device:** OPPO Reno 10 5G (CPH2531), Android 15,
ColorOS 15, debug build, USB connected. Also the Pixel 9 emulator (Android 16) for a first look.

**Problem (owner):** tapping "+" on a widget (e.g. Prayer) sometimes showed no change for 2–4 s, so the owner tapped
again and then both applied. **Decision:** every tap counts. The fix is instant feedback and reliable processing,
not blocking taps.

## How it was measured

Temporary timing marks (one logcat tag, `NixUraWidgetPerf`, `SystemClock.elapsedRealtime()` + thread name) at: process
start (`Process.getStartElapsedRealtime`), each step of `Application.onCreate`, tap received in
`WidgetActionReceiver.onReceive`, coroutine start on `Dispatchers.Default`, use-case lock, DB read and write, widget
update requested, each widget rebuilt + `updateAppWidget`, and the data observer's refresh. Taps were sent with
`adb shell input tap` on the real widget, with `/proc/uptime` (the same clock) read just before and after, so the
gap between the touch and our first line is visible. System lines naming the package (`ActivityManager`,
`OplusHansManager`, `AppWidgetServiceImpl`) were captured from the same logcat. The marks were removed after the
"after" run (they are not in the PR).

To measure again: add `Log.i("NixUraWidgetPerf", ...)` at the same points, then

```
adb logcat -c
adb logcat -v time | Select-String -Pattern "NixUraWidgetPerf|nixura"
```

Tests on the owner's real Prayer habit were done in pairs ("−" then "+") so the data ended exactly as it started
(4 / 5 today, 20 times in total).

## Where the time went (before)

Path of one tap: launcher → `PendingIntent` broadcast → `WidgetActionReceiver.onReceive` (main thread, `goAsync`) →
`WidgetUpdater` scope (`Dispatchers.Default`) → `IncrementGoodHabitTodayUseCase` (process-wide `Mutex`) → Room read +
write → `WidgetUpdater.refresh` → render + `updateAppWidget` for **every** NixUra widget, under one lock → 500 ms
later the DB observer re-rendered **every** widget again.

- **No network in the tap path.** Firestore sync runs only from the app UI (`SyncCoordinator`, 3 s debounce after a
  change) and never blocks a widget write. The widget path never touches the network.
- **Warm process (the app was used recently):** touch → `onReceive` ≈ 10 ms (launcher + broadcast, fine).
  Write 10–18 ms (it loaded all habits with their full log history to read one number). Prayer card drawing 55–80 ms
  (painted bitmaps). **Touch → new number ≈ 80–100 ms.** Then every other widget was redrawn too (quit counter
  35 ms), and 500 ms later the observer redrew all of them again (238–272 ms), holding the lock a next tap needs.
- **Cold process (app killed, e.g. by ColorOS after a while):** **touch → new number ≈ 1,120 ms**:
  - ≈ 90 ms touch → `Start proc ... for broadcast` (launcher + ActivityManager);
  - ≈ 575 ms process start → `Application.onCreate` (Android/ColorOS: fork, `createClassLoader` alone 386 ms on this
    debug APK with 25+ dex files, ColorOS hooks);
  - 162 ms `Application.onCreate`, of which **122 ms Facebook SDK** (only needed for Facebook sign-in);
  - 33 ms first Koin resolution, ≈ 100 ms first DB access (Room open + SQLite native library), 147 ms first Prayer
    card render.
- **ColorOS:** `OplusHansManager` keeps NixUra out of its freeze state while it hosts a widget
  ("cannot transition from R to M, importance=widget"). After 4.5 min of idle with the screen on, the process was
  still alive and not frozen (`isPendingFreeze=true, isFrozen=false`) and a tap took 110 ms. `am freeze --sticky` did
  not freeze it either (ColorOS kept it unfrozen), so the "frozen" case could not be forced through adb.
- **The 2–4 s was not reproduced through adb.** The slowest measured case is the cold start (≈ 1.1 s on a debug build
  with the phone awake and idle). The likely real-world 2–4 s is that cold start right after unlocking (CPU busy with
  the launcher and other apps waking, low clocks, the debug APK's class loading), with nothing on screen to show the
  tap was received, plus a second tap queued behind the old double full redraw. Screen-off for a long time could not
  be tested by an agent: the lock screen needs the owner's PIN. See "Owner test on the phone" below.

## What changed

1. **Instant acknowledgement, no process needed:** the "+" / "−" buttons of every quick-log layout now have a ripple
   foreground (`res/drawable/widget_btn_ripple.xml`). The **launcher draws it on touch-down**, before NixUra's
   process exists, so a cold tap is acknowledged at once. The number itself changes only after the write, so the
   shown value always matches what was saved (no optimistic guess).
2. **Cheap write:** `GoodHabitRepository.dayCountOf(id, day)` (Room: `activeById` + `countOn`, both index lookups)
   replaces loading every habit's full history. Same use case, same lock, same clamping: no duplicated logic.
3. **Redraw only what changed:**
   - a tap redraws the touched widget first, then only other quick-log widgets bound to the same habit
     (`WidgetUpdater.redrawAfterTap`); the quit counter and events widgets are not touched;
   - `ObserveWidgetDataChangesUseCase` now emits *which* widget kinds changed (equal Room re-emissions are ignored);
     the observer redraws only those, and `WidgetRenderer` skips a quick-log widget whose state, appearance and size
     equal what it last drew. So the observer's pass after a tap costs ≈ 35 ms and paints nothing;
   - system events (boot, app update, time change, the alarm, launcher `onUpdate`) still redraw everything.
4. **Coalesced redraws, every tap still counts:** `domain/widget/RefreshCoalescer` merges redraws asked while one is
   running into one more pass (latest DB state wins). Writes are never merged: each tap is its own write, serialized
   by the use case's `Mutex`. A redraw request only returns after a pass that **started after** it, so the picture is
   never older than the caller's write.
5. **Cheaper drawing:** the small prayer pieces (stars, beads, arch badge) are painted once and cached (`LruCache`);
   the same bitmap is also sent once per RemoteViews.
6. **Lighter cold start:** the Facebook SDK starts in `MainActivity.onCreate` instead of `Application.onCreate`, so
   widget taps, reminder alarms and boot no longer pay ≈ 120 ms for it. Opening the app behaves as before.
7. **Alarm stays correct after a partial redraw:** the next-refresh alarm is only re-armed when every widget's next
   change instant is known in this process; right after a cold start the alarm armed by the last full refresh stays.

## Before / after (Oppo Reno 10 5G, debug build)

"Tap → number" = from the injected touch (or `onReceive` for warm taps, where touch → `onReceive` ≈ 10 ms) to
`updateAppWidget` of the touched widget returning.

| Case | Before | After |
|---|---|---|
| Warm, one tap: tap → new number | 78–98 ms | 77–88 ms |
| Warm, two quick taps (≈ 330 ms apart) | 96 / 90 ms, +2 saved | 68 / 70 ms and 101 / 110 ms, +2 saved |
| Burst of 3 taps each way (≈ 115 ms apart) | not run | 60–88 ms each, count exact (4 → 1 → 4) |
| Cold (process killed): tap → new number | ≈ 1,120 ms | ≈ 980 ms |
| Cold: `Application.onCreate` | 162 ms | 53 ms |
| Cold: process start → `onCreate` (Android/ColorOS, APK size) | 575 ms | 584 ms (not ours to change, see below) |
| Widgets redrawn by one tap (2 on the home screen) | 2, then 2 again by the observer | 1; observer pass skips it |
| Observer pass 500 ms after a tap (holds the lock) | 238–272 ms of drawing | 31–37 ms, nothing drawn |
| DB read in the tap (4 habits, 13 log rows) | 12–18 ms warm, grows with history | 6–21 ms warm, two index lookups |
| Visible acknowledgement before the process runs | none | ripple on touch-down (launcher) |
| Idle 4.5 min (screen on), one tap | 110 ms | not re-run (same warm path) |

Emulator (Pixel 9, before only): warm 45–70 ms to the touched widget; cold 860 ms process start + 250 ms.

## What NixUra does not control

- Process start and class loading (≈ 580 ms here) belong to Android/ColorOS and the APK. A **release build** (R8
  shrinking, fewer dex files) and a **Baseline Profile** should cut class loading a lot; not measured (installing a
  differently signed release build would wipe the owner's data). Worth measuring before the first release.
- Whether ColorOS kills NixUra in the background. Users can set NixUra to "Allow background activity" / no battery
  restriction; the Help screen (phase A2) should say so.

## Tests

- `domain/usecase/WidgetTapConcurrencyTest`: real parallel taps on `Dispatchers.Default` against a repository that
  suspends between read and write: 2 quick taps = +2; 50 parallel taps = +50 with exactly 50 writes and results 1..50;
  30 "+" and 20 "−" in parallel net +10; two habits keep their own counts; "−" stops at 0 without extra writes; the
  tap path never loads the full history; a missing habit fails without writing; the default `dayCountOf`.
- `domain/widget/RefreshCoalescerTest`: redraws asked during a draw merge into one more pass (asked order kept); a
  request is only answered by a pass that started after it (shown = saved); a failing draw fails only its own
  requests; sequential requests get their own pass; the observer reports only the changed kinds, ignores equal
  re-emissions, and reports all kinds on a settings change.
- `WidgetUseCasesTest.changeObserverEmitsOnEveryRepositoryChange` updated: the current state is no longer emitted.
- `:shared:testAndroidHostTest`: all pass (2026-11-21). `:androidApp:assembleDebug` OK; installed on the Oppo.

## Owner test on the phone (please do before merging)

1. Leave the phone locked for 30+ minutes (or overnight), unlock, tap "+" on the Prayer widget once with your finger.
   Expected: the star button flashes at once; the number changes within about a second; one tap = +1.
2. Tap "+" twice quickly. Expected: two flashes, +2, never +1 or +3.
3. Tap "+" then "−" quickly. Expected: back to the same number.
4. Open the app: the Prayer card shows the same count as the widget.
5. If a tap still feels slow, run the logcat command above while repeating step 1 and send the output.

## Notes for later phases

- `tools/gen_widget_layouts.py` is **out of date** compared with the committed layouts (running it deletes and
  rewrites files). A1 edited the six quick-log layouts by hand and added the ripple to the generator's `circle_btn`
  so it is not lost, but the generator needs to be brought back in sync before anyone runs it again.
- Phase C (notification actions) should call `WidgetUpdater.redrawAfterTap` after its write, the same fast path.
