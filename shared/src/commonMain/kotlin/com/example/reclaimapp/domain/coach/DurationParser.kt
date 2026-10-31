package com.example.reclaimapp.domain.coach

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/** A duration found in free text, with the span it occupied (`start` inclusive, `end` exclusive, indexes of the original text). */
data class ParsedDuration(val duration: Duration, val start: Int, val end: Int)

/**
 * Reads natural-language durations from free text (English, Arabic, Egyptian Arabic, mixed), forgiving about typos
 * in the unit word. Pure Kotlin, no state.
 *
 * Understood: "6 hours", "6h", "90 minutes", "a day", "half a day", "a day and a half", "two weeks", "a month"
 * (30 days), "3d", "1.5 days", "2 days 3 hours", "1h30m", ranges ("3-5 days", "3 to 5 days": the lower bound is used),
 * number words, Arabic ("3 ساعات", "يومين", "٣ أيام", "اسبوع", "شهر", "نص يوم", "يوم ونص") and typos such as "housr", "wekk".
 *
 * Rejected (returns `null`): zero, negative, longer than 10 years, bare numbers without a unit, seconds.
 * A single-letter unit ("h", "d", "w", "m", "y") only counts when it is glued to the number ("3d"); a bare "m" is minutes
 * ("mo" is months). A bare English unit without a number or article ("every day", "per week") is not a duration.
 */
object DurationParser {

    val MAX_DURATION: Duration = 3650.days

    fun parse(text: String): Duration? = find(text)?.duration

    fun find(text: String): ParsedDuration? {
        if (text.isBlank()) return null
        val tokens = tokenize(text)
        var i = 0
        while (i < tokens.size) {
            val match = tryMatch(tokens, i)
            if (match != null) return match.parsed
            i++
        }
        return null
    }

    // ---------- Tokens ----------

    private class Tok(
        val isNumber: Boolean,
        val text: String,
        val value: Double,
        val start: Int,
        val end: Int,
        /** True when no whitespace separates this token from the previous one ("3d"). */
        val glued: Boolean,
        /** True when a dash sits between the previous token and this one ("3-5"). */
        val dashBefore: Boolean,
        /** True for a number written with a leading minus ("-3"). */
        val negative: Boolean,
    )

    private fun tokenize(text: String): List<Tok> {
        val out = ArrayList<Tok>()
        var i = 0
        var prevEnd = -1
        while (i < text.length) {
            val ch = text[i]
            if (digitValue(ch) >= 0) {
                val start = i
                val sb = StringBuilder()
                while (i < text.length) {
                    val c = text[i]
                    val d = digitValue(c)
                    if (d >= 0) {
                        sb.append(('0' + d))
                        i++
                    } else if ((c == '.' || c == ',' || c == '٫' || c == '،') && i + 1 < text.length && digitValue(text[i + 1]) >= 0 && sb.isNotEmpty()) {
                        // "1.5" and "1,5" are decimals; "1,000" is a thousands separator.
                        val digitsAfter = countDigits(text, i + 1)
                        if (c == ',' && digitsAfter == 3) {
                            i++
                        } else {
                            sb.append('.')
                            i++
                        }
                    } else {
                        break
                    }
                }
                val value = sb.toString().toDoubleOrNull() ?: 0.0
                val negative = start > 0 && isMinus(text[start - 1]) && (start == 1 || !text[start - 2].isLetterOrDigit())
                out += Tok(true, sb.toString(), value, start, i, glued(text, prevEnd, start), hasDash(text, prevEnd, start) && !negative, negative)
                prevEnd = i
            } else if (isWordChar(ch)) {
                val start = i
                val sb = StringBuilder()
                while (i < text.length && (isWordChar(text[i]) || isArabicMark(text[i]))) {
                    if (!isArabicMark(text[i])) sb.append(text[i])
                    i++
                }
                out += Tok(false, normWord(sb.toString()), 0.0, start, i, glued(text, prevEnd, start), hasDash(text, prevEnd, start), false)
                prevEnd = i
            } else {
                i++
            }
        }
        return out
    }

    private fun countDigits(text: String, from: Int): Int {
        var n = 0
        var i = from
        while (i < text.length && digitValue(text[i]) >= 0) { n++; i++ }
        return n
    }

    private fun glued(text: String, prevEnd: Int, start: Int) = prevEnd >= 0 && prevEnd == start
    private fun hasDash(text: String, prevEnd: Int, start: Int): Boolean {
        if (prevEnd < 0 || start <= prevEnd) return false
        for (k in prevEnd until start) if (isMinus(text[k])) return true
        return false
    }

