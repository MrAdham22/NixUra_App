package com.example.reclaimapp.domain.integration

/** A Google calendar, with the name Google gives it (for example "Holidays in Egypt"). */
data class CalendarChoice(val id: String, val name: String)

/** What NixUra imports from a linked Google account; [key] is what [IntegrationState.selection] stores. */
enum class GoogleFeed(val key: String) {
    /** Events on the user's main calendar (not birthdays, focus time or out of office). */
    EVENTS("events"),
    BIRTHDAYS("birthdays"),

    /** Google Tasks that have a due date. */
    TASKS("tasks"),

    /** Google's public holiday calendar for the user's country. */
    HOLIDAYS("holidays");

    companion object {
        fun of(key: String): GoogleFeed? = entries.firstOrNull { it.key == key }

        /** Picked for a new link: the user's own things. Holidays are opt-in. */
        val defaults: List<GoogleFeed> = listOf(EVENTS, BIRTHDAYS, TASKS)
    }
}

/** Keys of [IntegrationState.mapping] for Google: the holiday calendar found for the user's country. */
object GoogleMapping {
    const val HOLIDAY_CALENDAR = "holidayCalendar"
    const val HOLIDAY_NAME = "holidayName"
}

/** The country the phone is in (ISO 3166 alpha-2, for example "EG"), or null when unknown. */
fun interface DeviceRegion {
    fun countryCode(): String?
}

/** A Notion database shared with NixUra, with the properties that can be its title and its date. */
data class NotionDatabaseChoice(
    val id: String,
    val title: String,
    val titleProperties: List<String>,
    val dateProperties: List<String>,
)

data class NotionLinkStatus(val connected: Boolean, val workspace: String)

/** Answer from a linking call. */
sealed interface LinkReply<out T> {
    data class Ok<T>(val value: T) : LinkReply<T>
    data object NotConnected : LinkReply<Nothing>
    data object Offline : LinkReply<Nothing>
    data object Failed : LinkReply<Nothing>
}

interface GoogleCalendarCatalog {
    /**
     * Google's public holiday calendar for the user's country: Ok(null) when there is none, Offline or Failed when
     * Google cannot be asked, NotConnected when access is missing.
     */
    suspend fun holidayCalendar(): LinkReply<CalendarChoice?>
}

/** Notion linking, done by NixUra's server (it holds the client secret and the user's Notion token). */
interface NotionLink {
    suspend fun startUrl(): LinkReply<String>
    suspend fun status(): LinkReply<NotionLinkStatus>
    suspend fun databases(): LinkReply<List<NotionDatabaseChoice>>
}

/** Keys of [IntegrationState.mapping] for a Notion database. */
object NotionMapping {
    const val TITLE = "title"
    const val DATE = "date"
}
