package com.example.reclaimapp.presentation.habits

import com.example.reclaimapp.domain.money.CostUnit
import com.example.reclaimapp.domain.money.CountableProfile
import com.example.reclaimapp.domain.money.Currencies
import com.example.reclaimapp.domain.money.HabitCost

/**
 * The money section of the quit-habit form, as typed. [toCost] turns it into a [HabitCost] (or `null` when it is
 * off); [isComplete] says whether the numbers are usable, so the form can explain what is missing.
 */
data class CostInput(
    val enabled: Boolean = false,
    val item: String = "",
    val unit: CostUnit = CostUnit.CIGARETTE,
    val perDay: String = "",
    val inPacks: Boolean = false,
    val perPack: String = "",
    val price: String = "",
    val currency: String = Currencies.DEFAULT,
) {
    private fun perDayValue(): Double? = perDay.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it <= 1_000 }
    private fun perPackValue(): Int? = perPack.trim().toIntOrNull()?.takeIf { it in 1..1_000 }

    val isComplete: Boolean get() = toCostOrNull() != null

    fun toCostOrNull(): HabitCost? {
        val day = perDayValue() ?: return null
        val pack = perPackValue() ?: return null
        val priceMinor = Currencies.parseMinor(price, currency) ?: return null
        return HabitCost(
            item = item.trim().take(MAX_ITEM),
            unit = unit,
            unitsPerDay = if (inPacks) day * pack else day,
            pricePerPackMinor = priceMinor,
            unitsPerPack = pack,
            currency = currency,
            perDayInPacks = inPacks,
        )
    }

    /** What to save: the tracker when it is on and complete, `null` when it is off. */
    fun toCost(): HabitCost? = if (enabled) toCostOrNull() else null

    /** Keeps the typed values but follows a different habit kind (its units and pack size). */
    fun adaptedTo(profile: CountableProfile): CostInput = copy(
        unit = if (unit in profile.units) unit else profile.units.first(),
        inPacks = inPacks && profile.packs,
        perPack = perPack.ifBlank { profile.defaultPerPack.toString() },
    )

    companion object {
        private const val MAX_ITEM = 60

        fun from(cost: HabitCost?, profile: CountableProfile?, defaultCurrency: String): CostInput {
            if (cost == null) {
                // Pack size stays blank until the habit kind is known, so adaptedTo can fill the right default (20 cigarettes).
                return CostInput(
                    unit = profile?.units?.firstOrNull() ?: CostUnit.CIGARETTE,
                    perPack = profile?.defaultPerPack?.toString().orEmpty(),
                    currency = defaultCurrency,
                )
            }
            val perDay = if (cost.perDayInPacks) cost.unitsPerDay / cost.unitsPerPack else cost.unitsPerDay
            return CostInput(
                enabled = true,
                item = cost.item,
                unit = cost.unit,
                perDay = trimNumber(perDay),
                inPacks = cost.perDayInPacks,
                perPack = cost.unitsPerPack.toString(),
                price = Currencies.toInput(cost.pricePerPackMinor, cost.currency),
                currency = cost.currency,
            )
        }

        private fun trimNumber(v: Double): String =
            if (v == kotlin.math.floor(v)) v.toLong().toString() else ((v * 100).toLong() / 100.0).toString()
    }
}
