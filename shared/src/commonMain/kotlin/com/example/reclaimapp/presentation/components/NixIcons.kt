package com.example.reclaimapp.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class NixIcon {
    Home, Shield, CircleCheck, Calendar, Sparkle, User, Sliders, Plus, Back, Chevron, Bell, Trash,
    Pencil, Flame, Send, Close, Tick, Minus, Reset, Archive, Moon, Sun, Globe, Clock, Info, Leaf,
}

/** Hand-drawn 24×24 line icons (no icon library dependency). */
@Composable
fun NixIconView(icon: NixIcon, tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp, strokeWidth: Float = 1.8f) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        val stroke = Stroke(width = strokeWidth * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawNixIcon(icon, tint, s, stroke)
    }
}

private fun DrawScope.line(c: Color, s: Float, w: Float, x1: Float, y1: Float, x2: Float, y2: Float) =
    drawLine(c, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), strokeWidth = w, cap = StrokeCap.Round)

private fun DrawScope.poly(c: Color, s: Float, st: Stroke, close: Boolean, vararg pts: Float) {
    val path = Path()
    path.moveTo(pts[0] * s, pts[1] * s)
    var i = 2
    while (i < pts.size) {
        path.lineTo(pts[i] * s, pts[i + 1] * s)
        i += 2
    }
    if (close) path.close()
    drawPath(path, c, style = st)
}

