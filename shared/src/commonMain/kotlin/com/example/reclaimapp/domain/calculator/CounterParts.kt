package com.example.reclaimapp.domain.calculator

import kotlin.time.Duration

/** A duration broken into display units for counters (progressive or countdown). */
data class CounterParts(
    val days: Long,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
) {
    /** Units to show: from the largest non-zero unit down to seconds (always at least minutes + seconds). */
    val visibleUnits: List<CounterUnit>
        get() = when {
            days >= 1 -> listOf(CounterUnit.DAYS, CounterUnit.HOURS, CounterUnit.MINUTES, CounterUnit.SECONDS)
            hours >= 1 -> listOf(CounterUnit.HOURS, CounterUnit.MINUTES, CounterUnit.SECONDS)
            else -> listOf(CounterUnit.MINUTES, CounterUnit.SECONDS)
        }

    fun valueOf(unit: CounterUnit): Long = when (unit) {
        CounterUnit.DAYS -> days
        CounterUnit.HOURS -> hours.toLong()
        CounterUnit.MINUTES -> minutes.toLong()
        CounterUnit.SECONDS -> seconds.toLong()
    }
}

enum class CounterUnit { DAYS, HOURS, MINUTES, SECONDS }

fun Duration.toCounterParts(): CounterParts {
    val total = coerceAtLeast(Duration.ZERO).inWholeSeconds
    return CounterParts(
        days = total / 86_400L,
        hours = ((total % 86_400L) / 3_600L).toInt(),
        minutes = ((total % 3_600L) / 60L).toInt(),
        seconds = (total % 60L).toInt(),
    )
}
