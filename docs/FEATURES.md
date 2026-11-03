# NixUra: Product and Features

**Let's Nix the bad Aura.** NixUra is a mobile app that helps people quit bad habits, build good ones and stay focused on what matters to them, in one place.

## The problem

Most people trying to change their behaviour juggle separate tools: a counter for quitting, a tracker for daily habits, a calendar for what's coming up. Quitting and building are two sides of the same change, and these apps rarely treat them that way. Generic trackers also tend to stop at a checkmark. They don't show why progress is real, or help in the moment a craving hits.

## What NixUra does

| Pillar | What the user gets |
|---|---|
| **Quit** | A live counter of the exact days, hours, minutes and seconds since the last slip. Each habit's card visibly lightens as the streak grows. |
| **Build** | Daily targets such as "drink 8 glasses of water". Each tap fills the card with more green, until it is solid green when the day's target is reached. |
| **Count down** | Live countdowns to the events that matter, with reminders. |
| **Coach** | A private, built-in coach for cravings, slips and motivation. It uses the user's own progress and works with no internet connection. |

## Features available today

### Progress you can see and understand
- **Live quit counters** that keep running, with a card color that progresses at 1, 3, 7, 14 and 30 days.
- **Personal card colors:** users choose a color, and it becomes a gradient that grows with their streak.
- **Scores that explain themselves:** Breaking free, Discipline and Momentum. Tapping a score shows the rule behind it and each habit's share of it.
- **Honest relapse handling:** a relapse restarts the current streak but keeps the history and the best streak.
- **Recovery timelines** that show what can improve over time for a recognized habit, with a clear note that this is not medical advice.

### Daily habits that feel rewarding
- One-tap logging from the dashboard and the Build tab, with a progressive green fill and a small celebration when a target is reached.
- A 7-day strip, a month calendar, streaks and completion rates.
- Past days can be corrected from the calendar.

### Smart habit entry
- Free-text habit names with typo tolerance and English and Arabic recognition.
- A catalog of known habits, so the app can tailor its content to what the user typed.
- Quick picks for common good habits, plus a "Write my own" option.

### Events and reminders
- Live countdown cards, with multiple reminders per event.
- Daily check-in and streak milestone notifications (Android).

### Coach
- Works entirely on the device, with no internet connection and no account.
- Handles cravings, slips, progress questions and "what happens if I stop for 3 days?".
- Responds to crisis language with a supportive safety message.
- Separate chats with a history, stored only on the user's phone.

### Accounts and sync
- **Guest mode:** the complete app with no sign-up, entirely offline.
- **Optional accounts** with email verification.
- **Cloud sync** of habits, history, events and settings across devices, built to work offline first.
- Passwords hashed with Argon2id, rotating session tokens, and per-device session management.
- In-app account deletion that also deletes the user's server data.

### Experience
- Dark-first design with a light theme, and a distinctive six-color brand palette.
- Fluid screen transitions and touch feedback that respect the system's "remove animations" setting.
- An interactive starter guide that tours the real app and can be replayed from Settings.
- One tap from any main tab to account, sign-in and sign-out.
- Android home-screen widgets for a quit counter, one-tap good-habit logging, and event countdowns.

## Technology

| Area | Choice | Why it matters |
|---|---|---|
| App | Kotlin Multiplatform and Compose Multiplatform | One shared codebase is designed to serve both Android and iOS |
| Architecture | Clean Architecture with MVVM | Features can be added and tested independently |
| Data | Offline-first local database with versioned migrations | The app is fast and works anywhere, and user data survives upgrades |
| Backend | Node.js, TypeScript, Fastify, PostgreSQL | Standard, widely understood and portable, with no platform lock-in |
| Coach | Offline rule engine behind a replaceable interface | A real AI can be added later without rebuilding the app |

## Status

| Item | Status |
|---|---|
| Android app | Built and running on a real device, in owner testing |
| Backend | Runs locally with automated tests; not yet deployed to production hosting |
| iOS app | Code is written from the shared base; not yet built or run on an iPhone |
| Public release | Not yet released. There are no public user numbers yet |

## Roadmap

1. Finish user-feedback polish on Android.
2. Deploy the backend, with real email delivery.
3. Build and verify the iOS app, then release on TestFlight.
4. Add Notion and Google Calendar import.
5. Prepare the store release: final app ID, signing, privacy review and listing.
6. Add a real AI coach as a planned premium feature, with user consent and the offline coach as a fallback.

## Contact

adhamelgendydev@gmail.com
