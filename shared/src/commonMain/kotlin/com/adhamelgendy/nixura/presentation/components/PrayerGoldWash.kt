package com.adhamelgendy.nixura.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.recognition.PrayerHabitDetector
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import com.adhamelgendy.nixura.presentation.theme.PrayerPalette
import kotlin.math.PI
import kotlin.math.sin

/** Makes the prayer word of a habit name glow gold while the user types it. */
class PrayerWordTransformation(private val color: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val range = PrayerHabitDetector.wordRange(text.text) ?: return TransformedText(text, OffsetMapping.Identity)
        val styled = buildAnnotatedString {
            append(text)
            addStyle(
                SpanStyle(color = color, fontWeight = FontWeight.Bold, shadow = Shadow(color.copy(alpha = 0.7f), blurRadius = 14f)),
                range.first,
                range.last + 1,
            )
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

// Warm amber gold, darker at the top and bottom so light text and the gold button stay readable.
private val WashTop = Color(0xFF2A1607)
private val WashMiddle = Color(0xFF7A4A12)
private val WashBottom = Color(0xFF3D220A)
private val MosqueShade = Color(0xFF1A0E05)

/** Slow at both ends, so the gold settles in gently. */
private val WashEasing = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)

/**
 * The form turning gold. When [active], a glowing front of gold runs from the very top of the screen (status bar
 * included) down to the very bottom, leaving amber gold, a star pattern, twinkles, a crescent and a mosque standing in
 * a golden dawn. Put it behind the page content; it draws over the system-bar areas too. When no longer active the
 * gold drains away. With "remove animations" the gold simply appears.
 */
@Composable
fun PrayerGoldWash(active: Boolean, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val down = remember { Animatable(0f) }
    LaunchedEffect(active, reduced) {
        when {
            active && reduced -> down.snapTo(1f)
            active -> down.animateTo(1f, tween(2600, easing = WashEasing))
            else -> down.animateTo(0f, tween(350))
        }
    }
    val twinkle = rememberInfiniteTransition(label = "washTwinkle")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "twinkle")
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing
    val extraTop = insets.getTop(density).toFloat()
    val extraBottom = insets.getBottom(density).toFloat()
    val extraLeft = insets.getLeft(density, direction).toFloat()
    val extraRight = insets.getRight(density, direction).toFloat()

    if (down.value <= 0.001f) return
    Spacer(modifier.fillMaxSize().drawWithCache {
        // The whole screen, in this canvas's coordinates: the page sits inside the system bars.
        val left = -extraLeft
        val top = -extraTop
        val w = size.width + extraLeft + extraRight
        val h = size.height + extraTop + extraBottom
        val bottom = top + h
        val gold = Brush.verticalGradient(listOf(WashTop, WashMiddle, WashBottom), startY = top, endY = bottom)
        val tiling = starTiling(w, h, 58.dp.toPx())
        val mosqueWidth = w * 0.86f
        val mosque = mosquePath((w - mosqueWidth) / 2f, h + 1f, mosqueWidth)
        val moonX = w - 46.dp.toPx()
        val moonY = extraTop + 34.dp.toPx()
        val moon = crescentPath(moonX, moonY, 14.dp.toPx())
        onDrawBehind {
        val frontY = top + down.value * h
        clipRect(left = left, top = top, right = left + w, bottom = frontY) {
            drawRect(gold, topLeft = Offset(left, top), size = Size(w, h))
            translate(left, top) {
                drawPath(tiling, PrayerPalette.Gold.copy(alpha = 0.13f), style = Stroke(1.dp.toPx()))
                // Golden dawn and a mosque at the bottom
                drawRect(
                    Brush.radialGradient(
                        listOf(PrayerPalette.Gold.copy(alpha = 0.55f), PrayerPalette.Gold.copy(alpha = 0.10f), Color.Transparent),
                        Offset(w / 2f, h),
                        w * 0.75f,
                    ),
                    size = Size(w, h), // the whole screen, not just the page area inside the bars
                )
                drawPath(mosque, MosqueShade.copy(alpha = 0.82f))
                drawPath(mosque, PrayerPalette.Gold.copy(alpha = 0.55f), style = Stroke(1.2.dp.toPx()))
                // Crescent moon at the top right
                // A soft glow with no edge, so the moon doesn't look like a round button in the header.
                drawCircle(
                    Brush.radialGradient(listOf(PrayerPalette.GoldLight.copy(alpha = 0.22f), Color.Transparent), Offset(moonX, moonY), 34.dp.toPx()),
                    34.dp.toPx(),
                    Offset(moonX, moonY),
                )
                drawPath(moon, PrayerPalette.GoldLight.copy(alpha = 0.9f))
                // Twinkling stars
                val phase = twinkle.value * 2f * PI.toFloat()
                // In the side margins only, so no star sits on a field, a chip or a label.
                val margin = 18.dp.toPx()
                for (i in 0 until 26) {
                    val f = (i * 0.6180339f) % 1f
                    val x = if (i % 2 == 0) f * margin else w - margin + f * margin
                    val y = ((i * 0.3819660f + 0.17f) % 1f) * h * 0.8f
                    val a = (0.5f + 0.5f * sin(phase + i * 1.7f)).coerceIn(0f, 1f)
                    drawPath(
                        octagramPath(x, y, (3f + (i % 3) * 1.5f).dp.toPx() * (0.4f + 0.6f * a), phase),
                        PrayerPalette.GoldLight.copy(alpha = 0.7f * a),
                    )
                }
            }
        }
        // The glowing front while it is still running down
        if (down.value < 0.999f) {
            val glow = 90.dp.toPx()
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, PrayerPalette.Gold.copy(alpha = 0.5f), PrayerPalette.GoldLight.copy(alpha = 0.95f)),
                    startY = frontY - glow,
                    endY = frontY,
                ),
                topLeft = Offset(left, frontY - glow),
                size = Size(w, glow),
            )
            drawLine(PrayerPalette.GoldLight, Offset(left, frontY), Offset(left + w, frontY), strokeWidth = 2.dp.toPx())
        }
        }
    })
}
