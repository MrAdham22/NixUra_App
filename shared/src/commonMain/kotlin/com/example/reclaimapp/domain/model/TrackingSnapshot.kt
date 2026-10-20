package com.example.reclaimapp.domain.model

data class TrackingSnapshot(
    val activePeriodStartEpochMillis: Long?,
    val activeElapsedMillis: Long,
    val longestStreakMillis: Long,
)

data class HabitTracking(
    val habit: Habit,
    val events: List<HabitEvent>,
    val tracking: TrackingSnapshot,
)
