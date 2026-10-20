package com.example.reclaimapp.data.mapper

import com.example.reclaimapp.data.local.entity.HabitEntity
import com.example.reclaimapp.data.local.entity.HabitEventEntity
import com.example.reclaimapp.domain.model.Habit
import com.example.reclaimapp.domain.model.HabitEvent
import com.example.reclaimapp.domain.model.HabitEventType

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    createdAtEpochMillis = createdAtEpochMillis,
    archivedAtEpochMillis = archivedAtEpochMillis,
)

fun HabitEventEntity.toDomain(): HabitEvent = HabitEvent(
    id = id,
    habitId = habitId,
    type = HabitEventType.valueOf(eventType),
    occurredAtEpochMillis = occurredAtEpochMillis,
)

fun HabitEventType.toStorageValue(): String = name
