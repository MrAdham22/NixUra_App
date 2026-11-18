package com.adhamelgendy.nixura.presentation.components.themeanim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.presentation.platform.rememberDeviceTilt
import com.adhamelgendy.nixura.presentation.platform.rememberThemeHaptics
import kotlin.math.sin

/**
 * Water: a thin stream pours from the top centre and splashes, the water rises with a moving two-wave surface until
 * the page is full, then the stream stops and the water comes alive (bubbles, one small fish). A tap on
 * the water sends a ripple; the phone's tilt slopes the surface a little. Leaving the theme drains it.
 * With "remove animations": the water simply stands at its level, still.
 */
internal object WaterScene : ThemeScene {

    private const val BUBBLES = 14
    private const val DROPLETS = 6

    @Composable
    override fun Content(params: ThemeSceneParams) {
        val haptics = rememberThemeHaptics()
        val time = rememberSceneTime(params.running)
        val fill = remember { Animatable(0f) }
        val streamHead = remember { Animatable(0f) }
        val streamTail = remember { Animatable(0f) }
        val aliveSince = remember { mutableFloatStateOf(-1f) }
        val target = params.fillLevel.coerceIn(0f, 1f)

        LaunchedEffect(params.active, params.reducedMotion, target) {
            when {
                params.reducedMotion -> {
                    streamHead.snapTo(0f)
                    streamTail.snapTo(1f)
                    fill.snapTo(if (params.active) target else 0f)
                    aliveSince.floatValue = if (params.active) 0f else -1f
                    if (!params.active) params.onGone()
                }
                params.active -> {
                    if (fill.value < target - 0.01f) {
                        aliveSince.floatValue = -1f
                        streamTail.snapTo(0f)
                        streamHead.snapTo(0f)
                        haptics.pourStart()
                        streamHead.animateTo(1f, tween(450, easing = LinearOutSlowInEasing))
                        fill.animateTo(target, tween(2400, easing = FastOutSlowInEasing))
                        haptics.filled()
                        streamTail.animateTo(1f, tween(400))
                    } else {
                        streamTail.snapTo(1f)
                        fill.animateTo(target, tween(700))
                    }
                    if (aliveSince.floatValue < 0f) aliveSince.floatValue = time.value
                }
                else -> {
                    aliveSince.floatValue = -1f
                    streamTail.snapTo(1f)
                    fill.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
                    params.onGone()
                }
            }
        }

        val tilt = rememberDeviceTilt(enabled = params.active && params.running && aliveSince.floatValue >= 0f)
        val colors = params.colors
        val bleed = params.bleed
        val taps = params.taps
        val reduced = params.reducedMotion
        val rtl = params.rtl
        // Scene time at which each tap was first seen, by tap number. Only touched while drawing.
        val tapBirth = remember { HashMap<Long, Float>() }

        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val back = Path()
                val front = Path()
                val fish = Path()
                val top = Color.White
                val surfaceTint = if (colors.isDark) 0.40f else 0.26f
                val depthTint = if (colors.isDark) 0.62f else 0.40f
                onDrawBehind {
                    val level = fill.value
                    if (level <= 0.001f && streamHead.value <= 0.001f) return@onDrawBehind
                    val t = time.value
                    val left = -bleed.left
                    val pageTop = -bleed.top
                    val w = size.width + bleed.left + bleed.right
                    val bottom = size.height + bleed.bottom
                    val surf = ThemeAnimMath.surfaceY(level, pageTop, bottom, headroom = 10.dp.toPx())
                    val amp = 7.dp.toPx() * if (level >= 0.999f) 0.6f else 1f
                    val lean = tilt.value

                    // Two layers: a lighter back wave a little higher, then the main body.
                    waterPath(back, left, w, bottom, surf - 5.dp.toPx(), amp * 0.8f, t * 0.8f + 2f, lean)
                    drawPath(back, colors.water.copy(alpha = surfaceTint * 0.55f))
                    waterPath(front, left, w, bottom, surf, amp, t, lean)
                    drawPath(
                        front,
                        Brush.verticalGradient(
                            0f to colors.water.copy(alpha = surfaceTint),
                            1f to colors.waterDeep.copy(alpha = depthTint),
                            startY = surf,
                            endY = bottom,
                        ),
                    )

                    // The stream and its splash
                    val cx = left + w / 2f + lean * w * 0.08f
                    if (streamTail.value < 1f && streamHead.value > 0f) {
                        val span = surf - pageTop
                        val headY = pageTop + span * streamHead.value
                        val tailY = pageTop + span * streamTail.value
                        val width = 4.dp.toPx() + sin(t * 18f) * 0.6.dp.toPx()
                        drawRect(
                            Brush.verticalGradient(listOf(colors.water.copy(alpha = 0.55f), colors.water.copy(alpha = 0.9f)), startY = tailY, endY = headY),
                            topLeft = Offset(cx - width / 2f, tailY),
                            size = Size(width, (headY - tailY).coerceAtLeast(0f)),
                        )
                        if (streamHead.value >= 0.999f) drawSplash(cx, surf, t)
                    }

                    val since = aliveSince.floatValue
                    if (since >= 0f) {
                        val alive = if (reduced) 1f else ((t - since) / 0.8f).coerceIn(0f, 1f)
                        drawBubbles(left, w, surf, bottom, t, alive)
                        if (!reduced) {
                            ThemeAnimMath.fishProgress(t - since)?.let { p ->
                                val x = if (rtl) left + w * (1f - p) else left + w * p
                                val y = pageTop + (bottom - pageTop) * 0.64f + sin((t - since) * 1.3f) * 10.dp.toPx()
                                drawFish(fish, x, y, facingRight = !rtl, wiggle = sin(t * 9f), color = colors.accentWarm)
                            }
                        }
                    }

                    // Ripples where the water was tapped
                    if (!reduced) {
                        val seen = HashSet<Long>()
                        for ((at, id) in taps.items) {
                            seen += id
                            val born = tapBirth.getOrPut(id) { t }
                            val r = ThemeAnimMath.ripple(t - born) ?: continue
                            if (at.y < surf) continue
                            val (grow, fade) = r
                            drawCircle(top.copy(alpha = 0.45f * fade), radius = 12.dp.toPx() + 60.dp.toPx() * grow, center = at, style = Stroke(2.dp.toPx()))
                            drawCircle(top.copy(alpha = 0.25f * fade), radius = 6.dp.toPx() + 32.dp.toPx() * grow, center = at, style = Stroke(1.5.dp.toPx()))
                        }
                        tapBirth.keys.retainAll(seen)
                    }
                }
            },
        )
    }

    /** Fills [path] with the water body: the wavy surface at [rest] across the width, down to [bottom]. */
    private fun DrawScope.waterPath(path: Path, left: Float, w: Float, bottom: Float, rest: Float, amp: Float, t: Float, tilt: Float) {
        path.reset()
        val step = 10.dp.toPx()
        path.moveTo(left, bottom)
        var x = 0f
        while (x <= w + step) {
            path.lineTo(left + x, rest + ThemeAnimMath.waveOffset(x, w, t, amp, tilt))
            x += step
        }
        path.lineTo(left + w + step, bottom)
        path.close()
    }

    private fun DrawScope.drawSplash(cx: Float, surf: Float, t: Float) {
        val unitX = 16.dp.toPx()
        val unitY = 18.dp.toPx()
        for (i in 0 until DROPLETS) {
            val d = ThemeAnimMath.droplet(i, t)
            if (d.dy < -0.1f) continue
            drawCircle(Color.White.copy(alpha = 0.7f * d.alpha), radius = 2.2.dp.toPx(), center = Offset(cx + d.dx * unitX, surf - d.dy * unitY))
        }
        val pulse = 1f + 0.25f * sin(t * 10f)
        drawOval(
            Color.White.copy(alpha = 0.35f),
            topLeft = Offset(cx - 14.dp.toPx() * pulse, surf - 3.dp.toPx()),
            size = Size(28.dp.toPx() * pulse, 6.dp.toPx()),
            style = Stroke(1.5.dp.toPx()),
        )
    }

    private fun DrawScope.drawBubbles(left: Float, w: Float, surf: Float, bottom: Float, t: Float, alive: Float) {
        val column = bottom - surf
        if (column <= 0f) return
        for (i in 0 until BUBBLES) {
            val b = ThemeAnimMath.bubble(i, t)
            val r = (2f + 4f * b.radius).dp.toPx()
            val y = bottom - b.rise * column
            if (y < surf + r * 2f) continue
            // Fade in near the bottom and out just under the surface.
            val edge = (b.rise / 0.1f).coerceIn(0f, 1f) * ((1f - b.rise) / 0.15f).coerceIn(0f, 1f)
            val c = Offset(left + b.x * w, y)
            drawCircle(Color.White.copy(alpha = 0.35f * edge * alive), r, c, style = Stroke(1.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.5f * edge * alive), r * 0.28f, Offset(c.x - r * 0.35f, c.y - r * 0.35f))
        }
    }

    private fun DrawScope.drawFish(path: Path, x: Float, y: Float, facingRight: Boolean, wiggle: Float, color: Color) {
        val dir = if (facingRight) 1f else -1f
        val bodyW = 26.dp.toPx()
        val bodyH = 12.dp.toPx()
        drawOval(color.copy(alpha = 0.8f), topLeft = Offset(x - bodyW / 2f, y - bodyH / 2f), size = Size(bodyW, bodyH))
        val tailX = x - dir * bodyW / 2f
        val sway = wiggle * 3.dp.toPx()
        path.reset()
        path.moveTo(tailX + dir * 2.dp.toPx(), y)
        path.lineTo(tailX - dir * 9.dp.toPx(), y - 6.dp.toPx() + sway)
        path.lineTo(tailX - dir * 9.dp.toPx(), y + 6.dp.toPx() + sway)
        path.close()
        drawPath(path, color.copy(alpha = 0.8f))
        drawCircle(Color.White.copy(alpha = 0.9f), 1.8.dp.toPx(), Offset(x + dir * bodyW * 0.25f, y - bodyH * 0.12f))
    }
}
