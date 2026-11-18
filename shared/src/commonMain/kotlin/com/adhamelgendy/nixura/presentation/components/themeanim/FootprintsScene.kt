package com.adhamelgendy.nixura.presentation.components.themeanim

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Walking: two trails of bare footprints walk down the page, toes first, left and right feet in turn. One starts in
 * the top end corner beside the title, the other in the top start corner a few seconds later, so they cross behind
 * the form and leave through the open bottom of the page. Each print presses in, stays a moment and fades, so several
 * pairs are always on screen. Mirrored in RTL. With "remove animations": one still moment of both walks.
 */
internal object FootprintsScene : ThemeScene {

    /** Seconds between two steps: an easy walking pace. */
    private const val STEP_S = 0.42f

    /** Pause after a walk before it starts again. */
    private const val REST_S = 1.2f

    /** The second walker starts this much later, so the two trails are at different heights. */
    private const val SECOND_LANE_DELAY_S = 5.5f

    /** The moment shown with "remove animations": both walks partly done. */
    private const val STILL_AT_S = 9f

    @Composable
    override fun Content(params: ThemeSceneParams) {
        val presence = rememberPresence(params)
        val time = rememberSceneTime(params.running, stillAt = STILL_AT_S)
        val color = params.accent
        val peak = if (params.colors.isDark) 0.55f else 0.65f
        val still = params.reducedMotion
        val rtl = params.rtl
        val bleed = params.bleed
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val left = -bleed.left
                val top = -bleed.top
                val w = size.width + bleed.left + bleed.right
                val h = size.height + bleed.top + bleed.bottom
                val stride = 34.dp.toPx()
                val gait = 9.dp.toPx()
                val first = ThemeAnimMath.footprintTrail(w, h, top, stride, gait)
                val second = ThemeAnimMath.footprintTrail(w, h, top, stride, gait, mirrored = true)
                val leftFoot = footPath(this, leftFoot = true)
                val rightFoot = footPath(this, leftFoot = false)
                val cycle = maxOf(first.size, second.size) * STEP_S + ThemeAnimMath.PRINT_HOLD_S + ThemeAnimMath.PRINT_FADE_S + REST_S
                onDrawBehind {
                    val p = presence.value
                    if (p <= 0f || first.isEmpty()) return@onDrawBehind
                    val t = if (still) STILL_AT_S else time.value
                    // RTL: the same walks seen in a mirror.
                    scale(scaleX = if (rtl) -1f else 1f, scaleY = 1f, pivot = Offset(left + w / 2f, top + h / 2f)) {
                        drawTrail(first, t % cycle, left, leftFoot, rightFoot, color, peak * p)
                        val t2 = t - SECOND_LANE_DELAY_S
                        if (t2 >= 0f) drawTrail(second, t2 % cycle, left, leftFoot, rightFoot, color, peak * p)
                    }
                }
            },
        )
    }

    private fun DrawScope.drawTrail(
        trail: List<ThemeAnimMath.Footprint>,
        now: Float,
        left: Float,
        leftFoot: Path,
        rightFoot: Path,
        color: androidx.compose.ui.graphics.Color,
        strength: Float,
    ) {
        trail.forEachIndexed { i, print ->
            val a = ThemeAnimMath.printAlpha(now - i * STEP_S)
            if (a <= 0f) return@forEachIndexed
            translate(left + print.x, print.y) {
                rotate(print.angle, pivot = Offset.Zero) {
                    drawPath(if (print.left) leftFoot else rightFoot, color.copy(alpha = strength * a))
                }
            }
        }
    }

    /**
     * A bare footprint centred on (0, 0) with the toes up, 30 × 13 dp. The two feet are mirror images: the ball of the
     * foot and the big toe sit on the inner side, the arch leaves a gap on the inner side and the outer edge runs from
     * the ball to the heel, so a left and a right print are told apart at a glance.
     */
    private fun footPath(density: Density, leftFoot: Boolean): Path = with(density) {
        val length = 30.dp.toPx()
        val width = 13.dp.toPx()
        // Inner side: right for a left foot, left for a right foot.
        val inner = if (leftFoot) 1f else -1f
        // Points in units of the foot: x across (mirrored per foot), y along (-0.5 toes … 0.5 heel).
        fun px(x: Float) = inner * x * width
        fun py(y: Float) = y * length
        fun oval(cx: Float, cy: Float, rx: Float, ry: Float) =
            Rect(px(cx) - rx * width, py(cy) - ry * length, px(cx) + rx * width, py(cy) + ry * length)
        Path().apply {
            // The sole as one smooth outline: round heel, straight outer edge, wide ball, and the arch curving in on
            // the inner side.
            moveTo(px(0f), py(0.48f))
            cubicTo(px(-0.30f), py(0.48f), px(-0.38f), py(0.40f), px(-0.36f), py(0.30f))
            cubicTo(px(-0.34f), py(0.18f), px(-0.46f), py(0.06f), px(-0.42f), py(-0.06f))
            cubicTo(px(-0.38f), py(-0.30f), px(0.30f), py(-0.38f), px(0.44f), py(-0.20f))
            cubicTo(px(0.54f), py(-0.08f), px(0.30f), py(0.02f), px(0.22f), py(0.10f))
            cubicTo(px(0.14f), py(0.18f), px(0.34f), py(0.24f), px(0.32f), py(0.34f))
            cubicTo(px(0.30f), py(0.44f), px(0.16f), py(0.48f), px(0f), py(0.48f))
            close()
            // Toes: a big toe on the inner side, then smaller ones stepping down towards the outer side.
            addOval(oval(0.30f, -0.41f, 0.18f, 0.09f))
            addOval(oval(0.02f, -0.43f, 0.11f, 0.06f))
            addOval(oval(-0.17f, -0.40f, 0.10f, 0.055f))
            addOval(oval(-0.33f, -0.35f, 0.09f, 0.05f))
            addOval(oval(-0.46f, -0.28f, 0.08f, 0.045f))
        }
    }
}
