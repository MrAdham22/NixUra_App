package com.adhamelgendy.nixura.presentation.habits

import com.adhamelgendy.nixura.domain.analytics.UsageStats
import com.adhamelgendy.nixura.domain.analytics.UsageAction
import com.adhamelgendy.nixura.domain.analytics.NoUsageStats
import com.adhamelgendy.nixura.domain.recognition.HabitTopic
import com.adhamelgendy.nixura.domain.recognition.HabitTopicDetector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamelgendy.nixura.domain.calculator.TrackingStateCalculator
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.HabitEventType
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.money.MoneySavedCalculator
import com.adhamelgendy.nixura.domain.usecase.CreateHabitAndStartTrackingUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveActiveHabitTrackingUseCase
import com.adhamelgendy.nixura.domain.usecase.RecordRelapseUseCase
import com.adhamelgendy.nixura.domain.usecase.UpdateHabitUseCase
import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.presentation.deletion.PendingDeletions
import com.adhamelgendy.nixura.presentation.deletion.hides
import com.adhamelgendy.nixura.presentation.appearance.StreakColorMapper
import com.adhamelgendy.nixura.presentation.appearance.StreakColorProgressionConfig
import com.adhamelgendy.nixura.presentation.util.formatElapsed
import com.adhamelgendy.nixura.domain.coach.durationText
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.adhamelgendy.nixura.domain.usecase.SetHideNameOutsideUseCase

class HabitsViewModel(
    observeActiveHabitTracking: ObserveActiveHabitTrackingUseCase,
    private val createHabitAndStartTracking: CreateHabitAndStartTrackingUseCase,
    private val recordRelapse: RecordRelapseUseCase,
    private val clock: Clock,
    private val updateHabit: UpdateHabitUseCase,
    private val setHideNameOutside: SetHideNameOutsideUseCase,
    pendingDeletions: PendingDeletions,
    private val topics: HabitTopicDetector? = null,
    /** Anonymous usage stats (fixed action names only). */
    private val usageStats: UsageStats = NoUsageStats,
) : ViewModel() {

    private val topicCache = HashMap<String, HabitTopic>()

    private val mapper = StreakColorMapper()
    private val stageCount = StreakColorProgressionConfig.Default.stopCount

    private val trackingFromDb = observeActiveHabitTracking()
        .onEach { _uiState.update { it.copy(isLoading = false) } }

    // The live counter's 1 s tick. A cold flow: it only runs while the screen collects [uiState] (WhileSubscribed below
    // stops it 5 s after the app goes to the background), instead of ticking in the view model forever.
    private val tick = kotlinx.coroutines.flow.flow {
        var n = 0L
        while (true) {
            emit(n++)
            kotlinx.coroutines.delay(1_000)
        }
    }

    private val liveTracking = combine(trackingFromDb, tick) { trackingList, _ ->
        val now = clock.nowEpochMillis()
        trackingList.map { item ->
            item.copy(tracking = TrackingStateCalculator.compute(item.events, now))
        }
    }

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = combine(_uiState, liveTracking, pendingDeletions.pending) { state, tracking, pending ->
        // A habit waiting out its undo window is already gone from the lists.
        state.copy(habits = tracking.filterNot { pending.hides(DeletableItem.BadHabit(it.habit.id)) }.map { it.toCardUi() })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitsUiState(),
    )

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

    /** Creates a habit from the full-screen form. [onSuccess] runs after it is saved (e.g. navigate back). */
    fun onCreateHabit(
        name: String,
        colorArgb: Long?,
        startAtEpochMillis: Long,
        cost: HabitCost?,
        hideNameOutside: Boolean,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = createHabitAndStartTracking(name, startAtEpochMillis, colorArgb, cost, hideNameOutside)
            if (result.isSuccess) usageStats.logAction(UsageAction.QuitHabitCreated)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isSaving = false) },
                    onFailure = { error -> state.copy(isSaving = false, errorMessage = error.message ?: "Could not create habit") },
                )
            }
            if (result.isSuccess) onSuccess()
        }
    }

    /** [cost] is the tracker as the form now shows it (`null` = no tracker). */
    fun onUpdateHabit(habitId: Long, name: String, colorArgb: Long?, cost: HabitCost?, hideNameOutside: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = updateHabit(habitId, name, colorArgb, cost, hideNameOutside)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isSaving = false) },
                    onFailure = { error -> state.copy(isSaving = false, errorMessage = error.message ?: "Could not save changes") },
                )
            }
            if (result.isSuccess) onSuccess()
        }
    }

    fun onHideNameOutside(habitId: Long, hidden: Boolean) {
        viewModelScope.launch {
            setHideNameOutside(habitId, hidden).onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Could not save changes") }
            }
        }
    }

    fun onRecordRelapse(habitId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            recordRelapse(habitId, clock.nowEpochMillis()).onSuccess { usageStats.logAction(UsageAction.RelapseLogged) }.onFailure { error ->
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
            longestStreakLabel = durationText(tracking.longestStreakMillis.milliseconds),
            activeStageIndex = mapper.stageIndexFor(elapsed),
            stageCount = stageCount,
            colorArgb = habit.colorArgb,
            longestStreak = tracking.longestStreakMillis.milliseconds,
            relapseCount = events.count { it.type == HabitEventType.RELAPSE },
            activePeriodStartEpochMillis = tracking.activePeriodStartEpochMillis,
            trackingStartEpochMillis = events.firstOrNull { it.type == HabitEventType.TRACKING_START }?.occurredAtEpochMillis,
            relapseTimestamps = events.filter { it.type == HabitEventType.RELAPSE }
                .map { it.occurredAtEpochMillis }
                .sortedDescending(),
            cost = habit.cost,
            hideNameOutside = habit.hideNameOutside,
            // Follows the counter: the current clean streak, reset by a relapse.
            moneySaved = habit.cost?.let { MoneySavedCalculator.saved(it, tracking.activeElapsedMillis) },
            topic = topics?.let { t -> topicCache.getOrPut(habit.name) { t.quitTopic(habit.name) } } ?: HabitTopic.OTHER_QUIT,
        )
    }
}
