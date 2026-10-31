package com.example.reclaimapp.domain.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class StopTimelineExplainerTest {

    private fun category(
        id: String = "alpha",
        label: String = "Alpha",
        keyword: String = "alphabet",
        safety: String? = null,
        milestones: List<BodyMilestone> = listOf(
            BodyMilestone(1.hours, "First hour", "Detail of the first hour."),
            BodyMilestone(1.days, "First day", "Detail of the first day."),
            BodyMilestone(7.days, "First week", "Detail of the first week."),
        ),
    ) = HabitCategory(
        id = id,
        label = label,
        keywords = listOf(keyword),
        whyItMatters = "why $id",
        milestones = milestones,
        cravingTips = listOf("tip one", "tip two"),
        safetyNote = safety,
    )

    private fun explainer(vararg categories: HabitCategory) =
        StopTimelineExplainer(HabitKnowledgeBase(categories.toList(), emptyList(), emptyList()))

    private val plain = explainer(category())

    @Test
    fun beforeTheFirstMilestoneItIsTheEarlyPhase() {
        val a = plain.explain("alphabet", 30.minutes)
        assertTrue(a.alreadyHappened.isEmpty())
        assertEquals("First hour", a.next?.title)
        assertEquals(30.minutes, a.timeToNext)
        assertTrue(a.headline.contains("early phase"), a.headline)
        assertTrue(a.whatIsHappening.contains("early phase"), a.whatIsHappening)
        assertTrue(a.whatIsHappening.contains("First hour"), a.whatIsHappening)
        assertFalse(a.isGenericProfile)
        assertEquals("Alpha", a.habitLabel)
    }

    @Test
    fun atTheVeryStartTheHeadlineSaysSo() {
        val a = plain.explain("alphabet", Duration.ZERO)
        assertTrue(a.headline.startsWith("At the very start"), a.headline)
        assertEquals(1.hours, a.timeToNext)
    }

    @Test
    fun betweenMilestonesUsesTheClosestEarlierOne() {
        val a = plain.explain("alphabet", 3.hours)
        assertEquals(listOf("First hour"), a.alreadyHappened.map { it.title })
        assertEquals("First day", a.next?.title)
        assertEquals(21.hours, a.timeToNext)
        assertTrue(a.headline.contains("First hour"), a.headline)
        assertTrue(a.whatIsHappening.startsWith("First hour. Detail of the first hour."), a.whatIsHappening)
    }

    @Test
    fun exactlyOnAMilestoneCountsAsReached() {
        val a = plain.explain("alphabet", 1.days)
        assertEquals(listOf("First hour", "First day"), a.alreadyHappened.map { it.title })
        assertEquals("First week", a.next?.title)
        assertEquals(6.days, a.timeToNext)
    }

    @Test
    fun afterTheLastMilestoneTheMainOnesAreBehindYou() {
        val a = plain.explain("alphabet", 30.days)
        assertEquals(3, a.alreadyHappened.size)
        assertNull(a.next)
        assertNull(a.timeToNext)
        assertTrue(a.headline.contains("behind you"), a.headline)
        assertTrue(a.whatIsHappening.contains("First week"), a.whatIsHappening)
    }

    @Test
    fun negativeElapsedIsTreatedAsZero() {
        val a = plain.explain("alphabet", (-5).hours)
        assertEquals(Duration.ZERO, a.elapsed)
        assertTrue(a.alreadyHappened.isEmpty())
    }

    @Test
    fun answerAlwaysSaysTimelinesVary() {
        listOf(10.minutes, 3.hours, 30.days).forEach {
            assertTrue(plain.explain("alphabet", it).whatIsHappening.contains("not medical advice"), it.toString())
        }
    }

    @Test
    fun unrecognisedHabitUsesTheGeneralTimelineAndSaysSo() {
        val kb = HabitKnowledgeBase(listOf(category()), emptyList(), emptyList())
        val a = StopTimelineExplainer(kb).explain("  qwzx plorf  ", 2.days)
        assertTrue(a.isGenericProfile)
        assertEquals("qwzx plorf", a.habitLabel)
        assertEquals(kb.generic.milestones.filter { it.after <= 2.days }, a.alreadyHappened)
        assertTrue(a.whatIsHappening.contains("general timeline"), a.whatIsHappening)
        assertEquals(kb.generic.cravingTips, a.cravingTips)
    }

    @Test
    fun blankHabitNameStillWorks() {
        val a = plain.explain("   ", 6.hours)
        assertTrue(a.isGenericProfile)
        assertEquals("this habit", a.habitLabel)
        assertTrue(a.headline.isNotBlank())
    }

    @Test
    fun safetyNoteIsPassedOnInFull() {
        val note = "Do not stop suddenly after heavy use. Speak to a doctor first."
        val risky = explainer(category(safety = note))
        listOf(Duration.ZERO, 30.minutes, 3.hours, 30.days).forEach {
            assertEquals(note, risky.explain("alphabet", it).safetyNote, it.toString())
        }
        assertNull(plain.explain("alphabet", 3.hours).safetyNote)
    }

    @Test
    fun cravingTipsComeFromTheHabit() {
        assertEquals(listOf("tip one", "tip two"), plain.explain("alphabet", 1.days).cravingTips)
    }

    @Test
    fun unsortedMilestonesAreHandled() {
        val shuffled = category(
            milestones = listOf(
                BodyMilestone(7.days, "Week", "w"),
                BodyMilestone(1.hours, "Hour", "h"),
                BodyMilestone(1.days, "Day", "d"),
            ),
        )
        val a = explainer(shuffled).explain("alphabet", 2.hours)
        assertEquals(listOf("Hour"), a.alreadyHappened.map { it.title })
        assertEquals("Day", a.next?.title)
    }

    @Test
    fun aHabitWithoutMilestonesDoesNotCrash() {
        val kb = HabitKnowledgeBase(listOf(category()), emptyList(), emptyList())
        val empty = category(milestones = emptyList())
        val a = StopTimelineExplainer(kb).explain(empty, "Alpha", false, 5.hours)
        assertTrue(a.alreadyHappened.isEmpty())
        assertNull(a.next)
        assertTrue(a.headline.contains("no detailed timeline"), a.headline)
    }

    @Test
    fun upcomingListsTheNextMilestonesOnly() {
        assertEquals(listOf("First day", "First week"), plain.upcoming("alphabet", 2.hours).map { it.title })
        assertEquals(listOf("First week"), plain.upcoming("alphabet", 2.hours, 2).drop(1).map { it.title })
        assertTrue(plain.upcoming("alphabet", 30.days).isEmpty())
    }

    @Test
    fun worksWithTheRealCatalogIncludingGeneric() {
        val real = StopTimelineExplainer(HabitKnowledgeBase())
        val smoking = real.explain("Smoking", 6.hours)
        assertFalse(smoking.isGenericProfile)
        assertTrue(smoking.headline.isNotBlank())
        val unknown = real.explain("qwzx plorf", 6.hours)
        assertTrue(unknown.isGenericProfile)
        assertNotNull(unknown.next)
    }

    @Test
    fun everyRealCategoryExplainsEveryPresetMoment() {
        val kb = HabitKnowledgeBase()
        val explainer = StopTimelineExplainer(kb)
        val moments = listOf(Duration.ZERO, 30.minutes, 1.hours, 3.hours, 6.hours, 12.hours, 1.days, 3.days, 7.days, 14.days, 30.days, 365.days)
        (kb.categories + kb.generic).forEach { c ->
            moments.forEach { moment ->
                val a = explainer.explain(c.label, moment)
                assertTrue(a.headline.isNotBlank(), "${c.id} $moment")
                assertTrue(a.whatIsHappening.isNotBlank(), "${c.id} $moment")
            }
        }
    }

    @Test
    fun elapsedTextIsReadable() {
        assertEquals("1 day 12 hours", elapsedText(36.hours))
        assertEquals("5 hours", elapsedText(5.hours))
        assertEquals("1 hour", elapsedText(90.minutes))
        assertEquals("10 days", elapsedText(10.days))
        assertEquals("7 days", elapsedText(7.days + 3.hours))
        assertEquals("2 days", elapsedText(2.days))
        assertEquals("0 minutes", elapsedText((-1).hours))
    }
}
