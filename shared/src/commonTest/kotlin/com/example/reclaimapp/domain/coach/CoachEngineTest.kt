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
        // "betting" belongs to the gambling entry once the behaviour catalog is merged (legacy: spending).
        assertTrue(kb.match("Betting").id in setOf("spending", "gambling"))
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

    @Test
    fun pickedHabitIsUsedForCravingAndBodyReplies() {
        val picked = snapshot().copy(focusHabitId = 2)
        assertTrue(engine.reply("what's happening in my body?", picked, 0).contains("Late night scrolling"))
        // Without a pick the newest habit (shortest streak) is used, as before.
        assertTrue(engine.reply("what's happening in my body?", snapshot(), 0).contains("Smoking"))
    }

    @Test
    fun mentionedHabitBeatsPickedHabit() {
        val picked = snapshot().copy(focusHabitId = 2)
        assertTrue(engine.reply("body effects of smoking?", picked, 0).contains("Smoking"))
    }

    @Test
    fun pickedHabitGetsOverviewForOpenChat() {
        val picked = snapshot().copy(focusHabitId = 1)
        val reply = engine.reply("ok let's start", picked, 0)
        assertTrue(reply.contains("let's talk about Smoking"))
        assertTrue(engine.reply("ok let's start", snapshot(), 0).contains("I can help with"))
    }

    // ---------- "What happens if I stop for ..." ----------

    private fun single() = CoachSnapshot(badHabits = listOf(BadHabitSnapshot(1, "Smoking", 3.days, 12.days, 2)))

    @Test
    fun whatHappensWithADurationUsesThatMoment() {
        val reply = engine.reply("what happens if I stop smoking for 6 hours", snapshot(), 0)
        assertTrue(reply.contains("6 hours"), reply)
        assertTrue(reply.contains("Timelines vary"), reply)
        assertTrue(!reply.contains("which habit"), reply)
    }

    @Test
    fun whatHappensAnswersForTheSingleActiveHabit() {
        val reply = engine.reply("what will I feel after 3 days without it?", single(), 0)
        assertTrue(reply.contains("3 days"), reply)
        assertTrue(reply.contains("Smoking"), reply)
    }

    @Test
    fun whatHappensWithoutADurationSummarisesTheNextMilestones() {
        val picked = snapshot().copy(focusHabitId = 1)
        val reply = engine.reply("how long until cravings stop", picked, 0)
        assertTrue(reply.contains("coming up"), reply)
        assertTrue(reply.contains("Next:"), reply)
        assertTrue(reply.contains("Tell me a time"), reply)
    }

    @Test
    fun whatHappensWithSeveralHabitsAndNoHintAsksWhichOne() {
        val reply = engine.reply("what happens if I stop for 2 days", snapshot(), 0)
        assertTrue(reply.contains("which habit"), reply)
        assertTrue(reply.contains("Smoking"), reply)
        assertTrue(reply.contains("Late night scrolling"), reply)
    }

    @Test
    fun whatHappensUsesThePickedHabitWhenTheMessageHasNone() {
        val picked = snapshot().copy(focusHabitId = 2)
        val reply = engine.reply("what happens if I stop for 2 days", picked, 0)
        assertTrue(!reply.contains("which habit"), reply)
        assertTrue(reply.contains("Late night scrolling"), reply)
    }

    @Test
    fun whatHappensToleratesTyposInTheQuestionAndTheHabit() {
        val reply = engine.reply("what hapens if i stop smokng for 6 hours", snapshot(), 0)
        assertTrue(reply.contains("6 hours"), reply)
        assertTrue(reply.contains("Smoking"), reply)
        assertTrue(!reply.contains("which habit"), reply)
    }

    @Test
    fun whatHappensWorksInArabic() {
        val reply = engine.reply("ماذا يحدث لو بطلت يومين", single(), 0)
        assertTrue(reply.contains("2 days"), reply)
    }

    @Test
    fun whatHappensFollowUpReusesThePreviousHabit() {
        val s = snapshot().copy(recentUserTexts = listOf("what happens if I stop smoking for 6 hours"))
        val reply = engine.reply("and after a week?", s, 0)
        assertTrue(reply.contains("7 days"), reply)
        assertTrue(reply.contains("Smoking"), reply)
        assertTrue(!reply.contains("which habit"), reply)
    }

    @Test
    fun whatHappensForAnUnknownHabitIsAGeneralTimeline() {
        val s = CoachSnapshot(badHabits = listOf(BadHabitSnapshot(1, "qwzx plorf", 1.days, 1.days, 0)))
        val reply = engine.reply("what happens if I stop for 2 days", s, 0)
        assertTrue(reply.contains("general timeline"), reply)
    }

    @Test
    fun whatHappensWithNoHabitsAtAllStillAnswers() {
        val reply = engine.reply("what happens if I stop for 3 days", CoachSnapshot(), 0)
        assertTrue(reply.contains("3 days"), reply)
        assertTrue(reply.contains("general timeline"), reply)
    }

    @Test
    fun whatHappensIncludesTheSafetyNoteInFull() {
        val s = CoachSnapshot(badHabits = listOf(BadHabitSnapshot(1, "Alcohol", 1.days, 1.days, 0)))
        val reply = engine.reply("what happens if I stop for 2 days", s, 0)
        assertTrue(reply.contains("Important:"), reply)
        assertTrue(reply.contains("withdrawal can be dangerous"), reply)
    }

    @Test
    fun plainBodyQuestionsStillGetTheBodyReply() {
        val reply = engine.reply("what's happening in my body?", snapshot(), 0)
        assertTrue(reply.contains("here is what is happening with"), reply)
    }

    @Test
    fun sensitiveHabitsAreShownByTheirLabelNotTheTypedWords() {
        val sensitive = HabitCategory(
            id = "explicitcontent",
            label = "Explicit content",
            keywords = listOf("xyzword"),
            whyItMatters = "why",
            milestones = listOf(BodyMilestone(1.days, "A day", "detail")),
            cravingTips = listOf("tip"),
        )
        val local = CoachEngine(HabitKnowledgeBase(listOf(sensitive), emptyList(), emptyList()))
        val s = CoachSnapshot(badHabits = listOf(BadHabitSnapshot(1, "xyzword", 2.days, 2.days, 0)))
        val reply = local.reply("what happens after 2 days of xyzword", s, 0)
        assertTrue(reply.contains("Explicit content"), reply)
        assertTrue(!reply.contains("xyzword"), reply)
        assertTrue(!local.reply("I have a craving", s, 0).contains("xyzword"))
        assertEquals("Explicit content", local.displayName("xyzword"))
        assertEquals("Smoking", engine.displayName("Smoking"))
    }

    @Test
    fun crisisRepliesStillWinOverEverythingElse() {
        listOf(
            "I want to kill myself",
            "what happens if I end my life in 2 days",
            "i keep thinking about suicde",
            "عايز اموت",
            "I want to die and stop for 3 days",
        ).forEach {
            val reply = engine.reply(it, snapshot(), 0)
            assertTrue(reply.contains("emergency"), "$it -> $reply")
            assertTrue(!reply.contains("Timelines vary"), it)
        }
    }

    @Test
    fun intentMatchingToleratesTypos() {
        assertTrue(engine.reply("I just relasped", snapshot(), 0).contains("not the end"))
        assertTrue(engine.reply("thnaks", snapshot(), 0).let { "welcome" in it || "Anytime" in it || "Glad" in it })
        assertTrue(engine.reply("how am i doing", snapshot(), 0).contains("Smoking"))
    }

    @Test
    fun smallTalkGetsAnAnswer() {
        assertTrue(engine.reply("how are you", snapshot(), 0).contains("how are you doing today"))
        assertTrue(engine.reply("what can you do", snapshot(), 0).contains("offline"))
        assertTrue(engine.reply("bye", snapshot(), 0).startsWith("Take care"))
    }

    @Test
    fun thanksRepliesVaryWithTheSeed() {
        val replies = (0L..2L).map { engine.reply("thanks", snapshot(), it) }.toSet()
        assertEquals(3, replies.size)
    }

    @Test
    fun explainStopIsAvailableOnTheEngine() {
        val a = engine.explainStop("Smoking", 6.hours)
        assertTrue(!a.isGenericProfile)
        assertTrue(engine.explainStop("qwzx plorf", 6.hours).isGenericProfile)
    }
}
