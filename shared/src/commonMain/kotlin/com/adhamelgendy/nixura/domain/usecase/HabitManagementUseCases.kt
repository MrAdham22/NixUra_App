package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.validation.HabitNameValidator

class UpdateHabitUseCase(private val repository: HabitTrackingRepository) {
    /** [cost] `null` removes the money tracker; pass the habit's current cost to keep it. */
    suspend operator fun invoke(habitId: Long, name: String, colorArgb: Long?, cost: HabitCost?): Result<Unit> =
        HabitNameValidator.validate(name).mapCatching { valid ->
            require(habitId > 0) { "Invalid habit id" }
            repository.updateHabit(habitId, valid, colorArgb)
            repository.updateCost(habitId, cost)
        }
}
