package com.adhamelgendy.nixura.domain.repository

import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.money.HabitCost
import kotlinx.coroutines.flow.Flow

interface HabitTrackingRepository {
    fun observeActiveHabitTracking(): Flow<List<HabitTracking>>

    suspend fun createHabitAndStartTracking(
        name: String,
        startAtEpochMillis: Long,
        colorArgb: Long?,
        cost: HabitCost? = null,
        hideNameOutside: Boolean = false,
    ): Long

    suspend fun recordRelapse(habitId: Long, atEpochMillis: Long)

    /** One write, so a rename together with "hide name" never shows the new name outside the app in between. */
    suspend fun updateHabit(habitId: Long, name: String, colorArgb: Long?, hideNameOutside: Boolean)

    /** "Hide name outside the app", on its own (the card's long-press sheet). */
    suspend fun setHideNameOutside(habitId: Long, hidden: Boolean)

    /** Sets or clears (`null`) the habit's money-saved tracker. */
    suspend fun updateCost(habitId: Long, cost: HabitCost?)

    suspend fun archiveHabit(habitId: Long, atEpochMillis: Long)
}
