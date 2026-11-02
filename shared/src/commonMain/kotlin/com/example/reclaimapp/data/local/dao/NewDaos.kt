package com.example.reclaimapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reclaimapp.data.local.entity.CoachMessageEntity
import com.example.reclaimapp.data.local.entity.EventEntity
import com.example.reclaimapp.data.local.entity.GoodHabitEntity
import com.example.reclaimapp.data.local.entity.GoodHabitLogEntity
import com.example.reclaimapp.data.local.entity.PreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoodHabitDao {
    @Query("SELECT * FROM good_habits WHERE archived_at IS NULL AND deleted_at IS NULL ORDER BY created_at ASC, id ASC")
    fun observeActive(): Flow<List<GoodHabitEntity>>

    @Insert
    suspend fun insert(habit: GoodHabitEntity): Long

    @Query("UPDATE good_habits SET name = :name, daily_target = :dailyTarget, color_argb = :colorArgb, updated_at = :updatedAt, dirty = 1 WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, updatedAt: Long)

    @Query("UPDATE good_habits SET archived_at = :archivedAt, updated_at = :archivedAt, dirty = 1 WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Long)

    @Query("SELECT sync_id FROM good_habits WHERE id = :id LIMIT 1")
    suspend fun syncIdOf(id: Long): String?
}

@Dao
interface GoodHabitLogDao {
    @Query("SELECT * FROM good_habit_logs WHERE deleted_at IS NULL ORDER BY day_index ASC")
    fun observeAll(): Flow<List<GoodHabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: GoodHabitLogEntity)

    /** Clearing a day keeps a marked row, so the clear also reaches other devices. */
    @Query("UPDATE good_habit_logs SET count = 0, deleted_at = :at, updated_at = :at, dirty = 1 WHERE good_habit_id = :goodHabitId AND day_index = :dayIndex")
    suspend fun markDeleted(goodHabitId: Long, dayIndex: Long, at: Long)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE deleted_at IS NULL ORDER BY target_at ASC, id ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Insert
    suspend fun insert(event: EventEntity): Long

    @Query("UPDATE events SET title = :title, category = :category, target_at = :targetAt, reminders = :reminders, updated_at = :updatedAt, dirty = 1 WHERE id = :id")
    suspend fun update(id: Long, title: String, category: String, targetAt: Long, reminders: String, updatedAt: Long)

    /** Deleting keeps a marked row, so the delete also reaches other devices. */
    @Query("UPDATE events SET deleted_at = :at, updated_at = :at, dirty = 1 WHERE id = :id")
    suspend fun markDeleted(id: Long, at: Long)
}

@Dao
interface CoachMessageDao {
    @Query("SELECT * FROM coach_messages ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<CoachMessageEntity>>

    @Insert
    suspend fun insert(message: CoachMessageEntity): Long

    @Query("DELETE FROM coach_messages")
    suspend fun clear()
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences")
    fun observeAll(): Flow<List<PreferenceEntity>>

    @Query("SELECT * FROM preferences")
    suspend fun getAll(): List<PreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PreferenceEntity>)

    @Query("DELETE FROM preferences WHERE pref_key = :key")
    suspend fun delete(key: String)
}
