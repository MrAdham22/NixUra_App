package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.validation.HabitNameValidator

class CreateHabitAndStartTrackingUseCase(
    private val repository: HabitTrackingRepository,
) {
    suspend operator fun invoke(name: String, startAtEpochMillis: Long, colorArgb: Long? = null, cost: HabitCost? = null): Result<Long> {
        return HabitNameValidator.validate(name).mapCatching { validName ->
            require(startAtEpochMillis >= 0) { "Start time must be non-negative" }
            repository.createHabitAndStartTracking(validName, startAtEpochMillis, colorArgb, cost)
        }
    }
}
