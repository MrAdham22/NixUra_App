package com.adhamelgendy.nixura.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_events",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("habit_id"), Index(value = ["sync_id"], unique = true)],
)
data class HabitEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "habit_id") val habitId: Long,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "occurred_at") val occurredAtEpochMillis: Long,
    @ColumnInfo(name = "sync_id", defaultValue = "") val syncId: String = newSyncId(),
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAtEpochMillis: Long = occurredAtEpochMillis,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "dirty", defaultValue = "1") val dirty: Boolean = true,
)
