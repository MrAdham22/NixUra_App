package com.example.reclaimapp.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colors for the NixUra UI. Dark is the primary theme; light is the alternative. */
data class NixColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentWarm: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val positive: Color,
    val positiveSoft: Color,
    val danger: Color,
)

val DarkNixColors = NixColors(
    isDark = true,
    background = NixUraPalette.Navy,
    surface = Color(0xFF262248),
    surfaceRaised = Color(0xFF312C5A),
    border = Color(0x1FFFFFFF),
    textPrimary = Color(0xFFF6EFEE),
    textSecondary = Color(0xFFD6CCD6),
    textMuted = Color(0xFF9B94B0),
    accent = NixUraPalette.Rose,
    accentWarm = NixUraPalette.Orange,
    accentSoft = NixUraPalette.Blush,
    onAccent = Color(0xFFFFF7F5),
    positive = Color(0xFF4CC38A),
    positiveSoft = Color(0xFF4CC38A).copy(alpha = 0.18f),
    danger = Color(0xFFFF6B6B),
)

val LightNixColors = NixColors(
    isDark = false,
    background = Color(0xFFFBF8F6),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF4EEEC),
    border = Color(0x1F1D1A39),
    textPrimary = NixUraPalette.Navy,
    textSecondary = Color(0xFF4A4560),
    textMuted = Color(0xFF827E88),
    accent = NixUraPalette.Plum,
    accentWarm = Color(0xFFD9772B),
    accentSoft = NixUraPalette.Blush,
    onAccent = Color(0xFFFFF7F5),
    positive = Color(0xFF2F9E6B),
    positiveSoft = Color(0xFF2F9E6B).copy(alpha = 0.16f),
    danger = Color(0xFFB3261E),
)

val LocalNixColors = staticCompositionLocalOf { DarkNixColors }

object Nix {
    val colors: NixColors
        @Composable @ReadOnlyComposable get() = LocalNixColors.current
}

@Composable
fun ProvideNixColors(colors: NixColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNixColors provides colors, content = content)
}
