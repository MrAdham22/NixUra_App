package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.coach.ai.AiCoachGateway
import com.adhamelgendy.nixura.domain.coach.ai.AiContext
import com.adhamelgendy.nixura.domain.coach.ai.AiMode
import com.adhamelgendy.nixura.domain.coach.ai.AiQuota
import com.adhamelgendy.nixura.domain.coach.ai.AiResult
import com.adhamelgendy.nixura.domain.coach.ai.AiVoiceReply
import com.adhamelgendy.nixura.domain.coach.ai.CoachAction
import com.adhamelgendy.nixura.domain.coach.ai.CoachVisual
import com.adhamelgendy.nixura.domain.coach.ai.VoiceClip
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.model.HabitEventType
import com.adhamelgendy.nixura.domain.repository.EventRepository
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.repository.PreferencesRepository
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.domain.time.DayCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

private const val MAX_CONTEXT_ITEMS = 30
private const val EVENT_WINDOW_DAYS = 30
private const val HOUR_MILLIS = 3_600_000L
private val WEEKDAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

private fun two(n: Int) = n.toString().padStart(2, '0')

/** What the AI is told about the user: habits with their numbers, and events of the next 30 days. No personal details. */
class BuildAiContextUseCase(
    private val tracking: HabitTrackingRepository,
    private val goodHabits: GoodHabitRepository,
    private val events: EventRepository,
    private val clock: Clock,
    private val days: DayCalculator,
) {
    suspend operator fun invoke(): AiContext {
        val now = clock.nowEpochMillis()
        val today = days.dayIndexOf(now)
        val minuteOfDay = (days.millisOfDay(now) / 60_000L).toInt()
        return AiContext(
            today = isoDate(today),
            weekday = WEEKDAYS[CivilCalendar.isoWeekday(today) - 1],
            time = "${two(minuteOfDay / 60)}:${two(minuteOfDay % 60)}",
            quit = tracking.observeActiveHabitTracking().first().take(MAX_CONTEXT_ITEMS).map {
                AiContext.Quit(
                    habitId = it.habit.id,
                    name = it.habit.name,
                    streakHours = it.tracking.activeElapsedMillis / HOUR_MILLIS,
                    bestHours = it.tracking.longestStreakMillis / HOUR_MILLIS,
                    relapses = it.events.count { e -> e.type == HabitEventType.RELAPSE },
                )
            },
            build = goodHabits.observeGoodHabits().first().take(MAX_CONTEXT_ITEMS).map {
                val stats = GoodHabitStatsCalculator.compute(it, today, days.dayIndexOf(it.habit.createdAtEpochMillis))
                AiContext.Build(
                    habitId = it.habit.id,
                    name = it.habit.name,
                    today = stats.todayCount,
                    target = stats.dailyTarget,
                    streak = stats.currentStreakDays,
                    weekDone = stats.weekDone.takeIf { _ -> stats.isWeekly },
                    weekGoal = stats.weekGoal.takeIf { _ -> stats.isWeekly },
                )
            },
            events = events.observeEvents().first()
                .filter { it.targetAtEpochMillis > now && it.targetAtEpochMillis < now + EVENT_WINDOW_DAYS * DayCalculator.MILLIS_PER_DAY }
                .sortedBy { it.targetAtEpochMillis }
                .take(MAX_CONTEXT_ITEMS)
                .map {
                    val minute = (days.millisOfDay(it.targetAtEpochMillis) / 60_000L).toInt()
                    AiContext.Event(it.title, isoDate(days.dayIndexOf(it.targetAtEpochMillis)), "${two(minute / 60)}:${two(minute % 60)}")
                },
        )
    }

    private fun isoDate(dayIndex: Long): String {
        val c = CivilCalendar.civilFromDayIndex(dayIndex)
        return "${c.year.toString().padStart(4, '0')}-${two(c.month)}-${two(c.day)}"
    }
}

/** AI or offline coach for this user, live: it changes when they sign in or out, or answer the consent question. */
class ObserveAiModeUseCase(
    private val sessions: SessionManager,
    private val preferences: PreferencesRepository,
) {
    operator fun invoke(): Flow<AiMode> = combine(sessions.state, preferences.observeSettings()) { session, settings ->
        when {
            session !is SessionState.SignedIn -> AiMode.Guest
            settings.aiCoachConsent == null -> AiMode.NeedsConsent
            settings.aiCoachConsent -> AiMode.On
            else -> AiMode.Declined
        }
    }.distinctUntilChanged()
}

class SetAiConsentUseCase(private val preferences: PreferencesRepository) {
    suspend operator fun invoke(agreed: Boolean) {
        preferences.saveSettings(preferences.observeSettings().first().copy(aiCoachConsent = agreed))
    }
}

class GetAiQuotaUseCase(private val gateway: AiCoachGateway) {
    suspend operator fun invoke(): AiQuota? = (gateway.quota() as? AiResult.Ok)?.value
}

