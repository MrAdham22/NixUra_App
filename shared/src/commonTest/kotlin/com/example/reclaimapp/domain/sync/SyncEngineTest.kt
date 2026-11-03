package com.example.reclaimapp.domain.sync

import com.example.reclaimapp.domain.auth.AuthUser
import com.example.reclaimapp.domain.auth.GuestDataHandoff
import com.example.reclaimapp.domain.auth.SessionManager
import com.example.reclaimapp.domain.auth.SessionState
import com.example.reclaimapp.domain.clock.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Same rules as the real server: newest wins, ties pick the larger payload, retries change nothing. */
private class FakeServer {
    private var seq = 0L
    private val items = LinkedHashMap<Pair<SyncType, String>, Pair<Long, SyncRecord>>()
    var offline = false
    var failing = false
    var pageSize = 2

    fun current(id: String): SyncRecord? = items.entries.firstOrNull { it.key.second == id }?.value?.second

    fun remote() = object : SyncRemote {
        override suspend fun push(records: List<SyncRecord>): SyncRemoteResult<List<SyncRecord>> {
            problem()?.let { return it }
            val refused = records.filter { r ->
                val key = r.type to r.id
                val old = items[key]?.second
                val accept = old == null || r.updatedAt > old.updatedAt || (r.updatedAt == old.updatedAt && r.payload > old.payload)
                if (accept) items[key] = ++seq to r
                !accept
            }
            return SyncRemoteResult.Ok(refused)
        }

        override suspend fun pull(cursor: String): SyncRemoteResult<SyncPage> {
            problem()?.let { return it }
            val after = cursor.toLong()
            val newer = items.values.filter { it.first > after }.sortedBy { it.first }
            val page = newer.take(pageSize)
            return SyncRemoteResult.Ok(
                SyncPage(page.map { it.second }, (page.lastOrNull()?.first ?: after).toString(), newer.size > pageSize),
            )
        }

        private fun problem(): SyncRemoteResult<Nothing>? = when {
            offline -> SyncRemoteResult.Offline
            failing -> SyncRemoteResult.Failed
            else -> null
        }
    }
}

private data class Row(val record: SyncRecord, val dirty: Boolean)

/** A device's data, following the same [SyncRules] as the Room store. Payload is the item's visible content. */
private class FakeDevice(private val server: FakeServer, val sessions: FakeSessions = FakeSessions()) {
    val rows = LinkedHashMap<String, Row>()
    private var cursorValue = "0"
    private var last: Long? = null
    val clock = FakeClock()

    val local = object : SyncLocalStore {
        override fun observePendingCount(): Flow<Int> = MutableStateFlow(0).map { rows.values.count { r -> r.dirty } }
        override suspend fun pendingCount() = rows.values.count { it.dirty }
        override suspend fun pendingRecords(limit: Int) = rows.values.filter { it.dirty }.map { it.record }.sortedBy { it.type.ordinal }.take(limit)
        override suspend fun markUploaded(records: List<SyncRecord>) {
            records.forEach { r ->
                val row = rows[r.id]
                if (row != null && row.record.updatedAt == r.updatedAt) rows[r.id] = row.copy(dirty = false)
            }
        }

        override suspend fun applyRemote(records: List<SyncRecord>, cursor: String, adoptGuestData: Boolean) {
            records.sortedBy { it.type.ordinal }.forEach { r ->
                if (adoptGuestData && r.id !in rows) {
                    val match = rows.entries.firstOrNull { it.value.dirty && it.value.record.payload == r.payload && it.value.record.type == r.type }
                    if (match != null) {
                        rows.remove(match.key)
                        rows[r.id] = match.value.copy(record = match.value.record.copy(id = r.id))
                    }
                }
                val existing = rows[r.id]
                if (existing == null) {
                    if (!r.deleted) rows[r.id] = Row(r, dirty = false)
                } else if (SyncRules.remoteWins(existing.dirty, existing.record.updatedAt, r.updatedAt)) {
                    rows[r.id] = Row(r, dirty = false)
                }
            }
            cursorValue = cursor
        }

        override suspend fun cursor() = cursorValue
        override suspend fun lastSyncedAt() = last
        override suspend fun setLastSyncedAt(atEpochMillis: Long) {
            last = atEpochMillis
        }

        override suspend fun clearAccountData() {
            rows.clear()
            cursorValue = "0"
            last = null
        }

        override suspend fun detachFromAccount() {
            rows.replaceAll { _, row -> row.copy(dirty = true) }
            cursorValue = "0"
            last = null
        }
    }

    val engine = SyncEngine(local, server.remote(), sessions, clock)

