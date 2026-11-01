# Agent 1 handoff: swipe navigation, droplet nav bar, reminder edit/remove

**Verification state: nothing was compiled or run.** This session had no Gradle, kotlinc, emulator or network. Every API I used was checked to exist in the project's own Compose 1.12.1 / lifecycle 2.11 metadata klibs, but the code is **unverified** until you build it. First commands to run:

```
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:assembleDebug
```
No Room entity, DAO or migration changed, so `tools/room_migration_check.py` is unaffected.

## What was built

### 1. Swipe between tabs
- `HorizontalPager` hosts Home, Quit, Build and Events (`MainTabs.pagerTabs`). Tapping a bar icon animates the pager; swiping updates the selected tab once the page **settles** (mid-swipe positions are ignored).
- `Navigator` is still the source of truth, so Home's "See all", widget deep links, `selectTab` and the system back button behave as before.
- **Not swipeable (normal destinations):** the AI Coach (full-screen, tapping its icon opens it as before), Profile, Settings, all detail and form screens.
- **Back convention (unchanged):** back from a non-Home tab returns to Home (the pager animates there); back from Home leaves the app; back from a detail/form returns to the tab it came from. `MainTabsTest` pins this.
- `beyondViewportPageCount = 1`: neighbours are pre-composed so a swipe never shows an empty page, farther pages are disposed. Scroll positions are saved/restored by the pager's saveable state while the host is alive. Opening a detail screen leaves the host (same as before), so a tab's scroll position is not kept across a detail visit (existing behaviour, not regressed).
- Nested gestures: the pager uses Compose's normal nested scrolling; the only horizontally scrollable content inside the tabs (a chip row in `HabitComponents`) scrolls first and hands over at its edges. The good-habit calendar, the Coach chips and the widget config screens are outside the pager.
- RTL: the pager and the droplet both mirror (`DropletMath.toPixels` flips x). TalkBack: pages other than the current one are hidden from accessibility; every bar item is a `Role.Tab` with selected state and a content description.

### 2. Water-droplet bottom bar (`DropletNavBar`)
- Leading edge follows `currentPage + currentPageOffsetFraction`, so it tracks a finger; a spring tail (damping 0.58) lags behind, which stretches the droplet toward the target, then contracts it with a small overshoot. Squash and tail shrink scale with the stretch.
- Draw-phase only: position/tail are read inside `drawBehind` / `graphicsLayer`, so scrolling does not recompose the bar; one reused `Path`, no allocations per frame. Icon tint is two stacked icons cross-faded in the layer phase.
- Generic over 3 to 5 items, any width, font scale (items have a 56 dp minimum height and ellipsized labels).
- Haptic tick on selection (`HapticFeedbackType.TextHandleMove`, which follows the system touch-feedback setting). "Remove animations" (Android `ANIMATOR_DURATION_SCALE == 0`, iOS Reduce Motion) -> no stretch/overshoot and a short tween instead of a spring.
- **Decision:** I did not add the optional gooey blur + alpha-threshold `RenderEffect`. The droplet is one analytic tapered path, so it looks the same on every API level and on iOS and there is no second code path to keep glitch-free.

