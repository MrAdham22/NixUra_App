package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.recognition.GoodHabitLexicon
import com.adhamelgendy.nixura.domain.recognition.HabitTopic
import com.adhamelgendy.nixura.domain.recognition.HabitTopicDetector
import com.adhamelgendy.nixura.domain.reminder.HabitReminderCopyProvider
import com.adhamelgendy.nixura.domain.reminder.HabitReminderPlanner
import com.adhamelgendy.nixura.domain.reminder.HabitReminderSettings
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceState
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.icon
import com.adhamelgendy.nixura.presentation.strings.EnglishStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Owner decision 6 (D-lock-screen 4.2): a good habit's notification shows the glyph that is on its card. */
class NotificationGlyphTest {
    private val detector = HabitTopicDetector(HabitKnowledgeBase())
    private val days = DayCalculator { 0L }
    private val now = days.epochMillisOf(2026, 11, 22, 8, 0)
    private val planner = HabitReminderPlanner(days, HabitReminderCopyProvider { EnglishStrings }, detector)

    private fun planned(name: String, theme: HabitTheme = HabitTheme.DEFAULT): PlannedNotification {
        val habit = GoodHabit(1, name, 1, createdAtEpochMillis = 0, theme = theme, reminder = HabitReminderSettings(enabled = true))
        return planner.plan(now, "en", listOf(GoodHabitWithLogs(habit, emptyList())), ReminderDeviceState()).first()
    }

    @Test fun everyTopicAGoodHabitCanGetDrawsTheCardsGlyph() {
        for (topic in HabitTopic.entries.filter { it != HabitTopic.OTHER_BUILD }) {
            assertEquals(topic, NotificationGlyph.topicToDraw(NotificationKind.GOOD_HABIT, HabitTheme.DEFAULT, topic))
            assertTrue(topic.icon() != NixIcon.Gear, "$topic: the gear cannot be drawn outside Compose")
        }
    }

    @Test fun everyKindOfGoodHabitIsPlannedWithItsCardsTopic() {
        for (entry in GoodHabitLexicon.entries) {
            for (name in entry.keywords) {
                assertEquals(detector.buildTopic(name), planned(name).topic, name)
            }
        }
    }

    @Test fun theStoredThemeAloneIsNotEnough() {
        val names = mapOf("Meditation" to HabitTopic.CALM, "Journal" to HabitTopic.WRITING, "Piano" to HabitTopic.MUSIC, "تأمل" to HabitTopic.CALM)
        for ((name, topic) in names) {
            val item = planned(name) // all of them have the DEFAULT theme
            assertEquals(topic, NotificationGlyph.topicToDraw(item.kind, item.theme, item.topic), name)
        }
    }

    @Test fun aRenamedHabitsNextNotificationFollowsTheNewName() {
        assertEquals(HabitTopic.LEARNING, planned("Reading").topic)
        assertEquals(HabitTopic.CALM, planned("Meditation").topic)
    }

    @Test fun waterAndPrayerKeepTheirOwnIcons() {
        assertNull(NotificationGlyph.topicToDraw(NotificationKind.GOOD_HABIT, HabitTheme.WATER, HabitTopic.WATER))
        assertNull(NotificationGlyph.topicToDraw(NotificationKind.GOOD_HABIT, HabitTheme.PRAYER, HabitTopic.PRAYER))
    }

    @Test fun noNotificationShowsTheLeaf() {
        // Owner decision 2026-11-22 (evening): the NixUra mark took the leaf's place in every notification.
        for (kind in NotificationKind.entries) {
            for (theme in HabitTheme.entries) {
                for (topic in HabitTopic.entries) {
                    assertTrue(NotificationGlyph.topicToDraw(kind, theme, topic)?.icon() != NixIcon.Leaf, "$kind $theme $topic")
                }
            }
        }
    }

    @Test fun anUnrecognisedHabitGetsTheNixUraMark() {
        val item = planned("Zzqxv")
        assertEquals(HabitTopic.OTHER_BUILD, item.topic)
        assertNull(NotificationGlyph.topicToDraw(item.kind, item.theme, item.topic))
        assertNull(NotificationGlyph.topicToDraw(NotificationKind.GOOD_HABIT, null, null), "a bundle stands for several habits")
    }

    @Test fun otherKindsNeverShowACategory() {
        for (kind in NotificationKind.entries.filter { it != NotificationKind.GOOD_HABIT }) {
            for (topic in HabitTopic.entries) {
                assertNull(NotificationGlyph.topicToDraw(kind, null, topic), "$kind $topic would reveal the habit")
            }
        }
    }
}
