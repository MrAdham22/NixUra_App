package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.HabitKnowledgeBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class BehaviorCatalogTest {

    private val categories = BehaviorCatalog.categories
    private val slug = Regex("^[a-z][a-z0-9]*(-[a-z0-9]+)*$")
    private val arabic = Regex("[\\u0600-\\u06FF]")

    /** Ids owned by Agent 2 (substances, food and drink). Must never appear here. */
    private val agent2Reserved = setOf("nicotine", "caffeine", "sugar", "alcohol", "junkfood")

    /** Legacy ids this catalog is allowed to override. */
    private val overridable = setOf("screens", "nails", "sleep", "procrastination", "spending")

    /** Single, very generic words that would cause false matches if used as a keyword on their own. */
    private val genericDenyList = setOf(
        "game", "games", "phone", "mobile", "sweet", "delay", "lazy", "screen", "social", "news", "sleep", "late",
        "time", "work", "eat", "drink", "talk", "online", "night", "habit", "stop", "quit", "bad", "body", "hand",
        "نوم", "موبايل", "تسويف الوقت", "شغل", "اكل", "شرب", "كلام", "نت", "ليل",
    )

    @Test
    fun catalogIsNotEmptyAndHasEnoughHabits() {
        assertTrue(categories.size >= 15, "expected at least 15 habits, got ${categories.size}")
    }

    @Test
    fun idsAreUniqueLowercaseSlugs() {
        val ids = categories.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate ids: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}")
        ids.forEach { assertTrue(slug.matches(it), "bad id: $it") }
    }

    @Test
    fun idsDoNotCollideWithAgent2OrGeneric() {
        categories.forEach {
            assertFalse(it.id in agent2Reserved, "id reserved for the substance catalog: ${it.id}")
            assertFalse(it.id == "generic", "generic is reserved")
        }
    }

    @Test
    fun overriddenLegacyIdsExistInLegacyList() {
        val legacy = HabitKnowledgeBase().categories.map { it.id }.toSet()
        val declared = categories.map { it.id }.toSet()
        // every legacy id we override must really be a legacy id; every allowed id we declare must exist there
        overridable.forEach { assertTrue(it in legacy, "$it is not a legacy id") }
        overridable.forEach { assertTrue(it in declared, "$it should be enriched by this catalog") }
    }

    @Test
    fun everyCategoryHasLabelWhyAndEnoughKeywords() {
        categories.forEach { c ->
            assertTrue(c.label.isNotBlank(), "${c.id}: blank label")
            assertTrue(c.whyItMatters.isNotBlank(), "${c.id}: blank whyItMatters")
            assertTrue(c.keywords.size >= 8, "${c.id}: only ${c.keywords.size} keywords")
            c.keywords.forEach { assertTrue(it.isNotBlank(), "${c.id}: blank keyword") }
        }
    }

    @Test
    fun noDuplicateKeywordsInsideOneCategory() {
        categories.forEach { c ->
            val normalised = c.keywords.map { it.trim().lowercase() }
            assertEquals(normalised.size, normalised.toSet().size, "${c.id}: duplicate keyword")
        }
    }

    @Test
    fun noKeywordIsClaimedByTwoCategories() {
        val owner = HashMap<String, String>()
        categories.forEach { c ->
            c.keywords.map { it.trim().lowercase() }.forEach { k ->
                val previous = owner.put(k, c.id)
                assertTrue(previous == null || previous == c.id, "keyword '$k' is in both $previous and ${c.id}")
            }
        }
    }

    @Test
    fun noGenericSingleWordKeywords() {
        categories.forEach { c ->
            c.keywords.forEach { k ->
                assertFalse(k.trim().lowercase() in genericDenyList, "${c.id}: keyword '$k' is too generic")
            }
        }
    }

    @Test
    fun milestonesAreSortedAscendingAndComplete() {
        categories.forEach { c ->
            assertTrue(c.milestones.size >= 6, "${c.id}: only ${c.milestones.size} milestones")
            val afters = c.milestones.map { it.after }
            assertEquals(afters.sorted(), afters, "${c.id}: milestones not sorted")
            assertEquals(afters.size, afters.toSet().size, "${c.id}: duplicate milestone times")
            c.milestones.forEach { m ->
                assertTrue(m.title.isNotBlank(), "${c.id}: blank milestone title")
                assertTrue(m.detail.isNotBlank(), "${c.id}: blank milestone detail")
                assertTrue(m.after.isPositive(), "${c.id}: non-positive milestone time")
            }
        }
    }

    @Test
    fun earlyTimelineIsDense() {
        categories.forEach { c ->
            val first = c.milestones.first().after
            assertTrue(first <= 4.hours, "${c.id}: first milestone should be within 4 hours, was $first")
            val within48h = c.milestones.count { it.after <= 48.hours }
            assertTrue(within48h >= 3, "${c.id}: only $within48h milestones in the first 48 hours")
        }
    }

    @Test
    fun cravingTipsAreThreeToFive() {
        categories.forEach { c ->
            assertTrue(c.cravingTips.size in 3..5, "${c.id}: ${c.cravingTips.size} craving tips")
            c.cravingTips.forEach { assertTrue(it.isNotBlank(), "${c.id}: blank craving tip") }
        }
    }

    @Test
    fun sensitiveHabitsHaveSafetyNotes() {
        val mustHave = listOf(
            "gambling", "spending", "explicitcontent", "overthinking", "negativeselftalk",
            "skinpicking", "hairpulling", "anger",
        )
        mustHave.forEach { id ->
            val c = categories.firstOrNull { it.id == id }
            assertNotNull(c, "missing category $id")
            assertTrue(!c.safetyNote.isNullOrBlank(), "$id needs a safety note")
        }
    }

    @Test
    fun safetyNotesPointToHelpAndNeverDiagnose() {
        categories.mapNotNull { c -> c.safetyNote?.let { c.id to it } }.forEach { (id, note) ->
            val lower = note.lowercase()
            assertTrue(
                listOf("doctor", "gp", "counsellor", "dentist", "therapist", "support service", "advice service").any { it in lower },
                "$id: safety note should point to a professional",
            )
            assertFalse("you have " in lower, "$id: safety note must not diagnose")
        }
    }

    @Test
    fun noShamingOrExplicitWordingInText() {
        val banned = listOf("pathetic", "weak-willed", "disgusting", "loser", "stupid person")
        categories.forEach { c ->
            val text = (listOf(c.label, c.whyItMatters) + c.cravingTips + c.milestones.flatMap { listOf(it.title, it.detail) } + listOfNotNull(c.safetyNote))
                .joinToString(" ").lowercase()
            banned.forEach { assertFalse(it in text, "${c.id}: contains '$it'") }
        }
    }

    @Test
    fun commonHabitsHaveArabicKeywords() {
        val common = listOf(
            "screens", "socialmedia", "shortvideo", "gaming", "bingewatching", "phonechecking", "sleep",
            "procrastination", "overthinking", "spending", "gambling", "nails", "anger",
        )
        common.forEach { id ->
            val c = categories.firstOrNull { it.id == id }
            assertNotNull(c, "missing category $id")
            assertTrue(c.keywords.count { arabic.containsMatchIn(it) } >= 3, "$id needs at least 3 Arabic keywords")
        }
    }

    @Test
    fun scopeCoversTheRequestedHabits() {
        val required = listOf(
            "socialmedia", "doomscrolling", "shortvideo", "gaming", "bingewatching", "phonechecking", "latenightphone",
            "sleep", "oversleeping", "procrastination", "overthinking", "negativeselftalk", "comparison", "spending",
            "gambling", "nails", "skinpicking", "hairpulling", "lipbiting", "knucklecracking", "teethgrinding",
            "swearing", "gossiping", "anger", "texting", "sitting", "explicitcontent", "screens",
        )
        val ids = categories.map { it.id }.toSet()
        required.forEach { assertTrue(it in ids, "missing $it") }
    }

    @Test
    fun gamingKeywordIsSpecificButUsable() {
        val gaming = categories.first { it.id == "gaming" }
        assertTrue("playing games" in gaming.keywords)
        assertFalse("game" in gaming.keywords)
    }
}
