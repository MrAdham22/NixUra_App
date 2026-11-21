package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.model.Habit
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.model.TrackingSnapshot
import com.adhamelgendy.nixura.domain.time.DayCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class NotificationChannelsTest {

    @Test
    fun eventsKeepTheChannelIdThatExistedBeforePhaseB() {
        // Changing this ID would orphan the channel (and the user's choices for it) on every existing install.
        assertEquals("nixura_reminders", NotificationChannels.channelFor(NotificationKind.EVENT).id)
    }

    @Test
    fun everyKindHasItsOwnChannel() {
        val channels = NotificationKind.entries.map { NotificationChannels.channelFor(it) }
        assertEquals(NotificationKind.entries.size, channels.toSet().size)
        assertEquals(NixChannel.entries.toSet(), channels.toSet()) // and no channel is left unused
    }

    @Test
    fun channelAndGroupIdsAreUniqueAndNamespaced() {
        val ids = NixChannel.entries.map { it.id } + NixChannelGroup.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("nixura_") })
    }

    @Test
    fun onlyTimeBoundKindsMakeSound() {
        val loud = NixChannel.entries.filter { it.importance == ChannelImportance.DEFAULT }.toSet()
        assertEquals(setOf(NixChannel.EVENTS, NixChannel.GOOD_HABITS), loud)
    }

    @Test
    fun retiredChannelsNeverIncludeALiveOne() {
        val live = NixChannel.entries.map { it.id }.toSet()
        assertTrue(NotificationChannels.RETIRED_CHANNEL_IDS.none { it in live })
    }

    @Test
    fun groupKeysDifferPerKind() {
        val keys = NotificationKind.entries.map { NotificationChannels.groupKeyFor(it) }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun legacyKeysAreReadAsTheRightKind() {
        assertEquals(NotificationKind.EVENT, NotificationKind.fromKey("event:4:3600000"))
        assertEquals(NotificationKind.QUIT_MILESTONE, NotificationKind.fromKey("milestone:7:3:1700000000000"))
        assertEquals(NotificationKind.COACH, NotificationKind.fromKey("daily:20412"))
        assertEquals(NotificationKind.GOOD_HABIT, NotificationKind.fromKey("good:12"))
        assertEquals(NotificationKind.APP, NotificationKind.fromKey("something-else"))
    }

    @Test
    fun aPlanEntryWithoutKindTakesItFromItsKey() {
        // How AndroidNotificationScheduler decodes a plan stored before phase B.
        assertEquals(NotificationKind.EVENT, PlannedNotification("event:1:0", 0L, "t", "b").kind)
    }

    @Test
    fun plannerTagsEveryNotificationWithKindAndTarget() {
        val days = DayCalculator { 0L }
        val now = days.epochMillisOf(2026, 10, 20, 10, 0)
        val event = Event(5, "Trip", EventCategory.TRAVEL, now + 2.days.inWholeMilliseconds, listOf(1.hours.inWholeMilliseconds), now)
        val quit = HabitTracking(Habit(7, "Smoking", 0), emptyList(), TrackingSnapshot(now - 1_000, 1_000, 1_000))
        val plan = NotificationPlanner(CoachEngine(), days).plan(now, AppSettings(), "A", listOf(quit), emptyList(), listOf(event))

        plan.forEach { assertEquals(NotificationKind.fromKey(it.key), it.kind, it.key) } // explicit kind agrees with the key
        assertTrue(plan.filter { it.kind == NotificationKind.EVENT }.all { it.targetId == 5L })
        assertTrue(plan.filter { it.kind == NotificationKind.QUIT_MILESTONE }.all { it.targetId == 7L })
        assertTrue(plan.any { it.kind == NotificationKind.COACH && it.targetId == null })
    }
}
