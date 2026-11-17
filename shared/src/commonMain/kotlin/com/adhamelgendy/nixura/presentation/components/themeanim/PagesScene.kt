package com.adhamelgendy.nixura.presentation.components.themeanim

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Reading: a few loose pages drift down the page, turning over as they fall. Still frame: they hang mid-air. */
internal object PagesScene : ThemeScene {

    private const val PAGES = 7

    @Composable
    override fun Content(params: ThemeSceneParams) {
        val presence = rememberPresence(params)
        val time = rememberSceneTime(params.running)
        val ink = params.colors.textPrimary
        val accent = params.accent
        Spacer(
            Modifier.fillMaxSize().drawBehind {
                val p = presence.value
                if (p <= 0f) return@drawBehind
                val t = time.value
                val w = 34.dp.toPx()
                val h = 44.dp.toPx()
                for (i in 0 until PAGES) {
                    val page = ThemeAnimMath.page(i, t)
                    val at = Offset(page.x * size.width, page.y * size.height)
                    val a = 0.12f * p
                    rotate(page.rotation, pivot = at) {
                        // The flip squeezes the page sideways, as if it were turning over in the air.
                        scale(scaleX = abs(page.flip).coerceAtLeast(0.08f), scaleY = 1f, pivot = at) {
                            val tl = Offset(at.x - w / 2f, at.y - h / 2f)
                            drawRoundRect(ink.copy(alpha = a), tl, Size(w, h), CornerRadius(4.dp.toPx()))
                            drawRoundRect(accent.copy(alpha = a * 1.6f), tl, Size(w, h), CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx()))
                            for (line in 0 until 4) {
                                val y = tl.y + 10.dp.toPx() + line * 7.dp.toPx()
                                val end = tl.x + w - 6.dp.toPx() - (line % 2) * 6.dp.toPx()
                                drawLine(ink.copy(alpha = a * 1.4f), Offset(tl.x + 6.dp.toPx(), y), Offset(end, y), 1.dp.toPx())
                            }
                        }
                    }
                }
            },
        )
    }
}
