package com.example.reclaimapp.domain.recognition

import com.example.reclaimapp.domain.coach.HabitCategory

/** A category that matched the user's text, with how sure the matcher is (0..1). */
class CategoryMatch(
    val category: HabitCategory,
    val confidence: Float,
    /** The catalog keyword that matched. */
    val keyword: String,
    /** The word the user typed that matched it (normalized), or null for phrase matches. */
    val typedWord: String?,
    /** True when the typed word is the keyword itself or a normal inflection of it (not a typo). */
    val exact: Boolean,
)

internal class GoodMatch(
    val id: String,
    val confidence: Float,
    val keyword: String,
    val typedWord: String?,
    val exact: Boolean,
)

internal class MatchOutcome(
    val analysis: NormalizedHabitText,
    val category: CategoryMatch?,
    val good: GoodMatch?,
)

/**
 * Matches free text to the categories of a catalog. The catalog index is built once in the constructor;
 * [outcome] is safe to call on every keystroke.
 */
internal class HabitMatcher(private val categories: List<HabitCategory>) {

    private val index: KeywordIndex = KeywordIndex(
        keywordsByOwner = categories.map { it.keywords },
        weakKeywords = WEAK_KEYWORDS,
        expansions = HabitDictionary.latinWords,
        strictWords = HabitDictionary.commonWords,
    )

    /** Words offered as spelling corrections: the built-in dictionary plus longer single-word catalog keywords. */
    private val suggestionWords: List<Word> = run {
        val set = LinkedHashSet<String>(HabitDictionary.latinWords)
        for (c in categories) {
            for (k in c.keywords) {
                val n = HabitTextNormalizer.normalize(k)
                if (n.length >= 5 && !n.contains(' ') && n[0].code < 0x0600) {
                    // Skip bare stems such as "procrastinat": a longer dictionary word already extends them.
                    if (HabitDictionary.latinWords.none { it.length > n.length && it.startsWith(n) }) set.add(n)
                }
            }
        }
        set.map { Word.of(it) }
    }

    fun outcome(raw: String): MatchOutcome {
        val analysis = HabitTextNormalizer.analyze(raw)
        val tokens = analysis.coreTokens
        if (tokens.isEmpty()) return MatchOutcome(analysis, null, null)
        val query = ArrayList<QueryWord>(tokens.size)
        for (t in tokens) query.add(QueryWord(t, Word.of(Word.stripArabicPrefix(t))))

        val mask = BooleanArray(tokens.size)
        GoodHabitLexicon.maskNeutral(tokens, mask)

        // Good-habit lexicon first: its strong hits claim their words ("drinking water" is not a drinking habit).
        val goodHits = GoodHabitLexicon.index.match(query, mask)
        var goodBest: KeywordHit? = null
        for (h in goodHits) if (h != null && h.beats(goodBest)) goodBest = h
        for (h in goodHits) {
            if (h != null && h.quality >= WordMatch.STRONG) for (idx in h.tokenIndexes) mask[idx] = true
        }
        val good = goodBest?.takeIf { it.score >= MIN_CONFIDENCE }?.let {
            GoodMatch(
                id = GoodHabitLexicon.entries[it.owner].id,
                confidence = it.score,
                keyword = it.keyword,
                typedWord = typedWordOf(it, query),
                exact = it.quality >= WordMatch.STEM,
            )
        }

        val hits = index.match(query, mask)
        var best: KeywordHit? = null
        for (h in hits) if (h != null && h.beats(best)) best = h
        val category = best?.takeIf { it.score >= MIN_CONFIDENCE }?.let {
            CategoryMatch(
                category = categories[it.owner],
                confidence = it.score,
                keyword = it.keyword,
                typedWord = typedWordOf(it, query),
                exact = it.quality >= WordMatch.STEM,
            )
        }
        return MatchOutcome(analysis, category, good)
    }

    private fun typedWordOf(hit: KeywordHit, query: List<QueryWord>): String? =
        if (hit.tokenIndexes.size == 1) query[hit.tokenIndexes[0]].word.text else null

    /**
     * Nearest dictionary word for a misspelled [typed] word, or null when [typed] is already a real word
     * (or an inflection of one) or nothing is close enough.
     */
    fun correctionFor(typed: String): String? {
        val t = Word.of(typed)
        if (t.text.length < 4 || t.arabic) return null
        var bestWord: Word? = null
        var bestQuality = 0f
        for (w in suggestionWords) {
            val q = WordMatch.quality(t, w)
            if (q >= WordMatch.STEM) return null // already a valid word form
            if (q <= 0f) continue
            if (q > bestQuality || (q == bestQuality && better(w, bestWord, t))) {
                bestQuality = q
                bestWord = w
            }
        }
        return bestWord?.text
    }

    private fun better(a: Word, b: Word?, t: Word): Boolean {
        if (b == null) return true
        val da = kotlin.math.abs(a.text.length - t.text.length)
        val db = kotlin.math.abs(b.text.length - t.text.length)
        if (da != db) return da < db
        return a.text < b.text
    }

    companion object {
        const val MIN_CONFIDENCE = 0.5f

        /** Keywords that are too generic to be decisive on their own; their score is scaled down. */
        val WEAK_KEYWORDS: Set<String> by lazy {
            listOf(
                "drinking", "drink", "drinks", "شرب", "اكل", "eating", "eat", "phone", "game", "games",
                "midnight", "late", "late night", "sleep", "night", "نوم", "screen", "food", "time",
                "bite", "biting", "picking", "delay", "lazy", "spending", "shopping",
            ).map { HabitTextNormalizer.normalize(it) }.toHashSet()
        }
    }
}
