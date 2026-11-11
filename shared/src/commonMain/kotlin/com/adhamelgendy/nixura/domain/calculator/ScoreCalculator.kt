package com.adhamelgendy.nixura.domain.calculator

import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.HabitTracking
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

    /**
     * Average completion of all good habits for the week ending [endDay]: the last 7 days for a daily habit, and for a
     * weekly habit this week once it is decided, otherwise last week ([WeeklyGoalCalculator.scoreValue]). A weekly habit
     * with nothing decided yet is left out, so it never lowers the score early. `null` when nothing counts.
     */
    fun disciplineScore(
        habits: List<GoodHabitWithLogs>,
        endDay: Long,
        createdDayIndexOf: (GoodHabitWithLogs) -> Long,
    ): Int? {
        val values = habits.mapNotNull { weekCompletion(it, endDay, createdDayIndexOf) }
        if (values.isEmpty()) return null
        return (values.average() * 100).toInt()
    }

    /** What [disciplineScore] averages: each counted habit's own completion, best first. */
    fun disciplineLines(
        habits: List<GoodHabitWithLogs>,
        endDay: Long,
        createdDayIndexOf: (GoodHabitWithLogs) -> Long,
    ): List<HabitScoreLine> =
        habits.mapNotNull { habit ->
            weekCompletion(habit, endDay, createdDayIndexOf)?.let { HabitScoreLine(habit.habit.name, (it * 100).toInt()) }
        }.sortedByDescending { it.percent }

    private fun weekCompletion(habit: GoodHabitWithLogs, endDay: Long, createdDayIndexOf: (GoodHabitWithLogs) -> Long): Float? {
        val counts = GoodHabitStatsCalculator.countsByDay(habit)
        val createdDay = createdDayIndexOf(habit)
        habit.habit.weeklyTarget?.let { return WeeklyGoalCalculator.scoreValue(counts, it, endDay, createdDay) }
        return GoodHabitStatsCalculator.completion(
            counts = counts,
            target = habit.habit.dailyTarget,
            endDay = endDay,
            createdDayIndex = createdDay,
            windowDays = 7,
        )
    }

    fun momentum(quit: Int?, discipline: Int?): Int? = when {
        quit != null && discipline != null -> (quit + discipline) / 2
        quit != null -> quit
        discipline != null -> discipline
        else -> null
    }
}
