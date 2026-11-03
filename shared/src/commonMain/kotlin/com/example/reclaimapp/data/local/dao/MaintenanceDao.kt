package com.example.reclaimapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** Wipes habits, events and chats; profile and settings stay. Used by Settings → Erase all data and by sign-out. */
@Dao
abstract class MaintenanceDao {
    @Query("DELETE FROM habit_events")
    abstract suspend fun deleteHabitEvents()

    @Query("DELETE FROM habits")
    abstract suspend fun deleteHabits()

    @Query("DELETE FROM good_habit_logs")
    abstract suspend fun deleteGoodHabitLogs()

    @Query("DELETE FROM good_habits")
    abstract suspend fun deleteGoodHabits()

    @Query("DELETE FROM events")
    abstract suspend fun deleteEvents()

    @Query("DELETE FROM coach_messages")
    abstract suspend fun deleteCoachMessages()

    @Query("DELETE FROM coach_conversations")
    abstract suspend fun deleteCoachConversations()

    @Transaction
    open suspend fun clearAll() {
        deleteHabitEvents()
        deleteHabits()
        deleteGoodHabitLogs()
        deleteGoodHabits()
        deleteEvents()
        deleteCoachMessages()
        deleteCoachConversations()
    }

    @Query("UPDATE habit_events SET deleted_at = :at, updated_at = :at, dirty = 1 WHERE deleted_at IS NULL")
    abstract suspend fun markHabitEventsDeleted(at: Long)

    @Query("UPDATE habits SET deleted_at = :at, updated_at = :at, dirty = 1 WHERE deleted_at IS NULL")
    abstract suspend fun markHabitsDeleted(at: Long)

    @Query("UPDATE good_habit_logs SET count = 0, deleted_at = :at, updated_at = :at, dirty = 1 WHERE deleted_at IS NULL")
    abstract suspend fun markGoodHabitLogsDeleted(at: Long)

    @Query("UPDATE good_habits SET deleted_at = :at, updated_at = :at, dirty = 1 WHERE deleted_at IS NULL")
    abstract suspend fun markGoodHabitsDeleted(at: Long)

    @Query("UPDATE events SET deleted_at = :at, updated_at = :at, dirty = 1 WHERE deleted_at IS NULL")
    abstract suspend fun markEventsDeleted(at: Long)

    /** Erase for a signed-in user: rows stay marked as deleted, so the erase also reaches the account and other devices. */
    @Transaction
    open suspend fun markAllDeleted(at: Long) {
        markHabitEventsDeleted(at)
        markHabitsDeleted(at)
        markGoodHabitLogsDeleted(at)
        markGoodHabitsDeleted(at)
        markEventsDeleted(at)
        deleteCoachMessages()
        deleteCoachConversations()
    }

    @Query("UPDATE habit_events SET dirty = 1")
    abstract suspend fun markHabitEventsDirty()

    @Query("UPDATE habits SET dirty = 1")
    abstract suspend fun markHabitsDirty()

    @Query("UPDATE good_habit_logs SET dirty = 1")
    abstract suspend fun markGoodHabitLogsDirty()

    @Query("UPDATE good_habits SET dirty = 1")
    abstract suspend fun markGoodHabitsDirty()

    @Query("UPDATE events SET dirty = 1")
    abstract suspend fun markEventsDirty()

    /** The account behind this data is gone: everything counts as not uploaded again, for whichever account comes next. */
    @Transaction
    open suspend fun markAllDirty() {
        markHabitEventsDirty()
        markHabitsDirty()
        markGoodHabitLogsDirty()
        markGoodHabitsDirty()
        markEventsDirty()
    }
}
