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
 */
data class PlannedNotification(
    val key: String,
    val triggerAtEpochMillis: Long,
    val title: String,
    val body: String,
    val kind: NotificationKind = NotificationKind.fromKey(key),
    val targetId: Long? = null,
    val theme: HabitTheme? = null,
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
