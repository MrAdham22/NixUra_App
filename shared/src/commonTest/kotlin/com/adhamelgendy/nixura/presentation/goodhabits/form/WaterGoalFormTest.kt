package com.adhamelgendy.nixura.presentation.goodhabits.form

import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.theme.WaterUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaterGoalFormTest {

    @Test fun defaultsAreTwoLitersInEightCupsOf250() {
        val form = WaterGoalForm()
        assertEquals(2_000, form.goalMl)
        assertEquals(250, form.servingMl)
        assertEquals(8, form.servingsPerDay)
        assertEquals("8", form.cupsText())
        assertEquals("2", form.litersText())
    }

    @Test fun switchingUnitsNeverChangesTheGoal() {
        val form = WaterGoalForm(goalMl = 2_250, servingMl = 300)
        val liters = form.withUnit(WaterUnit.LITERS)
        val back = liters.withUnit(WaterUnit.CUPS)
        assertEquals(2_250, liters.goalMl)
        assertEquals(2_250, back.goalMl)
        assertEquals(300, back.servingMl)
        assertEquals(WaterUnit.CUPS, back.toGoal().unit)
    }

    @Test fun litersStepBy250AndLandOnWholeSteps() {
        val form = WaterGoalForm(goalMl = 2_000, unit = WaterUnit.LITERS)
        assertEquals(2_250, form.stepGoal(up = true).goalMl)
        assertEquals(1_750, form.stepGoal(up = false).goalMl)
        // From an odd value (cups of 300 ml) it snaps to the next quarter litre.
        assertEquals(2_000, WaterGoalForm(goalMl = 1_800, unit = WaterUnit.LITERS).stepGoal(up = true).goalMl)
        assertEquals(1_750, WaterGoalForm(goalMl = 1_800, unit = WaterUnit.LITERS).stepGoal(up = false).goalMl)
    }

    @Test fun cupsStepByWholeCupsOfTheCurrentSize() {
        val form = WaterGoalForm(goalMl = 2_000, servingMl = 300)
        assertEquals("6.7", form.cupsText())
        assertEquals(2_100, form.stepGoal(up = true).goalMl) // 7 cups
        assertEquals(1_800, form.stepGoal(up = false).goalMl) // 6 cups
        assertEquals(2_250, WaterGoalForm(goalMl = 2_000).stepGoal(up = true).goalMl) // 9 cups of 250
    }

    @Test fun goalStaysWithinLimits() {
        val low = WaterGoalForm(goalMl = WaterGoal.MIN_GOAL_ML, unit = WaterUnit.LITERS)
        assertFalse(low.canDecreaseGoal)
        assertEquals(WaterGoal.MIN_GOAL_ML, low.stepGoal(up = false).goalMl)
        val high = WaterGoalForm(goalMl = WaterGoal.MAX_GOAL_ML)
        assertFalse(high.canIncreaseGoal)
        assertEquals(WaterGoal.MAX_GOAL_ML, high.stepGoal(up = true).goalMl)
        // 100 ml cups: one cup down from 300 ml would be 200 ml, below the 250 ml minimum.
        assertEquals(250, WaterGoalForm(goalMl = 300, servingMl = 100).stepGoal(up = false).goalMl)
    }

    @Test fun cupSizeStepsBy50WithinLimitsAndKeepsTheGoal() {
        val bigger = WaterGoalForm().stepServing(up = true)
        assertEquals(300, bigger.servingMl)
        assertEquals(2_000, bigger.goalMl)
        assertEquals(7, bigger.servingsPerDay) // 2000 / 300 rounded up
        assertEquals(WaterGoal.MIN_SERVING_ML, WaterGoalForm(servingMl = 100).stepServing(up = false).servingMl)
        assertEquals(WaterGoal.MAX_SERVING_ML, WaterGoalForm(servingMl = 1_000).stepServing(up = true).servingMl)
        assertTrue(WaterGoalForm(servingMl = 150).canDecreaseServing)
    }

    @Test fun prefillsFromTheSavedGoal() {
        val saved = WaterGoal(goalMl = 3_000, servingMl = 500, unit = WaterUnit.LITERS)
        assertEquals(saved, WaterGoalForm.from(saved).toGoal())
        assertEquals(WaterGoalForm(), WaterGoalForm.from(null))
    }

    @Test fun formatsLitersWithoutTrailingZeros() {
        assertEquals("2", WaterGoalForm.formatLiters(2_000))
        assertEquals("2.5", WaterGoalForm.formatLiters(2_500))
        assertEquals("2.25", WaterGoalForm.formatLiters(2_250))
        assertEquals("0.25", WaterGoalForm.formatLiters(250))
        assertEquals("1.05", WaterGoalForm.formatLiters(1_050))
    }
}
