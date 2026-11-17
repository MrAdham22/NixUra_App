package com.adhamelgendy.nixura.presentation.goodhabits.water

import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitUi

/**
 * How full a day is for the calendar and the week strip, 0..1: a water habit by its ml goal, a weekly habit by
 * "done that day", every other habit by count / daily target.
 */
fun GoodHabitUi.dayProgress(count: Int): Float = when {
    count <= 0 -> 0f
    isWeekly -> 1f
    water != null -> water.progress(count)
    else -> (count.toFloat() / dailyTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
}
