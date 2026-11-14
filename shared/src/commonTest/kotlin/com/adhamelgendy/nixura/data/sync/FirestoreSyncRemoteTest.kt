package com.adhamelgendy.nixura.data.sync

import com.adhamelgendy.nixura.data.auth.AuthorizedApi
import com.adhamelgendy.nixura.data.auth.FakeFirebase
import com.adhamelgendy.nixura.data.auth.FirebaseAuthApi
import com.adhamelgendy.nixura.data.auth.FirebaseConfig
import com.adhamelgendy.nixura.data.auth.FirebaseSession
import com.adhamelgendy.nixura.data.auth.FixedClock
import com.adhamelgendy.nixura.data.auth.InMemorySecureStore
import com.adhamelgendy.nixura.data.auth.SessionManagerImpl
import com.adhamelgendy.nixura.data.auth.storedSession
import com.adhamelgendy.nixura.domain.sync.SyncEngine
import com.adhamelgendy.nixura.domain.sync.SyncLocalStore
import com.adhamelgendy.nixura.domain.sync.SyncOutcome
import com.adhamelgendy.nixura.domain.sync.SyncRecord
import com.adhamelgendy.nixura.domain.sync.SyncRemoteResult
import com.adhamelgendy.nixura.domain.sync.SyncRules
import com.adhamelgendy.nixura.domain.sync.SyncType
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A phone's data in memory, following the same [SyncRules] as the Room store. */
private class MemoryStore : SyncLocalStore {
    data class Row(val record: SyncRecord, val dirty: Boolean)

    val rows = LinkedHashMap<Pair<SyncType, String>, Row>()
    private var cursorValue = "0"
    private var last: Long? = null

    override fun observePendingCount(): Flow<Int> = MutableStateFlow(rows.values.count { it.dirty })
    override suspend fun pendingCount() = rows.values.count { it.dirty }
    override suspend fun pendingRecords(limit: Int) = rows.values.filter { it.dirty }.map { it.record }.sortedBy { it.type.ordinal }.take(limit)
    override suspend fun markUploaded(records: List<SyncRecord>) = records.forEach { r ->
        val key = r.type to r.id
        rows[key]?.let { if (it.record.updatedAt == r.updatedAt) rows[key] = it.copy(dirty = false) }
    }

