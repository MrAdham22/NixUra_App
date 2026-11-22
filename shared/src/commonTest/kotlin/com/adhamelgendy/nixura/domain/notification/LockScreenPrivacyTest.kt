package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.model.Habit
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.model.TrackingSnapshot
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.presentation.strings.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** D-lock-screen, section 4 item 1: what each type shows on the lock screen. */
class LockScreenPrivacyTest {
    private val days = DayCalculator { 0L }
    private val now = days.epochMillisOf(2026, 11, 22, 8, 0)
    private val hour = 3_600_000L

    private fun plan(code: String): List<PlannedNotification> {
        val language = ContentLanguage { "en-US" }
        val planner = NotificationPlanner(CoachEngine(HabitKnowledgeBase(language), language), days, { stringsFor(it) }, language)
        val event = Event(1, "Secret dinner", EventCategory.PERSONAL, now + 72 * hour, listOf(2 * hour, 0L), now)
        val quit = HabitTracking(Habit(7, "Smoking", 0), emptyList(), TrackingSnapshot(now, 0, 0))
        return planner.plan(now, AppSettings(languageCode = code), "Sam", listOf(quit), emptyList(), listOf(event))
    }

    @Test fun goodHabitsAndTodayArePublic() {
        assertEquals(LockScreenVisibility.PUBLIC, LockScreenPrivacy.visibilityOf(NotificationKind.TODAY))
        assertEquals(LockScreenVisibility.PUBLIC, LockScreenPrivacy.visibilityOf(NotificationKind.GOOD_HABIT))
    }

    @Test fun eventsCoachMilestonesAndTheAppFollowThePhonesSetting() {
        for (kind in listOf(NotificationKind.EVENT, NotificationKind.COACH, NotificationKind.QUIT_MILESTONE, NotificationKind.APP)) {
            assertEquals(LockScreenVisibility.PRIVATE, LockScreenPrivacy.visibilityOf(kind), kind.name)
        }
    }

    @Test fun eachPrivateTypeHasANeutralPublicVersion() {
        val plan = plan("en")
        val events = plan.filter { it.kind == NotificationKind.EVENT }.map { it.publicTitle }.toSet()
        assertEquals(setOf<String?>("Event in 2 hours", "Event now"), events)
        assertEquals(setOf<String?>("A milestone reached"), plan.filter { it.kind == NotificationKind.QUIT_MILESTONE }.map { it.publicTitle }.toSet())
        assertEquals(setOf<String?>("A note from your coach"), plan.filter { it.kind == NotificationKind.COACH }.map { it.publicTitle }.toSet())
    }

    @Test fun thePublicVersionsAreInArabicToo() {
        val plan = plan("ar")
        val events = plan.filter { it.kind == NotificationKind.EVENT }.map { it.publicTitle }.toSet()
        assertEquals(setOf<String?>("مناسبة بعد ساعتين", "مناسبة الآن"), events)
        assertEquals("وصلت إلى محطة جديدة", plan.first { it.kind == NotificationKind.QUIT_MILESTONE }.publicTitle)
        assertEquals("ملاحظة من مدربك", plan.first { it.kind == NotificationKind.COACH }.publicTitle)
    }

    @Test fun aPublicVersionNeverNamesTheEventTheHabitOrTheUser() {
        for (item in plan("en") + plan("ar")) {
            val shown = item.publicTitle.orEmpty()
            for (secret in listOf("Secret", "dinner", "Smoking", "Sam")) assertFalse(secret in shown, "${item.key}: $shown")
        }
    }

    @Test fun anythingPlannedWithoutAPublicVersionShowsNone() {
        assertNull(PlannedNotification("good:1:2:t1", now, "t", "b").publicTitle)
    }
}