    private fun isMinus(c: Char) = c == '-' || c == '–' || c == '—' || c == '−'
    private fun isWordChar(c: Char) = c.isLetter()
    private fun isArabicMark(c: Char): Boolean = c.code in 0x064B..0x065F || c.code == 0x0670 || c.code == 0x0640

    private fun digitValue(c: Char): Int = when (c.code) {
        in 0x30..0x39 -> c.code - 0x30
        in 0x0660..0x0669 -> c.code - 0x0660
        in 0x06F0..0x06F9 -> c.code - 0x06F0
        else -> -1
    }

    /** Lowercase and fold the Arabic spelling variants ("أيام" and "ايام" are the same word). */
    private fun normWord(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (c in raw.lowercase()) {
            sb.append(
                when (c) {
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                    'ى' -> 'ي'
                    'ة' -> 'ه'
                    else -> c
                },
            )
        }
        return sb.toString()
    }

    // ---------- Vocabulary ----------

    private class UnitInfo(val millis: Double, val multiplier: Int = 1, val singleLetter: Boolean = false, val arabic: Boolean = false)

    private const val MIN = 60_000.0
    private const val HOUR = 3_600_000.0
    private const val DAY = 86_400_000.0
    private const val WEEK = 7 * DAY
    private const val MONTH = 30 * DAY
    private const val YEAR = 365 * DAY

    private val units: Map<String, UnitInfo> = buildMap<String, UnitInfo> {
        fun en(millis: Double, vararg words: String) = words.forEach { put(it, UnitInfo(millis)) }
        en(MIN, "minute", "minutes", "min", "mins", "mn")
        en(HOUR, "hour", "hours", "hr", "hrs")
        en(DAY, "day", "days", "dy", "dys")
        en(WEEK, "week", "weeks", "wk", "wks")
        en(MONTH, "month", "months", "mo", "mos", "mth", "mths")
        en(YEAR, "year", "years", "yr", "yrs")
        put("m", UnitInfo(MIN, singleLetter = true))
        put("h", UnitInfo(HOUR, singleLetter = true))
        put("d", UnitInfo(DAY, singleLetter = true))
        put("w", UnitInfo(WEEK, singleLetter = true))
        put("y", UnitInfo(YEAR, singleLetter = true))
        fun ar(millis: Double, singular: List<String>, dual: List<String>, plural: List<String>) {
            singular.forEach { put(normWord(it), UnitInfo(millis, 1, arabic = true)) }
            dual.forEach { put(normWord(it), UnitInfo(millis, 2, arabic = true)) }
            plural.forEach { put(normWord(it), UnitInfo(millis, 1, arabic = true)) }
        }
        ar(MIN, listOf("دقيقة"), listOf("دقيقتين", "دقيقتان"), listOf("دقائق", "دقايق"))
        ar(HOUR, listOf("ساعة"), listOf("ساعتين", "ساعتان"), listOf("ساعات"))
        ar(DAY, listOf("يوم"), listOf("يومين", "يومان"), listOf("أيام", "ايام"))
        ar(WEEK, listOf("أسبوع", "اسبوع"), listOf("أسبوعين", "اسبوعين", "أسبوعان"), listOf("أسابيع", "اسابيع"))
        ar(MONTH, listOf("شهر"), listOf("شهرين", "شهران"), listOf("أشهر", "اشهر", "شهور"))
        ar(YEAR, listOf("سنة", "عام"), listOf("سنتين", "عامين", "سنتان"), listOf("سنين", "سنوات", "أعوام", "اعوام"))
    }

    /** Unit words that may be typed with a typo. */
    private val fuzzyUnitWords: List<String> = listOf("minute", "minutes", "hour", "hours", "days", "week", "weeks", "month", "months", "year", "years")

    /** Real words one edit away from a unit that must not be read as a typo of it ("3 dogs" is not 3 days). */
    private val notUnitTypos = setOf(
        "your", "yours", "weak", "weed", "mouth", "mouths", "moth", "dear", "hear", "heard", "yeah", "yearn",
        "dogs", "dads", "mind", "mine", "minds", "wheel", "monty",
    )

