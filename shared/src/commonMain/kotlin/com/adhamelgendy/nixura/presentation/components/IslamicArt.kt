package com.adhamelgendy.nixura.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.theme.PrayerPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * The Islamic art of the prayer habit card: an eight-pointed star (the khatam, two squares laid over each other),
 * a tiling of it behind the card, a burst of stars each time a prayer is counted, star-shaped buttons and the row
 * of stars that light up one by one. All drawn in code, no image files.
 */

/** Ratio of the inner to the outer radius of an eight-pointed star made of two overlapping squares. */
private const val STAR_INNER = 0.7654f
private const val PI_F = PI.toFloat()

/** The outline of an eight-pointed star. [rotation] is in radians. */
internal fun octagramPath(cx: Float, cy: Float, radius: Float, rotation: Float = 0f): Path {
    val path = Path()
    for (i in 0 until 16) {
        val angle = rotation + i * (PI_F / 8f) - PI_F / 2f
        val r = if (i % 2 == 0) radius else radius * STAR_INNER
        val x = cx + cos(angle) * r
        val y = cy + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** A star pattern covering [width] x [height]: rows of stars, every other row shifted by half a cell. */
internal fun starTiling(width: Float, height: Float, cell: Float): Path {
    val path = Path()
    var row = 0
    var y = cell / 2f
    while (y < height + cell) {
        var x = cell / 2f + (if (row % 2 == 0) 0f else cell / 2f)
        while (x < width + cell) {
            path.addPath(octagramPath(x, y, cell * 0.46f))
            x += cell
        }
        y += cell * 0.5f
        row++
    }
    return path
}

private fun easeOut(t: Float): Float {
    val x = 1f - t.coerceIn(0f, 1f)
    return 1f - x * x * x
}

/** The painting of a prayer card and the colors its content reads on. */
internal data class PrayerArt(
    val modifier: Modifier,
    val content: Color,
    val contentMuted: Color,
    val background: Color,
    val doneSquare: Color,
)

private class FixedState(override val value: Float) : State<Float>

/**
 * Builds the painting of a prayer card at [fill] (0..1) for today's [count]. Every counted prayer sends a burst of
 * golden stars across the card (not with "remove animations"); the card turns solid gold once all are done.
 */
@Composable
internal fun rememberPrayerArt(fill: Float, count: Int): PrayerArt {
    val reduced = rememberReducedMotion()
    val burst = remember { Animatable(1f) }
    var lastCount by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        if (count > lastCount && !reduced) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(1700, easing = LinearEasing))
        }
        lastCount = count
    }
    val transition = rememberInfiniteTransition(label = "prayerShimmer")
    val shimmerAnim = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5600, easing = LinearEasing)), label = "shimmer")
    val still = remember { FixedState(0.62f) }
    val shimmer: State<Float> = if (reduced) still else shimmerAnim

    val done = ((fill - 0.92f) / 0.08f).coerceIn(0f, 1f)
    val content = lerp(PrayerPalette.Cream, PrayerPalette.Night, done)
    val art = Modifier.drawWithCache {
        val tiling = starTiling(size.width, size.height, 46.dp.toPx())
        val corner = 20.dp.toPx()
        val frame = 1.6.dp.toPx()
        onDrawBehind {
            val w = size.width
            val h = size.height
            // Base: night and plum, replaced by solid gold when every prayer is done
            drawRect(Brush.linearGradient(listOf(PrayerPalette.NightPlum, PrayerPalette.Night), Offset.Zero, Offset(w, h)))
            if (done > 0f) {
                drawRect(
                    Brush.linearGradient(listOf(PrayerPalette.GoldLight, PrayerPalette.Gold, PrayerPalette.GoldDeep), Offset.Zero, Offset(w, h)),
                    alpha = done,
                )
            }
            // Rising golden light from the bottom, more with every prayer
            if (fill > 0f && done < 1f) {
                val rise = h * (0.18f + 0.42f * fill)
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, PrayerPalette.Gold.copy(alpha = 0.34f * (1f - done))),
                        startY = h - rise,
                        endY = h,
                    ),
                    topLeft = Offset(0f, h - rise),
                    size = Size(w, rise),
                )
            }
            // The star tiling
            val lineColor = lerp(PrayerPalette.Gold, PrayerPalette.Bronze, done)
            drawPath(tiling, lineColor.copy(alpha = 0.10f + 0.10f * fill + 0.12f * done), style = Stroke(1.dp.toPx()))
            // A soft band of light that keeps sliding across
            val bandX = (shimmer.value * 1.8f - 0.4f) * w
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.13f), Color.Transparent),
                    Offset(bandX - w * 0.18f, 0f),
                    Offset(bandX + w * 0.18f, h * 0.5f),
                ),
            )
            drawBurst(burst.value, w, h)
            // Gold frame
            drawRoundRect(
                Brush.linearGradient(listOf(PrayerPalette.GoldLight, PrayerPalette.GoldDeep, PrayerPalette.Gold), Offset.Zero, Offset(w, h)),
                topLeft = Offset(frame / 2f, frame / 2f),
                size = Size(w - frame, h - frame),
                cornerRadius = CornerRadius(corner),
                style = Stroke(frame),
                alpha = 0.75f,
            )
        }
    }
    return PrayerArt(
        modifier = art,
        content = content,
        contentMuted = content.copy(alpha = 0.72f),
        background = lerp(PrayerPalette.NightPlum, PrayerPalette.GoldDeep, done),
        doneSquare = lerp(PrayerPalette.Gold, PrayerPalette.Night, done),
    )
}

