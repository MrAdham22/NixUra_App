package com.example.reclaimapp.domain.coach

/**
 * Small, private-to-the-coach text helper: normalising and typo-tolerant word/phrase matching for the
 * offline coach's intent detection. Deliberately independent of `domain/recognition` (built in a parallel
 * branch); the two can be unified later.
 */
internal object CoachText {

    /** Lowercase, fold Arabic spelling variants, drop apostrophes, turn other punctuation into spaces, collapse spaces. */
    fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length)
        var lastSpace = true
        for (c in raw.lowercase()) {
            val folded = when (c) {
                'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                'ى' -> 'ي'
                'ة' -> 'ه'
                else -> c
            }
            when {
                folded == '\'' || folded == '’' || folded == '`' -> Unit
                c.code in 0x064B..0x065F || c.code == 0x0670 || c.code == 0x0640 -> Unit
                folded.isLetterOrDigit() -> { sb.append(folded); lastSpace = false }
                !lastSpace -> { sb.append(' '); lastSpace = true }
            }
        }
        return sb.toString().trim()
    }

    fun tokens(raw: String): List<String> {
        val n = normalize(raw)
        return if (n.isEmpty()) emptyList() else n.split(' ')
    }

    /** Phrase as normalized tokens (for lists declared once). */
    fun phrase(raw: String): List<String> = tokens(raw)

    /**
     * Whether [typed] is [key] or a likely typo of it. Up to 4 letters: exact; 5-8 letters: one edit; 9+: two edits.
     * The first letter must agree (people rarely mistype it, and it keeps false positives down).
     */
    fun wordMatches(typed: String, key: String): Boolean {
        if (typed == key) return true
        if (key.length <= 4 || typed.length <= 4) return false
        if (typed[0] != key[0]) return false
        val limit = if (key.length >= 9) 2 else 1
        if (kotlin.math.abs(typed.length - key.length) > limit) return false
        return distance(typed, key) <= limit
    }

    /** [word] starts with [stem], or (for stems of 6+ letters) its beginning is a one-edit typo of the stem. */
    fun stemMatches(word: String, stem: String): Boolean {
        if (word.startsWith(stem)) return true
        if (stem.length < 6 || word.isEmpty() || word[0] != stem[0]) return false
        for (len in (stem.length - 1)..(stem.length + 1)) {
            if (len > word.length) break
            if (distance(word.substring(0, len), stem) <= 1) return true
        }
        return false
    }

    /** True when [phrase] occurs as consecutive tokens of [tokens], each word allowed a typo. */
    fun hasPhrase(tokens: List<String>, phrase: List<String>): Boolean {
        if (phrase.isEmpty() || tokens.size < phrase.size) return false
        for (start in 0..(tokens.size - phrase.size)) {
            var ok = true
            for (k in phrase.indices) {
                if (!wordMatches(tokens[start + k], phrase[k])) { ok = false; break }
            }
            if (ok) return true
        }
        return false
    }

    fun hasWord(tokens: List<String>, key: String): Boolean = tokens.any { wordMatches(it, key) }

    /** Damerau-Levenshtein (optimal string alignment): swapping two neighbours costs 1. */
    fun distance(a: String, b: String): Int {
        val n = a.length
        val m = b.length
        if (n == 0) return m
        if (m == 0) return n
        var prev2 = IntArray(m + 1)
        var prev = IntArray(m + 1) { it }
        var cur = IntArray(m + 1)
        for (i in 1..n) {
            cur[0] = i
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, prev2[j - 2] + 1)
                cur[j] = v
            }
            val tmp = prev2; prev2 = prev; prev = cur; cur = tmp
        }
        return prev[m]
    }
}
