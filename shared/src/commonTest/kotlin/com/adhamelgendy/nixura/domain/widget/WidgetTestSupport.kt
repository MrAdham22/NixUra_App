package com.adhamelgendy.nixura.domain.widget

import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitLog
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.Habit
import com.adhamelgendy.nixura.domain.model.HabitEvent
import com.adhamelgendy.nixura.domain.model.HabitEventType
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.model.TrackingSnapshot
import com.adhamelgendy.nixura.domain.repository.EventRepository
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.time.TimeZoneOffsetProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

const val DAY = 86_400_000L
const val HOUR = 3_600_000L

/** Runs a suspend block that never really suspends (all fakes are in-memory) without needing runBlocking. */
fun <T> runSuspend(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
    return checkNotNull(outcome) { "block suspended; use a gate-based test instead" }.getOrThrow()
}

class FixedClock(var now: Long) : Clock {
    override fun nowEpochMillis(): Long = now
}

fun utcDays(offsetMillis: Long = 0L) = DayCalculator(TimeZoneOffsetProvider { offsetMillis })

fun habitTracking(id: Long, name: String = "Smoking", startAt: Long = 0L, archived: Boolean = false, vararg relapses: Long): HabitTracking {
    val events = buildList {
        add(HabitEvent(id = id * 100, habitId = id, type = HabitEventType.TRACKING_START, occurredAtEpochMillis = startAt))
        relapses.forEachIndexed { i, at -> add(HabitEvent(id = id * 100 + i + 1, habitId = id, type = HabitEventType.RELAPSE, occurredAtEpochMillis = at)) }
    }
    return HabitTracking(
        habit = Habit(id, name, createdAtEpochMillis = startAt, archivedAtEpochMillis = if (archived) 1L else null),
        events = events,
        tracking = TrackingSnapshot(startAt, 0, 0),
    )
}

fun goodHabit(id: Long, target: Int, vararg logs: Pair<Long, Int>, created: Long = 0L, archived: Boolean = false, name: String = "Water") =
    GoodHabitWithLogs(
        GoodHabit(id, name, target, createdAtEpochMillis = created, archivedAtEpochMillis = if (archived) 5L else null),
        logs.map { GoodHabitLog(id, it.first, it.second) },
    )

fun event(id: Long, at: Long, title: String = "Trip") = Event(id, title, EventCategory.TRAVEL, at, emptyList(), 0L)

/** In-memory [GoodHabitRepository]. [gate], when set, suspends inside [setCount] to force interleaving. */
class FakeGoodHabitRepository(initial: List<GoodHabitWithLogs>) : GoodHabitRepository {
    val state = MutableStateFlow(initial)
    var gate: CompletableDeferred<Unit>? = null
    var writes = 0

    override fun observeGoodHabits(): Flow<List<GoodHabitWithLogs>> = state
    override suspend fun create(name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?): Long = error("unused")
    override suspend fun update(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?) = error("unused")
    override suspend fun archive(id: Long) = error("unused")

    override suspend fun setCount(id: Long, dayIndex: Long, count: Int) {
        gate?.await()
        writes++
        state.value = state.value.map { item ->
            if (item.habit.id != id) item else {
                val others = item.logs.filter { it.dayIndex != dayIndex }
                item.copy(logs = if (count <= 0) others else others + GoodHabitLog(id, dayIndex, count))
            }
        }
    }

    fun countOn(id: Long, day: Long): Int =
        state.value.first { it.habit.id == id }.logs.firstOrNull { it.dayIndex == day }?.count ?: 0
}

class FakeEventRepository(initial: List<Event>) : EventRepository {
    val state = MutableStateFlow(initial)
    override fun observeEvents(): Flow<List<Event>> = state
    override suspend fun create(title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>): Long = error("unused")
    override suspend fun update(id: Long, title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>) = error("unused")
    override suspend fun delete(id: Long) = error("unused")
}
