package com.adhamelgendy.nixura.domain.privacy

import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.Habit
import com.adhamelgendy.nixura.domain.model.HabitEvent
import com.adhamelgendy.nixura.domain.model.HabitEventType
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.model.TrackingSnapshot
import com.adhamelgendy.nixura.domain.money.CostUnit
import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.NotificationPlanner
import com.adhamelgendy.nixura.domain.notification.PlannedNotification
import com.adhamelgendy.nixura.domain.recognition.HabitTopic
import com.adhamelgendy.nixura.domain.recognition.HabitTopicDetector
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.widget.QuitWidgetState
import com.adhamelgendy.nixura.domain.widget.WidgetStateMapper
import com.adhamelgendy.nixura.presentation.strings.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/** D-lock-screen 4.1: a hidden name never reaches a planned notification or a widget state. */
class HiddenNameTest {
    private val language = ContentLanguage { "en-US" }
    private val knowledge = HabitKnowledgeBase(language)
    private val engine = CoachEngine(knowledge, language)
    private val calc = DayCalculator { 0L }
    private val planner = NotificationPlanner(engine, calc, { stringsFor(it) }, language)
    private val detector = HabitTopicDetector(knowledge)
    private val now = calc.epochMillisOf(2026, 11, 22, 8, 0)

    private val names = listOf(
        "Smoking", "Vaping", "Shisha", "Alcohol", "Weed", "Gambling", "Porn", "Painkillers", "Nail biting",
        "Online shopping", "Zorblaxing", "التدخين", "القمار", "الكحول", "الاباحية",
    )
    private val ages = listOf(0L, 1L, 2L, 6L, 13L, 29L, 89L, 364L)

    private fun habit(id: Long, name: String, ageDays: Long, hidden: Boolean, cost: HabitCost? = null): HabitTracking {
        val start = now - ageDays.days.inWholeMilliseconds
        return HabitTracking(
            Habit(id, name, start, cost = cost, hideNameOutside = hidden),
            listOf(HabitEvent(id * 100, id, HabitEventType.TRACKING_START, start)),
            TrackingSnapshot(start, now - start, now - start),
        )
    }

    private fun plan(code: String, vararg habits: HabitTracking): List<PlannedNotification> =
        planner.plan(now, AppSettings(languageCode = code), "", habits.toList(), emptyList(), emptyList())

    @Test fun aHiddenNameNeverReachesAPlannedNotification() {
        for (code in listOf("en", "ar")) for (name in names) for (age in ages) {
            for (item in plan(code, habit(1, name, age, hidden = true))) {
                val everything = item.toString()
                val category = knowledge.match(name)
                assertFalse(everything.contains(name, ignoreCase = true), "$code $name: $everything")
                assertFalse(everything.contains(engine.displayName(name), ignoreCase = true), "$code $name: $everything")
                for (milestone in category.milestones) {
                    assertFalse(milestone.detail in everything, "$code $name, a category milestone: $everything")
                }
                assertNull(item.namedHabitId)
                assertNull(item.topic)
            }
        }
    }

    @Test fun aHiddenHabitsTextIsTheSameWhateverTheHabitIs() {
        // The strongest form of "reveals nothing": no habit can be told from another by its notifications.
        for (code in listOf("en", "ar")) for (age in ages) {
            val texts = names.map { name -> plan(code, habit(1, name, age, hidden = true)).map { listOf(it.key, it.title, it.body, it.publicTitle) } }
            assertEquals(1, texts.toSet().size, "$code, $age days")
        }
    }

    @Test fun theMilestoneKeepsEverythingButTheName() {
        val en = plan("en", habit(1, "Smoking", 6, hidden = true)).first { it.kind == NotificationKind.QUIT_MILESTONE }
        assertEquals("7 days clear", en.title)
        assertEquals("You reached 7 days. Keep going — you are proving it to yourself.", en.body)
        assertEquals(1L, en.targetId) // a tap still opens the habit
        assertEquals(now + 1.days.inWholeMilliseconds, en.triggerAtEpochMillis)
        val ar = plan("ar", habit(1, "Smoking", 6, hidden = true)).first { it.kind == NotificationKind.QUIT_MILESTONE }
        assertEquals("صامد منذ 7 أيام", ar.title)
        assertEquals("وصلت إلى 7 أيام. واصل، فأنت تثبت ذلك لنفسك.", ar.body)
    }

