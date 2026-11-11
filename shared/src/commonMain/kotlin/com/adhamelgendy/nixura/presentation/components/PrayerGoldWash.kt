package com.adhamelgendy.nixura.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
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

private val WashTop = Color(0xFF2A1C08)
private val WashBottom = Color(0xFF7A5815)

/**
 * The form turning gold. When [active] it starts at [originY] (the prayer word) and a front of gold runs down the
 * screen to the bottom, then climbs up to the top until everything is gold. When it is no longer active the gold
 * drains away. Put it behind the page content. With "remove animations" the gold simply appears.
 */
@Composable
fun PrayerGoldWash(active: Boolean, originY: Float, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val down = remember { Animatable(0f) }
    val up = remember { Animatable(0f) }
    LaunchedEffect(active, reduced) {
        if (active) {
            if (reduced) {
                down.snapTo(1f)
                up.snapTo(1f)
            } else {
                down.animateTo(1f, tween(1700, easing = NixEasing))
                up.animateTo(1f, tween(900, easing = NixEasing))
            }
        } else {
            up.snapTo(0f)
            down.animateTo(0f, tween(350))
        }
    }
    val twinkle = rememberInfiniteTransition(label = "washTwinkle")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "twinkle")

    if (down.value <= 0.001f) return
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val origin = originY.coerceIn(0f, h)
        val frontY = origin + down.value * (h - origin)
        val topY = origin - up.value * origin
        val gold = Brush.verticalGradient(listOf(WashTop, WashBottom), startY = 0f, endY = h)
        clipRect(left = 0f, top = topY, right = w, bottom = frontY) {
            drawRect(gold)
            // Stars of the tiling, brighter near the front
            drawPath(starTiling(w, h, 58.dp.toPx()), PrayerPalette.Gold.copy(alpha = 0.16f), style = Stroke(1.dp.toPx()))
            // Twinkling stars
            val phase = twinkle.value * 2f * PI.toFloat()
            for (i in 0 until 26) {
                val x = ((i * 0.6180339f) % 1f) * w
                val y = ((i * 0.3819660f + 0.17f) % 1f) * h
                val a = (0.5f + 0.5f * sin(phase + i * 1.7f)).coerceIn(0f, 1f)
                drawPath(
                    octagramPath(x, y, (3f + (i % 3) * 1.5f).dp.toPx() * (0.4f + 0.6f * a), phase),
                    PrayerPalette.GoldLight.copy(alpha = 0.75f * a),
                )
            }
        }
        // The glowing front, still running down
        if (down.value < 0.999f) {
            val glow = 70.dp.toPx()
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, PrayerPalette.Gold.copy(alpha = 0.55f), PrayerPalette.GoldLight.copy(alpha = 0.95f)),
                    startY = frontY - glow,
                    endY = frontY,
                ),
                topLeft = Offset(0f, frontY - glow),
                size = Size(w, glow),
            )
        }
        // And the one climbing to the top
        if (up.value in 0.001f..0.999f) {
            val glow = 50.dp.toPx()
            drawRect(
                Brush.verticalGradient(
                    listOf(PrayerPalette.GoldLight.copy(alpha = 0.9f), PrayerPalette.Gold.copy(alpha = 0.4f), Color.Transparent),
                    startY = topY,
                    endY = topY + glow,
                ),
                topLeft = Offset(0f, topY),
                size = Size(w, glow),
            )
        }
    }
}
