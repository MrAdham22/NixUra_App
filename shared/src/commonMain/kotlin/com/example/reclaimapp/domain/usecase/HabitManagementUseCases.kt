package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import com.example.reclaimapp.domain.validation.HabitNameValidator

class UpdateHabitUseCase(private val repository: HabitTrackingRepository) {
    suspend operator fun invoke(habitId: Long, name: String, colorArgb: Long?): Result<Unit> =
        HabitNameValidator.validate(name).mapCatching { valid ->
            require(habitId > 0) { "Invalid habit id" }
            repository.updateHabit(habitId, valid, colorArgb)
        }
}

class ArchiveHabitUseCase(
    private val repository: HabitTrackingRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(habitId: Long): Result<Unit> = runCatching {
        require(habitId > 0) { "Invalid habit id" }
        repository.archiveHabit(habitId, clock.nowEpochMillis())
    }
}
