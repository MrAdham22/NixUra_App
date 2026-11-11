package com.adhamelgendy.nixura.domain.calculator

import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitLog
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeeklyGoalCalculatorTest {
    private val w = WeeklyGoalCalculator

    /** A Sunday (2025-08-03). Day indexes are days since 1970-01-01. */
    private val sun = 20303L
    private val longAgo = sun - 70

    private fun days(vararg d: Long) = d.associateWith { 1 }

    @Test
    fun weekStartsOnSunday() {
        assertEquals(sun, w.weekStartOf(sun))
        assertEquals(sun, w.weekStartOf(sun + 6)) // Saturday
        assertEquals(sun + 7, w.weekStartOf(sun + 7)) // next Sunday
        assertEquals(3L, w.weekStartOf(3)) // 1970-01-04 was a Sunday
        assertEquals(-4L, w.weekStartOf(-1)) // 1969-12-31 (Wednesday) -> Sunday 1969-12-28
    }

    @Test
    fun goalMetEarlyIsFullMarks() {
        val counts = days(sun, sun + 1, sun + 2)
        assertEquals(1f, w.weekValue(counts, 3, sun, today = sun + 2, createdDay = longAgo))
    }

    @Test
    fun doingMoreThanTheGoalStaysAtFullMarks() {
        val counts = days(sun, sun + 1, sun + 2, sun + 3, sun + 4)
        assertEquals(1f, w.weekValue(counts, 3, sun, today = sun + 4, createdDay = longAgo))
    }

    @Test
    fun anOpenWeekIsLeftOut() {
        // Monday, nothing yet: 6 days left for a goal of 3.
        assertNull(w.weekValue(emptyMap(), 3, sun, today = sun + 1, createdDay = longAgo))
        // Friday, 1 done: Friday and Saturday are left, 1 + 2 = 3 is still reachable.
        assertNull(w.weekValue(days(sun), 3, sun, today = sun + 5, createdDay = longAgo))
    }

    @Test
    fun aWeekThatCanNoLongerBeMetCountsWhatWasDone() {
        // Saturday, 1 done and Saturday not done yet: at most 2 of 3.
        assertEquals(1f / 3, w.weekValue(days(sun), 3, sun, today = sun + 6, createdDay = longAgo))
    }

    @Test
    fun aFinishedWeekCountsWhatWasDone() {
        val counts = days(sun, sun + 3)
        assertEquals(2f / 3, w.weekValue(counts, 3, sun, today = sun + 9, createdDay = longAgo))
    }

    @Test
    fun aDayCountsOnceEvenIfLoggedTwice() {
        val counts = mapOf(sun to 2, sun + 1 to 1)
        assertEquals(2, w.doneDays(counts, sun, today = sun + 3, createdDay = longAgo))
    }

    @Test
    fun theCreationWeekHasASmallerGoal() {
        assertEquals(1, w.goalFor(sun, 3, createdDay = sun + 5)) // created Friday: ceil(3 x 2 / 7) = 1
        assertEquals(2, w.goalFor(sun, 3, createdDay = sun + 3)) // created Wednesday: ceil(3 x 4 / 7) = 2
        assertEquals(3, w.goalFor(sun, 3, createdDay = sun)) // created Sunday: the full goal
        assertEquals(3, w.goalFor(sun + 7, 3, createdDay = sun + 5)) // the week after: the full goal
    }

    @Test
    fun scoreUsesLastWeekWhileThisWeekIsOpen() {
        // Last week 2 of 3, this week just started.
        val counts = days(sun - 7, sun - 5)
        assertEquals(2f / 3, w.scoreValue(counts, 3, today = sun + 1, createdDay = longAgo))
        // Goal met this week: this week counts.
        val metThisWeek = counts + days(sun, sun + 1, sun + 2)
        assertEquals(1f, w.scoreValue(metThisWeek, 3, today = sun + 2, createdDay = longAgo))
    }

    @Test
    fun aBrandNewWeeklyHabitDoesNotCountYet() {
        assertNull(w.scoreValue(emptyMap(), 3, today = sun + 1, createdDay = sun))
    }

    @Test
    fun streaksCountWeeksAndAnOpenWeekDoesNotBreakThem() {
        val twoWeeksMet = days(sun - 14, sun - 13, sun - 12, sun - 7, sun - 6, sun - 5)
        assertEquals(2, w.currentStreakWeeks(twoWeeksMet, 3, today = sun + 1, createdDay = longAgo))
        val threeWeeksMet = twoWeeksMet + days(sun, sun + 1, sun + 2)
        assertEquals(3, w.currentStreakWeeks(threeWeeksMet, 3, today = sun + 2, createdDay = longAgo))
        val (best, met) = w.bestStreakAndMetWeeks(threeWeeksMet, 3, today = sun + 2, createdDay = longAgo)
        assertEquals(3, best)
        assertEquals(3, met)
    }

    @Test
    fun aMissedWeekEndsTheStreak() {
        val counts = days(sun - 21, sun - 20, sun - 19, sun - 7, sun - 6, sun - 5) // week -2 missed
        assertEquals(1, w.currentStreakWeeks(counts, 3, today = sun + 1, createdDay = longAgo))
    }

    @Test
    fun disciplineLeavesOutAnOpenWeeklyHabit() {
        val daily = GoodHabitWithLogs(
            GoodHabit(id = 1, name = "Water", dailyTarget = 1, createdAtEpochMillis = 0),
            (0L..6L).map { GoodHabitLog(1, sun + it, 1) },
        )
        val weekly = GoodHabitWithLogs(
            GoodHabit(id = 2, name = "Medicine", dailyTarget = 1, createdAtEpochMillis = 0, weeklyTarget = 3),
            emptyList(),
        )
        val created = mapOf(1L to sun, 2L to sun + 6)
        // Saturday: the daily habit is 100%; the weekly one was created today and can still be done today.
        assertEquals(100, ScoreCalculator.disciplineScore(listOf(daily, weekly), sun + 6) { created.getValue(it.habit.id) })
        assertEquals(1, ScoreCalculator.disciplineLines(listOf(daily, weekly), sun + 6) { created.getValue(it.habit.id) }.size)
        assertNull(ScoreCalculator.disciplineScore(listOf(weekly), sun + 6) { created.getValue(it.habit.id) })
    }

    @Test
    fun statsForAWeeklyHabit() {
        val habit = GoodHabitWithLogs(
            GoodHabit(id = 3, name = "Gym", dailyTarget = 1, createdAtEpochMillis = 0, weeklyTarget = 3),
            listOf(GoodHabitLog(3, sun, 1), GoodHabitLog(3, sun + 2, 2)),
        )
        val stats = GoodHabitStatsCalculator.compute(habit, todayIndex = sun + 2, createdDayIndex = longAgo)
        assertTrue(stats.isWeekly)
        assertEquals(2, stats.weekDone)
        assertEquals(3, stats.weekGoal)
        assertEquals(2, stats.progressCount)
        assertEquals(3, stats.progressTarget)
        assertEquals(1, stats.todayCount) // a day counts once
        assertTrue(stats.doneToday)
        assertEquals(false, stats.weekGoalMet)
        assertEquals(2f / 3, stats.completionLast7)
        assertEquals(2, stats.totalTimesDone)
    }
}
