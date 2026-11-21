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

    /** Delivery records older than this can be forgotten: [select] never re-arms anything that late. */
    fun keepDeliveredSince(nowMillis: Long): Long = nowMillis - 2 * LATE_GRACE_MILLIS
}
