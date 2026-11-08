package com.adhamelgendy.nixura.domain.recognition

import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import kotlin.test.Test
import kotlin.test.assertEquals

class GoodHabitFillStyleTest {

    private val detector = GoodHabitFillStyleDetector(HabitKnowledgeBase())

    @Test
    fun waterNamesFillWithWater() {
        listOf("Drink water", "water", "drinking water", "Hydration", "hydrate", "drink watr", "8 glasses of water", "شرب الماء")
            .forEach { assertEquals(GoodHabitFillStyle.WATER, detector.styleFor(it), it) }
    }

    @Test
    fun otherHabitsKeepThePlainFill() {
        listOf("Read 10 pages", "Workout", "Meditate", "test syncing 2", "")
            .forEach { assertEquals(GoodHabitFillStyle.PLAIN, detector.styleFor(it), it) }
    }

    @Test
    fun eachTapRaisesTheLevelByOneTarget() {
        assertEquals(0f, GoodHabitStatsCalculator.fillLevel(0, 10))
        assertEquals(0.1f, GoodHabitStatsCalculator.fillLevel(1, 10), 1e-6f)
        assertEquals(0.7f, GoodHabitStatsCalculator.fillLevel(7, 10), 1e-6f)
        assertEquals(1f, GoodHabitStatsCalculator.fillLevel(10, 10))
    }

    @Test
    fun levelStaysInsideTheCard() {
        assertEquals(1f, GoodHabitStatsCalculator.fillLevel(14, 10))
        assertEquals(0f, GoodHabitStatsCalculator.fillLevel(-2, 10))
        assertEquals(1f, GoodHabitStatsCalculator.fillLevel(1, 0)) // a broken target counts as 1
    }
}
