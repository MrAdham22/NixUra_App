package com.example.reclaimapp.domain.calculator

import com.example.reclaimapp.domain.model.GoodHabitWithLogs

enum class DayStatus { NONE, PARTIAL, DONE }

data class GoodHabitStats(
    val todayCount: Int,
    val dailyTarget: Int,
    val doneToday: Boolean,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    /** Sum of all counts ever logged ("times done"). */
    val totalTimesDone: Int,
    val daysCompleted: Int,
    /** Weighted completion (0f..1f) over the last 7 days, partial days count fractionally. */
    val completionLast7: Float,
    val completionLast30: Float,
)

object GoodHabitStatsCalculator {

    fun statusOf(count: Int, target: Int): DayStatus = when {
        count <= 0 -> DayStatus.NONE
        count >= target.coerceAtLeast(1) -> DayStatus.DONE
        else -> DayStatus.PARTIAL
    }

    fun countsByDay(item: GoodHabitWithLogs): Map<Long, Int> =
        item.logs.associate { it.dayIndex to it.count }

    fun compute(item: GoodHabitWithLogs, todayIndex: Long, createdDayIndex: Long): GoodHabitStats {
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
