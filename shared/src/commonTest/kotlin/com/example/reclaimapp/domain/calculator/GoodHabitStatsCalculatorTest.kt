package com.example.reclaimapp.domain.calculator

import com.example.reclaimapp.domain.model.GoodHabit
import com.example.reclaimapp.domain.model.GoodHabitLog
import com.example.reclaimapp.domain.model.GoodHabitWithLogs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoodHabitStatsCalculatorTest {

    private fun habit(target: Int, vararg logs: Pair<Long, Int>) = GoodHabitWithLogs(
        GoodHabit(id = 1, name = "Water", dailyTarget = target, createdAtEpochMillis = 0),
        logs.map { GoodHabitLog(1, it.first, it.second) },
    )

    @Test
    fun statusClassification() {
        assertEquals(DayStatus.NONE, GoodHabitStatsCalculator.statusOf(0, 3))
        assertEquals(DayStatus.PARTIAL, GoodHabitStatsCalculator.statusOf(2, 3))
        assertEquals(DayStatus.DONE, GoodHabitStatsCalculator.statusOf(3, 3))
        assertEquals(DayStatus.DONE, GoodHabitStatsCalculator.statusOf(5, 3))
    }

    @Test
    fun streakCountsBackFromTodayWhenDone() {
        val stats = GoodHabitStatsCalculator.compute(habit(2, 10L to 2, 9L to 2, 8L to 3, 6L to 2), todayIndex = 10, createdDayIndex = 0)
        assertEquals(3, stats.currentStreakDays)
        assertEquals(3, stats.bestStreakDays)
        assertTrue(stats.doneToday)
        assertEquals(9, stats.totalTimesDone)
    }

    @Test
    fun streakSurvivesUntilTodayEndsIfTodayNotDoneYet() {
        val stats = GoodHabitStatsCalculator.compute(habit(2, 9L to 2, 8L to 2, 10L to 1), todayIndex = 10, createdDayIndex = 0)
        assertEquals(2, stats.currentStreakDays)
        assertEquals(false, stats.doneToday)
    }

    @Test
    fun streakBreaksAfterMissedDay() {
        val stats = GoodHabitStatsCalculator.compute(habit(1, 7L to 1, 8L to 1), todayIndex = 10, createdDayIndex = 0)
        assertEquals(0, stats.currentStreakDays)
        assertEquals(2, stats.bestStreakDays)
    }

    @Test
    fun completionIsWeightedAndClippedToHabitLifetime() {
        // target 4: day 10 -> 2/4 = 0.5, day 9 -> 4/4 = 1. Habit created on day 9 so window = 2 days.
        val stats = GoodHabitStatsCalculator.compute(habit(4, 10L to 2, 9L to 4), todayIndex = 10, createdDayIndex = 9)
        assertEquals(0.75f, stats.completionLast7)
    }
}
