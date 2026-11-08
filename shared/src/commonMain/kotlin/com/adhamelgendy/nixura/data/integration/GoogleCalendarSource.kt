package com.adhamelgendy.nixura.data.integration

import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.integration.CalendarChoice
import com.adhamelgendy.nixura.domain.integration.CalendarSource
import com.adhamelgendy.nixura.domain.integration.DeviceRegion
import com.adhamelgendy.nixura.domain.integration.ExternalEvent
import com.adhamelgendy.nixura.domain.integration.FetchOutcome
import com.adhamelgendy.nixura.domain.integration.FetchResult
import com.adhamelgendy.nixura.domain.integration.GoogleAuthResult
import com.adhamelgendy.nixura.domain.integration.GoogleAuthorizer
import com.adhamelgendy.nixura.domain.integration.GoogleCalendarCatalog
import com.adhamelgendy.nixura.domain.integration.GoogleFeed
import com.adhamelgendy.nixura.domain.integration.GoogleMapping
import com.adhamelgendy.nixura.domain.integration.IntegrationProvider
import com.adhamelgendy.nixura.domain.integration.IntegrationState
import com.adhamelgendy.nixura.domain.integration.LinkReply
import com.adhamelgendy.nixura.domain.integration.SyncIntegrationsUseCase
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.time.DayCalculator
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
internal data class GCalendar(val id: String, val summary: String = "")

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

@Serializable
internal data class GTaskList(val id: String)

@Serializable
internal data class GTaskListPage(val items: List<GTaskList> = emptyList(), val nextPageToken: String? = null)

/** A Google task. [due] is a date only: Google drops the time part. */
@Serializable
internal data class GTask(
    val id: String,
    val title: String = "",
    val status: String = "needsAction",
    val due: String? = null,
    val updated: String = "",
    val deleted: Boolean = false,
)

@Serializable
internal data class GTaskPage(val items: List<GTask> = emptyList(), val nextPageToken: String? = null)

/** A reply from Google, reduced to what the sync needs. */
internal sealed interface GoogleReply<out T> {
    data class Ok<T>(val value: T) : GoogleReply<T>
    data object Unauthorized : GoogleReply<Nothing>

    /** HTTP 403: the API is off in the Google Cloud project, or the user did not allow this part. */
    data object Forbidden : GoogleReply<Nothing>
    data object NotFound : GoogleReply<Nothing>

    /** The sync token expired (HTTP 410): start a full fetch of that calendar again. */
    data object Gone : GoogleReply<Nothing>
    data object Offline : GoogleReply<Nothing>
    data object Failed : GoogleReply<Nothing>
}

