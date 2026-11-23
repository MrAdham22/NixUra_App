package com.adhamelgendy.nixura.presentation.goodhabits

import com.adhamelgendy.nixura.domain.analytics.UsageStats
import com.adhamelgendy.nixura.domain.analytics.UsageAction
import com.adhamelgendy.nixura.domain.analytics.NoUsageStats
import com.adhamelgendy.nixura.domain.recognition.HabitTopic
import com.adhamelgendy.nixura.domain.recognition.HabitTopicDetector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.recognition.GoodHabitFillStyle
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.usecase.CreateGoodHabitUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveGoodHabitsUseCase
import com.adhamelgendy.nixura.domain.usecase.SetGoodHabitCountUseCase
import com.adhamelgendy.nixura.domain.usecase.UpdateGoodHabitUseCase
import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.domain.usecase.MAX_DAILY_COUNT
import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import com.adhamelgendy.nixura.presentation.deletion.PendingDeletions
import com.adhamelgendy.nixura.presentation.deletion.hides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.theme.HabitThemeDetector
import com.adhamelgendy.nixura.domain.theme.WaterGoal

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
    val fillStyle: GoodHabitFillStyle = GoodHabitFillStyle.PLAIN,
    /** Kind of habit, for the glyph on the card (derived from the name). */
    val topic: HabitTopic = HabitTopic.OTHER_BUILD,
    /** `null` = daily habit. Otherwise days a week; then streaks and [daysCompleted] are in weeks. */
    val weeklyTarget: Int? = null,
    /** What the card fills with: today's count / daily target, or days done / goal this week. */
    val progressCount: Int = todayCount,
    val progressTarget: Int = dailyTarget,
    /** This week's goal is met (weekly habits only). */
    val weekGoalMet: Boolean = false,
    /** First day (Saturday) of the current week. */
    val weekStartIndex: Long = 0,
    /** Stored theme: colour, icon and animation through [com.adhamelgendy.nixura.presentation.appearance.HabitThemes]. */
    val theme: HabitTheme = HabitTheme.DEFAULT,
    /** Daily water habits only: the goal in ml. A day's count is cups of [WaterGoal.servingMl]. */
    val water: WaterGoal? = null,
) {
    val isWeekly: Boolean get() = weeklyTarget != null

    /** The goal of the period is reached: today's target, or this week's goal. Drives the completion pulse. */
    val goalReached: Boolean get() = if (isWeekly) weekGoalMet else doneToday

    /** Nothing more is needed today. */
    val settledToday: Boolean get() = doneToday || weekGoalMet

    val maxCountPerDay: Int get() = if (isWeekly) 1 else MAX_DAILY_COUNT
}

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
    private val setCount: SetGoodHabitCountUseCase,
    private val clock: Clock,
    private val dayCalculator: DayCalculator,
    pendingDeletions: PendingDeletions,
    private val topics: HabitTopicDetector? = null,
    private val themes: HabitThemeDetector? = null,
    /** Anonymous usage stats (fixed action names only). */
    private val usageStats: UsageStats = NoUsageStats,
) : ViewModel() {

    private val transient = MutableStateFlow(GoodHabitsUiState())

    private val topicCache = HashMap<String, HabitTopic>()
    private val tick = MutableStateFlow(0L)
    private val initialState = GoodHabitsUiState(todayIndex = dayCalculator.dayIndexOf(clock.nowEpochMillis()))

    val uiState: StateFlow<GoodHabitsUiState> =
        combine(observeGoodHabits(), tick, transient, pendingDeletions.pending) { items, _, extra, pending ->
            val today = dayCalculator.dayIndexOf(clock.nowEpochMillis())
            extra.copy(
                habits = items.filterNot { pending.hides(DeletableItem.GoodHabit(it.habit.id)) }.map { it.toUi(today) },
                todayIndex = today,
                isLoading = false,
            )
        }
            // Stats and the habit-name matcher (its first use builds the catalog index) stay off the main thread.
            .flowOn(Dispatchers.Default)
            .stateIn(
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
        if (delta > 0) usageStats.logAction(UsageAction.GoodHabitLogged)
        if (delta < 0 && habit.todayCount > 0) usageStats.logAction(UsageAction.GoodHabitUnlogged)
        onSetCount(habitId, state.todayIndex, habit.todayCount + delta)
    }

    fun onSetCount(habitId: Long, dayIndex: Long, count: Int) {
        val maxPerDay = uiState.value.habits.firstOrNull { it.id == habitId }?.maxCountPerDay ?: MAX_DAILY_COUNT
        viewModelScope.launch {
            setCount(habitId, dayIndex, count.coerceAtLeast(0), maxPerDay).onFailure { error ->
                transient.update { it.copy(errorMessage = error.message ?: "Could not save") }
            }
        }
    }

    /**
     * The theme a name would get if saved now, for the form's live preview (water section, background animation).
     * Off the main thread; saving detects again, so the stored theme always matches the saved name.
     */
    suspend fun previewTheme(name: String): HabitTheme =
        themes?.let { t -> withContext(Dispatchers.Default) { t.themeFor(name) } } ?: HabitTheme.DEFAULT

    /** [weeklyTarget] `null` = daily habit. [water]: the goal chosen on the form, for a daily water habit. */
    fun onCreate(
        name: String,
        dailyTarget: Int,
        colorArgb: Long?,
        weeklyTarget: Int? = null,
        water: WaterGoal? = null,
        remindersOn: Boolean? = null,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            transient.update { it.copy(isSaving = true, errorMessage = null) }
            val result = createGoodHabit(name, dailyTarget, colorArgb, weeklyTarget, water, remindersOn)
            if (result.isSuccess) usageStats.logAction(UsageAction.GoodHabitCreated)
            transient.update { state ->
                state.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.message)
            }
            if (result.isSuccess) onSuccess()
        }
    }

    /** Pass [water] on every save of a water habit: `null` resets it to N cups of 250 ml from [dailyTarget]. */
    fun onUpdate(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int? = null, water: WaterGoal? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            transient.update { it.copy(isSaving = true, errorMessage = null) }
            val result = updateGoodHabit(id, name, dailyTarget, colorArgb, weeklyTarget, water)
            transient.update { state ->
                state.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.message)
            }
            if (result.isSuccess) onSuccess()
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
            fillStyle = habit.theme.fillStyle,
            topic = topics?.let { t -> topicCache.getOrPut(habit.name) { t.buildTopic(habit.name) } } ?: HabitTopic.OTHER_BUILD,
            weeklyTarget = habit.weeklyTarget,
            progressCount = stats.progressCount,
            progressTarget = stats.progressTarget,
            weekGoalMet = stats.weekGoalMet,
            weekStartIndex = WeeklyGoalCalculator.weekStartOf(today),
            theme = habit.theme,
            water = habit.water,
        )
    }
}
