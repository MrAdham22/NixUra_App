# NixUra Privacy Policy

*Draft prepared with the app. Have it reviewed by a lawyer, fill the bracketed fields, and publish it at a public URL before releasing (Google Play requires a link).*

**Last updated:** [date]  **Controller / developer:** [your legal name or company, address]  **Contact:** adhamelgendydev@gmail.com

## 1. What NixUra is
NixUra helps you quit bad habits, build good habits and count down to events. The app works offline without an account. An account is optional and adds sign-in on your devices.

## 2. Data we collect
| Data | When | Why | Where it lives |
|---|---|---|---|
| Email address | You create an account | Sign-in, one-time codes, security notices | Our server |
| Password | You create an account | Sign-in | Only as an Argon2id hash on our server; never stored in plain text, never on your device |
| Name and age (optional) | You enter them in Profile | Personalise the app | On your device; with an account also on our server |
| Device sessions: device name (e.g. "Pixel 8"), platform, created / last-used time | You sign in | Show and revoke your signed-in devices; keep you signed in securely | Our server |
| Security log: event type (sign-in, password change, suspicious token reuse) with your account id | Account activity | Detect abuse and protect your account | Our server |
| Habits (including their names and colours), relapse dates, good-habit logs, events, reminders, theme, language and notification settings | You use the app | The core features; with an account, the same data on all your devices | On your device. With an account also on our server. Without an account it never leaves your device |
| Coach chat messages | You use the coach | The offline coach | On your device only |
| Authentication tokens | You sign in | Keep you signed in | Encrypted on your device (Android Keystore / iOS Keychain) |

We do **not** sell your data and do not use it for advertising.

<!-- AGENT-4 (AI coach): add chat content and habit context sent to our server and processed by Google (Gemini), consent, retention. -->
<!-- AGENT-5 (integrations): add Google Calendar / Notion data, tokens stored encrypted, how to disconnect and what is deleted. -->
<!-- AGENT-6 (analytics, crash, support): add Firebase Analytics/Crashlytics (opt-in), diagnostics in support reports. -->

## 3. How we protect it
HTTPS only; passwords hashed with Argon2id; one-time codes and refresh tokens stored hashed; rate limiting and lockout; encrypted token storage on the device. See `SECURITY.md`. No system is perfectly secure; we will tell affected users and authorities where the law requires it.

## 4. Service providers
- **Email delivery provider** (Resend or equivalent): receives your email address and the message text (the code) to deliver it.
- **Hosting and database provider:** [name, region] hosts the server and database.
Providers act on our instructions. [Add transfers/safeguards if hosting outside your users' region.]

## 5. Retention
Account data, including synced habits and events, is kept while the account exists. Signing out removes your data from that device; it stays in your account. Erase all data removes habits, events and chats from your account and devices. One-time codes expire after 10 minutes and are deleted with the account; revoked sessions are removed. Deleting the account removes your account record, synced data, sessions and security log entries and leaves one anonymous record that "an account was deleted". Backups may keep copies for up to [30] days before they are overwritten.

## 6. Your rights and choices
- **Delete your account and its server data:** in the app, Settings > Account > Delete account (password required), or by email (section 8 of `ACCOUNT_DELETION.md`).
- **Access / correct:** name and age are editable in Profile; for a copy of your account data email us.
- **Sign out of other devices:** Settings > Account > Devices, or change your password.
- Depending on where you live you may also have rights to object, restrict processing, portability, and to complain to your data-protection authority.

## 7. Children
NixUra is not directed at children under 13 (or the age required in your country). Do not create an account if you are younger. If we learn we hold an account of a child under that age we delete it.

## 8. Health-related content
Habit names can be sensitive (for example addictions). With an account they are stored on our server so they can reach your other devices; without one they stay on your device. NixUra is not a medical service and does not give medical advice.

## 9. Changes
We will update this page and the date above, and tell you in the app for material changes.

## 10. Contact
adhamelgendydev@gmail.com
