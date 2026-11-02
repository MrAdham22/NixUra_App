# Google Play Data Safety checklist (accounts, as shipped by Agent 2)

Fill the form from the real data flows below. Other agents append their own rows at the marked anchors; re-check the form after each release.

## Data collected and shared
| Play category | Data type | Collected | Shared with third parties | Required or optional | Purpose |
|---|---|---|---|---|---|
| Personal info | Email address | Yes | Email delivery provider acts as a service provider (processing on our behalf; Play does not count this as "sharing") | Required for an account (the app works without one) | Account management |
| Personal info | Name | Yes (optional field) | No | Optional | App functionality |
| Personal info | Age (not "date of birth") | Yes (optional field) | No | Optional | App functionality |
| App info and performance / Device or other IDs | Device sessions (device model name, platform) and an account id | Yes | No | Required for an account | Account management, security |
| Security log | Account events | Yes | No | Required for an account | Security, fraud prevention |
| App activity / Other user-generated content | Habit names, relapse dates, good-habit logs, events, reminders (cloud sync; habit names can reveal health information, so review whether to also declare Health info) | Yes, with an account only | No | Optional (no account means nothing leaves the device) | App functionality |
| App activity / Other actions | Theme, language and notification settings | Yes, with an account only | No | Optional | App functionality |

Not collected by this release: location, contacts, photos, files, financial info, advertising ID. Guests send nothing.

## Security practices section
- Data is encrypted in transit (HTTPS): **Yes**.
- You can request data deletion: **Yes** (in-app and web path in `ACCOUNT_DELETION.md`).
- Passwords are hashed (Argon2id); tokens are encrypted on the device.

## Account deletion
- In-app path: Settings > Account > Delete account.
- Web link: URL where `ACCOUNT_DELETION.md` is published (owner to host).

## Other Play items for apps with accounts
- Privacy policy URL (host `PRIVACY_POLICY.md`) in the store listing and inside the app.
- Provide a test account for app review (create one verified account; share its credentials in Play Console > App access).

<!-- AGENT-4: user-generated chat content processed by Google Gemini via our server. -->
<!-- AGENT-5: calendar events and Notion content imported (Calendar, Files and docs). -->
<!-- AGENT-6: analytics (app interactions, crash logs, diagnostics), opt-in. -->
