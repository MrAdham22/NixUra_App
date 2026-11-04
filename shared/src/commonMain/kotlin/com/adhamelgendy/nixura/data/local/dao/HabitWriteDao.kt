package com.adhamelgendy.nixura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Transaction
import com.adhamelgendy.nixura.data.local.entity.HabitEntity
import com.adhamelgendy.nixura.data.local.entity.HabitEventEntity

@Dao
abstract class HabitWriteDao {
    @Insert
    protected abstract suspend fun insertHabit(habit: HabitEntity): Long

    @Insert
    protected abstract suspend fun insertEvent(event: HabitEventEntity): Long

    @Transaction
    open suspend fun createHabitWithStartEvent(
        habit: HabitEntity,
        startEvent: HabitEventEntity,
    ): Long {
        val habitId = insertHabit(habit)
        insertEvent(startEvent.copy(habitId = habitId))
        return habitId
    }

    @Transaction
    open suspend fun insertRelapseEvent(event: HabitEventEntity) {
        insertEvent(event)
    }
}
