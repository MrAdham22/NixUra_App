package com.example.reclaimapp.domain.recognition

/** Is the habit something to quit (BAD) or something to build (GOOD)? UNKNOWN when the evidence is weak. */
enum class HabitPolarity { BAD, GOOD, UNKNOWN }

data class PolarityResult(val polarity: HabitPolarity, val confidence: Float)

/**
 * Combines the signals into one verdict. It never guesses aggressively: conflicting or thin evidence is UNKNOWN.
 *
 *  - a confident match to a catalog category = BAD,
 *  - a confident match to the good-habit lexicon = GOOD,
 *  - wrappers ("quit", "stop" / "start", "daily") only nudge the score,
 *  - a negated skip verb ("don't skip workout") flips to GOOD, a plain one ("skip breakfast") is UNKNOWN.
 */
internal object PolarityClassifier {

    private const val NUDGE = 0.1f
    private const val DECISIVE_GAP = 0.2f
    private const val MIN_VERDICT = 0.5f

    fun classify(analysis: NormalizedHabitText, category: CategoryMatch?, good: GoodMatch?): PolarityResult {
        val lean = analysis.intent
        var bad = category?.confidence ?: 0f
        var goodScore = good?.confidence ?: 0f

        if (goodScore > 0f && analysis.hasSkipVerb) {
            return if (analysis.isSkipNegated) {
                PolarityResult(HabitPolarity.GOOD, maxOf(goodScore, 0.85f).coerceAtMost(1f))
            } else {
                PolarityResult(HabitPolarity.UNKNOWN, 0.3f)
            }
        }

        if (bad <= 0f && goodScore <= 0f) {
            return PolarityResult(HabitPolarity.UNKNOWN, if (lean == IntentLean.NONE) 0f else 0.3f)
        }

        if (lean == IntentLean.BAD && bad > 0f) bad = minOf(1f, bad + NUDGE)
        if (lean == IntentLean.GOOD && goodScore > 0f) goodScore = minOf(1f, goodScore + NUDGE)

        if (bad > 0f && goodScore > 0f) {
            val gap = goodScore - bad
            return when {
                gap >= DECISIVE_GAP -> verdict(HabitPolarity.GOOD, goodScore - 0.15f)
                -gap >= DECISIVE_GAP -> verdict(HabitPolarity.BAD, bad - 0.15f)
                else -> PolarityResult(HabitPolarity.UNKNOWN, maxOf(bad, goodScore) * 0.5f)
            }
        }
        if (bad > 0f) {
            val c = if (lean == IntentLean.GOOD) bad * 0.85f else bad
            return verdict(HabitPolarity.BAD, c)
        }
        val c = if (lean == IntentLean.BAD) goodScore * 0.4f else goodScore
        return verdict(HabitPolarity.GOOD, c)
    }

    private fun verdict(polarity: HabitPolarity, confidence: Float): PolarityResult {
        val c = confidence.coerceIn(0f, 1f)
        return if (c < MIN_VERDICT) PolarityResult(HabitPolarity.UNKNOWN, c) else PolarityResult(polarity, c)
    }
}
