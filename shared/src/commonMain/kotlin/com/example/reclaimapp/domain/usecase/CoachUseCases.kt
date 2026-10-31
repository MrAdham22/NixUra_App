package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.calculator.GoodHabitStatsCalculator
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.coach.BadHabitSnapshot
import com.example.reclaimapp.domain.coach.CoachResponder
import com.example.reclaimapp.domain.coach.CoachSnapshot
import com.example.reclaimapp.domain.coach.EventSnapshot
import com.example.reclaimapp.domain.coach.GoodHabitSnapshot
import com.example.reclaimapp.domain.model.CoachMessage
import com.example.reclaimapp.domain.model.CoachRole
import com.example.reclaimapp.domain.model.HabitEventType
import com.example.reclaimapp.domain.repository.CoachRepository
import com.example.reclaimapp.domain.repository.EventRepository
import com.example.reclaimapp.domain.repository.GoodHabitRepository
import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import com.example.reclaimapp.domain.repository.PreferencesRepository
import com.example.reclaimapp.domain.time.DayCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

const val MAX_COACH_MESSAGE = 500
private const val RECENT_USER_MESSAGES = 6

class BuildCoachSnapshotUseCase(
    private val tracking: HabitTrackingRepository,
    private val goodHabits: GoodHabitRepository,
    private val events: EventRepository,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
    private val dayCalculator: DayCalculator,
) {
    suspend operator fun invoke(): CoachSnapshot {
        val now = clock.nowEpochMillis()
        val today = dayCalculator.dayIndexOf(now)
        return CoachSnapshot(
            userName = preferences.observeProfile().first().name,
            badHabits = tracking.observeActiveHabitTracking().first().map {
                BadHabitSnapshot(
                    id = it.habit.id,
                    name = it.habit.name,
                    elapsed = it.tracking.activeElapsedMillis.milliseconds,
                    longest = it.tracking.longestStreakMillis.milliseconds,
                    relapseCount = it.events.count { e -> e.type == HabitEventType.RELAPSE },
                )
            },
            goodHabits = goodHabits.observeGoodHabits().first().map {
                val stats = GoodHabitStatsCalculator.compute(it, today, dayCalculator.dayIndexOf(it.habit.createdAtEpochMillis))
                GoodHabitSnapshot(it.habit.name, stats.todayCount, stats.dailyTarget, stats.currentStreakDays)
            },
            nextEvent = events.observeEvents().first()
                .filter { it.targetAtEpochMillis > now }
                .minByOrNull { it.targetAtEpochMillis }
                ?.let { EventSnapshot(it.title, (it.targetAtEpochMillis - now).milliseconds) },
        )
    }
}

class ObserveCoachMessagesUseCase(private val repository: CoachRepository) {
    operator fun invoke(): Flow<List<CoachMessage>> = repository.observeMessages()
}

class EnsureCoachGreetingUseCase(
    private val repository: CoachRepository,
    private val responder: CoachResponder,
    private val buildSnapshot: BuildCoachSnapshotUseCase,
) {
    // The view model's init and the screen's LaunchedEffect can both call this at the same moment;
    // the lock prevents two greetings from being inserted.
    private val lock = Mutex()

    suspend operator fun invoke() {
        lock.withLock {
            if (repository.observeMessages().first().isEmpty()) {
                repository.append(CoachRole.COACH, responder.greeting(buildSnapshot()))
            }
        }
    }
}

class SendCoachMessageUseCase(
    private val repository: CoachRepository,
    private val responder: CoachResponder,
    private val buildSnapshot: BuildCoachSnapshotUseCase,
) {
    /** Stores the user's message, waits [replyDelayMillis] (typing feel), then stores the coach's reply. */
    suspend operator fun invoke(text: String, replyDelayMillis: Long = 0, focusHabitId: Long? = null): Result<Unit> = runCatching {
        val clean = text.trim()
        require(clean.isNotEmpty()) { "Message is empty" }
        require(clean.length <= MAX_COACH_MESSAGE) { "Message is too long" }
        // The earlier things the user said, so the coach can handle follow-ups ("and after a week?").
        val earlier = repository.observeMessages().first()
            .filter { it.role == CoachRole.USER }
            .takeLast(RECENT_USER_MESSAGES)
            .map { it.text }
        repository.append(CoachRole.USER, clean)
        if (replyDelayMillis > 0) delay(replyDelayMillis)
        val snapshot = buildSnapshot().copy(focusHabitId = focusHabitId, recentUserTexts = earlier)
        repository.append(CoachRole.COACH, responder.respond(clean, snapshot))
    }
}

class ClearCoachChatUseCase(private val repository: CoachRepository) {
    suspend operator fun invoke(): Result<Unit> = runCatching { repository.clear() }
}

data class CoachContext(
    val suggestions: List<String>,
    /** Active quit habits the user can pick from in the focus strip. */
    val habits: List<com.example.reclaimapp.domain.coach.BadHabitSnapshot> = emptyList(),
    /** The habit actually in focus (the picked one if it still exists, otherwise the habit with the shortest streak). */
    val focusHabitId: Long? = null,
    /** Short "today's focus" card shown above the chat; `null` when there is nothing to focus on yet. */
    val insight: com.example.reclaimapp.domain.coach.CoachNote?,
)

class GetCoachContextUseCase(
    private val buildSnapshot: BuildCoachSnapshotUseCase,
    private val engine: com.example.reclaimapp.domain.coach.CoachEngine,
    private val clock: Clock,
) {
    suspend operator fun invoke(selectedHabitId: Long? = null): CoachContext {
        val snapshot = buildSnapshot()
        val seed = clock.nowEpochMillis() / 86_400_000L
        val picked = selectedHabitId?.let { id -> snapshot.badHabits.firstOrNull { it.id == id } }
        val focus = picked ?: snapshot.badHabits.minByOrNull { it.elapsed }
        val insight = when {
            focus != null -> com.example.reclaimapp.domain.coach.CoachNote(
                title = "${engine.displayName(focus.name)} · ${com.example.reclaimapp.domain.coach.durationText(focus.elapsed)}",
                body = engine.motivation(focus.name, focus.elapsed, seed),
            )
            else -> null
        }
        return CoachContext(engine.suggestions(snapshot), snapshot.badHabits, focus?.id, insight)
    }
}
