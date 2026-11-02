# Agent 3 plan: cloud sync

**Goal:** sign in on another device with the same account and see the same habits, good-habit history, events, reminders and settings.

## Decisions
- **Offline-first:** Room stays the source of truth for the UI. The server is the source of truth across devices.
- **Identity:** local `Long` ids stay, because navigation, widgets and notifications use them. Every synced row also gets a global `sync_id`. Good-habit days use `<habit sync id>:<day>`, so two devices logging the same day merge into one row.
- **Outbox:** a `dirty` flag on each row instead of a separate table. A row holds its own latest state, so retries are naturally safe.
- **Conflicts:** last write wins per item by device timestamp, capped at the server clock. Equal timestamps with different content pick the larger payload, so all devices agree.
- **Deletes:** soft (`deleted_at`), so they reach other devices.
- **Transport:** Ktor through the existing `AuthorizedApi`, because the app also targets iOS.
- **Coach chats are not synced:** they are not in the task's entity list.

## Sync cycle
Pull first, then push. All pulled pages are applied in one transaction, parents before children, and only then is the cursor saved, so a crash just repeats the pull.

## Owner decisions
- **Sign-out:** upload pending changes, then clear the phone. Warn before losing changes that could not upload. Then show the landing page.
- **Erase all data:** removes good and bad habits, events and chats everywhere, and keeps the account, profile and settings.
- **Guests:** completely offline, with no network calls at all.

## Guest and account rules (Agent 2's contract)
- First sign-in with local data: upload it and merge. A local habit, good habit or event matching a server one by name (and date for events) becomes the same item.
- A different account signing in after a session ended without a clean sign-out: clear first, never upload.
- Explicit sign-out or account deletion forgets the last user, so later guest data counts as guest data.

## Triggers
App start and resume, after sign-in, 3 seconds after a local change, "Sync now", and Android background work (periodic plus retry when back online). iOS syncs in the foreground only for now.

## Risks
- The Room migration is hand-written. Checked with `tools/room_migration_check.py`, then for real on a phone.
- Client timestamps can be wrong: capping handles the future, but an old clock loses to newer edits.
- iOS code cannot be compiled on this machine.

## Tests
Backend endpoint tests (isolation, conflicts, retries, paging, deletion), engine tests with a fake server (offline edits, conflicts, deletes, two devices), migration check, then a real phone.
