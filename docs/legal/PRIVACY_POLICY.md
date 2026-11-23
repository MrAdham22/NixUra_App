# NixUra Privacy Policy

*Have this policy reviewed by a lawyer before release, and publish it at a public URL (Google Play requires a link).*

**Last updated:** 24 November 2026  **Developer (data controller):** Adham Elgendy, individual developer, Egypt  **Contact:** adhamelgendydev@gmail.com

## 1. What NixUra is
NixUra helps you quit bad habits, build good habits and count down to events. The app works offline without an account. An account is optional and keeps your data in step on all your devices.

## 2. Data we collect
| Data | When | Why | Where it lives |
|---|---|---|---|
| Email address | You create an account (email and password, Google or Facebook) | Sign-in; verification and password-reset emails | Google Firebase Authentication |
| Password | You create an account with email and password | Sign-in | Held only by Firebase Authentication, which stores it as a secure hash. NixUra never sees or stores your password |
| Name and age (optional) | You enter them in Profile | Personalise the app | On your device; with an account also in your synced data |
| Habits (including their names and colours), relapse dates, good-habit logs, events, reminders, theme, language and notification settings | You use the app | The core features; with an account, the same data on all your devices | On your device. With an account also in Google Cloud Firestore (Belgium, europe-west1), where only your account can read it. Without an account it never leaves your device |
| Approximate location (city level, rounded to about 1 km) | Only if you tap "Use my location" for a prayer habit's reminders | Work out prayer times on your phone | On your device only. Never synced, never sent to us or anyone else. Delete it by turning off the location permission and erasing app data |
| Coach chat messages | You use the coach | The offline coach | On your device only; chats are not synced |
| AI coach (only if you turn it on): your messages and voice notes, your habit names with their numbers, and your event titles and dates | You send a message to the AI coach or record a voice note | Sent through NixUra's server (Cloudflare) to online AI services (Google Gemini, Groq, Cloudflare, Mistral, OpenRouter) to write the reply | Not kept by NixUra; never sent with your name, email or account. Some of these services may use what they receive to improve their products. Our server keeps only a count of AI messages per account per day, to apply the daily limit, and deletes it after 3 days |
| Anonymous usage stats: which screens you open, a fixed list of actions (for example "a good habit was logged", "a coach message was sent"), app opens and time in the app, device model, system version, app version, approximate country or region, and a random app-instance id | On by default while you use the app; turn it off in Settings > Privacy > Share anonymous usage stats | Learn which features are used and where people get stuck, to improve NixUra | Sent to Google Firebase Analytics and kept there for 2 months. Never your habit names or kinds of habits, notes, chats, name, email or account id, and no advertising id |
| Bug reports you choose to send | You use "Bugs or issues?" | Fix the problem you report | What you write, plus app version, platform, system version, phone model and app language. Sent by email from your mail app; when you are signed in, also stored in Firestore with your account id |
| Sign-in tokens | You sign in | Keep you signed in | Encrypted on your device (Android Keystore / iOS Keychain) |

We do **not** sell your data and do not use it for advertising.

**Linked calendars (optional, account required).** If you link Google Calendar or Notion in Settings > Integrations, NixUra reads the title, date/time and last-change time of the events you choose: the Google calendars you pick, or the pages of the one Notion database you share. They are stored as events in the app and, like your other events, synced to your account. NixUra never writes to, changes or deletes anything in Google Calendar or Notion.
- **Google Calendar:** access is granted on your phone with Google's own screen, read-only (`calendar.readonly`). The short-lived access token is asked for at each sync and is not stored, on the phone or on our server. NixUra's use of data from Google APIs adheres to the Google API Services User Data Policy, including the Limited Use requirements.
- **Notion:** you approve access on Notion's page. Our server receives a Notion access token and stores it encrypted (AES-256-GCM); it is never sent to your phone. Our server reads the chosen database on your behalf and passes only event data to the app.
- **Disconnect** (Settings > Integrations) revokes Google access, deletes the stored Notion token and removes the imported events from your devices and account. You can also remove NixUra in your Google Account (Security > Third-party access) or in Notion (Settings > Connections). Signing out removes the links from that device; deleting your account deletes the Notion token.

## 3. How we protect it
All connections use HTTPS. Passwords are handled only by Firebase Authentication. Database rules let each account read and write only its own synced data. Sign-in tokens are encrypted on your device, and the Notion token is encrypted on our server. No system is perfectly secure; if a breach affects you, we will tell you and the authorities where the law requires it.

## 4. Service providers
They process data only to run NixUra, on our instructions:
- **Google Firebase:** Authentication (accounts, verification and reset emails), Cloud Firestore in Belgium (europe-west1) (synced data and bug reports), Analytics (the anonymous usage stats, while turned on) and Hosting (the page that opens email links).
- **Cloudflare:** runs NixUra's server for the AI coach and the Notion link (Workers and its database).
- **AI services** (Google Gemini, Groq, Cloudflare Workers AI, Mistral, OpenRouter): write AI coach replies, only if you turn the AI coach on.
- **Google and Facebook:** only if you choose to sign in with them, or link Google Calendar.

These providers may process data outside your country, including in the European Union and the United States, under their own data-protection terms.

## 5. Retention
Synced data is kept while your account exists. Signing out removes your data from that device; it stays in your account. Erase all data removes habits, events and chats from your devices and your account, and keeps your profile and settings. Deleting your account erases your synced data, your Notion link and your Firebase account straight away; we keep no backups of synced data. AI message counts are deleted after 3 days. Usage stats are kept by Firebase for 2 months. Bug reports are kept until the problem is handled; they are not removed automatically when you delete your account, so email us if you want them deleted.

## 6. Your rights and choices
- **Delete your account and its data:** in the app, Settings > Account > Delete account, or by email (see `ACCOUNT_DELETION.md`).
- **Access / correct:** name and age are editable in Profile; for a copy of your account data, email us.
- **Usage stats:** turn them off at any time in Settings > Privacy > Share anonymous usage stats. Collection stops on that device at once.
- **AI coach:** turn it off in Settings > Privacy; the app then uses the offline coach only.
- **Sign out everywhere:** change your password; your other devices are signed out.
- Depending on where you live, you may also have rights to object, restrict processing and data portability, and to complain to your data-protection authority.

## 7. Children
NixUra is not directed at children under 13 (or the age required in your country). Do not create an account if you are younger. If we learn we hold an account of a child under that age, we delete it.

## 8. Health-related content
Habit names can be sensitive (for example addictions). With an account they are stored in your synced data so they can reach your other devices; without one they stay on your device. They are never part of the usage stats. NixUra is not a medical service and does not give medical advice.

## 9. Changes
We will update this page and the date above, and tell you in the app about material changes.

## 10. Contact
Adham Elgendy, adhamelgendydev@gmail.com
