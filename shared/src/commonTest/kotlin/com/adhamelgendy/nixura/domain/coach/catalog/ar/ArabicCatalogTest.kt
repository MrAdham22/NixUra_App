package com.adhamelgendy.nixura.domain.coach.catalog.ar

import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.coach.CoachSnapshot
import com.adhamelgendy.nixura.domain.coach.BadHabitSnapshot
import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ArabicCatalogTest {
    private val arabic = ContentLanguage { "ar-EG" }.also { it.update("system") }
    private val kbAr = HabitKnowledgeBase(arabic)
    private val kbEn = HabitKnowledgeBase()

    @Test
    fun everyCategoryHasAnArabicLabel() {
        val missing = (kbEn.categories.map { it.id } + kbEn.generic.id).filter { it !in ArabicCatalog.labels }
        if (missing.isNotEmpty()) fail("No Arabic label: $missing")
    }

    @Test
    fun everyCategoryIsFullyTranslated() {
        val missing = (kbEn.categories.map { it.id } + kbEn.generic.id).filter { it !in ArabicCatalog.texts }
        if (missing.isNotEmpty()) fail("No Arabic details: $missing")
    }

    @Test
    fun arabicTextsLineUpWithTheirEnglishEntries() {
        val byId = (kbEn.categories + kbEn.generic).associateBy { it.id }
        ArabicCatalog.texts.forEach { (id, text) ->
            val en = byId[id] ?: fail("Arabic text for an unknown category: $id")
            assertEquals(en.milestones.size, text.milestones.size, "milestones of $id")
            assertTrue(text.cravingTips.isNotEmpty(), "tips of $id")
            assertEquals(en.safetyNote != null, text.safetyNote != null, "safety note of $id")
        }
    }

    @Test
    fun matchingIsTheSameInBothLanguages() {
        listOf("smoking", "سجاير", "قهوة", "social media", "اشرب مياه", "nail biting").forEach {
            assertEquals(kbEn.match(it).id, kbAr.match(it).id, it)
        }
    }

    @Test
    fun arabicKeepsTheTimesAndSwapsTheText() {
        val en = kbEn.match("smoking")
        val ar = kbAr.match("smoking")
        assertEquals(en.milestones.map { it.after }, ar.milestones.map { it.after })
        assertEquals(ArabicCatalog.labels.getValue(en.id), ar.label)
    }

    @Test
    fun theOfflineCoachAnswersInArabic() {
        val engine = CoachEngine(kbAr, arabic)
        val snapshot = CoachSnapshot(userName = "سارة", badHabits = listOf(BadHabitSnapshot(1, "التدخين", 3.days + 2.hours, 3.days, 0)))
        val greeting = engine.greeting(snapshot, 1)
        assertTrue(greeting.startsWith("أهلًا يا سارة."), greeting)
        assertTrue("3 أيام" in greeting, greeting)
        assertTrue(engine.reply("عندي رغبة قوية", snapshot, 1).contains("مؤقتة"))
        assertTrue(engine.reply("تعثّرت", snapshot, 1).contains("التعثّر ليس النهاية"))
        assertTrue(engine.reply("ماذا يحدث لو توقفت لمدة يومين؟", snapshot, 1).contains("بعد نحو"))
        // Every suggestion chip leads somewhere other than the "I can help with" fallback.
        val fallback = engine.reply("؟؟", CoachSnapshot(), 1)
        engine.suggestions(snapshot).forEach { assertTrue(engine.reply(it, snapshot, 2) != fallback, it) }
    }

    @Test
    fun englishStaysEnglish() {
        val engine = CoachEngine(kbEn)
        assertTrue(engine.greeting(CoachSnapshot(userName = "Sam"), 1).startsWith("Hi Sam."))
    }
}
