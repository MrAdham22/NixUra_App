package com.adhamelgendy.nixura.presentation.appearance

import androidx.compose.ui.graphics.compositeOver
import com.adhamelgendy.nixura.presentation.theme.DarkNixColors
import com.adhamelgendy.nixura.presentation.theme.LightNixColors
import kotlin.test.Test
import kotlin.test.assertTrue

class GoodFillContrastTest {
    @Test
    fun aPlainGoodHabitCardStaysReadableAtEveryFillInBothThemes() {
        listOf(DarkNixColors, LightNixColors).forEach { theme ->
            (0..10).map { it / 10f }.forEach { fill ->
                val c = goodFillColors(fill, water = false, theme)
                listOf(c.gradientStart, c.background, c.gradientEnd).forEach { bg ->
                    val name = contrastRatio(c.content.compositeOver(bg), bg)
                    val label = contrastRatio(c.contentMuted.compositeOver(bg), bg)
                    // 3.9 is about the best two inks can do on a mid-tone (see CardInkContrastTest)
                    assertTrue(name >= 3.9f, "name at fill $fill (dark=${theme.isDark}): $name")
                    assertTrue(label >= 3f, "label at fill $fill (dark=${theme.isDark}): $label")
                }
            }
        }
    }

    @Test
    fun aFinishedDayIsADeepEmeraldWithLightText() {
        val done = goodFillColors(1f, water = false, DarkNixColors)
        assertTrue(done.background.relativeLuminance() < 0.2f)
        assertTrue(done.content.relativeLuminance() > 0.8f)
    }
}
