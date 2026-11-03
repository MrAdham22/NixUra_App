package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.money.HabitCost
import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import com.example.reclaimapp.domain.validation.HabitNameValidator

class UpdateHabitUseCase(private val repository: HabitTrackingRepository) {
    /** [cost] `null` removes the money tracker; pass the habit's current cost to keep it. */
    suspend operator fun invoke(habitId: Long, name: String, colorArgb: Long?, cost: HabitCost?): Result<Unit> =
        HabitNameValidator.validate(name).mapCatching { valid ->
            require(habitId > 0) { "Invalid habit id" }
            repository.updateHabit(habitId, valid, colorArgb)
            repository.updateCost(habitId, cost)
        }
}
