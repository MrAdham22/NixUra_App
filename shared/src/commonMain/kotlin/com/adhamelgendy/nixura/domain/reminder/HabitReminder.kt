package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.theme.HabitTheme

/**
 * A good habit's reminder choices, stored with the habit (Room `good_habits`, synced with it).
 *
 * @param enabled `null` = the user never chose: the default for the habit's type applies ([HabitReminderRules.isOnByDefault]).
 * @param timeMinutes minutes after local midnight, for habits reminded at one time of day. `null` = the default time.
 * @param windowStartMinutes / [windowEndMinutes] the water window. `null` = the default window.
 * @param pausedUntilEpochMillis no reminders for this habit before this moment ("Pause for today", "Pause for 1 week").
 */
data class HabitReminderSettings(
    val enabled: Boolean? = null,
    val timeMinutes: Int? = null,
    val windowStartMinutes: Int? = null,
    val windowEndMinutes: Int? = null,
    val pausedUntilEpochMillis: Long? = null,
)

/** How a habit is reminded (PLAN.md, phase C strategy table). Derived from the habit, never stored. */
enum class ReminderStrategy {
    /** Theme PRAYER with 5 a day: ~10 min after each prayer starts, for the next unmarked prayer. Needs a location. */
    PRAYER,

    /** Theme WATER, daily: every 2 h inside a waking window, only while behind pace. */
    WATER,

    /** Any other daily habit: one time the user picks (a second one 3 h later when the target is above 1). */
    DAILY_TIME,

    /** A weekly habit: one time on the days the week's goal is still reachable. */
    WEEKLY_TIME,
}

/** The approved rules and defaults of phase C (PLAN.md). Pure, unit-tested. */
object HabitReminderRules {
    const val PRAYERS_PER_DAY = 5

    /** Minutes after a prayer's start time: never before it starts, early enough to still be useful. */
    const val PRAYER_OFFSET_MINUTES = 10
    const val PRAYER_SNOOZE_MINUTES = 15

    const val WATER_WINDOW_START_MINUTES = 9 * 60
    const val WATER_WINDOW_END_MINUTES = 21 * 60
    const val WATER_INTERVAL_MINUTES = 120
    const val WATER_MAX_PER_DAY = 6

    const val DEFAULT_TIME_MINUTES = 19 * 60
    const val SECOND_REMINDER_AFTER_MINUTES = 3 * 60
    const val SNOOZE_MINUTES = 60

    /** No good-habit reminder from 22:00 to 08:00, except Fajr for a prayer habit (the user kept its reminder on). */
    const val QUIET_START_MINUTES = 22 * 60
    const val QUIET_END_MINUTES = 8 * 60

    /** Across all good habits on one local day, and never two closer than this. */
    const val MAX_PER_DAY = 8
    const val MIN_GAP_MINUTES = 30

    fun strategyOf(habit: GoodHabit): ReminderStrategy = when {
        habit.isWeekly -> ReminderStrategy.WEEKLY_TIME
        habit.theme == HabitTheme.PRAYER && habit.dailyTarget == PRAYERS_PER_DAY -> ReminderStrategy.PRAYER
        habit.theme == HabitTheme.WATER -> ReminderStrategy.WATER
        else -> ReminderStrategy.DAILY_TIME
    }

    /** Time-based habits (prayer, water) are suggested on; the rest are offered, off until the user turns them on. */
    fun isOnByDefault(habit: GoodHabit): Boolean =
        strategyOf(habit) == ReminderStrategy.PRAYER || strategyOf(habit) == ReminderStrategy.WATER

    /** The user's choice, or the default for this kind of habit. */
    fun isEnabled(habit: GoodHabit): Boolean = habit.reminder.enabled ?: isOnByDefault(habit)

    fun timeOf(habit: GoodHabit): Int = habit.reminder.timeMinutes?.takeIf { it in 0 until MINUTES_PER_DAY } ?: DEFAULT_TIME_MINUTES

    /** The water window, falling back to the default when the stored one is missing or not a real window. */
    fun waterWindowOf(habit: GoodHabit): IntRange {
        val start = habit.reminder.windowStartMinutes ?: WATER_WINDOW_START_MINUTES
        val end = habit.reminder.windowEndMinutes ?: WATER_WINDOW_END_MINUTES
        val valid = start in 0 until MINUTES_PER_DAY && end in 0..MINUTES_PER_DAY && end - start >= WATER_INTERVAL_MINUTES
        return if (valid) start..end else WATER_WINDOW_START_MINUTES..WATER_WINDOW_END_MINUTES
    }

    /** True for a minute of the day inside the quiet hours (22:00–08:00, across midnight). */
    fun isQuiet(minuteOfDay: Int): Boolean = minuteOfDay >= QUIET_START_MINUTES || minuteOfDay < QUIET_END_MINUTES

    /** True when [habit] is paused at [atMillis]. */
    fun isPausedAt(habit: GoodHabit, atMillis: Long): Boolean = (habit.reminder.pausedUntilEpochMillis ?: Long.MIN_VALUE) > atMillis

    /**
     * How many glasses should be done by [minuteOfDay] to stay on pace: the goal spread evenly over the window,
     * rounded down (so the first slot never nags for a fraction of a glass).
     */
    fun waterExpectedBy(target: Int, window: IntRange, minuteOfDay: Int): Int {
        if (minuteOfDay <= window.first) return 0
        if (minuteOfDay >= window.last) return target
        return (target.toLong() * (minuteOfDay - window.first) / (window.last - window.first)).toInt()
    }

    private const val MINUTES_PER_DAY = 24 * 60
}
