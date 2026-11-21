package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.notification.NotificationAction
import com.adhamelgendy.nixura.domain.notification.NotificationActionType
import com.adhamelgendy.nixura.domain.notification.NotificationArming
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.NotificationProgress
import com.adhamelgendy.nixura.domain.notification.NotificationTiming
import com.adhamelgendy.nixura.domain.notification.PlannedNotification
import com.adhamelgendy.nixura.domain.time.DayCalculator
import kotlin.math.abs

/**
 * Pure planner for good-habit reminders (phase C). Turns habits, today's logs and this phone's reminder state into the
 * notifications that should exist for today and tomorrow, following PLAN.md's per-habit strategy:
 *
 * | Strategy | When | Skip |
 * |---|---|---|
 * | Prayer | 10 min after each prayer starts (computed offline from the saved location) | that prayer is marked |
 * | Water | every 2 h inside the window (09:00–21:00) | on or ahead of pace, or the goal is met |
 * | Daily | the chosen time (19:00), a second one 3 h later when the target is above 1 | today's target is met |
 * | Weekly | the chosen time, on days the week's goal is still reachable | done today, or the week's goal is met |
 *
 * Then, per local day: quiet hours 22:00–08:00 (Fajr and a snooze the user asked for are exempt), at most 8 reminders,
 * never two within 30 min (prayer and snoozes win over a chosen time, which wins over water), per-habit pause and the
 * phone-wide pause. A snoozed habit gets one reminder at the snooze time instead of the ones before it.
 *
 * Every reminder of a habit is posted under one key (`good:<habitId>`), so a new one replaces the one showing.
 * Each moment still has its own alarm key (`good:<habitId>:<day>:<slot>`). Logs re-plan (app, widget, notification
 * button), so a reminder planned "3 of 8 glasses" is re-written or dropped as soon as the count changes.
 */
