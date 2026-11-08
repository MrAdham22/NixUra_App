package com.adhamelgendy.nixura.domain.money

import com.adhamelgendy.nixura.domain.recognition.HabitRecognizer

/**
 * How a countable, purchasable habit is bought and counted. [units] are the choices offered in the form (first =
 * default); [defaultPerPack] is pre-filled (20 cigarettes in a pack, 1 can). [packs] = the user may also count per pack.
 */
data class CountableProfile(
    val categoryId: String,
    val units: List<CostUnit>,
    val defaultPerPack: Int,
    val packs: Boolean,
)

/**
 * Which quit habits can be counted and priced. Only substances and drinks bought by the unit qualify; habits such as
 * procrastination or social media get no money tracker. Recognition reuses [HabitRecognizer], so "smokng", "cola"
 * or "سجائر" are found too.
 */
class CountableHabits(private val recognizer: HabitRecognizer) {

    fun profileFor(habitName: String): CountableProfile? {
        val id = recognizer.recognize(habitName).category?.id ?: return null
        return PROFILES[id]
    }

    companion object {
        private val PROFILES: Map<String, CountableProfile> = listOf(
            CountableProfile("nicotine", listOf(CostUnit.CIGARETTE), defaultPerPack = 20, packs = true),
            CountableProfile("vaping", listOf(CostUnit.POD), defaultPerPack = 1, packs = true),
            CountableProfile("smokeless", listOf(CostUnit.POUCH), defaultPerPack = 20, packs = true),
            CountableProfile("shisha", listOf(CostUnit.SESSION), defaultPerPack = 1, packs = false),
            CountableProfile("softdrinks", listOf(CostUnit.CAN, CostUnit.BOTTLE), defaultPerPack = 1, packs = false),
            CountableProfile("energydrinks", listOf(CostUnit.CAN, CostUnit.BOTTLE), defaultPerPack = 1, packs = false),
            CountableProfile("sugarindrinks", listOf(CostUnit.BOTTLE, CostUnit.CAN, CostUnit.CUP), defaultPerPack = 1, packs = false),
            CountableProfile("caffeine", listOf(CostUnit.CUP, CostUnit.CAN), defaultPerPack = 1, packs = false),
            CountableProfile("tea", listOf(CostUnit.CUP), defaultPerPack = 1, packs = false),
            CountableProfile("alcohol", listOf(CostUnit.DRINK, CostUnit.CAN, CostUnit.BOTTLE), defaultPerPack = 1, packs = false),
            CountableProfile("junkfood", listOf(CostUnit.MEAL), defaultPerPack = 1, packs = false),
        ).associateBy { it.categoryId }
    }
}
