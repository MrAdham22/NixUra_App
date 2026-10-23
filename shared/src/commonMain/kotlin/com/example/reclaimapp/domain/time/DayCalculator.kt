package com.example.reclaimapp.domain.time

/** Supplies the local UTC offset (including DST) at an instant. Implemented per platform/data layer. */
fun interface TimeZoneOffsetProvider {
    fun offsetMillisAt(epochMillis: Long): Long
}

data class CivilDate(val year: Int, val month: Int, val day: Int) {
    val monthLength: Int get() = CivilCalendar.monthLength(year, month)
}

/** Pure proleptic Gregorian calendar math (no time zones). Day index = days since 1970-01-01. */
object CivilCalendar {
    fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    fun monthLength(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if (isLeapYear(year)) 29 else 28
    }

    fun civilFromDayIndex(dayIndex: Long): CivilDate {
        val z = dayIndex + 719468L
        val era = (if (z >= 0) z else z - 146096L) / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
        val y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val d = doy - (153L * mp + 2L) / 5L + 1L
        val m = if (mp < 10L) mp + 3L else mp - 9L
        val year = (if (m <= 2L) y + 1L else y).toInt()
        return CivilDate(year, m.toInt(), d.toInt())
    }

    fun dayIndexFromCivil(year: Int, month: Int, day: Int): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = (if (y >= 0) y else y - 399L) / 400L
        val yoe = y - era * 400L
        val mp = (month + if (month > 2) -3 else 9).toLong()
        val doy = (153L * mp + 2L) / 5L + day - 1L
        val doe = yoe * 365L + yoe / 4L - yoe / 100L + doy
        return era * 146097L + doe - 719468L
    }

    /** ISO weekday: Monday = 1 … Sunday = 7. */
    fun isoWeekday(dayIndex: Long): Int = ((dayIndex + 3L).mod(7L)).toInt() + 1

    fun addMonths(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val total = year * 12 + (month - 1) + delta
        return total.floorDiv(12) to (total.mod(12) + 1)
    }
}

class DayCalculator(private val offsets: TimeZoneOffsetProvider) {

    fun dayIndexOf(epochMillis: Long): Long =
        (epochMillis + offsets.offsetMillisAt(epochMillis)).floorDiv(MILLIS_PER_DAY)

    /** Millisecond of day (0 until 86_400_000) in local time. */
    fun millisOfDay(epochMillis: Long): Long =
        (epochMillis + offsets.offsetMillisAt(epochMillis)).mod(MILLIS_PER_DAY)

    /** UTC epoch millis of local midnight that starts [dayIndex]. */
    fun startOfDayMillis(dayIndex: Long): Long = epochMillisOfLocal(dayIndex * MILLIS_PER_DAY)

    /** Convert a local wall-clock instant (millis since local epoch) to UTC epoch millis. */
    fun epochMillisOfLocal(localMillis: Long): Long {
        val guess = localMillis - offsets.offsetMillisAt(localMillis)
        return localMillis - offsets.offsetMillisAt(guess)
    }

    fun epochMillisOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val dayIndex = CivilCalendar.dayIndexFromCivil(year, month, day)
        return epochMillisOfLocal(dayIndex * MILLIS_PER_DAY + hour * 3_600_000L + minute * 60_000L)
    }

    companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
