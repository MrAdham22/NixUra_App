package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import com.adhamelgendy.nixura.domain.coach.BadHabitSnapshot
import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.coach.CoachSnapshot
import com.adhamelgendy.nixura.domain.coach.GoodHabitSnapshot
import com.adhamelgendy.nixura.domain.coach.arAfterPreposition
import com.adhamelgendy.nixura.domain.coach.durationText
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.locale.AppLocales
import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.HabitEventType
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.time.DayCalculator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import com.adhamelgendy.nixura.domain.privacy.OutsideAppPrivacy

/**
 * Pure planner: turns current app data into the list of notifications that should exist.
 * Three kinds: event reminders (user-chosen lead times), streak milestones, and a daily check-in.
 * One of the two exits where a hidden name is taken out ([OutsideAppPrivacy]).
 */
class NotificationPlanner(
    private val coach: CoachEngine,
    private val dayCalculator: DayCalculator,
    /** What a phone that hides sensitive content shows instead ([LockScreenPrivacy]). */
    private val copies: LockScreenCopyProvider,
    /** Notifications are written in the saved language (resolved here, so background re-planning gets it right). */
    private val language: ContentLanguage = ContentLanguage.English,
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
        val shownLanguage = language.update(settings.languageCode)
        val arabic = shownLanguage == AppLocales.ARABIC
        val neutral = copies.forLanguage(shownLanguage)
        val planned = mutableListOf<PlannedNotification>()
        // Items up to this late are still planned, so a re-plan does not cancel an alarm the OS has not delivered yet
        // (NotificationArming decides whether it is armed again).
        val lateLimit = nowMillis - NotificationArming.LATE_GRACE_MILLIS

        // 1. Event reminders
        for (event in events) {
            for (lead in event.reminderLeadMillis.distinct()) {
                val trigger = event.targetAtEpochMillis - lead
                if (trigger <= lateLimit) continue
                planned += PlannedNotification(
                    key = "${NotificationKind.EVENT_PREFIX}${event.id}:$lead",
                    triggerAtEpochMillis = trigger,
                    kind = NotificationKind.EVENT,
                    targetId = event.id,
                    title = event.title,
                    publicTitle = neutral.lockScreenEvent(if (lead <= 0L) null else durationText(lead.milliseconds, arabic)),
                    body = when {
                        arabic && lead <= 0L -> "حان الوقت."
                        arabic -> "تبدأ بعد ${arAfterPreposition(durationText(lead.milliseconds, arabic = true))}."
                        lead <= 0L -> "It's time."
                        else -> "Starts in ${durationText(lead.milliseconds)}."
                    },
                )
            }
        }

        // 2. Streak milestones (next two per habit)
        for (item in badHabits) {
            val start = item.tracking.activePeriodStartEpochMillis ?: continue
            val elapsed = (nowMillis - start).coerceAtLeast(0).milliseconds
            val justPassed = MILESTONES.filter { it <= elapsed && start + it.inWholeMilliseconds > lateLimit }
            (justPassed + MILESTONES.filter { it > elapsed }.take(2)).forEach { threshold ->
                val name = OutsideAppPrivacy.nameOutside(item.habit)
                val note = coach.milestoneNote(name, threshold)
                planned += PlannedNotification(
                    key = "${NotificationKind.MILESTONE_PREFIX}${item.habit.id}:${threshold.inWholeDays}:$start",
                    triggerAtEpochMillis = start + threshold.inWholeMilliseconds,
                    kind = NotificationKind.QUIT_MILESTONE,
                    targetId = item.habit.id,
                    title = note.title,
                    body = note.body,
                    publicTitle = neutral.lockScreenMilestone(),
                    namedHabitId = item.habit.id.takeIf { name != null },
                )
            }
        }

        // 3. Daily check-in for the next two weeks
        val today = dayCalculator.dayIndexOf(nowMillis)
        for (offset in -1 until DAILY_HORIZON_DAYS) { // -1: yesterday's check-in may still be due just after midnight
            val day = today + offset
            val trigger = dayCalculator.startOfDayMillis(day) + settings.dailyCheckInMinutes * 60_000L
            if (trigger <= lateLimit) continue
            val ahead = (trigger - nowMillis).coerceAtLeast(0).milliseconds
            val snapshot = CoachSnapshot(
                userName = userName,
                badHabits = badHabits.mapNotNull { item ->
                    if (item.tracking.activePeriodStartEpochMillis == null) return@mapNotNull null
                    BadHabitSnapshot(
                        id = item.habit.id,
                        name = OutsideAppPrivacy.nameOutside(item.habit).orEmpty(), // empty = hidden
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
                key = "${NotificationKind.DAILY_PREFIX}$day",
                triggerAtEpochMillis = trigger,
                kind = NotificationKind.COACH,
                title = note.title,
                body = note.body,
                publicTitle = neutral.lockScreenCoach(),
                namedHabitId = coach.checkInFocus(snapshot)?.takeIf { it.name.isNotEmpty() }?.id,
            )
        }

        return planned.sortedBy { it.triggerAtEpochMillis }
    }

    companion object {
        const val DAILY_HORIZON_DAYS = 14

        val MILESTONES: List<Duration> = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365).map { it.days }
    }
}
