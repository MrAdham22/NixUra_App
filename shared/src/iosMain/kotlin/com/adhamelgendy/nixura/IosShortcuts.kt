package com.adhamelgendy.nixura

import com.adhamelgendy.nixura.presentation.navigation.AppShortcut
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import org.koin.mp.KoinPlatform

/**
 * Home-screen quick actions (Info.plist UIApplicationShortcutItems), called from iOSApp.swift. A cold start delivers
 * the action before Compose and Koin exist, so it waits in [pending] until [MainViewController] has started them.
 * UNVERIFIED: written on Windows, needs a Mac to build and run.
 */
object IosShortcuts {
    private const val TYPE_PREFIX = "com.adhamelgendy.nixura."
    private var pending: AppShortcut? = null

    /** [type] is the UIApplicationShortcutItem type, e.g. "com.adhamelgendy.nixura.help". */
    fun handle(type: String) {
        val shortcut = AppShortcut.fromId(type.removePrefix(TYPE_PREFIX)) ?: return
        val navigator = KoinPlatform.getKoinOrNull()?.getOrNull<Navigator>()
        if (navigator == null) pending = shortcut else shortcut.open(navigator)
    }

    internal fun applyPending(navigator: Navigator) {
        pending?.open(navigator)
        pending = null
    }
}
