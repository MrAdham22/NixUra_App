package com.example.reclaimapp.domain.recognition

/** One query word of the user's text, ready for matching. */
internal class QueryWord(val raw: String, val word: Word)

internal class KeywordHit(
    val owner: Int,
    val score: Float,
    val quality: Float,
    val keyword: String,
    val wordCount: Int,
    val keywordLength: Int,
    /** Index (in the query) of every token this hit consumed. */
    val tokenIndexes: IntArray,
) {
    /** Better score first, then longer phrase, then longer (more specific) keyword. */
    fun beats(other: KeywordHit?): Boolean {
        if (other == null) return true
        if (score != other.score) return score > other.score
        if (wordCount != other.wordCount) return wordCount > other.wordCount
        return keywordLength > other.keywordLength
    }
}

private class Entry(
    val owner: Int,
    val keyword: String,
    val words: List<Word>,
    val weak: Boolean,
)

/**
 * Index of normalized keywords grouped by owner (a habit category or a good-habit lexicon entry).
 * Built once; [match] is cheap enough for every keystroke.
 *
 * @param keywordsByOwner keywords of each owner, in any language.
 * @param weakKeywords normalized keywords that are too generic to be decisive ("drinking", "phone", ...).
 * @param expansions full words used to complete short keyword stems ("smok" -> "smoking") so typos in them are caught.
 */
internal class KeywordIndex(
    keywordsByOwner: List<List<String>>,
    weakKeywords: Set<String> = emptySet(),
    expansions: List<String> = emptyList(),
    /** Real words that must only match exactly or as an inflection, never as a typo ("friend" is not "fried"). */
    private val strictWords: Set<String> = emptySet(),
) {
    val ownerCount: Int = keywordsByOwner.size

    private val singles = HashMap<Char, MutableList<Entry>>()
    private val phrases = ArrayList<Entry>()

    init {
        val seen = HashSet<String>()
        for (owner in keywordsByOwner.indices) {
            for (keyword in keywordsByOwner[owner]) {
                val normalized = HabitTextNormalizer.normalize(keyword)
                if (normalized.isEmpty()) continue
                val tokens = normalized.split(' ').map { Word.stripArabicPrefix(it) }
                val weak = normalized in weakKeywords
                add(Entry(owner, normalized, tokens.map { Word.of(it) }, weak), seen)
                if (tokens.size == 1 && tokens[0].length >= 4 && tokens[0][0].code < 0x0600) {
                    for (full in expansions) {
                        if (full.length > tokens[0].length && full.startsWith(tokens[0]) && isInflection(full, tokens[0])) {
                            add(Entry(owner, full, listOf(Word.of(full)), weak), seen)
                        }
                    }
                }
            }
        }
    }

    private fun add(e: Entry, seen: HashSet<String>) {
        if (!seen.add("${e.owner}|${e.keyword}")) return
        if (e.words.size == 1) {
            val first = e.words[0].text[0]
            singles.getOrPut(first) { ArrayList() }.add(e)
        } else {
            phrases.add(e)
        }
    }

    private fun isInflection(full: String, stem: String): Boolean {
        val suffix = full.substring(stem.length)
        return suffix.length <= 5
    }

    /**
     * Best hit per owner (null when an owner has no hit). [masked] tokens are ignored.
     */
    fun match(query: List<QueryWord>, masked: BooleanArray?): Array<KeywordHit?> {
        val best = arrayOfNulls<KeywordHit>(ownerCount)
        for (i in query.indices) {
            if (masked != null && masked[i]) continue
            val w = query[i].word
            if (w.text.isEmpty()) continue
            val bucket = singles[w.text[0]] ?: continue
            // A word that is itself a known keyword (or a plain real word) must not also be read as a typo of another one.
            var knownWord = w.text in strictWords
            if (!knownWord) {
                for (e in bucket) {
                    if (WordMatch.quality(w, e.words[0]) >= WordMatch.STRONG) {
                        knownWord = true
                        break
                    }
                }
            }
            for (e in bucket) {
                val q = WordMatch.quality(w, e.words[0])
                if (q <= 0f) continue
                if (knownWord && q < WordMatch.STRONG) continue
                val score = if (e.weak) q * WEAK_FACTOR else q
                val hit = KeywordHit(e.owner, score, q, e.keyword, 1, e.keyword.length, intArrayOf(i))
                if (hit.beats(best[e.owner])) best[e.owner] = hit
            }
        }
        for (e in phrases) {
            val hit = matchPhrase(e, query, masked) ?: continue
            if (hit.beats(best[e.owner])) best[e.owner] = hit
        }
        return best
    }

    private fun matchPhrase(e: Entry, query: List<QueryWord>, masked: BooleanArray?): KeywordHit? {
        val used = IntArray(e.words.size)
        var minQuality = 1f
        for (j in e.words.indices) {
            var bestIdx = -1
            var bestQ = 0f
            for (i in query.indices) {
                if (masked != null && masked[i]) continue
                var taken = false
                for (u in 0 until j) if (used[u] == i) taken = true
                if (taken) continue
                val q = WordMatch.quality(query[i].word, e.words[j])
                if (q > bestQ) {
                    bestQ = q
                    bestIdx = i
                }
            }
            if (bestIdx < 0) return null
            used[j] = bestIdx
            if (bestQ < minQuality) minQuality = bestQ
        }
        var inOrder = true
        var contiguous = true
        for (j in 1 until used.size) {
            if (used[j] < used[j - 1]) inOrder = false
            if (used[j] != used[j - 1] + 1) contiguous = false
        }
        val orderFactor = if (contiguous) 1f else if (inOrder) 0.97f else 0.92f
        val weak = e.weak
        val score = minQuality * orderFactor * (if (weak) WEAK_FACTOR else 1f)
        return KeywordHit(e.owner, score, minQuality, e.keyword, e.words.size, e.keyword.length, used)
    }

    companion object {
        const val WEAK_FACTOR = 0.6f
    }
}
