package com.example.reclaimapp.widgets

/**
 * Widget colors. Mirror `DarkNixColors` / `LightNixColors` (good-habit and event cards follow the app theme) and
 * `NixUraPalette.onDark*` / `onLight*` (quit card text, which depends on the streak shade, not on the theme).
 * Colors that need transparency are split into an opaque `*` value plus an `*Alpha` (0..255) because the renderer
 * tints white shapes with `setColorFilter` and fades them with `setImageAlpha`.
 */
internal data class WidgetColors(
    val surface: Int,
    val surfaceRaised: Int,
    val border: Int,
    val borderAlpha: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val textMuted: Int,
    val accent: Int,
    val accentWarm: Int,
    val onAccent: Int,
    val positive: Int,
    val positiveSoftAlpha: Int,
)

internal object WidgetTheme {
    private val Dark = WidgetColors(
        surface = 0xFF262248.toInt(),
        surfaceRaised = 0xFF312C5A.toInt(),
        border = 0xFFFFFFFF.toInt(),
        borderAlpha = 0x1F,
        textPrimary = 0xFFF6EFEE.toInt(),
        textSecondary = 0xFFD6CCD6.toInt(),
        textMuted = 0xFF9B94B0.toInt(),
        accent = 0xFFAE445A.toInt(),
        accentWarm = 0xFFF39F5A.toInt(),
        onAccent = 0xFFFFF7F5.toInt(),
        positive = 0xFF4CC38A.toInt(),
        positiveSoftAlpha = 46, // 0.18
    )
    private val Light = WidgetColors(
        surface = 0xFFFFFFFF.toInt(),
        surfaceRaised = 0xFFF4EEEC.toInt(),
        border = 0xFF1D1A39.toInt(),
        borderAlpha = 0x1F,
        textPrimary = 0xFF1D1A39.toInt(),
        textSecondary = 0xFF4A4560.toInt(),
        textMuted = 0xFF827E88.toInt(),
        accent = 0xFF862549.toInt(),
        accentWarm = 0xFFD9772B.toInt(),
        onAccent = 0xFFFFF7F5.toInt(),
        positive = 0xFF2F9E6B.toInt(),
        positiveSoftAlpha = 41, // 0.16
    )

    fun of(darkTheme: Boolean): WidgetColors = if (darkTheme) Dark else Light

    // Quit card text, from NixUraPalette (light text on dark shades, dark text on light shades).
    const val ON_DARK_CARD_PRIMARY = 0xFFE8BCB9.toInt()
    const val ON_DARK_CARD_MUTED = 0xFFE8BCB9.toInt() // drawn with alpha 0.72 -> see MUTED_ON_DARK_ALPHA
    const val ON_LIGHT_CARD_PRIMARY = 0xFF1D1A39.toInt()
    const val ON_LIGHT_CARD_MUTED = 0xFF862549.toInt() // drawn with alpha 0.85
    const val MUTED_ON_DARK_ALPHA = 184
    const val MUTED_ON_LIGHT_ALPHA = 217

    fun withAlpha(color: Int, alpha: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)
}
