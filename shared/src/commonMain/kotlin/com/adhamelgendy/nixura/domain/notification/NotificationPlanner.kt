package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import com.adhamelgendy.nixura.domain.coach.BadHabitSnapshot
import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.coach.CoachSnapshot
import com.adhamelgendy.nixura.domain.coach.GoodHabitSnapshot
import com.adhamelgendy.nixura.domain.coach.durationText
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.HabitEventType
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.time.DayCalculator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/**
 * Pure planner: turns current app data into the list of notifications that should exist.
 * Three kinds: event reminders (user-chosen lead times), streak milestones, and a daily check-in.
 */
class NotificationPlanner(
    private val coach: CoachEngine,
    private val dayCalculator: DayCalculator,
) {

    fun plan(
        nowMillis: Long,
        settings: AppSettings,
        userName: String,
        badHabits: List<HabitTracking>,
        goodHabits: List<GoodHabitWithLogs>,
        events: List<Event>,
    ): List<PlannedNotification> {
        if (!settings.notificationsEnabled) return emptyList()
        val planned = mutableListOf<PlannedNotification>()

        // 1. Event reminders
        for (event in events) {
            for (lead in event.reminderLeadMillis.distinct()) {
                val trigger = event.targetAtEpochMillis - lead
                if (trigger <= nowMillis) continue
                planned += PlannedNotification(
                    key = "event:${event.id}:$lead",
                    triggerAtEpochMillis = trigger,
                    title = event.title,
                    body = if (lead <= 0L) "It's time." else "Starts in ${durationText(lead.milliseconds)}.",
                )
            }
        }

        // 2. Streak milestones (next two per habit)
        for (item in badHabits) {
            val start = item.tracking.activePeriodStartEpochMillis ?: continue
            val elapsed = (nowMillis - start).coerceAtLeast(0).milliseconds
            MILESTONES.filter { it > elapsed }.take(2).forEach { threshold ->
                val note = coach.milestoneNote(item.habit.name, threshold)
                planned += PlannedNotification(
                    key = "milestone:${item.habit.id}:${threshold.inWholeDays}:$start",
                    triggerAtEpochMillis = start + threshold.inWholeMilliseconds,
                    title = note.title,
                    body = note.body,
                )
            }
        }

        // 3. Daily check-in for the next two weeks
        val today = dayCalculator.dayIndexOf(nowMillis)
        for (offset in 0 until DAILY_HORIZON_DAYS) {
            val day = today + offset
            val trigger = dayCalculator.startOfDayMillis(day) + settings.dailyCheckInMinutes * 60_000L
            if (trigger <= nowMillis) continue
            val ahead = (trigger - nowMillis).milliseconds
            val snapshot = CoachSnapshot(
                userName = userName,
                badHabits = badHabits.mapNotNull { item ->
                    if (item.tracking.activePeriodStartEpochMillis == null) return@mapNotNull null
                    BadHabitSnapshot(
                        id = item.habit.id,
                        name = item.habit.name,
                        elapsed = item.tracking.activeElapsedMillis.milliseconds + ahead,
                        longest = item.tracking.longestStreakMillis.milliseconds + ahead,
                        relapseCount = item.events.count { it.type == HabitEventType.RELAPSE },
                    )
                },
                goodHabits = goodHabits.map { gh ->
                    val todayCount = if (offset == 0) gh.logs.firstOrNull { it.dayIndex == day }?.count ?: 0 else 0
                    val weekly = gh.habit.weeklyTarget
                    if (weekly == null) {
                        GoodHabitSnapshot(gh.habit.name, todayCount, gh.habit.dailyTarget, 0)
                    } else {
                        // What is known now: days already done count only while that day is still in this week.
                        val created = dayCalculator.dayIndexOf(gh.habit.createdAtEpochMillis)
                        val week = WeeklyGoalCalculator.weekStartOf(day)
                        val done = if (week == WeeklyGoalCalculator.weekStartOf(today)) {
                            WeeklyGoalCalculator.doneDays(GoodHabitStatsCalculator.countsByDay(gh), week, today, created)
                        } else {
                            0
                        }
                        GoodHabitSnapshot(gh.habit.name, todayCount, 1, 0, weekly, done, WeeklyGoalCalculator.goalFor(week, weekly, created))
                    }
                },
            )
            val note = coach.dailyCheckIn(snapshot, seed = day)
            planned += PlannedNotification(
                key = "daily:$day",
                triggerAtEpochMillis = trigger,
                title = note.title,
                body = note.body,
            )
        }

        return planned.sortedBy { it.triggerAtEpochMillis }
    }

    companion object {
        const val DAILY_HORIZON_DAYS = 14

        val MILESTONES: List<Duration> = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365).map { it.days }
    }
}