### 3. Reminders: edit and remove (event edit screen = the "Events details" screen)
- New reminders section: one row per reminder (label, "Notifies you on ..." or "Already passed"), edit and remove buttons (48 dp targets), quick-add chips for presets not yet added, "Add reminder" dialog (value + unit + "At event time") with live validation.
- Remove shows a snackbar with **Undo**. Undo uses `restore`, which skips the "must be in the future" rule so a reminder that passed meanwhile can still come back.
- Rules (`ReminderRules`, pure): no negative lead, max 365 days, no duplicates, max 10 per event, trigger must be in the future, none for a passed event. `SaveEventUseCase` now also normalizes the list (distinct, sorted, capped).
- **Consistency:** for an existing event every reminder change is saved immediately (`EventReminderEditor`, mutex-serialized). The sync service rebuilds the plan from the database and `AndroidNotificationScheduler.replaceAll` cancels everything it armed before, so an edited reminder (new key) or removed one (missing key) can never leave an orphaned alarm. Keys are stable (`event:<id>:<lead>`); the PendingIntent request code is now `key.hashCode()` as well as the unique data URI. Editing an event's time re-plans all its reminders automatically. Deleting an event removes its reminders from the plan.
- A new (unsaved) event keeps a draft list that is saved with the event.
- Reboot/update: unchanged mechanism (`BootReceiver` re-arms the stored plan). It also re-arms when the exact-alarm permission changes.
- Widgets refresh after any change because the events flow changes (`ObserveWidgetDataChangesUseCase`).
- **Permissions:** Android 13+ notification permission is requested when the first reminder is added (as before). If notifications are blocked, a card explains it and opens the app's notification settings. From Android 12, event reminders use `setExactAndAllowWhileIdle` only when `canScheduleExactAlarms()`; otherwise they fall back to the inexact alarm and the UI offers "Allow exact alarms". Milestones and the daily check-in are always inexact.

## Files
New: `domain/notification/{ReminderRules,ReminderCapabilities}.kt`, `presentation/navigation/MainTabs.kt`, `presentation/components/{DropletNavBar,DropletMath}.kt`, `presentation/platform/ReducedMotion.kt` + `androidMain`/`iosMain` actuals, `presentation/screen/EventRemindersSection.kt`.
Changed: `presentation/screen/{AppShell,EventFormScreen}.kt`, `presentation/events/EventsViewModel.kt`, `domain/usecase/EventUseCases.kt`, `presentation/strings/Strings.kt` (one additive block), `di/AppModules.kt`, both `PlatformModule` files, Android `notifications/{AndroidNotificationPermission,AndroidNotificationScheduler,BootReceiver}.kt`, `AndroidManifest.xml`.
Tests (`commonTest`): `ReminderRulesTest`, `EventReminderEditorTest`, `ReminderPlanningTest`, `MainTabsTest`, `DropletMathTest`.

## Owner setup / decisions
1. Build and run the tests above; fix any first-compile issue (I could not).
2. **Play Store:** `SCHEDULE_EXACT_ALARM` is a restricted-use permission. The app works without it (inexact fallback), so if you do not want a Play declaration, delete the `<uses-permission ... SCHEDULE_EXACT_ALARM>` line; the "Allow exact alarms" card then simply never appears (it needs the permission to be declared).
3. Check emulator/device with these manual tests (no Compose UI test setup exists in the repo, so the "UI test for edit + delete + undo" is manual):
   - Tap each tab, swipe each way, mixed (tap during a swipe), rotate; droplet follows the finger and settles.
   - Event with 3 reminders: edit one (old alarm must not fire: `adb shell dumpsys alarm | grep nixura`), remove one + Undo, add a duplicate / past one (error shown), edit the event time.
   - Deny notifications -> card appears -> open settings -> allow -> return (card disappears on resume). Revoke exact alarms -> inexact fallback.
   - Reboot with a pending reminder. Widget shows the nearest event after changes.
   - Arabic/RTL (force RTL in developer options), TalkBack, Remove animations, font scale 200%.
4. Strings: new text is English in `Strings.kt` per the repo convention.

## Known limitations
- Unverified: compilation, droplet look at 60 fps, pager/Crossfade transition, TalkBack behaviour, all iOS code (`UIAccessibilityIsReduceMotionEnabled`, no-op capabilities).
- In edit mode, reminder changes are saved immediately but title/date/category still need "Save changes". If the user changes the date without saving, reminder validation uses the date on screen while the final guard uses the stored date.
- No Compose UI/instrumented test; no ViewModel test (the repo has no coroutines-test dependency, and I did not add one).
- Reminder time/duration text for "Notifies you on" uses the existing date formatter; "Already passed" rows stay listed so the user can remove them.
