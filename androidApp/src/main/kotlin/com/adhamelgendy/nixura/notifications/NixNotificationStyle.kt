package com.adhamelgendy.nixura.notifications

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.adhamelgendy.nixura.R
import com.adhamelgendy.nixura.domain.notification.NixChannel
import com.adhamelgendy.nixura.domain.notification.NixChannelGroup
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.theme.HabitTheme

/** How one notification looks: the parts that let the user tell its type apart before reading it. */
data class NixNotificationStyle(
    @DrawableRes val smallIcon: Int,
    @ColorInt val accent: Int,
    /** A `NotificationCompat.CATEGORY_*`: lets the system rank it and apply Do Not Disturb rules correctly. */
    val category: String,
)

/**
 * The Android look per kind (and per habit theme for good habits). Colours are the brand palette from DESIGN.md;
 * water and prayer reuse the card colours from `HabitThemeVisuals` (dark theme values, the app's default).
 */
object NixNotificationStyles {
    @ColorInt private const val ROSE = 0xFFAE445A.toInt() // quit cards
    @ColorInt private const val WINE = 0xFF862549.toInt() // events
    @ColorInt private const val AMBER = 0xFFF39F5A.toInt() // coach and app
    @ColorInt private const val LEAF = 0xFFE8BCB9.toInt() // other good habits: the palette's soft blush
    @ColorInt private const val WATER = 0xFF5BB6EC.toInt() // HabitThemeVisuals.WaterDark
    @ColorInt private const val GOLD = 0xFFF1B451.toInt() // HabitThemeVisuals.GoldDark

    fun of(kind: NotificationKind, theme: HabitTheme? = null): NixNotificationStyle = when (kind) {
        NotificationKind.EVENT -> NixNotificationStyle(R.drawable.ic_notif_event, WINE, NotificationCompat.CATEGORY_EVENT)
        NotificationKind.GOOD_HABIT -> when (theme) {
            HabitTheme.WATER -> NixNotificationStyle(R.drawable.ic_notif_water, WATER, NotificationCompat.CATEGORY_REMINDER)
            HabitTheme.PRAYER -> NixNotificationStyle(R.drawable.ic_notif_prayer, GOLD, NotificationCompat.CATEGORY_REMINDER)
            else -> NixNotificationStyle(R.drawable.ic_notif_good, LEAF, NotificationCompat.CATEGORY_REMINDER)
        }
        NotificationKind.QUIT_MILESTONE -> NixNotificationStyle(R.drawable.ic_notif_quit, ROSE, NotificationCompat.CATEGORY_STATUS)
        NotificationKind.COACH -> NixNotificationStyle(R.drawable.ic_notif_coach, AMBER, NotificationCompat.CATEGORY_RECOMMENDATION)
        NotificationKind.APP -> NixNotificationStyle(R.drawable.ic_notification, AMBER, NotificationCompat.CATEGORY_STATUS)
    }

    @StringRes
    fun nameOf(channel: NixChannel): Int = when (channel) {
        NixChannel.EVENTS -> R.string.channel_reminders_name
        NixChannel.GOOD_HABITS -> R.string.channel_good_habits_name
        NixChannel.QUIT_PROGRESS -> R.string.channel_quit_progress_name
        NixChannel.COACH -> R.string.channel_coach_name
        NixChannel.APP -> R.string.channel_app_name
    }

    @StringRes
    fun descriptionOf(channel: NixChannel): Int = when (channel) {
        NixChannel.EVENTS -> R.string.channel_reminders_description
        NixChannel.GOOD_HABITS -> R.string.channel_good_habits_description
        NixChannel.QUIT_PROGRESS -> R.string.channel_quit_progress_description
        NixChannel.COACH -> R.string.channel_coach_description
        NixChannel.APP -> R.string.channel_app_description
    }

    @StringRes
    fun nameOf(group: NixChannelGroup): Int = when (group) {
        NixChannelGroup.HABITS -> R.string.channel_group_habits
        NixChannelGroup.EVENTS -> R.string.channel_group_events
        NixChannelGroup.COACH_AND_APP -> R.string.channel_group_coach_app
    }
}
