package com.example.reclaimapp.data.integration

import com.example.reclaimapp.domain.time.CivilCalendar

/** Parses the date formats calendar APIs send, without a time-zone library. */
internal object Rfc3339 {
    private val dateTime = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?(Z|[+-]\d{2}:\d{2})$""")
    private val date = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")

    /** "2026-11-05T09:30:00+02:00" / "...Z" → epoch millis; `null` when it is not such a timestamp. */
    fun parseInstant(text: String): Long? {
        val m = dateTime.matchEntire(text.trim()) ?: return null
        val g = m.groupValues
        val day = CivilCalendar.dayIndexFromCivil(g[1].toInt(), g[2].toInt(), g[3].toInt())
        val millisOfDay = g[4].toLong() * 3_600_000 + g[5].toLong() * 60_000 + (g[6].toLongOrNull() ?: 0) * 1_000 +
            (g[7].takeIf { it.isNotEmpty() }?.padEnd(3, '0')?.take(3)?.toLong() ?: 0)
        val offset = when (val z = g[8]) {
            "Z" -> 0L
            else -> {
                val sign = if (z[0] == '-') -1 else 1
                sign * (z.substring(1, 3).toLong() * 3_600_000 + z.substring(4, 6).toLong() * 60_000)
            }
        }
        return day * 86_400_000 + millisOfDay - offset
    }

    /** "2026-11-05" → local day index; `null` otherwise. */
    fun parseDayIndex(text: String): Long? {
        val m = date.matchEntire(text.trim()) ?: return null
        val (y, mo, d) = m.destructured
        val month = mo.toInt()
        val dayOfMonth = d.toInt()
        if (month !in 1..12 || dayOfMonth !in 1..CivilCalendar.monthLength(y.toInt(), month)) return null
        return CivilCalendar.dayIndexFromCivil(y.toInt(), month, dayOfMonth)
    }

    /** Epoch millis → "2026-11-05T07:30:00Z", for query parameters. */
    fun formatUtc(epochMillis: Long): String {
        val day = epochMillis.floorDiv(86_400_000L)
        val ms = epochMillis.mod(86_400_000L)
        val c = CivilCalendar.civilFromDayIndex(day)
        fun p(v: Long) = v.toString().padStart(2, '0')
        return "${c.year}-${p(c.month.toLong())}-${p(c.day.toLong())}T${p(ms / 3_600_000)}:${p(ms / 60_000 % 60)}:${p(ms / 1_000 % 60)}Z"
    }
}
