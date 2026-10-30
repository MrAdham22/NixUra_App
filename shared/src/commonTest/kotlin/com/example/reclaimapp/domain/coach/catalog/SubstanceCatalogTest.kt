package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.HabitCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Integrity checks for the data in [SubstanceCatalog]. Pure logic, no platform APIs. */
class SubstanceCatalogTest {
    private val categories = SubstanceCatalog.categories
    private val slug = Regex("^[a-z][a-z0-9]*$")
    private val arabic = Regex("[\\u0600-\\u06FF]")

    /** The ids that exist in the legacy knowledge base today. */
    private val legacyIds = setOf(
        "nicotine", "caffeine", "sugar", "alcohol", "screens", "junkfood", "nails", "sleep", "procrastination", "spending",
    )
    private val overridableLegacyIds = setOf("nicotine", "caffeine", "sugar", "alcohol", "junkfood")
    private val reservedForBehaviorCatalog = setOf("screens", "nails", "sleep", "procrastination", "spending")

    /** Substances where stopping abruptly can be risky. They must carry a safetyNote. */
    private val safetyRequiredIds = setOf("alcohol", "painkillers", "sleepingpills", "stimulants")

    private fun byId(id: String): HabitCategory = categories.first { it.id == id }
    private fun norm(s: String) = s.trim().lowercase()

    @Test
    fun catalogIsNotEmptyAndHasReasonableSize() {
        assertTrue(categories.size in 15..25, "expected 15..25 habits, got ${categories.size}")
    }

    @Test
    fun idsAreUniqueLowercaseSlugs() {
        val ids = categories.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate ids: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}")
        ids.forEach { assertTrue(slug.matches(it), "bad id '$it'") }
    }

    @Test
    fun idsDoNotCollideWithBehaviorCatalogReservedIds() {
        val clash = categories.map { it.id }.filter { it in reservedForBehaviorCatalog }
        assertTrue(clash.isEmpty(), "ids reserved for Agent 3: $clash")
        assertTrue("generic" !in categories.map { it.id })
    }

    @Test
    fun overriddenLegacyIdsExistInLegacyList() {
        val overridden = categories.map { it.id }.filter { it in legacyIds }.toSet()
        assertTrue(overridden.all { it in overridableLegacyIds }, "overrides a legacy id we do not own: $overridden")
        assertEquals(overridableLegacyIds, overridden, "every enriched legacy id should be re-declared")
    }

    @Test
    fun everyCategoryHasLabelAndWhyItMatters() {
        categories.forEach {
            assertTrue(it.label.isNotBlank(), "${it.id}: blank label")
            assertTrue(it.whyItMatters.isNotBlank(), "${it.id}: blank whyItMatters")
        }
    }

    @Test
    fun everyCategoryHasEnoughKeywords() {
        categories.forEach {
            assertTrue(it.keywords.size >= 8, "${it.id}: only ${it.keywords.size} keywords")
            it.keywords.forEach { k -> assertTrue(k.isNotBlank(), "${it.id}: blank keyword") }
        }
    }

    @Test
    fun noDuplicateKeywordsInsideACategory() {
        categories.forEach {
            val normalized = it.keywords.map(::norm)
            assertEquals(normalized.size, normalized.toSet().size, "${it.id}: duplicate keywords ${normalized.groupBy { k -> k }.filter { e -> e.value.size > 1 }.keys}")
        }
    }

    @Test
    fun noDangerouslyGenericSingleWordKeywords() {
        val denied = setOf("drink", "drinks", "game", "phone", "sweet", "eat", "food", "pill", "pills", "medicine", "drug", "drugs", "water", "sleep")
        categories.forEach {
            it.keywords.map(::norm).forEach { k -> assertTrue(k !in denied, "${it.id}: generic keyword '$k'") }
        }
    }

    @Test
    fun milestonesAreSufficientAndSortedAscending() {
        categories.forEach {
            assertTrue(it.milestones.size >= 6, "${it.id}: only ${it.milestones.size} milestones")
            it.milestones.forEach { m ->
                assertTrue(m.title.isNotBlank() && m.detail.isNotBlank(), "${it.id}: blank milestone text")
            }
            val afters = it.milestones.map { m -> m.after }
            assertEquals(afters.sorted(), afters, "${it.id}: milestones not sorted")
            assertEquals(afters.size, afters.toSet().size, "${it.id}: duplicate milestone thresholds")
        }
    }

    @Test
    fun denseEarlyTimelineStartsWithinFourHoursWhereMeaningful() {
        categories.forEach {
            val first = it.milestones.first().after
            assertTrue(first.inWholeMinutes <= 6 * 60, "${it.id}: first milestone too late ($first)")
        }
        // All substances that act within hours must start inside the first four hours.
        listOf("nicotine", "vaping", "caffeine", "alcohol", "cannabis").forEach {
            assertTrue(byId(it).milestones.first().after.inWholeHours <= 4, "$it: first milestone after 4 h")
        }
    }

    @Test
    fun cravingTipsAreThreeToFive() {
        categories.forEach {
            assertTrue(it.cravingTips.size in 3..5, "${it.id}: ${it.cravingTips.size} tips")
            it.cravingTips.forEach { t -> assertTrue(t.isNotBlank()) }
        }
    }

    @Test
    fun riskySubstancesHaveSafetyNote() {
        safetyRequiredIds.forEach {
            val note = byId(it).safetyNote
            assertNotNull(note, "$it needs a safetyNote")
            assertTrue(note.contains("doctor", ignoreCase = true), "$it: safety note should point to a doctor")
        }
        listOf("painkillers", "sleepingpills", "alcohol").forEach {
            assertTrue(byId(it).safetyNote!!.contains("suddenly", ignoreCase = true), "$it: must warn against stopping suddenly")
        }
    }

    @Test
    fun noDosingOrSourcingLanguageInRiskyCategories() {
        val banned = listOf("mg ", " mg", "taper schedule", "where to buy", "how to get", "how to take", "dose of")
        categories.filter { it.id in safetyRequiredIds || it.id == "cannabis" }.forEach { c ->
            val text = (c.whyItMatters + c.milestones.joinToString(" ") { it.title + " " + it.detail } +
                c.cravingTips.joinToString(" ") + (c.safetyNote ?: "")).lowercase()
            banned.forEach { b -> assertTrue(!text.contains(b), "${c.id}: contains '$b'") }
        }
    }

    @Test
    fun commonHabitsHaveArabicKeywords() {
        listOf("nicotine", "shisha", "caffeine", "tea", "softdrinks", "sugar", "junkfood", "alcohol", "cannabis", "energydrinks").forEach {
            assertTrue(byId(it).keywords.any { k -> arabic.containsMatchIn(k) }, "$it: no Arabic keyword")
        }
    }

    @Test
    fun mostCategoriesHaveArabicKeywords() {
        val withArabic = categories.count { c -> c.keywords.any { arabic.containsMatchIn(it) } }
        assertEquals(categories.size, withArabic)
    }

    @Test
    fun timelineTextUsesHedgedLanguage() {
        val hedges = listOf("typically", "many people", "about", "often", "usually", "common", "most people", "some people", "can ")
        categories.forEach { c ->
            val hedged = c.milestones.count { m -> hedges.any { h -> m.detail.lowercase().contains(h) } }
            assertTrue(hedged >= c.milestones.size / 2, "${c.id}: timeline sounds too certain")
        }
    }

    @Test
    fun labelsAreShortAndUserFacing() {
        categories.forEach { assertTrue(it.label.length in 3..40, "${it.id}: label '${it.label}'") }
    }
}
