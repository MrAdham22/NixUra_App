package com.adhamelgendy.nixura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adhamelgendy.nixura.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived_at IS NULL AND deleted_at IS NULL ORDER BY created_at ASC, id ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Query("SELECT * FROM habits WHERE id = :habitId AND deleted_at IS NULL LIMIT 1")
    suspend fun getById(habitId: Long): HabitEntity?

    @Query("UPDATE habits SET name = :name, color_argb = :colorArgb, updated_at = :updatedAt, dirty = 1 WHERE id = :habitId")
    suspend fun updateDetails(habitId: Long, name: String, colorArgb: Long?, updatedAt: Long)

    @Query(
        "UPDATE habits SET cost_item = :item, cost_unit = :unit, cost_units_per_day = :unitsPerDay, cost_price_minor = :priceMinor, " +
            "cost_units_per_pack = :unitsPerPack, cost_currency = :currency, cost_per_day_in_packs = :perDayInPacks, " +
            "updated_at = :updatedAt, dirty = 1 WHERE id = :habitId",
    )
    suspend fun updateCost(
        habitId: Long,
        item: String?,
        unit: String?,
        unitsPerDay: Double?,
        priceMinor: Long?,
        unitsPerPack: Int?,
        currency: String?,
        perDayInPacks: Boolean?,
        updatedAt: Long,
    )

    @Query("UPDATE habits SET archived_at = :archivedAt, updated_at = :archivedAt, dirty = 1 WHERE id = :habitId")
    suspend fun archive(habitId: Long, archivedAt: Long)
}
