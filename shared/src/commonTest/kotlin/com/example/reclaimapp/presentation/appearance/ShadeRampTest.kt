package com.example.reclaimapp.presentation.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class ShadeRampTest {
    private val config = StreakColorProgressionConfig.Default
    private val base = 0xFFAE445AL

    private fun luminance(argb: Long): Float {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    @Test
    fun stopsGetMonotonicallyLighter() {
        val stops = ShadeRamp.stops(base, config.stopCount)
        assertEquals(config.stopCount, stops.size)
        stops.zipWithNext().forEach { (a, b) -> assertTrue(luminance(a) < luminance(b), "stops must get lighter") }
    }

    @Test
    fun startIsAlmostDarkestShadeOfChosenColor() {
        val stops = ShadeRamp.stops(base, config.stopCount)
        val start = stops.first()
        assertTrue(luminance(start) < luminance(base) * 0.5f)
        // hue preserved: red channel still dominant
        assertTrue(((start shr 16) and 0xFF) > (start and 0xFF))
    }

    @Test
    fun streakProgressesFromDarkToLight() {
        val stops = ShadeRamp.stops(base, config.stopCount)
        fun at(d: kotlin.time.Duration) = ShadeRamp.colorFor(stops, StreakProgression.segmentFor(d, config))
        val day0 = luminance(at(kotlin.time.Duration.ZERO))
        val day3 = luminance(at(3.days))
        val day14 = luminance(at(14.days))
        val day30 = luminance(at(30.days))
        assertTrue(day0 < day3 && day3 < day14 && day14 < day30)
        assertEquals(stops.last(), at(60.days))
    }

    @Test
    fun relapseResetReturnsToDarkestStart() {
        val stops = ShadeRamp.stops(base, config.stopCount)
        val reset = ShadeRamp.colorFor(stops, StreakProgression.segmentFor(kotlin.time.Duration.ZERO, config))
        assertEquals(stops.first(), reset)
    }
}
