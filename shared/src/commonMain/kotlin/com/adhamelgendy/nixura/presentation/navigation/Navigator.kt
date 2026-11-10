package com.adhamelgendy.nixura.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface Route {
    /** Routes shown in the bottom navigation bar. */
    sealed interface Tab : Route

    data object Home : Tab
    data object BadHabits : Tab
    data object GoodHabits : Tab
    data object Events : Tab
    data object Coach : Tab

    data object CoachHistory : Route
    data object Profile : Route
    data object Settings : Route

    /** Settings → Integrations: Google Calendar and Notion. */
    data object Integrations : Route
    data class BadHabitDetail(val habitId: Long) : Route
    data class GoodHabitDetail(val habitId: Long) : Route

    /** `editId == null` creates a new item. */
    data class BadHabitForm(val editId: Long? = null) : Route
    data class GoodHabitForm(val editId: Long? = null) : Route
    data class EventForm(val editId: Long? = null) : Route

    /** Account screens (Agent 2). Shown in the sign-in gate or pushed from Settings > Account. */
    sealed interface Auth : Route {
        data object Welcome : Auth
        data object Login : Auth
        data object Register : Auth
        data class VerifyEmail(val email: String) : Auth
        data object ForgotPassword : Auth
    }

    data object Account : Route
    data object ChangePassword : Route
    data object DeleteAccount : Route
    data class Legal(val document: com.adhamelgendy.nixura.presentation.legal.LegalDocument) : Route
}

/** Minimal back-stack navigator (no external navigation library). Registered as a Koin singleton. */
class Navigator {
    var stack: List<Route> by mutableStateOf(listOf(Route.Home))
        private set

    val current: Route get() = stack.last()

    /** True when the system back button should be handled by the app instead of leaving it. */
    val canGoBack: Boolean get() = stack.size > 1 || current != Route.Home

    /** True while a screen opened from the account sheet is on top: going Back to the tabs opens the sheet again. */
    private var returnToAccountSheet = false

    fun selectTab(tab: Route.Tab) {
        returnToAccountSheet = false
        stack = listOf(tab)
    }

    /** Opens [route] from the account sheet (Profile, Settings) and remembers to show the sheet again on the way back. */
    fun pushFromAccountSheet(route: Route) {
        push(route)
        returnToAccountSheet = true
    }

    /** Asked by the tabs' top bar: true once, when the user has come back from a screen the account sheet opened. */
    fun takeAccountSheetReturn(): Boolean {
        if (!returnToAccountSheet || current !is Route.Tab) return false
        returnToAccountSheet = false
        return true
    }

    fun push(route: Route) {
        if (route != current) stack = stack + route
    }

    /**
     * Goes to [route] without stacking copies: if it is already open further down, everything above it is closed
     * (so "Sign in" on Create account returns to the Login screen it came from); otherwise it is pushed.
     */
    fun switchTo(route: Route) {
        val at = stack.lastIndexOf(route)
        stack = if (at >= 0) stack.take(at + 1) else stack + route
    }

    /** Returns true if the back press was consumed. */
    fun back(): Boolean = when {
        stack.size > 1 -> {
            stack = stack.dropLast(1)
            true
        }
        current != Route.Home -> {
            stack = listOf(Route.Home)
            true
        }
        else -> false
    }

    /** Pops to the last tab screen, e.g. after deleting the item whose detail screen is open. */
    fun popToTab() {
        val lastTab = stack.lastOrNull { it is Route.Tab } ?: Route.Home
        returnToAccountSheet = false
        stack = listOf(lastTab)
    }
}
