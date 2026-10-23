package com.example.reclaimapp.domain.time

import kotlin.test.Test
import kotlin.test.assertEquals

class DayCalculatorTest {

    @Test
    fun epochDayZeroIsThursdayFirstOfJanuary1970() {
        assertEquals(CivilDate(1970, 1, 1), CivilCalendar.civilFromDayIndex(0))
        assertEquals(4, CivilCalendar.isoWeekday(0))
    }

    @Test
    fun civilRoundTripAcrossYears() {
        for (day in -800L..30000L step 37L) {
            val c = CivilCalendar.civilFromDayIndex(day)
            assertEquals(day, CivilCalendar.dayIndexFromCivil(c.year, c.month, c.day))
        }
    }

    @Test
    fun knownDates() {
        // 2026-10-20 is a Tuesday
        val idx = CivilCalendar.dayIndexFromCivil(2026, 10, 20)
        assertEquals(CivilDate(2026, 10, 20), CivilCalendar.civilFromDayIndex(idx))
        assertEquals(2, CivilCalendar.isoWeekday(idx))
        assertEquals(29, CivilCalendar.monthLength(2024, 2))
        assertEquals(28, CivilCalendar.monthLength(2026, 2))
        assertEquals(29, CivilCalendar.monthLength(2000, 2))
        assertEquals(28, CivilCalendar.monthLength(1900, 2))
    }

    @Test
    fun addMonthsWrapsYears() {
        assertEquals(2027 to 1, CivilCalendar.addMonths(2026, 12, 1))
        assertEquals(2025 to 12, CivilCalendar.addMonths(2026, 1, -1))
        assertEquals(2026 to 6, CivilCalendar.addMonths(2026, 6, 0))
    }

    @Test
    fun dayIndexRespectsOffset() {
        val cairoSummer = DayCalculator { 3 * 3_600_000L }
        // 2026-10-20 22:30 UTC is already 2026-10-21 01:30 local
        val utc = DayCalculator { 0L }
        val instant = utc.epochMillisOf(2026, 10, 20, 22, 30)
        assertEquals(CivilCalendar.dayIndexFromCivil(2026, 10, 20), utc.dayIndexOf(instant))
        assertEquals(CivilCalendar.dayIndexFromCivil(2026, 10, 21), cairoSummer.dayIndexOf(instant))
    }

    @Test
    fun startOfDayIsLocalMidnight() {
        val calc = DayCalculator { 2 * 3_600_000L }
        val day = CivilCalendar.dayIndexFromCivil(2026, 3, 1)
        val start = calc.startOfDayMillis(day)
        assertEquals(day, calc.dayIndexOf(start))
        assertEquals(0L, calc.millisOfDay(start))
        assertEquals(day - 1, calc.dayIndexOf(start - 1))
    }
}
