package com.adhamelgendy.nixura.domain.calculator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class MilestoneCalculatorTest {
    @Test
    fun aNewStreakAimsForTheFirstDay() {
        assertEquals(NextMilestone(1, 1.days), MilestoneCalculator.next(Duration.ZERO))
    }

    @Test
    fun midwayCountsDownToTheNextStage() {
        val elapsed = 3.days + 3.hours + 54.minutes
        assertEquals(NextMilestone(7, 7.days - elapsed), MilestoneCalculator.next(elapsed))
    }

    @Test
    fun reachingAMilestoneMovesOnToTheNextOne() {
        assertEquals(14, MilestoneCalculator.next(7.days).days)
    }

    @Test
    fun afterAYearEveryYearIsAMilestone() {
        assertEquals(365, MilestoneCalculator.next(200.days).days)
        assertEquals(730, MilestoneCalculator.next(365.days).days)
        assertEquals(1095, MilestoneCalculator.next(800.days).days)
    }

    @Test
    fun negativeTimeCountsAsZero() {
        assertEquals(NextMilestone(1, 1.days), MilestoneCalculator.next((-5).hours))
    }
}