/** Sends a recorded voice note to the AI and returns what it understood. Nothing is added here. */
class UnderstandVoiceNoteUseCase(
    private val gateway: AiCoachGateway,
    private val buildContext: BuildAiContextUseCase,
) {
    suspend operator fun invoke(clip: VoiceClip): AiResult<AiVoiceReply> = gateway.voice(clip, buildContext())
}

/** Creates the habits and events the user confirmed. Returns how many were added and how many failed. */
class ApplyCoachActionsUseCase(
    private val createQuit: CreateHabitAndStartTrackingUseCase,
    private val createGood: CreateGoodHabitUseCase,
    private val saveEvent: SaveEventUseCase,
    private val clock: Clock,
    private val days: DayCalculator,
) {
    data class Outcome(val added: Int, val failed: Int)

    suspend operator fun invoke(actions: List<CoachAction>): Outcome {
        var added = 0
        for (action in actions) {
            val result = when (action) {
                // The counter starts now, like the "I quit just now" choice in the form.
                is CoachAction.Quit -> createQuit(action.name, clock.nowEpochMillis())
                is CoachAction.Build -> createGood(action.name, action.perDay, null, action.daysPerWeek)
                is CoachAction.Event -> saveEvent(
                    id = null,
                    title = action.title,
                    category = EventCategory.EVENT,
                    targetAtEpochMillis = days.epochMillisOf(action.year, action.month, action.day, action.hour ?: DEFAULT_EVENT_HOUR, action.minute ?: 0),
                    reminderLeadMillis = emptyList(),
                )
            }
            if (result.isSuccess) added++
        }
        return Outcome(added, actions.size - added)
    }

    private companion object {
        const val DEFAULT_EVENT_HOUR = 9
    }
}

/** The numbers behind the progress cards the coach shows in the chat, read fresh from the user's data. */
class BuildCoachVisualsUseCase(
    private val tracking: HabitTrackingRepository,
    private val goodHabits: GoodHabitRepository,
    private val clock: Clock,
    private val days: DayCalculator,
) {
    suspend operator fun invoke(keys: Set<String>): Map<String, CoachVisual> {
        if (keys.isEmpty()) return emptyMap()
        val today = days.dayIndexOf(clock.nowEpochMillis())
        val quit = tracking.observeActiveHabitTracking().first()
        val good = goodHabits.observeGoodHabits().first()
        val out = mutableMapOf<String, CoachVisual>()
        for (key in keys) {
            val id = key.substringAfter(':', "").toLongOrNull()
            when {
                key == CoachVisual.OVERVIEW -> {
                    val stats = good.map { GoodHabitStatsCalculator.compute(it, today, days.dayIndexOf(it.habit.createdAtEpochMillis)) }
                    if (quit.isNotEmpty() || good.isNotEmpty()) {
                        out[key] = CoachVisual.Overview(
                            quitCount = quit.size,
                            bestCleanMillis = quit.maxOfOrNull { it.tracking.activeElapsedMillis } ?: 0L,
                            goodDone = stats.count { it.settledToday },
                            goodTotal = stats.size,
                            weekPercent = if (stats.isEmpty()) null else (stats.map { it.completionLast7 }.average() * 100).toInt().coerceIn(0, 100),
                        )
                    }
                }
                key.startsWith("quit:") -> quit.firstOrNull { it.habit.id == id }?.let {
                    val elapsed = it.tracking.activeElapsedMillis
                    val goalDays = CoachVisual.MILESTONE_DAYS.firstOrNull { d -> elapsed < d * DayCalculator.MILLIS_PER_DAY } ?: CoachVisual.MILESTONE_DAYS.last()
                    out[key] = CoachVisual.Quit(
                        name = it.habit.name,
                        elapsedMillis = elapsed,
                        longestMillis = maxOf(it.tracking.longestStreakMillis, elapsed),
                        relapses = it.events.count { e -> e.type == HabitEventType.RELAPSE },
                        goalDays = goalDays,
                        progress = (elapsed.toFloat() / (goalDays * DayCalculator.MILLIS_PER_DAY)).coerceIn(0f, 1f),
                    )
                }
                key.startsWith("good:") -> good.firstOrNull { it.habit.id == id }?.let { item ->
                    val stats = GoodHabitStatsCalculator.compute(item, today, days.dayIndexOf(item.habit.createdAtEpochMillis))
                    val byDay = item.logs.associate { it.dayIndex to it.count }
                    val target = if (stats.isWeekly) 1 else stats.dailyTarget.coerceAtLeast(1)
                    val week = (6 downTo 0).map { back -> today - back }
                    out[key] = CoachVisual.Good(
                        name = item.habit.name,
                        days = week.map { ((byDay[it] ?: 0).toFloat() / target).coerceIn(0f, 1f) },
                        weekdays = week.map { CivilCalendar.isoWeekday(it) },
                        streak = stats.currentStreakDays,
                        weekly = stats.isWeekly,
                        count = stats.progressCount,
                        target = stats.progressTarget,
                    )
                }
            }
        }
        return out
    }
}
