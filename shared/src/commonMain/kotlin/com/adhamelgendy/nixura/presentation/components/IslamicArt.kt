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
    val shimmerAnim = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "shimmer")
    val still = remember { FixedState(0.62f) }
    val shimmer: State<Float> = if (reduced) still else shimmerAnim

    val done = ((fill - 0.92f) / 0.08f).coerceIn(0f, 1f)
    val content = lerp(PrayerPalette.Cream, PrayerPalette.Night, done)
    val art = Modifier.drawWithCache {
        val w = size.width
        val h = size.height
        val tiling = starTiling(w, h, 46.dp.toPx())
        val corner = 20.dp.toPx()
        val frame = 1.6.dp.toPx()
        val inset = 5.dp.toPx()
        // Sized by the card's height too, so a short row gets a small mosque that never reaches its text
        // A short row (Home) shows a big mosque filling its height, the owner's favourite; a full card keeps it
        // smaller in the room left for it at the bottom, with the moon at the top centre, clear of the buttons.
        val shortRow = h < 120.dp.toPx()
        val mosqueWidth = if (shortRow) minOf(w * 0.56f, 240.dp.toPx()) else minOf(w * 0.46f, 200.dp.toPx())
        val mosque = mosquePath((w - mosqueWidth) / 2f, h + 1f, mosqueWidth)
        val domeTop = Offset(w / 2f, h - mosqueWidth * MOSQUE_DOME_TOP)
        val moonCenter = if (shortRow) Offset(w * 0.58f, 14.dp.toPx()) else Offset(w / 2f, 12.dp.toPx())
        val moon = crescentPath(moonCenter.x, moonCenter.y, 7.dp.toPx())
        // On the inner frame, halfway round each rounded corner
        val d = corner - (corner - inset) * 0.7071f
        val cornerStars = listOf(Offset(d, d), Offset(w - d, d), Offset(d, h - d), Offset(w - d, h - d))
            .map { octagramPath(it.x, it.y, 4.dp.toPx()) }
        onDrawBehind {
            // Night sky, a faint glow from above; replaced by solid gold when every prayer is done
            drawRect(Brush.verticalGradient(listOf(PrayerPalette.NightPlum, PrayerPalette.Night)))
            drawRect(Brush.radialGradient(listOf(PrayerPalette.Gold.copy(alpha = 0.10f), Color.Transparent), Offset(w * 0.58f, 0f), w * 0.6f))
            if (done > 0f) {
                drawRect(
                    Brush.linearGradient(listOf(PrayerPalette.GoldLight, PrayerPalette.Gold, PrayerPalette.GoldDeep), Offset.Zero, Offset(w, h)),
                    alpha = done,
                )
            }
            // The star tiling
            val lineColor = lerp(PrayerPalette.Gold, PrayerPalette.Bronze, done)
            drawPath(tiling, lineColor.copy(alpha = 0.08f + 0.08f * fill + 0.12f * done), style = Stroke(1.dp.toPx()))
            // A golden dawn behind the mosque that grows with every prayer
            val dawn = (0.25f + 0.75f * fill) * (1f - done)
            if (dawn > 0f) {
                drawRect(
                    Brush.radialGradient(
                        listOf(PrayerPalette.Gold.copy(alpha = 0.55f * dawn), PrayerPalette.Gold.copy(alpha = 0.12f * dawn), Color.Transparent),
                        Offset(w / 2f, h),
                        (w * (0.35f + 0.35f * fill)).coerceAtLeast(1f),
                    ),
                )
            }
            // Rays from the dome once the day is complete
            if (done > 0f) {
                for (i in 0 until 18) {
                    val a = PI_F + i * (PI_F / 17f)
                    val len = w * 0.7f
                    drawLine(
                        Color.White.copy(alpha = 0.30f * done),
                        domeTop,
                        Offset(domeTop.x + cos(a) * len, domeTop.y + sin(a) * len),
                        strokeWidth = (if (i % 2 == 0) 3f else 1.5f).dp.toPx(),
                    )
                }
            }
            // The mosque, a dark silhouette with a golden rim
            drawPath(mosque, lerp(Color(0xFF110B20), PrayerPalette.GoldDeep, done).copy(alpha = 0.78f))
            drawPath(mosque, lerp(PrayerPalette.Gold, PrayerPalette.Night, done).copy(alpha = 0.45f), style = Stroke(1.dp.toPx()))
            // Crescent moon
            val moonColor = lerp(PrayerPalette.GoldLight, PrayerPalette.Night, done)
            drawCircle(moonColor.copy(alpha = 0.16f), 15.dp.toPx(), moonCenter)
            drawPath(moon, moonColor.copy(alpha = 0.92f))
            // Light that keeps sliding across. Two bands half a loop apart: one leaves the card while the other is in
            // the middle, and each wraps around only while it is fully outside, so the loop never shows a seam.
            drawShine(shimmer.value, w, h)
            drawShine((shimmer.value + 0.5f) % 1f, w, h)
            drawBurst(burst.value, w, h)
            // Double gold frame with a small star at each corner
            val frameBrush = Brush.linearGradient(listOf(PrayerPalette.GoldLight, PrayerPalette.GoldDeep, PrayerPalette.Gold), Offset.Zero, Offset(w, h))
            drawRoundRect(
                frameBrush,
                topLeft = Offset(frame / 2f, frame / 2f),
                size = Size(w - frame, h - frame),
                cornerRadius = CornerRadius(corner),
                style = Stroke(frame),
                alpha = 0.85f,
            )
            drawRoundRect(
                frameBrush,
                topLeft = Offset(inset, inset),
                size = Size(w - inset * 2f, h - inset * 2f),
                cornerRadius = CornerRadius(corner - inset),
                style = Stroke(0.8.dp.toPx()),
                alpha = 0.35f,
            )
            cornerStars.forEach { drawPath(it, PrayerPalette.Gold.copy(alpha = 0.9f)) }
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

/**
 * One diagonal band of light at loop position [p] (0..1). It travels from fully left of the card to fully right of
 * it, so the jump from 1 back to 0 happens out of sight.
 */
private fun DrawScope.drawShine(p: Float, w: Float, h: Float) {
    val half = w * 0.22f
    val x = -half * 2f + p * (w + half * 4f)
    drawRect(
        Brush.linearGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent),
            Offset(x - half, 0f),
            Offset(x + half, h * 0.45f),
        ),
    )
}

