package com.adhamelgendy.nixura.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutManagerCompat
import com.adhamelgendy.nixura.presentation.navigation.AppShortcut
import com.adhamelgendy.nixura.presentation.navigation.Navigator

/**
 * Turns a launcher App Shortcut (res/xml/shortcuts.xml) into navigation, like [com.adhamelgendy.nixura.widgets.WidgetDeepLink]
 * does for widget taps. Which screen each id opens lives in the shared [AppShortcut], so iOS quick actions match.
 */
object ShortcutDeepLink {

    /** The intent extra set by every static shortcut in res/xml/shortcuts.xml. */
    const val EXTRA_SHORTCUT = "nixura.shortcut"

    /**
     * Returns true if [intent] came from a shortcut. The extra is removed afterwards, so the same intent re-delivered
     * (activity recreated, opened again from Recents) does not reset the user's place in the app a second time.
     */
    fun apply(context: Context, intent: Intent?, navigator: Navigator): Boolean {
        val id = intent?.getStringExtra(EXTRA_SHORTCUT) ?: return false
        intent.removeExtra(EXTRA_SHORTCUT)
        // Opened again from Recents: Android hands back the original launch intent, extras and all. Not a new tap.
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return false
        val shortcut = AppShortcut.fromId(id) ?: return false
        shortcut.open(navigator)
        // Helps the launcher rank the shortcuts the user really uses (no effect on launchers that ignore it).
        runCatching { ShortcutManagerCompat.reportShortcutUsed(context, shortcut.id) }
        return true
    }
}
