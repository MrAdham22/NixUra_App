package com.example.reclaimapp.domain.calculator

import com.example.reclaimapp.domain.model.HabitEvent
import com.example.reclaimapp.domain.model.HabitEventType
import com.example.reclaimapp.domain.model.TrackingSnapshot

object TrackingStateCalculator {
    fun compute(events: List<HabitEvent>, nowEpochMillis: Long): TrackingSnapshot {
        require(nowEpochMillis >= 0) { "now must be non-negative" }

        val sorted = events.sortedWith(compareBy(HabitEvent::occurredAtEpochMillis, HabitEvent::id))
        var periodStart: Long? = null
        var longestCompletedMillis = 0L

        for (event in sorted) {
            when (event.type) {
                HabitEventType.TRACKING_START -> {
                    periodStart = event.occurredAtEpochMillis
                }
                HabitEventType.RELAPSE -> {
                    val start = periodStart ?: continue
                    val completed = (event.occurredAtEpochMillis - start).coerceAtLeast(0)
                    if (completed > longestCompletedMillis) {
                        longestCompletedMillis = completed
                    }
                    periodStart = event.occurredAtEpochMillis
                }
            }
        }

        val activeElapsedMillis = periodStart?.let { start ->
            (nowEpochMillis - start).coerceAtLeast(0)
        } ?: 0L

        val longestStreakMillis = maxOf(longestCompletedMillis, activeElapsedMillis)

        return TrackingSnapshot(
            activePeriodStartEpochMillis = periodStart,
            activeElapsedMillis = activeElapsedMillis,
            longestStreakMillis = longestStreakMillis,
        )
    }
}
