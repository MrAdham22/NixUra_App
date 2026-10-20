package com.example.reclaimapp.presentation.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reclaimapp.domain.calculator.TrackingStateCalculator
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.model.HabitTracking
import com.example.reclaimapp.domain.usecase.CreateHabitAndStartTrackingUseCase
import com.example.reclaimapp.domain.usecase.ObserveActiveHabitTrackingUseCase
import com.example.reclaimapp.domain.usecase.RecordRelapseUseCase
import com.example.reclaimapp.presentation.appearance.StreakColorMapper
import com.example.reclaimapp.presentation.appearance.StreakColorProgressionConfig
import com.example.reclaimapp.presentation.util.formatElapsed
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HabitsViewModel(
    observeActiveHabitTracking: ObserveActiveHabitTrackingUseCase,
    private val createHabitAndStartTracking: CreateHabitAndStartTrackingUseCase,
    private val recordRelapse: RecordRelapseUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val mapper = StreakColorMapper()
    private val stageCount = StreakColorProgressionConfig.Default.stopCount

    private val trackingFromDb = observeActiveHabitTracking()
        .onEach { _uiState.update { it.copy(isLoading = false) } }

    private val tick = MutableStateFlow(0L)

    private val liveTracking = combine(trackingFromDb, tick) { trackingList, _ ->
        val now = clock.nowEpochMillis()
        trackingList.map { item ->
            item.copy(tracking = TrackingStateCalculator.compute(item.events, now))
        }
    }

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = combine(_uiState, liveTracking) { state, tracking ->
        state.copy(habits = tracking.map { it.toCardUi() })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitsUiState(),
    )

    init {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1_000)
                tick.update { it + 1 }
            }
        }
    }

    fun onAddHabitClick() {
        _uiState.update { it.copy(showCreateDialog = true, createNameInput = "", errorMessage = null) }
    }

    fun onCreateNameChange(name: String) {
        _uiState.update { it.copy(createNameInput = name) }
    }

    fun onDismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, createNameInput = "") }
    }

    fun onConfirmCreateHabit() {
        val name = _uiState.value.createNameInput
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = createHabitAndStartTracking(name, clock.nowEpochMillis())
            _uiState.update { state ->
                result.fold(
                    onSuccess = {
                        state.copy(
                            isSaving = false,
                            showCreateDialog = false,
                            createNameInput = "",
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Could not create habit",
                        )
                    },
                )
            }
        }
    }

    fun onRecordRelapse(habitId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            recordRelapse(habitId, clock.nowEpochMillis()).onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Could not record relapse")
                }
            }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun HabitTracking.toCardUi(): HabitCardUiModel {
        val elapsed = tracking.activeElapsedMillis.milliseconds
        return HabitCardUiModel(
            habitId = habit.id,
            name = habit.name,
            activeElapsed = elapsed,
            longestStreakLabel = formatElapsed(tracking.longestStreakMillis.milliseconds),
            activeStageIndex = mapper.stageIndexFor(elapsed),
            stageCount = stageCount,
        )
    }
}
