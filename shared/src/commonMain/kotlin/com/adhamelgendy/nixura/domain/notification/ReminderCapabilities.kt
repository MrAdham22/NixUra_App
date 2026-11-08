package com.adhamelgendy.nixura.domain.notification

/**
 * What the OS currently allows for reminders, plus shortcuts to the system screens where the user can
 * change it. Lets the Events UI explain a denied permission instead of failing silently.
 * Implemented per platform; [AlwaysAvailableReminderCapabilities] is used where nothing can be denied.
 */
interface ReminderCapabilities {
    /** True when notifications can be shown (runtime permission granted and not switched off in system settings). */
    fun notificationsAllowed(): Boolean

    /** True when reminders can fire at the exact minute. When false they still fire, possibly a few minutes late. */
    fun exactAlarmsAllowed(): Boolean

    /** Opens the system screen for this app's notification settings. */
    fun openNotificationSettings()

    /** Opens the system screen where exact alarms can be allowed for this app (no-op where not applicable). */
    fun openExactAlarmSettings()
}

object AlwaysAvailableReminderCapabilities : ReminderCapabilities {
    override fun notificationsAllowed(): Boolean = true
    override fun exactAlarmsAllowed(): Boolean = true
    override fun openNotificationSettings() = Unit
    override fun openExactAlarmSettings() = Unit
}
