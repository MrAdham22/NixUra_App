package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.coach.BadHabitSnapshot
import com.adhamelgendy.nixura.domain.coach.ai.AiCoachGateway
import com.adhamelgendy.nixura.domain.coach.ai.AiContext
import com.adhamelgendy.nixura.domain.coach.ai.CoachVisual
import com.adhamelgendy.nixura.domain.coach.ai.AiMode
import com.adhamelgendy.nixura.domain.coach.ai.AiQuota
import com.adhamelgendy.nixura.domain.coach.ai.AiResult
import com.adhamelgendy.nixura.domain.coach.ai.AiTurn
import com.adhamelgendy.nixura.domain.coach.ai.AiTurnRole
import com.adhamelgendy.nixura.domain.coach.ai.CoachAction
import com.adhamelgendy.nixura.domain.coach.CoachResponder
import com.adhamelgendy.nixura.domain.coach.CoachSnapshot
import com.adhamelgendy.nixura.domain.coach.EventSnapshot
import com.adhamelgendy.nixura.domain.coach.GoodHabitSnapshot
import com.adhamelgendy.nixura.domain.model.CoachConversation
import com.adhamelgendy.nixura.domain.model.CoachMessage
import com.adhamelgendy.nixura.domain.model.CoachRole
import com.adhamelgendy.nixura.domain.model.HabitEventType
import com.adhamelgendy.nixura.domain.repository.CoachRepository
import com.adhamelgendy.nixura.domain.repository.EventRepository
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.repository.PreferencesRepository
import com.adhamelgendy.nixura.domain.time.DayCalculator
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
                GoodHabitSnapshot(
                    it.habit.name, stats.todayCount, stats.dailyTarget, stats.currentStreakDays,
                    stats.weeklyTarget, stats.weekDone, stats.weekGoal,
                )
            },
            nextEvent = events.observeEvents().first()
                .filter { it.targetAtEpochMillis > now }
                .minByOrNull { it.targetAtEpochMillis }
                ?.let { EventSnapshot(it.title, (it.targetAtEpochMillis - now).milliseconds) },
        )
    }
}

class ObserveCoachMessagesUseCase(private val repository: CoachRepository) {
    operator fun invoke(conversationId: Long): Flow<List<CoachMessage>> = repository.observeMessages(conversationId)
}

class ObserveCoachConversationsUseCase(private val repository: CoachRepository) {
    operator fun invoke(): Flow<List<CoachConversation>> = repository.observeConversations()
}

class EnsureCoachGreetingUseCase(
    private val repository: CoachRepository,
    private val responder: CoachResponder,
    private val buildSnapshot: BuildCoachSnapshotUseCase,
) {
    // The view model's init and the screen's LaunchedEffect can both call this at the same moment;
    // the lock prevents two chats or two greetings from being created.
    private val lock = Mutex()

    /** The chat to show: [preferred] if it still exists, else the latest, else a new one. Always has a greeting. */
    suspend operator fun invoke(preferred: Long? = null): Long = lock.withLock {
        val id = preferred?.takeIf { repository.exists(it) }
            ?: repository.latestConversationId()
            ?: repository.createConversation()
        greetIfEmpty(id)
        id
    }

    /** "New chat": an unused chat if there is one, so empty chats never pile up. */
    suspend fun newChat(): Long = lock.withLock {
        val id = repository.unusedConversationId() ?: repository.createConversation()
        greetIfEmpty(id)
        id
    }

    private suspend fun greetIfEmpty(id: Long) {
        if (!repository.hasMessages(id)) repository.append(id, CoachRole.COACH, responder.greeting(buildSnapshot()))
    }
}

/** Which progress cards fit a message, without any AI: a habit the user names, or the overview for "how am I doing?". */
object OfflineVisualPicker {
    private val progressWords = listOf("how am i doing", "how am i", "my progress", "progress", "my stats", "statistics", "how's my", "how is my", "doing with", "doing on", "streak")

    fun pick(text: String, context: AiContext, focusHabitId: Long?): List<String> {
        val t = text.lowercase()
        if (progressWords.none { it in t }) return emptyList()
        val named = context.quit.filter { it.name.lowercase() in t }.map { "quit:${it.habitId}" } +
            context.build.filter { it.name.lowercase() in t }.map { "good:${it.habitId}" }
        if (named.isNotEmpty()) return named.take(2)
        if (context.quit.isEmpty() && context.build.isEmpty()) return emptyList()
        val focus = context.quit.firstOrNull { it.habitId == focusHabitId }?.let { "quit:${it.habitId}" }
        return listOfNotNull(CoachVisual.OVERVIEW, focus)
    }
}

/** Why the offline coach answered although the AI is switched on. */
enum class CoachNotice { LimitReached, AiResting, Offline }

