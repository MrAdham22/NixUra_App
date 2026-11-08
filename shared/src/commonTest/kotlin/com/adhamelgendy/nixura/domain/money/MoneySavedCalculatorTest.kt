package com.adhamelgendy.nixura.domain.money

import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import com.adhamelgendy.nixura.domain.recognition.HabitRecognizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneySavedCalculatorTest {

    private val day = 86_400_000L

    // 20 a day from packs of 20 at EGP 100.00 = EGP 100 a day
    private val cigarettes = HabitCost("Cleopatra", CostUnit.CIGARETTE, unitsPerDay = 20.0, pricePerPackMinor = 10_000, unitsPerPack = 20, currency = "EGP")

    @Test
    fun savesTheDailyCostForEveryCleanDay() {
        val r = MoneySavedCalculator.saved(cigarettes, 17 * day)
        assertEquals(170_000, r.savedMinor) // EGP 1,700.00
        assertEquals(340, r.unitsAvoided) // 340 cigarettes not smoked
    }

    @Test
    fun partOfADayCountsPartly() {
        val half = MoneySavedCalculator.saved(cigarettes, day / 2)
        assertEquals(5_000, half.savedMinor)
        assertEquals(10, half.unitsAvoided)
    }

    @Test
    fun packsPerDayAndCansWork() {
        val packs = cigarettes.copy(unitsPerDay = 1.5 * 20, perDayInPacks = true) // 1.5 packs a day
        assertEquals(150_00, MoneySavedCalculator.saved(packs, day).savedMinor)
        val cola = HabitCost("Pepsi", CostUnit.CAN, unitsPerDay = 3.0, pricePerPackMinor = 1_500, unitsPerPack = 1, currency = "EGP")
        assertEquals(4_500 * 7L, MoneySavedCalculator.saved(cola, 7 * day).savedMinor)
        assertEquals(21, MoneySavedCalculator.saved(cola, 7 * day).unitsAvoided)
    }

    @Test
    fun noTimeOrNegativeTimeSavesNothing() {
        assertEquals(0, MoneySavedCalculator.saved(cigarettes, 0).savedMinor)
        assertEquals(0, MoneySavedCalculator.saved(cigarettes, -5 * day).savedMinor)
    }

    @Test
    fun aRelapseStartsANewStreakLikeTheCounter() {
        val start = 1_000L
        val streaks = MoneySavedCalculator.streaks(cigarettes, start, listOf(start + 3 * day), start + 5 * day)
        assertEquals(2, streaks.size)
        assertEquals(30_000, streaks[0].savedMinor) // 3 days before the slip
        assertEquals(20_000, streaks[1].savedMinor) // 2 days since: what "saved now" shows
        assertTrue(streaks[1].isCurrent)
        assertTrue(MoneySavedCalculator.streaks(cigarettes, null, emptyList(), start).isEmpty())
    }

    @Test
    fun pricesParseAndFormatInTheCurrencyDecimals() {
        assertEquals(3_550, Currencies.parseMinor("35.5", "EGP"))
        assertEquals(3_550, Currencies.parseMinor("35,50", "EGP"))
        assertEquals(1_250, Currencies.parseMinor("1.25", "KWD"))
        assertNull(Currencies.parseMinor("abc", "EGP"))
        assertNull(Currencies.parseMinor("-3", "EGP"))
        assertEquals("EGP 1,700.00", Currencies.format(170_000, "EGP"))
        assertEquals("$12.40", Currencies.format(1_240, "USD"))
        assertEquals("35.5", Currencies.toInput(3_550, "EGP"))
        assertEquals("USD", Currencies.defaultFor("USD"))
        assertEquals("EGP", Currencies.defaultFor("XYZ"))
        assertEquals("EGP", Currencies.defaultFor(null))
    }

    @Test
    fun onlyCountableHabitsGetATracker() {
        val countable = CountableHabits(HabitRecognizer(HabitKnowledgeBase()))
        assertEquals(CostUnit.CIGARETTE, assertNotNull(countable.profileFor("Smoking")).units.first())
        assertNotNull(countable.profileFor("cigarettes"))
        assertNotNull(countable.profileFor("smokng")) // typo
        assertNotNull(countable.profileFor("cola"))
        assertNotNull(countable.profileFor("energy drinks"))
        assertNotNull(countable.profileFor("coffee"))
        assertNotNull(countable.profileFor("alcohol"))
        assertNull(countable.profileFor("procrastination"))
        assertNull(countable.profileFor("social media"))
        assertNull(countable.profileFor("nail biting"))
    }
}
