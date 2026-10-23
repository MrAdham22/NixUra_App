package com.example.reclaimapp.domain.calculator

import com.example.reclaimapp.domain.model.GoodHabit
import com.example.reclaimapp.domain.model.GoodHabitLog
import com.example.reclaimapp.domain.model.GoodHabitWithLogs
import com.example.reclaimapp.domain.model.Habit
import com.example.reclaimapp.domain.model.HabitTracking
import com.example.reclaimapp.domain.model.TrackingSnapshot
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
    fun momentumCombinesAvailableScores() {
        assertEquals(60, ScoreCalculator.momentum(80, 40))
        assertEquals(80, ScoreCalculator.momentum(80, null))
        assertEquals(40, ScoreCalculator.momentum(null, 40))
        assertNull(ScoreCalculator.momentum(null, null))
    }
}
