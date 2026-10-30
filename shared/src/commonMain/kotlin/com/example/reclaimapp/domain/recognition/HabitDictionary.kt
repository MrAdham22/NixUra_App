package com.example.reclaimapp.domain.recognition

/**
 * Common Latin-script habit words. They serve two purposes: they complete short catalog stems
 * ("smok" -> "smoking") so typos like "smokng" are still caught, and they are the candidates
 * for "Did you mean ...?" corrections. Matching never depends on a word being listed here.
 */
internal object HabitDictionary {

    private val base = listOf(
        // nicotine
        "smoking", "smoke", "smoker", "smokers", "smokes", "smoked", "cigarette", "cigarettes", "cigar", "cigars",
        "vaping", "vape", "vapes", "nicotine", "tobacco", "shisha", "hookah", "snus",
        // drinks and food
        "coffee", "caffeine", "espresso", "alcohol", "drinking", "beer", "wine", "vodka", "whiskey", "liquor",
        "sugar", "sugary", "sweets", "sweet", "soda", "candy", "dessert", "desserts", "chocolate", "cookies", "cake",
        "junk", "snacking", "snacks", "burger", "burgers", "pizza", "fries", "chips", "fried", "takeaway", "overeating",
        // screens
        "scrolling", "scroll", "doomscrolling", "instagram", "facebook", "twitter", "snapchat", "youtube", "tiktok",
        "netflix", "gaming", "games", "reels", "shorts", "phone", "screen",
        // behaviours
        "gambling", "gambler", "gamble", "betting", "casino", "lottery", "shopping", "spending",
        "procrastination", "procrastinating", "procrastinate", "lazy", "laziness", "nails", "biting", "picking",
        "oversleeping", "midnight", "porn", "cannabis", "marijuana", "tramadol", "painkillers", "stimulants",
    )

    /** Everyday words that merely look like a habit keyword; they never count as typos of it. */
    val commonWords: Set<String> by lazy {
        listOf(
            "friend", "friends", "friendly", "sweat", "sweaty", "sweater", "desert", "deserts", "fired", "fire",
            "tried", "sending", "stopping", "better", "getting", "setting", "letting", "bitten", "bitter", "scream",
            "screed", "snake", "mailing", "mail", "same", "gate", "ready", "reader", "pitch", "winter", "wiser",
            "shipping", "chins", "shops", "shopper", "stoping", "casinos", "canny", "handy", "cooler",
        ).map { HabitTextNormalizer.normalize(it) }.toHashSet()
    }

    val latinWords: List<String> by lazy {
        (base + GoodHabitLexicon.dictionaryWords())
            .map { HabitTextNormalizer.normalize(it) }
            .filter { it.length >= 4 && !it.contains(' ') }
            .distinct()
    }
}