    private val numberWords: Map<String, Int> = buildMap<String, Int> {
        val ones = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
        ones.forEachIndexed { i, w -> put(w, i) }
        put("twenty", 20); put("thirty", 30); put("forty", 40); put("fifty", 50)
        put("sixty", 60); put("seventy", 70); put("eighty", 80); put("ninety", 90); put("hundred", 100)
        fun ar(value: Int, vararg words: String) = words.forEach { put(normWord(it), value) }
        ar(1, "واحد", "واحدة", "وحدة")
        ar(2, "اتنين", "اثنين", "اثنان", "إثنين")
        ar(3, "ثلاث", "ثلاثة", "تلات", "تلاتة", "تلاته")
        ar(4, "أربع", "أربعة", "اربع", "اربعة")
        ar(5, "خمس", "خمسة")
        ar(6, "ست", "ستة")
        ar(7, "سبع", "سبعة")
        ar(8, "ثمان", "ثمانية", "تمان", "تمانية", "تمن", "ثماني")
        ar(9, "تسع", "تسعة")
        ar(10, "عشر", "عشرة")
        ar(20, "عشرين")
        ar(30, "ثلاثين", "تلاتين")
        ar(40, "أربعين", "اربعين")
        ar(50, "خمسين")
        ar(60, "ستين")
        ar(90, "تسعين")
    }

