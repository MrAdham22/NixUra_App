package com.example.reclaimapp.presentation.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.reclaimapp.presentation.platform.rememberReducedMotion
import com.example.reclaimapp.presentation.theme.NixUraPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.ui.graphics.Canvas as BitmapCanvas

private const val HOLD_MS = 1600L
private const val DISSOLVE_MS = 3400
private const val GONE_MS = 1700L
private const val RETURN_MS = 700

private const val ASH_ARGB = 0xFF9A8F97.toInt()
private const val EMBER_ARGB = 0xFFF39F5A.toInt()

/**
 * Draws [text], then blows it away as ash, cell by cell, and brings it back after a pause. All animation is read in
 * the draw phase, so it never recomposes. With "remove animations" on, the word simply stays.
 */
@Composable
fun AshWord(text: String, style: TextStyle, brush: Brush, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val measurer = rememberTextMeasurer()
    val reducedMotion = rememberReducedMotion()
    val layout = remember(text, style, density) { measurer.measure(text, style) }
    val rendered = remember(layout, brush) {
        val bitmap = ImageBitmap(layout.size.width, layout.size.height)
        CanvasDrawScope().draw(density, direction, BitmapCanvas(bitmap), Size(bitmap.width.toFloat(), bitmap.height.toFloat())) {
            drawText(layout, brush = brush)
        }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(pixels)
        bitmap to AshField.fromPixels(pixels, bitmap.width, bitmap.height)
    }
    val bitmap = rendered.first
    val field = rendered.second
    val progress = remember { Animatable(0f) }
    val reveal = remember { Animatable(1f) }

    LaunchedEffect(reducedMotion, field) {
        progress.snapTo(0f)
        reveal.snapTo(1f)
        if (reducedMotion) return@LaunchedEffect
        while (true) {
            delay(HOLD_MS)
            progress.animateTo(1f, tween(DISSOLVE_MS, easing = LinearEasing))
            delay(GONE_MS)
            reveal.snapTo(0f)
            progress.snapTo(0f)
            reveal.animateTo(1f, tween(RETURN_MS, easing = FastOutSlowInEasing))
        }
    }

    val widthDp = with(density) { layout.size.width.toDp() }
    val heightDp = with(density) { layout.size.height.toDp() }
    val windPx = with(density) { 96.dp.toPx() }
    val risePx = with(density) { 62.dp.toPx() }
    val swayPx = with(density) { 7.dp.toPx() }

    Canvas(modifier.size(widthDp, heightDp)) {
        val p = progress.value
        if (p <= 0f) {
            drawImage(bitmap, IntOffset.Zero, IntSize(bitmap.width, bitmap.height), alpha = reveal.value)
            return@Canvas
        }
        for (i in 0 until field.count) {
            val t = field.cellProgress(i, p)
            if (t >= 1f) continue
            val eased = t * t
            val x = field.xs[i] + field.drifts[i] * eased * windPx + sin(field.phases[i] + t * 7f) * t * swayPx
            val y = field.ys[i] - field.rises[i] * eased * risePx
            val source = field.argb[i]
            val sourceAlpha = (source ushr 24) / 255f
            val color = when {
                t <= 0f -> source
                t < 0.22f -> AshField.mixArgb(source, EMBER_ARGB, t / 0.22f, sourceAlpha)
                else -> AshField.mixArgb(EMBER_ARGB, ASH_ARGB, (t - 0.22f) / 0.5f, sourceAlpha * (1f - t))
            }
            val side = field.cellPx * (1f - 0.45f * t)
            drawRect(Color(color), Offset(x, y), Size(side, side))
        }
    }
}

/** The account screens' backdrop: brand gradient, two slow glows and a few rising embers. Static with "remove animations". */
@Composable
fun AuraBackdrop(modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "aura")
    val clock = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "auraClock",
    )
    val embers = remember {
        val random = Random(11)
        List(26) { floatArrayOf(random.nextFloat(), random.nextFloat(), 0.5f + random.nextFloat(), random.nextFloat() * 6.283f, 1f + random.nextInt(3)) }
    }
    Canvas(modifier.fillMaxSize()) {
        val time = if (reducedMotion) 0.2f else clock.value
        val turn = time * 2f * PI.toFloat()
        drawRect(
            Brush.verticalGradient(
                0f to NixUraPalette.Navy,
                0.55f to Color(0xFF2B1B47),
                1f to NixUraPalette.Navy,
            ),
        )
        val w = size.width
        val h = size.height
        val glowOne = Offset(w * (0.78f + 0.12f * sin(turn)), h * (0.20f + 0.05f * sin(turn + 1f)))
        drawCircle(
            Brush.radialGradient(listOf(NixUraPalette.Rose.copy(alpha = 0.42f), Color.Transparent), glowOne, w * 0.85f),
            w * 0.85f,
            glowOne,
        )
        val glowTwo = Offset(w * (0.18f + 0.10f * sin(turn + 2.4f)), h * (0.78f + 0.05f * sin(turn + 4f)))
        drawCircle(
            Brush.radialGradient(listOf(NixUraPalette.Orange.copy(alpha = 0.20f), Color.Transparent), glowTwo, w * 0.9f),
            w * 0.9f,
            glowTwo,
        )
        for (ember in embers) {
            val rise = (ember[1] + time * ember[2] * 3f) % 1f
            val fade = sin(rise * PI.toFloat())
            val x = ember[0] * w + sin(turn * 2f + ember[3]) * 14f
            drawCircle(NixUraPalette.Orange.copy(alpha = 0.32f * fade), ember[4] * density * 0.7f, Offset(x, h * (1f - rise)))
        }
    }
}

/** Fades and lifts content in on first show, [delayMillis] after the screen appears. Uses the layer phase only. */
fun Modifier.riseIn(delayMillis: Int): Modifier = composed {
    val reducedMotion = rememberReducedMotion()
    val shown = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            delay(delayMillis.toLong())
            shown.animateTo(1f, tween(620, easing = LinearOutSlowInEasing))
        }
    }
    graphicsLayer {
        alpha = shown.value
        translationY = (1f - shown.value) * 22.dp.toPx()
    }
}
