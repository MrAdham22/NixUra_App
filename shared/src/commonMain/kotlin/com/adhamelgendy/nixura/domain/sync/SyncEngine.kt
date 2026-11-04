package com.adhamelgendy.nixura.domain.sync

import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.clock.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Parents come first, so a device never receives a relapse before its habit. */
enum class SyncType(val wire: String) {
    Habit("habit"),
    GoodHabit("good_habit"),
    HabitEvent("habit_event"),
    GoodHabitLog("good_habit_log"),
    Event("event"),
    Settings("settings"),
    ;

    companion object {
        fun fromWire(value: String): SyncType? = entries.firstOrNull { it.wire == value }
    }
}

/** One synced thing as the server sees it. [payload] is JSON that only the app understands. */
data class SyncRecord(val type: SyncType, val id: String, val updatedAt: Long, val deleted: Boolean, val payload: String)

data class SyncPage(val records: List<SyncRecord>, val cursor: String, val hasMore: Boolean)

sealed interface SyncRemoteResult<out T> {
    data class Ok<T>(val value: T) : SyncRemoteResult<T>
    data object Offline : SyncRemoteResult<Nothing>
    data object SessionEnded : SyncRemoteResult<Nothing>
    data object Failed : SyncRemoteResult<Nothing>
}

/** The backend. Implemented over HTTP; a fake server in tests. */
interface SyncRemote {
    /** Returns the records the server refused because it holds a newer version. */
    suspend fun push(records: List<SyncRecord>): SyncRemoteResult<List<SyncRecord>>
    suspend fun pull(cursor: String): SyncRemoteResult<SyncPage>
}

/** This device's copy of the data. Implemented over Room. */
interface SyncLocalStore {
    fun observePendingCount(): Flow<Int>
    suspend fun pendingCount(): Int

    /** Up to [limit] changes waiting to upload, parents first. */
    suspend fun pendingRecords(limit: Int): List<SyncRecord>

    /** Uploaded. A row edited again since then stays pending. */
    suspend fun markUploaded(records: List<SyncRecord>)

    /**
     * Applies pulled records and saves [cursor], all or nothing, following [SyncRules]. With [adoptGuestData], a local
     * habit, good habit or event that matches a new remote one by content becomes that item instead of a duplicate.
     */
    suspend fun applyRemote(records: List<SyncRecord>, cursor: String, adoptGuestData: Boolean)

    suspend fun cursor(): String
    suspend fun lastSyncedAt(): Long?
    suspend fun setLastSyncedAt(atEpochMillis: Long)

    /** Removes this account's data from the device: habits, events, chats, profile and the sync position. */
    suspend fun clearAccountData()

    /** The account is gone but the data stays: everything counts as not uploaded, for whichever account comes next. */
    suspend fun detachFromAccount()
}

/** Android schedules real background work; iOS only syncs while the app is open for now. */
interface BackgroundSyncScheduler {
    fun schedulePeriodic()
    fun scheduleRetry()
    fun cancelAll()
}

object NoOpBackgroundSyncScheduler : BackgroundSyncScheduler {
    override fun schedulePeriodic() = Unit
    override fun scheduleRetry() = Unit
    override fun cancelAll() = Unit
}

object SyncRules {
    /** Should a pulled version replace the local one? A local edit waiting to upload survives only if it is newer. */
    fun remoteWins(localDirty: Boolean, localUpdatedAt: Long, remoteUpdatedAt: Long): Boolean =
        !localDirty || remoteUpdatedAt >= localUpdatedAt
}

sealed interface SyncStatus {
    val lastSyncedAt: Long?

    data class Idle(override val lastSyncedAt: Long?) : SyncStatus
    data class Syncing(override val lastSyncedAt: Long?) : SyncStatus
    data class Offline(override val lastSyncedAt: Long?) : SyncStatus
    data class Failed(override val lastSyncedAt: Long?) : SyncStatus
}

