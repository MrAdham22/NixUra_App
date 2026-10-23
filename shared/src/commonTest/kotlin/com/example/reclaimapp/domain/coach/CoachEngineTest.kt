package com.example.reclaimapp.domain.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CoachEngineTest {
    private val kb = HabitKnowledgeBase()
    private val engine = CoachEngine(kb)

    private fun snapshot() = CoachSnapshot(
        userName = "Adham",
        badHabits = listOf(
            BadHabitSnapshot(1, "Smoking", 3.days, 12.days, 2),
            BadHabitSnapshot(2, "Late night scrolling", 20.days, 20.days, 0),
        ),
        goodHabits = listOf(GoodHabitSnapshot("Drink water", 3, 8, 4)),
        nextEvent = EventSnapshot("Half marathon", 10.days),
    )

    @Test
    fun matchesCategoriesByKeyword() {
        assertEquals("nicotine", kb.match("Smoking").id)
        assertEquals("nicotine", kb.match("تدخين").id)
        assertEquals("sugar", kb.match("Sugary drinks").id)
        assertEquals("caffeine", kb.match("Energy drink").id)
        assertEquals("alcohol", kb.match("Drinking").id)
        assertEquals("screens", kb.match("Late night scrolling").id)
        assertEquals("generic", kb.match("Skipping breakfast").id)
        assertEquals("generic", kb.match("  ").id)
    }

    @Test
    fun betDoesNotMatchBetterOrBetween() {
        assertEquals("generic", kb.match("Be better between meals").id)
        assertEquals("spending", kb.match("Betting").id)
    }

    @Test
    fun alcoholAndGamblingCarrySafetyNotes() {
        assertNotNull(kb.match("alcohol").safetyNote)
        assertNotNull(kb.match("gambling").safetyNote)
        assertNull(kb.match("sugar").safetyNote)
    }

    @Test
    fun milestonesAreSortedAndNonEmptyForEveryCategory() {
        (kb.categories + kb.generic).forEach { c ->
            assertTrue(c.milestones.isNotEmpty(), c.id)
            assertEquals(c.milestones.map { it.after }.sorted(), c.milestones.map { it.after }, c.id)
            assertTrue(c.cravingTips.isNotEmpty(), c.id)
        }
    }

    @Test
    fun insightTracksAchievedAndNextMilestone() {
        val early = engine.insightFor("Smoking", 30.minutes)
        assertEquals(1, early.achieved.size)
        assertEquals(12.hours, early.next?.after)
        assertTrue(early.progressToNext in 0f..1f)

        val none = engine.insightFor("Smoking", 5.minutes)
        assertEquals(0, none.achieved.size)

        val done = engine.insightFor("Smoking", 400.days)
        assertNull(done.next)
        assertEquals(1f, done.progressToNext)
    }

    @Test
    fun motivationIsDeterministicPerSeed() {
        assertEquals(engine.motivation("x", 2.days, 5), engine.motivation("x", 2.days, 5))
        assertTrue(engine.motivation("x", 40.days, 1).isNotBlank())
    }

    @Test
    fun cravingReplyMentionsHabitAndBreathing() {
        val reply = engine.reply("I have a craving for a cigarette", snapshot(), 1)
        assertTrue(reply.contains("Smoking"))
        assertTrue(reply.contains("10-minute"))
    }

    @Test
    fun relapseReplyIsKindAndPointsToRecordRelapse() {
        val reply = engine.reply("I slipped up today", snapshot(), 0)
        assertTrue(reply.contains("not the end"))
        assertTrue(reply.contains("Record relapse"))
    }

    @Test
    fun crisisMessagesGetSupportNotHabitAdvice() {
        val reply = engine.reply("I want to die", snapshot(), 0)
        assertTrue(reply.contains("emergency"))
        assertTrue(!reply.contains("Delay"))
    }

    @Test
    fun alcoholCravingIncludesSafetyWarning() {
        val s = CoachSnapshot(badHabits = listOf(BadHabitSnapshot(1, "Alcohol", 1.days, 1.days, 0)))
        assertTrue(engine.reply("I crave a drink", s, 0).contains("withdrawal can be dangerous"))
    }

    @Test
    fun shortGreetingWordsDoNotMatchInsideOtherWords() {
        val reply = engine.reply("this is history", snapshot(), 0)
        assertTrue(reply.contains("I can help with"))
        assertTrue(engine.reply("hi", snapshot(), 0).contains("NixUra coach"))
    }

    @Test
    fun progressReplyListsHabits() {
        val reply = engine.reply("How am I doing?", snapshot(), 0)
        assertTrue(reply.contains("Smoking"))
        assertTrue(reply.contains("Drink water"))
    }

    @Test
    fun dailyCheckInFocusesOnMostVulnerableHabit() {
        val note = engine.dailyCheckIn(snapshot(), 2)
        assertTrue(note.title.contains("Adham"))
        assertTrue(note.body.startsWith("Smoking"))
    }

    @Test
    fun durationTextUsesLargestUnit() {
        assertEquals("1 day", durationText(1.days + 5.hours))
        assertEquals("5 hours", durationText(5.hours + 30.minutes))
        assertEquals("12 minutes", durationText(12.minutes))
        assertEquals("0 minutes", durationText((-3).minutes))
    }

    @Test
    fun suggestionsAdaptToHabits() {
        assertTrue(engine.suggestions(snapshot()).contains("I have a craving"))
        assertTrue(!engine.suggestions(CoachSnapshot()).contains("I have a craving"))
    }
}
