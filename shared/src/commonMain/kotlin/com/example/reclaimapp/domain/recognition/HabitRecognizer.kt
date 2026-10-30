package com.example.reclaimapp.domain.recognition

import com.example.reclaimapp.domain.coach.HabitCategory
import com.example.reclaimapp.domain.coach.HabitKnowledgeBase

/**
 * What the app understood from a habit name typed by the user.
 *
 * @property normalizedText the habit with intent wrappers and filler words removed ("quit smoking cigarettes" -> "smoking cigarettes").
 * @property polarity BAD = a habit to quit, GOOD = a habit to build.
 * @property category the matched knowledge category, or null when nothing matched well.
 * @property suggestedCorrection a properly spelled word when the match came from a typo ("smoking" for "smokng").
 * @property correctedText the user's whole text with the typo replaced, ready to put back into the field.
 */
data class HabitRecognition(
    val normalizedText: String,
    val polarity: HabitPolarity,
    val polarityConfidence: Float,
    val category: HabitCategory?,
    val categoryConfidence: Float,
    val suggestedCorrection: String?,
    val correctedText: String? = null,
)

/** Works out which known habit a free-typed name is and whether it is good or bad. Pure, offline, per-keystroke fast. */
class HabitRecognizer(private val knowledge: HabitKnowledgeBase) {

    fun recognize(rawText: String): HabitRecognition {
        val outcome = knowledge.outcomeFor(rawText)
        val a = outcome.analysis
        val polarity = PolarityClassifier.classify(a, outcome.category, outcome.good)

        // The suggestion belongs to whichever side drove the verdict (or the category when undecided).
        val typed = when (polarity.polarity) {
            HabitPolarity.GOOD -> outcome.good?.takeIf { !it.exact }?.typedWord
            HabitPolarity.BAD -> outcome.category?.takeIf { !it.exact }?.typedWord
            HabitPolarity.UNKNOWN -> outcome.category?.takeIf { !it.exact }?.typedWord
        }
        val suggestion = typed?.let { knowledge.correctionFor(it) }
        val corrected = if (typed != null && suggestion != null) replaceWord(rawText, typed, suggestion) else null

        return HabitRecognition(
            normalizedText = a.coreText,
            polarity = polarity.polarity,
            polarityConfidence = polarity.confidence,
            category = outcome.category?.category,
            categoryConfidence = outcome.category?.confidence ?: 0f,
            suggestedCorrection = if (corrected != null) suggestion else null,
            correctedText = corrected,
        )
    }

    /** Replaces the first raw word whose normalized form is [typed] with [replacement]. */
    private fun replaceWord(rawText: String, typed: String, replacement: String): String? {
        val parts = rawText.trim().split(' ', '\t', '\n').filter { it.isNotEmpty() }.toMutableList()
        for (i in parts.indices) {
            if (HabitTextNormalizer.normalize(parts[i]) == typed) {
                parts[i] = replacement
                return parts.joinToString(" ")
            }
        }
        return null
    }
}

/**
 * Carries a habit name from one form to the other ("Add it to Build instead?") without touching the Navigator.
 * Registered as a Koin singleton; the receiving form calls [take] once.
 */
class HabitNameHandoff {
    private var pending: String? = null

    fun put(name: String) {
        pending = name
    }

    /** Returns the carried name once, then forgets it. */
    fun take(): String? {
        val value = pending
        pending = null
        return value
    }
}
