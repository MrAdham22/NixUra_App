package com.adhamelgendy.nixura.presentation.goodhabits.form

import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.theme.WaterUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * What the water section of the Add/Edit Good Habit form holds. The goal is always millilitres: the unit only changes
 * how it is shown and how the goal stepper moves (250 ml in litres, one cup in cups), so switching units never changes
 * the goal. Pure Kotlin, unit-tested.
 */
data class WaterGoalForm(
    val goalMl: Int = WaterGoal.DEFAULT_GOAL_ML,
    val servingMl: Int = WaterGoal.DEFAULT_SERVING_ML,
    val unit: WaterUnit = WaterUnit.CUPS,
) {
    /** The goal to save; values are kept inside [WaterGoal]'s limits. */
    fun toGoal(): WaterGoal = WaterGoal(
        goalMl.coerceIn(WaterGoal.MIN_GOAL_ML, WaterGoal.MAX_GOAL_ML),
        servingMl.coerceIn(WaterGoal.MIN_SERVING_ML, WaterGoal.MAX_SERVING_ML),
        unit,
    )

    fun withUnit(newUnit: WaterUnit): WaterGoalForm = copy(unit = newUnit)

    /** The goal in cups of the current size (may be fractional, e.g. 2 L in 300 ml cups = 6.67). */
    val cups: Double get() = goalMl.toDouble() / servingMl

    val canDecreaseGoal: Boolean get() = goalMl > WaterGoal.MIN_GOAL_ML
    val canIncreaseGoal: Boolean get() = goalMl < WaterGoal.MAX_GOAL_ML

    /** One step up or down: 250 ml in litres, one whole cup in cups. Lands on whole steps, within the limits. */
    fun stepGoal(up: Boolean): WaterGoalForm {
        val next = when (unit) {
            WaterUnit.LITERS -> {
                val steps = goalMl.toDouble() / LITER_STEP_ML
                val n = if (up) floor(steps + EPS) + 1 else ceil(steps - EPS) - 1
                (n * LITER_STEP_ML).roundToInt()
            }
            WaterUnit.CUPS -> {
                val n = if (up) floor(cups + EPS) + 1 else ceil(cups - EPS) - 1
                (n * servingMl).roundToInt()
            }
        }
        return copy(goalMl = next.coerceIn(WaterGoal.MIN_GOAL_ML, WaterGoal.MAX_GOAL_ML))
    }

    val canDecreaseServing: Boolean get() = servingMl > WaterGoal.MIN_SERVING_ML
    val canIncreaseServing: Boolean get() = servingMl < WaterGoal.MAX_SERVING_ML

    /** Cup size ±50 ml. The goal in ml stays as it is; only the number of cups it takes changes. */
    fun stepServing(up: Boolean): WaterGoalForm {
        val next = if (up) servingMl + SERVING_STEP_ML else servingMl - SERVING_STEP_ML
        return copy(servingMl = next.coerceIn(WaterGoal.MIN_SERVING_ML, WaterGoal.MAX_SERVING_ML))
    }

    /** Cups a day the habit will count (its daily target). */
    val servingsPerDay: Int get() = toGoal().servingsPerDay

    /** The goal as shown in litres: "2", "2.5", "2.25". */
    fun litersText(): String = formatLiters(goalMl)

    /** The goal as shown in cups: "8", or one decimal when it is not a whole number of cups ("6.7"). */
    fun cupsText(): String {
        val tenths = (cups * 10).roundToInt()
        return if (tenths % 10 == 0) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
    }

    companion object {
        const val LITER_STEP_ML = 250
        const val SERVING_STEP_ML = 50
        private const val EPS = 1e-6

        /** The form for a habit's saved goal, or the defaults (2 L, 250 ml cups) for a new one. */
        fun from(goal: WaterGoal?): WaterGoalForm =
            goal?.let { WaterGoalForm(it.goalMl, it.servingMl, it.unit) } ?: WaterGoalForm()

        /** Millilitres as litres without trailing zeros: 2000 → "2", 2500 → "2.5", 2250 → "2.25". */
        fun formatLiters(ml: Int): String {
            val whole = ml / 1000
            val rest = ml % 1000
            if (rest == 0) return whole.toString()
            val decimals = rest.toString().padStart(3, '0').trimEnd('0')
            return "$whole.$decimals"
        }
    }
}
