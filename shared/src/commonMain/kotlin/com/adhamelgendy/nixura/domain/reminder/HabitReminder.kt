package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.time.DayCalculator

/**
 * A good habit's reminder choices, stored with the habit (Room `good_habits`, synced with it).
 *
 * @param enabled `null` = the user never chose: the default for the habit's type applies ([HabitReminderRules.isOnByDefault]).
 * @param timeMinutes minutes after local midnight, for habits reminded at one time of day. `null` = the default time.
 * @param windowStartMinutes / [windowEndMinutes] the water window, both as times of day. An end at or before the start
 *   is on the next day (14:00 to 02:00). `null` = the default window.
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

    /** Theme WATER, daily: every 2 h inside the window the user picks (may pass midnight), only while behind pace. */
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

    /** A water reminder moves to stay this far from the other good-habit reminders. Nothing is dropped for being close. */
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

    /**
     * The water window in minutes after the local midnight it starts on. Its end is past 24:00 when the window passes
     * midnight (14:00 to 02:00 is `840..1560`). Falls back to the default when the stored one is not a real window.
     */
    fun waterWindowOf(habit: GoodHabit): IntRange =
        windowBetween(
            habit.reminder.windowStartMinutes ?: WATER_WINDOW_START_MINUTES,
            habit.reminder.windowEndMinutes ?: WATER_WINDOW_END_MINUTES,
        ) ?: (WATER_WINDOW_START_MINUTES..WATER_WINDOW_END_MINUTES)

    private fun windowBetween(start: Int, end: Int): IntRange? {
        if (start !in 0 until MINUTES_PER_DAY || end !in 0..MINUTES_PER_DAY || start == end) return null
        val last = if (end < start) end + MINUTES_PER_DAY else end
        return if (last - start >= WATER_INTERVAL_MINUTES) start..last else null
    }

    /** The times of day to store after the user picks "From". An end that is too close moves, visibly, to 2 h later. */
    fun windowStartingAt(window: IntRange, start: Int): Pair<Int, Int> {
        val end = window.last % MINUTES_PER_DAY
        return if (windowBetween(start, end) != null) start to end else start to (start + WATER_INTERVAL_MINUTES) % MINUTES_PER_DAY
    }

    /** The times of day to store after the user picks "Until". A start that is too close moves, visibly, to 2 h earlier. */
    fun windowEndingAt(window: IntRange, end: Int): Pair<Int, Int> =
        if (windowBetween(window.first, end) != null) {
            window.first to end
        } else {
            (end - WATER_INTERVAL_MINUTES + MINUTES_PER_DAY) % MINUTES_PER_DAY to end
        }

    /** True when the window is too long for a reminder every 2 h, so its [WATER_MAX_PER_DAY] are spread out instead. */
    fun isSpreadOut(window: IntRange): Boolean = window.last - window.first > WATER_MAX_PER_DAY * WATER_INTERVAL_MINUTES

    /** When water may remind, before moving away from other habits: every 2 h, or [WATER_MAX_PER_DAY] spread to the end. */
    fun waterSlotsOf(window: IntRange): List<Int> {
        val length = window.last - window.first
        return (1..WATER_MAX_PER_DAY)
            .map { n -> window.first + if (isSpreadOut(window)) length * n / WATER_MAX_PER_DAY else n * WATER_INTERVAL_MINUTES }
            .filter { it <= window.last }
    }

    /**
     * The day whose water window [atMillis] belongs to: after midnight, until a late window ends, still the day before.
     * Any other habit: the calendar day.
     */
    fun windowDayOf(habit: GoodHabit, days: DayCalculator, atMillis: Long): Long {
        val day = days.dayIndexOf(atMillis)
        if (strategyOf(habit) != ReminderStrategy.WATER) return day
        val afterMidnight = waterWindowOf(habit).last - MINUTES_PER_DAY
        return if (days.millisOfDay(atMillis) <= afterMidnight * MILLIS_PER_MINUTE) day - 1 else day
    }

    /** When "Pause for today" ([count] = 1) or "for 1 week" (7) ends: not before the late part of a water window is over. */
    fun pauseEndOf(habit: GoodHabit, days: DayCalculator, nowMillis: Long, count: Int): Long {
        val midnight = days.startOfDayMillis(windowDayOf(habit, days, nowMillis) + count)
        if (strategyOf(habit) != ReminderStrategy.WATER) return midnight
        val afterMidnight = waterWindowOf(habit).last - MINUTES_PER_DAY
        return if (afterMidnight >= 0) midnight + (afterMidnight + 1) * MILLIS_PER_MINUTE else midnight
    }

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
    private const val MILLIS_PER_MINUTE = 60_000L
}