enum class SyncOutcome { Synced, NotSignedIn, Offline, SessionEnded, Failed }

/**
 * One sync cycle: settle the guest-data handoff, pull, push. Pull comes first so guest data can merge with what the
 * account already has before anything is uploaded. Only one cycle runs at a time, and retries are always safe: pulled
 * data and the cursor are saved together, and the server ignores repeated changes.
 */
class SyncEngine(
    private val local: SyncLocalStore,
    private val remote: SyncRemote,
    private val sessions: SessionManager,
    private val clock: Clock,
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle(null))
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    suspend fun syncNow(): SyncOutcome = mutex.withLock {
        if (sessions.state.value !is SessionState.SignedIn) return@withLock SyncOutcome.NotSignedIn
        val last = local.lastSyncedAt()
        _status.value = SyncStatus.Syncing(last)
        val outcome = try {
            cycle()
        } catch (e: CancellationException) {
            _status.value = SyncStatus.Idle(last)
            throw e
        } catch (e: Exception) {
            SyncOutcome.Failed
        }
        _status.value = when (outcome) {
            SyncOutcome.Synced -> SyncStatus.Idle(local.lastSyncedAt())
            SyncOutcome.Offline -> SyncStatus.Offline(last)
            SyncOutcome.Failed -> SyncStatus.Failed(last)
            SyncOutcome.NotSignedIn, SyncOutcome.SessionEnded -> SyncStatus.Idle(last)
        }
        outcome
    }

    /** Runs [block] with no sync cycle in progress or able to start, for changes like clearing data at sign-out. */
    suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock {
        val result = block()
        _status.value = SyncStatus.Idle(local.lastSyncedAt())
        result
    }

    private suspend fun cycle(): SyncOutcome {
        var adopt = false
        sessions.pendingGuestHandoff()?.let { handoff ->
            if (handoff.previousUserId != null && handoff.previousUserId != handoff.userId) {
                // Another account's data is on this device: it must never reach this account.
                local.clearAccountData()
                sessions.markGuestHandoffHandled()
            } else {
                adopt = true
            }
        }
        pullAll(adopt)?.let { return it }
        if (adopt) sessions.markGuestHandoffHandled()

        var refused = false
        repeat(MAX_PUSH_BATCHES) {
            val batch = local.pendingRecords(PUSH_BATCH)
            if (batch.isEmpty()) return@repeat
            when (val result = remote.push(batch)) {
                is SyncRemoteResult.Ok -> {
                    local.markUploaded(batch)
                    if (result.value.isNotEmpty()) refused = true
                }
                else -> return failureOf(result)
            }
        }
        // The server holds something newer than what was just sent: fetch it, so this device ends up the same.
        if (refused) pullAll(adoptGuestData = false)?.let { return it }

        local.setLastSyncedAt(clock.nowEpochMillis())
        return SyncOutcome.Synced
    }

    /** Fetches every page before applying, because a parent can arrive on a later page than its child. Null means success. */
    private suspend fun pullAll(adoptGuestData: Boolean): SyncOutcome? {
        val records = ArrayList<SyncRecord>()
        var cursor = local.cursor()
        while (true) {
            when (val result = remote.pull(cursor)) {
                is SyncRemoteResult.Ok -> {
                    records += result.value.records
                    cursor = result.value.cursor
                    if (!result.value.hasMore) break
                }
                else -> return failureOf(result)
            }
        }
        local.applyRemote(records, cursor, adoptGuestData)
        return null
    }

    private fun failureOf(result: SyncRemoteResult<*>): SyncOutcome = when (result) {
        SyncRemoteResult.SessionEnded -> SyncOutcome.SessionEnded
        SyncRemoteResult.Offline -> SyncOutcome.Offline
        else -> SyncOutcome.Failed
    }

    private companion object {
        const val PUSH_BATCH = 200
        const val MAX_PUSH_BATCHES = 500
    }
}