    @Test fun theCheckInKeepsTheDaysAndTheEncouragement() {
        val en = plan("en", habit(1, "Smoking", 7, hidden = true)).first { it.kind == NotificationKind.COACH }
        assertTrue(en.body.startsWith("7 days clear. "), en.body)
        val ar = plan("ar", habit(1, "Smoking", 7, hidden = true)).first { it.kind == NotificationKind.COACH }
        assertTrue(ar.body.startsWith("صامد منذ 7 أيام. "), ar.body)
        val shown = plan("en", habit(1, "Smoking", 7, hidden = false)).first { it.kind == NotificationKind.COACH }
        assertEquals("Smoking: " + en.body, shown.body)
    }

    @Test fun onlyTheHiddenHabitIsMasked() {
        val plan = plan("en", habit(1, "Gambling", 20, hidden = true), habit(2, "Smoking", 6, hidden = false))
        val milestones = plan.filter { it.kind == NotificationKind.QUIT_MILESTONE }
        assertTrue(milestones.filter { it.targetId == 1L }.none { "ambl" in it.title + it.body })
        assertTrue(milestones.filter { it.targetId == 2L }.all { "Smoking" in it.title })
    }

    @Test fun aNotificationSaysWhoseNameItShowsSoItCanBeRemovedLater() {
        val plan = plan("en", habit(1, "Gambling", 20, hidden = false), habit(2, "Smoking", 6, hidden = false))
        for (item in plan.filter { it.kind == NotificationKind.QUIT_MILESTONE }) assertEquals(item.targetId, item.namedHabitId)
        assertEquals(2L, plan.first { it.kind == NotificationKind.COACH }.namedHabitId) // the check-in is about the newest streak
    }

    @Test fun aHiddenNameNeverReachesAWidgetState() {
        val cost = HabitCost("Marlboro Red", CostUnit.CIGARETTE, 20.0, 10_000, 20, "EGP")
        for (name in names) {
            val state = assertIs<QuitWidgetState.Active>(
                WidgetStateMapper.quit(1, listOf(habit(1, name, 7, hidden = true, cost = cost)), now, detector::quitTopic),
            )
            assertNull(state.name)
            assertEquals(HabitTopic.OTHER_QUIT, state.topic, "$name: the category icon would reveal it")
            assertFalse(state.toString().contains(name, ignoreCase = true), state.toString())
            assertFalse("Marlboro" in state.toString(), "what it used to cost names the habit too")
        }
    }

    @Test fun theWidgetKeepsEveryOtherDetail() {
        val cost = HabitCost("Marlboro Red", CostUnit.CIGARETTE, 20.0, 10_000, 20, "EGP")
        val shown = assertIs<QuitWidgetState.Active>(WidgetStateMapper.quit(1, listOf(habit(1, "Smoking", 7, false, cost)), now, detector::quitTopic))
        val hidden = assertIs<QuitWidgetState.Active>(WidgetStateMapper.quit(1, listOf(habit(1, "Smoking", 7, true, cost)), now, detector::quitTopic))
        assertEquals(shown.elapsedMillis, hidden.elapsedMillis)
        assertEquals(shown.longestStreakMillis, hidden.longestStreakMillis)
        assertEquals(shown.moneySaved?.savedMinor, hidden.moneySaved?.savedMinor)
        assertEquals(shown.nextChangeAtEpochMillis, hidden.nextChangeAtEpochMillis)
        assertEquals("Smoking", shown.name)
        assertEquals(HabitTopic.SMOKING, shown.topic)
    }

    @Test fun theRecentsPreviewIsHiddenWhileAnyNameIs() {
        assertFalse(OutsideAppPrivacy.anyNameHidden(emptyList()))
        assertFalse(OutsideAppPrivacy.anyNameHidden(listOf(habit(1, "Smoking", 1, false))))
        assertTrue(OutsideAppPrivacy.anyNameHidden(listOf(habit(1, "Smoking", 1, false), habit(2, "Gambling", 1, true))))
    }
}
