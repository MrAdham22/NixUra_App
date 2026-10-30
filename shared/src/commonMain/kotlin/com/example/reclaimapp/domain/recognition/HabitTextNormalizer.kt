package com.example.reclaimapp.domain.recognition

/** Which way the wording of a habit name leans ("quit smoking" leans BAD, "daily walk" leans GOOD). */
enum class IntentLean { NONE, BAD, GOOD }

/**
 * The result of [HabitTextNormalizer.analyze].
 *
 * @property normalized full normalized text, intent wrappers included.
 * @property coreTokens the tokens left once intent wrappers and filler words are removed.
 * @property intent lean of the wrappers that were removed.
 * @property hasSkipVerb true when the text contained a verb such as "skip" / "miss" / "forget".
 */
data class NormalizedHabitText(
    val normalized: String,
    val coreTokens: List<String>,
    val intent: IntentLean,
    val hasSkipVerb: Boolean,
) {
    /** "don't skip workout", "stop missing prayers": a negated skip verb means the object is a GOOD habit. */
    val isSkipNegated: Boolean get() = hasSkipVerb && intent == IntentLean.BAD

    val coreText: String get() = if (coreTokens.isEmpty()) normalized else coreTokens.joinToString(" ")
}

/**
 * Pure text clean-up used on every keystroke of the habit-name field. No platform APIs.
 *
 * Latin: lowercase, accents folded, apostrophes dropped, other punctuation/emoji/digits become spaces,
 * runs of 3+ identical letters collapse to one letter ("coooffee" -> "coffee"; legit doubles stay).
 * Arabic: tashkeel/tatweel removed, alef forms unified, alef-maqsura -> ya, ta-marbuta -> ha, hamza carriers unified.
 */
object HabitTextNormalizer {

    private const val DROP = '\u0000'

    private val latinFold: CharArray = buildLatinFold()

    fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length)
        var pendingSpace = false
        var last = DROP
        var run = 0
        for (ch in raw) {
            val c = foldChar(ch)
            if (c == DROP) continue
            if (c == ' ') {
                if (sb.isNotEmpty()) pendingSpace = true
                last = DROP
                run = 0
                continue
            }
            if (pendingSpace) {
                sb.append(' ')
                pendingSpace = false
            }
            if (c == last) {
                run++
                when {
                    run == 3 -> sb.setLength(sb.length - 1) // 3+ identical letters -> one letter
                    run > 3 -> Unit
                    else -> sb.append(c)
                }
            } else {
                last = c
                run = 1
                sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Collapses every run of identical letters to one ("coffee" -> "cofe"). Used only for comparing. */
    fun squeeze(word: String): String {
        if (word.length < 2) return word
        val sb = StringBuilder(word.length)
        var last = DROP
        for (ch in word) {
            if (ch != last) {
                sb.append(ch)
                last = ch
            }
        }
        return sb.toString()
    }

    fun analyze(raw: String): NormalizedHabitText {
        val normalized = normalize(raw)
        if (normalized.isEmpty()) return NormalizedHabitText("", emptyList(), IntentLean.NONE, false)
        val all = normalized.split(' ')
        val core = ArrayList<String>(all.size)
        var bad = 0
        var good = 0
        var skip = false
        var i = 0
        while (i < all.size) {
            val token = all[i]
            val phrases = Wrappers.byFirstWord[token]
            var consumed = 0
            var lean = IntentLean.NONE
            if (phrases != null) {
                for (p in phrases) {
                    if (i + p.words.size <= all.size && matchesAt(all, i, p.words)) {
                        consumed = p.words.size
                        lean = p.lean
                        break
                    }
                }
            }
            when {
                consumed > 0 -> {
                    if (lean == IntentLean.BAD) bad++ else good++
                    i += consumed
                }
                token in Wrappers.fillers -> i++
                token in Wrappers.skipVerbs -> {
                    skip = true
                    i++
                }
                else -> {
                    core.add(token)
                    i++
                }
            }
        }
        val lean = when {
            bad > 0 && good == 0 -> IntentLean.BAD
            good > 0 && bad == 0 -> IntentLean.GOOD
            else -> IntentLean.NONE
        }
        return NormalizedHabitText(normalized, core, lean, skip)
    }

    private fun matchesAt(all: List<String>, start: Int, words: List<String>): Boolean {
        for (k in words.indices) if (all[start + k] != words[k]) return false
        return true
    }

    private fun foldChar(ch: Char): Char {
        val code = ch.code
        // Arabic combining marks, tatweel, bidi / zero-width marks, apostrophes: dropped silently.
        if ((code in 0x064B..0x065F) || code == 0x0670 || (code in 0x06D6..0x06ED) || code == 0x0640) return DROP
        if (code == 0x200B || code == 0x200C || code == 0x200D || code == 0x200E || code == 0x200F || code == 0x00AD) return DROP
        if (ch == '\'' || ch == '\u2019' || ch == '\u2018' || ch == '`') return DROP
        if (code in 0x0600..0x06FF) {
            return when (ch) {
                '\u0623', '\u0625', '\u0622', '\u0671' -> '\u0627' // alef forms -> bare alef
                '\u0649' -> '\u064A' // alef maqsura -> ya
                '\u0629' -> '\u0647' // ta marbuta -> ha
                '\u0624' -> '\u0648' // hamza on waw -> waw
                '\u0626' -> '\u064A' // hamza on ya -> ya
                '\u06A9' -> '\u0643' // Persian kaf -> kaf
                '\u06CC' -> '\u064A' // Persian ya -> ya
                else -> if (ch.isLetter()) ch else ' ' // includes Arabic-Indic digits and punctuation
            }
        }
        if (!ch.isLetter()) return ' '
        val lower = ch.lowercaseChar()
        val idx = lower.code - 0xC0
        if (idx in latinFold.indices) return latinFold[idx]
        return lower
    }

    private fun buildLatinFold(): CharArray {
        val table = CharArray(0x180 - 0xC0) { (it + 0xC0).toChar() }
        // First char of each group is the folded result, the rest are the accented sources.
        val groups = listOf(
            "a\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5\u0101\u0103\u0105",
            "c\u00E7\u0107\u0109\u010B\u010D",
            "d\u010F\u0111",
            "e\u00E8\u00E9\u00EA\u00EB\u0113\u0115\u0117\u0119\u011B",
            "g\u011D\u011F\u0121\u0123",
            "i\u00EC\u00ED\u00EE\u00EF\u0129\u012B\u012D\u012F\u0131",
            "l\u013A\u013C\u013E\u0142",
            "n\u00F1\u0144\u0146\u0148",
            "o\u00F2\u00F3\u00F4\u00F5\u00F6\u00F8\u014D\u014F\u0151",
            "r\u0155\u0157\u0159",
            "s\u015B\u015D\u015F\u0161",
            "t\u0163\u0165",
            "u\u00F9\u00FA\u00FB\u00FC\u0169\u016B\u016D\u016F\u0171\u0173",
            "y\u00FD\u00FF",
            "z\u017A\u017C\u017E",
        )
        for (g in groups) {
            val target = g[0]
            for (k in 1 until g.length) {
                val idx = g[k].code - 0xC0
                if (idx in table.indices) table[idx] = target
            }
        }
        return table
    }

    // ---------------------------------------------------------------------------------------------
    // Intent wrappers and filler words (normalized once, on first use)
    // ---------------------------------------------------------------------------------------------

    internal class Wrapper(val words: List<String>, val lean: IntentLean)

    internal object Wrappers {
        val byFirstWord: Map<String, List<Wrapper>>
        val fillers: Set<String>
        val skipVerbs: Set<String>

        init {
            val all = ArrayList<Wrapper>()
            fun add(lean: IntentLean, phrases: List<String>) {
                for (p in phrases) {
                    val n = HabitTextNormalizer.normalize(p)
                    if (n.isNotEmpty()) all.add(Wrapper(n.split(' '), lean))
                }
            }
            add(IntentLean.BAD, HabitTextNormalizer.BAD_WRAPPERS)
            add(IntentLean.GOOD, HabitTextNormalizer.GOOD_WRAPPERS)
            // Longest phrase first, so "no more" wins over "no".
            all.sortByDescending { it.words.size }
            val grouped = HashMap<String, MutableList<Wrapper>>()
            for (w in all) grouped.getOrPut(w.words[0]) { ArrayList() }.add(w)
            byFirstWord = grouped
            fillers = HabitTextNormalizer.FILLERS.map { HabitTextNormalizer.normalize(it) }.filter { it.isNotEmpty() }.toHashSet()
            skipVerbs = HabitTextNormalizer.SKIP_VERBS.map { HabitTextNormalizer.normalize(it) }.filter { it.isNotEmpty() }.toHashSet()
        }
    }

    internal val BAD_WRAPPERS = listOf(
        "quit", "quitting", "stop", "stopping", "stops", "no more", "no", "not", "dont", "do not", "never",
        "avoid", "avoiding", "less", "fewer", "reduce", "reducing", "limit", "limiting", "without",
        "give up", "giving up", "gave up", "cut down", "cutting down", "cut back", "cutting back",
        "cut out", "cutting out", "kick", "kicking", "stay away from", "get rid of", "detox",
        // Arabic / Egyptian / Gulf
        "بطل", "بطلت", "ابطل", "ابطال", "اقلع", "اقلاع", "الاقلاع", "اقلعت", "ترك", "اترك", "تركت", "الترك",
        "اوقف", "توقف", "التوقف", "اقلل", "تقليل", "بلاش", "بدون", "من غير", "لا", "ممنوع", "ابتعد",
        "الابتعاد", "تجنب", "تجنبت", "مقاطعه", "قطع",
    )

    internal val GOOD_WRAPPERS = listOf(
        "start", "starting", "begin", "beginning", "build", "building", "develop", "developing", "more",
        "daily", "everyday", "every day", "each day", "once a day", "twice a day", "regularly", "regular",
        "improve", "increase", "keep", "maintain", "continue",
        // Arabic
        "ابدا", "بدايه", "البدايه", "يوميا", "يومي", "كل يوم", "اكثر", "زياده", "التزام", "الالتزام", "التزم",
        "استمر", "حافظ", "تحسين", "اهتم",
    )

    internal val SKIP_VERBS = listOf(
        "skip", "skipping", "skips", "miss", "missing", "forget", "forgetting", "neglect", "neglecting", "ignore", "ignoring",
        "تفوت", "فوت", "اهمل", "تهمل", "اتخطي", "تتخطي",
    )

    internal val FILLERS = listOf(
        "i", "im", "ive", "id", "ill", "am", "are", "is", "its", "it", "me", "my", "mine", "we", "you", "your",
        "the", "a", "an", "to", "of", "on", "in", "at", "for", "from", "by", "with", "and", "or", "be", "been",
        "want", "wanna", "need", "needs", "would", "like", "trying", "try", "tried", "going", "gonna", "will",
        "can", "could", "should", "must", "please", "really", "very", "so", "much", "too", "bit", "little",
        "lot", "lots", "some", "any", "habit", "habits", "bad", "good", "thing", "things", "stuff",
        // Arabic
        "انا", "عايز", "عاوز", "عايزه", "اريد", "ابي", "ابغي", "بدي", "نفسي", "محتاج", "ان", "انه", "في", "من",
        "الي", "علي", "عن", "عاده", "عادات", "سيئه", "سيئ", "ده", "دي", "هذا", "هذه", "شويه", "حاجه",
    )
}
