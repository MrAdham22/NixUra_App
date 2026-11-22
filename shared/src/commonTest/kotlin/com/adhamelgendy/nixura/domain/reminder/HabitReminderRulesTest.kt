package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.time.TimeZoneOffsetProvider
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

    @Test fun aWaterWindowMayPassMidnight() {
        assertEquals(14 * 60..26 * 60, HabitReminderRules.waterWindowOf(window(14 * 60, 2 * 60)))
        assertEquals(22 * 60..24 * 60, HabitReminderRules.waterWindowOf(window(22 * 60, 0)))
    }

    @Test fun pickingATimeTooCloseMovesTheOtherOneInsteadOfResettingBoth() {
        val evening = 9 * 60..21 * 60
        assertEquals(20 * 60 to 22 * 60, HabitReminderRules.windowStartingAt(evening, 20 * 60))
        assertEquals(23 * 60 to 60, HabitReminderRules.windowEndingAt(23 * 60 + 30..26 * 60, 60))
        assertEquals(14 * 60 to 2 * 60, HabitReminderRules.windowEndingAt(14 * 60..21 * 60, 2 * 60))
        assertEquals(15 * 60 to 2 * 60, HabitReminderRules.windowStartingAt(14 * 60..26 * 60, 15 * 60))
    }

    @Test fun pauseForTodayCoversTheLatePartOfTheWindow() {
        val days = DayCalculator(TimeZoneOffsetProvider { 0L })
        val hour = 3_600_000L
        val late = window(14 * 60, 2 * 60)
        val until = days.startOfDayMillis(21) + 2 * hour + 60_000L
        assertEquals(until, HabitReminderRules.pauseEndOf(late, days, days.startOfDayMillis(20) + 16 * hour, 1))
        // At 01:00 it is still that evening: tonight only, not the coming evening as well.
        assertEquals(until, HabitReminderRules.pauseEndOf(late, days, days.startOfDayMillis(21) + hour, 1))
        assertEquals(days.startOfDayMillis(21), HabitReminderRules.pauseEndOf(window(9 * 60, 21 * 60), days, days.startOfDayMillis(20) + hour, 1))
    }

    @Test fun pauseForTodayCoversTheSecondReminderAfterMidnight() {
        val days = DayCalculator(TimeZoneOffsetProvider { 0L })
        val minute = 60_000L
        val night = GoodHabit(1, "Read", 2, createdAtEpochMillis = 0, reminder = HabitReminderSettings(enabled = true, timeMinutes = 22 * 60 + 50))
        val second = days.startOfDayMillis(21) + 110 * minute
        assertEquals(second + minute, HabitReminderRules.pauseEndOf(night, days, days.startOfDayMillis(20) + 16 * 60 * minute, 1))
        assertEquals(second + minute, HabitReminderRules.pauseEndOf(night, days, days.startOfDayMillis(21) + 30 * minute, 1))
    }

    private fun window(start: Int, end: Int) = GoodHabit(1, "Water", 8, createdAtEpochMillis = 0, theme = HabitTheme.WATER,
        reminder = HabitReminderSettings(windowStartMinutes = start, windowEndMinutes = end))

    @Test fun waterPaceIsTheGoalSpreadOverTheWindow() {
        val window = 9 * 60..21 * 60
        assertEquals(0, HabitReminderRules.waterExpectedBy(8, window, 9 * 60))
        assertEquals(1, HabitReminderRules.waterExpectedBy(8, window, 11 * 60))
        assertEquals(4, HabitReminderRules.waterExpectedBy(8, window, 15 * 60))
        assertEquals(8, HabitReminderRules.waterExpectedBy(8, window, 21 * 60))
    }

    @Test fun aBrokenWaterWindowFallsBackToTheDefault() {
        assertEquals(9 * 60..21 * 60, HabitReminderRules.waterWindowOf(window(20 * 60, 20 * 60)), "no length")
        assertEquals(9 * 60..21 * 60, HabitReminderRules.waterWindowOf(window(20 * 60, 21 * 60)), "under 2 h")
        assertEquals(9 * 60..21 * 60, HabitReminderRules.waterWindowOf(window(-5, 21 * 60)), "not a time")
    }
}
