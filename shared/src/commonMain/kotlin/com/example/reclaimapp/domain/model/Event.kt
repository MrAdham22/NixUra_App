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
)

enum class EventCategory {
    PERSONAL,
    TRAVEL,
    WORK,
    HEALTH,
    CELEBRATION,
    OTHER,
}
