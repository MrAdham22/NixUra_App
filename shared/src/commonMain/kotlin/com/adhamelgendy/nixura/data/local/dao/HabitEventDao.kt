package com.adhamelgendy.nixura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adhamelgendy.nixura.data.local.entity.HabitEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitEventDao {
    @Query("SELECT * FROM habit_events WHERE deleted_at IS NULL ORDER BY occurred_at ASC, id ASC")
    fun observeAllEvents(): Flow<List<HabitEventEntity>>

    @Query("SELECT * FROM habit_events WHERE habit_id = :habitId AND deleted_at IS NULL ORDER BY occurred_at ASC, id ASC")
    suspend fun getEventsForHabit(habitId: Long): List<HabitEventEntity>

    @Insert
    suspend fun insert(event: HabitEventEntity): Long
}
