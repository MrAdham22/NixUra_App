package com.adhamelgendy.nixura.domain.theme

import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import com.adhamelgendy.nixura.domain.recognition.GoodHabitFillStyle
import com.adhamelgendy.nixura.domain.recognition.HabitTextNormalizer
import com.adhamelgendy.nixura.domain.recognition.HabitTopic
import com.adhamelgendy.nixura.domain.recognition.PrayerHabitDetector
import com.adhamelgendy.nixura.domain.recognition.Word

/**
 * The look of a habit to build: its colour, icon and animation all follow from this one value. Detected once from the
 * name when the habit is saved and stored with it, so screens never guess again and a language switch cannot change it.
 */
enum class HabitTheme(
    /** Stored in Room and in synced records. Never rename an existing key. */
    val storageKey: String,
    /** The glyph family shown on cards and widgets. */
    val topic: HabitTopic,
) {
    WATER("water", HabitTopic.WATER),
    PRAYER("prayer", HabitTopic.PRAYER),
    WALK("walk", HabitTopic.MOVEMENT),
    WORKOUT("workout", HabitTopic.FITNESS),
    READING("reading", HabitTopic.LEARNING),
    SLEEP("sleep", HabitTopic.SLEEP),
    DEFAULT("default", HabitTopic.OTHER_BUILD),
    ;

    /** How the card shows today's progress: rising water, the gold prayer card, or the plain green fill. */
    val fillStyle: GoodHabitFillStyle
        get() = when (this) {
            WATER -> GoodHabitFillStyle.WATER
            PRAYER -> GoodHabitFillStyle.PRAYER
            else -> GoodHabitFillStyle.PLAIN
        }

    companion object {
        /** Unknown or missing keys (a record from a newer build, or none yet) read as [DEFAULT]. */
        fun fromStorageKey(key: String?): HabitTheme? = key?.let { k -> entries.firstOrNull { it.storageKey == k } }
    }
}

/**
 * Picks the [HabitTheme] of a habit name with the recognizer the forms and the coach already use (typos, synonyms and
 * Arabic included). It does not match words itself: it only maps the recognizer's answer to a theme.
 * The first call builds the catalog index, so call it off the main thread (save path, migration backfill, sync).
 */
class HabitThemeDetector(private val knowledge: HabitKnowledgeBase) {

    fun themeFor(name: String): HabitTheme {
        if (name.isBlank()) return HabitTheme.DEFAULT
        if (PrayerHabitDetector.isPrayer(name)) return HabitTheme.PRAYER
        val outcome = knowledge.outcomeFor(name.trim())
        outcome.good?.id?.let { return themeOfBuildHabit(it) }
        // A bare "sleep" / "نوم" is left out of the lexicon on purpose (as a habit to quit it is ambiguous), but as a
        // habit to build it can only mean sleeping well. Uses the recognizer's own normalizer, not a new matcher.
        val core = HabitTextNormalizer.analyze(name).coreText
        return if (core in BARE_SLEEP || Word.stripArabicPrefix(core) in BARE_SLEEP) HabitTheme.SLEEP else HabitTheme.DEFAULT
    }

    companion object {
        private val BARE_SLEEP = setOf("sleep", "sleeping", "نوم")

        /** Entry ids of the build lexicon (`GoodHabitLexicon`) to themes. Unlisted ids keep the default look. */
        fun themeOfBuildHabit(id: String): HabitTheme = when (id) {
            "water", "hydrate_lemon" -> HabitTheme.WATER
            "pray", "tahajjud" -> HabitTheme.PRAYER
            "walk", "walk_dog", "stairs", "run" -> HabitTheme.WALK
            "workout", "pushups", "weights", "cycle", "swim", "sports" -> HabitTheme.WORKOUT
            "read", "study", "quran_memorize" -> HabitTheme.READING
            "sleep_early", "sleep_routine" -> HabitTheme.SLEEP
            else -> HabitTheme.DEFAULT
        }
    }
}