/** The burst of a counted prayer: three stars growing and turning in turn, a flash and sparkles flying outward. */
private fun DrawScope.drawBurst(p: Float, w: Float, h: Float) {
    if (p >= 1f) return
    val cx = w * 0.5f
    val cy = h * 0.5f
    val reach = w * 0.62f
    drawRect(PrayerPalette.GoldLight.copy(alpha = 0.22f * (1f - p) * (1f - p)))
    for (i in 0 until 3) {
        val t = ((p * 1.25f) - i * 0.14f).coerceIn(0f, 1f)
        if (t <= 0f) continue
        val radius = reach * easeOut(t)
        val alpha = 1f - t
        val turn = (if (i % 2 == 0) 1f else -1f) * t * (PI_F / 4f)
        val star = octagramPath(cx, cy, radius, turn)
        drawPath(star, PrayerPalette.Gold.copy(alpha = 0.16f * alpha))
        drawPath(star, PrayerPalette.GoldLight.copy(alpha = 0.95f * alpha), style = Stroke(2.2.dp.toPx()))
    }
    val spread = reach * 1.05f * easeOut(p)
    for (j in 0 until 16) {
        val angle = j * (PI_F / 8f) + p * 0.6f
        val r = spread * (0.55f + 0.45f * ((j % 3) / 2f))
        val sparkle = octagramPath(cx + cos(angle) * r, cy + sin(angle) * r, 5.dp.toPx() * (1f - p), p * 3f)
        drawPath(sparkle, PrayerPalette.GoldLight.copy(alpha = (1f - p).coerceIn(0f, 1f)))
    }
}

