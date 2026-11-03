package com.example.reclaimapp.domain.integration

/** A Google calendar the user can pick. */
data class CalendarChoice(val id: String, val name: String, val primary: Boolean)

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
    /** The user's calendars, or null when Google cannot be reached or access is missing. */
    suspend fun calendars(): List<CalendarChoice>?
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