class HabitReminderPlanner(
    private val days: DayCalculator,
    private val copies: HabitReminderCopyProvider,
) {

    fun plan(
        nowMillis: Long,
        language: String,
        habits: List<GoodHabitWithLogs>,
        device: ReminderDeviceState,
    ): List<PlannedNotification> {
        val copy = copies.forLanguage(language)
        val today = days.dayIndexOf(nowMillis)
        // Like the other planners: keep items up to the late grace, so NotificationArming can re-arm an undelivered one.
        val earliest = nowMillis - NotificationArming.LATE_GRACE_MILLIS
        val enabled = habits.filter { HabitReminderRules.isEnabled(it.habit) }
        val planned = mutableListOf<PlannedNotification>()
        for (day in today until today + HORIZON_DAYS) {
            val candidates = enabled.flatMap { item -> candidatesFor(item, day, today, nowMillis, device, copy) }
            planned += pickForDay(candidates).filter { it.triggerAtEpochMillis > earliest }
        }
        return planned.sortedBy { it.triggerAtEpochMillis }
    }

    // ---- candidates per habit ----

    private class Candidate(val notification: PlannedNotification, val priority: Int, val quietExempt: Boolean) {
        val at: Long get() = notification.triggerAtEpochMillis
    }

    private fun candidatesFor(
        item: GoodHabitWithLogs,
        day: Long,
        today: Long,
        nowMillis: Long,
        device: ReminderDeviceState,
        copy: HabitReminderCopy,
    ): List<Candidate> {
        val habit = item.habit
        // What is known now: today's real count; nothing logged yet on a later day.
        val count = if (day == today) item.logs.firstOrNull { it.dayIndex == day }?.count ?: 0 else 0
        val snoozeAt = device.snoozes[habit.id]?.takeIf { days.dayIndexOf(it) == day && it > nowMillis - NotificationArming.LATE_GRACE_MILLIS }
        val scheduled = when (HabitReminderRules.strategyOf(habit)) {
            ReminderStrategy.PRAYER -> prayer(habit, day, count, device.location, copy)
            ReminderStrategy.WATER -> water(habit, day, count, copy)
            ReminderStrategy.DAILY_TIME -> daily(habit, day, count, copy)
            ReminderStrategy.WEEKLY_TIME -> weekly(item, day, today, count, copy)
        }
        val kept = if (snoozeAt != null) scheduled.filter { it.at > snoozeAt } else scheduled
        val snoozed = snoozeAt?.let { snoozedReminder(item, day, count, it, device.location, copy) }
        return (kept + listOfNotNull(snoozed)).filterNot { candidate ->
            HabitReminderRules.isPausedAt(habit, candidate.at) || (device.pausedUntilEpochMillis ?: Long.MIN_VALUE) > candidate.at
        }
    }

    private fun prayer(habit: GoodHabit, day: Long, count: Int, location: GeoPoint?, copy: HabitReminderCopy): List<Candidate> {
        val place = location ?: return emptyList() // no location yet: the reminders sheet explains and asks
        val times = PrayerTimeCalculator.forDay(day, place)
        return Prayer.entries.mapNotNull { prayer ->
            if (count >= prayer.index) return@mapNotNull null // already marked
            val start = times[prayer] ?: return@mapNotNull null
            val at = start + HabitReminderRules.PRAYER_OFFSET_MINUTES * MINUTE
            Candidate(prayerNotification(habit, "$day:p${prayer.index}", at, prayer, count, copy), PRIORITY_PRAYER, quietExempt = prayer == Prayer.FAJR)
        }
    }

    private fun prayerNotification(habit: GoodHabit, slot: String, at: Long, prayer: Prayer, count: Int, copy: HabitReminderCopy) =
        notification(
            habit, slot, at,
            title = copy.reminderPrayerTitle(prayer),
            body = copy.reminderPrayerBody(prayer, count),
            progress = NotificationProgress(count, HabitReminderRules.PRAYERS_PER_DAY),
            actions = listOf(
                NotificationAction(NotificationActionType.LOG_PRAYER, copy.reminderActionPrayed(prayer), habit.id, prayerIndex = prayer.index),
                snooze(habit, HabitReminderRules.PRAYER_SNOOZE_MINUTES, copy),
            ),
            timing = NotificationTiming.ON_TIME,
        )

    private fun water(habit: GoodHabit, day: Long, count: Int, copy: HabitReminderCopy): List<Candidate> {
        val target = habit.dailyTarget.coerceAtLeast(1)
        if (count >= target) return emptyList()
        val window = HabitReminderRules.waterWindowOf(habit)
        return (1..HabitReminderRules.WATER_MAX_PER_DAY).mapNotNull { n ->
            val minute = window.first + n * HabitReminderRules.WATER_INTERVAL_MINUTES
            if (minute > window.last) return@mapNotNull null
            // Behind pace only: on or ahead of where the goal spread over the window says you'd be.
            if (count >= HabitReminderRules.waterExpectedBy(target, window, minute)) return@mapNotNull null
            Candidate(waterNotification(habit, "$day:w$minute", atMinute(day, minute), count, target, variant = (day + n).toInt(), copy), PRIORITY_WATER, quietExempt = false)
        }
    }

    private fun waterNotification(habit: GoodHabit, slot: String, at: Long, count: Int, target: Int, variant: Int, copy: HabitReminderCopy) =
        notification(
            habit, slot, at,
            title = copy.reminderWaterTitle(variant),
            body = if (target - count == 1) copy.reminderWaterLastBody(count, target) else copy.reminderWaterBody(count, target),
            progress = NotificationProgress(count, target),
            actions = listOf(
                NotificationAction(NotificationActionType.LOG_ONE, copy.reminderActionGlass(habit.water?.servingMl ?: DEFAULT_SERVING_ML), habit.id),
                snooze(habit, HabitReminderRules.SNOOZE_MINUTES, copy),
            ),
            timing = NotificationTiming.FLEXIBLE,
        )

    private fun daily(habit: GoodHabit, day: Long, count: Int, copy: HabitReminderCopy): List<Candidate> {
        val target = habit.dailyTarget.coerceAtLeast(1)
        if (count >= target) return emptyList()
        val first = HabitReminderRules.timeOf(habit)
        val minutes = listOfNotNull(first, (first + HabitReminderRules.SECOND_REMINDER_AFTER_MINUTES).takeIf { target > 1 && it < MINUTES_PER_DAY })
        return minutes.mapIndexed { n, minute ->
            Candidate(dailyNotification(habit, "$day:t${n + 1}", atMinute(day, minute), count, target, variant = (day + n).toInt(), copy), PRIORITY_TIME, quietExempt = false)
        }
    }

    private fun dailyNotification(habit: GoodHabit, slot: String, at: Long, count: Int, target: Int, variant: Int, copy: HabitReminderCopy) =
        notification(
            habit, slot, at,
            title = copy.reminderHabitTitle(habit.theme, habit.name, variant),
            body = copy.reminderDailyBody(habit.name, count, target),
            progress = NotificationProgress(count, target).takeIf { target > 1 },
            actions = listOf(
                NotificationAction(NotificationActionType.LOG_ONE, copy.reminderActionDone(), habit.id),
                snooze(habit, HabitReminderRules.SNOOZE_MINUTES, copy),
            ),
            timing = NotificationTiming.FLEXIBLE,
        )

    private fun weekly(item: GoodHabitWithLogs, day: Long, today: Long, count: Int, copy: HabitReminderCopy): List<Candidate> {
        val week = weekProgress(item, day, today) ?: return emptyList()
        if (count > 0) return emptyList() // done today
        return listOf(
            Candidate(weeklyNotification(item.habit, "$day:t1", atMinute(day, HabitReminderRules.timeOf(item.habit)), week, variant = day.toInt(), copy), PRIORITY_TIME, quietExempt = false),
        )
    }

    /** Days done and the goal of [day]'s week, or `null` when the goal is met or out of reach. */
    private fun weekProgress(item: GoodHabitWithLogs, day: Long, today: Long): Pair<Int, Int>? {
        val target = item.habit.weeklyTarget ?: return null
        val created = days.dayIndexOf(item.habit.createdAtEpochMillis)
        val weekStart = WeeklyGoalCalculator.weekStartOf(day)
        val goal = WeeklyGoalCalculator.goalFor(weekStart, target, created)
        // Days already done count only up to today; a later day of the week knows nothing yet.
        val done = WeeklyGoalCalculator.doneDays(GoodHabitStatsCalculator.countsByDay(item), weekStart, minOf(today, day), created)
        val daysLeft = (weekStart + WeeklyGoalCalculator.DAYS_IN_WEEK - day).toInt() // including [day]
        return if (done < goal && done + daysLeft >= goal) done to goal else null
    }

    private fun weeklyNotification(habit: GoodHabit, slot: String, at: Long, week: Pair<Int, Int>, variant: Int, copy: HabitReminderCopy) =
        notification(
            habit, slot, at,
            title = copy.reminderHabitTitle(habit.theme, habit.name, variant),
            body = copy.reminderWeeklyBody(habit.name, week.first, week.second),
            progress = NotificationProgress(week.first, week.second),
            actions = listOf(
                NotificationAction(NotificationActionType.LOG_ONE, copy.reminderActionDone(), habit.id),
                snooze(habit, HabitReminderRules.SNOOZE_MINUTES, copy),
            ),
            timing = NotificationTiming.FLEXIBLE,
        )

    /** The reminder the user asked to see again, as it reads at [at]; `null` when there is nothing left to do. */
    private fun snoozedReminder(item: GoodHabitWithLogs, day: Long, count: Int, at: Long, location: GeoPoint?, copy: HabitReminderCopy): Candidate? {
        val habit = item.habit
        val slot = "$day:s$at"
        val notification = when (HabitReminderRules.strategyOf(habit)) {
            ReminderStrategy.PRAYER -> {
                val times = location?.let { PrayerTimeCalculator.forDay(day, it) } ?: return null
                // The latest prayer that has started by then and is not marked yet.
                val prayer = Prayer.entries.lastOrNull { p -> (times[p] ?: Long.MAX_VALUE) <= at && count < p.index } ?: return null
                prayerNotification(habit, slot, at, prayer, count, copy)
            }
            ReminderStrategy.WATER -> {
                val target = habit.dailyTarget.coerceAtLeast(1)
                if (count >= target) return null
                waterNotification(habit, slot, at, count, target, variant = day.toInt(), copy)
            }
            ReminderStrategy.DAILY_TIME -> {
                val target = habit.dailyTarget.coerceAtLeast(1)
                if (count >= target) return null
                dailyNotification(habit, slot, at, count, target, variant = day.toInt(), copy)
            }
            ReminderStrategy.WEEKLY_TIME -> {
                if (count > 0) return null
                val week = weekProgress(item, day, day) ?: return null
                weeklyNotification(habit, slot, at, week, variant = day.toInt(), copy)
            }
        }
        // The user asked for this moment, so quiet hours do not drop it.
        return Candidate(notification, PRIORITY_SNOOZE, quietExempt = true)
    }

    // ---- one day's limits ----

    /** Quiet hours, then the daily cap and the minimum gap, most important first. */
    private fun pickForDay(candidates: List<Candidate>): List<PlannedNotification> {
        val accepted = mutableListOf<Candidate>()
        candidates
            .filter { it.quietExempt || !HabitReminderRules.isQuiet((days.millisOfDay(it.at) / MINUTE).toInt()) }
            .sortedWith(compareBy<Candidate>({ it.priority }, { it.at }))
            .forEach { candidate ->
                if (accepted.size >= HabitReminderRules.MAX_PER_DAY) return@forEach
                if (accepted.any { abs(it.at - candidate.at) < HabitReminderRules.MIN_GAP_MINUTES * MINUTE }) return@forEach
                accepted += candidate
            }
        return accepted.map { it.notification }
    }

    // ---- helpers ----

    private fun notification(
        habit: GoodHabit,
        slot: String,
        at: Long,
        title: String,
        body: String,
        progress: NotificationProgress?,
        actions: List<NotificationAction>,
        timing: NotificationTiming,
    ) = PlannedNotification(
        key = "${postKeyOf(habit.id)}:$slot",
        triggerAtEpochMillis = at,
        title = title,
        body = body,
        kind = NotificationKind.GOOD_HABIT,
        targetId = habit.id,
        theme = habit.theme,
        postKey = postKeyOf(habit.id),
        actions = actions,
        progress = progress,
        timing = timing,
    )

    private fun snooze(habit: GoodHabit, minutes: Int, copy: HabitReminderCopy) =
        NotificationAction(NotificationActionType.SNOOZE, copy.reminderActionSnooze(minutes), habit.id, snoozeMinutes = minutes)

    /** Local wall-clock [minute] of [day], DST-safe (through [DayCalculator], never "+24 h"). */
    private fun atMinute(day: Long, minute: Int): Long =
        days.epochMillisOfLocal(day * DayCalculator.MILLIS_PER_DAY + minute * MINUTE)

    companion object {
        /** Today and tomorrow: water and prayer change daily, so the plan stays small (far under the 60-alarm cap). */
        const val HORIZON_DAYS = 2

        /** The notification key every reminder of a habit is posted under (one on screen per habit). */
        fun postKeyOf(habitId: Long): String = "${NotificationKind.GOOD_HABIT_PREFIX}$habitId"

        private const val MINUTE = 60_000L
        private const val MINUTES_PER_DAY = 24 * 60
        private const val DEFAULT_SERVING_ML = 250
        private const val PRIORITY_SNOOZE = 0
        private const val PRIORITY_PRAYER = 1
        private const val PRIORITY_TIME = 2
        private const val PRIORITY_WATER = 3
    }
}
