package com.example.reclaimapp.domain.notification

/** One local notification to be shown at [triggerAtEpochMillis]. [key] is stable and unique per notification. */
data class PlannedNotification(
    val key: String,
    val triggerAtEpochMillis: Long,
    val title: String,
    val body: String,
)

/** Schedules local notifications. Each call replaces everything previously scheduled by the app. */
interface NotificationScheduler {
    fun replaceAll(planned: List<PlannedNotification>)
}

/** Runtime notification permission (Android 13+). iOS/older Android: always granted or no-op. */
interface NotificationPermissionGateway {
    fun isGranted(): Boolean
    fun request()
}

object NoOpNotificationScheduler : NotificationScheduler {
    override fun replaceAll(planned: List<PlannedNotification>) = Unit
}

object AlwaysGrantedPermissionGateway : NotificationPermissionGateway {
    override fun isGranted(): Boolean = true
    override fun request() = Unit
}
