package com.adhamelgendy.nixura.domain.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationArmingTest {
    private val now = 1_800_000_000_000L
    private val minute = 60_000L

    private fun item(key: String, at: Long) = PlannedNotification(key, at, "t", "b")

    private fun keys(list: List<PlannedNotification>) = list.map { it.key }

    @Test
    fun futureNotificationsAreArmedInTimeOrder() {
        val planned = listOf(item("event:1:0", now + 10 * minute), item("daily:5", now + minute))
        assertEquals(listOf("daily:5", "event:1:0"), keys(NotificationArming.select(planned, emptyList(), emptyMap(), now)))
    }

    @Test
    fun aLateAlarmTheOsHasNotDeliveredIsArmedAgain() {
        // Armed for 18:25, the OS has not delivered it yet at 18:27, the app re-plans: it must not be lost.
        val late = item("event:2:600000", now - 2 * minute)
        assertEquals(listOf(late), NotificationArming.select(listOf(late), armed = listOf(late), delivered = emptyMap(), nowMillis = now))
    }

    @Test
    fun aDeliveredNotificationIsNeverShownTwice() {
        val late = item("event:2:600000", now - 2 * minute)
        val result = NotificationArming.select(listOf(late), listOf(late), mapOf(late.key to late.triggerAtEpochMillis), now)
        assertTrue(result.isEmpty())
    }

    @Test
    fun anEventMovedIntoThePastDoesNotFireAtOnce() {
        // Was armed for tomorrow; the user moved the event to 5 minutes ago. The UI says reminders won't be sent.
        val before = item("event:3:0", now + 1_440 * minute)
        val moved = item("event:3:0", now - 5 * minute)
        assertTrue(NotificationArming.select(listOf(moved), listOf(before), emptyMap(), now).isEmpty())
    }

    @Test
    fun aNotificationNeverArmedIsNotFiredLate() {
        assertTrue(NotificationArming.select(listOf(item("daily:9", now - minute)), emptyList(), emptyMap(), now).isEmpty())
    }

    @Test
    fun aDeletedEventsLateReminderIsDropped() {
        val late = item("event:4:0", now - minute)
        assertTrue(NotificationArming.select(planned = emptyList(), armed = listOf(late), delivered = emptyMap(), nowMillis = now).isEmpty())
    }

    @Test
    fun tooLateIsDropped() {
        val stale = item("event:5:0", now - NotificationArming.LATE_GRACE_MILLIS)
        assertTrue(NotificationArming.select(listOf(stale), listOf(stale), emptyMap(), now).isEmpty())
    }

    @Test
    fun anEditedFutureTimeIsArmedEvenIfTheOldTimeWasDelivered() {
        val delivered = mapOf("event:6:0" to now - 10 * minute)
        val moved = item("event:6:0", now + 30 * minute)
        assertEquals(listOf(moved), NotificationArming.select(listOf(moved), emptyList(), delivered, now))
    }

    @Test
    fun onlyRemindersCloseByBecomeAlarmClocks() {
        val lead = NotificationArming.ALARM_CLOCK_LEAD_MILLIS
        assertTrue(NotificationArming.armAsAlarmClock(now + lead, now))
        assertTrue(NotificationArming.armAsAlarmClock(now - minute, now)) // due: fire at once, exactly
        assertTrue(!NotificationArming.armAsAlarmClock(now + lead + 1, now))
    }

    @Test
    fun theWakeUpIsSetForTheSoonestFarReminder() {
        val lead = NotificationArming.ALARM_CLOCK_LEAD_MILLIS
        val armed = listOf(
            item("event:1:0", now + 10 * minute), // already an alarm clock
            item("daily:2", now + 300 * minute),
            item("event:3:0", now + 200 * minute),
        )
        assertEquals(now + 200 * minute - lead, NotificationArming.handOffAt(armed, now))
    }

    @Test
    fun noWakeUpWhenEverythingIsCloseOrNothingIsArmed() {
        assertEquals(null, NotificationArming.handOffAt(listOf(item("event:1:0", now + 5 * minute)), now))
        assertEquals(null, NotificationArming.handOffAt(emptyList(), now))
    }

    @Test
    fun theWakeUpArrivesBeforeTheReminderEvenWhenTheOsWidensIt() {
        // ColorOS widens an exact alarm to 75% of the time left, capped at 1 hour.
        fun latestDelivery(armedAt: Long, at: Long) = at + minOf((at - armedAt) * 3 / 4, 60 * minute)
        for (aheadMinutes in listOf(76L, 90L, 120L, 600L, 20_000L)) {
            val reminder = now + aheadMinutes * minute
            val wake = NotificationArming.handOffAt(listOf(item("e", reminder)), now)!!
            assertTrue(latestDelivery(now, wake) < reminder, "reminder $aheadMinutes min ahead")
        }
    }

    @Test
    fun theArmedCountIsCappedKeepingTheSoonest() {
        val planned = (1..80).map { item("daily:$it", now + it * minute) }
        val result = NotificationArming.select(planned, emptyList(), emptyMap(), now)
        assertEquals(NotificationArming.MAX_ARMED, result.size)
        assertEquals("daily:1", result.first().key)
    }
}