/** Star-shaped button of the prayer card: gold and slowly turning for "+", a quiet outline for "-". */
@Composable
fun PrayerStarButton(
    icon: NixIcon,
    onClick: () -> Unit,
    size: Dp,
    onGold: Boolean,
    secondary: Boolean,
    ink: Color,
    modifier: Modifier = Modifier,
    haptic: Boolean = false,
) {
    val reduced = rememberReducedMotion()
    val spinAnim = rememberInfiniteTransition(label = "starSpin")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "spin")
    val box = size * 1.18f
    Box(
        modifier.pressable(pressedScale = 0.88f, haptic = haptic, onClick = onClick).size(box),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val spin = if (reduced) 0f else spinAnim.value * (PI_F / 180f)
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.minDimension / 2f * 0.96f
            if (secondary) {
                drawPath(octagramPath(c.x, c.y, r), ink.copy(alpha = 0.10f))
                drawPath(octagramPath(c.x, c.y, r), ink.copy(alpha = 0.55f), style = Stroke(1.3.dp.toPx()))
            } else {
                val fillBrush = if (onGold) {
                    Brush.radialGradient(listOf(PrayerPalette.NightPlum, PrayerPalette.Night), c, r)
                } else {
                    Brush.radialGradient(listOf(PrayerPalette.GoldLight, PrayerPalette.Gold, PrayerPalette.GoldDeep), c, r)
                }
                val edge = if (onGold) PrayerPalette.Gold else PrayerPalette.Bronze.copy(alpha = 0.55f)
                // The outer star turns slowly, the inner outline the other way
                val outer = octagramPath(c.x, c.y, r, spin)
                drawPath(outer, fillBrush)
                drawPath(outer, edge, style = Stroke(1.4.dp.toPx()))
                drawPath(octagramPath(c.x, c.y, r * 0.74f, -spin * 1.4f), edge.copy(alpha = 0.7f), style = Stroke(1.dp.toPx()))
                drawCircle(
                    if (onGold) PrayerPalette.Gold.copy(alpha = 0.20f) else PrayerPalette.GoldLight.copy(alpha = 0.35f),
                    r * 0.44f,
                    c,
                )
            }
        }
        NixIconView(icon, if (secondary) ink else if (onGold) PrayerPalette.GoldLight else PrayerPalette.Night, size = size * 0.42f)
    }
}

/**
 * The plus / minus / tick button of a good-habit card: the ordinary round button, or the golden star for a prayer
 * habit. [look] says whether the card is already solid gold.
 */
@Composable
fun GoodHabitButton(
    prayer: Boolean,
    look: GoodFill,
    icon: NixIcon,
    onClick: () -> Unit,
    size: Dp,
    tint: Color,
    background: Color,
    secondary: Boolean = false,
    haptic: Boolean = false,
) {
    if (prayer) {
        PrayerStarButton(icon, onClick, size, onGold = look.fill >= 0.99f, secondary = secondary, ink = look.content, haptic = haptic)
    } else {
        NixIconButton(icon, onClick, size = size, tint = tint, background = background, bordered = false, haptic = haptic)
    }
}

/**
 * One small star per prayer of the day. Each lights up gold with a little pop when it is counted. With a target of
 * five the stars carry the names of the five prayers.
 */
@Composable
fun PrayerBeads(count: Int, target: Int, look: GoodFill, names: List<String>, modifier: Modifier = Modifier) {
    val solid = look.fill >= 0.99f
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        for (i in 0 until target) {
            val lit = i < count
            val pop by animateFloatAsState(
                if (lit) 1f else 0f,
                spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
                label = "bead$i",
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(26.dp).graphicsLayer { scaleX = 0.85f + 0.3f * pop; scaleY = 0.85f + 0.3f * pop }) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension / 2f
                    val star = octagramPath(c.x, c.y, r * 0.92f)
                    if (pop > 0.01f) {
                        val litColor = if (solid) PrayerPalette.Night else PrayerPalette.Gold
                        drawCircle(litColor.copy(alpha = (0.22f * pop).coerceIn(0f, 1f)), r, c)
                        drawPath(star, litColor.copy(alpha = pop.coerceIn(0f, 1f)))
                    }
                    drawPath(star, look.content.copy(alpha = 0.40f), style = Stroke(1.2.dp.toPx()))
                }
                names.getOrNull(i)?.let {
                    Text(
                        it,
                        style = NixType.Small,
                        color = if (lit) look.content else look.contentMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
