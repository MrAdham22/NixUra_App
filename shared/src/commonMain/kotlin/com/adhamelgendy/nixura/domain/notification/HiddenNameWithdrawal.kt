package com.adhamelgendy.nixura.domain.notification

/** Whose name a notification that is showing carries. */
sealed interface ShownName {
    data class Of(val habitId: Long) : ShownName

    /** Names no quit habit. */
    data object None : ShownName

    /** Posted without saying (by an older build). */
    data object Unknown : ShownName
}

/**
 * When a name is hidden, the milestone and check-in already showing it must go (a re-plan only fixes what has not
 * been posted yet). Pure, so each platform only has to list what is showing.
 */
object HiddenNameWithdrawal {
    fun shouldRemove(kind: NotificationKind, shown: ShownName, hiddenHabitIds: Set<Long>): Boolean {
        if (hiddenHabitIds.isEmpty()) return false
        if (kind != NotificationKind.QUIT_MILESTONE && kind != NotificationKind.COACH) return false
        return when (shown) {
            is ShownName.Of -> shown.habitId in hiddenHabitIds
            ShownName.None -> false
            ShownName.Unknown -> true
        }
    }
}
