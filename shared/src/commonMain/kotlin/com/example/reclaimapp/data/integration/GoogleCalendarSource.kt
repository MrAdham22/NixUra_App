package com.example.reclaimapp.data.integration

import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.integration.CalendarChoice
import com.example.reclaimapp.domain.integration.CalendarSource
import com.example.reclaimapp.domain.integration.GoogleCalendarCatalog
import com.example.reclaimapp.domain.integration.ExternalEvent
import com.example.reclaimapp.domain.integration.FetchOutcome
import com.example.reclaimapp.domain.integration.FetchResult
import com.example.reclaimapp.domain.integration.GoogleAuthResult
import com.example.reclaimapp.domain.integration.GoogleAuthorizer
import com.example.reclaimapp.domain.integration.IntegrationProvider
import com.example.reclaimapp.domain.integration.IntegrationState
import com.example.reclaimapp.domain.integration.SyncIntegrationsUseCase
import com.example.reclaimapp.domain.time.DayCalculator
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Serializable
internal data class GCalendarList(val items: List<GCalendar> = emptyList())

@Serializable
internal data class GCalendar(val id: String, val summary: String = "", val primary: Boolean = false)

@Serializable
internal data class GEventTime(val date: String? = null, val dateTime: String? = null)

@Serializable
internal data class GEvent(
    val id: String,
    val status: String = "confirmed",
    val summary: String = "",
    val updated: String = "",
    val start: GEventTime? = null,
)

@Serializable
internal data class GEventPage(val items: List<GEvent> = emptyList(), val nextPageToken: String? = null, val nextSyncToken: String? = null)


/** A reply from Google, reduced to what the sync needs. */
internal sealed interface GoogleReply<out T> {
    data class Ok<T>(val value: T) : GoogleReply<T>
    data object Unauthorized : GoogleReply<Nothing>

    /** The sync token expired (HTTP 410): start a full fetch of that calendar again. */
    data object Gone : GoogleReply<Nothing>
    data object Offline : GoogleReply<Nothing>
    data object Failed : GoogleReply<Nothing>
}

