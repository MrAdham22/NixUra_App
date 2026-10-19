package com.example.reclaimapp.presentation.util

import kotlin.time.Duration

fun formatElapsed(duration: Duration): String {
    if (duration <= Duration.ZERO) return "0m"

    val totalMinutes = duration.inWholeMinutes
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60

    return when {
        days > 0 -> buildString {
            append("${days}d")
            if (hours > 0) append(" ${hours}h")
        }
        hours > 0 -> buildString {
            append("${hours}h")
            if (minutes > 0) append(" ${minutes}m")
        }
        else -> "${minutes}m"
    }
}
