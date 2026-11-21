package com.adhamelgendy.nixura.domain.model

import com.adhamelgendy.nixura.domain.reminder.HabitReminderSettings
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.theme.WaterGoal

/**
 * A habit the user wants to build: daily, e.g. "Drink water" 8 times a day, or weekly, e.g. "Take medicine" 3 days a
 * week ([weeklyTarget] set; see [com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator]).
 */
data class GoodHabit(
    val id: Long,
    val name: String,
    /** How many times per day counts as "done". 1 for weekly habits. */
    val dailyTarget: Int,
    val colorArgb: Long? = null,
    val createdAtEpochMillis: Long,
    val archivedAtEpochMillis: Long? = null,
    /** `null` = daily habit. 1..7 = weekly habit: done on this many days of the week (Saturday to Friday). */
    val weeklyTarget: Int? = null,
    /** Colour, icon and animation family, detected from the name when saved. */
    val theme: HabitTheme = HabitTheme.DEFAULT,
    /** Daily water habits only: the goal in ml. Then [dailyTarget] is [WaterGoal.servingsPerDay]. */
    val water: WaterGoal? = null,
    /** Reminder choices (phase C). The defaults mean "not chosen": prayer and water are on, the rest off. */
    val reminder: HabitReminderSettings = HabitReminderSettings(),
) {
    val isWeekly: Boolean get() = weeklyTarget != null
}

/** Number of times a good habit was done on one local day. [dayIndex] = local days since 1970-01-01. */
data class GoodHabitLog(
    val goodHabitId: Long,
    val dayIndex: Long,
    val count: Int,
)

data class GoodHabitWithLogs(
    val habit: GoodHabit,
    val logs: List<GoodHabitLog>,
)