data class CoachSendOutcome(
    /** Things the AI understood the user wants to add; the screen asks before creating them. */
    val actions: List<CoachAction> = emptyList(),
    val quota: AiQuota? = null,
    val notice: CoachNotice? = null,
)

class SendCoachMessageUseCase(
    private val repository: CoachRepository,
    private val responder: CoachResponder,
    private val buildSnapshot: BuildCoachSnapshotUseCase,
    private val gateway: AiCoachGateway,
    private val aiMode: ObserveAiModeUseCase,
    private val buildAiContext: BuildAiContextUseCase,
) {
    /**
     * Stores the user's message, then the coach's reply: from the real AI when it is switched on for this user, and
     * from the offline coach otherwise or whenever the AI cannot answer (limit reached, no service free, no network).
     */
    suspend operator fun invoke(conversationId: Long, text: String, replyDelayMillis: Long = 0, focusHabitId: Long? = null): Result<CoachSendOutcome> = runCatching {
        val clean = text.trim()
        require(clean.isNotEmpty()) { "Message is empty" }
        require(clean.length <= MAX_COACH_MESSAGE) { "Message is too long" }
        val before = repository.observeMessages(conversationId).first()
        // The earlier things the user said, so the coach can handle follow-ups ("and after a week?").
        val earlier = before.filter { it.role == CoachRole.USER }.takeLast(RECENT_USER_MESSAGES).map { it.text }
        repository.append(conversationId, CoachRole.USER, clean)

        var notice: CoachNotice? = null
        if (aiMode().first() == AiMode.On) {
            val turns = before.takeLast(AI_HISTORY_MESSAGES)
                .dropWhile { it.role == CoachRole.COACH } // a chat sent to an AI starts with the user
                .map { AiTurn(if (it.role == CoachRole.USER) AiTurnRole.USER else AiTurnRole.COACH, it.text) } +
                AiTurn(AiTurnRole.USER, clean)
            when (val reply = gateway.chat(turns, buildAiContext(), focusHabitId)) {
                is AiResult.Ok -> {
                    repository.append(conversationId, CoachRole.COACH, reply.value.text, reply.value.visuals)
                    return@runCatching CoachSendOutcome(reply.value.actions, reply.value.quota)
                }
                AiResult.LimitReached -> notice = CoachNotice.LimitReached
                AiResult.Offline -> notice = CoachNotice.Offline
                AiResult.Unavailable, AiResult.SignedOut -> notice = CoachNotice.AiResting
            }
        } else if (replyDelayMillis > 0) {
            delay(replyDelayMillis)
        }
        val snapshot = buildSnapshot().copy(focusHabitId = focusHabitId, recentUserTexts = earlier)
        // The offline coach shows the progress cards too, when the user asks how things are going.
        val visuals = OfflineVisualPicker.pick(clean, buildAiContext(), focusHabitId)
        repository.append(conversationId, CoachRole.COACH, responder.respond(clean, snapshot), visuals)
        CoachSendOutcome(notice = notice)
    }

    private companion object {
        const val AI_HISTORY_MESSAGES = 8
    }
}

class DeleteCoachChatUseCase(private val repository: CoachRepository) {
    suspend operator fun invoke(conversationId: Long): Result<Unit> = runCatching { repository.delete(conversationId) }
}

data class CoachContext(
    val suggestions: List<String>,
    /** Active quit habits the user can pick from in the focus strip. */
    val habits: List<com.adhamelgendy.nixura.domain.coach.BadHabitSnapshot> = emptyList(),
    /** The habit actually in focus (the picked one if it still exists, otherwise the habit with the shortest streak). */
    val focusHabitId: Long? = null,
    /** Short "today's focus" card shown above the chat; `null` when there is nothing to focus on yet. */
    val insight: com.adhamelgendy.nixura.domain.coach.CoachNote?,
)

class GetCoachContextUseCase(
    private val buildSnapshot: BuildCoachSnapshotUseCase,
    private val engine: com.adhamelgendy.nixura.domain.coach.CoachEngine,
    private val clock: Clock,
) {
    suspend operator fun invoke(selectedHabitId: Long? = null): CoachContext {
        val snapshot = buildSnapshot()
        val seed = clock.nowEpochMillis() / 86_400_000L
        val picked = selectedHabitId?.let { id -> snapshot.badHabits.firstOrNull { it.id == id } }
        val focus = picked ?: snapshot.badHabits.minByOrNull { it.elapsed }
        val insight = when {
            focus != null -> com.adhamelgendy.nixura.domain.coach.CoachNote(
                title = "${engine.displayName(focus.name)} · ${com.adhamelgendy.nixura.domain.coach.durationText(focus.elapsed)}",
                body = engine.motivation(focus.name, focus.elapsed, seed),
            )
            else -> null
        }
        return CoachContext(engine.suggestions(snapshot), snapshot.badHabits, focus?.id, insight)
    }
}
