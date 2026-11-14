package com.adhamelgendy.nixura.domain.coach.ai

import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.usecase.MAX_DAILY_TARGET
import com.adhamelgendy.nixura.domain.usecase.MAX_EVENT_TITLE

/** Why an understood (or edited) item cannot be added yet. */
enum class CoachActionProblem { NameMissing, NameTooLong, DailyTarget, DaysAWeek, BadDate, InThePast }

/**
 * The same rules the normal add forms use, for the items the AI understood and the user may have edited.
 * Pure logic: the sheet shows the problem, and nothing with a problem is added.
 */
object CoachActionRules {
    const val MAX_HABIT_NAME = 80

    /** Time used for an event the AI gave no time for, like the event form's default. */
    const val DEFAULT_EVENT_HOUR = 9

    fun problem(action: CoachAction, nowEpochMillis: Long, days: DayCalculator): CoachActionProblem? = when (action) {
        is CoachAction.Quit -> nameProblem(action.name, MAX_HABIT_NAME)
        is CoachAction.Build -> nameProblem(action.name, MAX_HABIT_NAME) ?: when {
            action.daysPerWeek != null && action.daysPerWeek !in 1..WeeklyGoalCalculator.MAX_DAYS_PER_WEEK -> CoachActionProblem.DaysAWeek
            action.daysPerWeek == null && action.perDay !in 1..MAX_DAILY_TARGET -> CoachActionProblem.DailyTarget
            else -> null
        }
        is CoachAction.Event -> nameProblem(action.title, MAX_EVENT_TITLE) ?: eventProblem(action, nowEpochMillis, days)
    }

    /** When the event happens. A missing time means [DEFAULT_EVENT_HOUR]. */
    fun eventMillis(action: CoachAction.Event, days: DayCalculator): Long =
        days.epochMillisOf(action.year, action.month, action.day, action.hour ?: DEFAULT_EVENT_HOUR, action.minute ?: 0)

    /** The user switched between quit and build: keep the name, use the build defaults (once a day). */
    fun switchKind(action: CoachAction): CoachAction = when (action) {
        is CoachAction.Quit -> CoachAction.Build(action.name, perDay = 1, daysPerWeek = null)
        is CoachAction.Build -> CoachAction.Quit(action.name)
        is CoachAction.Event -> action
    }

    /** The event moved to [epochMillis], picked with the date and time picker (so it now has a time). */
    fun withEventTime(action: CoachAction.Event, epochMillis: Long, days: DayCalculator): CoachAction.Event {
        val civil = CivilCalendar.civilFromDayIndex(days.dayIndexOf(epochMillis))
        val minute = (days.millisOfDay(epochMillis) / 60_000L).toInt()
        return action.copy(year = civil.year, month = civil.month, day = civil.day, hour = minute / 60, minute = minute % 60)
    }

    /** Trims what the user typed, the way the add forms save it. */
    fun cleaned(action: CoachAction): CoachAction = when (action) {
        is CoachAction.Quit -> action.copy(name = action.name.trim())
        is CoachAction.Build -> action.copy(name = action.name.trim())
        is CoachAction.Event -> action.copy(title = action.title.trim())
    }

    private fun nameProblem(name: String, max: Int): CoachActionProblem? {
        val clean = name.trim()
        return when {
            clean.isEmpty() -> CoachActionProblem.NameMissing
            clean.length > max -> CoachActionProblem.NameTooLong
            else -> null
        }
    }

    private fun eventProblem(action: CoachAction.Event, now: Long, days: DayCalculator): CoachActionProblem? {
        val validDate = action.month in 1..12 && action.day in 1..CivilCalendar.monthLength(action.year, action.month) &&
            (action.hour == null || action.hour in 0..23) && (action.minute == null || action.minute in 0..59)
        if (!validDate) return CoachActionProblem.BadDate
        // A missing time is saved as 9:00, so that is the moment that must still be ahead.
        return if (eventMillis(action, days) <= now) CoachActionProblem.InThePast else null
    }
}
