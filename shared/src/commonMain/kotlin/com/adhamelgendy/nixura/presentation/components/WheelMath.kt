package com.adhamelgendy.nixura.presentation.components

/** Which number is on which row of a [NumberWheel]. A looping wheel repeats its numbers, starting in the middle. */
internal class WheelMath(private val range: IntRange, private val loop: Boolean) {
    private val size = range.last - range.first + 1
    private val repeats = if (loop) LOOP_REPEATS else 1

    val rowCount: Int = size * repeats

    fun valueAt(row: Int): Int = range.first + row.coerceIn(0, rowCount - 1) % size

    /** Where [value] first shows: in the middle repeat, so there is room to turn either way. */
    fun startRowOf(value: Int): Int = size * (repeats / 2) + value.coerceIn(range) - range.first

    /** The row of [value] closest to [row], so a looping wheel takes the short way (59 to 00 is one step). */
    fun nearestRowOf(value: Int, row: Int): Int {
        val offset = value.coerceIn(range) - range.first
        if (!loop) return offset
        val turns = (row - offset + size / 2).floorDiv(size).coerceIn(0, repeats - 1)
        return turns * size + offset
    }

    private companion object {
        const val LOOP_REPEATS = 1000
    }
}

/** A time of day as the 12-hour picker shows it. */
internal data class ClockSelection(val hour12: Int, val minute: Int, val pm: Boolean) {
    val minutesOfDay: Int get() = ((hour12 % 12) + if (pm) 12 else 0) * 60 + minute

    companion object {
        fun of(minutesOfDay: Int): ClockSelection {
            val safe = minutesOfDay.coerceIn(0, 24 * 60 - 1)
            val hour24 = safe / 60
            return ClockSelection((hour24 % 12).let { if (it == 0) 12 else it }, safe % 60, hour24 >= 12)
        }
    }
}
