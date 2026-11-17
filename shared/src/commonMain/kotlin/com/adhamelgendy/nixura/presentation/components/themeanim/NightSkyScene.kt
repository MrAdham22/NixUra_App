package com.adhamelgendy.nixura.presentation.components.themeanim

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.presentation.theme.NixUraPalette

/**
 * Sleep: the top of the page turns into a night sky with twinkling stars and a crescent moon in the top end corner.
 * On the light theme it is a faint plum dusk so the dark text stays readable. Still frame: stars without twinkle.
 */
internal object NightSkyScene : ThemeScene {

    private const val STARS = 40

    @Composable
    override fun Content(params: ThemeSceneParams) {
        val presence = rememberPresence(params, inMs = 1200)
        val time = rememberSceneTime(params.running)
        val dark = params.colors.isDark
        val bleed = params.bleed
        val rtl = params.rtl
        val reduced = params.reducedMotion
        val background = params.colors.background
        val star = if (dark) Color(0xFFFFF4E0) else NixUraPalette.Plum
        val sky = if (dark) Color(0xFF0E0C22) else NixUraPalette.Plum
        val skyAlpha = if (dark) 0.75f else 0.10f
        val moon = if (dark) Color(0xFFFFE9C2) else NixUraPalette.Rose
        Spacer(
            Modifier.fillMaxSize().drawBehind {
                val p = presence.value
                if (p <= 0f) return@drawBehind
                val t = time.value
                val left = -bleed.left
                val top = -bleed.top
                val w = size.width + bleed.left + bleed.right
                val skyBottom = size.height * 0.6f
                drawRect(
                    Brush.verticalGradient(listOf(sky.copy(alpha = skyAlpha * p), Color.Transparent), startY = top, endY = skyBottom),
                    topLeft = Offset(left, top),
                    size = Size(w, skyBottom - top),
                )
                for (i in 0 until STARS) {
                    val x = left + ThemeAnimMath.seeded(i, 20) * w
                    val y = top + ThemeAnimMath.seeded(i, 21) * (skyBottom - top) * 0.9f
                    val tw = if (reduced) 0.7f else ThemeAnimMath.twinkle(i, t)
                    val r = (0.8f + ThemeAnimMath.seeded(i, 22) * 1.6f).dp.toPx()
                    drawCircle(star.copy(alpha = (if (dark) 0.85f else 0.35f) * tw * p), r, Offset(x, y))
                }
                // Crescent in the top end corner: a disc with a bite the colour of the sky behind it.
                val mr = 13.dp.toPx()
                val mx = if (rtl) left + 40.dp.toPx() else left + w - 40.dp.toPx()
                val my = 30.dp.toPx()
                drawCircle(Brush.radialGradient(listOf(moon.copy(alpha = 0.25f * p), Color.Transparent), Offset(mx, my), mr * 3f), mr * 3f, Offset(mx, my))
                drawCircle(moon.copy(alpha = 0.9f * p), mr, Offset(mx, my))
                val biteColor = if (dark) androidx.compose.ui.graphics.lerp(background, sky, skyAlpha) else background
                val side = if (rtl) -1f else 1f
                drawCircle(biteColor.copy(alpha = p), mr * 0.85f, Offset(mx + side * mr * 0.45f, my - mr * 0.25f))
            },
        )
    }
}
