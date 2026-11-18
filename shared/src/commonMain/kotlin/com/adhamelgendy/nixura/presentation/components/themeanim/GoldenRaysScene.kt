package com.adhamelgendy.nixura.presentation.components.themeanim

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import com.adhamelgendy.nixura.presentation.theme.PrayerPalette
import kotlin.math.sin

/**
 * Prayer: soft golden light rays fanning down from above the top centre, slowly swaying and breathing. Drawn over the
 * gold wash ([com.adhamelgendy.nixura.presentation.components.PrayerGoldWash]), which stays the prayer background;
 * this only adds the rays. Still frame: the rays without movement.
 */
internal object GoldenRaysScene : ThemeScene {

    private const val RAYS = 7

    @Composable
    override fun Content(params: ThemeSceneParams) {
        val presence = rememberPresence(params, inMs = 1600)
        val time = rememberSceneTime(params.running)
        val bleed = params.bleed
        val path = remember { Path() }
        Spacer(
            Modifier.fillMaxSize().drawBehind {
                val p = presence.value
                if (p <= 0f) return@drawBehind
                val t = time.value
                val origin = Offset(size.width / 2f, -bleed.top - size.height * 0.08f)
                val length = (size.height + bleed.top + bleed.bottom) * 0.95f
                val sway = sin(t * 0.25f) * 4f
                for (i in 0 until RAYS) {
                    val spread = (i - (RAYS - 1) / 2f) * 11f
                    val halfWidth = length * (0.035f + 0.015f * ThemeAnimMath.seeded(i, 30))
                    val a = (0.10f + 0.06f * sin(t * 0.6f + i * 1.3f)) * p
                    path.reset()
                    path.moveTo(origin.x, origin.y)
                    path.lineTo(origin.x - halfWidth, origin.y + length)
                    path.lineTo(origin.x + halfWidth, origin.y + length)
                    path.close()
                    rotate(spread + sway, pivot = origin) {
                        drawPath(
                            path,
                            Brush.verticalGradient(
                                listOf(PrayerPalette.GoldLight.copy(alpha = a), PrayerPalette.Gold.copy(alpha = a * 0.4f), Color.Transparent),
                                startY = origin.y,
                                endY = origin.y + length,
                            ),
                        )
                    }
                }
            },
        )
    }
}
