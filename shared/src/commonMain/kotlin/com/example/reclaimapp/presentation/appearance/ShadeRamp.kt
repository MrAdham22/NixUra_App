package com.example.reclaimapp.presentation.appearance

/**
 * Pure color math (ARGB longs, no Compose) for user-chosen habit colors.
 *
 * A custom card starts at an almost-darkest shade of the chosen color and moves through the color itself
 * to a light tint as the streak grows. It reuses [StreakProgression] segments so thresholds stay shared
 * with the default NixUra brand progression.
 */
object ShadeRamp {
    /** Brightness multiplier for the starting (darkest) shade. */
    const val DARK_FACTOR = 0.30f

    /** Amount of white mixed in for the final (lightest) shade. */
    const val LIGHT_MIX = 0.78f

    fun stops(baseArgb: Long, count: Int): List<Long> {
        require(count >= 2) { "Need at least two stops" }
        val base = Rgb.fromArgb(baseArgb)
        val dark = Rgb(base.r * DARK_FACTOR, base.g * DARK_FACTOR, base.b * DARK_FACTOR)
        val light = Rgb(
            base.r + (1f - base.r) * LIGHT_MIX,
            base.g + (1f - base.g) * LIGHT_MIX,
            base.b + (1f - base.b) * LIGHT_MIX,
        )
        return List(count) { index ->
            val t = index.toFloat() / (count - 1)
            val rgb = if (t <= 0.5f) Rgb.lerp(dark, base, t / 0.5f) else Rgb.lerp(base, light, (t - 0.5f) / 0.5f)
            rgb.toArgb()
        }
    }

    fun colorFor(stops: List<Long>, segment: StreakProgressionSegment): Long {
        val from = Rgb.fromArgb(stops[segment.fromStopIndex.coerceIn(stops.indices)])
        val to = Rgb.fromArgb(stops[segment.toStopIndex.coerceIn(stops.indices)])
        return if (segment.fromStopIndex == segment.toStopIndex) from.toArgb() else Rgb.lerp(from, to, segment.fraction).toArgb()
    }

    internal data class Rgb(val r: Float, val g: Float, val b: Float) {
        fun toArgb(): Long {
            fun c(v: Float) = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt().toLong()
            return 0xFF000000L or (c(r) shl 16) or (c(g) shl 8) or c(b)
        }

        companion object {
            fun fromArgb(argb: Long) = Rgb(
                r = ((argb shr 16) and 0xFF) / 255f,
                g = ((argb shr 8) and 0xFF) / 255f,
                b = (argb and 0xFF) / 255f,
            )

            fun lerp(a: Rgb, b: Rgb, t: Float) = Rgb(
                a.r + (b.r - a.r) * t,
                a.g + (b.g - a.g) * t,
                a.b + (b.b - a.b) * t,
            )
        }
    }
}
