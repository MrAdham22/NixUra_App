package com.adhamelgendy.nixura.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.theme.PrayerPalette

// Polished-metal gold, from a bright top edge down to a deep bottom edge.
private val MetalTop = Color(0xFFFFF3C9)
private val MetalHigh = Color(0xFFF7D27A)
private val MetalMid = Color(0xFFD9A23C)
private val MetalLow = Color(0xFFA86E1C)
private val MetalEdge = Color(0xFF6B4210)
private val Engraving = Color(0xFF4A2C07)
private val Ink = Color(0xFF2E1A04)

/**
 * The prayer habit's main button: polished gold with a soft halo, a glossy top, an engraved double edge, a star at
 * each end and a light that keeps sweeping across it in a seamless loop (still with "remove animations").
 */
@Composable
fun PrayerGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val reduced = rememberReducedMotion()
    val sweep = rememberInfiniteTransition(label = "goldButton")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "sweep")
    Row(
        modifier = modifier
            .pressable(enabled = enabled && !loading, pressedScale = 0.97f, haptic = true, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .height(58.dp)
            .drawWithContent {
                val w = size.width
                val h = size.height
                val radius = CornerRadius(h / 2.6f)
                // Halo around the button
                for (step in 3 downTo 1) {
                    val grow = step * 3.dp.toPx()
                    drawRoundRect(
                        PrayerPalette.Gold.copy(alpha = 0.07f * (4 - step)),
                        topLeft = Offset(-grow, -grow),
                        size = Size(w + grow * 2f, h + grow * 2f),
                        cornerRadius = CornerRadius(radius.x + grow),
                    )
                }
                // Metal body
                drawRoundRect(
                    Brush.verticalGradient(0f to MetalTop, 0.28f to MetalHigh, 0.62f to MetalMid, 1f to MetalLow),
                    cornerRadius = radius,
                )
                val shape = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, radius))
                }
                clipPath(shape, ClipOp.Intersect) {
                    // Gloss on the upper half
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), startY = 0f, endY = h * 0.5f),
                        topLeft = Offset(3.dp.toPx(), 2.dp.toPx()),
                        size = Size(w - 6.dp.toPx(), h * 0.5f),
                        cornerRadius = CornerRadius(radius.x - 3.dp.toPx()),
                    )
                    // Faint star pattern engraved into the metal
                    drawPath(starTiling(w, h, 26.dp.toPx()), Engraving.copy(alpha = 0.08f), style = Stroke(1.dp.toPx()))
                    // Sweeping light: two bands half a loop apart, each wrapping only while outside the button
                    val p = if (reduced) 0.3f else sweep.value
                    for (phase in listOf(p, (p + 0.5f) % 1f)) {
                        val band = h * 1.1f
                        val x = -band * 2f + phase * (w + band * 4f)
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                                Offset(x - band, 0f),
                                Offset(x + band * 0.4f, h),
                            ),
                        )
                    }
                }
                // Engraved double edge
                drawRoundRect(MetalEdge, cornerRadius = radius, style = Stroke(1.2.dp.toPx()))
                val inset = 3.5.dp.toPx()
                drawRoundRect(
                    Color.White.copy(alpha = 0.55f),
                    topLeft = Offset(inset, inset),
                    size = Size(w - inset * 2f, h - inset * 2f),
                    cornerRadius = CornerRadius(radius.x - inset),
                    style = Stroke(0.8.dp.toPx()),
                )
                // A star at each end, with a fine line running toward the label
                val starR = 6.dp.toPx()
                for (cx in listOf(22.dp.toPx(), w - 22.dp.toPx())) {
                    drawPath(octagramPath(cx, h / 2f, starR), Engraving.copy(alpha = 0.85f))
                    drawCircle(MetalTop, starR * 0.32f, Offset(cx, h / 2f))
                    val dir = if (cx < w / 2f) 1f else -1f
                    drawLine(
                        Brush.horizontalGradient(
                            listOf(Engraving.copy(alpha = 0.55f), Color.Transparent),
                            startX = cx + dir * starR * 1.6f,
                            endX = cx + dir * w * 0.2f,
                        ),
                        Offset(cx + dir * starR * 1.6f, h / 2f),
                        Offset(cx + dir * w * 0.2f, h / 2f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                drawContent()
            }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text(
                text,
                style = NixType.BodyStrong.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
                color = Ink,
                maxLines = 1,
            )
        }
    }
}