    override suspend fun applyRemote(records: List<SyncRecord>, cursor: String, adoptGuestData: Boolean) {
        records.forEach { r ->
            val key = r.type to r.id
            val existing = rows[key]
            if (existing == null) {
                if (!r.deleted) rows[key] = Row(r, dirty = false)
            } else if (SyncRules.remoteWins(existing.dirty, existing.record.updatedAt, r.updatedAt)) {
                // A delete marker arrives without content; the row keeps its fields and is marked deleted.
                rows[key] = Row(if (r.deleted) existing.record.copy(updatedAt = r.updatedAt, deleted = true) else r, dirty = false)
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
    }

    override suspend fun detachFromAccount() = Unit

    fun put(type: SyncType, id: String, payload: String, at: Long, deleted: Boolean = false) {
        rows[type to id] = Row(SyncRecord(type, id, at, deleted, payload), dirty = true)
    }

    fun payload(type: SyncType, id: String): JsonElement? = rows[type to id]?.takeIf { !it.record.deleted }?.let { SyncJson.parseToJsonElement(it.record.payload) }
}

private class Phone(val server: FakeFirestore, uid: String = "u1") {
    val clock = FixedClock(server.serverMicros / 1_000)
    val sessions = SessionManagerImpl(InMemorySecureStore())
    private val firebase = FakeFirebase { _, _ -> error("no token refresh expected") }
    val remote = FirestoreSyncRemote(
        AuthorizedApi(server.transport(), FirebaseSession(FirebaseAuthApi(FirebaseConfig("k"), firebase), sessions, clock)),
        "nixura",
        sessions,
        clock,
    )
    val local = MemoryStore()
    val engine = SyncEngine(local, remote, sessions, clock)
    private val session = storedSession(access = "tok-$uid", userId = uid, expiresAt = Long.MAX_VALUE / 2)

    suspend fun signIn() = sessions.signIn(session)
}

class FirestoreSyncRemoteTest {
    private val server = FakeFirestore { bearer -> bearer?.removePrefix("tok-") }

    private fun habit(name: String) = SyncJson.encodeToString(
        HabitPayload.serializer(),
        HabitPayload(name, createdAt = 10, colorArgb = 0xFFAE445AL, cost = HabitCostPayload("cigarette", "PIECE", 12.5, 2_500, 20, "EUR")),
    )

    private fun weekly(name: String) = SyncJson.encodeToString(GoodHabitPayload.serializer(), GoodHabitPayload(name, 1, null, 10, null, weeklyTarget = 3))
    private fun log(habitId: String, day: Long, count: Int) = SyncJson.encodeToString(GoodHabitLogPayload.serializer(), GoodHabitLogPayload(habitId, day, count))
    private fun relapse(habitId: String, at: Long) = SyncJson.encodeToString(HabitEventPayload.serializer(), HabitEventPayload(habitId, "RELAPSE", at))
    private fun event(title: String) = SyncJson.encodeToString(EventPayload.serializer(), EventPayload(title, "OTHER", 99, "[60]", 10))

    private val day = CivilCalendar.dayIndexFromCivil(2026, 11, 14)

    @Test fun everythingComesBackOnAnotherPhone() = runTest {
        val a = Phone(server).apply { signIn() }
        val now = a.clock.now
        a.local.put(SyncType.Habit, "h1", habit("Smoking"), now)
        a.local.put(SyncType.HabitEvent, "e1", relapse("h1", 50), now)
        a.local.put(SyncType.GoodHabit, "g1", weekly("Medicine"), now)
        a.local.put(SyncType.GoodHabitLog, "g1:$day", log("g1", day, 1), now)
        a.local.put(SyncType.Event, "ev1", event("Exam"), now)
        assertEquals(SyncOutcome.Synced, a.engine.syncNow())
        assertEquals(0, a.local.pendingCount())

        val b = Phone(server).apply { signIn() }
        assertEquals(SyncOutcome.Synced, b.engine.syncNow())
        for ((key, row) in a.local.rows) {
            assertEquals(SyncJson.parseToJsonElement(row.record.payload), b.local.payload(key.first, key.second), "$key")
        }
        // Weekly habits keep their goal.
        assertEquals("3", b.local.payload(SyncType.GoodHabit, "g1")!!.jsonObject["weeklyTarget"].toString())
        // Data is stored as readable fields, and the log day sits in its month document.
        val docs = server.docsOf("u1").keys.map { it.substringAfterLast('/') }
        assertTrue("habit~h1" in docs && "good_habit~g1" in docs && "goodlog~g1~2026-11" in docs, "$docs")
    }

    @Test fun anotherUsersDataIsNeverReached() = runTest {
        val a = Phone(server, uid = "u1").apply { signIn() }
        a.local.put(SyncType.Habit, "h1", habit("Smoking"), a.clock.now)
        a.engine.syncNow()
        val other = Phone(server, uid = "u2").apply { signIn() }
        assertEquals(SyncOutcome.Synced, other.engine.syncNow())
        assertTrue(other.local.rows.isEmpty())
        assertTrue(server.docsOf("u2").isEmpty())
    }

    @Test fun olderEditLosesAndThePhoneTakesTheNewerVersion() = runTest {
        val a = Phone(server).apply { signIn() }
        val b = Phone(server).apply { signIn() }
        a.local.put(SyncType.Habit, "h1", habit("Smoking"), 1_000)
        a.engine.syncNow()
        b.engine.syncNow()
        a.local.put(SyncType.Habit, "h1", habit("Smoking (new)"), 3_000)
        a.engine.syncNow()
        // b already pulled past nothing yet but edits with a clock behind a's: refused, then b ends up with a's version.
        b.local.put(SyncType.Habit, "h1", habit("Smoking (old)"), 2_000)
        assertEquals(SyncOutcome.Synced, b.engine.syncNow())
        assertEquals(SyncJson.parseToJsonElement(habit("Smoking (new)")), b.local.payload(SyncType.Habit, "h1"))
    }

    @Test fun oneStaleItemDoesNotBlockTheOthers() = runTest {
        val a = Phone(server).apply { signIn() }
        a.local.put(SyncType.Habit, "h1", habit("Newer"), 5_000)
        a.engine.syncNow()
        val b = Phone(server).apply { signIn() }
        val refused = b.remote.push(
            listOf(
                SyncRecord(SyncType.Habit, "h1", 1_000, false, habit("Stale")),
                SyncRecord(SyncType.Habit, "h2", 1_000, false, habit("Fresh 1")),
                SyncRecord(SyncType.Event, "ev1", 1_000, false, event("Fresh 2")),
            ),
        )
        assertIs<SyncRemoteResult.Ok<List<SyncRecord>>>(refused)
        assertEquals(listOf("h1"), refused.value.map { it.id })
        assertEquals(setOf("habit~h1", "habit~h2", "event~ev1"), server.docsOf("u1").keys.map { it.substringAfterLast('/') }.toSet())
        assertTrue(server.refusedCommits >= 1)
    }

    @Test fun refusedEditIsFetchedEvenWhenTheCursorIsPastIt() = runTest {
        val a = Phone(server).apply { signIn() }
        val b = Phone(server).apply { signIn() }
        a.local.put(SyncType.Habit, "h1", habit("Newer"), 5_000)
        a.engine.syncNow()
        b.engine.syncNow() // b now holds "Newer"
        server.serverMicros += 60_000_000 // a minute later, out of the re-read window
        b.engine.syncNow() // the cursor moves past it
        server.serverMicros += 60_000_000
        b.clock.now += 120_000
        val fetchesBefore = server.calls.count { it.endsWith(":batchGet") }
        b.local.put(SyncType.Habit, "h1", habit("Older clock"), 4_000)
        assertEquals(SyncOutcome.Synced, b.engine.syncNow())
        assertEquals(SyncJson.parseToJsonElement(habit("Newer")), b.local.payload(SyncType.Habit, "h1"))
        assertEquals(0, b.local.pendingCount())
        assertEquals(fetchesBefore + 1, server.calls.count { it.endsWith(":batchGet") })
    }

    @Test fun twoDaysOfOneMonthAreBothKeptAndAnOlderDayEditLoses() = runTest {
        val a = Phone(server).apply { signIn() }
        a.local.put(SyncType.GoodHabit, "g1", weekly("Run"), 1_000)
        a.local.put(SyncType.GoodHabitLog, "g1:$day", log("g1", day, 1), 1_000)
        a.local.put(SyncType.GoodHabitLog, "g1:${day - 1}", log("g1", day - 1, 2), 1_000)
        assertEquals(SyncOutcome.Synced, a.engine.syncNow())

        val b = Phone(server).apply { signIn() }
        b.engine.syncNow()
        assertEquals(2, b.local.rows.keys.count { it.first == SyncType.GoodHabitLog })
        b.local.put(SyncType.GoodHabitLog, "g1:$day", log("g1", day, 0), 500) // older than a's edit
        b.engine.syncNow()
        assertEquals("1", b.local.payload(SyncType.GoodHabitLog, "g1:$day")!!.jsonObject["count"].toString())
    }

    @Test fun deleteKeepsOnlyAMarkerAndReachesTheOtherPhone() = runTest {
        val a = Phone(server).apply { signIn() }
        val b = Phone(server).apply { signIn() }
        a.local.put(SyncType.Event, "ev1", event("Private title"), 1_000)
        a.engine.syncNow()
        b.engine.syncNow()
        a.local.put(SyncType.Event, "ev1", event("Private title"), 2_000, deleted = true)
        a.engine.syncNow()
        val doc = server.docsOf("u1").values.single()
        assertEquals("{}", doc["data"]!!.jsonObject["mapValue"]!!.jsonObject["fields"].toString())
        b.engine.syncNow()
        assertNull(b.local.payload(SyncType.Event, "ev1"))
    }

    @Test fun usedUpQuotaCountsAsOfflineAndKeepsTheChanges() = runTest {
        val a = Phone(server).apply { signIn() }
        a.local.put(SyncType.Habit, "h1", habit("Smoking"), 1_000)
        server.status = 429
        assertEquals(SyncOutcome.Offline, a.engine.syncNow())
        assertEquals(1, a.local.pendingCount())
        server.status = null
        assertEquals(SyncOutcome.Synced, a.engine.syncNow())
        assertEquals(0, a.local.pendingCount())
    }

    @Test fun guestsMakeNoNetworkCalls() = runTest {
        val guest = Phone(server) // never signed in
        guest.local.put(SyncType.Habit, "h1", habit("Smoking"), 1_000)
        assertEquals(SyncOutcome.NotSignedIn, guest.engine.syncNow())
        assertIs<SyncRemoteResult.SessionEnded>(guest.remote.push(guest.local.pendingRecords(10)))
        assertTrue(server.calls.isEmpty())
    }

    @Test fun anIdleSyncReadsNothingOnceTheWindowHasPassed() = runTest {
        val a = Phone(server).apply { signIn() }
        a.local.put(SyncType.Habit, "h1", habit("Smoking"), 1_000)
        a.engine.syncNow()
        server.serverMicros += 60_000_000
        a.engine.syncNow() // re-reads its own recent write once, then moves the cursor past it
        server.serverMicros += 60_000_000
        val page = a.remote.pull(a.local.cursor())
        assertIs<SyncRemoteResult.Ok<*>>(page)
        assertTrue((page.value as com.adhamelgendy.nixura.domain.sync.SyncPage).records.isEmpty())
    }

    @Test fun deletingTheAccountRemovesItsDocuments() = runTest {
        val a = Phone(server).apply { signIn() }
        a.local.put(SyncType.Habit, "h1", habit("Smoking"), 1_000)
        a.local.put(SyncType.GoodHabit, "g1", weekly("Run"), 1_000)
        a.local.put(SyncType.GoodHabitLog, "g1:$day", log("g1", day, 1), 1_000)
        a.engine.syncNow()
        val other = Phone(server, uid = "u2").apply { signIn() }
        other.local.put(SyncType.Habit, "x", habit("Other"), 1_000)
        other.engine.syncNow()
        assertTrue(a.remote.deleteEverything())
        assertTrue(server.docsOf("u1").isEmpty())
        assertEquals(1, server.docsOf("u2").size)
    }

    @Test fun aFastPhoneClockIsLoweredToTheServerTime() = runTest {
        val a = Phone(server).apply { signIn() }
        a.engine.syncNow() // learns the server's clock
        val future = a.clock.now + 3 * 3_600_000L
        a.local.put(SyncType.Habit, "h1", habit("Smoking"), future)
        a.engine.syncNow()
        val stored = server.docsOf("u1").values.single()["updatedAt"]!!.jsonObject["integerValue"].toString().trim('"').toLong()
        assertTrue(stored < future && stored <= server.serverMicros / 1_000, "$stored")
        assertEquals(0, a.local.pendingCount())
    }
}
