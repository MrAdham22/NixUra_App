package com.example.reclaimapp.domain.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class HabitKnowledgeBaseTest {

    private fun category(id: String, label: String, vararg keywords: String) = HabitCategory(
        id = id,
        label = label,
        keywords = keywords.toList(),
        whyItMatters = "why $id",
        milestones = listOf(BodyMilestone(1.days, "m $id", "d $id")),
        cravingTips = listOf("tip $id"),
    )

    private val legacy = listOf(
        category("alpha", "Alpha", "alphabet"),
        category("beta", "Beta", "betamax"),
    )

    @Test
    fun defaultKnowledgeBaseKeepsTheBuiltInCategories() {
        val kb = HabitKnowledgeBase()
        val ids = kb.categories.map { it.id }
        listOf("nicotine", "caffeine", "sugar", "alcohol", "screens", "junkfood", "nails", "sleep", "procrastination", "spending")
            .forEach { assertTrue(it in ids, it) }
        assertEquals(ids.size, ids.toSet().size, "ids must be unique")
    }

    @Test
    fun laterCatalogOverridesEarlierOneWithTheSameId() {
        val override = category("beta", "Beta v2", "betamax", "videotape")
        val kb = HabitKnowledgeBase(legacy, emptyList(), listOf(override))
        assertEquals(2, kb.categories.size)
        assertEquals("Beta v2", kb.categories.first { it.id == "beta" }.label)
        assertSame(override, kb.categories.first { it.id == "beta" })
    }

    @Test
    fun overrideKeepsTheOriginalPosition() {
        val override = category("alpha", "Alpha v2", "alphabet")
        val kb = HabitKnowledgeBase(legacy, listOf(override), emptyList())
        assertEquals(listOf("alpha", "beta"), kb.categories.map { it.id })
        assertEquals("Alpha v2", kb.categories[0].label)
    }

    @Test
    fun newIdsAreAppendedInCatalogOrder() {
        val sub = category("gamma", "Gamma", "gammaray")
        val beh = category("delta", "Delta", "deltaforce")
        val kb = HabitKnowledgeBase(legacy, listOf(sub), listOf(beh))
        assertEquals(listOf("alpha", "beta", "gamma", "delta"), kb.categories.map { it.id })
    }

    @Test
    fun behaviorWinsOverSubstanceWhichWinsOverBuiltIn() {
        val sub = category("alpha", "Alpha substance", "alphabet")
        val beh = category("alpha", "Alpha behavior", "alphabet")
        val kb = HabitKnowledgeBase(legacy, listOf(sub), listOf(beh))
        assertEquals("Alpha behavior", kb.categories.first { it.id == "alpha" }.label)
        assertEquals(2, kb.categories.size)
    }

    @Test
    fun aCatalogEntryNamedGenericIsIgnored() {
        val kb = HabitKnowledgeBase(legacy, listOf(category("generic", "Imposter", "imposter")), emptyList())
        assertTrue(kb.categories.none { it.id == "generic" })
        assertEquals("generic", kb.match("imposter").id)
    }

    @Test
    fun mergedCatalogsAreMatchedWithoutHardCodedIds() {
        val sub = category("tramadolx", "Tramadol-like", "tramadol", "painkillers")
        val beh = category("fidgeting", "Fidgeting", "fidgeting")
        val kb = HabitKnowledgeBase(legacy, listOf(sub), listOf(beh))
        assertEquals("tramadolx", kb.match("tramdol").id)
        assertEquals("tramadolx", kb.match("I want to stop painkillers").id)
        assertEquals("fidgeting", kb.match("fidgetng").id)
        assertEquals("alpha", kb.match("alphabet").id)
        assertEquals("generic", kb.match("nothing here").id)
    }

    @Test
    fun overriddenKeywordsReplaceTheOldOnes() {
        val override = category("alpha", "Alpha v2", "omega")
        val kb = HabitKnowledgeBase(legacy, listOf(override), emptyList())
        assertEquals("alpha", kb.match("omega").id)
        assertEquals("generic", kb.match("alphabet").id)
    }

    @Test
    fun matchDetailedExposesConfidence() {
        val kb = HabitKnowledgeBase(legacy, emptyList(), emptyList())
        val exact = assertNotNull(kb.matchDetailed("alphabet"))
        assertEquals(1f, exact.confidence)
        assertTrue(exact.exact)
    }
}
