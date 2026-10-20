package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.repository.HabitTrackingRepository

class RecordRelapseUseCase(
    private val repository: HabitTrackingRepository,
) {
    suspend operator fun invoke(habitId: Long, atEpochMillis: Long): Result<Unit> = runCatching {
        require(habitId > 0) { "Invalid habit id" }
        require(atEpochMillis >= 0) { "Relapse time must be non-negative" }
        repository.recordRelapse(habitId, atEpochMillis)
    }
}
