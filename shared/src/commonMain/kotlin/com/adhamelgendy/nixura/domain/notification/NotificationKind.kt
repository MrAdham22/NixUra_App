package com.adhamelgendy.nixura.domain.notification

/**
 * What a notification is about. Decides its channel, look (icon, accent colour), grouping and what a tap opens,
 * so the user can tell the type apart before reading the text. Set by the planner; the platform layer
 * (Android `notifications/NixNotifications`, later iOS categories) maps it to OS features.
 */
enum class NotificationKind {
    /** A reminder before one of the user's events. */
    EVENT,

    /** A nudge for a good habit (water, prayer, other counters; planned in phase C). */
    GOOD_HABIT,

    /** Support for a quit habit: streak milestones. Never a "you relapsed" style message. */
    QUIT_MILESTONE,

    /** The coach: daily check-in, coach notes. */
    COACH,

    /** The app itself: sync problems, account, updates. */
    APP,

    /** The opt-in, silent "Today" notification that stays on the lock screen ([TodaySummary]). Never planned. */
    TODAY,
    ;

    companion object {
        const val EVENT_PREFIX = "event:"
        const val MILESTONE_PREFIX = "milestone:"
        const val DAILY_PREFIX = "daily:"
        const val GOOD_HABIT_PREFIX = "good:"

        /**
         * Infers the kind from a stable notification key. Plans stored before phase B have no kind, so this keeps
         * an app update from losing or mis-styling reminders that were already armed.
         */
        fun fromKey(key: String): NotificationKind = when {
            key.startsWith(EVENT_PREFIX) -> EVENT
            key.startsWith(MILESTONE_PREFIX) -> QUIT_MILESTONE
            key.startsWith(DAILY_PREFIX) -> COACH
            key.startsWith(GOOD_HABIT_PREFIX) -> GOOD_HABIT
            else -> APP
        }
    }
}