/** Thin read-only client for Google Calendar v3 and Google Tasks v1. */
internal class GoogleCalendarApi(private val client: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun calendars(token: String): GoogleReply<List<CalendarChoice>> =
        get("$CALENDAR/users/me/calendarList?minAccessRole=reader&maxResults=100", token) { text ->
            json.decodeFromString(GCalendarList.serializer(), text).items.map { CalendarChoice(it.id, it.summary) }
        }

    /** One calendar's name; works for public calendars the user does not follow (Google's holiday calendars). */
    suspend fun calendar(token: String, calendarId: String): GoogleReply<CalendarChoice> =
        get("$CALENDAR/calendars/${calendarId.encodeURLParameter()}", token) { text ->
            json.decodeFromString(GCalendar.serializer(), text).let { CalendarChoice(it.id, it.summary) }
        }

    /** [eventTypes] empty = every type (Google's default). */
    suspend fun events(
        token: String,
        calendarId: String,
        eventTypes: List<String>,
        syncToken: String?,
        timeMin: String,
        pageToken: String?,
    ): GoogleReply<GEventPage> {
        val q = buildString {
            append("singleEvents=true&showDeleted=true&maxResults=250") // recurring events come as their single instances
            eventTypes.forEach { append("&eventTypes=").append(it) }
            if (syncToken != null) append("&syncToken=").append(syncToken.encodeURLParameter())
            else append("&timeMin=").append(timeMin.encodeURLParameter())
            if (pageToken != null) append("&pageToken=").append(pageToken.encodeURLParameter())
        }
        return get("$CALENDAR/calendars/${calendarId.encodeURLParameter()}/events?$q", token) { json.decodeFromString(GEventPage.serializer(), it) }
    }

    suspend fun taskLists(token: String, pageToken: String?): GoogleReply<GTaskListPage> {
        val page = pageToken?.let { "&pageToken=${it.encodeURLParameter()}" }.orEmpty()
        return get("$TASKS/users/@me/lists?maxResults=100$page", token) { json.decodeFromString(GTaskListPage.serializer(), it) }
    }

    /** Completed, cleared and deleted tasks are included so the import can remove them. */
    suspend fun tasks(token: String, listId: String, dueMin: String, pageToken: String?): GoogleReply<GTaskPage> {
        val q = buildString {
            append("showCompleted=true&showHidden=true&showDeleted=true&maxResults=100")
            append("&dueMin=").append(dueMin.encodeURLParameter())
            if (pageToken != null) append("&pageToken=").append(pageToken.encodeURLParameter())
        }
        return get("$TASKS/lists/${listId.encodeURLParameter()}/tasks?$q", token) { json.decodeFromString(GTaskPage.serializer(), it) }
    }

    private suspend fun <T> get(url: String, token: String, read: (String) -> T): GoogleReply<T> = try {
        val r = client.get(url) { header(HttpHeaders.Authorization, "Bearer $token") }
        when (r.status.value) {
            200 -> runCatching { GoogleReply.Ok(read(r.bodyAsText())) }.getOrElse { GoogleReply.Failed }
            401 -> GoogleReply.Unauthorized
            403 -> GoogleReply.Forbidden
            404 -> GoogleReply.NotFound
            410 -> GoogleReply.Gone
            else -> GoogleReply.Failed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        GoogleReply.Offline
    }

    private companion object {
        const val CALENDAR = "https://www.googleapis.com/calendar/v3"
        const val TASKS = "https://tasks.googleapis.com/tasks/v1"
    }
}

/** Pure mapping rules for Google data (unit-tested). */
internal object GoogleImport {
    private const val HOLIDAY_SUFFIX = "#holiday@group.v.calendar.google.com"

    /** Google's public holiday calendar for a country, for example "EG" → `en.eg#holiday@group.v.calendar.google.com`. */
    fun holidayCalendarId(countryCode: String): String = "en.${countryCode.lowercase()}$HOLIDAY_SUFFIX"

    fun isHolidayCalendar(calendarId: String): Boolean = calendarId.endsWith(HOLIDAY_SUFFIX)

    /** Event types of [GoogleFeed.EVENTS]: what the user put on the calendar, plus events Gmail added. */
    val ownEventTypes: List<String> = listOf("default", "fromGmail")

    fun event(feed: GoogleFeed, g: GEvent, startOfDay: (Long) -> Long): ExternalEvent? {
        val updated = Rfc3339.parseInstant(g.updated) ?: 0L
        val id = "${feed.key}|${g.id}"
        if (g.status == "cancelled") return ExternalEvent(id, "", Long.MAX_VALUE, false, updated, cancelled = true)
        val start = g.start ?: return null
        val instant = start.dateTime?.let(Rfc3339::parseInstant)
        val at = instant ?: start.date?.let(Rfc3339::parseDayIndex)?.let(startOfDay) ?: return null
        return ExternalEvent(id, g.summary, at, allDay = instant == null, updatedAtEpochMillis = updated, category = category(feed))
    }

    /** A task becomes an all-day event on its due date; a done or deleted task is removed; no due date = not imported. */
    fun task(listId: String, t: GTask, startOfDay: (Long) -> Long): ExternalEvent? {
        val updated = Rfc3339.parseInstant(t.updated) ?: 0L
        val id = "${GoogleFeed.TASKS.key}|$listId|${t.id}"
        if (t.deleted || t.status == "completed") return ExternalEvent(id, "", Long.MAX_VALUE, true, updated, cancelled = true)
        val day = t.due?.take(10)?.let(Rfc3339::parseDayIndex) ?: return null
        return ExternalEvent(id, t.title, startOfDay(day), allDay = true, updatedAtEpochMillis = updated, category = EventCategory.TASK)
    }

    fun category(feed: GoogleFeed): EventCategory = when (feed) {
        GoogleFeed.EVENTS -> EventCategory.EVENT
        GoogleFeed.BIRTHDAYS -> EventCategory.BIRTHDAY
        GoogleFeed.TASKS -> EventCategory.TASK
        GoogleFeed.HOLIDAYS -> EventCategory.HOLIDAY
    }
}

/**
 * Imports the [GoogleFeed]s the user picked. Calendar feeds are incremental: each keeps Google's sync token, so later
 * syncs only fetch changes; an expired token (410) or a new selection starts that feed over. Tasks have no sync token
 * and are read in full every time. Deleting "missing" events only happens after a full read of every picked feed.
 */
internal class GoogleCalendarSource(
    private val auth: GoogleAuthorizer,
    private val api: GoogleCalendarApi,
    private val days: DayCalculator,
    private val clock: Clock,
    private val region: DeviceRegion,
) : CalendarSource, GoogleCalendarCatalog {
    override val provider = IntegrationProvider.GOOGLE
    private val cursorJson = Json
    private val cursorSerializer = MapSerializer(String.serializer(), String.serializer())

    override suspend fun fetch(state: IntegrationState): FetchOutcome {
        val feeds = state.selection.mapNotNull(GoogleFeed::of)
        // Everything turned off: a full, empty read, so what the last entry imported is removed too.
        if (feeds.isEmpty()) return FetchOutcome.Ok(FetchResult(emptyList(), fullSync = true, cursor = null))
        val token = when (val a = auth.authorize(interactive = false)) {
            is GoogleAuthResult.Granted -> a.accessToken
            GoogleAuthResult.Offline -> return FetchOutcome.Offline
            GoogleAuthResult.NeedsConsent, GoogleAuthResult.Cancelled, GoogleAuthResult.Denied -> return FetchOutcome.Unauthorized
            GoogleAuthResult.NotAvailable -> return FetchOutcome.Failed("not_available")
        }
        val cursors = state.cursor?.let { runCatching { cursorJson.decodeFromString(cursorSerializer, it) }.getOrNull() }.orEmpty()
        val timeMin = Rfc3339.formatUtc(clock.nowEpochMillis() - SyncIntegrationsUseCase.WINDOW_PAST_MILLIS)
        val run = Run(token, timeMin, cursors)
        for (feed in feeds) {
            val stop = when (feed) {
                GoogleFeed.EVENTS -> run.calendar(feed, PRIMARY, GoogleImport.ownEventTypes)
                GoogleFeed.BIRTHDAYS -> run.calendar(feed, PRIMARY, listOf("birthday"))
                GoogleFeed.HOLIDAYS -> state.mapping[GoogleMapping.HOLIDAY_CALENDAR]?.let { run.calendar(feed, it, emptyList()) }
                GoogleFeed.TASKS -> run.tasks()
            }
            if (stop != null) {
                if (stop == FetchOutcome.Unauthorized) auth.forget(token)
                return stop
            }
        }
        return FetchOutcome.Ok(FetchResult(run.events, fullSync = run.allFull, cursor = cursorJson.encodeToString(cursorSerializer, run.next)))
    }

    /** One sync's work in progress; each step returns an outcome only when the whole sync has to stop. */
    private inner class Run(val token: String, val timeMin: String, val cursors: Map<String, String>) {
        val events = ArrayList<ExternalEvent>()
        val next = HashMap<String, String>()
        var allFull = true

        suspend fun calendar(feed: GoogleFeed, calendarId: String, eventTypes: List<String>): FetchOutcome? {
            var syncToken = cursors[feed.key]
            var restart = true
            while (restart) {
                restart = false
                val full = syncToken == null
                var page: String? = null
                var pages = 0
                do {
                    when (val r = api.events(token, calendarId, eventTypes, syncToken, timeMin, page)) {
                        is GoogleReply.Ok -> {
                            r.value.items.forEach { g -> GoogleImport.event(feed, g, days::startOfDayMillis)?.let(events::add) }
                            page = r.value.nextPageToken
                            r.value.nextSyncToken?.let { next[feed.key] = it }
                        }
                        GoogleReply.Gone -> { syncToken = null; restart = true; page = null }
                        GoogleReply.Unauthorized, GoogleReply.Forbidden -> return FetchOutcome.Unauthorized
                        GoogleReply.Offline -> return FetchOutcome.Offline
                        GoogleReply.NotFound, GoogleReply.Failed -> return FetchOutcome.Failed("google")
                    }
                } while (page != null && ++pages < MAX_PAGES)
                // Incremental, or a full fetch cut short: absence proves nothing, so nothing may be deleted for it.
                if (!restart && (!full || page != null)) allFull = false
            }
            return null
        }

        /** Tasks the account cannot read (Tasks API off, or not allowed on the consent screen) are skipped, not fatal. */
        suspend fun tasks(): FetchOutcome? {
            val lists = ArrayList<String>()
            var page: String? = null
            var pages = 0
            do {
                when (val r = api.taskLists(token, page)) {
                    is GoogleReply.Ok -> { r.value.items.mapTo(lists) { it.id }; page = r.value.nextPageToken }
                    GoogleReply.Unauthorized -> return FetchOutcome.Unauthorized
                    GoogleReply.Offline -> return FetchOutcome.Offline
                    else -> { allFull = false; return null }
                }
            } while (page != null && ++pages < MAX_PAGES)
            if (page != null) allFull = false
            for (listId in lists) {
                page = null
                pages = 0
                do {
                    when (val r = api.tasks(token, listId, timeMin, page)) {
                        is GoogleReply.Ok -> {
                            r.value.items.forEach { t -> GoogleImport.task(listId, t, days::startOfDayMillis)?.let(events::add) }
                            page = r.value.nextPageToken
                        }
                        GoogleReply.Unauthorized -> return FetchOutcome.Unauthorized
                        GoogleReply.Offline -> return FetchOutcome.Offline
                        else -> { allFull = false; page = null }
                    }
                } while (page != null && ++pages < MAX_PAGES)
                if (page != null) allFull = false
            }
            return null
        }
    }

    /**
     * Google's holiday calendar for the country the phone is in; else the holiday calendar the account follows. Google's
     * services can hand out a cached token that was revoked (after Disconnect, for about an hour), so a rejected token is
     * dropped and Google is asked once more.
     */
    override suspend fun holidayCalendar(): LinkReply<CalendarChoice?> {
        for (attempt in 0..1) {
            val token = (auth.authorize(interactive = false) as? GoogleAuthResult.Granted)?.accessToken ?: return LinkReply.NotConnected
            val country = region.countryCode()?.takeIf { it.length == 2 }
            if (country != null) {
                when (val r = api.calendar(token, GoogleImport.holidayCalendarId(country))) {
                    is GoogleReply.Ok -> return LinkReply.Ok(r.value)
                    GoogleReply.Unauthorized, GoogleReply.Forbidden -> { auth.forget(token); continue }
                    GoogleReply.Offline -> return LinkReply.Offline
                    else -> Unit // no holiday calendar under that id: try the account's own below
                }
            }
            return when (val r = api.calendars(token)) {
                is GoogleReply.Ok -> LinkReply.Ok(r.value.firstOrNull { GoogleImport.isHolidayCalendar(it.id) })
                GoogleReply.Unauthorized, GoogleReply.Forbidden -> { auth.forget(token); continue }
                GoogleReply.Offline -> LinkReply.Offline
                else -> LinkReply.Failed
            }
        }
        return LinkReply.NotConnected
    }

    private companion object {
        const val PRIMARY = "primary"

        /** 250 events (or 100 tasks) a page; a very busy calendar is finished on the next sync. */
        const val MAX_PAGES = 8
    }
}
