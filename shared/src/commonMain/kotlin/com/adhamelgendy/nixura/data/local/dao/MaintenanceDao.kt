package com.adhamelgendy.nixura.data.local.dao

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

    /** Calendar links belong to the account that made them; a sign-out must not hand them to the next one. */
    @Query("DELETE FROM preferences WHERE pref_key LIKE 'integration.%'")
    abstract suspend fun deleteIntegrationLinks()

    @Transaction
    open suspend fun clearAll() {
        deleteIntegrationLinks()
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

    // ---- deleting one item: rows stay marked as deleted, so the delete also reaches the account and other devices ----

    @Query("UPDATE habit_events SET deleted_at = :at, updated_at = :at, dirty = 1 WHERE habit_id = :habitId AND deleted_at IS NULL")
    abstract suspend fun markEventsOfHabitDeleted(habitId: Long, at: Long)

    @Query("UPDATE habits SET deleted_at = :at, updated_at = :at, dirty = 1 WHERE id = :habitId AND deleted_at IS NULL")
    abstract suspend fun markHabitDeleted(habitId: Long, at: Long)

    @Query("UPDATE good_habit_logs SET count = 0, deleted_at = :at, updated_at = :at, dirty = 1 WHERE good_habit_id = :habitId AND deleted_at IS NULL")
    abstract suspend fun markLogsOfGoodHabitDeleted(habitId: Long, at: Long)

    @Query("UPDATE good_habits SET deleted_at = :at, updated_at = :at, dirty = 1 WHERE id = :habitId AND deleted_at IS NULL")
    abstract suspend fun markGoodHabitDeleted(habitId: Long, at: Long)

    /** A quit habit with its whole history (start, relapses). */
    @Transaction
    open suspend fun deleteBadHabit(habitId: Long, at: Long) {
        markEventsOfHabitDeleted(habitId, at)
        markHabitDeleted(habitId, at)
    }

    /** A good habit with every day it was logged. */
    @Transaction
    open suspend fun deleteGoodHabit(habitId: Long, at: Long) {
        markLogsOfGoodHabitDeleted(habitId, at)
        markGoodHabitDeleted(habitId, at)
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
