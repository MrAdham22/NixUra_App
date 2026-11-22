package com.adhamelgendy.nixura.domain.notification

/** What a notification shows on a locked screen. */
enum class LockScreenVisibility {
    /** All of it, whatever the phone's setting. */
    PUBLIC,

    /**
     * Follows the phone: "show all content" shows all of it, "hide sensitive content" shows only the neutral public
     * version ([PlannedNotification.publicTitle]), or just the app's name when there is none.
     */
    PRIVATE,
}

/**
 * The lock-screen rule per type (owner decisions 2 and 4, D-lock-screen section 4). Set on each notification:
 * Android ignores an app's channel-level value. [LockScreenVisibility.PRIVATE] cannot hide a name on a phone set to
 * show all content; that is what "hide name outside the app" is for.
 */
object LockScreenPrivacy {
    fun visibilityOf(kind: NotificationKind): LockScreenVisibility = when (kind) {
        NotificationKind.GOOD_HABIT -> LockScreenVisibility.PUBLIC
        NotificationKind.EVENT,
        NotificationKind.QUIT_MILESTONE,
        NotificationKind.COACH,
        NotificationKind.APP,
        -> LockScreenVisibility.PRIVATE
    }
}

/** The neutral words a phone that hides sensitive content shows. Never a name. Implemented by `Strings`. */
interface LockScreenCopy {
    /** "Event in 2 hours"; [inText] is that duration in words, `null` at the event's own time ("Event now"). */
    fun lockScreenEvent(inText: String?): String

    fun lockScreenMilestone(): String

    fun lockScreenCoach(): String
}

/** The wording for a resolved language ("en" / "ar"). */
fun interface LockScreenCopyProvider {
    fun forLanguage(language: String): LockScreenCopy
}
