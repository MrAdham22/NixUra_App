package com.example.reclaimapp.domain.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReminderRulesTest {
    private val now = 1_000_000_000L
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val target = now + 2 * day

    private fun check(lead: Long, others: Collection<Long> = emptyList(), at: Long = target) =
        ReminderRules.validate(lead, others, at, now)

    @Test
    fun acceptsFutureReminderAndAtEventTime() {
        assertNull(check(hour))
        assertNull(check(0L))
        assertNull(check(day))
    }

    @Test
    fun rejectsNegativeAndTooFarAhead() {
        assertEquals(ReminderError.NEGATIVE, check(-1L))
        assertEquals(ReminderError.TOO_FAR_AHEAD, check(ReminderRules.MAX_LEAD_MILLIS + 1))
    }

    @Test
    fun rejectsReminderWhoseTriggerIsNotInTheFuture() {
        assertEquals(ReminderError.IN_PAST, check(3 * day)) // would have fired yesterday
        assertEquals(ReminderError.IN_PAST, check(2 * day)) // would fire exactly now
    }

    @Test
    fun rejectsAnythingForAnEventThatAlreadyPassed() {
        assertEquals(ReminderError.EVENT_PASSED, check(0L, at = now - 1))
        assertEquals(ReminderError.EVENT_PASSED, check(hour, at = now))
    }

    @Test
    fun rejectsDuplicates() {
        assertEquals(ReminderError.DUPLICATE, check(hour, listOf(hour, day)))
    }

    @Test
    fun rejectsMoreThanTheLimit() {
        val full = (1..ReminderRules.MAX_REMINDERS_PER_EVENT).map { it * 1_000L }
        assertEquals(ReminderError.TOO_MANY, check(hour, full))
        assertNull(check(hour, full.dropLast(1)))
    }

    @Test
    fun normalizeDropsInvalidDuplicatesSortsAndCaps() {
        val messy = listOf(day, -5L, hour, hour, ReminderRules.MAX_LEAD_MILLIS + 1, 0L)
        assertEquals(listOf(0L, hour, day), ReminderRules.normalize(messy))
        val many = (1..30).map { it * 1_000L }
        assertEquals(ReminderRules.MAX_REMINDERS_PER_EVENT, ReminderRules.normalize(many).size)
    }
}
