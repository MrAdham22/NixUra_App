package com.example.reclaimapp.domain.calculator

import com.example.reclaimapp.domain.model.HabitEvent
import com.example.reclaimapp.domain.model.HabitEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TrackingStateCalculatorTest {

    private val t0 = 1_000_000L

    @Test
    fun noEventsIsZeroActive() {
        val snapshot = TrackingStateCalculator.compute(emptyList(), t0 + 5_000)
        assertEquals(0L, snapshot.activeElapsedMillis)
        assertEquals(0L, snapshot.longestStreakMillis)
        assertEquals(null, snapshot.activePeriodStartEpochMillis)
    }

    @Test
    fun trackingStartSetsActiveElapsed() {
        val events = listOf(
            event(1, HabitEventType.TRACKING_START, t0),
        )
        val snapshot = TrackingStateCalculator.compute(events, t0 + 90_000)
        assertEquals(t0, snapshot.activePeriodStartEpochMillis)
        assertEquals(90_000L, snapshot.activeElapsedMillis)
        assertEquals(90_000L, snapshot.longestStreakMillis)
    }

    @Test
    fun relapsePreservesLongestAndResetsActivePeriod() {
        val relapseAt = t0 + 2.days.inWholeMilliseconds
        val events = listOf(
            event(1, HabitEventType.TRACKING_START, t0),
            event(2, HabitEventType.RELAPSE, relapseAt),
        )
        val now = relapseAt + 1.hours.inWholeMilliseconds
        val snapshot = TrackingStateCalculator.compute(events, now)
        assertEquals(relapseAt, snapshot.activePeriodStartEpochMillis)
        assertEquals(1.hours.inWholeMilliseconds, snapshot.activeElapsedMillis)
        assertEquals(2.days.inWholeMilliseconds, snapshot.longestStreakMillis)
    }

    @Test
    fun multipleRelapsesTrackLongestCompletedPeriod() {
        val firstRelapse = t0 + 1.days.inWholeMilliseconds
        val secondRelapse = firstRelapse + 3.days.inWholeMilliseconds
        val events = listOf(
            event(1, HabitEventType.TRACKING_START, t0),
            event(2, HabitEventType.RELAPSE, firstRelapse),
            event(3, HabitEventType.RELAPSE, secondRelapse),
        )
        val now = secondRelapse + 30.minutes.inWholeMilliseconds
        val snapshot = TrackingStateCalculator.compute(events, now)
        assertEquals(3.days.inWholeMilliseconds, snapshot.longestStreakMillis)
        assertEquals(30.minutes.inWholeMilliseconds, snapshot.activeElapsedMillis)
    }

    private fun event(id: Long, type: HabitEventType, at: Long) = HabitEvent(
        id = id,
        habitId = 1L,
        type = type,
        occurredAtEpochMillis = at,
    )
}
