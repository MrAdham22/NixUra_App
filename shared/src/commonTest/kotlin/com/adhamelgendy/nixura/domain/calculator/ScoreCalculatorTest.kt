package com.adhamelgendy.nixura.domain.calculator

import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitLog
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.Habit
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.model.TrackingSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class ScoreCalculatorTest {

    private fun bad(days: Int) = HabitTracking(
        Habit(1, "x", 0),
        emptyList(),
        TrackingSnapshot(0, days.days.inWholeMilliseconds, days.days.inWholeMilliseconds),
    )

    @Test
    fun quitScoreAveragesProgressTowardThirtyDays() {
        assertNull(ScoreCalculator.quitScore(emptyList()))
        assertEquals(50, ScoreCalculator.quitScore(listOf(bad(15))))
        assertEquals(66, ScoreCalculator.quitScore(listOf(bad(30), bad(10))))
        assertEquals(100, ScoreCalculator.quitScore(listOf(bad(90))))
    }

    @Test
    fun disciplineScoreUsesWeightedLastSevenDays() {
        val h = GoodHabitWithLogs(
            GoodHabit(1, "w", 2, null, 0),
            (4L..10L).map { GoodHabitLog(1, it, 1) }, // half of target every day
        )
        assertEquals(50, ScoreCalculator.disciplineScore(listOf(h), 10) { 0L })
        assertNull(ScoreCalculator.disciplineScore(emptyList(), 10) { 0L })
    }

    @Test
    fun quitLinesExplainTheScoreBestFirst() {
        val lines = ScoreCalculator.quitLines(listOf(bad(10), bad(30)))
        assertEquals(listOf(100, 33), lines.map { it.percent })
        assertEquals(66, lines.map { it.percent }.average().toInt())
    }

    @Test
    fun disciplineLinesMatchTheScore() {
        val half = GoodHabitWithLogs(GoodHabit(1, "half", 2, null, 0), (4L..10L).map { GoodHabitLog(1, it, 1) })
        val full = GoodHabitWithLogs(GoodHabit(2, "full", 1, null, 0), (4L..10L).map { GoodHabitLog(2, it, 1) })
        val lines = ScoreCalculator.disciplineLines(listOf(half, full), 10) { 0L }
        assertEquals(listOf("full" to 100, "half" to 50), lines.map { it.name to it.percent })
        assertEquals(75, ScoreCalculator.disciplineScore(listOf(half, full), 10) { 0L })
    }

    @Test
    fun momentumCombinesAvailableScores() {
        assertEquals(60, ScoreCalculator.momentum(80, 40))
        assertEquals(80, ScoreCalculator.momentum(80, null))
        assertEquals(40, ScoreCalculator.momentum(null, 40))
        assertNull(ScoreCalculator.momentum(null, null))
    }
}
