package com.example.reclaimapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.reclaimapp.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived_at IS NULL ORDER BY created_at ASC, id ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getById(habitId: Long): HabitEntity?

    @Query("UPDATE habits SET name = :name, color_argb = :colorArgb WHERE id = :habitId")
    suspend fun updateDetails(habitId: Long, name: String, colorArgb: Long?)

    @Query("UPDATE habits SET archived_at = :archivedAt WHERE id = :habitId")
    suspend fun archive(habitId: Long, archivedAt: Long)
}
