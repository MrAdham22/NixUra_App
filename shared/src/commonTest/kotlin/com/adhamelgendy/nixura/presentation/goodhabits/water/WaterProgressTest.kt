package com.adhamelgendy.nixura.presentation.goodhabits.water

import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitUi
import com.adhamelgendy.nixura.presentation.strings.ArabicStrings
import com.adhamelgendy.nixura.presentation.strings.Strings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WaterProgressTest {

    private val en = Strings()

    @Test
    fun amountsShowAtMostTwoDecimalsWithoutTrailingZeros() {
        assertEquals("2", WaterProgress.formatAmount(2.0))
        assertEquals("1.2", WaterProgress.formatAmount(1.2))
        assertEquals("1.3", WaterProgress.formatAmount(1.30))
        assertEquals("1.25", WaterProgress.formatAmount(1.25))
        assertEquals("2.25", WaterProgress.formatAmount(2.25))
        assertEquals("0.05", WaterProgress.formatAmount(0.05))
        assertEquals("6.67", WaterProgress.formatAmount(2000.0 / 300))
        assertEquals("0", WaterProgress.formatAmount(0.0))
    }

    @Test
    fun litresAndCupsProgress() {
        val litres = WaterGoal.fromLiters(2.0)
        assertEquals("1 / 2 L", WaterProgress.text(litres, 4, en))
        assertEquals("1.5 / 2 L", WaterProgress.text(litres, 6, en))
        assertEquals("0 / 2 L", WaterProgress.text(litres, 0, en))
        val cups = WaterGoal.fromCups(8)
        assertEquals("5 / 8 cups", WaterProgress.text(cups, 5, en))
        assertEquals("0 / 1 cup", WaterProgress.text(WaterGoal.fromCups(1, servingMl = 500), 0, en))
    }

    @Test
    fun whatIsLeftInTheGoalsUnit() {
        assertEquals("0.75 L to go", WaterProgress.leftText(WaterGoal.fromLiters(2.0), 5, en))
        assertEquals("1.25 L to go", WaterProgress.leftText(WaterGoal.fromLiters(2.25, servingMl = 250), 4, en))
        assertEquals("3 cups to go", WaterProgress.leftText(WaterGoal.fromCups(8), 5, en))
        assertEquals("1 cup to go", WaterProgress.leftText(WaterGoal.fromCups(8), 7, en))
        assertNull(WaterProgress.leftText(WaterGoal.fromCups(8), 9, en))
    }

    @Test
    fun quickAddAddsOneServingLabelledInTheUnit() {
        assertEquals("+1 cup", WaterProgress.quickAddLabel(WaterGoal.fromCups(8, servingMl = 300), en))
        assertEquals("+250 ml", WaterProgress.quickAddLabel(WaterGoal.fromLiters(2.0), en))
        assertEquals("+330 ml", WaterProgress.quickAddLabel(WaterGoal.fromLiters(2.0, servingMl = 330), en))
    }

    @Test
    fun arabicUsesTheRightNounAfterTheNumber() {
        assertEquals("5 / 8 أكواب", WaterProgress.text(WaterGoal.fromCups(8), 5, ArabicStrings))
        assertEquals("5 / 12 كوبًا", WaterProgress.text(WaterGoal.fromCups(12), 5, ArabicStrings))
        assertEquals("1 / 2 لتر", WaterProgress.text(WaterGoal.fromLiters(2.0), 4, ArabicStrings))
        assertEquals("+250 مل", WaterProgress.quickAddLabel(WaterGoal.fromLiters(2.0), ArabicStrings))
    }

    @Test
    fun dayProgressFollowsTheMlGoalForWaterAndTheTargetOtherwise() {
        // 300 ml cups for 2 L: 7 servings a day, but 6 cups is 1.8 L = 90 % of the goal.
        val water = habit(dailyTarget = 7, water = WaterGoal(2000, 300))
        assertEquals(0.9f, water.dayProgress(6), 0.0001f)
        assertEquals(1f, water.dayProgress(8))
        val prayer = habit(dailyTarget = 5)
        assertEquals(0.4f, prayer.dayProgress(2), 0.0001f)
        assertEquals(0f, prayer.dayProgress(0))
        val weekly = habit(dailyTarget = 1, weeklyTarget = 3)
        assertEquals(1f, weekly.dayProgress(1))
    }

    @Test
    fun progressLineUsesWaterTextOnlyForWaterHabits() {
        assertEquals("1 / 2 L", habit(8, WaterGoal.fromLiters(2.0), todayCount = 4).waterProgressTextOrNull(en))
        assertNull(habit(8).waterProgressTextOrNull(en))
    }

    private fun habit(dailyTarget: Int, water: WaterGoal? = null, weeklyTarget: Int? = null, todayCount: Int = 0) = GoodHabitUi(
        id = 1, name = "Test", dailyTarget = dailyTarget, colorArgb = null, todayCount = todayCount, doneToday = false,
        currentStreak = 0, bestStreak = 0, totalTimesDone = 0, daysCompleted = 0, completion7 = 0f, completion30 = 0f,
        countsByDay = emptyMap(), createdDayIndex = 0, weeklyTarget = weeklyTarget, water = water,
    )
}
