package com.example.reclaimapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** Wipes every table. Used by Settings → Erase all data. */
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

    @Query("DELETE FROM preferences")
    abstract suspend fun deletePreferences()

    @Transaction
    open suspend fun clearAll() {
        deleteHabitEvents()
        deleteHabits()
        deleteGoodHabitLogs()
        deleteGoodHabits()
        deleteEvents()
        deleteCoachMessages()
        deletePreferences()
    }
}
