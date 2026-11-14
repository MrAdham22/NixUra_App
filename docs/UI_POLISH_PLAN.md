# UI polish plan (every screen)

Written 2026-11-14 after a full pass over the app. The main screens were checked on the owner's phone (dark theme; the Quit tab also in light theme). The landing and sign-in screens were reviewed **from code only**, because that needs signing out. The starter guide, the voice-note sheet and the home-screen widgets were **not reviewed yet**. The visual rules come from [DESIGN.md](../DESIGN.md), the product rules from [PRODUCT.md](../PRODUCT.md).

Severity: **P0** wrong or misleading for users, fix before release · **P1** hurts the main job or many screens · **P2** inconsistency or friction · **P3** nice to have.

Done already: the **Quit tab** (critique 26 → 32/40, then lift, compact craving button, screen-reader text). See DEVELOPMENT_STATUS.md.

## Phase 0: before anything else (P0)

| Screen | Flaw | Fix |
|---|---|---|
| Privacy Policy / Terms (`Legal`) | Users see draft text: "Draft prepared with the app. Have it reviewed by a lawyer", "Last updated: [date]", "[your legal name or company]". It also says passwords are stored as "Argon2id" hashes, which was the old Node backend; accounts are now on Firebase. Register links here. | Fill in the real details (owner input needed: legal name, contact, date) and update the data section to Firebase, the Worker, AI providers and opt-in analytics. Until then, hide the draft banner from users. |
| Account (**done 2026-11-14**: hidden behind `CloudSync.AVAILABLE` until Firestore sync) | "Cloud sync · Offline. Not synced yet · **Sync now**" while sync is paused. The button promises a feature that doesn't run. | Hide the row, or show "Cloud sync is coming soon" with no button, until Firestore sync exists. |

## Phase 1: fixes that touch many screens at once

