package com.adhamelgendy.nixura.domain.coach

import com.adhamelgendy.nixura.domain.coach.ai.CoachAction
import com.adhamelgendy.nixura.domain.coach.ai.CoachActionProblem
import com.adhamelgendy.nixura.domain.coach.ai.CoachActionRules
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.time.TimeZoneOffsetProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoachActionRulesTest {
    private val days = DayCalculator(TimeZoneOffsetProvider { 0L })

    // 2026-11-14 12:00 UTC.
    private val now = days.epochMillisOf(2026, 11, 14, 12, 0)

    private fun problem(action: CoachAction) = CoachActionRules.problem(action, now, days)

    @Test
    fun habitNamesFollowTheFormRules() {
        assertEquals(CoachActionProblem.NameMissing, problem(CoachAction.Quit("   ")))
        assertEquals(CoachActionProblem.NameTooLong, problem(CoachAction.Build("x".repeat(81), 1, null)))
        assertNull(problem(CoachAction.Quit("Smoking")))
        assertNull(problem(CoachAction.Build("x".repeat(80), 1, null)))
    }

    @Test
    fun buildTargetsMustBeInRange() {
        assertEquals(CoachActionProblem.DailyTarget, problem(CoachAction.Build("Water", 0, null)))
        assertEquals(CoachActionProblem.DailyTarget, problem(CoachAction.Build("Water", 100, null)))
        assertEquals(CoachActionProblem.DaysAWeek, problem(CoachAction.Build("Gym", 1, 0)))
        assertEquals(CoachActionProblem.DaysAWeek, problem(CoachAction.Build("Gym", 1, 8)))
        assertNull(problem(CoachAction.Build("Water", 5, null)))
        assertNull(problem(CoachAction.Build("Gym", 1, 3)))
    }

    @Test
    fun eventTitleIsLimitedLikeTheEventForm() {
        assertEquals(CoachActionProblem.NameTooLong, problem(CoachAction.Event("x".repeat(61), 2026, 12, 1, 9, 0)))
        assertNull(problem(CoachAction.Event("x".repeat(60), 2026, 12, 1, 9, 0)))
    }

    @Test
    fun eventsMustBeInTheFuture() {
        assertEquals(CoachActionProblem.InThePast, problem(CoachAction.Event("Meeting", 2026, 11, 14, 11, 59)))
        assertNull(problem(CoachAction.Event("Meeting", 2026, 11, 14, 12, 1)))
        // No time means 9:00, which has passed today but not tomorrow.
        assertEquals(CoachActionProblem.InThePast, problem(CoachAction.Event("Meeting", 2026, 11, 14, null, null)))
        assertNull(problem(CoachAction.Event("Meeting", 2026, 11, 15, null, null)))
    }

    @Test
    fun impossibleDatesAreCaught() {
        assertEquals(CoachActionProblem.BadDate, problem(CoachAction.Event("Trip", 2027, 2, 30, 9, 0)))
        assertEquals(CoachActionProblem.BadDate, problem(CoachAction.Event("Trip", 2027, 13, 1, 9, 0)))
        assertEquals(CoachActionProblem.BadDate, problem(CoachAction.Event("Trip", 2027, 1, 1, 24, 0)))
    }

    @Test
    fun switchingKindKeepsTheName() {
        assertEquals(CoachAction.Build("Running", 1, null), CoachActionRules.switchKind(CoachAction.Quit("Running")))
        assertEquals(CoachAction.Quit("Soda"), CoachActionRules.switchKind(CoachAction.Build("Soda", 4, 3)))
    }

    @Test
    fun pickedTimeIsStoredOnTheEvent() {
        val event = CoachAction.Event("Dentist", 2026, 11, 20, null, null, EventCategory.HEALTH)
        val moved = CoachActionRules.withEventTime(event, days.epochMillisOf(2026, 12, 2, 16, 30), days)
        assertEquals(CoachAction.Event("Dentist", 2026, 12, 2, 16, 30, EventCategory.HEALTH), moved)
    }

    @Test
    fun cleanedTrimsTypedText() {
        assertEquals(CoachAction.Quit("Soda"), CoachActionRules.cleaned(CoachAction.Quit("  Soda ")))
        assertEquals("Trip", (CoachActionRules.cleaned(CoachAction.Event(" Trip ", 2026, 12, 1, 9, 0)) as CoachAction.Event).title)
    }
}