/** Height of the mosque's main dome top, as a share of the mosque's width. */
internal const val MOSQUE_DOME_TOP = 0.33f

/**
 * A mosque silhouette standing on [baseY]: a wall, a large central dome on a drum, two small domes and two minarets
 * with balconies and pointed caps. [width] sets the size; it is about 0.42 × width tall.
 */
internal fun mosquePath(left: Float, baseY: Float, width: Float): Path {
    val u = width
    val path = Path()
    fun x(f: Float) = left + f * u
    fun rect(l: Float, t: Float, r: Float, b: Float) = path.addRect(androidx.compose.ui.geometry.Rect(x(l), baseY - t * u, x(r), baseY - b * u))
    fun dome(cx: Float, base: Float, rx: Float, ry: Float) {
        path.moveTo(x(cx - rx), baseY - base * u)
        path.arcTo(
            androidx.compose.ui.geometry.Rect(x(cx - rx), baseY - (base + ry) * u, x(cx + rx), baseY - (base - ry) * u),
            180f, 180f, false,
        )
        path.close()
    }
    fun cap(cx: Float, base: Float, hw: Float, height: Float) {
        path.moveTo(x(cx - hw), baseY - base * u)
        path.lineTo(x(cx), baseY - (base + height) * u)
        path.lineTo(x(cx + hw), baseY - base * u)
        path.close()
    }
    // Wall
    rect(0.05f, 0.08f, 0.95f, 0f)
    // Central drum and dome, with a finial
    rect(0.36f, 0.13f, 0.64f, 0.07f)
    dome(0.5f, 0.13f, 0.15f, 0.17f)
    rect(0.495f, MOSQUE_DOME_TOP + 0.03f, 0.505f, 0.29f)
    path.addOval(androidx.compose.ui.geometry.Rect(x(0.488f), baseY - (MOSQUE_DOME_TOP + 0.05f) * u, x(0.512f), baseY - (MOSQUE_DOME_TOP + 0.026f) * u))
    // Side domes
    for (c in listOf(0.24f, 0.76f)) {
        rect(c - 0.075f, 0.11f, c + 0.075f, 0.07f)
        dome(c, 0.11f, 0.075f, 0.08f)
        rect(c - 0.004f, 0.215f, c + 0.004f, 0.18f)
    }
    // Minarets
    for (c in listOf(0.08f, 0.92f)) {
        rect(c - 0.016f, 0.36f, c + 0.016f, 0.02f)
        rect(c - 0.03f, 0.29f, c + 0.03f, 0.27f)
        cap(c, 0.36f, 0.022f, 0.07f)
    }
    return path
}

