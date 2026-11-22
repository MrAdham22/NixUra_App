package com.adhamelgendy.nixura.presentation.goodhabits.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.reminder.DeviceLocationGateway
import com.adhamelgendy.nixura.domain.reminder.HabitReminderRules
import com.adhamelgendy.nixura.domain.reminder.HabitReminderSettings
import com.adhamelgendy.nixura.domain.reminder.LocationAccess
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceRepository
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceState
import com.adhamelgendy.nixura.domain.reminder.ReminderStrategy
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.time.DayCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Where reading the location for prayer times stands, for the reminders sheet. */
enum class LocationReading { IDLE, READING, FAILED }

data class HabitRemindersUiState(
    val habits: Map<Long, GoodHabit> = emptyMap(),
    val device: ReminderDeviceState = ReminderDeviceState(),
    val location: LocationReading = LocationReading.IDLE,
)

/**
 * The reminder controls of good habits (the long-press sheet, phase C). Pure state + writes; the words and the look
 * are in [HabitRemindersSheet]. Choices are saved on the habit (synced); location and pauses stay on this phone.
 */
class HabitRemindersViewModel(
    private val goodHabits: GoodHabitRepository,
    private val device: ReminderDeviceRepository,
    private val location: DeviceLocationGateway,
    private val clock: Clock,
    private val days: DayCalculator,
) : ViewModel() {

    private val reading = MutableStateFlow(LocationReading.IDLE)

    val uiState: StateFlow<HabitRemindersUiState> = combine(
        goodHabits.observeGoodHabits().map { list -> list.associate { it.habit.id to it.habit } },
        device.observe(),
        reading.asStateFlow(),
    ) { habits, state, loc -> HabitRemindersUiState(habits, state, loc) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitRemindersUiState())

    fun locationAccess(): LocationAccess = location.access()

    fun setEnabled(habit: GoodHabit, enabled: Boolean) = save(habit) { it.copy(enabled = enabled, pausedUntilEpochMillis = null) }

    fun setTime(habit: GoodHabit, minutes: Int) = save(habit) { it.copy(timeMinutes = minutes) }

    fun setWindowStart(habit: GoodHabit, minutes: Int) =
        saveWindow(habit, HabitReminderRules.windowStartingAt(HabitReminderRules.waterWindowOf(habit), minutes))

    fun setWindowEnd(habit: GoodHabit, minutes: Int) =
        saveWindow(habit, HabitReminderRules.windowEndingAt(HabitReminderRules.waterWindowOf(habit), minutes))

    private fun saveWindow(habit: GoodHabit, times: Pair<Int, Int>) =
        save(habit) { it.copy(windowStartMinutes = times.first, windowEndMinutes = times.second) }

    /** "Pause for today": until tomorrow starts, or until a late water window is over. */
    fun pauseToday(habit: GoodHabit) = pauseDays(habit, 1)

    fun pauseWeek(habit: GoodHabit) = pauseDays(habit, 7)

    fun resume(habit: GoodHabit) = save(habit) { it.copy(pausedUntilEpochMillis = null) }

    private fun pauseDays(habit: GoodHabit, count: Int) {
        val until = HabitReminderRules.pauseEndOf(habit, days, clock.nowEpochMillis(), count)
        save(habit) { it.copy(pausedUntilEpochMillis = until) }
    }

    private fun save(habit: GoodHabit, change: (HabitReminderSettings) -> HabitReminderSettings) {
        // Store the effective choice, so "not chosen" becomes the user's explicit choice from now on.
        val current = habit.reminder.copy(enabled = HabitReminderRules.isEnabled(habit))
        viewModelScope.launch { goodHabits.setReminder(habit.id, change(current)) }
    }

    /**
     * "Use my location" / "Update location": the system dialog when needed, then one coarse reading, kept on this
     * phone only. Asked only from a prayer habit's reminders (PLAN.md decision 1).
     */
    fun useLocation() {
        when (location.access()) {
            LocationAccess.ALLOWED -> readLocation()
            LocationAccess.ASK -> location.request { granted -> if (granted) readLocation() else reading.value = LocationReading.IDLE }
            LocationAccess.OPEN_SETTINGS -> location.openSettings()
            LocationAccess.UNAVAILABLE -> Unit
        }
    }

    fun openLocationSettings() = location.openSettings()

    private fun readLocation() {
        reading.value = LocationReading.READING
        viewModelScope.launch {
            val point = runCatching { location.currentCoarse() }.getOrNull()
            if (point != null) device.setLocation(point)
            reading.value = if (point != null) LocationReading.IDLE else LocationReading.FAILED
        }
    }

    /** Settings → "Pause habit reminders" on this phone. */
    fun pauseAllFor(count: Int) {
        viewModelScope.launch { device.setPausedUntil(days.startOfDayMillis(days.dayIndexOf(clock.nowEpochMillis()) + count)) }
    }

    fun resumeAll() {
        viewModelScope.launch { device.setPausedUntil(null) }
    }

    companion object {
        /** True when prayer reminders cannot run yet because no location is saved. */
        fun needsLocation(habit: GoodHabit, device: ReminderDeviceState): Boolean =
            HabitReminderRules.strategyOf(habit) == ReminderStrategy.PRAYER && device.location == null
    }
}
