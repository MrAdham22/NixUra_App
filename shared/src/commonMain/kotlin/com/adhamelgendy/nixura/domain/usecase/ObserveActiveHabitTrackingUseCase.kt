package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import kotlinx.coroutines.flow.Flow

class ObserveActiveHabitTrackingUseCase(
    private val repository: HabitTrackingRepository,
) {
    operator fun invoke(): Flow<List<HabitTracking>> = repository.observeActiveHabitTracking()
}
