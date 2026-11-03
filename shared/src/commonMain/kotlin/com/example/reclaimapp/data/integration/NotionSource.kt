package com.example.reclaimapp.data.integration

import com.example.reclaimapp.data.auth.AuthorizedApi
import com.example.reclaimapp.data.auth.TransportResponse
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.integration.CalendarSource
import com.example.reclaimapp.domain.integration.ExternalEvent
import com.example.reclaimapp.domain.integration.FetchOutcome
import com.example.reclaimapp.domain.integration.FetchResult
import com.example.reclaimapp.domain.integration.IntegrationProvider
import com.example.reclaimapp.domain.integration.IntegrationState
import com.example.reclaimapp.domain.integration.LinkReply
import com.example.reclaimapp.domain.integration.NotionDatabaseChoice
import com.example.reclaimapp.domain.integration.NotionLink
import com.example.reclaimapp.domain.integration.NotionLinkStatus
import com.example.reclaimapp.domain.integration.NotionMapping
import com.example.reclaimapp.domain.time.DayCalculator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class NotionStartResponse(val url: String)

@Serializable
internal data class NotionStatusDto(val connected: Boolean = false, val workspace: String = "")

@Serializable
internal data class IntegrationsStatusResponse(val notion: NotionStatusDto = NotionStatusDto())

@Serializable
internal data class NotionDatabaseDto(
    val id: String,
    val title: String = "",
    val titleProperties: List<String> = emptyList(),
    val dateProperties: List<String> = emptyList(),
)

@Serializable
internal data class NotionDatabasesResponse(val databases: List<NotionDatabaseDto> = emptyList())

@Serializable
internal data class NotionQueryRequest(val databaseId: String, val titleProperty: String, val dateProperty: String, val since: Long? = null)

/** One page of the database as the server maps it: [start] epoch millis for a date-time, [allDayDate] for a date. */
@Serializable
internal data class NotionItem(
    val id: String,
    val title: String = "",
    val start: Long? = null,
    val allDayDate: String? = null,
    val lastEditedAt: Long = 0,
    val archived: Boolean = false,
)

@Serializable
internal data class NotionQueryResponse(val items: List<NotionItem> = emptyList(), val complete: Boolean = true)

/** Everything Notion goes through NixUra's server: the client secret and the user's Notion token never reach the phone. */
internal class NotionBackendClient(private val api: AuthorizedApi) : NotionLink {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun startUrl(): LinkReply<String> = read(api.call("POST", "/v1/integrations/notion/start", "{}")) {
        json.decodeFromString(NotionStartResponse.serializer(), it).url
    }

    override suspend fun status(): LinkReply<NotionLinkStatus> = read(api.call("GET", "/v1/integrations", null)) {
        json.decodeFromString(IntegrationsStatusResponse.serializer(), it).notion.let { n -> NotionLinkStatus(n.connected, n.workspace) }
    }

    override suspend fun databases(): LinkReply<List<NotionDatabaseChoice>> = read(api.call("GET", "/v1/integrations/notion/databases", null)) {
        json.decodeFromString(NotionDatabasesResponse.serializer(), it).databases.map { d ->
            NotionDatabaseChoice(d.id, ImportedText.clean(d.title), d.titleProperties, d.dateProperties)
        }
    }

    suspend fun disconnect(): LinkReply<Unit> = read(api.call("DELETE", "/v1/integrations/notion", null)) { }

    suspend fun query(request: NotionQueryRequest): LinkReply<NotionQueryResponse> =
        read(api.call("POST", "/v1/integrations/notion/query", json.encodeToString(NotionQueryRequest.serializer(), request))) {
            json.decodeFromString(NotionQueryResponse.serializer(), it)
        }

    private fun <T> read(r: TransportResponse, parse: (String) -> T): LinkReply<T> = when {
        r.isNetworkFailure -> LinkReply.Offline
        r.status == 409 -> LinkReply.NotConnected // the server has no (or a revoked) Notion link for this account
        r.status !in 200..299 -> LinkReply.Failed
        else -> runCatching { LinkReply.Ok(parse(r.body)) }.getOrElse { LinkReply.Failed }
    }
}

/**
 * Imports one Notion database. The first sync (and one every few days) reads all of it, which also catches deleted
 * pages; the others only ask for pages edited since the last sync (`last_edited_time`), slightly overlapped.
 */
internal class NotionCalendarSource(
    private val client: NotionBackendClient,
    private val days: DayCalculator,
    private val clock: Clock,
) : CalendarSource {
    override val provider = IntegrationProvider.NOTION

    override suspend fun fetch(state: IntegrationState): FetchOutcome {
        val database = state.selection.firstOrNull() ?: return FetchOutcome.NotConfigured
        val titleProp = state.mapping[NotionMapping.TITLE] ?: return FetchOutcome.NotConfigured
        val dateProp = state.mapping[NotionMapping.DATE] ?: return FetchOutcome.NotConfigured
        // Cursor: "<newest last_edited_time seen>:<time of the last full read>"
        val parts = state.cursor?.split(':')
        val newestSeen = parts?.getOrNull(0)?.toLongOrNull()
        val lastFull = parts?.getOrNull(1)?.toLongOrNull() ?: 0
        val now = clock.nowEpochMillis()
        val full = newestSeen == null || now - lastFull > FULL_EVERY_MILLIS
        val since = if (full || newestSeen == null) null else newestSeen - OVERLAP_MILLIS
        return when (val r = client.query(NotionQueryRequest(database, titleProp, dateProp, since))) {
            is LinkReply.Ok -> {
                val events = r.value.items.map(::toExternal)
                val newest = maxOf(r.value.items.maxOfOrNull { it.lastEditedAt } ?: 0L, newestSeen ?: 0L)
                val complete = full && r.value.complete
                FetchOutcome.Ok(FetchResult(events, fullSync = complete, cursor = "$newest:${if (complete) now else lastFull}"))
            }
            LinkReply.NotConnected -> FetchOutcome.Unauthorized
            LinkReply.Offline -> FetchOutcome.Offline
            LinkReply.Failed -> FetchOutcome.Failed("notion")
        }
    }

    private fun toExternal(n: NotionItem): ExternalEvent {
        // An archived page, or one whose date was cleared, is no longer an event.
        val at = if (n.archived) null else n.start ?: n.allDayDate?.let(Rfc3339::parseDayIndex)?.let(days::startOfDayMillis)
        return if (at == null) {
            ExternalEvent(n.id, "", Long.MAX_VALUE, false, n.lastEditedAt, cancelled = true)
        } else {
            ExternalEvent(n.id, n.title, at, allDay = n.start == null, updatedAtEpochMillis = n.lastEditedAt)
        }
    }

    private companion object {
        const val OVERLAP_MILLIS = 5 * 60_000L
        const val FULL_EVERY_MILLIS = 3 * 86_400_000L
    }
}
