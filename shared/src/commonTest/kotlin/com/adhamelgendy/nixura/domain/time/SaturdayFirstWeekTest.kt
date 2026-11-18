package com.adhamelgendy.nixura.domain.time

import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import kotlin.test.Test
import kotlin.test.assertEquals

/** Every NixUra week starts on Saturday (owner decision 2026-11-18), in every language. */
class SaturdayFirstWeekTest {

    @Test
    fun saturdayIsTheFirstColumnAndFridayTheLast() {
        val saturday = CivilCalendar.dayIndexFromCivil(2026, 11, 14)
        assertEquals(6, CivilCalendar.isoWeekday(saturday))
        assertEquals(0, CivilCalendar.weekColumn(saturday))
        assertEquals(1, CivilCalendar.weekColumn(saturday + 1)) // Sunday
        assertEquals(6, CivilCalendar.weekColumn(saturday + 6)) // Friday
    }

    @Test
    fun weekStartIsTheSaturdayOnOrBefore() {
        val saturday = CivilCalendar.dayIndexFromCivil(2026, 11, 14)
        (0L..6L).forEach { assertEquals(saturday, CivilCalendar.weekStartOf(saturday + it)) }
        assertEquals(saturday + 7, CivilCalendar.weekStartOf(saturday + 7))
    }

    @Test
    fun calendarColumnsMatchTheWeeklyGoalWeek() {
        // The month grid, the card strips and weekly goals must agree on where a week starts.
        val start = CivilCalendar.dayIndexFromCivil(2026, 1, 1)
        for (day in start until start + 400) {
            assertEquals(WeeklyGoalCalculator.weekStartOf(day), day - CivilCalendar.weekColumn(day), "day $day")
        }
    }

    @Test
    fun weekdayLettersStartOnSaturday() {
        val iso = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
        assertEquals(listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr"), CivilCalendar.inWeekOrder(iso))
    }

    @Test
    fun november2026OpensInTheSecondColumn() {
        // 1 November 2026 is a Sunday.
        assertEquals(1, CivilCalendar.weekColumn(CivilCalendar.dayIndexFromCivil(2026, 11, 1)))
    }
}
