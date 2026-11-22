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
 * | Water | every 2 h inside the window (default 09:00–21:00, may pass midnight) | on or ahead of pace, or the goal is met |
 * | Daily | the chosen time (19:00) on time, a second one 3 h later (may pass midnight) when the target is above 1 | today's target is met |
 * | Weekly | the chosen time on time, on days the week's goal is still reachable | done today, or the week's goal is met |
 *
 * There are no quiet hours and no limit shared between habits (owner decision 2026-11-22): the user picks the times,
 * and each habit has its own maximum (prayer 5, water 6, daily 2, weekly 1). Prayers, chosen times and snoozes keep
 * their moment. A water reminder that would come within 30 min of one **moves** inside its window; it is never dropped
 * for being close. Where it moves depends on the schedule only, not on what is logged, so logging another habit cannot
 * pull it back to a time already past. Also: per-habit pause and the phone-wide pause. A snoozed habit gets one
 * reminder at the snooze time instead of the ones before it.
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
        // From yesterday: a window (or a late reminder) can reach past midnight.
        val planDays = (today - 1) until (today + HORIZON_DAYS)
        val waterTimes = placeWater(enabled.map { it.habit }, planDays, device.location)
        return planDays
            .flatMap { day -> enabled.flatMap { item -> remindersFor(item, day, today, nowMillis, device, waterTimes, copy) } }
            .filter { it.triggerAtEpochMillis > earliest }
            .sortedBy { it.triggerAtEpochMillis }
    }

    // ---- reminders per habit ----

    private data class WaterSlot(val habitId: Long, val day: Long, val minute: Int)

    private fun remindersFor(
        item: GoodHabitWithLogs,
        day: Long,
        today: Long,
        nowMillis: Long,
        device: ReminderDeviceState,
        waterTimes: Map<WaterSlot, Long>,
        copy: HabitReminderCopy,
    ): List<PlannedNotification> {
        val habit = item.habit
        val count = countOn(item, day) // a day still to come has nothing logged yet
        val strategy = HabitReminderRules.strategyOf(habit)
        val snoozeAt = device.snoozes[habit.id]?.takeIf { it > nowMillis - NotificationArming.LATE_GRACE_MILLIS }
        val scheduled = when (strategy) {
            ReminderStrategy.PRAYER -> prayer(habit, day, count, device.location, copy)
            ReminderStrategy.WATER -> water(item, day, waterTimes, copy)
            ReminderStrategy.DAILY_TIME -> daily(item, day, copy)
            ReminderStrategy.WEEKLY_TIME -> weekly(item, day, today, count, copy)
        }
        // A snooze holds back the regular moments before it, also across midnight; water also the ones right after it.
        val heldUntil = snoozeAt?.let { if (strategy == ReminderStrategy.WATER) it + GAP - 1 else it }
        val kept = if (heldUntil != null) scheduled.filter { it.triggerAtEpochMillis > heldUntil } else scheduled
        val snoozed = snoozeAt?.takeIf { days.dayIndexOf(it) == day }?.let { snoozedReminder(item, day, count, it, device.location, copy) }
        return (kept + listOfNotNull(snoozed)).filterNot { reminder ->
            val at = reminder.triggerAtEpochMillis
            HabitReminderRules.isPausedAt(habit, at) || (device.pausedUntilEpochMillis ?: Long.MIN_VALUE) > at
        }
    }

    private fun countOn(item: GoodHabitWithLogs, day: Long): Int = item.logs.firstOrNull { it.dayIndex == day }?.count ?: 0

    /** Done for the reminder answered at [at]: after midnight, the evening's and the night's together. */
    private fun countAt(item: GoodHabitWithLogs, at: Long): Int {
        val day = days.dayIndexOf(at)
        val from = HabitReminderRules.reminderDayOf(item.habit, days, at, HabitReminderRules.SNOOZE_MINUTES)
        return countOn(item, day) + if (from < day) countOn(item, from) else 0
    }

    /** Done by the reminder at [minute] of [day]. Past 24:00 it also counts what was logged after midnight. */
    private fun countBy(item: GoodHabitWithLogs, day: Long, minute: Int): Int =
        countOn(item, day) + if (minute >= MINUTES_PER_DAY) countOn(item, day + 1) else 0

    private fun prayer(habit: GoodHabit, day: Long, count: Int, location: GeoPoint?, copy: HabitReminderCopy): List<PlannedNotification> {
        val place = location ?: return emptyList() // no location yet: the reminders sheet explains and asks
        val times = PrayerTimeCalculator.forDay(day, place)
        return Prayer.entries.mapNotNull { prayer ->
            if (count >= prayer.index) return@mapNotNull null // already marked
            val start = times[prayer] ?: return@mapNotNull null
            val at = start + HabitReminderRules.PRAYER_OFFSET_MINUTES * MINUTE
            prayerNotification(habit, "$day:p${prayer.index}", at, prayer, count, copy)
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

    private fun water(item: GoodHabitWithLogs, day: Long, times: Map<WaterSlot, Long>, copy: HabitReminderCopy): List<PlannedNotification> {
        val habit = item.habit
        val target = habit.dailyTarget.coerceAtLeast(1)
        val window = HabitReminderRules.waterWindowOf(habit)
        return HabitReminderRules.waterSlotsOf(window).mapIndexedNotNull { n, minute ->
            val count = countBy(item, day, minute)
            if (count >= target) return@mapIndexedNotNull null
            // Behind pace only: on or ahead of where the goal spread over the window says you'd be.
            if (count >= HabitReminderRules.waterExpectedBy(target, window, minute)) return@mapIndexedNotNull null
            val at = times[WaterSlot(habit.id, day, minute)] ?: atMinute(day, minute)
            waterNotification(habit, "$day:w$minute", at, count, target, variant = (day + n + 1).toInt(), copy)
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

    private fun daily(item: GoodHabitWithLogs, day: Long, copy: HabitReminderCopy): List<PlannedNotification> {
        val habit = item.habit
        val target = habit.dailyTarget.coerceAtLeast(1)
        return HabitReminderRules.dailyTimesOf(habit).mapIndexedNotNull { n, minute ->
            val count = countBy(item, day, minute)
            if (count >= target) return@mapIndexedNotNull null
            dailyNotification(habit, "$day:t${n + 1}", atMinute(day, minute), count, target, variant = (day + n).toInt(), copy)
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
            timing = NotificationTiming.ON_TIME,
        )

    private fun weekly(item: GoodHabitWithLogs, day: Long, today: Long, count: Int, copy: HabitReminderCopy): List<PlannedNotification> {
        val week = weekProgress(item, day, today) ?: return emptyList()
        if (count > 0) return emptyList() // done today
        return listOf(
            weeklyNotification(item.habit, "$day:t1", atMinute(day, HabitReminderRules.timeOf(item.habit)), week, variant = day.toInt(), copy),
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
            timing = NotificationTiming.ON_TIME,
        )

    /** The reminder the user asked to see again, as it reads at [at]; `null` when there is nothing left to do. */
    private fun snoozedReminder(item: GoodHabitWithLogs, day: Long, count: Int, at: Long, location: GeoPoint?, copy: HabitReminderCopy): PlannedNotification? {
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
                val glasses = countAt(item, at)
                if (glasses >= target) return null
                waterNotification(habit, slot, at, glasses, target, variant = day.toInt(), copy)
            }
            ReminderStrategy.DAILY_TIME -> {
                val target = habit.dailyTarget.coerceAtLeast(1)
                val done = countAt(item, at)
                if (done >= target) return null
                dailyNotification(habit, slot, at, done, target, variant = day.toInt(), copy)
            }
            ReminderStrategy.WEEKLY_TIME -> {
                if (count > 0) return null
                val week = weekProgress(item, day, day) ?: return null
                weeklyNotification(habit, slot, at, week, variant = day.toInt(), copy)
            }
        }
        return notification
    }

    // ---- water next to the other habits ----

    /**
     * Where each water slot rings. The full schedule of the other habits is used (as if nothing were logged or paused),
     * so a slot keeps its time when they are. A close slot moves later, up to the window's end and before the next
     * slot; else earlier, after the slot before it. With no room at all it keeps its time rather than being lost.
     */
    private fun placeWater(habits: List<GoodHabit>, planDays: LongRange, location: GeoPoint?): Map<WaterSlot, Long> {
        val busy = mutableListOf<Long>()
        for (day in planDays.first..planDays.last + 1) { // one more: the last window may pass midnight
            for (habit in habits) busy += fixedTimesOf(habit, day, location)
        }
        val placed = mutableMapOf<WaterSlot, Long>()
        for (habit in habits.filter { HabitReminderRules.strategyOf(it) == ReminderStrategy.WATER }.sortedBy { it.id }) {
            val window = HabitReminderRules.waterWindowOf(habit)
            val minutes = HabitReminderRules.waterSlotsOf(window)
            for (day in planDays) {
                var previous = atMinute(day, window.first)
                minutes.forEachIndexed { i, minute ->
                    val wanted = atMinute(day, minute)
                    val latest = minutes.getOrNull(i + 1)?.let { atMinute(day, it) - 1 } ?: atMinute(day, window.last)
                    val at = freeTimeNear(wanted, after = previous, latest = latest, busy = busy)
                    placed[WaterSlot(habit.id, day, minute)] = at
                    busy += at
                    previous = at
                }
            }
        }
        return placed
    }

    /** The moments that never move: prayers and chosen times. */
    private fun fixedTimesOf(habit: GoodHabit, day: Long, location: GeoPoint?): List<Long> = when (HabitReminderRules.strategyOf(habit)) {
        ReminderStrategy.PRAYER -> location
            ?.let { PrayerTimeCalculator.forDay(day, it).starts.values.map { start -> start + HabitReminderRules.PRAYER_OFFSET_MINUTES * MINUTE } }
            .orEmpty()
        ReminderStrategy.WATER -> emptyList()
        ReminderStrategy.DAILY_TIME -> HabitReminderRules.dailyTimesOf(habit).map { atMinute(day, it) }
        ReminderStrategy.WEEKLY_TIME -> listOf(atMinute(day, HabitReminderRules.timeOf(habit)))
    }

    private fun freeTimeNear(wanted: Long, after: Long, latest: Long, busy: List<Long>): Long {
        fun isFree(at: Long) = busy.none { abs(it - at) < GAP }
        if (isFree(wanted)) return wanted
        // The nearest free moment is always exactly one gap away from something busy.
        val later = busy.map { it + GAP }.filter { it in (wanted + 1)..latest && isFree(it) }.minOrNull()
        return later ?: busy.map { it - GAP }.filter { it in (after + 1) until wanted && isFree(it) }.maxOrNull() ?: wanted
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
        private const val GAP = HabitReminderRules.MIN_GAP_MINUTES * MINUTE
        private const val DEFAULT_SERVING_ML = 250
    }
}
