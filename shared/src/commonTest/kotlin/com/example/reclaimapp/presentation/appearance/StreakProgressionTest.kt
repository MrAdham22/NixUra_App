package com.example.reclaimapp.presentation.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class StreakProgressionTest {

    private val config = StreakColorProgressionConfig.Default

    @Test
    fun startUsesFirstSegment() {
        val seg = StreakProgression.segmentFor(0.milliseconds, config)
        assertEquals(0, seg.fromStopIndex)
        assertEquals(1, seg.toStopIndex)
        assertEquals(0f, seg.fraction)
    }

    @Test
    fun atOneDayBoundaryStartsSecondSegment() {
        val seg = StreakProgression.segmentFor(1.days, config)
        assertEquals(1, seg.fromStopIndex)
        assertEquals(2, seg.toStopIndex)
        assertEquals(0f, seg.fraction)
    }

    @Test
    fun atThirtyDaysUsesFinalStopOnly() {
        val seg = StreakProgression.segmentFor(30.days, config)
        assertEquals(config.stopCount - 1, seg.fromStopIndex)
        assertEquals(config.stopCount - 1, seg.toStopIndex)
    }

    @Test
    fun relapseResetIsZeroElapsed() {
        val before = StreakProgression.activeStageIndex(14.days, config)
        val after = StreakProgression.activeStageIndex(0.milliseconds, config)
        assertEquals(true, before > after)
        assertEquals(0, after)
    }

    @Test
    fun midSegmentInterpolationFraction() {
        val seg = StreakProgression.segmentFor(2.days, config)
        assertEquals(1, seg.fromStopIndex)
        assertEquals(2, seg.toStopIndex)
        assertEquals(true, seg.fraction > 0.4f && seg.fraction < 0.6f)
    }

    @Test
    fun stageIndexAtTwelveHoursStaysOnFirstMilestone() {
        assertEquals(0, StreakProgression.activeStageIndex(12.hours, config))
    }

    @Test
    fun stageIndexAtOneDayBoundary() {
        assertEquals(1, StreakProgression.activeStageIndex(1.days, config))
    }

    @Test
    fun stageIndexMidSegmentBeforeNextMilestone() {
        assertEquals(1, StreakProgression.activeStageIndex(2.days, config))
    }

    @Test
    fun stageIndexAtThreeDayMilestone() {
        assertEquals(2, StreakProgression.activeStageIndex(3.days, config))
    }

    @Test
    fun stageIndexAtThirtyDays() {
        assertEquals(config.stopCount - 1, StreakProgression.activeStageIndex(30.days, config))
    }
}
