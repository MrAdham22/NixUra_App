package com.example.reclaimapp.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.reclaimapp.presentation.appearance.LiquidGeometry
import com.example.reclaimapp.presentation.platform.rememberReducedMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Colors of a liquid fill: what is behind it, then the liquid from its surface down to its depth. */
@Immutable
data class LiquidColors(val empty: Color, val surface: Color, val depth: Color)

/**
 * Paints a container that fills with liquid up to [level] (0..1). Each change rises or drains smoothly with a short
 * splash on the surface, a gentle wave keeps moving while partly full, and reaching full plays a light sweep.
 *
 * Every moving value is read only while drawing, so a tap redraws the card but never recomposes its content. With
 * "remove animations" on, the level jumps and the surface stays still. Reusable for any measurable habit.
 */
@Composable
fun Modifier.liquidFill(level: Float, colors: LiquidColors): Modifier {
    val reducedMotion = rememberReducedMotion()
    val target = level.coerceIn(0f, 1f)
    val shown = remember { Animatable(target) }
    val splash = remember { Animatable(0f) }
    val phase = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }
    var wasFull by remember { mutableStateOf(target >= 1f) }

    LaunchedEffect(target, reducedMotion) {
        if (reducedMotion) {
            shown.snapTo(target)
            return@LaunchedEffect
        }
        if (target == shown.value) return@LaunchedEffect
        launch {
            splash.snapTo(1f)
            splash.animateTo(0f, tween(1400))
        }
        shown.animateTo(target, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessVeryLow))
    }
    // The wave only runs while there is a surface to move; an empty or full card costs nothing.
    val waving = !reducedMotion && target > 0f && target < 1f
    LaunchedEffect(waving) {
        while (waving) {
            phase.animateTo(1f, tween(WAVE_PERIOD_MS, easing = LinearEasing))
            phase.snapTo(0f)
        }
    }
    LaunchedEffect(target >= 1f, reducedMotion) {
        val full = target >= 1f
        if (full && !wasFull && !reducedMotion) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(900))
            sweep.snapTo(0f)
        }
        wasFull = full
    }

    return this.drawWithCache {
        val back = Path()
        val front = Path()
        val body = Brush.verticalGradient(listOf(colors.surface, colors.depth))
        val baseAmplitude = 3.5.dp.toPx()
        onDrawBehind {
            drawRect(colors.empty)
            val l = shown.value
            if (l <= 0f) return@onDrawBehind
            val edge = LiquidGeometry.calmness(l)
            val amplitude = baseAmplitude * (1f + 1.6f * splash.value) * edge
            val top = size.height * (1f - l)
            val p = phase.value * 2f * PI.toFloat()
            wave(back, top - amplitude * 0.4f, amplitude * 0.8f, p + 1.7f, 1.3f)
            drawPath(back, colors.surface.copy(alpha = 0.45f))
            wave(front, top, amplitude, -p, 1f)
            drawPath(front, body)
            val s = sweep.value
            if (s > 0f) {
                val x = size.width * (s * 1.6f - 0.3f)
                val glint = Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.28f * sin(PI.toFloat() * s)), Color.Transparent),
                    start = Offset(x - size.width * 0.25f, 0f),
                    end = Offset(x + size.width * 0.25f, size.height),
                )
                drawRect(glint)
            }
        }
    }
}

/** Rebuilds [path] as a sine-topped body filling the bottom of the box from [top] down. */
private fun DrawScope.wave(path: Path, top: Float, amplitude: Float, phase: Float, cycles: Float) {
    path.rewind()
    val w = size.width
    val step = 6.dp.toPx()
    path.moveTo(0f, size.height)
    var x = 0f
    while (x <= w) {
        path.lineTo(x, LiquidGeometry.surfaceY(x, w, top, amplitude, phase, cycles))
        x += step
    }
    path.lineTo(w, LiquidGeometry.surfaceY(w, w, top, amplitude, phase, cycles))
    path.lineTo(w, size.height)
    path.close()
}

private const val WAVE_PERIOD_MS = 2600
