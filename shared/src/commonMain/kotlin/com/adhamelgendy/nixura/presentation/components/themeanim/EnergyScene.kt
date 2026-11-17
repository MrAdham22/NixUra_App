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
import kotlin.math.sin

/** Workout: warm sparks of energy rise from a glow at the bottom of the page. Still frame: the sparks stand still. */
internal object EnergyScene : ThemeScene {

    private const val SPARKS = 24

    @Composable
    override fun Content(params: ThemeSceneParams) {
        val presence = rememberPresence(params)
        val time = rememberSceneTime(params.running)
        val warm = params.colors.accentWarm
        val accent = params.accent
        val bleed = params.bleed
        Spacer(
            Modifier.fillMaxSize().drawBehind {
                val p = presence.value
                if (p <= 0f) return@drawBehind
                val t = time.value
                val bottom = size.height + bleed.bottom
                val glowTop = bottom * 0.6f
                val pulse = 0.8f + 0.2f * sin(t * 2.4f)
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, warm.copy(alpha = 0.16f * p * pulse)), startY = glowTop, endY = bottom),
                    topLeft = Offset(-bleed.left, glowTop),
                    size = Size(size.width + bleed.left + bleed.right, bottom - glowTop),
                )
                for (i in 0 until SPARKS) {
                    val s = ThemeAnimMath.spark(i, t)
                    val c = if (i % 3 == 0) accent else warm
                    val r = (1.5f + 2.5f * s.life).dp.toPx()
                    val at = Offset(s.x * size.width, s.y * bottom)
                    val a = 0.55f * s.life * p
                    drawCircle(c.copy(alpha = a * 0.35f), r * 3f, at)
                    drawCircle(c.copy(alpha = a), r, at)
                }
            },
        )
    }
}
