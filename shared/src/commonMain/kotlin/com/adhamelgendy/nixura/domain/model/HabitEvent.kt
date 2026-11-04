package com.adhamelgendy.nixura.domain.model

data class HabitEvent(
    val id: Long,
    val habitId: Long,
    val type: HabitEventType,
    val occurredAtEpochMillis: Long,
)
