package com.adhamelgendy.nixura.domain.coach.ai

/** The data of one progress card in the coach chat. The AI only picks which card to show; the numbers are the app's. */
sealed interface CoachVisual {
    /** A quit habit: how far the current streak is towards the next milestone ([goalDays]). */
    data class Quit(
        val name: String,
        val elapsedMillis: Long,
        val longestMillis: Long,
        val relapses: Int,
        val goalDays: Int,
        val progress: Float,
    ) : CoachVisual

    /** A good habit: the last 7 days, oldest first, each 0..1 of that day's target. [weekdays] are ISO (Monday = 1). */
    data class Good(
        val name: String,
        val days: List<Float>,
        val weekdays: List<Int>,
        /** Days for a daily habit, weeks for a weekly one. */
        val streak: Int,
        val weekly: Boolean,
        val count: Int,
        val target: Int,
    ) : CoachVisual

    data class Overview(
        val quitCount: Int,
        val bestCleanMillis: Long,
        val goodDone: Int,
        val goodTotal: Int,
        /** Average completion of the good habits over the last 7 days; null when there are none. */
        val weekPercent: Int?,
    ) : CoachVisual

    companion object {
        const val OVERVIEW = "overview"
        val MILESTONE_DAYS = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)
    }
}
