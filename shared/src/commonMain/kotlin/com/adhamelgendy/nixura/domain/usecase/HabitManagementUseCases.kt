package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.validation.HabitNameValidator
import com.adhamelgendy.nixura.domain.privacy.OutsideAppPrivacy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class UpdateHabitUseCase(private val repository: HabitTrackingRepository) {
    /** [cost] `null` removes the money tracker; pass the habit's current cost to keep it. */
    suspend operator fun invoke(habitId: Long, name: String, colorArgb: Long?, cost: HabitCost?, hideNameOutside: Boolean): Result<Unit> =
        HabitNameValidator.validate(name).mapCatching { valid ->
            require(habitId > 0) { "Invalid habit id" }
            repository.updateHabit(habitId, valid, colorArgb, hideNameOutside)
            repository.updateCost(habitId, cost)
        }
}

/** "Hide name outside the app" for one quit habit. Notifications and widgets follow the data. */
class SetHideNameOutsideUseCase(private val repository: HabitTrackingRepository) {
    suspend operator fun invoke(habitId: Long, hidden: Boolean): Result<Unit> = runCatching {
        require(habitId > 0) { "Invalid habit id" }
        repository.setHideNameOutside(habitId, hidden)
    }
}

/** True while any quit habit's name is hidden: the recent-apps preview is then switched off. */
class ObserveAnyNameHiddenUseCase(private val repository: HabitTrackingRepository) {
    operator fun invoke(): Flow<Boolean> =
        repository.observeActiveHabitTracking().map(OutsideAppPrivacy::anyNameHidden).distinctUntilChanged()
}
