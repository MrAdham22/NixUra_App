package com.adhamelgendy.nixura.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** A new global id for a synced row. Local `Long` ids stay as they are; this one is the same on every device. */
@OptIn(ExperimentalUuidApi::class)
fun newSyncId(): String = Uuid.random().toString()

@Entity(tableName = "habits", indices = [Index(value = ["sync_id"], unique = true)])
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "archived_at") val archivedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "color_argb") val colorArgb: Long? = null,
    @ColumnInfo(name = "sync_id", defaultValue = "") val syncId: String = newSyncId(),
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAtEpochMillis: Long = createdAtEpochMillis,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "dirty", defaultValue = "1") val dirty: Boolean = true,
    // Money-saved tracker (v5). All null = no tracker for this habit.
    @ColumnInfo(name = "cost_item") val costItem: String? = null,
    @ColumnInfo(name = "cost_unit") val costUnit: String? = null,
    @ColumnInfo(name = "cost_units_per_day") val costUnitsPerDay: Double? = null,
    @ColumnInfo(name = "cost_price_minor") val costPriceMinor: Long? = null,
    @ColumnInfo(name = "cost_units_per_pack") val costUnitsPerPack: Int? = null,
    @ColumnInfo(name = "cost_currency") val costCurrency: String? = null,
    @ColumnInfo(name = "cost_per_day_in_packs") val costPerDayInPacks: Boolean? = null,
)
