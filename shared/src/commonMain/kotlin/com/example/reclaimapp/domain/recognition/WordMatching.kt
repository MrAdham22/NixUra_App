package com.example.reclaimapp.domain.recognition

/** A normalized word with the extra forms the matcher compares against, computed once. */
internal class Word private constructor(
    val text: String,
    /** [text] with every run of identical letters collapsed ("coffee" -> "cofe"). */
    val sq: String,
    /** Consonant skeleton of [sq] ("smoking" -> "smkng"); equals [sq] for Arabic. */
    val skel: String,
    val vowels: Int,
    val arabic: Boolean,
) {
    companion object {
        fun of(normalizedWord: String): Word {
            val text = normalizedWord
            val sq = HabitTextNormalizer.squeeze(text)
            val arabic = text.isNotEmpty() && text[0].code in 0x0600..0x06FF
            if (arabic) return Word(text, sq, sq, 0, true)
            val sb = StringBuilder(sq.length)
            var vowels = 0
            for (ch in sq) {
                if (ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u') vowels++ else sb.append(ch)
            }
            // Keep a leading vowel so "alcohol" and "alchl" share a skeleton start.
            val skel = if (sq.isNotEmpty() && isVowel(sq[0])) sq[0] + sb.toString() else sb.toString()
            return Word(text, sq, skel, vowels, false)
        }

        private fun isVowel(c: Char) = c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u'

        /** Strips a leading Arabic article ("ال", "بال", "وال", "لل") when at least 3 letters remain. */
        fun stripArabicPrefix(w: String): String {
            if (w.isEmpty() || w[0].code !in 0x0600..0x06FF) return w
            for (p in ARABIC_PREFIXES) {
                if (w.startsWith(p) && w.length - p.length >= 3) return w.substring(p.length)
            }
            return w
        }

        private val ARABIC_PREFIXES = listOf("وال", "بال", "كال", "فال", "ال", "لل")
    }
}

/**
 * Quality of the match between a typed word and a keyword word, 0 when they do not match.
 * Word-boundary aware by construction: whole words only, never "keyword inside another word".
 */
internal object WordMatch {
    const val EXACT = 1.0f
    const val STEM = 0.9f
    const val SQ_EQUAL = 0.88f
    const val SQ_STEM = 0.85f
    const val FUZZY_1 = 0.75f
    const val FUZZY_2 = 0.65f
    const val SKELETON = 0.7f
    const val TRUNCATED = 0.7f

    /** Matches at or above this quality are treated as "the user typed this word (or an inflection of it)". */
    const val STRONG = 0.85f

    private val ENGLISH_SUFFIXES: Set<String> = hashSetOf(
        "s", "es", "ed", "d", "e", "er", "ers", "r", "ing", "ings", "ion", "ions", "ation", "ations",
        "ive", "ly", "ie", "ies", "ic", "al", "ness", "or", "ors",
    )

    fun quality(t: Word, k: Word): Float {
        if (t.text == k.text) return EXACT
        if (t.text.isEmpty() || k.text.isEmpty()) return 0f
        if (t.text[0] != k.text[0]) return 0f
        val kl = k.text.length
        val minStem = if (k.arabic) 3 else 4
        if (kl >= minStem && t.text.length > kl && t.text.startsWith(k.text) && suffixOk(t.text, kl, k.arabic)) return STEM
        if (t.sq == k.sq) return SQ_EQUAL
        val ksl = k.sq.length
        if (ksl >= minStem && t.sq.length > ksl && t.sq.startsWith(k.sq) && suffixOk(t.sq, ksl, k.arabic)) return SQ_STEM

        val tol = tolerance(ksl, k.arabic)
        if (tol > 0 && kotlin.math.abs(t.sq.length - ksl) <= tol) {
            val d = damerau(t.sq, k.sq, tol)
            if (d in 1..tol && allowedEdit(t.sq, k.sq, d, ksl, k.arabic)) return if (d == 1) FUZZY_1 else FUZZY_2
        }
        // Typo inside a long word that carries a suffix ("scrollng" vs "scroll").
        if (ksl >= 6 && t.sq.length > ksl && t.sq.length - ksl <= 4) {
            val d = damerau(t.sq.substring(0, ksl), k.sq, tol)
            if (d in 1..tol) return TRUNCATED
        }
        // Dropped vowels ("smkng", "cigrt").
        if (!k.arabic && !t.arabic && t.text.length >= 4 && k.vowels > t.vowels && k.skel.length >= 3) {
            if (t.skel == k.skel) return SKELETON
            if (k.skel.length >= 4 && t.skel.startsWith(k.skel) && t.skel.length - k.skel.length <= 2) return SKELETON
        }
        return 0f
    }

    private fun tolerance(len: Int, arabic: Boolean): Int = if (arabic) {
        when {
            len < 5 -> 0
            len < 8 -> 1
            else -> 2
        }
    } else {
        when {
            len <= 3 -> 0
            len <= 6 -> 1
            else -> 2
        }
    }

    /**
     * 4-letter English keywords are collision-prone ("game"/"gate", "read"/"ready"): only a swap of two neighbours
     * is accepted. A distance of 2 additionally needs the same first two letters, so unrelated words such as
     * "shopping" and "smoking" never meet.
     */
    private fun allowedEdit(t: String, k: String, d: Int, kl: Int, arabic: Boolean): Boolean {
        if (d >= 2 && (t.length < 2 || k.length < 2 || t[1] != k[1])) return false
        if (arabic || kl != 4) return true
        return d == 1 && t.length == 4 && isAdjacentSwap(t, k)
    }

    private fun isAdjacentSwap(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var i = 0
        while (i < a.length && a[i] == b[i]) i++
        if (i + 1 >= a.length) return false
        if (a[i] != b[i + 1] || a[i + 1] != b[i]) return false
        for (j in i + 2 until a.length) if (a[j] != b[j]) return false
        return true
    }

    private fun suffixOk(text: String, keywordLength: Int, arabic: Boolean): Boolean {
        val extra = text.length - keywordLength
        if (arabic) return extra <= 3
        if (extra > 5) return false
        return text.substring(keywordLength) in ENGLISH_SUFFIXES
    }

    /** Optimal-string-alignment Damerau-Levenshtein distance; returns max + 1 as soon as it must exceed [max]. */
    fun damerau(a: String, b: String, max: Int): Int {
        val n = a.length
        val m = b.length
        if (kotlin.math.abs(n - m) > max) return max + 1
        if (n == 0) return m
        if (m == 0) return n
        var prev2 = IntArray(m + 1)
        var prev = IntArray(m + 1) { it }
        var cur = IntArray(m + 1)
        for (i in 1..n) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    v = minOf(v, prev2[j - 2] + 1)
                }
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > max) return max + 1
            val tmp = prev2
            prev2 = prev
            prev = cur
            cur = tmp
        }
        return prev[m]
    }
}
