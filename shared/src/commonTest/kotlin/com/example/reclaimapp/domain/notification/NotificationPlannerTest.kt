package com.example.reclaimapp.domain.notification

import com.example.reclaimapp.domain.coach.CoachEngine
import com.example.reclaimapp.domain.model.AppSettings
import com.example.reclaimapp.domain.model.Event
import com.example.reclaimapp.domain.model.EventCategory
import com.example.reclaimapp.domain.model.Habit
import com.example.reclaimapp.domain.model.HabitTracking
import com.example.reclaimapp.domain.model.TrackingSnapshot
import com.example.reclaimapp.domain.time.DayCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class NotificationPlannerTest {
    private val dayCalc = DayCalculator { 0L }
    private val planner = NotificationPlanner(CoachEngine(), dayCalc)
    private val now = dayCalc.epochMillisOf(2026, 10, 20, 10, 0)

    private fun event(id: Long, daysAhead: Int, vararg leads: Long) = Event(
        id, "Trip", EventCategory.TRAVEL, now + daysAhead.days.inWholeMilliseconds, leads.toList(), now,
    )

    private fun bad(startedAgo: Long) = HabitTracking(
        Habit(7, "Smoking", 0),
        emptyList(),
        TrackingSnapshot(now - startedAgo, startedAgo, startedAgo),
    )

    @Test
    fun disabledNotificationsPlanNothing() {
        val plan = planner.plan(now, AppSettings(notificationsEnabled = false), "A", listOf(bad(0)), emptyList(), listOf(event(1, 3, 3_600_000)))
        assertTrue(plan.isEmpty())
    }

    @Test
    fun eventRemindersSkipPastTriggers() {
        val plan = planner.plan(
            now, AppSettings(), "A", emptyList(), emptyList(),
            listOf(event(1, 2, 1.hours.inWholeMilliseconds, 3.days.inWholeMilliseconds, 0L)),
        ).filter { it.key.startsWith("event:") }
        // 3-day lead is already in the past for an event 2 days ahead
        assertEquals(2, plan.size)
        assertTrue(plan.any { it.body == "It's time." })
        assertTrue(plan.any { it.body == "Starts in 1 hour." })
    }

    @Test
    fun nextTwoMilestonesAreScheduledAtStartPlusThreshold() {
        val started = 2.days.inWholeMilliseconds + 1
        val plan = planner.plan(now, AppSettings(), "A", listOf(bad(started)), emptyList(), emptyList())
            .filter { it.key.startsWith("milestone:") }
        assertEquals(2, plan.size) // 3d and 7d
        assertEquals(now - started + 3.days.inWholeMilliseconds, plan.first().triggerAtEpochMillis)
    }

    @Test
    fun dailyCheckInsCoverTwoWeeksAtChosenTimeAndAreSorted() {
        val plan = planner.plan(now, AppSettings(dailyCheckInMinutes = 18 * 60), "A", emptyList(), emptyList(), emptyList())
        val daily = plan.filter { it.key.startsWith("daily:") }
        assertEquals(NotificationPlanner.DAILY_HORIZON_DAYS, daily.size)
        assertTrue(daily.all { dayCalc.millisOfDay(it.triggerAtEpochMillis) == 18 * 3_600_000L })
        assertEquals(plan.map { it.triggerAtEpochMillis }.sorted(), plan.map { it.triggerAtEpochMillis })
        assertTrue(plan.all { it.triggerAtEpochMillis > now })
    }

    @Test
    fun checkInAlreadyPassedTodayStartsTomorrow() {
        val plan = planner.plan(now, AppSettings(dailyCheckInMinutes = 8 * 60), "A", emptyList(), emptyList(), emptyList())
        val daily = plan.filter { it.key.startsWith("daily:") }
        assertEquals(NotificationPlanner.DAILY_HORIZON_DAYS - 1, daily.size)
    }

    @Test
    fun keysAreUnique() {
        val plan = planner.plan(now, AppSettings(), "A", listOf(bad(0)), emptyList(), listOf(event(1, 5, 0L, 3_600_000L)))
        assertEquals(plan.size, plan.map { it.key }.toSet().size)
    }
}
