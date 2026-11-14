package com.adhamelgendy.nixura.domain.calculator

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** The next milestone of a clean streak: reached at [days] days, [remaining] from now. */
data class NextMilestone(val days: Int, val remaining: Duration)

/**
 * Milestones of a quit streak: 1, 3, 7, 14 and 30 days (the card's color stages), then 60, 90, 180 and 365 days,
 * then every full year. Pure; the card shows the next one so there is always a near goal to hold on to.
 */
object MilestoneCalculator {
    val MILESTONE_DAYS: List<Int> = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)

    fun next(elapsed: Duration): NextMilestone {
        val clean = elapsed.coerceAtLeast(Duration.ZERO)
        val days = MILESTONE_DAYS.firstOrNull { it.days > clean }
            ?: ((clean.inWholeDays / 365 + 1) * 365).toInt()
        return NextMilestone(days, days.days - clean)
    }
}
