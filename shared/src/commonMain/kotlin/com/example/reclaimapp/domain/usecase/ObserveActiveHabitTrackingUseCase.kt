package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.model.HabitTracking
import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import kotlinx.coroutines.flow.Flow

class ObserveActiveHabitTrackingUseCase(
    private val repository: HabitTrackingRepository,
) {
    operator fun invoke(): Flow<List<HabitTracking>> = repository.observeActiveHabitTracking()
}
