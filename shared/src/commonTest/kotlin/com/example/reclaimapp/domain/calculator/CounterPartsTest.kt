package com.example.reclaimapp.domain.calculator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CounterPartsTest {

    @Test
    fun splitsIntoDaysHoursMinutesSeconds() {
        val p = (3.days + 4.hours + 5.minutes + 6.seconds).toCounterParts()
        assertEquals(CounterParts(3, 4, 5, 6), p)
        assertEquals(
            listOf(CounterUnit.DAYS, CounterUnit.HOURS, CounterUnit.MINUTES, CounterUnit.SECONDS),
            p.visibleUnits,
        )
    }

    @Test
    fun hidesDaysUnderOneDayAndHoursUnderOneHour() {
        assertEquals(
            listOf(CounterUnit.HOURS, CounterUnit.MINUTES, CounterUnit.SECONDS),
            (5.hours + 1.seconds).toCounterParts().visibleUnits,
        )
        assertEquals(
            listOf(CounterUnit.MINUTES, CounterUnit.SECONDS),
            59.minutes.toCounterParts().visibleUnits,
        )
    }

    @Test
    fun negativeDurationClampsToZero() {
        assertEquals(CounterParts(0, 0, 0, 0), (-5).minutes.toCounterParts())
    }

    @Test
    fun zeroHoursInsideDaysStillShown() {
        val p = (2.days + 5.seconds).toCounterParts()
        assertEquals(0L, p.valueOf(CounterUnit.HOURS))
        assertEquals(4, p.visibleUnits.size)
        assertEquals(Duration.ZERO.toCounterParts().seconds, 0)
    }
}
