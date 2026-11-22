package com.adhamelgendy.nixura.notifications

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.IconCompat
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.widgets.WidgetCardPainter
import kotlin.math.roundToInt

/**
 * Turns a card glyph into a notification small icon: white on transparent (Android tints it), painted by the same
 * code as the card and the widgets, so the shapes exist once. 24 dp, with the 2-unit line of the `ic_notif_*` icons.
 */
internal object NotificationIconPainter {
    private const val SIZE_DP = 24
    private const val STROKE = 2f
    private val cache = LruCache<Pair<NixIcon, Int>, IconCompat>(16)

    fun smallIcon(context: Context, style: NixNotificationStyle): IconCompat {
        val glyph = style.glyph ?: return IconCompat.createWithResource(context, style.smallIcon)
        val density = context.resources.displayMetrics.density
        val sizePx = (SIZE_DP * density).roundToInt()
        return cache.get(glyph to sizePx)
            ?: IconCompat.createWithBitmap(WidgetCardPainter.icon(sizePx, density, glyph, Color.White, STROKE)).also { cache.put(glyph to sizePx, it) }
    }
}
