package com.adhamelgendy.nixura.domain.theme

import kotlin.math.ceil
import kotlin.math.roundToInt

/** How a water goal is shown. Only the display changes: the goal itself is always stored in millilitres. */
enum class WaterUnit(val storageKey: String) {
    LITERS("liters"),
    CUPS("cups"),
    ;

    companion object {
        fun fromStorageKey(key: String?): WaterUnit? = key?.let { k -> entries.firstOrNull { it.storageKey == k } }
    }
}

/**
 * The daily goal of a water habit. A day's log counts servings ("cups") of [servingMl], so
 * `count × servingMl` is what was drunk, and the habit's daily target is [servingsPerDay] (that keeps cards, widgets,
 * scores and the calendar working unchanged). Changing the cup size later re-reads every logged day with the new size.
 */
data class WaterGoal(
    val goalMl: Int,
    val servingMl: Int = DEFAULT_SERVING_ML,
    val unit: WaterUnit = WaterUnit.CUPS,
) {
    init {
        require(goalMl in MIN_GOAL_ML..MAX_GOAL_ML) { "Water goal must be between $MIN_GOAL_ML and $MAX_GOAL_ML ml" }
        require(servingMl in MIN_SERVING_ML..MAX_SERVING_ML) { "Cup size must be between $MIN_SERVING_ML and $MAX_SERVING_ML ml" }
    }

    /** Servings needed to reach the goal (rounded up, so the goal is never under-counted). */
    val servingsPerDay: Int get() = ceil(goalMl.toDouble() / servingMl).toInt().coerceAtLeast(1)

    /** Millilitres drunk on a day with [count] servings. */
    fun drunkMl(count: Int): Int = count.coerceAtLeast(0) * servingMl

    /** 0..1 share of the goal reached with [count] servings; the calendar's colour intensity and the fill level. */
    fun progress(count: Int): Float = (drunkMl(count).toFloat() / goalMl).coerceIn(0f, 1f)

    /** The goal in the chosen unit: litres (one decimal) or whole cups. */
    fun goalInUnit(): Double = amountInUnit(goalMl)

    /** A day's amount in the chosen unit. */
    fun drunkInUnit(count: Int): Double = amountInUnit(drunkMl(count))

    private fun amountInUnit(ml: Int): Double = when (unit) {
        WaterUnit.LITERS -> (ml / 100.0).roundToInt() / 10.0
        WaterUnit.CUPS -> ml.toDouble() / servingMl
    }

    /** Same goal, shown in another unit. Switching units never changes the goal itself. */
    fun withUnit(newUnit: WaterUnit): WaterGoal = copy(unit = newUnit)

    companion object {
        const val DEFAULT_SERVING_ML = 250
        const val DEFAULT_GOAL_ML = 2_000
        const val MIN_GOAL_ML = 250
        const val MAX_GOAL_ML = 6_000
        const val MIN_SERVING_ML = 100
        const val MAX_SERVING_ML = 1_000

        /** A goal set in litres, e.g. 2.0 → 2000 ml, clamped to the allowed range. */
        fun fromLiters(liters: Double, servingMl: Int = DEFAULT_SERVING_ML): WaterGoal =
            WaterGoal((liters * 1000).roundToInt().coerceIn(MIN_GOAL_ML, MAX_GOAL_ML), servingMl, WaterUnit.LITERS)

        /** A goal set in cups, e.g. 8 cups of 250 ml → 2000 ml, clamped to the allowed range. */
        fun fromCups(cups: Int, servingMl: Int = DEFAULT_SERVING_ML): WaterGoal =
            WaterGoal((cups * servingMl).coerceIn(MIN_GOAL_ML, MAX_GOAL_ML), servingMl, WaterUnit.CUPS)

        /**
         * The goal of a water habit saved before goals existed: its "N times a day" becomes N cups of 250 ml, so
         * nothing the user logged changes meaning.
         */
        fun defaultFor(dailyTarget: Int): WaterGoal = fromCups(dailyTarget.coerceAtLeast(1))
    }
}
