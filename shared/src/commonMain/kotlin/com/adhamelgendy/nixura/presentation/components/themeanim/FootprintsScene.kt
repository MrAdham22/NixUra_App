package com.adhamelgendy.nixura.presentation.components.themeanim

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/**
 * Walking: footprints appear one by one across the page, from the bottom start corner to the top end corner, and fade
 * behind. Mirrored in RTL so the walk follows the reading direction. Still frame: a few prints of one walk.
 */
internal object FootprintsScene : ThemeScene {

    private const val STEPS_PER_WALK = 14
    private const val STEP_S = 0.55f
    private const val VISIBLE = 7

    @Composable
    override fun Content(params: ThemeSceneParams) {
        val presence = rememberPresence(params)
        val time = rememberSceneTime(params.running, stillAt = STEP_S * 8)
        val color = params.accent
        val rtl = params.rtl
        Spacer(
            Modifier.fillMaxSize().drawBehind {
                val p = presence.value
                if (p <= 0f) return@drawBehind
                val now = time.value / STEP_S
                val newest = floor(now).toInt()
                for (k in 0 until VISIBLE) {
                    val step = newest - k
                    if (step < 0) break
                    val age = (now - step) / VISIBLE
                    val alpha = (1f - age).coerceIn(0f, 1f) * 0.30f * p
                    val print = ThemeAnimMath.footprint(step, STEPS_PER_WALK)
                    val x = if (rtl) 1f - print.x else print.x
                    val angle = if (rtl) -print.angle else print.angle
                    drawFoot(Offset(x * size.width, print.y * size.height), angle, print.left != rtl, color.copy(alpha = alpha))
                }
            },
        )
    }

    private fun DrawScope.drawFoot(at: Offset, angle: Float, left: Boolean, color: Color) {
        if (color.alpha <= 0f) return
        rotate(angle, pivot = at) {
            val w = 12.dp.toPx()
            val h = 22.dp.toPx()
            // Sole and heel
            drawOval(color, topLeft = Offset(at.x - w / 2f, at.y - h / 2f), size = Size(w, h * 0.7f))
            drawOval(color, topLeft = Offset(at.x - w * 0.4f, at.y + h * 0.25f), size = Size(w * 0.8f, h * 0.38f))
            // Toes, the big toe on the inner side of each foot.
            val inner = if (left) 1f else -1f
            for (i in 0 until 4) {
                val r = (2.4f - i * 0.35f).dp.toPx()
                val x = at.x + inner * (1.2f - i) * 3.dp.toPx()
                drawCircle(color, r, Offset(x, at.y - h * 0.62f + i * 1.1.dp.toPx()))
            }
        }
    }
}
