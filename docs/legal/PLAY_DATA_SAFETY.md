# Google Play Data Safety checklist

Fill the form from the real data flows below. Other agents append their own rows at the marked anchors; re-check the form after each release.

## Data collected and shared
| Play category | Data type | Collected | Shared with third parties | Required or optional | Purpose |
|---|---|---|---|---|---|
| Personal info | Email address | Yes | No (Firebase Authentication acts as a service provider; Play does not count this as "sharing") | Required for an account (the app works without one) | Account management |
| Personal info | Name | Yes (optional field) | No | Optional | App functionality |
| Personal info | Age (not "date of birth") | Yes (optional field) | No | Optional | App functionality |
| App info and performance | Bug reports the user chooses to send (text, app/OS version, device model) | Yes, only when sent | No | Optional | App functionality (support) |
| App activity / Other user-generated content | Habit names, relapse dates, good-habit logs, events, reminders (cloud sync; habit names can reveal health information, so review whether to also declare Health info) | Yes, with an account only | No | Optional (no account means nothing leaves the device) | App functionality |
| App activity / Other actions | Theme, language and notification settings | Yes, with an account only | No | Optional | App functionality |

Not collected by this release: location, contacts, photos, files, financial info, advertising ID. Guests send nothing except the anonymous usage stats below (on by default, can be turned off).

Approximate location (`ACCESS_COARSE_LOCATION`, UX phase C): read once on the device, only when the user asks for prayer-time reminders, rounded to about 1 km and used only on the device to calculate prayer times. It is never transmitted off the device, so under Play's definition it is **not collected** (data processed only on the device). Declare the permission's use in the store listing / permissions section if asked; re-check if prayer reminders ever sync the location.

## Security practices section
- Data is encrypted in transit (HTTPS): **Yes**.
- You can request data deletion: **Yes** (in-app and web path in `ACCOUNT_DELETION.md`).
- Passwords are handled only by Firebase Authentication (hashed by Google); sign-in tokens are encrypted on the device; database rules limit each account to its own data.

## Account deletion
- In-app path: Settings > Account > Delete account.
- Web link: URL where `ACCOUNT_DELETION.md` is published (owner to host).

## Other Play items for apps with accounts
- Privacy policy URL (host `PRIVACY_POLICY.md`) in the store listing and inside the app.
- Provide a test account for app review (create one verified account; share its credentials in Play Console > App access).

<!-- AGENT-4: user-generated chat content processed by Google Gemini via our server. -->
| Calendar | Calendar events (title, date/time) imported from Google Calendar or a shared Notion database | Yes, only when the user links a calendar (account required) | No | Optional | App functionality |
| Files and docs | Notion database pages (title and date properties only) | Yes, only when Notion is linked | No | Optional | App functionality |

Google Calendar access is read-only and its token is not stored; the Notion token is stored encrypted on the server. Google's OAuth verification for the sensitive `calendar.readonly` scope is needed before release (see README, "Linking calendars").

## Usage stats (Firebase Analytics, on by default since 2026-11-23)
| Play category | Data type | Collected | Shared with third parties | Required or optional | Purpose |
|---|---|---|---|---|---|
| App activity | App interactions (screens viewed, a fixed list of actions such as "good habit logged") | Yes | No (Firebase acts as a service provider) | Optional: users can turn it off in Settings > Privacy | Analytics |
| Device or other IDs | Firebase app-instance id | Yes | No (service provider) | Optional (same switch) | Analytics |
| App info and performance | Device model, OS and app version, session length | Yes | No (service provider) | Optional (same switch) | Analytics |

Not sent: habit names or kinds of habits, notes, chats, name, email, account id, advertising id (`google_analytics_adid_collection_enabled` is false in the manifest). Before submitting, compare these rows with Google's official Firebase data-disclosure page (it may also list approximate location derived from the IP address) and keep the Analytics data-retention period in Firebase at 2 months to match the privacy policy. Consider an EU/UK consent step, since collection is on by default.
