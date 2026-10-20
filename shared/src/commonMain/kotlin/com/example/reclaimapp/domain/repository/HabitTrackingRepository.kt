package com.example.reclaimapp.domain.repository

import com.example.reclaimapp.domain.model.HabitTracking
import kotlinx.coroutines.flow.Flow

interface HabitTrackingRepository {
    fun observeActiveHabitTracking(): Flow<List<HabitTracking>>

    suspend fun createHabitAndStartTracking(name: String, startAtEpochMillis: Long): Long

    suspend fun recordRelapse(habitId: Long, atEpochMillis: Long)
}