**Done 2026-11-14** (all eight items; checked on the owner's phone, 497 tests pass). Weeks start on Sunday everywhere (owner decision).

| # | Flaw (where) | Fix | Sev |
|---|---|---|---|
| 1 | **Rings show a lone dot at low values**: Home "Momentum" outer ring (Breaking free 3%) and the coach's progress card (2%) draw only a round cap at the top, which reads as a glitch. | Give the arc a visible minimum, or hide the cap under ~3%; keep the number as the main signal. | P1 |
| 2 | **Two week starts**: good-habit cards show S M T W T F S (weeks are Sunday to Saturday), the detail calendar shows M T W T F S S. | One week start everywhere (Sunday, matching weekly goals), or follow the phone's setting in both. | P1 |
| 3 | **Long header subtitles**: Build (2 lines) and Events (3 lines) push content down, like the Quit tab did before. | One short line each, as on the Quit tab. | P2 |
| 4 | **Rose used for things that aren't actions**: "Breaking free 3%" in rose on Home and Profile (reads as an alarm), the "English" pill in Settings looks like a primary button but there is nothing to choose, "EVENT" eyebrows on every event card. Breaks DESIGN.md's One Action Colour rule. | Scores in the text color or their own meaning color, the language as plain text, category labels muted. | P2 |
| 5 | **Two primary buttons**: sign-in screens use a gradient button (`AuthGradientButton`), the rest of the app a flat rose `NixButton`. | Pick one primary style and use it everywhere. | P2 |
| 6 | **User text shown as typed**: "smoking", "pray", "scrolling", "adham ahmed elgendy" in lowercase next to capitalized categories and headings. | Capitalize the first letter for display only (never change the saved name). | P2 |
| 7 | **Only quit cards are lifted**: the new soft shadow (DESIGN.md, Elevation & Depth) is on quit cards; good-habit cards, event cards and other `NixCard`s are flat. | Roll the lift out to the other card types in one change, then record it in DESIGN.md. | P2 |
| 8 | **Seconds tick where they don't matter**: an event 6 days away and a past event ("passed since") both show live seconds at full size. | Seconds only within the last day; past events compact. | P3 |

## Phase 2: screen by screen

**Done 2026-11-14 except where noted below as open.** Still open: Phase 0 legal text (needs the owner's details), Home "Breaking free" after a slip (a product decision for the owner), the landing wordmark gradient (owner decision), and an on-device check of the landing, sign-in and widgets.

### Home
- **Done 2026-11-14:** compact quit rows on Home (`QuitHabitRow`).
- **P1** Very long: voice card, score card, today's habits, then every quit card at full size (the same cards as the Quit tab), then next event and the thought. It takes ~3 screens to reach the event. Fix: compact quit rows on Home (name, live time, next milestone, craving), full cards stay on the Quit tab.
- **P2** "Breaking free 3%" right after a slip feels like a punishment. Consider showing the best active streak or next win instead of an average that drops to almost zero.

### Build (good habits)
- **P2** "Today · 0 of 3 done" while the prayer card is at 4/5: partial progress counts as nothing. Show partial progress in the bar.
- **P2** "🔥 0 day streak" in bold on new or missed habits is discouraging. Show "Start today" (or nothing) at 0.
- **P3** The quick picks in the new-habit form offer Drink water, Workout and Pray even when they already exist.

### Good habit detail
- **P2** "Add to Home screen" is a full-width button above the stats; it ranks higher than the habit itself. Move it under the stats or make it a small row.
- **P3** The "Today" card is plain here, but the same habit fills with water on the list. Use the same fill.

### Events
- **P2** Every upcoming card says "EVENT" in rose (the default category), past events say "PERSONAL" in grey: same position, two colors.
- **P2** Past events are as big as upcoming ones and still tick. Make them compact and dimmed.
- **P3** No visible menu on event cards (long-press only), unlike the new ⋯ on quit cards.

### Event form
- **P3** 11 category chips in one block; "Add reminder" repeats the quick-add chips, and the reminder explanation appears twice. Delete is only on long-press, not in the form.

### Coach
- **P2** Header says "AI Coach" and then "AI coach · 9 messages left": the name twice.
- **P2** Today's focus resets when the app restarts and can disagree with the chat (a Junk Food craving chat showed "Today's focus · scrolling"). Remember the focus per chat.
- **P3** "Choose a habit to talk about" uses the uppercase eyebrow tracking on a sentence; quick-prompt chips are cut at the screen edge with no hint they scroll.

### Coach history ("Chats")
- **P3** Fine. Each row has a trash icon at full strength; it could be quieter, or moved into a long-press like the cards.

### Profile
- **P2** The name is shown huge (two lines) in lowercase, and "Breaking free" is in rose (see Phase 1 #4, #6).

### Settings
- **P2** The row is called "Calendars" but opens a screen titled "Integrations". One name.
- **P2** Theme offers Dark and Light only; add "Same as phone".
- **P3** The language pill (only English) looks like a button (Phase 1 #4).

### Integrations
- **P3** Notion shows "Connected" and "Not synced yet" with no hint of what happens next or when it syncs on its own.

### New / edit quit habit
- **P2** Name recognition false positive: "Test stage card" became "Did you mean 'cardio'?" and "This looks like a good habit. Add it to Build instead?". Tighten the fuzzy match so a whole-word miss doesn't trigger it.

### Landing (code review only)
- **P2** The official broken-chain icon (owner decision 2026-11-14) is not on the first screen.
- **P2** PRODUCT.md says private and offline come first, but the guest path is a small grey "continue offline" link under "Get Started". Give it equal standing.
- **P2** "Get Started" opens Sign in, not Create account; a new user's first step is the less likely one.
- **P3** "NixUra" is gradient text (DESIGN.md has no gradient type). Owner decision: keep as a wordmark or make it solid.

### Sign in, Create account, Verify email, Forgot password (code review only)
- **P2** Gradient primary button (Phase 1 #5). Create account links to the draft legal pages (Phase 0).
- To check on a device: error messages, the glass card on small phones, keyboard covering the button.

### Good-habit form, prayer look (audited 2026-11-14)
- **Done:** chips, "Every day", the stepper and cursor are gold in the prayer look (they were rose next to a gold button).
- **P2** Adding "Pray" again when a prayer habit already exists gives no warning (two prayer cards).
- **P3** The decorative stars drift over the name field's edge and between chips; keep them in the margins.
- **P3** The moon sits in a round, button-shaped circle in the header but does nothing when tapped.
- **P3** The small 8-pointed stars on the gold button read as settings gears at that size.

### Not reviewed yet
Starter guide, voice-note sheet (records audio), home-screen widgets (only the earlier version was owner-checked). These need a separate pass.

## Order of work

1. Phase 0 (needs the owner's legal details for the policy text).
2. Phase 1 items 1–2 (P1), then 3–7 in one consistency round.
3. Home, then Build and Events, then Coach, then Settings/Profile/Integrations, then the auth screens on a device.
4. Re-run `/impeccable critique` on each screen after its round and record the score here.
