package com.example.reclaimapp.domain.money

import kotlin.math.floor

/** Money not spent and units not consumed during a clean period. */
data class MoneySaved(val savedMinor: Long, val unitsAvoided: Long, val currency: String)

/** One clean streak and what it saved. */
data class StreakSaving(val startEpochMillis: Long, val endEpochMillis: Long, val savedMinor: Long, val isCurrent: Boolean)

/**
 * Money saved by not consuming: (units per day / units per pack × pack price) × clean time. It follows the streak
 * exactly like the counter does: a relapse starts it again from zero (the current streak is what is "saved now").
 */
object MoneySavedCalculator {
    private const val MILLIS_PER_DAY = 86_400_000.0

    /** What a normal day used to cost, in minor units. */
    fun dailyCostMinor(cost: HabitCost): Double = cost.unitsPerDay / cost.unitsPerPack * cost.pricePerPackMinor

    fun saved(cost: HabitCost, cleanMillis: Long): MoneySaved {
        val days = cleanMillis.coerceAtLeast(0) / MILLIS_PER_DAY
        return MoneySaved(
            savedMinor = floor(dailyCostMinor(cost) * days).toLong(),
            unitsAvoided = floor(cost.unitsPerDay * days).toLong(),
            currency = cost.currency,
        )
    }

    /**
     * Every clean streak from the tracking start to now, oldest first: each relapse ends one and starts the next. The
     * last one is the running streak. Empty when tracking never started.
     */
    fun streaks(cost: HabitCost, trackingStartEpochMillis: Long?, relapseEpochMillis: List<Long>, nowEpochMillis: Long): List<StreakSaving> {
        val start = trackingStartEpochMillis ?: return emptyList()
        val bounds = buildList {
            add(start)
            relapseEpochMillis.filter { it in start..nowEpochMillis }.sorted().forEach { add(it) }
            add(nowEpochMillis.coerceAtLeast(start))
        }
        return bounds.zipWithNext().mapIndexed { i, (from, to) ->
            StreakSaving(from, to, saved(cost, to - from).savedMinor, isCurrent = i == bounds.size - 2)
        }
    }
}
