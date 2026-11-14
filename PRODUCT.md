# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

The main user is someone quitting a bad habit or addiction, such as smoking, porn, gambling, alcohol or endless scrolling. They open the app many times a day, often in a weak moment, to see how long they have held out and to get through a craving. Building good habits and counting down to events help with that, but quitting comes first (owner, 2026-11-14).

## Product Purpose

NixUra helps people quit bad habits and stay quit. It shows honest, lasting progress (a live counter since the last slip, a history that is never wiped), supports relapses without shame, and helps people build good habits to replace the old ones. It works well if people keep coming back on hard days and the counter keeps going up.

## Positioning

The owner confirmed three things that make NixUra different (2026-11-14):

- **Private and offline by default.** Everything works with no account and no network. Guests make no network calls except anonymous usage stats, and only if they opt in.
- **Cards that lighten as you go.** Each quit habit is a card with a live counter ticking every second, and its colour moves from dark to light as the active streak grows (0/1/3/7/14/30 days). The colour is worked out from the streak, never stored.
- **Recovery without shame.** A relapse only resets the current streak. The history and the longest streak stay. The coach never shames a slip.

## Operating Context

- People check it often and briefly: a glance at the counter, logging a slip, quick-logging a good habit, or opening the coach during a craving.
- Home-screen widgets on Android (quit counter, good-habit quick log, events) bring the cards outside the app.
- Notifications: event reminders, streak milestones, and a daily check-in at a time the user chooses.
- The coach is a full-screen chat. Guests get the offline rule-based coach. Signed-in users who agree get the real AI coach and voice notes through the Cloudflare Worker, limited to 10 messages and 2 voice notes a day.

## Capabilities and Constraints

- **Quit habits:** live counter, backdated start, relapse log, editable history, recovery timeline from an offline knowledge base, money saved, a colour the user can pick (`ShadeRamp`), archive.
- **Good habits:** daily target counters or "N days a week", streaks, 7/30-day completion, a calendar, editing past days.
- **Events:** countdown or count-up, categories, several reminders each, imports from Google Calendar and Notion.
- **Coach, profile, settings, starter guide, scores** ("Breaking free", "Discipline", "Momentum", all worked out from the data, never stored).
- **Accounts are optional** (Firebase: email, Google, Facebook). Cloud sync is paused.
- **Platforms:** Android is built and runs. iOS is a first-class target that shares the same Compose UI but has never been built or run. The owner wants one NixUra look on both phones, adapting only native details such as safe areas, back gestures and the status bar (owner, 2026-11-14). Platform is recorded as `android` because that is where it runs today. It is not a reason to drop iOS.
- **Language:** English only for now. All UI text lives in `presentation/strings/Strings.kt`.
- **Undecided:** paid plan (Play Billing / StoreKit, not built), onboarding fields, milestone schedule, iOS widgets.

## Brand Commitments

- Name **NixUra** everywhere. App id `com.adhamelgendy.nixura`, which never changes.
- Official icon (owner, 2026-11-14): the owner's artwork of a purple chain broken by a glowing gold "i" on a dark purple square.
- Fixed palette: `#1D1A39` `#451952` `#862549` `#AE445A` `#F39F5A` `#E8BCB9`, dark theme by default. The owner made this binding. Tokens are in `presentation/theme/NixColors.kt`.
- Tone: premium and warm. Not empty and minimal, not cluttered.
- No AI or assistant names anywhere in the product, code or docs.

## Evidence on Hand

- Offline habit knowledge and recovery timelines: `domain/coach/HabitKnowledgeBase.kt`, `domain/coach/catalog/`.
- App icon artwork: `androidApp/src/main/res/mipmap-*` and the 512 px Play Store icon.
- No testimonials, user numbers, reviews, press or clinical claims exist. Never make them up. The coach gives support, not medical advice.

## Product Principles

1. **Progress is never wiped.** A slip resets the current streak, not the person's record.
2. **Private comes first.** Everything works offline with no account. Anything online is opt-in.
3. **Built for the hard moment.** The counter, logging a slip and the coach are always one tap away and quick to read in a weak moment.
4. **Kind, never preachy.** Encourage, never shame. Crisis messages always get a local support reply.
5. **Quitting leads.** Good habits and events support quitting. They don't compete with it.

## Accessibility & Inclusion

- Streak progress is shown by text and a stage marker, never by colour alone.
- Users may be in distress. Copy stays calm and plain, and crisis wording gets a support message even offline.
