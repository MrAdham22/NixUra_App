package com.adhamelgendy.nixura.domain.time

import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import kotlin.test.Test
import kotlin.test.assertEquals

class SundayFirstWeekTest {
    @Test
    fun sundayIsTheFirstColumnAndSaturdayTheLast() {
        val sunday = CivilCalendar.dayIndexFromCivil(2026, 11, 15)
        assertEquals(0, CivilCalendar.sundayFirstColumn(sunday))
        assertEquals(1, CivilCalendar.sundayFirstColumn(sunday + 1))
        assertEquals(6, CivilCalendar.sundayFirstColumn(sunday + 6))
    }

    @Test
    fun calendarColumnsMatchTheWeeklyGoalWeek() {
        // The month grid and weekly goals must agree on where a week starts.
        val start = CivilCalendar.dayIndexFromCivil(2026, 1, 1)
        for (day in start until start + 400) {
            assertEquals(WeeklyGoalCalculator.weekStartOf(day), day - CivilCalendar.sundayFirstColumn(day), "day $day")
        }
    }

    @Test
    fun november2026OpensOnSunday() {
        assertEquals(0, CivilCalendar.sundayFirstColumn(CivilCalendar.dayIndexFromCivil(2026, 11, 1)))
    }
}
