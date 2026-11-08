package com.adhamelgendy.nixura.domain.repository

import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.money.HabitCost
import kotlinx.coroutines.flow.Flow

interface HabitTrackingRepository {
    fun observeActiveHabitTracking(): Flow<List<HabitTracking>>

    suspend fun createHabitAndStartTracking(name: String, startAtEpochMillis: Long, colorArgb: Long?, cost: HabitCost? = null): Long

    suspend fun recordRelapse(habitId: Long, atEpochMillis: Long)

    suspend fun updateHabit(habitId: Long, name: String, colorArgb: Long?)

    /** Sets or clears (`null`) the habit's money-saved tracker. */
    suspend fun updateCost(habitId: Long, cost: HabitCost?)

    suspend fun archiveHabit(habitId: Long, atEpochMillis: Long)
}
