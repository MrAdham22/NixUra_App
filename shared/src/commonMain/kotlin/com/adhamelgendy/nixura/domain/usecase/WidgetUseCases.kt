package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.recognition.HabitTopic
import com.adhamelgendy.nixura.domain.recognition.HabitTopicDetector
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.recognition.GoodHabitFillStyle
import com.adhamelgendy.nixura.domain.repository.EventRepository
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.repository.PreferencesRepository
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.widget.EventWidgetState
import com.adhamelgendy.nixura.domain.widget.GoodWidgetState
import com.adhamelgendy.nixura.domain.widget.QuitWidgetState
import com.adhamelgendy.nixura.domain.widget.WidgetAppearance
import com.adhamelgendy.nixura.domain.widget.WidgetStateMapper
import kotlinx.coroutines.flow.Flow
import com.adhamelgendy.nixura.domain.widget.WidgetKind
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
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
        // One habit, one day: an archived or deleted habit is "not found".
        val item = repository.dayCountOf(habitId, today) ?: throw IllegalStateException("Habit not found")
        val current = item.count
        val next = (current + delta).coerceIn(0, item.habit.maxCountPerDay)
        if (next != current) repository.setCount(habitId, today, next)
        next
    }
}

/** Adds 1 to today's (local day) count of a good habit, clamped to [maxCountPerDay] (1 for weekly). Returns the new count. */
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
    private val topics: HabitTopicDetector? = null,
) {
    suspend fun quit(habitId: Long?): QuitWidgetState =
        WidgetStateMapper.quit(habitId, habits.observeActiveHabitTracking().first(), clock.nowEpochMillis()) { name ->
            topics?.quitTopic(name) ?: HabitTopic.OTHER_QUIT
        }

    suspend fun good(habitId: Long?): GoodWidgetState {
        val all = goodHabits.observeGoodHabits().first()
        val styles = all.associate { it.habit.name to it.habit.theme.fillStyle }
        return WidgetStateMapper.good(
            habitId,
            all,
            clock.nowEpochMillis(),
            days,
            fillStyleOf = { name -> styles[name] ?: GoodHabitFillStyle.PLAIN },
            topicOf = { name -> topics?.buildTopic(name) ?: HabitTopic.OTHER_BUILD },
        )
    }

    /** [eventId] `null` = nearest event. */
    suspend fun event(eventId: Long?): EventWidgetState =
        WidgetStateMapper.event(eventId, events.observeEvents().first(), clock.nowEpochMillis())

    suspend fun appearance(): WidgetAppearance {
        val settings = preferences.observeSettings().first()
        return WidgetAppearance(darkTheme = settings.darkTheme, languageCode = settings.languageCode, digitStyle = settings.digitStyle)
    }
}

/**
 * Emits, after the first value, the widget kinds whose data really changed: quit habits/relapses → quit counters,
 * good habits/logs → quick-log widgets, events → event widgets, settings (theme, language, digits) → all of them.
 * Room re-emits a query on any write to its tables, so equal values are filtered out here; a "+" on one habit
 * therefore never redraws the quit or event widgets.
 */
class ObserveWidgetDataChangesUseCase(
    private val habits: HabitTrackingRepository,
    private val goodHabits: GoodHabitRepository,
    private val events: EventRepository,
    private val preferences: PreferencesRepository,
) {
    private data class Snapshot(val quit: Any?, val good: Any?, val events: Any?, val settings: Any?)

    operator fun invoke(): Flow<Set<WidgetKind>> = combine(
        habits.observeActiveHabitTracking(),
        goodHabits.observeGoodHabits(),
        events.observeEvents(),
        preferences.observeSettings(),
    ) { quit, good, events, settings -> Snapshot(quit, good, events, settings) }
        .scan(null as Pair<Snapshot?, Set<WidgetKind>>?) { previous, now -> now to changedKinds(previous?.first, now) }
        .drop(2) // the seed and the first real value: widgets are already drawn for the current state
        .map { it!!.second }
        .filter { it.isNotEmpty() }

    private fun changedKinds(before: Snapshot?, now: Snapshot): Set<WidgetKind> {
        if (before == null || before.settings != now.settings) return WidgetKind.entries.toSet()
        return buildSet {
            if (before.quit != now.quit) add(WidgetKind.QUIT_COUNTER)
            if (before.good != now.good) add(WidgetKind.GOOD_QUICK_LOG)
            if (before.events != now.events) add(WidgetKind.EVENT)
        }
    }
}
