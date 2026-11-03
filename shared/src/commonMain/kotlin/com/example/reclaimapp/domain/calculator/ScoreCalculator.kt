package com.example.reclaimapp.domain.calculator

import com.example.reclaimapp.domain.model.GoodHabitWithLogs
import com.example.reclaimapp.domain.model.HabitTracking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** One habit's share of a score, so the app can show how a percentage was reached. */
data class HabitScoreLine(val name: String, val percent: Int)

/** Derived 0..100 scores shown on Home and Profile. Nothing here is persisted. */
object ScoreCalculator {

    /** Horizon for "fully broken": matches the last streak color threshold (30 days). */
    val QUIT_HORIZON: Duration = 30.days

    /** Average progress of all active bad habits toward [QUIT_HORIZON]. `null` when there are none. */
    fun quitScore(habits: List<HabitTracking>): Int? {
        if (habits.isEmpty()) return null
        return (habits.map(::quitProgress).average() * 100).toInt()
    }

    /** What [quitScore] averages: each habit's own progress, best first. */
    fun quitLines(habits: List<HabitTracking>): List<HabitScoreLine> =
        habits.map { HabitScoreLine(it.habit.name, (quitProgress(it) * 100).toInt()) }.sortedByDescending { it.percent }

    private fun quitProgress(habit: HabitTracking): Double =
        (habit.tracking.activeElapsedMillis.toDouble() / QUIT_HORIZON.inWholeMilliseconds).coerceIn(0.0, 1.0)

    /** Average weighted completion of all good habits over the 7 days ending [endDay]. `null` when there are none. */
    fun disciplineScore(
        habits: List<GoodHabitWithLogs>,
        endDay: Long,
        createdDayIndexOf: (GoodHabitWithLogs) -> Long,
    ): Int? {
        if (habits.isEmpty()) return null
        return (habits.map { weekCompletion(it, endDay, createdDayIndexOf) }.average() * 100).toInt()
    }

    /** What [disciplineScore] averages: each habit's own completion, best first. */
    fun disciplineLines(
        habits: List<GoodHabitWithLogs>,
        endDay: Long,
        createdDayIndexOf: (GoodHabitWithLogs) -> Long,
    ): List<HabitScoreLine> =
        habits.map { HabitScoreLine(it.habit.name, (weekCompletion(it, endDay, createdDayIndexOf) * 100).toInt()) }
            .sortedByDescending { it.percent }

    private fun weekCompletion(habit: GoodHabitWithLogs, endDay: Long, createdDayIndexOf: (GoodHabitWithLogs) -> Long): Float =
        GoodHabitStatsCalculator.completion(
            counts = GoodHabitStatsCalculator.countsByDay(habit),
            target = habit.habit.dailyTarget,
            endDay = endDay,
            createdDayIndex = createdDayIndexOf(habit),
            windowDays = 7,
        )

    fun momentum(quit: Int?, discipline: Int?): Int? = when {
        quit != null && discipline != null -> (quit + discipline) / 2
        quit != null -> quit
        discipline != null -> discipline
        else -> null
    }
}
