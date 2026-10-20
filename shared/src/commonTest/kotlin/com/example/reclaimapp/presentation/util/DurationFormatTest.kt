package com.example.reclaimapp.presentation.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DurationFormatTest {

    @Test
    fun zeroElapsedTime() {
        assertEquals("00:00", formatElapsed(0.seconds))
    }

    @Test
    fun oneSecond() {
        assertEquals("00:01", formatElapsed(1.seconds))
    }

    @Test
    fun fiftyNineSeconds() {
        assertEquals("00:59", formatElapsed(59.seconds))
    }

    @Test
    fun oneMinute() {
        assertEquals("01:00", formatElapsed(1.minutes))
    }

    @Test
    fun fiftyNineMinutesFiftyNineSeconds() {
        assertEquals("59:59", formatElapsed(59.minutes + 59.seconds))
    }

    @Test
    fun exactlyOneHour() {
        assertEquals("01:00:00", formatElapsed(1.hours))
    }

    @Test
    fun oneHourOneMinuteOneSecond() {
        assertEquals("01:01:01", formatElapsed(1.hours + 1.minutes + 1.seconds))
    }

    @Test
    fun exactlyTwentyFourHours() {
        assertEquals("1d 00:00:00", formatElapsed(24.hours))
    }

    @Test
    fun oneDayPlusOneSecond() {
        assertEquals("1d 00:00:01", formatElapsed(1.days + 1.seconds))
    }

    @Test
    fun multipleDaysWithNonzeroHms() {
        assertEquals(
            "7d 12:34:56",
            formatElapsed(7.days + 12.hours + 34.minutes + 56.seconds),
        )
    }

    @Test
    fun exactlyThirtyDays() {
        assertEquals("30d 00:00:00", formatElapsed(30.days))
    }

    @Test
    fun negativeDurationIsClampedToZero() {
        assertEquals("00:00", formatElapsed((-5).seconds))
    }

    @Test
    fun formatBoundaryJustBelowOneHour() {
        assertEquals("59:59", formatElapsed(1.hours - 1.seconds))
    }

    @Test
    fun formatBoundaryJustBelowOneDay() {
        assertEquals("23:59:59", formatElapsed(1.days - 1.seconds))
    }
}
