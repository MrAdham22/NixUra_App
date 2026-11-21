package com.adhamelgendy.nixura.domain.notification

/** Whether the app can show notifications right now, and what the UI should offer if not. */
enum class NotificationAccess {
    /** Notifications can be shown. */
    ALLOWED,

    /** Not allowed yet, but the system dialog can still be shown: explain why first, then ask. */
    ASK,

    /** The system will not show its dialog again (denied twice) or notifications are switched off: offer system settings. */
    OPEN_SETTINGS,
}

/** Pure decision behind [NotificationAccess], so the rules are unit-tested without a phone. */
object NotificationAccessPolicy {

    /**
     * @param runtimePermissionRequired true on Android 13+ (POST_NOTIFICATIONS); false on older Android.
     * @param permissionGranted the runtime permission's current state (ignored when not required).
     * @param appNotificationsEnabled false when the user switched all of the app's notifications off in system settings.
     * @param askedBefore true once the app has shown the system dialog at least once on this install.
     * @param rationaleAvailable Android's `shouldShowRequestPermissionRationale`: true after exactly one denial.
     */
    fun resolve(
        runtimePermissionRequired: Boolean,
        permissionGranted: Boolean,
        appNotificationsEnabled: Boolean,
        askedBefore: Boolean,
        rationaleAvailable: Boolean,
    ): NotificationAccess {
        // Granted (or not needed): only the system-wide switch can still block us.
        if (!runtimePermissionRequired || permissionGranted) {
            return if (appNotificationsEnabled) NotificationAccess.ALLOWED else NotificationAccess.OPEN_SETTINGS
        }
        // Never asked, or denied once (Android still shows its dialog): explain, then ask.
        // Asked before with no rationale = denied twice, Android stays silent. (A dialog dismissed by tapping outside
        // looks the same; the settings card is the honest path then too.)
        return if (!askedBefore || rationaleAvailable) NotificationAccess.ASK else NotificationAccess.OPEN_SETTINGS
    }
}
