# UX A2: launcher App Shortcuts

**Date:** 2026-11-21. **Branch:** `feature/app-shortcuts`. **Status:** done, merged 2026-11-21 (PR #17). **Owner device
test on the Oppo: passed (2026-11-21).**

Long-press the NixUra icon: four real Android App Shortcuts appear under the launcher's own items (App info, ...).

| Id (stable, never rename) | Label EN / AR (long · short) | Opens |
|---|---|---|
| `keep_nixura` | 🥺 Don't delete NixUra! · 🥺 Stay? / 🥺 لا تحذف NixUra! · 🥺 ابقَ؟ | new **Keep NixUra** screen |
| `report_bug` | 🐛 Bugs or issues? · 🐛 Bugs? / 🐛 مشكلة أو خلل؟ · 🐛 خلل؟ | new **Report a problem** screen |
| `help` | 💡 Need help? · 💡 Help / 💡 تحتاج مساعدة؟ · 💡 مساعدة | new **Help** (FAQ) screen |
| `ai_coach` | 🧠 Talk to your AI Coach · 🧠 Coach / 🧠 تحدّث مع مدربك الذكي · 🧠 المدرب | the existing Coach tab |

Long labels are 12–23 characters (limit 25), short 6–9 (limit 10), counted in UTF-16 units with the emoji.

## Where things are (edit here)

- **Shortcut list:** `androidApp/src/main/res/xml/shortcuts.xml` (one file, no logic). Labels in
  `res/values/strings.xml` and `res/values-ar/strings.xml` (`shortcut_*`). Icons: `res/drawable/ic_shortcut_*.xml`
  (Android 7.1 round badge) and `res/drawable-v26/ic_shortcut_*.xml` (adaptive: plum `#451952` background,
  blush `#E8BCB9` glyph from `shortcut_glyph_*.xml`, Material icon paths).
- **Id → screen:** `shared/.../presentation/navigation/AppShortcut.kt` (shared by Android and iOS).
- **Android entry:** `androidApp/.../shortcuts/ShortcutDeepLink.kt`, called from `MainActivity.onCreate` (fresh start)
  and `onNewIntent`. Reports use with `ShortcutManagerCompat.reportShortcutUsed`.
- **Screens:** `shared/.../presentation/support/SupportScreens.kt` + `SupportViewModels.kt`; routes `Route.KeepNixUra`,
  `Route.ReportBug`, `Route.Help` in `Navigator.kt`, wired in `AppShell.kt`.
- **Logic (pure, tested):** `shared/.../domain/support/` (`BugReport.kt` models and ports, `BugReportRules`,
  `BugReportEmail`, `SendBugReportUseCase`, `KeepNixUraSummary`).
- **Firestore:** `data/support/FirestoreBugReportRemote.kt`, rules in `firestore.rules` (`bugReports`), tests in
  `tools/firestore-rules-test/rules.test.mjs`.
- **Platform:** `shared/src/androidMain/.../data/support/AndroidSupport.kt`, `shared/src/iosMain/.../data/support/IosSupport.kt`,
  bound in both platform modules.
- **Text:** `Strings.kt` / `ArabicStrings.kt`, block "UX A2".

## Decisions

- **Static, not dynamic shortcuts.** The four entries never change per user, so XML is simpler: no code at app start,
  they exist from install (before the first launch), and translations come from resources. Dynamic would only be needed
  for per-user items (for example "log water"), which A2 does not have.
- **Back stack:** a shortcut opens its screen over Home (`[Home, screen]`), so Back lands on Home and a second Back
  leaves the app. The Coach is a tab, selected directly; Back goes Home. Static shortcuts start the activity with
  `NEW_TASK | CLEAR_TASK`, so cold and warm starts both arrive in `onCreate`; the extra is removed after use and a
  re-launch from Recents (`FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`) is ignored. A shortcut replaces where the user was
  (like the widgets do).
- **Bug reports** (owner decision 4): signed-in → Firestore `bugReports/{random id}` **and** the email; guests →
  email only (guests make no network calls). The Firestore copy goes first (10 s timeout), then the mail app opens
  with `ACTION_SENDTO mailto:` to adhamelgendydev@gmail.com, subject `NixUra <version> bug [<category>]: <first line>`,
  body = text, steps, category, app version, device (`MANUFACTURER MODEL`), Android version + API, app language. Never
  a name, email or account id in the mail. Offline: the result says the saved copy did not go; the mail app queues the
  mail. No mail app: the screen shows the address. Empty / under 10 letters / over 2000: inline message, nothing sent.
- **Rules:** only `create`, only when `request.auth != null`, `uid == request.auth.uid`, exactly the 10 known fields,
  `createdAt == request.time`, known category, text ≤ 2000, short fields bounded. `read, update, delete: false` for
  everyone (the owner reads reports in the Firebase console). **Not deployed:** needs the owner's OK
  (`firebase deploy --only firestore:rules --project nixura`). Until then a signed-in report's cloud copy is refused
  and the screen says "The saved copy didn't go through, but the email will."
- **Keep NixUra tone:** funny, light guilt (owner decision 5, re-confirmed by the owner on 2026-11-21 over the A2
  prompt's "no guilt-tripping"). Real numbers: longest running quit streak, the water habit's daily cups ("8 glasses of
  tears"), the habit count, guest vs backed up. Three buttons of the same size: "Fine, I'll stay" (Home), "Tell me
  what's wrong" (bug screen), "Uninstall anyway" (system App info page, where Uninstall is; iOS: the app's Settings
  page) with "No hard feelings. Mostly." under it.

## Help FAQ (draft for the owner to review)

1. How do I add a habit? · 2. I slipped. What now? · 3. How do I log a good habit? · 4. How do I put a habit on my home
screen? · 5. Why don't my reminders arrive? (includes the ColorOS battery / auto-start advice) · 6. What does the AI
Coach know? Is it private? · 7. Do I need an account? Is my data backed up? · 8. How do I change the language? ·
9. Why does my quit card change colour? · 10. How do I delete a habit, my data or my account?
Full English and Arabic text: `Strings.helpFaq` / `ArabicStrings.helpFaq`. Each answer was checked against the README
feature table and the real Settings labels.

## Verification

| Check | Result |
|---|---|
| `:shared:testAndroidHostTest` | 657 tests, 0 failures (new: `AppShortcutTest` 3, `BugReportRulesTest` 4, `BugReportEmailTest` 2, `KeepNixUraSummaryTest` 4, `FirestoreBugReportBodyTest` 1, `SendBugReportUseCaseTest` 5; `ArabicCoverageTest` passes) |
| `:androidApp:assembleDebug` | OK |
| Firestore rules on the emulator (`firebase emulators:exec ... node --test tools/firestore-rules-test/rules.test.mjs`, demo project) | 11/11 (3 new: create as self; guest / other uid / bad fields / too long / extra field / no server time refused; nobody can read, list, overwrite or delete) |
| Android 17 emulator (Pixel launcher) | `dumpsys shortcut`: all 4 registered. Each shortcut intent (launcher flags) opens the right screen; Help → Back → Home → Back → launcher; empty Send shows the hint; dark mode; Arabic RTL (Keep, Help with an open answer, mirrored chevrons); emoji render (Noto Color Emoji) |
| Owner's Oppo (CPH2531, Android 15, ColorOS 15) | Installed; `dumpsys shortcut` shows all 4 registered. **Owner device test passed (2026-11-21).** |
| iOS | **Unverified** (Windows). Static quick actions in `Info.plist` (English titles only), `iOSApp.swift` scene delegate, `IosShortcuts.kt`, `IosSupport.kt`. On a Mac: `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:linkDebugFrameworkIosSimulatorArm64 :shared:iosSimulatorArm64Test`, then build `iosApp` in Xcode and long-press the icon |

## Test steps for the Oppo Reno 10 5G

1. Unlock the phone. Long-press the NixUra icon on the home screen. Expect App info etc. plus the 4 shortcuts, in
   this order or ranked by ColorOS, each with the emoji and a plum round icon. Note if any label is cut or an emoji is
   missing.
2. Tap **🥺 Don't delete NixUra!** Expect the "breaking up" screen with your real longest quit streak and the water
   "glasses of tears" line if you have a water habit. Tap **Fine, I'll stay** → Home.
3. Long-press again → **Don't delete** → **Uninstall anyway** → ColorOS App info page for NixUra (do not uninstall).
   Press Back → the Keep screen.
4. Long-press → **🐛 Bugs or issues?** Tap Send with nothing typed → red "Write a few words" line. Type "test" → still
   too short. Type a real sentence, pick a category, Send → Gmail opens with the address, subject and device lines.
   Back to NixUra → "Thank you!" card. Signed in, you also see a line about the saved copy (it fails until the rules
   are deployed).
5. Long-press → **💡 Need help?** → open a few answers; "Report a problem" at the bottom opens the bug screen.
6. Long-press → **🧠 Talk to your AI Coach** → the Coach opens. Back → Home.
7. With the app already open on another screen (for example Settings), use a shortcut: it opens its screen; Back → Home.
8. Swipe NixUra away in Recents, then use a shortcut (cold start): same result.
9. Settings → Language → العربية, then long-press the icon: Arabic labels; open each screen: right-to-left layout.
10. Optional: drag a shortcut onto the home screen (pinned shortcut) and tap it.

## Left for later

- Deploy the `bugReports` rules (owner's OK).
- iOS: build and run on a Mac; Arabic quick-action titles need `InfoPlist.strings` (en/ar) added to the Xcode project.
- Owner review of the FAQ and the Keep wording (EN + AR).
