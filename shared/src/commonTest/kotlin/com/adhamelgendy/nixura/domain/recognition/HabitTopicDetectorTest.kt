package com.adhamelgendy.nixura.domain.recognition

import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HabitTopicDetectorTest {

    private val knowledge = HabitKnowledgeBase()
    private val detector = HabitTopicDetector(knowledge)

    @Test fun quitHabitsGetTheGlyphOfTheirCategory() {
        mapOf(
            "smoking" to HabitTopic.SMOKING,
            "Vaping" to HabitTopic.SMOKING,
            "تدخين" to HabitTopic.SMOKING,
            "Junk Food" to HabitTopic.JUNK_FOOD,
            "coffee" to HabitTopic.HOT_DRINK,
            "alcohol" to HabitTopic.ALCOHOL,
            "instagram" to HabitTopic.PHONE,
            "gaming" to HabitTopic.GAMING,
            "gambling" to HabitTopic.GAMBLING,
            "nail biting" to HabitTopic.HANDS,
        ).forEach { (name, topic) -> assertEquals(topic, detector.quitTopic(name), name) }
    }

    @Test fun buildHabitsGetTheGlyphOfTheirKind() {
        mapOf(
            "Drink water" to HabitTopic.WATER,
            "pray" to HabitTopic.PRAYER,
            "صلاة" to HabitTopic.PRAYER,
            "Workout" to HabitTopic.FITNESS,
            "read" to HabitTopic.LEARNING,
            "meditation" to HabitTopic.CALM,
            "walking" to HabitTopic.MOVEMENT,
        ).forEach { (name, topic) -> assertEquals(topic, detector.buildTopic(name), name) }
    }

    @Test fun unknownNamesFallBackToTheDefaultGlyphs() {
        assertEquals(HabitTopic.OTHER_QUIT, detector.quitTopic("qwzx"))
        assertEquals(HabitTopic.OTHER_BUILD, detector.buildTopic("qwzx"))
        assertEquals(HabitTopic.OTHER_QUIT, detector.quitTopic(" "))
    }

    @Test fun everyCatalogCategoryHasATopic() {
        knowledge.categories.filter { it.id != "generic" }.forEach {
            assertNotNull(HabitTopicDetector.topicOfQuitCategory(it.id), "no topic for quit category ${it.id}")
        }
    }
}
