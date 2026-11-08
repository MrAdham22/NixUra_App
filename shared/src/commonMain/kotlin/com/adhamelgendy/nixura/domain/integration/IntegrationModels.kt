package com.adhamelgendy.nixura.domain.integration

import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.model.EventSource

/** A calendar the user can link. Imported events carry the matching [EventSource]. */
enum class IntegrationProvider(val source: EventSource) {
    GOOGLE(EventSource.GOOGLE),
    NOTION(EventSource.NOTION),
}

/**
 * One event as the source reports it. [externalId] is unique within the provider and stable across edits;
 * [cancelled] = deleted or cancelled at the source. [category] = what the source says it is (a Google birthday or
 * task); null = Other. It is set on import only: a category the user picked in NixUra is kept.
 */
data class ExternalEvent(
    val externalId: String,
    val title: String,
    val startEpochMillis: Long,
    val allDay: Boolean,
    val updatedAtEpochMillis: Long,
    val cancelled: Boolean = false,
    val category: EventCategory? = null,
)

/**
 * A fetch from a source. [fullSync] = [events] is everything the source has in the import window, so anything
 * imported before and missing now was deleted there. [cursor] is what to pass next time (sync tokens, a timestamp).
 */
data class FetchResult(val events: List<ExternalEvent>, val fullSync: Boolean, val cursor: String?)

sealed interface FetchOutcome {
    data class Ok(val result: FetchResult) : FetchOutcome

    /** Access was revoked or expired and cannot be renewed silently: the user must connect again. */
    data object Unauthorized : FetchOutcome
    data object Offline : FetchOutcome

    /** Not set up yet (no calendar or database picked). */
    data object NotConfigured : FetchOutcome
    data class Failed(val reason: String) : FetchOutcome
}

/** Reads events from one linked provider. Implementations never log tokens or event text. */
interface CalendarSource {
    val provider: IntegrationProvider
    suspend fun fetch(state: IntegrationState): FetchOutcome
}

/** Why the last sync did not work, as a code the UI turns into a clear message. */
enum class IntegrationError { REVOKED, OFFLINE, NOT_CONFIGURED, FAILED }

/**
 * What the app remembers about one link, on this device. Tokens are not in here: Google access tokens are asked for
 * each time and Notion's stays on the server.
 *
 * @property selection Google: the [GoogleFeed] keys to import; Notion: the database id.
 * @property mapping Notion: property names for the title and the date; Google: the holiday calendar ([GoogleMapping]).
 */
data class IntegrationState(
    val connected: Boolean = false,
    val accountLabel: String = "",
    val selection: List<String> = emptyList(),
    val mapping: Map<String, String> = emptyMap(),
    val cursor: String? = null,
    val lastSyncedAt: Long? = null,
    val lastError: IntegrationError? = null,
)

interface IntegrationStateStore {
    suspend fun get(provider: IntegrationProvider): IntegrationState
    suspend fun save(provider: IntegrationProvider, state: IntegrationState)
}

/**
 * An event row already imported from a provider (deleted rows included, so a user's delete is remembered).
 * [removedByImport] = NixUra removed it itself (gone at the source, its entry turned off, Disconnect), not the user:
 * such a row comes back as soon as the source lists the event again.
 */
data class ImportedRow(
    val id: Long,
    val externalId: String,
    val externalUpdatedAt: Long?,
    val startEpochMillis: Long,
    val deletedAt: Long?,
    val removedByImport: Boolean = false,
)

/** Writes imported events into the app's events table. */
interface ImportedEventStore {
    suspend fun rows(provider: IntegrationProvider): List<ImportedRow>
    suspend fun apply(provider: IntegrationProvider, plan: ImportPlan, nowEpochMillis: Long)

    /** Disconnect: every event from [provider] goes (as a synced delete). */
    suspend fun removeAll(provider: IntegrationProvider, nowEpochMillis: Long)
}
