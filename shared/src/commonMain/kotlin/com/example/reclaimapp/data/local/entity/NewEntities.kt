package com.example.reclaimapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "good_habits")
data class GoodHabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "daily_target") val dailyTarget: Int,
    @ColumnInfo(name = "color_argb") val colorArgb: Long? = null,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "archived_at") val archivedAtEpochMillis: Long? = null,
)

@Entity(
    tableName = "good_habit_logs",
    indices = [Index(value = ["good_habit_id", "day_index"], unique = true)],
)
data class GoodHabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "good_habit_id") val goodHabitId: Long,
    @ColumnInfo(name = "day_index") val dayIndex: Long,
    val count: Int,
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    @ColumnInfo(name = "target_at") val targetAtEpochMillis: Long,
    /** Comma-separated lead times in millis, e.g. "0,3600000,86400000". Empty = no reminders. */
    val reminders: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
)

@Entity(tableName = "coach_messages")
data class CoachMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
)

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey @ColumnInfo(name = "pref_key") val key: String,
    @ColumnInfo(name = "pref_value") val value: String,
)
