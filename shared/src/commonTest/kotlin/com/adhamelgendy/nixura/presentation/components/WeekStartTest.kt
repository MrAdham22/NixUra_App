package com.adhamelgendy.nixura.presentation.components

import com.adhamelgendy.nixura.domain.time.CivilCalendar
import kotlin.test.Test
import kotlin.test.assertEquals

class WeekStartTest {

    private val isoLetters = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    @Test
    fun englishStartsOnSundayAndArabicOnSaturday() {
        assertEquals(WeekStart.SUNDAY, WeekStart.forLocale("en", rtl = false))
        assertEquals(WeekStart.SUNDAY, WeekStart.forLocale(null, rtl = false))
        assertEquals(WeekStart.SATURDAY, WeekStart.forLocale("ar", rtl = false))
        assertEquals(WeekStart.SATURDAY, WeekStart.forLocale("ar-EG", rtl = false))
        assertEquals(WeekStart.SATURDAY, WeekStart.forLocale(null, rtl = true))
    }

    @Test
    fun headerLettersFollowTheWeekStart() {
        assertEquals(listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"), WeekStart.SUNDAY.reorder(isoLetters))
        assertEquals(listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr"), WeekStart.SATURDAY.reorder(isoLetters))
    }

    @Test
    fun firstDayOfTheMonthLandsInTheRightColumn() {
        // 1 November 2026 is a Sunday.
        val nov1 = CivilCalendar.dayIndexFromCivil(2026, 11, 1)
        assertEquals(7, CivilCalendar.isoWeekday(nov1))
        assertEquals(0, WeekStart.SUNDAY.columnOf(nov1))
        assertEquals(1, WeekStart.SATURDAY.columnOf(nov1))
        // 1 August 2026 is a Saturday.
        val aug1 = CivilCalendar.dayIndexFromCivil(2026, 8, 1)
        assertEquals(6, WeekStart.SUNDAY.columnOf(aug1))
        assertEquals(0, WeekStart.SATURDAY.columnOf(aug1))
    }

    @Test
    fun sundayColumnsMatchTheOldSundayFirstGrid() {
        (0L until 14L).forEach { d -> assertEquals(CivilCalendar.sundayFirstColumn(d), WeekStart.SUNDAY.columnOf(d)) }
    }
}