    /** Visible items: what the user sees on this device. */
    fun visible(): Map<String, String> = rows.filter { !it.value.record.deleted }.mapValues { it.value.record.payload }

    fun edit(id: String, content: String, at: Long, type: SyncType = SyncType.Habit) {
        rows[id] = Row(SyncRecord(type, id, at, false, content), dirty = true)
    }

    fun delete(id: String, at: Long) {
        val row = rows.getValue(id)
        rows[id] = Row(row.record.copy(updatedAt = at, deleted = true), dirty = true)
    }
}

private class FakeClock : Clock {
    var now = 1_000L
    override fun nowEpochMillis() = now
}

private class FakeSessions(signedIn: Boolean = true) : SessionManager {
    private val user = AuthUser("user-1", "a@example.com", "", null, true)
    override val state: StateFlow<SessionState> =
        MutableStateFlow(if (signedIn) SessionState.SignedIn(user) else SessionState.SignedOut)
    override val currentUserId: Flow<String?> = state.map { (it as? SessionState.SignedIn)?.user?.id }
    var handoff: GuestDataHandoff? = null
    var forgotLastUser = false
    override suspend fun pendingGuestHandoff() = handoff
    override suspend fun markGuestHandoffHandled() {
        handoff = null
    }

    override suspend fun forgetLastUser() {
        forgotLastUser = true
        handoff = null
    }

    override suspend fun hasLeftoverAccountData() = false

    override suspend fun signOutLocally() = Unit
}

class SyncEngineTest {
    private val server = FakeServer()
    private val a = FakeDevice(server)
    private val b = FakeDevice(server)

    @Test fun anEditOnOneDeviceAppearsOnTheOther() = runTest {
        a.edit("h1", "Smoking", 10)
        assertEquals(SyncOutcome.Synced, a.engine.syncNow())
        assertEquals(SyncOutcome.Synced, b.engine.syncNow())
        assertEquals(mapOf("h1" to "Smoking"), b.visible())
    }

    @Test fun anOfflineEditSurvivesAndUploadsOnReconnect() = runTest {
        a.edit("h1", "Smoking", 10)
        server.offline = true
        assertEquals(SyncOutcome.Offline, a.engine.syncNow())
        assertEquals(1, a.local.pendingCount())
        assertIs<SyncStatus.Offline>(a.engine.status.value)

        server.offline = false
        assertEquals(SyncOutcome.Synced, a.engine.syncNow())
        assertEquals(0, a.local.pendingCount())
        b.engine.syncNow()
        assertEquals(mapOf("h1" to "Smoking"), b.visible())
    }

    @Test fun aServerErrorIsReportedAndNothingIsLost() = runTest {
        a.edit("h1", "Smoking", 10)
        server.failing = true
        assertEquals(SyncOutcome.Failed, a.engine.syncNow())
        assertIs<SyncStatus.Failed>(a.engine.status.value)
        assertEquals(1, a.local.pendingCount())
    }

    @Test fun deletesPropagate() = runTest {
        a.edit("h1", "Smoking", 10)
        a.engine.syncNow()
        b.engine.syncNow()
        a.delete("h1", 20)
        a.engine.syncNow()
        b.engine.syncNow()
        assertTrue(b.visible().isEmpty())
        assertEquals(0, b.local.pendingCount())
    }

    @Test fun theNewestConflictingEditWinsOnBothDevices() = runTest {
        a.edit("h1", "Original", 10)
        a.engine.syncNow()
        b.engine.syncNow()
        a.edit("h1", "Older edit", 20)
        b.edit("h1", "Newer edit", 30)
        b.engine.syncNow()
        a.engine.syncNow()
        b.engine.syncNow()
        assertEquals("Newer edit", a.visible()["h1"])
        assertEquals("Newer edit", b.visible()["h1"])
        assertEquals(0, a.local.pendingCount())
    }

    @Test fun aNewerLocalEditSurvivesAnOlderIncomingOne() = runTest {
        a.edit("h1", "Older", 10)
        a.engine.syncNow()
        b.edit("h1", "Newer", 20)
        b.engine.syncNow()
        assertEquals("Newer", b.visible()["h1"])
        a.engine.syncNow()
        assertEquals("Newer", server.current("h1")?.payload)
        assertEquals("Newer", a.visible()["h1"])
    }

    @Test fun anEditAfterADeleteBringsTheItemBackEverywhere() = runTest {
        a.edit("h1", "Smoking", 10)
        a.engine.syncNow()
        b.engine.syncNow()
        a.delete("h1", 20)
        a.engine.syncNow()
        b.edit("h1", "Still here", 30)
        b.engine.syncNow()
        a.engine.syncNow()
        assertEquals("Still here", a.visible()["h1"])
    }

