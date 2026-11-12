package com.adhamelgendy.nixura.domain.recognition

import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase

/**
 * The kind of habit, as shown by the glyph on its card and widget (a cigarette for smoking, a drop for water...).
 * Derived from the habit's name every time, never stored, so renaming a habit changes its glyph.
 */
enum class HabitTopic {
    // habits people quit
    SMOKING, ALCOHOL, HOT_DRINK, SOFT_DRINK, SWEETS, JUNK_FOOD, PHONE, GAMING, TV, PILLS, MONEY, GAMBLING, SLEEP,
    TIME, MIND, SITTING, SPEECH, HANDS, ADULT_CONTENT,

    // habits people build
    WATER, FITNESS, MOVEMENT, LEARNING, PRAYER, CALM, WRITING, HEALTHY_FOOD, MORNING, SELF_CARE, PEOPLE, MUSIC, TIDY,
    EYES,

    /** Not recognised: the default shield of a quit habit. */
    OTHER_QUIT,

    /** Not recognised: the default leaf of a habit to build. */
    OTHER_BUILD,
}

/**
 * Picks the [HabitTopic] of a habit from its free-typed name with the same offline recognizer the forms and the coach
 * use (typos, synonyms and Arabic work too). Pure and fast; callers cache by name.
 */
class HabitTopicDetector(private val knowledge: HabitKnowledgeBase) {

    /** Topic of a habit the user is quitting. */
    fun quitTopic(name: String): HabitTopic {
        if (name.isBlank()) return HabitTopic.OTHER_QUIT
        val outcome = knowledge.outcomeFor(name.trim())
        return outcome.category?.category?.id?.let(::topicOfQuitCategory)
            ?: outcome.good?.id?.let(::topicOfBuildHabit)
            ?: HabitTopic.OTHER_QUIT
    }

    /** Topic of a habit the user is building. */
    fun buildTopic(name: String): HabitTopic {
        if (name.isBlank()) return HabitTopic.OTHER_BUILD
        if (PrayerHabitDetector.isPrayer(name)) return HabitTopic.PRAYER
        val outcome = knowledge.outcomeFor(name.trim())
        return outcome.good?.id?.let(::topicOfBuildHabit)
            ?: outcome.category?.category?.id?.let(::topicOfQuitCategory)
            ?: HabitTopic.OTHER_BUILD
    }

    companion object {
        /** Category ids of the quit catalog (`domain/coach/catalog`) to topics; `null` for the generic profile. */
        fun topicOfQuitCategory(id: String): HabitTopic? = when (id) {
            "nicotine", "vaping", "shisha", "smokeless" -> HabitTopic.SMOKING
            "alcohol" -> HabitTopic.ALCOHOL
            "caffeine", "energydrinks", "tea" -> HabitTopic.HOT_DRINK
            "softdrinks", "sugarindrinks" -> HabitTopic.SOFT_DRINK
            "sugar" -> HabitTopic.SWEETS
            "junkfood", "friedfood", "overeating", "latenighteating", "salt" -> HabitTopic.JUNK_FOOD
            "screens", "socialmedia", "doomscrolling", "shortvideo", "phonechecking", "latenightphone", "texting" -> HabitTopic.PHONE
            "gaming" -> HabitTopic.GAMING
            "bingewatching" -> HabitTopic.TV
            "cannabis", "painkillers", "sleepingpills", "stimulants" -> HabitTopic.PILLS
            "spending" -> HabitTopic.MONEY
            "gambling" -> HabitTopic.GAMBLING
            "sleep", "oversleeping" -> HabitTopic.SLEEP
            "procrastination" -> HabitTopic.TIME
            "overthinking", "negativeselftalk", "comparison" -> HabitTopic.MIND
            "sitting" -> HabitTopic.SITTING
            "swearing", "gossiping", "anger" -> HabitTopic.SPEECH
            "nails", "skinpicking", "hairpulling", "lipbiting", "knucklecracking", "teethgrinding" -> HabitTopic.HANDS
            "explicitcontent" -> HabitTopic.ADULT_CONTENT
            else -> null
        }

        /** Entry ids of the build lexicon ([GoodHabitLexicon]) to topics. */
        fun topicOfBuildHabit(id: String): HabitTopic? = when (id) {
            "water", "hydrate_lemon" -> HabitTopic.WATER
            "workout", "pushups", "weights", "posture", "posture2" -> HabitTopic.FITNESS
            "walk", "run", "stairs", "walk_dog", "cycle", "swim", "sports", "morning" -> HabitTopic.MOVEMENT
            "read", "study", "learn", "learn_skill", "quran_memorize", "podcast" -> HabitTopic.LEARNING
            "pray", "tahajjud" -> HabitTopic.PRAYER
            "meditate", "breathe", "stretch" -> HabitTopic.CALM
            "journal", "creative", "plan" -> HabitTopic.WRITING
            "healthy_food", "vegetables", "cook", "protein", "fiber", "nuts", "fasting" -> HabitTopic.HEALTHY_FOOD
            "water_fruit" -> HabitTopic.HOT_DRINK
            "vitamins" -> HabitTopic.PILLS
            "sleep_early", "sleep_routine" -> HabitTopic.SLEEP
            "wake_early", "sunlight" -> HabitTopic.MORNING
            "teeth", "skincare", "cold_shower", "floss_hands" -> HabitTopic.SELF_CARE
            "call_family", "family", "gratitude", "smile", "charity" -> HabitTopic.PEOPLE
            "save", "budget_track" -> HabitTopic.MONEY
            "practice", "music" -> HabitTopic.MUSIC
            "tidy", "tidy_desk" -> HabitTopic.TIDY
            "digital" -> HabitTopic.PHONE
            "eye" -> HabitTopic.EYES
            else -> null
        }
    }
}
