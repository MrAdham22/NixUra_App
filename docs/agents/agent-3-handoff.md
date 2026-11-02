# Agent 3 handoff: cloud sync

## Verification state
| Check | Result |
|---|---|
| Backend typecheck and tests (PGlite) | **Pass**, 55 of 55, 12 of them sync |
| `:shared:testAndroidHostTest` | **Pass**, 369, including `SyncEngineTest` (fake server, two devices) |
| `:androidApp:assembleDebug`, `installDebug` | **Pass** |
| `tools/room_migration_check.py` | **Pass**: v2 to v3 backfill, 62 queries |
| Migration on a real phone with real data | **Pass**: v3, no crash, all rows kept, nothing blank |
| Sync between app and server, two devices, sign-out clearing, erase across devices | **Not yet tested on a device** |
| `RoomSyncLocalStore` | **No automated test.** The repo has no Room test setup, so it is covered only by the phone test |
| Background sync (`SyncWorker`) | **Not run** |
| iOS | **Not compiled** (needs a Mac). Foreground sync only, by design for now |

## What was built
- **Backend:** `migrations/002_sync.sql`, `src/sync/syncService.ts`, `POST /v1/sync/push`, `GET /v1/sync/pull`.
- **Room v3 (`MIGRATION_2_3`, additive):** `sync_id`, `updated_at`, `deleted_at`, `dirty` on `habits`, `habit_events`, `good_habits`, `good_habit_logs`, `events`.
- **Domain (`domain/sync`):** `SyncEngine` (one cycle: handoff, pull, push), `SyncCoordinator` (when to sync), `SignOutUseCase`, `DetachDataAfterAccountDeletionUseCase`, ports `SyncLocalStore`, `SyncRemote`, `BackgroundSyncScheduler`.
- **Data (`data/sync`):** `HttpSyncRemote`, `RoomSyncLocalStore`, `dao/SyncDao`.
- **Android:** `sync/AndroidBackgroundSync.kt` (WorkManager, every 6 h plus retry when online). New dependency: `androidx.work:work-runtime-ktx` 2.10.1.
- **UI:** cloud sync card in Settings > Account (last synced, offline, error, Sync now).
- **Fix:** `AuthorizedApi` is now one shared Koin instance. Separate instances would have defeated its single-flight token refresh.

## Rules
- **Synced:** bad habits (with colour), relapses, good habits and daily counts, events and reminders, theme, language, notification and check-in settings. Name and age use Agent 2's `/v1/me`.
- **Not synced:** coach chats and the onboarding flag.
- **Conflicts:** newest edit per item wins, by device timestamp capped at the server clock. Equal timestamps pick the larger payload.
- **Sign-out (owner decision):** upload, then clear the phone. If changes could not upload, the user is warned and chooses.
- **Erase all data (owner decision):** signed in, marks everything deleted and syncs it; chats are removed at once. As a guest, deletes locally. Profile and settings stay.
- **Account deletion:** server data is removed; the phone keeps the data as guest data.
- **Guests (owner decision):** no network calls.
- **First sign-in with guest data:** uploaded and merged. Same-named habits and good habits, and events with the same title and date, become one item. Merged habits keep the earliest tracking start.

## Known limitations
- Editing the same item on two devices keeps only the newest whole item; there is no field-level merge.
- Erase while signed in removes only what this device knows about. Items created on another device and not yet pulled survive.
- If a session ends without a clean sign-out (expired or revoked), the data stays on the phone until someone signs in. A different account then clears it, including anything not uploaded.
- Items marked deleted are kept forever locally and on the server; there is no clean-up job yet.
- No per-user storage quota. Requests are rate limited and capped at 200 items and 8 KB per item.
- Payloads are not encrypted at rest beyond what the database host provides. Habit names can be sensitive, so choose a host with encrypted storage.
- A wrong phone clock set in the past makes that phone's edits lose to newer ones.
- The second-device first pull has no progress screen; items simply appear. The Account card shows the status.
- Room's exported schema is not produced (Room warns the schema directory is not passed on), so `shared/schemas` stays empty. Pre-existing; the migration check does not use it.

## Owner setup
Nothing new beyond Agent 2's server. Run `npm run migrate` on every deploy.
