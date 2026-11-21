package com.adhamelgendy.nixura.presentation.navigation

/**
 * The launcher shortcuts (Android App Shortcuts, iOS Home-screen quick actions) and the screen each one opens.
 * [id] is stable: it is the `shortcutId` in Android's `res/xml/shortcuts.xml`, the value of its `nixura.shortcut`
 * extra, and the iOS `UIApplicationShortcutItemType` suffix. Never rename an id; add a new one instead.
 */
enum class AppShortcut(val id: String) {
    KeepNixUra("keep_nixura"),
    ReportBug("report_bug"),
    Help("help"),
    AiCoach("ai_coach");

    /**
     * Opens this shortcut's screen with a sensible back stack: Home underneath, so Back from the shortcut screen lands
     * on Home and a second Back leaves the app, exactly like after opening the app normally. The Coach is a tab, so it
     * is selected directly (Back from a tab other than Home already goes to Home).
     */
    fun open(navigator: Navigator) {
        when (this) {
            AiCoach -> navigator.selectTab(Route.Coach)
            KeepNixUra -> openOverHome(navigator, Route.KeepNixUra)
            ReportBug -> openOverHome(navigator, Route.ReportBug)
            Help -> openOverHome(navigator, Route.Help)
        }
    }

    private fun openOverHome(navigator: Navigator, route: Route) {
        navigator.selectTab(Route.Home)
        navigator.push(route)
    }

    companion object {
        /** Null for an unknown or missing id (an old pinned shortcut, a hand-made intent): the app then just opens. */
        fun fromId(id: String?): AppShortcut? = entries.firstOrNull { it.id == id }
    }
}
