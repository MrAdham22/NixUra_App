package com.adhamelgendy.nixura.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.adhamelgendy.nixura.presentation.appearance.CardGradient
import com.adhamelgendy.nixura.presentation.appearance.LiquidGeometry
import com.adhamelgendy.nixura.presentation.appearance.prefersLightContentOnCard
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Paints widget card backgrounds that a tinted shape cannot show: the quit card's diagonal gradient with its corner
 * glow (as `habitCardFill`) and the water habit's liquid level (as `liquidFill`, frozen: widgets cannot animate, so the
 * level changes in a step on every count change). The result goes into the layout's `w_bg` ImageView (fitXY).
 *
 * Bitmaps are drawn at most [MAX_PIXELS] so a large widget stays far below the RemoteViews bitmap limit; the soft
 * shapes do not show the scaling.
 */
internal object WidgetCardPainter {
    private const val MAX_PIXELS = 360_000

    fun gradient(widthPx: Int, heightPx: Int, radiusPx: Float, g: CardGradient): Bitmap = draw(widthPx, heightPx, radiusPx) { w, h, clip ->
        val diagonal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, w, h, intArrayOf(g.start.toArgb(), g.middle.toArgb(), g.end.toArgb()), null, Shader.TileMode.CLAMP)
        }
        drawPath(clip, diagonal)
        val glowAlpha = if (g.middle.prefersLightContentOnCard()) 0.13f else 0.28f
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.9f, 0f, maxOf(w, h) * 0.75f,
                Color.White.copy(alpha = glowAlpha).toArgb(), Color.Transparent.toArgb(), Shader.TileMode.CLAMP,
            )
        }
        drawPath(clip, glow)
    }

    fun liquid(widthPx: Int, heightPx: Int, radiusPx: Float, level: Float, empty: Color, surface: Color, depth: Color): Bitmap =
        draw(widthPx, heightPx, radiusPx) { w, h, clip ->
            drawPath(clip, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = empty.toArgb() })
            val l = level.coerceIn(0f, 1f)
            if (l <= 0f) return@draw
            save()
            clipPath(clip)
            val amplitude = h * 0.035f * LiquidGeometry.calmness(l)
            val top = h * (1f - l)
            val back = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surface.copy(alpha = 0.45f).toArgb() }
            drawPath(wavePath(w, h, top - amplitude * 0.4f, amplitude * 0.8f, 1.7f + PHASE, 1.3f), back)
            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, top - amplitude, 0f, h, surface.toArgb(), depth.toArgb(), Shader.TileMode.CLAMP)
            }
            drawPath(wavePath(w, h, top, amplitude, -PHASE, 1f), body)
            restore()
        }

    private fun wavePath(w: Float, h: Float, top: Float, amplitude: Float, phase: Float, cycles: Float): Path = Path().apply {
        moveTo(0f, h)
        val steps = 48
        for (i in 0..steps) {
            val x = w * i / steps
            lineTo(x, LiquidGeometry.surfaceY(x, w, top, amplitude, phase, cycles))
        }
        lineTo(w, h)
        close()
    }

    private inline fun draw(widthPx: Int, heightPx: Int, radiusPx: Float, block: Canvas.(Float, Float, Path) -> Unit): Bitmap {
        val w0 = widthPx.coerceAtLeast(1)
        val h0 = heightPx.coerceAtLeast(1)
        val scale = minOf(1f, sqrt(MAX_PIXELS.toFloat() / (w0.toLong() * h0)))
        val w = (w0 * scale).toInt().coerceAtLeast(1)
        val h = (h0 * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val r = radiusPx * scale
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, Path.Direction.CW) }
        canvas.block(w.toFloat(), h.toFloat(), clip)
        return bitmap
    }

    /** A still wave looks natural slightly off its crest. */
    private const val PHASE = (PI * 0.35).toFloat()
}
