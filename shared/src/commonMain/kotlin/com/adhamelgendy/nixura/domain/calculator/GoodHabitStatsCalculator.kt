package com.adhamelgendy.nixura.domain.calculator

import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs

enum class DayStatus { NONE, PARTIAL, DONE }

/**
 * For a weekly habit ([weeklyTarget] set) the streaks and [daysCompleted] are in **weeks**, [completionLast7] is this
 * week's progress and [completionLast30] covers the last 4 weeks (see [WeeklyGoalCalculator]).
 */
data class GoodHabitStats(
    val todayCount: Int,
    val dailyTarget: Int,
    val doneToday: Boolean,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    /** Sum of all counts ever logged ("times done"); days done for a weekly habit. */
    val totalTimesDone: Int,
    val daysCompleted: Int,
    /** Weighted completion (0f..1f) over the last 7 days, partial days count fractionally. */
    val completionLast7: Float,
    val completionLast30: Float,
    val weeklyTarget: Int? = null,
    /** Days done this week (Sunday to Saturday) and this week's goal. 0 for daily habits. */
    val weekDone: Int = 0,
    val weekGoal: Int = 0,
) {
    val isWeekly: Boolean get() = weeklyTarget != null
    val weekGoalMet: Boolean get() = isWeekly && weekDone >= weekGoal

    /** Nothing more is needed today: today's target is reached, or this week's goal is already met. */
    val settledToday: Boolean get() = doneToday || weekGoalMet

    /** The progress the cards fill with: today's count for a daily habit, days this week for a weekly one. */
    val progressCount: Int get() = if (isWeekly) weekDone else todayCount
    val progressTarget: Int get() = if (isWeekly) weekGoal else dailyTarget
}

object GoodHabitStatsCalculator {

    fun statusOf(count: Int, target: Int): DayStatus = when {
        count <= 0 -> DayStatus.NONE
        count >= target.coerceAtLeast(1) -> DayStatus.DONE
        else -> DayStatus.PARTIAL
    }

    /**
     * How full today's card is, 0..1: each tap adds 1 / target (target 10 → 10% per tap), capped at full. The same
     * number drives the in-app liquid and the widget's fill steps, so both always agree.
     */
    fun fillLevel(count: Int, target: Int): Float =
        (count.coerceAtLeast(0).toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)

    fun countsByDay(item: GoodHabitWithLogs): Map<Long, Int> =
        item.logs.associate { it.dayIndex to it.count }

    fun compute(item: GoodHabitWithLogs, todayIndex: Long, createdDayIndex: Long): GoodHabitStats {
        item.habit.weeklyTarget?.let { return computeWeekly(item, it, todayIndex, createdDayIndex) }
        val target = item.habit.dailyTarget.coerceAtLeast(1)
        val counts = countsByDay(item)
        val todayCount = counts[todayIndex] ?: 0
        val doneToday = todayCount >= target

        val doneDays = counts.filter { (_, c) -> c >= target }.keys.sorted()

        // Current streak: consecutive done days ending today, or yesterday if today is not done yet.
        var streak = 0
        var cursor = if (doneToday) todayIndex else todayIndex - 1
        while ((counts[cursor] ?: 0) >= target) {
            streak++
            cursor--
        }

        var best = 0
        var run = 0
        var previous: Long? = null
        for (day in doneDays) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            if (run > best) best = run
            previous = day
        }

        return GoodHabitStats(
            todayCount = todayCount,
            dailyTarget = target,
            doneToday = doneToday,
            currentStreakDays = streak,
            bestStreakDays = maxOf(best, streak),
            totalTimesDone = counts.values.sum(),
            daysCompleted = doneDays.size,
            completionLast7 = completion(counts, target, todayIndex, createdDayIndex, 7),
            completionLast30 = completion(counts, target, todayIndex, createdDayIndex, 30),
        )
    }

    private fun computeWeekly(item: GoodHabitWithLogs, weeklyTarget: Int, todayIndex: Long, createdDayIndex: Long): GoodHabitStats {
        val w = WeeklyGoalCalculator
        val counts = countsByDay(item)
        val todayCount = (counts[todayIndex] ?: 0).coerceAtMost(1)
        val thisWeek = w.weekStartOf(todayIndex)
        val (best, metWeeks) = w.bestStreakAndMetWeeks(counts, weeklyTarget, todayIndex, createdDayIndex)
        val current = w.currentStreakWeeks(counts, weeklyTarget, todayIndex, createdDayIndex)
        val goal = w.goalFor(thisWeek, weeklyTarget, createdDayIndex)
        return GoodHabitStats(
            todayCount = todayCount,
            dailyTarget = 1,
            doneToday = todayCount > 0,
            currentStreakDays = current,
            bestStreakDays = maxOf(best, current),
            totalTimesDone = counts.values.count { it > 0 },
            daysCompleted = metWeeks,
            completionLast7 = (w.doneDays(counts, thisWeek, todayIndex, createdDayIndex).toFloat() / goal).coerceIn(0f, 1f),
            completionLast30 = w.recentCompletion(counts, weeklyTarget, todayIndex, createdDayIndex, weeks = 4),
            weeklyTarget = weeklyTarget,
            weekDone = w.doneDays(counts, thisWeek, todayIndex, createdDayIndex),
            weekGoal = goal,
        )
    }

    /** Weighted completion over a window ending at [endDay] (inclusive), clipped to the habit's lifetime. */
    fun completion(
        counts: Map<Long, Int>,
        target: Int,
        endDay: Long,
        createdDayIndex: Long,
        windowDays: Int,
    ): Float {
        val safeTarget = target.coerceAtLeast(1)
        val firstDay = maxOf(endDay - windowDays + 1, createdDayIndex)
        if (endDay < firstDay) return 0f
        val span = (endDay - firstDay + 1).toInt()
        var sum = 0f
        for (day in firstDay..endDay) {
            val c = counts[day] ?: 0
            sum += (c.toFloat() / safeTarget).coerceIn(0f, 1f)
        }
        return sum / span
    }
}
