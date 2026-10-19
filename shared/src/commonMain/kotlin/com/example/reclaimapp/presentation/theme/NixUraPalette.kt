package com.example.reclaimapp.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Official NixUra brand colors. Application-wide foundation; habit cards interpolate across these stops.
 */
object NixUraPalette {
    val Navy = Color(0xFF1D1A39)
    val Purple = Color(0xFF451952)
    val Plum = Color(0xFF862549)
    val Rose = Color(0xFFAE445A)
    val Orange = Color(0xFFF39F5A)
    val Blush = Color(0xFFE8BCB9)

    /** Ordered dark → light for streak progression. */
    val streakStops: List<Color> = listOf(Navy, Purple, Plum, Rose, Orange, Blush)

    /** Semantic exception for errors — documented, not part of the six-color brand set. */
    val Error = Color(0xFFB3261E)

    val onDarkPrimary = Blush
    val onDarkMuted = Blush.copy(alpha = 0.72f)
    val onLightPrimary = Navy
    val onLightMuted = Plum.copy(alpha = 0.85f)
}
