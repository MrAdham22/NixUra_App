package com.adhamelgendy.nixura.presentation.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.presentation.theme.NixUraPalette

/**
 * Each event category has its own color, so the Events list reads at a glance (DESIGN.md, Colors: meaning colors).
 * Prayer gold is never used here (it belongs to the prayer habit only), and none of these is the action rose's job:
 * they tint a card, they never mark a button.
 */
fun eventCategoryColor(category: EventCategory): Color = when (category) {
    EventCategory.PERSONAL -> NixUraPalette.Rose
    EventCategory.TRAVEL -> Color(0xFF5BB6EC)
    EventCategory.WORK -> Color(0xFF7C74E8)
    EventCategory.HEALTH -> Color(0xFF4CC38A)
    EventCategory.CELEBRATION -> NixUraPalette.Orange
    EventCategory.BIRTHDAY -> Color(0xFFE77FA8)
    EventCategory.HOLIDAY -> Color(0xFF2AA198)
    EventCategory.TASK -> Color(0xFF8B93B8)
    EventCategory.EVENT -> Color(0xFFC25B8C)
    EventCategory.OTHER -> Color(0xFF9B94B0)
}

/** How an event card is painted: a diagonal tint of its category color, a glow, an edge and an accent for labels. */
data class EventCardLook(val start: Color, val end: Color, val glow: Color, val edge: Color, val accent: Color)

/** Events closer than this get their full color; further ones start calmer. */
const val EVENT_WARM_UP_DAYS = 14f

/**
 * The card for [category] on a page whose card color is [surface]. [daysLeft] makes it warmer as the event comes
 * closer (night into dawn): calm two weeks out, full color on the day. A past event stays plain.
 */
fun eventCardLook(category: EventCategory, daysLeft: Float, passed: Boolean, surface: Color, isDark: Boolean): EventCardLook {
    val tint = eventCategoryColor(category)
    if (passed) {
        return EventCardLook(surface, surface, Color.Transparent, tint.copy(alpha = 0.18f), tint.copy(alpha = 0.7f))
    }
    val near = 1f - (daysLeft / EVENT_WARM_UP_DAYS).coerceIn(0f, 1f)
    // Light theme tints less, so navy text keeps its contrast on the pale card.
    val strength = if (isDark) 0.22f + 0.30f * near else 0.10f + 0.16f * near
    return EventCardLook(
        start = lerp(surface, tint, strength),
        end = lerp(surface, tint, strength * 0.35f),
        glow = tint.copy(alpha = if (isDark) 0.30f else 0.22f),
        edge = tint.copy(alpha = 0.40f),
        accent = if (isDark) lerp(tint, Color.White, 0.35f) else lerp(tint, Color.Black, 0.25f),
    )
}
