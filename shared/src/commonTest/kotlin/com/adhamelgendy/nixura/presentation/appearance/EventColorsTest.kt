package com.adhamelgendy.nixura.presentation.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.presentation.theme.DarkNixColors
import com.adhamelgendy.nixura.presentation.theme.LightNixColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventColorsTest {
    @Test
    fun everyCategoryHasItsOwnColor() {
        val colors = EventCategory.entries.map { eventCategoryColor(it) }
        assertEquals(colors.size, colors.toSet().size)
    }

    @Test
    fun aCloserEventIsWarmer() {
        val far = eventCardLook(EventCategory.TRAVEL, 30f, false, DarkNixColors.surface, true).start
        val near = eventCardLook(EventCategory.TRAVEL, 0.5f, false, DarkNixColors.surface, true).start
        val tint = eventCategoryColor(EventCategory.TRAVEL)
        assertTrue(distance(near, tint) < distance(far, tint))
    }

    @Test
    fun textStaysReadableOnEveryCardInBothThemes() {
        listOf(DarkNixColors, LightNixColors).forEach { theme ->
            EventCategory.entries.forEach { cat ->
                listOf(0f, 3f, 30f).forEach { days ->
                    val look = eventCardLook(cat, days, false, theme.surface, theme.isDark)
                    listOf(look.start, look.end).forEach { bg ->
                        val ratio = contrastRatio(theme.textPrimary.compositeOver(bg), bg)
                        assertTrue(ratio >= 4.5f, "$cat at $days days (dark=${theme.isDark}): $ratio")
                    }
                }
            }
        }
    }

    private fun distance(a: Color, b: Color) =
        (a.red - b.red) * (a.red - b.red) + (a.green - b.green) * (a.green - b.green) + (a.blue - b.blue) * (a.blue - b.blue)
}
