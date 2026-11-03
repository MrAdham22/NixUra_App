package com.example.reclaimapp.presentation.appearance

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Shape of a liquid surface, shared by the in-app liquid fill and the widget's still picture of it. */
object LiquidGeometry {
    /** Wave height multiplier for a fill level: calm near empty and full, so a full card is completely full. */
    fun calmness(level: Float): Float = min(1f, min(level, 1f - level).coerceAtLeast(0f) * 8f)

    /** Surface height at [x] across a box [width] wide, around the line [top]. */
    fun surfaceY(x: Float, width: Float, top: Float, amplitude: Float, phase: Float, cycles: Float): Float =
        top + amplitude * sin(phase + x / width * cycles * 2f * PI.toFloat())
}
