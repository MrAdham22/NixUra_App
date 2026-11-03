package com.example.reclaimapp.presentation.navigation

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
        data class VerifyOtp(val email: String) : Auth
        data object ForgotPassword : Auth
        data class ResetPassword(val email: String) : Auth
    }

    data object Account : Route
    data object ChangePassword : Route
    data object DeleteAccount : Route
    data object Devices : Route
    data class Legal(val document: com.example.reclaimapp.presentation.legal.LegalDocument) : Route
}

/** Minimal back-stack navigator (no external navigation library). Registered as a Koin singleton. */
class Navigator {
    var stack: List<Route> by mutableStateOf(listOf(Route.Home))
        private set

    val current: Route get() = stack.last()

    /** True when the system back button should be handled by the app instead of leaving it. */
    val canGoBack: Boolean get() = stack.size > 1 || current != Route.Home

    fun selectTab(tab: Route.Tab) {
        stack = listOf(tab)
    }

    fun push(route: Route) {
        if (route != current) stack = stack + route
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
        stack = listOf(lastTab)
    }
}
