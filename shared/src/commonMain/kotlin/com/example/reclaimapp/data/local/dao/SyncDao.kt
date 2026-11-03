package com.example.reclaimapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.reclaimapp.data.local.entity.EventEntity
import com.example.reclaimapp.data.local.entity.GoodHabitEntity
import com.example.reclaimapp.data.local.entity.GoodHabitLogEntity
import com.example.reclaimapp.data.local.entity.HabitEntity
import com.example.reclaimapp.data.local.entity.HabitEventEntity
import kotlinx.coroutines.flow.Flow

/** What the sync layer needs from the tables. These reads include rows marked as deleted, on purpose. */
@Dao
abstract class SyncDao {
    @Transaction
    open suspend fun inTransaction(block: suspend () -> Unit) = block()

    @Query(
        "SELECT (SELECT COUNT(*) FROM habits WHERE dirty = 1) + (SELECT COUNT(*) FROM habit_events WHERE dirty = 1) + " +
            "(SELECT COUNT(*) FROM good_habits WHERE dirty = 1) + (SELECT COUNT(*) FROM good_habit_logs WHERE dirty = 1) + " +
            "(SELECT COUNT(*) FROM events WHERE dirty = 1)",
    )
    abstract fun observeDirtyCount(): Flow<Int>

    // Waiting to upload
    @Query("SELECT * FROM habits WHERE dirty = 1 LIMIT :limit")
    abstract suspend fun dirtyHabits(limit: Int): List<HabitEntity>

    @Query("SELECT * FROM habit_events WHERE dirty = 1 LIMIT :limit")
    abstract suspend fun dirtyHabitEvents(limit: Int): List<HabitEventEntity>

    @Query("SELECT * FROM good_habits WHERE dirty = 1 LIMIT :limit")
    abstract suspend fun dirtyGoodHabits(limit: Int): List<GoodHabitEntity>

    @Query("SELECT * FROM good_habit_logs WHERE dirty = 1 LIMIT :limit")
    abstract suspend fun dirtyGoodHabitLogs(limit: Int): List<GoodHabitLogEntity>

    @Query("SELECT * FROM events WHERE dirty = 1 LIMIT :limit")
    abstract suspend fun dirtyEvents(limit: Int): List<EventEntity>

    // Uploaded. The time check keeps a row dirty if it was edited again while the upload was running.
    @Query("UPDATE habits SET dirty = 0 WHERE sync_id = :syncId AND updated_at = :updatedAt")
    abstract suspend fun cleanHabit(syncId: String, updatedAt: Long)

    @Query("UPDATE habit_events SET dirty = 0 WHERE sync_id = :syncId AND updated_at = :updatedAt")
    abstract suspend fun cleanHabitEvent(syncId: String, updatedAt: Long)

    @Query("UPDATE good_habits SET dirty = 0 WHERE sync_id = :syncId AND updated_at = :updatedAt")
    abstract suspend fun cleanGoodHabit(syncId: String, updatedAt: Long)

    @Query("UPDATE good_habit_logs SET dirty = 0 WHERE sync_id = :syncId AND updated_at = :updatedAt")
    abstract suspend fun cleanGoodHabitLog(syncId: String, updatedAt: Long)

    @Query("UPDATE events SET dirty = 0 WHERE sync_id = :syncId AND updated_at = :updatedAt")
    abstract suspend fun cleanEvent(syncId: String, updatedAt: Long)

    // Lookups
    @Query("SELECT * FROM habits WHERE sync_id = :syncId LIMIT 1")
    abstract suspend fun habitBySyncId(syncId: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    abstract suspend fun habitById(id: Long): HabitEntity?

    @Query("SELECT * FROM habit_events WHERE sync_id = :syncId LIMIT 1")
    abstract suspend fun habitEventBySyncId(syncId: String): HabitEventEntity?

    @Query("SELECT * FROM good_habits WHERE sync_id = :syncId LIMIT 1")
    abstract suspend fun goodHabitBySyncId(syncId: String): GoodHabitEntity?

    @Query("SELECT * FROM good_habits WHERE id = :id LIMIT 1")
    abstract suspend fun goodHabitById(id: Long): GoodHabitEntity?

    @Query("SELECT * FROM good_habit_logs WHERE sync_id = :syncId LIMIT 1")
    abstract suspend fun goodHabitLogBySyncId(syncId: String): GoodHabitLogEntity?

    @Query("SELECT * FROM events WHERE sync_id = :syncId LIMIT 1")
    abstract suspend fun eventBySyncId(syncId: String): EventEntity?

    @Query("SELECT * FROM events WHERE source = :source AND external_id = :externalId LIMIT 1")
    abstract suspend fun eventByExternal(source: String, externalId: String): EventEntity?

    // First sign-in with guest data: find the same thing under a different id.
    @Query("SELECT * FROM habits WHERE dirty = 1 AND archived_at IS NULL AND deleted_at IS NULL AND lower(trim(name)) = :name LIMIT 1")
    abstract suspend fun guestHabitNamed(name: String): HabitEntity?

    @Query("SELECT * FROM good_habits WHERE dirty = 1 AND archived_at IS NULL AND deleted_at IS NULL AND lower(trim(name)) = :name LIMIT 1")
    abstract suspend fun guestGoodHabitNamed(name: String): GoodHabitEntity?

    @Query("SELECT * FROM events WHERE dirty = 1 AND deleted_at IS NULL AND lower(trim(title)) = :title AND target_at = :targetAt LIMIT 1")
    abstract suspend fun guestEventMatching(title: String, targetAt: Long): EventEntity?

    @Query("UPDATE habits SET sync_id = :syncId WHERE id = :id")
    abstract suspend fun rekeyHabit(id: Long, syncId: String)

    @Query("UPDATE good_habits SET sync_id = :syncId WHERE id = :id")
    abstract suspend fun rekeyGoodHabit(id: Long, syncId: String)

    @Query("UPDATE good_habit_logs SET sync_id = :goodHabitSyncId || ':' || day_index WHERE good_habit_id = :goodHabitId")
    abstract suspend fun rekeyGoodHabitLogs(goodHabitId: Long, goodHabitSyncId: String)

    @Query("UPDATE events SET sync_id = :syncId WHERE id = :id")
    abstract suspend fun rekeyEvent(id: Long, syncId: String)

    /** A merged habit has two "started tracking" events; the earliest one stands, so the streak history stays correct. */
    @Query(
        "UPDATE habit_events SET deleted_at = :at, updated_at = :at, dirty = 1 WHERE habit_id = :habitId AND event_type = 'TRACKING_START' " +
            "AND deleted_at IS NULL AND id <> (SELECT id FROM habit_events WHERE habit_id = :habitId AND event_type = 'TRACKING_START' " +
            "AND deleted_at IS NULL ORDER BY occurred_at ASC, id ASC LIMIT 1)",
    )
    abstract suspend fun keepEarliestTrackingStart(habitId: Long, at: Long)

    // Writing what came from the server
    @Insert
    abstract suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    abstract suspend fun updateHabit(habit: HabitEntity)

    @Insert
    abstract suspend fun insertHabitEvent(event: HabitEventEntity): Long

    @Update
    abstract suspend fun updateHabitEvent(event: HabitEventEntity)

    @Insert
    abstract suspend fun insertGoodHabit(habit: GoodHabitEntity): Long

    @Update
    abstract suspend fun updateGoodHabit(habit: GoodHabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertGoodHabitLog(log: GoodHabitLogEntity)

    @Insert
    abstract suspend fun insertEvent(event: EventEntity): Long

    @Update
    abstract suspend fun updateEvent(event: EventEntity)
}
