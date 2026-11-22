package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import com.adhamelgendy.nixura.domain.coach.arAfterPreposition
import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.presentation.strings.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** After a preposition (بعد, منذ, إلى) an Arabic dual is genitive: "بعد ساعتين", never "بعد ساعتان". */
class ArabicDualAfterPrepositionTest {
    private val dayCalc = DayCalculator { 0L }
    private val now = dayCalc.epochMillisOf(2026, 10, 20, 10, 0)

    private fun arabic() = ContentLanguage { "en-US" }.also { it.update("ar") }

    @Test
    fun everyDualIsMadeGenitive() {
        assertEquals("ساعتين", arAfterPreposition("ساعتان"))
        assertEquals("يومين", arAfterPreposition("يومان"))
        assertEquals("دقيقتين", arAfterPreposition("دقيقتان"))
        assertEquals("أسبوعين", arAfterPreposition("أسبوعان"))
        assertEquals("3 أيام", arAfterPreposition("3 أيام"))
    }

    @Test
    fun eventReminderBodySaysBaadSaatayn() {
        val language = arabic()
        val planner = NotificationPlanner(CoachEngine(HabitKnowledgeBase(language), language), dayCalc, { stringsFor(it) }, language)
        val event = Event(1, "Trip", EventCategory.TRAVEL, now + 3.days.inWholeMilliseconds,
            listOf(2.hours.inWholeMilliseconds, 2.days.inWholeMilliseconds, 2.minutes.inWholeMilliseconds), now)
        val bodies = planner.plan(now, AppSettings(languageCode = "ar"), "A", emptyList(), emptyList(), listOf(event))
            .filter { it.key.startsWith("event:") }.map { it.body }.toSet()
        assertEquals(setOf("تبدأ بعد ساعتين.", "تبدأ بعد يومين.", "تبدأ بعد دقيقتين."), bodies)
    }

    @Test
    fun milestoneTitleWithADualIsGenitiveAfterMundhu() {
        val language = arabic()
        val coach = CoachEngine(HabitKnowledgeBase(language), language)
        val note = coach.milestoneNote(null, 2.days)
        assertEquals("صامد منذ يومين", note.title)
        assertTrue(note.body.startsWith("وصلت إلى يومين"), note.body)
    }
}
