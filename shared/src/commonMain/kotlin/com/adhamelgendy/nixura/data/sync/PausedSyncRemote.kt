package com.adhamelgendy.nixura.data.sync

import com.adhamelgendy.nixura.domain.sync.SyncPage
import com.adhamelgendy.nixura.domain.sync.SyncRecord
import com.adhamelgendy.nixura.domain.sync.SyncRemote
import com.adhamelgendy.nixura.domain.sync.SyncRemoteResult

/**
 * Cloud sync while accounts move to Firebase: the old NixUra server cannot check Firebase sign-ins and the Firestore
 * version is not built yet, so every call reports "offline". Changes stay queued on the phone and upload once the
 * Firestore remote replaces this object.
 */
internal object PausedSyncRemote : SyncRemote {
    override suspend fun push(records: List<SyncRecord>): SyncRemoteResult<List<SyncRecord>> = SyncRemoteResult.Offline
    override suspend fun pull(cursor: String): SyncRemoteResult<SyncPage> = SyncRemoteResult.Offline
}
