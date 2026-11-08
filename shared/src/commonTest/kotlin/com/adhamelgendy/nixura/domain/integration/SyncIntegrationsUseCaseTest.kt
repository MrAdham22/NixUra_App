package com.adhamelgendy.nixura.domain.integration

import com.adhamelgendy.nixura.data.integration.ImportedText
import com.adhamelgendy.nixura.data.integration.Rfc3339
import com.adhamelgendy.nixura.domain.auth.GuestDataHandoff
import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.presentation.integrations.NotionPropertyGuess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncIntegrationsUseCaseTest {
    private val day = 86_400_000L
    private val now = 20_000 * day
    private val clock = object : Clock { override fun nowEpochMillis() = now }

    private class Sessions(signedIn: Boolean) : SessionManager {
        override val state: StateFlow<SessionState> = MutableStateFlow(
            if (signedIn) SessionState.SignedIn(com.adhamelgendy.nixura.domain.auth.AuthUser("u", "a@b.c", "", null, true)) else SessionState.SignedOut,
        )
        override val currentUserId: Flow<String?> = flowOf(null)
        override suspend fun pendingGuestHandoff(): GuestDataHandoff? = null
        override suspend fun markGuestHandoffHandled() = Unit
        override suspend fun forgetLastUser() = Unit
        override suspend fun hasLeftoverAccountData() = false
        override suspend fun signOutLocally() = Unit
    }

    private class Store : ImportedEventStore {
        val rows = mutableListOf<ImportedRow>()
        var nextId = 1L
        override suspend fun rows(provider: IntegrationProvider) = rows.toList()
        override suspend fun apply(provider: IntegrationProvider, plan: ImportPlan, nowEpochMillis: Long) {
            plan.upserts.forEach { e ->
                rows.removeAll { it.externalId == e.externalId }
                rows += ImportedRow(nextId++, e.externalId, e.updatedAtEpochMillis, e.startEpochMillis, null)
            }
            plan.deleteIds.forEach { id -> rows.replaceAll { if (it.id == id) it.copy(deletedAt = nowEpochMillis, removedByImport = true) else it } }
        }
        override suspend fun removeAll(provider: IntegrationProvider, nowEpochMillis: Long) = rows.clear()
    }

    private class States : IntegrationStateStore {
        val map = mutableMapOf<IntegrationProvider, IntegrationState>()
        override suspend fun get(provider: IntegrationProvider) = map[provider] ?: IntegrationState()
        override suspend fun save(provider: IntegrationProvider, state: IntegrationState) { map[provider] = state }
    }

    private class Source(var outcome: FetchOutcome) : CalendarSource {
        override val provider = IntegrationProvider.GOOGLE
        var calls = 0
        override suspend fun fetch(state: IntegrationState): FetchOutcome { calls++; return outcome }
    }

    private fun ev(id: String, cancelled: Boolean = false) = ExternalEvent(id, "Event $id", now + day, false, 1, cancelled)

    @Test
    fun importsThenAppliesDeletesFromTheSource() = runTest {
        val store = Store()
        val states = States().apply { map[IntegrationProvider.GOOGLE] = IntegrationState(connected = true, selection = listOf("primary")) }
        val source = Source(FetchOutcome.Ok(FetchResult(listOf(ev("a"), ev("b")), fullSync = true, cursor = "t1")))
        val sync = SyncIntegrationsUseCase(listOf(source), store, states, Sessions(true), clock)

        assertEquals(mapOf(IntegrationProvider.GOOGLE to null), sync())
        assertEquals(2, store.rows.size)
        assertEquals("t1", states.map[IntegrationProvider.GOOGLE]!!.cursor)
        assertEquals(now, states.map[IntegrationProvider.GOOGLE]!!.lastSyncedAt)

        source.outcome = FetchOutcome.Ok(FetchResult(listOf(ev("a", cancelled = true)), fullSync = false, cursor = "t2"))
        sync()
        assertTrue(store.rows.first { it.externalId == "a" }.deletedAt != null)
        assertNull(store.rows.first { it.externalId == "b" }.deletedAt)
    }

    @Test
    fun guestsAndUnlinkedProvidersMakeNoCalls() = runTest {
        val source = Source(FetchOutcome.Ok(FetchResult(emptyList(), true, null)))
        val linked = States().apply { map[IntegrationProvider.GOOGLE] = IntegrationState(connected = true) }
        SyncIntegrationsUseCase(listOf(source), Store(), linked, Sessions(signedIn = false), clock)()
        SyncIntegrationsUseCase(listOf(source), Store(), States(), Sessions(signedIn = true), clock)()
        assertEquals(0, source.calls)
    }

    @Test
    fun revokedAccessIsReportedAndKeepsTheImportedEvents() = runTest {
        val store = Store().apply { rows += ImportedRow(1, "a", 1, now + day, null) }
        val states = States().apply { map[IntegrationProvider.GOOGLE] = IntegrationState(connected = true) }
        val result = SyncIntegrationsUseCase(listOf(Source(FetchOutcome.Unauthorized)), store, states, Sessions(true), clock)()
        assertEquals(IntegrationError.REVOKED, result[IntegrationProvider.GOOGLE])
        assertEquals(IntegrationError.REVOKED, states.map[IntegrationProvider.GOOGLE]!!.lastError)
        assertEquals(1, store.rows.size)
    }

    @Test
    fun automaticRunsRespectTheInterval() = runTest {
        val source = Source(FetchOutcome.Ok(FetchResult(emptyList(), true, null)))
        val states = States().apply { map[IntegrationProvider.GOOGLE] = IntegrationState(connected = true, lastSyncedAt = now - 60_000) }
        val sync = SyncIntegrationsUseCase(listOf(source), Store(), states, Sessions(true), clock)
        sync(skipIfSyncedWithinMillis = SyncIntegrationsUseCase.AUTO_SYNC_EVERY_MILLIS)
        assertEquals(0, source.calls)
        sync() // "Sync now"
        assertEquals(1, source.calls)
    }

    @Test
    fun disconnectRemovesEventsAndTheLink() = runTest {
        val store = Store().apply { rows += ImportedRow(1, "a", 1, now, null) }
        val states = States().apply { map[IntegrationProvider.NOTION] = IntegrationState(connected = true) }
        var revoked = false
        DisconnectIntegrationUseCase({ revoked = true }, store, states, clock)(IntegrationProvider.NOTION)
        assertTrue(revoked)
        assertTrue(store.rows.isEmpty())
        assertEquals(IntegrationState(), states.map[IntegrationProvider.NOTION])
    }

    @Test
    fun calendarDatesParse() {
        assertEquals(1_762_327_800_000, Rfc3339.parseInstant("2025-11-05T09:30:00+02:00"))
        assertEquals(1_762_327_800_000, Rfc3339.parseInstant("2025-11-05T07:30:00Z"))
        assertEquals(1_762_327_800_250, Rfc3339.parseInstant("2025-11-05T07:30:00.250Z"))
        assertNull(Rfc3339.parseInstant("2025-11-05"))
        assertEquals(20_397, Rfc3339.parseDayIndex("2025-11-05"))
        assertNull(Rfc3339.parseDayIndex("2025-02-30"))
        assertEquals("2025-11-05T07:30:00Z", Rfc3339.formatUtc(1_762_327_800_000))
    }

    @Test
    fun importedTextIsCleaned() {
        assertEquals("Team sync", ImportedText.clean("  Team\n\u0007 sync  "))
        assertEquals("Untitled", ImportedText.clean("   "))
        assertEquals(120, ImportedText.clean("x".repeat(500)).length)
    }

    @Test
    fun theDatePropertyIsGuessed() {
        assertEquals("Due", NotionPropertyGuess.dateProperty(listOf("Created", "Due")))
        assertEquals("Event date", NotionPropertyGuess.dateProperty(listOf("Created", "Event date")))
        assertEquals("Created", NotionPropertyGuess.dateProperty(listOf("Created")))
        assertNull(NotionPropertyGuess.dateProperty(emptyList()))
    }
}
