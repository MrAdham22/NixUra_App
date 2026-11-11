package com.adhamelgendy.nixura.domain.recognition

/**
 * Spots a good-habit name that means Muslim prayer ("pray", "Salah", "pray 5 times", "صلاة", "اصلي الفجر"...).
 * It looks at each word on its own, so the position of the word in the text is known and the form can start its
 * gold effect right there. Pure Kotlin, safe to call on every keystroke.
 */
object PrayerHabitDetector {

    /** The daily target a prayer habit starts with: the five obligatory prayers. */
    const val DAILY_PRAYERS = 5

    /** Where the prayer word sits in [text] (first match), or null when the name is not about prayer. */
    fun wordRange(text: String): IntRange? {
        var start = -1
        for (i in 0..text.length) {
            val boundary = i == text.length || text[i].isWhitespace() || text[i] in SEPARATORS
            if (!boundary && start < 0) start = i
            if (boundary && start >= 0) {
                if (isPrayerWord(text.substring(start, i))) return start until i
                start = -1
            }
        }
        return null
    }

    fun isPrayer(text: String): Boolean = text.isNotBlank() && wordRange(text) != null

    private val SEPARATORS = charArrayOf(',', '.', ';', ':', '!', '?', '-', '_', '/', '(', ')', '،', '؛', '؟')

    private fun isPrayerWord(raw: String): Boolean {
        val word = HabitTextNormalizer.normalize(raw).replace(" ", "")
        if (word.length < 3) return false
        if (word in LATIN_EXACT || word.startsWith("pray")) return true
        if (word.length >= 5 && LATIN_FUZZY.any { editDistanceAtMostOne(word, it) }) return true
        return isArabicPrayer(word)
    }

    /** Arabic: drop a leading "al-" / "wa-" / "bi-" / "li-" first, then compare with the known forms. */
    private fun isArabicPrayer(word: String): Boolean {
        if (word in ARABIC_EXACT) return true
        for (prefix in ARABIC_PREFIXES) {
            if (word.startsWith(prefix) && word.removePrefix(prefix) in ARABIC_EXACT) return true
        }
        return false
    }

    private fun editDistanceAtMostOne(a: String, b: String): Boolean {
        if (a == b) return true
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) { i++; j++; continue }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                a.length < b.length -> j++
                else -> { i++; j++ }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }

    private val LATIN_EXACT = setOf(
        "salah", "salat", "salaah", "salaat", "solat", "namaz", "namaaz", "fajr", "fajar", "dhuhr", "zuhr", "duhr",
        "maghrib", "isha", "ishaa", "tahajjud", "taraweeh", "tarawih",
    )
    private val LATIN_FUZZY = listOf("prayer", "prayers", "praying", "salah", "salat", "maghrib")

    // Written in the normalized form (ta-marbuta -> ha, alef-maqsura -> ya, alef forms -> plain alef).
    private val ARABIC_EXACT = setOf(
        "صلاه", "صلاتي", "صلوات", "صلواتي", "صلي", "اصلي", "نصلي", "تصلي", "يصلي", "صليت", "صلوا",
        "فجر", "مغرب", "تهجد", "تراويح", "الصلاه", "الصلوات",
    )
    private val ARABIC_PREFIXES = listOf("ال", "و", "ب", "ل", "وال", "بال", "لل")
}
