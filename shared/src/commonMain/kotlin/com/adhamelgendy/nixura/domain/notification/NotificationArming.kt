package com.adhamelgendy.nixura.domain.notification

/**
 * Decides which planned notifications the OS scheduler arms. Pure, so the rules are unit-tested.
 *
 * The problem it solves: the OS may deliver an alarm later than its time (battery savers, OEM freezers,
 * inexact windows). The app re-plans often (every data change, every app start). A re-plan that simply dropped
 * everything whose time has passed would cancel such a late alarm before it fires, and the reminder would be lost.
 *
 * Rules:
 * - A notification in the future is armed.
 * - A notification whose time has passed is armed again (the OS then fires it at once) only if **all** hold:
 *   it is still planned (so a deleted event never fires), it was armed before with this exact time (so an event
 *   moved into the past does not fire at once), it was not delivered yet, and it is less than [LATE_GRACE_MILLIS] late.
 */
object NotificationArming {

    /** How late a not-yet-delivered notification may still be shown. Matches Android's widest inexact alarm window. */
    const val LATE_GRACE_MILLIS: Long = 60 * 60_000L

    /** The OS scheduler arms at most this many alarms (Android caps alarms per app). */
    const val MAX_ARMED: Int = 60

    /**
     * @param planned what should exist now (the planner already includes items up to [LATE_GRACE_MILLIS] late).
     * @param armed what was armed by the previous call (its stored plan).
     * @param delivered key → trigger time of each notification already shown.
     */
    fun select(
        planned: List<PlannedNotification>,
        armed: List<PlannedNotification>,
        delivered: Map<String, Long>,
        nowMillis: Long,
        limit: Int = MAX_ARMED,
    ): List<PlannedNotification> {
        val armedAt = armed.associate { it.key to it.triggerAtEpochMillis }
        return planned
            .filter { item ->
                val at = item.triggerAtEpochMillis
                when {
                    at > nowMillis -> delivered[item.key] != at
                    at <= nowMillis - LATE_GRACE_MILLIS -> false
                    else -> armedAt[item.key] == at && delivered[item.key] != at
                }
            }
            .distinctBy { it.key }
            .sortedBy { it.triggerAtEpochMillis }
            .take(limit)
    }

    /**
     * Hybrid timing (Android). Some phones (ColorOS) deliver ordinary exact alarms up to ~75% of the time left late
     * (capped at 1 h) and keep only alarm clocks exact, but an alarm clock shows the alarm icon and "next alarm".
     * So a reminder is armed as an alarm clock only once it is at most this close; before that it is an ordinary
     * exact alarm (on time on most phones, at worst an hour late), and one wake-up alarm at [handOffAt] re-arms it.
     *
     * Why 75 min: a wake-up armed far ahead for T - 75 min arrives by T - 15 min even with the 1 h widening, and one
     * armed close by arrives within 75% of the time left, also before T. The icon then shows 15-75 min before a reminder.
     */
    const val ALARM_CLOCK_LEAD_MILLIS: Long = 75 * 60_000L

    /** True when [triggerAt] is close enough to be armed as an alarm clock (or already due). */
    fun armAsAlarmClock(triggerAt: Long, nowMillis: Long): Boolean = triggerAt - nowMillis <= ALARM_CLOCK_LEAD_MILLIS

    /** When to wake up and hand the next far reminder over to an alarm clock, or `null` if none is far. */
    fun handOffAt(armed: List<PlannedNotification>, nowMillis: Long): Long? =
        armed.asSequence()
            .map { it.triggerAtEpochMillis }
            .filterNot { armAsAlarmClock(it, nowMillis) }
            .minOrNull()
            ?.minus(ALARM_CLOCK_LEAD_MILLIS)

    /** Delivery records older than this can be forgotten: [select] never re-arms anything that late. */
    fun keepDeliveredSince(nowMillis: Long): Long = nowMillis - 2 * LATE_GRACE_MILLIS
}
