package com.example.reclaimapp.domain.repository

import com.example.reclaimapp.domain.model.HabitTracking
import kotlinx.coroutines.flow.Flow

interface HabitTrackingRepository {
    fun observeActiveHabitTracking(): Flow<List<HabitTracking>>

    suspend fun createHabitAndStartTracking(name: String, startAtEpochMillis: Long, colorArgb: Long?): Long

    suspend fun recordRelapse(habitId: Long, atEpochMillis: Long)

    suspend fun updateHabit(habitId: Long, name: String, colorArgb: Long?)

    suspend fun archiveHabit(habitId: Long, atEpochMillis: Long)
}