    private val tensWords = setOf("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
    private val articles = setOf("a", "an")
    private val halfWords = setOf("half", normWord("نص"), normWord("نصف"))
    private val andWords = setOf("and", "و", "plus")
    private val rangeWords = setOf("to", "or", "till", "until", "الي", "او", "لحد")

    /** A bare "a day" right after one of these is a frequency ("3 times a day"), not a duration. */
    private val frequencyBefore = setOf("times", "time", "once", "twice", "per", "every", "each", "x", "كل", "مره", "مرات", "مرتين")

    // ---------- Matching ----------

    private class Match(val parsed: ParsedDuration)

    private class Part(val value: Double, val unit: UnitInfo, val next: Int, val start: Int, val end: Int)

    private fun tryMatch(t: List<Tok>, i: Int): Match? {
        if (t[i].negative) return null
        val first = readPart(t, i) ?: return null
        var totalMillis = first.value * first.unit.millis
        var end = first.end
        var lastUnit = first.unit.millis
        var next = first.next
        // Compound: "2 days 3 hours", "1d 6h", "1 week and 2 days", "1h30m".
        while (next < t.size) {
            var k = next
            while (k < t.size && !t[k].isNumber && t[k].text in andWords) k++
            if (k >= t.size || !t[k].isNumber || t[k].negative) break
            val part = readPart(t, k) ?: break
            if (part.unit.millis >= lastUnit) break
            totalMillis += part.value * part.unit.millis
            lastUnit = part.unit.millis
            end = part.end
            next = part.next
        }
        if (totalMillis < MIN || totalMillis.isNaN()) return null
        val duration = totalMillis.toLong().milliseconds
        if (duration > MAX_DURATION) return null
        return Match(ParsedDuration(duration, first.start, end))
    }

    /** Reads "<number> <unit>", "a <unit>", "half a <unit>", "<unit> and a half", or a bare Arabic unit, starting at [i]. */
    private fun readPart(t: List<Tok>, i: Int): Part? {
        val tok = t[i]
        var value: Double
        var pos: Int
        var viaArticle = false

        if (tok.isNumber) {
            value = tok.value
            pos = i + 1
            // Range: "3-5 days", "3 to 5 days" (lower bound is used).
            if (pos < t.size && t[pos].isNumber && t[pos].dashBefore) {
                value = minOf(value, t[pos].value); pos++
            } else if (pos + 1 < t.size && !t[pos].isNumber && t[pos].text in rangeWords && t[pos + 1].isNumber) {
                value = minOf(value, t[pos + 1].value); pos += 2
            }
        } else {
            val word = tok.text
            val nw = numberWordAt(t, i)
            when {
                nw != null -> { value = nw.first.toDouble(); pos = nw.second }
                word in halfWords -> {
                    // "half a day", "half an hour", "half day", "نص يوم"
                    var k = i + 1
                    if (k < t.size && !t[k].isNumber && t[k].text == "of") k++
                    if (k < t.size && !t[k].isNumber && t[k].text in articles) k++
                    val u = unitAt(t, k) ?: return null
                    return Part(0.5, u.first, k + 1, tok.start, t[k].end)
                }
                word in articles || word == "couple" -> {
                    if (i > 0 && !t[i - 1].isNumber && t[i - 1].text in frequencyBefore) return null
                    var k = i + 1
                    if (word == "couple") {
                        if (k < t.size && !t[k].isNumber && t[k].text == "of") k++
                        value = 2.0
                    } else {
                        value = 1.0
                        viaArticle = true
                    }
                    pos = k
                }
                else -> {
                    // A bare Arabic unit: "يومين", "اسبوع", "شهر" (but not "كل يوم" = every day).
                    val u = unitAt(t, i) ?: return null
                    if (!u.first.arabic) return null
                    if (i > 0 && !t[i - 1].isNumber && t[i - 1].text in frequencyBefore) return null
                    var millisValue = u.first.multiplier.toDouble()
                    var end = tok.end
                    var nxt = i + 1
                    val half = halfAt(t, nxt)
                    if (half != null && u.first.multiplier == 1) { millisValue += 0.5; end = t[half - 1].end; nxt = half }
                    return Part(millisValue, u.first, nxt, tok.start, end)
                }
            }
        }

        // Optional "and a half": "2 and a half days".
        var halfBefore = false
        run {
            var k = pos
            if (k < t.size && !t[k].isNumber && t[k].text in andWords) {
                k++
                if (k < t.size && !t[k].isNumber && t[k].text in articles) k++
                if (k < t.size && !t[k].isNumber && t[k].text in halfWords) { halfBefore = true; pos = k + 1 }
            } else if (k < t.size && !t[k].isNumber && t[k].text.length > 1 && t[k].text.startsWith("و") && t[k].text.substring(1) in halfWords) {
                halfBefore = true; pos = k + 1
            }
        }

        val unitPos = pos
        val u = unitAt(t, unitPos) ?: return null
        if (viaArticle && u.first.arabic) return null
        var total = value
        if (halfBefore) total += 0.5
        var end = t[unitPos].end
        var nxt = unitPos + 1
        // "a day and a half", "يوم ونص"
        if (!halfBefore) {
            val half = halfAt(t, nxt)
            if (half != null) { total += 0.5; end = t[half - 1].end; nxt = half }
        }
        return Part(total, u.first, nxt, tok.start, end)
    }

    /** If tokens at [i] spell "and a half" / "ونص", the index after them. */
    private fun halfAt(t: List<Tok>, i: Int): Int? {
        if (i >= t.size || t[i].isNumber) return null
        val w = t[i].text
        if (w.length > 1 && w.startsWith("و") && w.substring(1) in halfWords) return i + 1
        if (w in andWords) {
            var k = i + 1
            if (k < t.size && !t[k].isNumber && t[k].text in articles) k++
            if (k < t.size && !t[k].isNumber && t[k].text in halfWords) return k + 1
        }
        return null
    }

    private fun numberWordAt(t: List<Tok>, i: Int): Pair<Int, Int>? {
        if (t[i].isNumber) return null
        val v = numberWords[t[i].text] ?: return null
        if (t[i].text in tensWords && i + 1 < t.size && !t[i + 1].isNumber) {
            val ones = numberWords[t[i + 1].text]
            if (ones != null && ones in 1..9) return Pair(v + ones, i + 2)
        }
        return Pair(v, i + 1)
    }

    /** Unit at token [i]. Single letters count only when glued to the number before them ("3d"). */
    private fun unitAt(t: List<Tok>, i: Int): Pair<UnitInfo, Int>? {
        if (i >= t.size || t[i].isNumber) return null
        val tok = t[i]
        val w = tok.text
        val exact = units[w]
        if (exact != null) {
            if (exact.singleLetter && !(tok.glued && i > 0 && t[i - 1].isNumber)) return null
            return Pair(exact, i)
        }
        // Arabic prefixes: "لاسبوع", "ويومين", "بيومين".
        if (w.length >= 4 && w[0] in "ولب") {
            val stripped = units[w.substring(1)]
            if (stripped != null && stripped.arabic) return Pair(stripped, i)
        }
        // Typos of English unit words ("housr", "wekk", "monthss").
        if (w.length >= 4 && w[0].code < 0x0600 && w !in notUnitTypos) {
            val limit = if (w.length >= 8) 2 else 1
            var best: String? = null
            var bestDistance = limit + 1
            for (cand in fuzzyUnitWords) {
                if (kotlin.math.abs(cand.length - w.length) > limit) continue
                if (cand[0] != w[0] && cand.length < 6) continue
                val d = distance(w, cand)
                if (d < bestDistance) { bestDistance = d; best = cand }
            }
            if (best != null) return Pair(units.getValue(best), i)
        }
        return null
    }

    /** Damerau-Levenshtein (optimal string alignment): a swap of two neighbours costs 1. */
    private fun distance(a: String, b: String): Int {
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
