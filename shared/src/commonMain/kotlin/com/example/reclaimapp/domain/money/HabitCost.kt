package com.example.reclaimapp.domain.money

/** What one consumed unit of a countable habit is. */
enum class CostUnit { CIGARETTE, POD, POUCH, SESSION, CUP, CAN, BOTTLE, DRINK, MEAL }

/**
 * What a countable quit habit used to cost. Money is in minor units of [currency] (piastres, cents) so sums never
 * drift. One "pack" is what the user buys: a pack of [unitsPerPack] cigarettes, or one can ([unitsPerPack] = 1).
 *
 * @property item what exactly it was ("Marlboro Red", "Pepsi"); may be blank.
 * @property unitsPerDay average units per day (cigarettes, cans...), always stored per unit, never per pack.
 * @property perDayInPacks the user gave the daily amount in packs; only affects how the form shows it again.
 */
data class HabitCost(
    val item: String,
    val unit: CostUnit,
    val unitsPerDay: Double,
    val pricePerPackMinor: Long,
    val unitsPerPack: Int,
    val currency: String,
    val perDayInPacks: Boolean = false,
) {
    init {
        require(unitsPerDay > 0) { "Units per day must be above 0" }
        require(pricePerPackMinor >= 0) { "Price can't be negative" }
        require(unitsPerPack >= 1) { "A pack holds at least 1" }
    }
}
