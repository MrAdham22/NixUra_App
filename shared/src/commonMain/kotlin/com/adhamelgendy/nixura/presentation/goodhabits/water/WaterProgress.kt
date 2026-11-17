package com.adhamelgendy.nixura.presentation.goodhabits.water

import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.theme.WaterUnit
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitUi
import com.adhamelgendy.nixura.presentation.strings.Strings
import kotlin.math.roundToInt

/** Text of a water habit's progress, in the unit its goal is shown in. Pure, so it is unit-tested. */
object WaterProgress {

    /** "2", "1.2", "6.7": at most one decimal, none when it is whole. */
    fun formatAmount(value: Double): String {
        val tenths = (value * 10).roundToInt().coerceAtLeast(0)
        return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    }

    /** "1.2 / 2 L" or "5 / 8 cups" for a day with [count] servings. */
    fun text(goal: WaterGoal, count: Int, s: Strings): String {
        val drunk = formatAmount(goal.drunkInUnit(count))
        val target = formatAmount(goal.goalInUnit())
        return when (goal.unit) {
            WaterUnit.LITERS -> s.waterProgressLiters(drunk, target)
            WaterUnit.CUPS -> s.waterProgressCups(drunk, target, goal.goalInUnit().roundToInt())
        }
    }

    /** "0.8 L to go" / "3 cups to go", or null once the goal is reached. */
    fun leftText(goal: WaterGoal, count: Int, s: Strings): String? {
        val leftMl = goal.goalMl - goal.drunkMl(count)
        if (leftMl <= 0) return null
        return when (goal.unit) {
            WaterUnit.LITERS -> s.waterLeftLiters(formatAmount(leftMl / 1000.0))
            WaterUnit.CUPS -> {
                val cups = leftMl.toDouble() / goal.servingMl
                s.waterLeftCups(formatAmount(cups), cups.roundToInt())
            }
        }
    }

    /** The quick-add label: "+1 cup", or "+250 ml" when the goal is in litres. Both add one serving. */
    fun quickAddLabel(goal: WaterGoal, s: Strings): String = when (goal.unit) {
        WaterUnit.CUPS -> s.waterAddCup
        WaterUnit.LITERS -> s.waterAddMl(goal.servingMl)
    }

    /** Share of today's goal reached, 0..1 (the card's water level follows the same count). */
    fun progress(goal: WaterGoal, count: Int): Float = goal.progress(count)
}

/** A good habit's progress line: water in its unit ("1.2 / 2 L"), every other habit as before ("3 / 8 today"). */
fun GoodHabitUi.waterProgressTextOrNull(s: Strings): String? = water?.let { WaterProgress.text(it, todayCount, s) }
