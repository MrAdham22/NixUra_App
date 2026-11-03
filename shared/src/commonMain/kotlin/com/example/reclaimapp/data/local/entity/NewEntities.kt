package com.example.reclaimapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "good_habits", indices = [Index(value = ["sync_id"], unique = true)])
data class GoodHabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "daily_target") val dailyTarget: Int,
    @ColumnInfo(name = "color_argb") val colorArgb: Long? = null,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "archived_at") val archivedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "sync_id", defaultValue = "") val syncId: String = newSyncId(),
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAtEpochMillis: Long = createdAtEpochMillis,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "dirty", defaultValue = "1") val dirty: Boolean = true,
)

/** [syncId] is `<good habit sync id>:<day>`, so two devices logging the same day update one row instead of adding two. */
@Entity(
    tableName = "good_habit_logs",
    indices = [Index(value = ["good_habit_id", "day_index"], unique = true), Index(value = ["sync_id"], unique = true)],
)
data class GoodHabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "good_habit_id") val goodHabitId: Long,
    @ColumnInfo(name = "day_index") val dayIndex: Long,
    val count: Int,
    @ColumnInfo(name = "sync_id", defaultValue = "") val syncId: String,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "dirty", defaultValue = "1") val dirty: Boolean = true,
)

/**
 * [source] is LOCAL for events made in the app, or GOOGLE / NOTION for imported ones; imported events are matched by
 * (`source`, [externalId]) so a re-import updates the same row instead of adding a copy.
 */
@Entity(
    tableName = "events",
    indices = [Index(value = ["sync_id"], unique = true), Index(value = ["source", "external_id"], unique = true)],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    @ColumnInfo(name = "target_at") val targetAtEpochMillis: Long,
    /** Comma-separated lead times in millis, e.g. "0,3600000,86400000". Empty = no reminders. */
    val reminders: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "sync_id", defaultValue = "") val syncId: String = newSyncId(),
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAtEpochMillis: Long = createdAtEpochMillis,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "dirty", defaultValue = "1") val dirty: Boolean = true,
    // Imported events (v5)
    @ColumnInfo(name = "source", defaultValue = "LOCAL") val source: String = "LOCAL",
    @ColumnInfo(name = "external_id") val externalId: String? = null,
    @ColumnInfo(name = "external_updated_at") val externalUpdatedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "all_day", defaultValue = "0") val allDay: Boolean = false,
)

/** One coach chat. [title] is the user's first message, empty until they send one. Stays on this phone; not synced. */
@Entity(tableName = "coach_conversations")
data class CoachConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
)

@Entity(tableName = "coach_messages", indices = [Index(value = ["conversation_id"])])
data class CoachMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "conversation_id", defaultValue = "0") val conversationId: Long,
    val role: String,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
)

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey @ColumnInfo(name = "pref_key") val key: String,
    @ColumnInfo(name = "pref_value") val value: String,
)
