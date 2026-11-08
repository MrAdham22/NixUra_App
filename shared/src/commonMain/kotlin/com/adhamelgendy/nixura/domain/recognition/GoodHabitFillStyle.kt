package com.adhamelgendy.nixura.domain.recognition

import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase

/**
 * How a good-habit card shows today's progress. [WATER] habits fill with rising water; everything else uses the plain
 * green fill. New measurable kinds (steps, pages...) get their own value here and reuse the same liquid visual.
 */
enum class GoodHabitFillStyle { WATER, PLAIN }

/**
 * Picks the fill style from the habit's free-typed name with the same recognizer the forms use, so typos, synonyms
 * and Arabic ("drink watr", "hydration", "شرب الماء") work too. Stateless and per-keystroke fast, so safe from any thread.
 */
class GoodHabitFillStyleDetector(private val knowledge: HabitKnowledgeBase) {

    fun styleFor(name: String): GoodHabitFillStyle {
        val goodId = knowledge.outcomeFor(name.trim()).good?.id
        return if (goodId in WATER_IDS) GoodHabitFillStyle.WATER else GoodHabitFillStyle.PLAIN
    }

    private companion object {
        val WATER_IDS = setOf("water", "hydrate_lemon")
    }
}
