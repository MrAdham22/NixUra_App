package com.adhamelgendy.nixura.domain.support

import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.HabitTracking

/**
 * The user's real progress, shown on the "Don't delete NixUra!" screen. Built from data the app already has; nothing
 * is stored. [bestStreakDays] is the longest streak running right now (whole days) across quit habits.
 */
data class KeepNixUraSummary(
    val quitHabits: Int,
    val goodHabits: Int,
    val bestStreakDays: Long,
    /** Name of the quit habit with that streak, null when there is none. */
    val bestStreakHabit: String?,
    /** Guests have no cloud copy: deleting the app deletes everything. */
    val isGuest: Boolean,
    /** Daily goal of the first water habit, in cups ("8 glasses of tears"); null without a water habit. */
    val waterGlasses: Int? = null,
) {
    val hasProgress: Boolean get() = quitHabits > 0 || goodHabits > 0

    companion object {
        private const val DAY_MILLIS = 86_400_000L

        fun from(quit: List<HabitTracking>, good: List<GoodHabitWithLogs>, isGuest: Boolean): KeepNixUraSummary {
            // activeElapsedMillis is computed when the list is read (TrackingStateCalculator), so it is current.
            val best = quit.maxByOrNull { it.tracking.activeElapsedMillis }
            return KeepNixUraSummary(
                quitHabits = quit.size,
                goodHabits = good.size,
                bestStreakDays = (best?.tracking?.activeElapsedMillis ?: 0L) / DAY_MILLIS,
                bestStreakHabit = best?.habit?.name,
                isGuest = isGuest,
                waterGlasses = good.firstNotNullOfOrNull { it.habit.water }
                    ?.let { ((it.goalMl + it.servingMl / 2) / it.servingMl).coerceAtLeast(1) },
            )
        }
    }
}
