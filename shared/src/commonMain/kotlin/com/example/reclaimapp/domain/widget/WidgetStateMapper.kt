package com.example.reclaimapp.domain.widget

import com.example.reclaimapp.domain.calculator.GoodHabitStatsCalculator
import com.example.reclaimapp.domain.calculator.TrackingStateCalculator
import com.example.reclaimapp.domain.model.Event
import com.example.reclaimapp.domain.model.GoodHabitWithLogs
import com.example.reclaimapp.domain.model.HabitTracking
import com.example.reclaimapp.domain.time.CivilCalendar
import com.example.reclaimapp.domain.time.DayCalculator

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
        )
    }

    fun good(
        boundHabitId: Long?,
        habits: List<GoodHabitWithLogs>,
        nowEpochMillis: Long,
        days: DayCalculator,
    ): GoodWidgetState {
        val active = habits.filter { it.habit.archivedAtEpochMillis == null }
        if (active.isEmpty()) return GoodWidgetState.NoHabits
        if (boundHabitId == null) return GoodWidgetState.NotConfigured
        val item = active.firstOrNull { it.habit.id == boundHabitId } ?: return GoodWidgetState.HabitMissing
        val todayIndex = days.dayIndexOf(nowEpochMillis)
        val createdDayIndex = days.dayIndexOf(item.habit.createdAtEpochMillis)
        val stats = GoodHabitStatsCalculator.compute(item, todayIndex, createdDayIndex)
        val counts = GoodHabitStatsCalculator.countsByDay(item)
        val week = (6 downTo 0).map { offset ->
            val day = todayIndex - offset
            WidgetDay(
                isoWeekday = CivilCalendar.isoWeekday(day),
                status = GoodHabitStatsCalculator.statusOf(counts[day] ?: 0, stats.dailyTarget),
                enabled = day >= createdDayIndex - 6, // same rule as the in-app week strip
                isToday = offset == 0,
            )
        }
        return GoodWidgetState.Active(
            habitId = item.habit.id,
            name = item.habit.name,
            count = stats.todayCount,
            target = stats.dailyTarget,
            colorArgb = item.habit.colorArgb,
            week = week,
            currentStreakDays = stats.currentStreakDays,
            totalTimesDone = stats.totalTimesDone,
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
