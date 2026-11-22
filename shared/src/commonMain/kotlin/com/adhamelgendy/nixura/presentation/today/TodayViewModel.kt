package com.adhamelgendy.nixura.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.notification.TodayPresenter
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceRepository
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.usecase.ObserveGoodHabitsUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveSettingsUseCase
import com.adhamelgendy.nixura.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** @param goodHabitCount `null` until first read, so loading never looks like "a second habit was added". */
data class TodayUiState(
    val enabled: Boolean = false,
    /** The app-wide switch. While it is off Today cannot show, so it is not offered. */
    val notificationsOn: Boolean = true,
    val showStreak: Boolean = false,
    val offerSeen: Boolean = true,
    val hiddenToday: Boolean = false,
    val goodHabitCount: Int? = null,
)

/** The controls of the opt-in "Today" notification: Settings, the habit sheet's row and the one-time offer. */
class TodayViewModel(
    observeSettings: ObserveSettingsUseCase,
    observeGoodHabits: ObserveGoodHabitsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val device: ReminderDeviceRepository,
    private val presenter: TodayPresenter,
    private val clock: Clock,
    private val days: DayCalculator,
) : ViewModel() {

    /** False where the platform has no ongoing notification (iOS): the controls are not shown. */
    val isSupported: Boolean get() = presenter.isSupported

    val uiState: StateFlow<TodayUiState> = combine(observeSettings(), observeGoodHabits(), device.observe()) { settings, habits, state ->
        TodayUiState(
            enabled = settings.todayNotification,
            notificationsOn = settings.notificationsEnabled,
            showStreak = settings.todayShowStreak,
            offerSeen = settings.todayOfferSeen,
            hiddenToday = state.todayHiddenDay == days.dayIndexOf(clock.nowEpochMillis()),
            goodHabitCount = habits.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Turning it on also brings it back when it was dismissed for today. */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) device.setTodayHiddenDay(null)
            updateSettings { it.copy(todayNotification = enabled, todayOfferSeen = true) }
        }
    }

    fun setShowStreak(show: Boolean) {
        viewModelScope.launch { updateSettings { it.copy(todayShowStreak = show) } }
    }

    fun declineOffer() {
        viewModelScope.launch { updateSettings { it.copy(todayOfferSeen = true) } }
    }
}
