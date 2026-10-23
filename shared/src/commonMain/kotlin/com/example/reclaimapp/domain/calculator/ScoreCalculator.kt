package com.example.reclaimapp.domain.calculator

import com.example.reclaimapp.domain.model.GoodHabitWithLogs
import com.example.reclaimapp.domain.model.HabitTracking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** Derived 0..100 scores shown on Home and Profile. Nothing here is persisted. */
object ScoreCalculator {

    /** Horizon for "fully broken": matches the last streak color threshold (30 days). */
    val QUIT_HORIZON: Duration = 30.days

    /** Average progress of all active bad habits toward [QUIT_HORIZON]. `null` when there are none. */
    fun quitScore(habits: List<HabitTracking>): Int? {
        if (habits.isEmpty()) return null
        val avg = habits
            .map { (it.tracking.activeElapsedMillis.toDouble() / QUIT_HORIZON.inWholeMilliseconds).coerceIn(0.0, 1.0) }
            .average()
        return (avg * 100).toInt()
    }

    /** Average weighted completion of all good habits over the 7 days ending [endDay]. `null` when there are none. */
    fun disciplineScore(
        habits: List<GoodHabitWithLogs>,
        endDay: Long,
        createdDayIndexOf: (GoodHabitWithLogs) -> Long,
    ): Int? {
        if (habits.isEmpty()) return null
        val avg = habits.map {
            GoodHabitStatsCalculator.completion(
                counts = GoodHabitStatsCalculator.countsByDay(it),
                target = it.habit.dailyTarget,
                endDay = endDay,
                createdDayIndex = createdDayIndexOf(it),
                windowDays = 7,
            )
        }.average()
        return (avg * 100).toInt()
    }

    /** Completion across all good habits for a single day (0f..1f). */
    fun dayCompletion(habits: List<GoodHabitWithLogs>, day: Long): Float {
        if (habits.isEmpty()) return 0f
        return habits.map {
            val c = it.logs.firstOrNull { log -> log.dayIndex == day }?.count ?: 0
            (c.toFloat() / it.habit.dailyTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
        }.average().toFloat()
    }

    fun momentum(quit: Int?, discipline: Int?): Int? = when {
        quit != null && discipline != null -> (quit + discipline) / 2
        quit != null -> quit
        discipline != null -> discipline
        else -> null
    }
}
