package com.example.reclaimapp.domain.model

/** An important upcoming moment with a countdown and optional reminders. */
data class Event(
    val id: Long,
    val title: String,
    val category: EventCategory,
    val targetAtEpochMillis: Long,
    /** How long before [targetAtEpochMillis] to notify. `0` = at the event time. */
    val reminderLeadMillis: List<Long>,
    val createdAtEpochMillis: Long,
    /** Where it came from: made in the app, or imported from a linked calendar (edits happen there). */
    val source: EventSource = EventSource.LOCAL,
    /** A whole-day event: [targetAtEpochMillis] is local midnight of that day. */
    val allDay: Boolean = false,
)

enum class EventCategory {
    PERSONAL,
    TRAVEL,
    WORK,
    HEALTH,
    CELEBRATION,
    OTHER,
}

enum class EventSource { LOCAL, GOOGLE, NOTION }
