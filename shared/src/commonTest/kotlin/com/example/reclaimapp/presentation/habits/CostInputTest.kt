package com.example.reclaimapp.presentation.habits

import com.example.reclaimapp.domain.money.CostUnit
import com.example.reclaimapp.domain.money.CountableProfile
import com.example.reclaimapp.domain.money.HabitCost
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CostInputTest {
    private val cigarettes = CountableProfile("nicotine", listOf(CostUnit.CIGARETTE), defaultPerPack = 20, packs = true)
    private val drinks = CountableProfile("softdrinks", listOf(CostUnit.CAN, CostUnit.BOTTLE), defaultPerPack = 1, packs = false)

    @Test
    fun recognisingTheHabitFillsThePackSize() {
        val blank = CostInput.from(null, null, "EGP")
        assertEquals("20", blank.adaptedTo(cigarettes).perPack)
        assertEquals("1", blank.adaptedTo(drinks).perPack)
        assertEquals(CostUnit.CAN, blank.adaptedTo(drinks).unit)
    }

    @Test
    fun packsPerDayAreStoredAsUnits() {
        val input = CostInput(enabled = true, unit = CostUnit.CIGARETTE, perDay = "1.5", inPacks = true, perPack = "20", price = "100", currency = "EGP")
        val cost = input.toCost()!!
        assertEquals(30.0, cost.unitsPerDay)
        assertEquals(10_000, cost.pricePerPackMinor)
        // and shown again the way it was typed
        assertEquals("1.5", CostInput.from(cost, cigarettes, "USD").perDay)
        assertTrue(CostInput.from(cost, cigarettes, "USD").inPacks)
    }

    @Test
    fun offOrIncompleteSavesNoTracker() {
        assertNull(CostInput(enabled = false, perDay = "10", perPack = "20", price = "5").toCost())
        val missingPrice = CostInput(enabled = true, perDay = "10", perPack = "20", price = "")
        assertFalse(missingPrice.isComplete)
        assertNull(missingPrice.toCost())
    }

    @Test
    fun editingKeepsTheSavedValues() {
        val cost = HabitCost("Pepsi", CostUnit.BOTTLE, 2.0, 2_500, 1, "EGP")
        val input = CostInput.from(cost, drinks, "USD")
        assertEquals("Pepsi", input.item)
        assertEquals("25", input.price)
        assertEquals("EGP", input.currency)
        assertEquals(cost, input.toCost())
    }
}
