package com.adhamelgendy.nixura.domain.widget

import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.calculator.TrackingStateCalculator
import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.recognition.GoodHabitFillStyle
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.domain.time.DayCalculator

/**
 * Maps repository data to widget states. All numbers come from the same calculators the app screens use
 * ([TrackingStateCalculator], [GoodHabitStatsCalculator]) so a widget never disagrees with the app.
 */
object WidgetStateMapper {

    fun quit(boundHabitId: Long?, trackings: List<HabitTracking>, nowEpochMillis: Long): QuitWidgetState {
        if (trackings.isEmpty()) return QuitWidgetState.NoHabits
        if (boundHabitId == null) return QuitWidgetState.NotConfigured
        val item = trackings.firstOrNull { it.habit.id == boundHabitId && !it.habit.isArchived }
            ?: return QuitWidgetState.HabitMissing
        val snapshot = TrackingStateCalculator.compute(item.events, nowEpochMillis.coerceAtLeast(0))
        return QuitWidgetState.Active(
            habitId = item.habit.id,
            name = item.habit.name,
            elapsedMillis = snapshot.activeElapsedMillis,
            periodStartEpochMillis = snapshot.activePeriodStartEpochMillis,
            colorArgb = item.habit.colorArgb,
            longestStreakMillis = snapshot.longestStreakMillis,
            cost = item.habit.cost,
            nowEpochMillis = nowEpochMillis,
        )
    }

    fun good(
        boundHabitId: Long?,
        habits: List<GoodHabitWithLogs>,
        nowEpochMillis: Long,
        days: DayCalculator,
        fillStyleOf: (String) -> GoodHabitFillStyle = { GoodHabitFillStyle.PLAIN },
    ): GoodWidgetState {
        val active = habits.filter { it.habit.archivedAtEpochMillis == null }
        if (active.isEmpty()) return GoodWidgetState.NoHabits
        if (boundHabitId == null) return GoodWidgetState.NotConfigured
        val item = active.firstOrNull { it.habit.id == boundHabitId } ?: return GoodWidgetState.HabitMissing
        val todayIndex = days.dayIndexOf(nowEpochMillis)
        val createdDayIndex = days.dayIndexOf(item.habit.createdAtEpochMillis)
        val stats = GoodHabitStatsCalculator.compute(item, todayIndex, createdDayIndex)
        val counts = GoodHabitStatsCalculator.countsByDay(item)
        // Same days as the in-app strip: the last 7 days, or this week (Sunday to Saturday) for a weekly habit.
        val firstDay = if (stats.isWeekly) WeeklyGoalCalculator.weekStartOf(todayIndex) else todayIndex - 6
        val week = (0 until 7).map { i ->
            val day = firstDay + i
            WidgetDay(
                isoWeekday = CivilCalendar.isoWeekday(day),
                status = GoodHabitStatsCalculator.statusOf(counts[day] ?: 0, stats.dailyTarget),
                enabled = if (stats.isWeekly) day <= todayIndex else day >= createdDayIndex - 6,
                isToday = day == todayIndex,
                ratio = GoodHabitStatsCalculator.fillLevel(counts[day] ?: 0, stats.dailyTarget),
            )
        }
        return GoodWidgetState.Active(
            habitId = item.habit.id,
            name = item.habit.name,
            count = stats.progressCount,
            target = stats.progressTarget,
            weeklyTarget = stats.weeklyTarget,
            todayCount = stats.todayCount,
            colorArgb = item.habit.colorArgb,
            week = week,
            currentStreakDays = stats.currentStreakDays,
            totalTimesDone = stats.totalTimesDone,
            fillStyle = fillStyleOf(item.habit.name),
        )
    }

    /** [boundEventId] `null` = show the nearest event. */
    fun event(boundEventId: Long?, events: List<Event>, nowEpochMillis: Long): EventWidgetState {
        if (events.isEmpty()) return EventWidgetState.NoEvents
        val event = if (boundEventId == null) {
            NearestEventSelector.select(events, nowEpochMillis)
        } else {
            events.firstOrNull { it.id == boundEventId }
        } ?: return if (boundEventId == null) EventWidgetState.NoEvents else EventWidgetState.EventMissing
        val remaining = event.targetAtEpochMillis - nowEpochMillis
        return if (remaining > 0) {
            EventWidgetState.Upcoming(event.id, event.title, event.targetAtEpochMillis, remaining, event.category, event.reminderLeadMillis.size)
        } else {
            EventWidgetState.Passed(event.id, event.title, event.targetAtEpochMillis, -remaining, event.category, event.reminderLeadMillis.size)
        }
    }
}
