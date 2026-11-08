package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.time.DayCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The OS schedule is always replaced by the plan built from the stored event, so an edited or removed reminder
 * can only be cancelled correctly if the plan's keys change with it. These tests pin that contract.
 */
class ReminderPlanningTest {
    private val dayCalc = DayCalculator { 0L }
    private val planner = NotificationPlanner(CoachEngine(), dayCalc)
    private val now = dayCalc.epochMillisOf(2026, 10, 20, 10, 0)
    private val hour = 3_600_000L
    private val day = 24 * hour

    private fun eventKeys(vararg leads: Long): List<String> {
        val event = Event(5, "Trip", EventCategory.TRAVEL, now + 3 * day, leads.toList(), now)
        return planner.plan(now, AppSettings(), "A", emptyList(), emptyList(), listOf(event))
            .filter { it.key.startsWith("event:") }.map { it.key }
    }

    @Test
    fun everyReminderHasItsOwnStableKey() {
        assertEquals(listOf("event:5:${day}", "event:5:${hour}").sorted(), eventKeys(hour, day).sorted())
        assertEquals(eventKeys(hour, day).sorted(), eventKeys(hour, day).sorted()) // stable across runs
    }

    @Test
    fun editingALeadReplacesItsKey() {
        val before = eventKeys(hour, day)
        val after = eventKeys(2 * hour, day)
        assertTrue("event:5:$hour" in before)
        assertFalse("event:5:$hour" in after) // old alarm is no longer in the plan -> scheduler cancels it
        assertTrue("event:5:${2 * hour}" in after)
        assertTrue("event:5:$day" in after) // untouched reminder keeps its key
    }

    @Test
    fun removingALeadDropsItsKey() {
        assertFalse("event:5:$hour" in eventKeys(day))
    }
}