    @Test fun syncingRepeatedlyCreatesNoDuplicatesAndGoesQuiet() = runTest {
        a.edit("h1", "Smoking", 10)
        repeat(3) { a.engine.syncNow() }
        repeat(3) { b.engine.syncNow() }
        assertEquals(1, b.visible().size)
        assertEquals(0, a.local.pendingCount())
    }

    @Test fun aSecondDeviceGetsEverythingAcrossManyPages() = runTest {
        (1..7).forEach { a.edit("h$it", "Habit $it", it.toLong()) }
        a.engine.syncNow()
        b.engine.syncNow()
        assertEquals(7, b.visible().size)
    }

    @Test fun parentsAreAppliedBeforeChildrenEvenIfTheChildArrivedFirst() = runTest {
        val order = ArrayList<SyncType>()
        val store = object : SyncLocalStore by a.local {
            override suspend fun applyRemote(records: List<SyncRecord>, cursor: String, adoptGuestData: Boolean) {
                order += records.sortedBy { it.type.ordinal }.map { it.type }
            }
        }
        val remote = object : SyncRemote {
            override suspend fun push(records: List<SyncRecord>) = SyncRemoteResult.Ok(emptyList<SyncRecord>())
            override suspend fun pull(cursor: String) = SyncRemoteResult.Ok(
                SyncPage(
                    listOf(
                        SyncRecord(SyncType.HabitEvent, "e1", 1, false, ""),
                        SyncRecord(SyncType.Habit, "h1", 2, false, ""),
                    ),
                    "2",
                    false,
                ),
            )
        }
        SyncEngine(store, remote, FakeSessions(), a.clock).syncNow()
        assertEquals(listOf(SyncType.Habit, SyncType.HabitEvent), order)
    }

    @Test fun theLastSyncTimeIsRecordedOnlyOnSuccess() = runTest {
        a.clock.now = 5_000
        server.offline = true
        a.engine.syncNow()
        assertNull(a.engine.status.value.lastSyncedAt)
        server.offline = false
        a.engine.syncNow()
        assertEquals(5_000, a.engine.status.value.lastSyncedAt)
    }

    @Test fun guestsNeverTouchTheNetwork() = runTest {
        val guest = FakeDevice(server, FakeSessions(signedIn = false))
        guest.edit("h1", "Private", 10)
        assertEquals(SyncOutcome.NotSignedIn, guest.engine.syncNow())
        assertNull(server.current("h1"))
    }

    @Test fun firstSignInUploadsGuestData() = runTest {
        val phone = FakeDevice(server)
        phone.edit("guest1", "Sweets", 10)
        phone.sessions.handoff = GuestDataHandoff("user-1", previousUserId = null)
        phone.engine.syncNow()
        assertEquals("Sweets", server.current("guest1")?.payload)
        assertNull(phone.sessions.handoff)
    }

    @Test fun firstSignInMergesGuestDataWithTheSameItemInTheAccount() = runTest {
        a.edit("account-id", "Smoking", 10)
        a.engine.syncNow()

        val phone = FakeDevice(server)
        phone.edit("guest-id", "Smoking", 20)
        phone.sessions.handoff = GuestDataHandoff("user-1", previousUserId = null)
        phone.engine.syncNow()

        assertEquals(mapOf("account-id" to "Smoking"), phone.visible())
        assertNull(server.current("guest-id"))
    }

    @Test fun anotherAccountsDataIsClearedAndNeverUploaded() = runTest {
        val phone = FakeDevice(server)
        phone.edit("other-account", "Secret", 10)
        phone.sessions.handoff = GuestDataHandoff("user-1", previousUserId = "user-2")
        phone.engine.syncNow()
        assertTrue(phone.visible().isEmpty())
        assertNull(server.current("other-account"))
        assertNull(phone.sessions.handoff)
    }

    @Test fun aFailedFirstSyncKeepsTheHandoffForTheRetry() = runTest {
        val phone = FakeDevice(server)
        phone.edit("guest1", "Sweets", 10)
        phone.sessions.handoff = GuestDataHandoff("user-1", previousUserId = null)
        server.offline = true
        phone.engine.syncNow()
        assertEquals("user-1", phone.sessions.handoff?.userId)
    }

    @Test fun afterDetachingFromADeletedAccountEverythingCountsAsNotUploaded() = runTest {
        a.edit("h1", "Smoking", 10)
        a.engine.syncNow()
        assertEquals(0, a.local.pendingCount())
        a.local.detachFromAccount()
        assertEquals(1, a.local.pendingCount())
        assertEquals("0", a.local.cursor())
        assertFalse(a.visible().isEmpty())
    }
}
