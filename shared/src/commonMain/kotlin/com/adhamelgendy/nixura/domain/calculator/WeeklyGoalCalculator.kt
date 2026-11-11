package com.adhamelgendy.nixura.domain.calculator

import kotlin.math.ceil

/**
 * Rules for weekly good habits ("N days a week", owner decision 2026-11-11):
 * - A week runs **Sunday to Saturday**.
 * - A day counts **once**, however many times it was logged.
 * - Reaching the goal makes the week 100%; doing more never goes above 100%.
 * - The current week never lowers the Discipline score early: until the goal is met it is left out, unless it can no
 *   longer be met (then it counts as done / goal). A finished week counts as done / goal.
 * - In the week the habit was created, the goal shrinks to the days it existed: ceil(goal × days / 7), at least 1.
 *
 * Day indexes are local days since 1970-01-01 (a Thursday).
 */
object WeeklyGoalCalculator {
    const val DAYS_IN_WEEK = 7
    const val MAX_DAYS_PER_WEEK = 7

    /** The Sunday on or before [day]. */
    fun weekStartOf(day: Long): Long = day - ((day + 4) % 7 + 7) % 7

    /** The goal for the week starting [weekStart]; smaller in the week the habit was created. */
    fun goalFor(weekStart: Long, target: Int, createdDay: Long): Int {
        val safe = target.coerceIn(1, MAX_DAYS_PER_WEEK)
        val existingDays = (weekStart + DAYS_IN_WEEK - maxOf(weekStart, createdDay)).coerceIn(0, DAYS_IN_WEEK.toLong()).toInt()
        if (existingDays >= DAYS_IN_WEEK) return safe
        return ceil(safe * existingDays / DAYS_IN_WEEK.toDouble()).toInt().coerceIn(1, safe)
    }

    /** Days with at least one log in the week starting [weekStart], from the habit's first day up to [today]. */
    fun doneDays(counts: Map<Long, Int>, weekStart: Long, today: Long, createdDay: Long): Int {
        val first = maxOf(weekStart, createdDay)
        val last = minOf(weekStart + DAYS_IN_WEEK - 1, today)
        if (last < first) return 0
        return (first..last).count { (counts[it] ?: 0) > 0 }
    }

    /**
     * The week's share of the goal, 0..1, or `null` while it is still open (goal not met yet, but still reachable).
     * Also `null` for a week that ended before the habit existed.
     */
    fun weekValue(counts: Map<Long, Int>, target: Int, weekStart: Long, today: Long, createdDay: Long): Float? {
        val weekEnd = weekStart + DAYS_IN_WEEK - 1
        if (weekEnd < createdDay || weekStart > today) return null
        val goal = goalFor(weekStart, target, createdDay)
        val done = doneDays(counts, weekStart, today, createdDay)
        if (done >= goal) return 1f
        val daysLeft = when {
            today > weekEnd -> 0L
            else -> (weekEnd - today) + if ((counts[today] ?: 0) > 0) 0 else 1
        }
        if (done + daysLeft >= goal) return null
        return done.toFloat() / goal
    }

    /** What the Discipline score uses: this week once it is decided, otherwise last week. `null` = nothing to count yet. */
    fun scoreValue(counts: Map<Long, Int>, target: Int, today: Long, createdDay: Long): Float? {
        val thisWeek = weekStartOf(today)
        weekValue(counts, target, thisWeek, today, createdDay)?.let { return it }
        return weekValue(counts, target, thisWeek - DAYS_IN_WEEK, today, createdDay)
    }

    /** Average of the decided weeks among the last [weeks] weeks (this one included); this week's progress if none. */
    fun recentCompletion(counts: Map<Long, Int>, target: Int, today: Long, createdDay: Long, weeks: Int): Float {
        val thisWeek = weekStartOf(today)
        val values = (0 until weeks).mapNotNull { back ->
            weekValue(counts, target, thisWeek - back * DAYS_IN_WEEK.toLong(), today, createdDay)
        }
        if (values.isNotEmpty()) return values.average().toFloat()
        val goal = goalFor(thisWeek, target, createdDay)
        return (doneDays(counts, thisWeek, today, createdDay).toFloat() / goal).coerceIn(0f, 1f)
    }

    fun weekMet(counts: Map<Long, Int>, target: Int, weekStart: Long, today: Long, createdDay: Long): Boolean =
        weekStart + DAYS_IN_WEEK - 1 >= createdDay &&
            doneDays(counts, weekStart, today, createdDay) >= goalFor(weekStart, target, createdDay)

    /** Weeks in a row the goal was met, ending this week if met, otherwise last week (an open week never breaks it). */
    fun currentStreakWeeks(counts: Map<Long, Int>, target: Int, today: Long, createdDay: Long): Int {
        var week = weekStartOf(today)
        if (!weekMet(counts, target, week, today, createdDay)) week -= DAYS_IN_WEEK
        var streak = 0
        while (weekMet(counts, target, week, today, createdDay)) {
            streak++
            week -= DAYS_IN_WEEK
        }
        return streak
    }

    /** Most weeks in a row ever met, and how many weeks were met in total. */
    fun bestStreakAndMetWeeks(counts: Map<Long, Int>, target: Int, today: Long, createdDay: Long): Pair<Int, Int> {
        val firstWeek = weekStartOf(createdDay)
        val thisWeek = weekStartOf(today)
        var best = 0
        var run = 0
        var met = 0
        var week = firstWeek
        while (week <= thisWeek) {
            if (weekMet(counts, target, week, today, createdDay)) {
                run++
                met++
                if (run > best) best = run
            } else if (week != thisWeek) {
                run = 0
            }
            week += DAYS_IN_WEEK
        }
        return best to met
    }
}
