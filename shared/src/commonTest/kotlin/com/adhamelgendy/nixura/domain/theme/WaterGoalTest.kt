package com.adhamelgendy.nixura.domain.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WaterGoalTest {

    @Test
    fun switchingUnitsNeverChangesTheGoal() {
        val goal = WaterGoal.fromLiters(2.0)
        val cups = goal.withUnit(WaterUnit.CUPS)
        assertEquals(2_000, cups.goalMl)
        assertEquals(8.0, cups.goalInUnit())
        assertEquals(2.0, cups.withUnit(WaterUnit.LITERS).goalInUnit())
    }

    @Test
    fun servingsRoundUpSoTheGoalIsNeverUnderCounted() {
        assertEquals(8, WaterGoal(2_000, 250).servingsPerDay)
        assertEquals(7, WaterGoal(1_500, 220).servingsPerDay)
    }

    @Test
    fun progressIsTheShareOfTheGoalAndStopsAtFull() {
        val goal = WaterGoal(2_000, 250)
        assertEquals(0f, goal.progress(0))
        assertEquals(0.5f, goal.progress(4))
        assertEquals(1f, goal.progress(12))
        assertEquals(1.2, WaterGoal(2_000, 200, WaterUnit.LITERS).drunkInUnit(6)) // 1200 ml shows as 1.2 L
        assertEquals(1.25, WaterGoal(1_000, 250, WaterUnit.LITERS).drunkInUnit(5)) // never rounded up to 1.3
        assertEquals(2.25, WaterGoal(2_250, 300, WaterUnit.LITERS).goalInUnit())
    }

    @Test
    fun oldWaterHabitsKeepTheirCountsAsCups() {
        val goal = WaterGoal.defaultFor(dailyTarget = 8)
        assertEquals(2_000, goal.goalMl)
        assertEquals(8, goal.servingsPerDay)
        assertEquals(WaterUnit.CUPS, goal.unit)
    }

    @Test
    fun limitsRejectZeroAndAbsurdGoals() {
        assertFailsWith<IllegalArgumentException> { WaterGoal(0) }
        assertFailsWith<IllegalArgumentException> { WaterGoal(50_000) }
        assertFailsWith<IllegalArgumentException> { WaterGoal(2_000, servingMl = 5) }
        assertEquals(WaterGoal.MAX_GOAL_ML, WaterGoal.fromLiters(50.0).goalMl)
        assertEquals(WaterGoal.MIN_GOAL_ML, WaterGoal.fromCups(0).goalMl)
    }
}
