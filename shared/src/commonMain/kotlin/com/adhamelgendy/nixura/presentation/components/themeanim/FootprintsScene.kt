package com.adhamelgendy.nixura.presentation.components.themeanim

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Walking: a trail of footprints walks up the page, left and right feet in turn, each print pressed in and then
 * fading behind. The path starts in the open bottom of the page, passes behind the form and leaves through the empty
 * top end corner beside the title. Mirrored in RTL. With "remove animations": the whole trail, still and soft.
 */
internal object FootprintsScene : ThemeScene {

    /** Seconds between two steps: an easy walking pace. */
    private const val STEP_S = 0.42f

    /** Pause after a walk before the next one starts. */
    private const val REST_S = 0.8f

    @Composable
    override fun Content(params: ThemeSceneParams) {
        val presence = rememberPresence(params)
        val time = rememberSceneTime(params.running)
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
                val trail = ThemeAnimMath.footprintTrail(w, h, top, stride = 34.dp.toPx(), gait = 8.dp.toPx())
                val leftFoot = footPath(this, leftFoot = true)
                val rightFoot = footPath(this, leftFoot = false)
                val cycle = trail.size * STEP_S + ThemeAnimMath.PRINT_HOLD_S + ThemeAnimMath.PRINT_FADE_S + REST_S
                onDrawBehind {
                    val p = presence.value
                    if (p <= 0f || trail.isEmpty()) return@onDrawBehind
                    val now = if (still) 0f else time.value % cycle
                    // RTL: the same walk seen in a mirror, so it starts at the bottom right and leaves top left.
                    scale(scaleX = if (rtl) -1f else 1f, scaleY = 1f, pivot = Offset(left + w / 2f, top + h / 2f)) {
                        trail.forEachIndexed { i, print ->
                            val a = if (still) 0.6f else ThemeAnimMath.printAlpha(now - i * STEP_S)
                            if (a <= 0f) return@forEachIndexed
                            translate(left + print.x, print.y) {
                                rotate(print.angle, pivot = Offset.Zero) {
                                    drawPath(if (print.left) leftFoot else rightFoot, color.copy(alpha = peak * a * p))
                                }
                            }
                        }
                    }
                }
            },
        )
    }

    /**
     * A bare footprint centred on (0, 0), toes up: forefoot, a narrow arch on the outer side, the heel, five toes
     * with the big toe on the inner side. 30 × 13 dp.
     */
    private fun footPath(density: Density, leftFoot: Boolean): Path = with(density) {
        val length = 30.dp.toPx()
        val width = 13.dp.toPx()
        // Inner side: right for a left foot, left for a right foot.
        val inner = if (leftFoot) 1f else -1f
        Path().apply {
            // Forefoot (ball of the foot), a little towards the inner side.
            addOval(Rect(center = Offset(inner * width * 0.06f, -length * 0.12f), radius = 0f).inflateTo(width * 0.5f, length * 0.24f))
            // Outer edge of the arch, joining forefoot and heel.
            addOval(Rect(center = Offset(-inner * width * 0.16f, length * 0.12f), radius = 0f).inflateTo(width * 0.26f, length * 0.2f))
            // Heel
            addOval(Rect(center = Offset(0f, length * 0.33f), radius = 0f).inflateTo(width * 0.4f, length * 0.16f))
            // Toes, big toe first on the inner side, getting smaller towards the outer side.
            val toes = listOf(
                Triple(0.30f, -0.46f, 0.15f),
                Triple(0.06f, -0.49f, 0.11f),
                Triple(-0.13f, -0.46f, 0.10f),
                Triple(-0.30f, -0.41f, 0.09f),
                Triple(-0.44f, -0.34f, 0.08f),
            )
            for ((tx, ty, r) in toes) {
                val radius = width * r
                addOval(Rect(center = Offset(inner * tx * width, ty * length), radius = radius))
            }
        }
    }

    private fun Rect.inflateTo(halfWidth: Float, halfHeight: Float): Rect =
        Rect(center.x - halfWidth, center.y - halfHeight, center.x + halfWidth, center.y + halfHeight)
}
