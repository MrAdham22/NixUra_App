package com.example.reclaimapp.presentation.util

import kotlin.time.Duration

/**
 * Formats elapsed streak time for display.
 *
 * - Under 1 hour: `MM:SS`
 * - 1 hour until 1 day: `HH:MM:SS`
 * - 1 day or more: `Dd HH:MM:SS`
 */
fun formatElapsed(duration: Duration): String {
    val totalSeconds = duration.coerceAtLeast(Duration.ZERO).inWholeSeconds

    val days = totalSeconds / SECONDS_PER_DAY
    val remainderAfterDays = totalSeconds % SECONDS_PER_DAY
    val hours = remainderAfterDays / SECONDS_PER_HOUR
    val remainderAfterHours = remainderAfterDays % SECONDS_PER_HOUR
    val minutes = remainderAfterHours / SECONDS_PER_MINUTE
    val seconds = remainderAfterHours % SECONDS_PER_MINUTE

    return when {
        days >= 1 -> "${days}d ${twoDigits(hours)}:${twoDigits(minutes)}:${twoDigits(seconds)}"
        hours >= 1 -> "${twoDigits(hours)}:${twoDigits(minutes)}:${twoDigits(seconds)}"
        else -> "${twoDigits(minutes)}:${twoDigits(seconds)}"
    }
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR

private fun twoDigits(value: Long): String = value.toString().padStart(2, '0')
