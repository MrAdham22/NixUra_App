package com.adhamelgendy.nixura.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WheelMathTest {
    private val minutes = WheelMath(0..59, loop = true)
    private val hours = WheelMath(1..12, loop = true)
    private val days = WheelMath(0..3650, loop = false)

    @Test fun everyMinuteIsOnTheWheel() {
        val start = minutes.startRowOf(0)
        assertEquals((0..59).toList(), (start until start + 60).map(minutes::valueAt))
    }

    @Test fun fiftyNineIsFollowedByZero() {
        val row = minutes.startRowOf(59)
        assertEquals(0, minutes.valueAt(row + 1))
        assertEquals(59, minutes.valueAt(minutes.startRowOf(0) - 1))
    }

    @Test fun twelveIsFollowedByOne() {
        assertEquals(1, hours.valueAt(hours.startRowOf(12) + 1))
        assertEquals(12, hours.valueAt(hours.startRowOf(1) - 1))
    }

    @Test fun itStartsOnTheValueWithRoomBothWays() {
        for (minute in 0..59) {
            val row = minutes.startRowOf(minute)
            assertEquals(minute, minutes.valueAt(row))
            assertTrue(row > 1000 && row < minutes.rowCount - 1000)
        }
    }

    @Test fun aValueSetFromOutsideTakesTheShortWay() {
        val fiftyNine = minutes.startRowOf(59)
        assertEquals(fiftyNine + 1, minutes.nearestRowOf(0, fiftyNine))
        val zero = minutes.startRowOf(0)
        assertEquals(zero - 1, minutes.nearestRowOf(59, zero))
    }

    @Test fun aWheelThatDoesNotLoopStopsAtItsEnds() {
        assertEquals(3651, days.rowCount)
        assertEquals(0, days.valueAt(-1))
        assertEquals(3650, days.valueAt(9999))
        assertEquals(3650, days.startRowOf(5000))
        assertEquals(40, days.nearestRowOf(40, 900))
    }

    @Test fun everyMinuteOfTheDayRoundTrips() {
        for (minute in 0 until 24 * 60) assertEquals(minute, ClockSelection.of(minute).minutesOfDay)
    }

    @Test fun midnightAndNoonReadAsTwelve() {
        assertEquals(ClockSelection(12, 0, pm = false), ClockSelection.of(0))
        assertEquals(ClockSelection(12, 0, pm = true), ClockSelection.of(12 * 60))
        assertEquals(ClockSelection(5, 56, pm = true), ClockSelection.of(17 * 60 + 56))
    }
}
