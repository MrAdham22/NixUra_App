package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Which habits get reminders without asking (PLAN.md: prayer and water suggested on, the rest offered). */
class HabitReminderRulesTest {

    private fun habit(theme: HabitTheme, target: Int = 1, weekly: Int? = null, enabled: Boolean? = null) =
        GoodHabit(1, "x", target, createdAtEpochMillis = 0, theme = theme, weeklyTarget = weekly, reminder = HabitReminderSettings(enabled = enabled))

    @Test fun prayerAndWaterAreOnByDefault() {
        assertTrue(HabitReminderRules.isEnabled(habit(HabitTheme.PRAYER, target = 5)))
        assertTrue(HabitReminderRules.isEnabled(habit(HabitTheme.WATER, target = 8)))
    }

    @Test fun everythingElseIsOffByDefault() {
        listOf(HabitTheme.WALK, HabitTheme.WORKOUT, HabitTheme.READING, HabitTheme.SLEEP, HabitTheme.DEFAULT).forEach {
            assertFalse(HabitReminderRules.isEnabled(habit(it, target = 2)), "$it")
        }
        assertFalse(HabitReminderRules.isEnabled(habit(HabitTheme.WATER, weekly = 3)), "a weekly water habit is not time-based")
    }

    @Test fun theUsersChoiceWins() {
        assertFalse(HabitReminderRules.isEnabled(habit(HabitTheme.WATER, target = 8, enabled = false)))
        assertTrue(HabitReminderRules.isEnabled(habit(HabitTheme.READING, enabled = true)))
    }

    @Test fun onlyAFivePrayerHabitFollowsPrayerTimes() {
        assertEquals(ReminderStrategy.PRAYER, HabitReminderRules.strategyOf(habit(HabitTheme.PRAYER, target = 5)))
        // "Tahajjud" once a night: a prayer theme, but one chosen time.
        assertEquals(ReminderStrategy.DAILY_TIME, HabitReminderRules.strategyOf(habit(HabitTheme.PRAYER, target = 1)))
        assertEquals(ReminderStrategy.WEEKLY_TIME, HabitReminderRules.strategyOf(habit(HabitTheme.PRAYER, target = 1, weekly = 2)))
    }

    @Test fun quietHoursWrapAroundMidnight() {
        assertTrue(HabitReminderRules.isQuiet(22 * 60))
        assertTrue(HabitReminderRules.isQuiet(3 * 60))
        assertTrue(HabitReminderRules.isQuiet(7 * 60 + 59))
        assertFalse(HabitReminderRules.isQuiet(8 * 60))
        assertFalse(HabitReminderRules.isQuiet(21 * 60 + 59))
    }

    @Test fun waterPaceIsTheGoalSpreadOverTheWindow() {
        val window = 9 * 60..21 * 60
        assertEquals(0, HabitReminderRules.waterExpectedBy(8, window, 9 * 60))
        assertEquals(1, HabitReminderRules.waterExpectedBy(8, window, 11 * 60))
        assertEquals(4, HabitReminderRules.waterExpectedBy(8, window, 15 * 60))
        assertEquals(8, HabitReminderRules.waterExpectedBy(8, window, 21 * 60))
    }

    @Test fun aBrokenWaterWindowFallsBackToTheDefault() {
        val broken = GoodHabit(1, "Water", 8, createdAtEpochMillis = 0, theme = HabitTheme.WATER,
            reminder = HabitReminderSettings(windowStartMinutes = 20 * 60, windowEndMinutes = 19 * 60))
        assertEquals(9 * 60..21 * 60, HabitReminderRules.waterWindowOf(broken))
    }
}
