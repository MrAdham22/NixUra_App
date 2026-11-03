package com.example.reclaimapp.domain.integration

import com.example.reclaimapp.domain.auth.SessionManager
import com.example.reclaimapp.domain.auth.SessionState
import com.example.reclaimapp.domain.clock.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Ends a link at the source: Google revokes the grant, Notion's token is deleted on the server. */
fun interface IntegrationRevoker {
    suspend fun revoke(provider: IntegrationProvider)
}

/**
 * Imports events from every linked calendar. Runs from the Integrations screen ("Sync now"), when the app comes to the
 * front and from the background worker. Linked calendars need an account: guests make no network calls at all.
 * One run at a time, so a background run and a tap never import the same events twice.
 */
class SyncIntegrationsUseCase(
    private val sources: List<CalendarSource>,
    private val store: ImportedEventStore,
    private val states: IntegrationStateStore,
    private val sessions: SessionManager,
    private val clock: Clock,
) {
    private val lock = Mutex()

    /**
     * [skipIfSyncedWithinMillis]: for automatic runs (app opened, background), so providers' rate limits and the
     * battery are respected; a tap on "Sync now" passes null.
     */
    suspend operator fun invoke(
        only: IntegrationProvider? = null,
        skipIfSyncedWithinMillis: Long? = null,
    ): Map<IntegrationProvider, IntegrationError?> = lock.withLock {
        if (sessions.state.value !is SessionState.SignedIn) return@withLock emptyMap()
        val results = LinkedHashMap<IntegrationProvider, IntegrationError?>()
        for (source in sources) {
            if (only != null && source.provider != only) continue
            val state = states.get(source.provider)
            if (!state.connected) continue
            val last = state.lastSyncedAt
            if (skipIfSyncedWithinMillis != null && last != null && clock.nowEpochMillis() - last < skipIfSyncedWithinMillis) continue
            results[source.provider] = syncOne(source, state)
        }
        results
    }

    private suspend fun syncOne(source: CalendarSource, state: IntegrationState): IntegrationError? {
        val now = clock.nowEpochMillis()
        val outcome = try {
            source.fetch(state)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FetchOutcome.Failed("unexpected")
        }
        val error = when (outcome) {
            is FetchOutcome.Ok -> {
                val plan = ImportPlanner.plan(
                    existing = store.rows(source.provider),
                    incoming = outcome.result.events,
                    fullSync = outcome.result.fullSync,
                    windowStartEpochMillis = now - WINDOW_PAST_MILLIS,
                    windowEndEpochMillis = now + WINDOW_FUTURE_MILLIS,
                )
                if (!plan.isEmpty) store.apply(source.provider, plan, now)
                states.save(source.provider, state.copy(cursor = outcome.result.cursor, lastSyncedAt = now, lastError = null))
                return null
            }
            FetchOutcome.Unauthorized -> IntegrationError.REVOKED
            FetchOutcome.Offline -> IntegrationError.OFFLINE
            FetchOutcome.NotConfigured -> IntegrationError.NOT_CONFIGURED
            is FetchOutcome.Failed -> IntegrationError.FAILED
        }
        states.save(source.provider, state.copy(lastError = error))
        return error
    }

    companion object {
        /** Imported: from yesterday to a year ahead. */
        const val WINDOW_PAST_MILLIS = 86_400_000L
        const val WINDOW_FUTURE_MILLIS = 366L * 86_400_000L

        /** Automatic syncs are at most this often. */
        const val AUTO_SYNC_EVERY_MILLIS = 15 * 60_000L
    }
}

/** Disconnect: revoke at the source, forget the link on this device and remove every event it imported. */
class DisconnectIntegrationUseCase(
    private val revoker: IntegrationRevoker,
    private val store: ImportedEventStore,
    private val states: IntegrationStateStore,
    private val clock: Clock,
) {
    suspend operator fun invoke(provider: IntegrationProvider): Result<Unit> = runCatching {
        // Revoking can fail offline; the local link and events still go, and the server forgets Notion on its own call.
        runCatching { revoker.revoke(provider) }
        store.removeAll(provider, clock.nowEpochMillis())
        states.save(provider, IntegrationState())
    }
}