/** A crescent moon around ([cx], [cy]) with outer radius [r], opening to the upper right. */
internal fun crescentPath(cx: Float, cy: Float, r: Float): Path {
    val outer = Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r)) }
    val ir = r * 0.82f
    val ox = cx + r * 0.42f
    val oy = cy - r * 0.28f
    val inner = Path().apply { addOval(androidx.compose.ui.geometry.Rect(ox - ir, oy - ir, ox + ir, oy + ir)) }
    return Path().apply { op(outer, inner, androidx.compose.ui.graphics.PathOperation.Difference) }
}

/** A pointed arch (a mihrab) inside a gold ring: the badge of a prayer card. */
@Composable
fun PrayerArchBadge(look: GoodFill, modifier: Modifier = Modifier) {
    val solid = look.fill >= 0.99f
    Canvas(modifier.size(44.dp)) {
        val s = size.minDimension
        val c = Offset(s / 2f, s / 2f)
        drawCircle(if (solid) PrayerPalette.Night.copy(alpha = 0.85f) else PrayerPalette.Night.copy(alpha = 0.55f), s / 2f, c)
        drawCircle(
            Brush.linearGradient(listOf(PrayerPalette.GoldLight, PrayerPalette.GoldDeep), Offset.Zero, Offset(s, s)),
            s / 2f - 1.dp.toPx(),
            c,
            style = Stroke(1.6.dp.toPx()),
        )
        fun arch(scale: Float): Path {
            val l = s * (0.5f - 0.2f * scale)
            val r = s * (0.5f + 0.2f * scale)
            val bottom = s * 0.76f
            val spring = s * (0.5f - 0.02f * scale)
            val apex = s * (0.5f - 0.32f * scale)
            return Path().apply {
                moveTo(l, bottom)
                lineTo(l, spring)
                cubicTo(l, s * 0.5f - (s * 0.5f - apex) * 0.55f, s * 0.5f - s * 0.06f * scale, apex + s * 0.05f * scale, s * 0.5f, apex)
                cubicTo(s * 0.5f + s * 0.06f * scale, apex + s * 0.05f * scale, r, s * 0.5f - (s * 0.5f - apex) * 0.55f, r, spring)
                lineTo(r, bottom)
            }
        }
        val gold = PrayerPalette.Gold
        drawPath(arch(1f), Brush.verticalGradient(listOf(gold.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(arch(1f), gold, style = Stroke(1.6.dp.toPx()))
        drawPath(arch(0.62f), gold.copy(alpha = 0.6f), style = Stroke(1.dp.toPx()))
        drawLine(gold, Offset(s * 0.24f, s * 0.76f), Offset(s * 0.76f, s * 0.76f), strokeWidth = 1.6.dp.toPx())
    }
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
