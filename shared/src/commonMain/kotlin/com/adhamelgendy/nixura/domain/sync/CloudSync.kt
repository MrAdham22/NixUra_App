package com.adhamelgendy.nixura.domain.sync

/**
 * Whether the app offers cloud sync ("Sync now" and the sync status on the Account screen and the account sheet).
 * Sync runs on Firestore (`data/sync/FirestoreSyncRemote`). To pause it again, set this to false and bind `SyncRemote`
 * to `PausedSyncRemote` in `di/AuthModule.kt`, so the app never promises a backup that does not run.
 */
object CloudSync {
    const val AVAILABLE: Boolean = true
}
