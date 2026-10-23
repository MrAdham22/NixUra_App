package com.example.reclaimapp.presentation.goodhabits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reclaimapp.domain.calculator.GoodHabitStatsCalculator
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.model.GoodHabitWithLogs
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.domain.usecase.ArchiveGoodHabitUseCase
import com.example.reclaimapp.domain.usecase.CreateGoodHabitUseCase
import com.example.reclaimapp.domain.usecase.ObserveGoodHabitsUseCase
import com.example.reclaimapp.domain.usecase.SetGoodHabitCountUseCase
import com.example.reclaimapp.domain.usecase.UpdateGoodHabitUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoodHabitUi(
    val id: Long,
    val name: String,
    val dailyTarget: Int,
    val colorArgb: Long?,
    val todayCount: Int,
    val doneToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalTimesDone: Int,
    val daysCompleted: Int,
    val completion7: Float,
    val completion30: Float,
    /** dayIndex → count. Used to draw calendars. */
    val countsByDay: Map<Long, Int>,
    val createdDayIndex: Long,
)

data class GoodHabitsUiState(
    val habits: List<GoodHabitUi> = emptyList(),
    val todayIndex: Long = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class GoodHabitsViewModel(
    observeGoodHabits: ObserveGoodHabitsUseCase,
    private val createGoodHabit: CreateGoodHabitUseCase,
    private val updateGoodHabit: UpdateGoodHabitUseCase,
    private val archiveGoodHabit: ArchiveGoodHabitUseCase,
    private val setCount: SetGoodHabitCountUseCase,
    private val clock: Clock,
    private val dayCalculator: DayCalculator,
) : ViewModel() {

    private val transient = MutableStateFlow(GoodHabitsUiState())
    private val tick = MutableStateFlow(0L)
    private val initialState = GoodHabitsUiState(todayIndex = dayCalculator.dayIndexOf(clock.nowEpochMillis()))

    val uiState: StateFlow<GoodHabitsUiState> =
        combine(observeGoodHabits(), tick, transient) { items, _, extra ->
            val today = dayCalculator.dayIndexOf(clock.nowEpochMillis())
            extra.copy(
                habits = items.map { it.toUi(today) },
                todayIndex = today,
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialState,
        )

    init {
        // Re-evaluate "today" so the day rolls over at midnight while the app is open.
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                tick.update { it + 1 }
            }
        }
    }

    fun onChangeToday(habitId: Long, delta: Int) {
        val state = uiState.value
        val habit = state.habits.firstOrNull { it.id == habitId } ?: return
        onSetCount(habitId, state.todayIndex, habit.todayCount + delta)
    }

    fun onSetCount(habitId: Long, dayIndex: Long, count: Int) {
        viewModelScope.launch {
            setCount(habitId, dayIndex, count.coerceAtLeast(0)).onFailure { error ->
                transient.update { it.copy(errorMessage = error.message ?: "Could not save") }
            }
        }
    }

    fun onCreate(name: String, dailyTarget: Int, colorArgb: Long?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            transient.update { it.copy(isSaving = true, errorMessage = null) }
            val result = createGoodHabit(name, dailyTarget, colorArgb)
            transient.update { state ->
                state.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.message)
            }
            if (result.isSuccess) onSuccess()
        }
    }

    fun onUpdate(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            transient.update { it.copy(isSaving = true, errorMessage = null) }
            val result = updateGoodHabit(id, name, dailyTarget, colorArgb)
            transient.update { state ->
                state.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.message)
            }
            if (result.isSuccess) onSuccess()
        }
    }

    fun onArchive(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            archiveGoodHabit(id).fold(
                onSuccess = { onSuccess() },
                onFailure = { error -> transient.update { it.copy(errorMessage = error.message ?: "Could not remove habit") } },
            )
        }
    }

    fun onDismissError() {
        transient.update { it.copy(errorMessage = null) }
    }

    private fun GoodHabitWithLogs.toUi(today: Long): GoodHabitUi {
        val createdDay = dayCalculator.dayIndexOf(habit.createdAtEpochMillis)
        val stats = GoodHabitStatsCalculator.compute(this, today, createdDay)
        return GoodHabitUi(
            id = habit.id,
            name = habit.name,
            dailyTarget = habit.dailyTarget,
            colorArgb = habit.colorArgb,
            todayCount = stats.todayCount,
            doneToday = stats.doneToday,
            currentStreak = stats.currentStreakDays,
            bestStreak = stats.bestStreakDays,
            totalTimesDone = stats.totalTimesDone,
            daysCompleted = stats.daysCompleted,
            completion7 = stats.completionLast7,
            completion30 = stats.completionLast30,
            countsByDay = GoodHabitStatsCalculator.countsByDay(this),
            createdDayIndex = createdDay,
        )
    }
}
