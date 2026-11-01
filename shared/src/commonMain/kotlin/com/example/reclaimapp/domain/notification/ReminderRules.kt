package com.example.reclaimapp.domain.notification

/** Why a reminder lead time was rejected. The UI maps each value to a localized message. */
enum class ReminderError {
    /** Lead time is negative. */
    NEGATIVE,

    /** Lead time is longer than [ReminderRules.MAX_LEAD_MILLIS]. */
    TOO_FAR_AHEAD,

    /** The event itself is already over, so no new reminder can fire. */
    EVENT_PASSED,

    /** `event time - lead` is not in the future, so the reminder could never fire. */
    IN_PAST,

    /** The event already has this reminder. */
    DUPLICATE,

    /** The event already has [ReminderRules.MAX_REMINDERS_PER_EVENT] reminders. */
    TOO_MANY,
}

/** Thrown by [com.example.reclaimapp.domain.usecase.EventReminderEditor] when a change breaks a rule. */
class ReminderValidationException(val error: ReminderError) : Exception("Invalid reminder: $error")

/** Pure validation for event reminders (lead times, in milliseconds before the event). */
object ReminderRules {
    const val MAX_REMINDERS_PER_EVENT = 10
    const val MAX_LEAD_MILLIS = 365L * 24 * 60 * 60 * 1000

    /**
     * Validates [leadMillis] for an event that happens at [targetAtEpochMillis].
     *
     * @param otherLeads the event's other reminders. When editing a reminder, leave the edited one out.
     * @return the first rule that is broken, or `null` when the reminder is fine.
     */
    fun validate(
        leadMillis: Long,
        otherLeads: Collection<Long>,
        targetAtEpochMillis: Long,
        nowEpochMillis: Long,
    ): ReminderError? = when {
        leadMillis < 0L -> ReminderError.NEGATIVE
        leadMillis > MAX_LEAD_MILLIS -> ReminderError.TOO_FAR_AHEAD
        targetAtEpochMillis <= nowEpochMillis -> ReminderError.EVENT_PASSED
        targetAtEpochMillis - leadMillis <= nowEpochMillis -> ReminderError.IN_PAST
        leadMillis in otherLeads -> ReminderError.DUPLICATE
        otherLeads.size >= MAX_REMINDERS_PER_EVENT -> ReminderError.TOO_MANY
        else -> null
    }

    /** Drops invalid values, removes duplicates, sorts ascending and caps the list. Never throws. */
    fun normalize(leads: Collection<Long>): List<Long> =
        leads.filter { it in 0L..MAX_LEAD_MILLIS }.distinct().sorted().take(MAX_REMINDERS_PER_EVENT)
}
