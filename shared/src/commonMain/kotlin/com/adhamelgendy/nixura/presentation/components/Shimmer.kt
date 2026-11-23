package com.adhamelgendy.nixura.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import com.adhamelgendy.nixura.presentation.theme.Nix

private const val SWEEP_MILLIS = 1_300

/**
 * A loading placeholder: a calm base tone with a soft highlight sweeping across, in the reading direction (left to
 * right, or right to left in Arabic). With "remove animations" on it stays a still base tone.
 *
 * The sweep is read only while drawing, so it redraws the shape each frame without recomposing anything, and it stops
 * by itself when the placeholder leaves the screen.
 */
@Composable
fun Modifier.shimmer(shape: Shape = RectangleShape): Modifier {
    val c = Nix.colors
    val base = c.textPrimary.copy(alpha = if (c.isDark) 0.07f else 0.08f)
    // Dark theme: a slightly brighter band. Light theme: the band lifts towards the card's own white.
    val highlight = if (c.isDark) c.textPrimary.copy(alpha = 0.13f) else c.surface.copy(alpha = 0.85f)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val sweep: State<Float>? = if (rememberReducedMotion()) {
        null
    } else {
        rememberLoop(SWEEP_MILLIS)
    }
    return this.clip(shape).drawWithCache {
        val band = size.width * 0.55f
        onDrawBehind {
            drawRect(base)
            val p = sweep?.value ?: return@onDrawBehind
            val travelled = -band + (size.width + 2 * band) * p
            val center = if (rtl) size.width - travelled else travelled
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, highlight, Color.Transparent),
                    start = Offset(center - band / 2f, 0f),
                    end = Offset(center + band / 2f, size.height),
                ),
            )
        }
    }
}

/** A text-line placeholder: [fraction] of the width, [height] tall, with rounded ends. */
@Composable
fun SkeletonLine(fraction: Float, height: Dp, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(fraction).height(height).shimmer(RoundedCornerShape(height / 2)))
}

/** A round placeholder for icons, buttons and rings. */
@Composable
fun SkeletonCircle(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).shimmer(CircleShape))
}

/** Default height of a skeleton line that stands in for small text. */
val SkeletonTextHeight: Dp = 12.dp
