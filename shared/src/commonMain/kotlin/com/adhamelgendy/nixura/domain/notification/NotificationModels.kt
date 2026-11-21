package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.theme.HabitTheme

/**
 * One local notification to be shown at [triggerAtEpochMillis]. [key] is stable and unique per notification
 * (and is the OS notification identity, so posting the same key again replaces it instead of stacking).
 *
 * @param kind decides channel, look, grouping and tap target. Defaults to what the key prefix says, which is
 *   also how plans stored before phase B are read back.
 * @param targetId the event or habit the notification is about (a tap opens it), or `null`.
 * @param theme for [NotificationKind.GOOD_HABIT]: the habit's stored theme (water and prayer get their own look).
 * @param postKey the identity of the notification on screen. Several alarms (one per moment) may share it, so a new
 *   reminder for a habit replaces the one already showing instead of stacking. Defaults to [key].
 * @param actions buttons shown on the notification (phase C: "+1 glass", "Prayed", Snooze).
 * @param progress shown with the platform's own progress style ("3 / 8 glasses"); `null` = none.
 * @param timing how precisely the OS must fire it (see [NotificationTiming]).
 */
data class PlannedNotification(
    val key: String,
    val triggerAtEpochMillis: Long,
    val title: String,
    val body: String,
    val kind: NotificationKind = NotificationKind.fromKey(key),
    val targetId: Long? = null,
    val theme: HabitTheme? = null,
    val postKey: String = key,
    val actions: List<NotificationAction> = emptyList(),
    val progress: NotificationProgress? = null,
    val timing: NotificationTiming = NotificationTiming.ON_TIME,
)

/** How precisely a notification must fire. */
enum class NotificationTiming {
    /** Tied to a moment (an event, a prayer): exact when the user allows exact alarms (phase B's hybrid timing). */
    ON_TIME,

    /**
     * A nudge (water, a habit's chosen time): a few minutes either way is fine. Android arms it inside a short
     * window without the exact-alarm permission and without the status-bar alarm icon.
     */
    FLEXIBLE,
}

/** A count shown with the platform's progress style. */
data class NotificationProgress(val current: Int, val max: Int)

/** What a notification button does. Handled without opening the app (see `domain/reminder/ReminderActionHandler`). */
enum class NotificationActionType {
    /** +1 today, the same write as the widget's "+" (a glass, "Done"). */
    LOG_ONE,

    /** Marks prayer [NotificationAction.prayerIndex] (today's count becomes at least that index). */
    LOG_PRAYER,

    /** Shows the reminder again after [NotificationAction.snoozeMinutes]. */
    SNOOZE,
}

/** One button. [habitId] is the habit it writes to. */
data class NotificationAction(
    val type: NotificationActionType,
    val label: String,
    val habitId: Long,
    val prayerIndex: Int? = null,
    val snoozeMinutes: Int? = null,
)

/** Schedules local notifications. Each call replaces everything previously scheduled by the app. */
interface NotificationScheduler {
    fun replaceAll(planned: List<PlannedNotification>)
}

/** Runtime notification permission (Android 13+). iOS/older Android: always granted or no-op. */
interface NotificationPermissionGateway {
    fun isGranted(): Boolean

    /** Shows the system dialog. Call only after the user saw why (see [access]). */
    fun request()

    /** What the UI should do before relying on notifications (ask with a reason, or point to system settings). */
    fun access(): NotificationAccess = if (isGranted()) NotificationAccess.ALLOWED else NotificationAccess.ASK
}

object NoOpNotificationScheduler : NotificationScheduler {
    override fun replaceAll(planned: List<PlannedNotification>) = Unit
}

object AlwaysGrantedPermissionGateway : NotificationPermissionGateway {
    override fun isGranted(): Boolean = true
    override fun request() = Unit
}