private fun DrawScope.drawNixIcon(icon: NixIcon, c: Color, s: Float, st: Stroke) {
    val w = st.width
    when (icon) {
        NixIcon.Home -> {
            poly(c, s, st, false, 3f, 11f, 12f, 3.5f, 21f, 11f)
            poly(c, s, st, false, 5.5f, 9.5f, 5.5f, 20f, 18.5f, 20f, 18.5f, 9.5f)
            poly(c, s, st, false, 10f, 20f, 10f, 14f, 14f, 14f, 14f, 20f)
        }
        NixIcon.Shield -> {
            val p = Path()
            p.moveTo(12f * s, 3f * s)
            p.lineTo(19.5f * s, 6f * s)
            p.lineTo(19.5f * s, 11.5f * s)
            p.cubicTo(19.5f * s, 16f * s, 16.5f * s, 19.5f * s, 12f * s, 21f * s)
            p.cubicTo(7.5f * s, 19.5f * s, 4.5f * s, 16f * s, 4.5f * s, 11.5f * s)
            p.lineTo(4.5f * s, 6f * s)
            p.close()
            drawPath(p, c, style = st)
        }
        NixIcon.CircleCheck -> {
            drawCircle(c, radius = 9f * s, center = Offset(12f * s, 12f * s), style = st)
            poly(c, s, st, false, 8f, 12.5f, 11f, 15.5f, 16f, 9.5f)
        }
        NixIcon.Calendar -> {
            drawRoundRect(c, Offset(4f * s, 5.5f * s), Size(16f * s, 14.5f * s), CornerRadius(2.5f * s), style = st)
            line(c, s, w, 4f, 10f, 20f, 10f)
            line(c, s, w, 8.5f, 3.5f, 8.5f, 7f)
            line(c, s, w, 15.5f, 3.5f, 15.5f, 7f)
        }
        NixIcon.Sparkle -> {
            poly(c, s, st, true, 12f, 3f, 14f, 10f, 21f, 12f, 14f, 14f, 12f, 21f, 10f, 14f, 3f, 12f, 10f, 10f)
        }
        NixIcon.User -> {
            drawCircle(c, radius = 4f * s, center = Offset(12f * s, 8f * s), style = st)
            val p = Path()
            p.moveTo(4.5f * s, 20f * s)
            p.cubicTo(4.5f * s, 15f * s, 8f * s, 13.5f * s, 12f * s, 13.5f * s)
            p.cubicTo(16f * s, 13.5f * s, 19.5f * s, 15f * s, 19.5f * s, 20f * s)
            drawPath(p, c, style = st)
        }
        NixIcon.Sliders -> {
            line(c, s, w, 4f, 7f, 6.6f, 7f); line(c, s, w, 11.4f, 7f, 20f, 7f)
            drawCircle(c, 2.4f * s, Offset(9f * s, 7f * s), style = st)
            line(c, s, w, 4f, 12f, 12.6f, 12f); line(c, s, w, 17.4f, 12f, 20f, 12f)
            drawCircle(c, 2.4f * s, Offset(15f * s, 12f * s), style = st)
            line(c, s, w, 4f, 17f, 5.6f, 17f); line(c, s, w, 10.4f, 17f, 20f, 17f)
            drawCircle(c, 2.4f * s, Offset(8f * s, 17f * s), style = st)
        }
        NixIcon.Plus -> { line(c, s, w, 12f, 5f, 12f, 19f); line(c, s, w, 5f, 12f, 19f, 12f) }
        NixIcon.Back -> poly(c, s, st, false, 15f, 5f, 8f, 12f, 15f, 19f)
        NixIcon.Chevron -> poly(c, s, st, false, 9f, 5f, 16f, 12f, 9f, 19f)
        NixIcon.Bell -> {
            val p = Path()
            p.moveTo(6f * s, 17f * s)
            p.lineTo(18f * s, 17f * s)
            p.cubicTo(18f * s, 17f * s, 17f * s, 15.5f * s, 17f * s, 11f * s)
            p.cubicTo(17f * s, 7.8f * s, 14.8f * s, 5.5f * s, 12f * s, 5.5f * s)
            p.cubicTo(9.2f * s, 5.5f * s, 7f * s, 7.8f * s, 7f * s, 11f * s)
            p.cubicTo(7f * s, 15.5f * s, 6f * s, 17f * s, 6f * s, 17f * s)
            p.close()
            drawPath(p, c, style = st)
            line(c, s, w, 12f, 3.2f, 12f, 5.5f)
            line(c, s, w, 10.2f, 20f, 13.8f, 20f)
        }
        NixIcon.Trash -> {
            line(c, s, w, 4.5f, 7f, 19.5f, 7f)
            poly(c, s, st, false, 9f, 7f, 9f, 4.5f, 15f, 4.5f, 15f, 7f)
            poly(c, s, st, false, 6.5f, 7f, 7.5f, 20f, 16.5f, 20f, 17.5f, 7f)
            line(c, s, w, 10.5f, 11f, 10.5f, 16f); line(c, s, w, 13.5f, 11f, 13.5f, 16f)
        }
        NixIcon.Pencil -> {
            poly(c, s, st, true, 4f, 20f, 4f, 16f, 15.5f, 4.5f, 19.5f, 8.5f, 8f, 20f)
            line(c, s, w, 13f, 7f, 17f, 11f)
        }
        NixIcon.Flame -> {
            val p = Path()
            p.moveTo(12f * s, 3f * s)
            p.cubicTo(12f * s, 3f * s, 18f * s, 8f * s, 18f * s, 14f * s)
            p.cubicTo(18f * s, 17.6f * s, 15.5f * s, 20.5f * s, 12f * s, 20.5f * s)
            p.cubicTo(8.5f * s, 20.5f * s, 6f * s, 17.6f * s, 6f * s, 14f * s)
            p.cubicTo(6f * s, 11.5f * s, 7.6f * s, 10f * s, 8.5f * s, 8.5f * s)
            p.cubicTo(9.5f * s, 10.5f * s, 10.5f * s, 11f * s, 11f * s, 11f * s)
            p.cubicTo(11.6f * s, 8.6f * s, 12f * s, 6f * s, 12f * s, 3f * s)
            drawPath(p, c, style = st)
        }
        NixIcon.Send -> { line(c, s, w, 12f, 19f, 12f, 5f); poly(c, s, st, false, 6f, 11f, 12f, 5f, 18f, 11f) }
        NixIcon.Close -> { line(c, s, w, 6f, 6f, 18f, 18f); line(c, s, w, 18f, 6f, 6f, 18f) }
        NixIcon.Tick -> poly(c, s, st, false, 5f, 12.5f, 10f, 17.5f, 19f, 7f)
        NixIcon.Minus -> line(c, s, w, 5f, 12f, 19f, 12f)
        NixIcon.Reset -> {
            drawArc(c, -50f, 280f, false, Offset(4.5f * s, 4.5f * s), Size(15f * s, 15f * s), style = st)
            poly(c, s, st, false, 7.2f, 2.8f, 7.2f, 6.4f, 10.8f, 6.4f)
        }
        NixIcon.Archive -> {
            drawRoundRect(c, Offset(3.5f * s, 4f * s), Size(17f * s, 5f * s), CornerRadius(1.5f * s), style = st)
            poly(c, s, st, false, 5f, 9f, 5f, 19f, 19f, 19f, 19f, 9f)
            line(c, s, w, 10f, 13f, 14f, 13f)
        }
        NixIcon.Moon -> {
            val p = Path()
            p.moveTo(19.5f * s, 14.5f * s)
            p.cubicTo(18.5f * s, 15f * s, 17.3f * s, 15.3f * s, 16f * s, 15.3f * s)
            p.cubicTo(11.8f * s, 15.3f * s, 8.7f * s, 12.2f * s, 8.7f * s, 8f * s)
            p.cubicTo(8.7f * s, 6.700f * s, 9f * s, 5.5f * s, 9.5f * s, 4.5f * s)
            p.cubicTo(6.4f * s, 5.5f * s, 4.2f * s, 8.4f * s, 4.2f * s, 11.8f * s)
            p.cubicTo(4.2f * s, 16.1f * s, 7.7f * s, 19.6f * s, 12f * s, 19.6f * s)
            p.cubicTo(15.4f * s, 19.6f * s, 18.4f * s, 17.4f * s, 19.5f * s, 14.5f * s)
            p.close()
            drawPath(p, c, style = st)
        }
        NixIcon.Sun -> {
            drawCircle(c, 4f * s, Offset(12f * s, 12f * s), style = st)
            for (i in 0 until 8) {
                val a = i * PI / 4
                val x1 = 12f + 7f * cos(a).toFloat(); val y1 = 12f + 7f * sin(a).toFloat()
                val x2 = 12f + 9.4f * cos(a).toFloat(); val y2 = 12f + 9.4f * sin(a).toFloat()
                line(c, s, w, x1, y1, x2, y2)
            }
        }
        NixIcon.Globe -> {
            drawCircle(c, 9f * s, Offset(12f * s, 12f * s), style = st)
            drawOval(c, Offset(8f * s, 3f * s), Size(8f * s, 18f * s), style = st)
            line(c, s, w, 3f, 12f, 21f, 12f)
        }
        NixIcon.Clock -> {
            drawCircle(c, 9f * s, Offset(12f * s, 12f * s), style = st)
            poly(c, s, st, false, 12f, 7f, 12f, 12f, 15.5f, 14f)
        }
        NixIcon.Info -> {
            drawCircle(c, 9f * s, Offset(12f * s, 12f * s), style = st)
            line(c, s, w, 12f, 11f, 12f, 16.5f)
            line(c, s, w, 12f, 7.8f, 12f, 7.9f)
        }
        NixIcon.Leaf -> {
            val p = Path()
            p.moveTo(5f * s, 19f * s)
            p.cubicTo(5f * s, 10f * s, 10f * s, 5f * s, 19f * s, 5f * s)
            p.cubicTo(19f * s, 14f * s, 14f * s, 19f * s, 5f * s, 19f * s)
            p.close()
            drawPath(p, c, style = st)
            line(c, s, w, 5f, 19f, 13f, 11f)
        }
    }
}
