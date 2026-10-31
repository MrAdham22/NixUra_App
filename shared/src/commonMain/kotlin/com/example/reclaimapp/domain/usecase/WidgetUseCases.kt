package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.repository.EventRepository
import com.example.reclaimapp.domain.repository.GoodHabitRepository
import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import com.example.reclaimapp.domain.repository.PreferencesRepository
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.domain.widget.EventWidgetState
import com.example.reclaimapp.domain.widget.GoodWidgetState
import com.example.reclaimapp.domain.widget.QuitWidgetState
import com.example.reclaimapp.domain.widget.WidgetAppearance
import com.example.reclaimapp.domain.widget.WidgetStateMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One lock for all quick-log writes in this process. A widget "+" is a read-modify-write on today's count,
 * so rapid double taps must not both read the same old value (lost update).
 */
private val goodHabitWidgetWriteLock = Mutex()

private suspend fun adjustTodayCount(
    repository: GoodHabitRepository,
    clock: Clock,
    days: DayCalculator,
    habitId: Long,
    delta: Int,
): Result<Int> = runCatching {
    require(habitId > 0) { "Invalid habit id" }
    goodHabitWidgetWriteLock.withLock {
        val now = clock.nowEpochMillis()
        val today = days.dayIndexOf(now)
        // observeGoodHabits() only contains active (non-archived) habits.
        val item = repository.observeGoodHabits().first().firstOrNull { it.habit.id == habitId }
            ?: throw IllegalStateException("Habit not found")
        val current = item.logs.firstOrNull { it.dayIndex == today }?.count ?: 0
        val next = (current + delta).coerceIn(0, MAX_DAILY_COUNT)
        if (next != current) repository.setCount(habitId, today, next)
        next
    }
}

/** Adds 1 to today's (local day) count of a good habit, clamped to [MAX_DAILY_COUNT]. Returns the new count. */
class IncrementGoodHabitTodayUseCase(
    private val repository: GoodHabitRepository,
    private val clock: Clock,
    private val days: DayCalculator,
) {
    suspend operator fun invoke(habitId: Long): Result<Int> = adjustTodayCount(repository, clock, days, habitId, +1)
}

/** Removes 1 from today's count (floor 0; reaching 0 clears the day). Returns the new count. */
class DecrementGoodHabitTodayUseCase(
    private val repository: GoodHabitRepository,
    private val clock: Clock,
    private val days: DayCalculator,
) {
    suspend operator fun invoke(habitId: Long): Result<Int> = adjustTodayCount(repository, clock, days, habitId, -1)
}

/** One-shot reads that turn repository data into widget states (the widget process has no ViewModels). */
class LoadWidgetStateUseCase(
    private val habits: HabitTrackingRepository,
    private val goodHabits: GoodHabitRepository,
    private val events: EventRepository,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
    private val days: DayCalculator,
) {
    suspend fun quit(habitId: Long?): QuitWidgetState =
        WidgetStateMapper.quit(habitId, habits.observeActiveHabitTracking().first(), clock.nowEpochMillis())

    suspend fun good(habitId: Long?): GoodWidgetState =
        WidgetStateMapper.good(habitId, goodHabits.observeGoodHabits().first(), clock.nowEpochMillis(), days)

    /** [eventId] `null` = nearest event. */
    suspend fun event(eventId: Long?): EventWidgetState =
        WidgetStateMapper.event(eventId, events.observeEvents().first(), clock.nowEpochMillis())

    suspend fun appearance(): WidgetAppearance {
        val settings = preferences.observeSettings().first()
        return WidgetAppearance(darkTheme = settings.darkTheme, languageCode = settings.languageCode)
    }
}

/** Emits whenever anything a widget shows may have changed (habits, relapses, logs, events, theme/language). */
class ObserveWidgetDataChangesUseCase(
    private val habits: HabitTrackingRepository,
    private val goodHabits: GoodHabitRepository,
    private val events: EventRepository,
    private val preferences: PreferencesRepository,
) {
    operator fun invoke(): Flow<Unit> = combine(
        habits.observeActiveHabitTracking(),
        goodHabits.observeGoodHabits(),
        events.observeEvents(),
        preferences.observeSettings(),
    ) { _, _, _, _ -> }.map { }
}
