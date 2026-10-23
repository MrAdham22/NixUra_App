package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import com.example.reclaimapp.domain.validation.HabitNameValidator

class CreateHabitAndStartTrackingUseCase(
    private val repository: HabitTrackingRepository,
) {
    suspend operator fun invoke(name: String, startAtEpochMillis: Long, colorArgb: Long? = null): Result<Long> {
        return HabitNameValidator.validate(name).mapCatching { validName ->
            require(startAtEpochMillis >= 0) { "Start time must be non-negative" }
            repository.createHabitAndStartTracking(validName, startAtEpochMillis, colorArgb)
        }
    }
}
