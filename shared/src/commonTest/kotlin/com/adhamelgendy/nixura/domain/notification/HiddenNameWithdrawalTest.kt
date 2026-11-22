package com.adhamelgendy.nixura.domain.notification

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Turning "hide name" on also takes away what is already showing that name. */
class HiddenNameWithdrawalTest {
    private val hidden = setOf(7L)

    @Test fun aShownMilestoneOrCheckInNamingTheHabitIsRemoved() {
        assertTrue(HiddenNameWithdrawal.shouldRemove(NotificationKind.QUIT_MILESTONE, ShownName.Of(7), hidden))
        assertTrue(HiddenNameWithdrawal.shouldRemove(NotificationKind.COACH, ShownName.Of(7), hidden))
    }

    @Test fun anotherHabitsAndAMaskedOneStay() {
        assertFalse(HiddenNameWithdrawal.shouldRemove(NotificationKind.QUIT_MILESTONE, ShownName.Of(8), hidden))
        assertFalse(HiddenNameWithdrawal.shouldRemove(NotificationKind.QUIT_MILESTONE, ShownName.None, hidden))
        assertFalse(HiddenNameWithdrawal.shouldRemove(NotificationKind.COACH, ShownName.None, hidden))
    }

    @Test fun oneThatCannotSayWhomItNamesIsRemovedToBeSafe() {
        assertTrue(HiddenNameWithdrawal.shouldRemove(NotificationKind.COACH, ShownName.Unknown, hidden))
        assertFalse(HiddenNameWithdrawal.shouldRemove(NotificationKind.COACH, ShownName.Unknown, emptySet()))
    }

    @Test fun otherTypesAreNeverTouched() {
        for (kind in NotificationKind.entries.filter { it != NotificationKind.COACH && it != NotificationKind.QUIT_MILESTONE }) {
            assertFalse(HiddenNameWithdrawal.shouldRemove(kind, ShownName.Unknown, hidden), kind.name)
        }
    }
}