/** Thin read-only Google Calendar v3 client. */
internal class GoogleCalendarApi(private val client: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun calendars(token: String): GoogleReply<List<CalendarChoice>> =
        get("$BASE/users/me/calendarList?minAccessRole=reader&maxResults=100", token) { text ->
            json.decodeFromString(GCalendarList.serializer(), text).items.map { CalendarChoice(it.id, it.summary, it.primary) }
        }

    suspend fun events(token: String, calendarId: String, syncToken: String?, timeMin: String, pageToken: String?): GoogleReply<GEventPage> {
        val q = buildString {
            append("singleEvents=true&showDeleted=true&maxResults=250") // recurring events come as their single instances
            if (syncToken != null) append("&syncToken=").append(syncToken.encodeURLParameter())
            else append("&timeMin=").append(timeMin.encodeURLParameter())
            if (pageToken != null) append("&pageToken=").append(pageToken.encodeURLParameter())
        }
        return get("$BASE/calendars/${calendarId.encodeURLParameter()}/events?$q", token) { json.decodeFromString(GEventPage.serializer(), it) }
    }

    private suspend fun <T> get(url: String, token: String, read: (String) -> T): GoogleReply<T> = try {
        val r = client.get(url) { header(HttpHeaders.Authorization, "Bearer $token") }
        when (r.status.value) {
            200 -> runCatching { GoogleReply.Ok(read(r.bodyAsText())) }.getOrElse { GoogleReply.Failed }
            401, 403 -> GoogleReply.Unauthorized
            410 -> GoogleReply.Gone
            else -> GoogleReply.Failed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        GoogleReply.Offline
    }

    private companion object {
        const val BASE = "https://www.googleapis.com/calendar/v3"
    }
}

/**
 * Imports the calendars the user picked. Incremental: each calendar keeps Google's sync token, so later syncs only
 * fetch changes (edits, cancellations); an expired token (410) or a new selection starts that calendar over. Deleting
 * "missing" events only happens after a full fetch of every picked calendar.
 */
internal class GoogleCalendarSource(
    private val auth: GoogleAuthorizer,
    private val api: GoogleCalendarApi,
    private val days: DayCalculator,
    private val clock: Clock,
) : CalendarSource, GoogleCalendarCatalog {
    override val provider = IntegrationProvider.GOOGLE
    private val cursorJson = Json
    private val cursorSerializer = MapSerializer(String.serializer(), String.serializer())

    override suspend fun fetch(state: IntegrationState): FetchOutcome {
        if (state.selection.isEmpty()) return FetchOutcome.NotConfigured
        val token = when (val a = auth.authorize(interactive = false)) {
            is GoogleAuthResult.Granted -> a.accessToken
            GoogleAuthResult.Offline -> return FetchOutcome.Offline
            GoogleAuthResult.NeedsConsent, GoogleAuthResult.Denied -> return FetchOutcome.Unauthorized
            GoogleAuthResult.NotAvailable -> return FetchOutcome.Failed("not_available")
        }
        val cursors = state.cursor?.let { runCatching { cursorJson.decodeFromString(cursorSerializer, it) }.getOrNull() }.orEmpty()
        val timeMin = Rfc3339.formatUtc(clock.nowEpochMillis() - SyncIntegrationsUseCase.WINDOW_PAST_MILLIS)
        val events = ArrayList<ExternalEvent>()
        val next = HashMap<String, String>()
        var allFull = true
        for (calendarId in state.selection) {
            var syncToken = cursors[calendarId]
            var restart = true
            while (restart) {
                restart = false
                val full = syncToken == null
                var page: String? = null
                var pages = 0
                do {
                    when (val r = api.events(token, calendarId, syncToken, timeMin, page)) {
                        is GoogleReply.Ok -> {
                            r.value.items.forEach { g -> toExternal(calendarId, g)?.let(events::add) }
                            page = r.value.nextPageToken
                            r.value.nextSyncToken?.let { next[calendarId] = it }
                        }
                        GoogleReply.Gone -> { syncToken = null; restart = true; page = null }
                        GoogleReply.Unauthorized -> return FetchOutcome.Unauthorized
                        GoogleReply.Offline -> return FetchOutcome.Offline
                        GoogleReply.Failed -> return FetchOutcome.Failed("google")
                    }
                } while (page != null && ++pages < MAX_PAGES)
                // Incremental, or a full fetch cut short: absence proves nothing, so nothing may be deleted for it.
                if (!restart && (!full || page != null)) allFull = false
            }
        }
        return FetchOutcome.Ok(FetchResult(events, fullSync = allFull, cursor = cursorJson.encodeToString(cursorSerializer, next)))
    }

    /** Lists the user's calendars, for the picker. */
    override suspend fun calendars(): List<CalendarChoice>? {
        val token = (auth.authorize(interactive = false) as? GoogleAuthResult.Granted)?.accessToken ?: return null
        return (api.calendars(token) as? GoogleReply.Ok)?.value
    }

    private fun toExternal(calendarId: String, g: GEvent): ExternalEvent? {
        val updated = Rfc3339.parseInstant(g.updated) ?: 0L
        val id = "$calendarId|${g.id}"
        if (g.status == "cancelled") return ExternalEvent(id, "", Long.MAX_VALUE, false, updated, cancelled = true)
        val start = g.start ?: return null
        val instant = start.dateTime?.let(Rfc3339::parseInstant)
        val allDay = instant == null
        val at = instant ?: start.date?.let(Rfc3339::parseDayIndex)?.let(days::startOfDayMillis) ?: return null
        return ExternalEvent(id, g.summary, at, allDay, updated)
    }

    private companion object {
        /** 250 events a page; a very busy calendar is finished on the next sync. */
        const val MAX_PAGES = 8
    }
}
