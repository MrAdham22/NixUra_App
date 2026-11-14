package com.adhamelgendy.nixura.domain.calculator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class FarSecondsTest {
    @Test
    fun aDayOrMoreAwayHidesTheSeconds() = assertEquals(
        listOf(CounterUnit.DAYS, CounterUnit.HOURS, CounterUnit.MINUTES),
        (2.days + 3.hours).toCounterParts().unitsWithoutFarSeconds,
    )

    @Test
    fun theLastDayKeepsTheSeconds() = assertEquals(
        listOf(CounterUnit.HOURS, CounterUnit.MINUTES, CounterUnit.SECONDS),
        (5.hours + 2.minutes).toCounterParts().unitsWithoutFarSeconds,
    )
}
