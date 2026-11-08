package com.adhamelgendy.nixura.presentation.navigation

/**
 * Which bottom-navigation tabs are pages of the swipeable pager and which are not.
 *
 * The AI Coach is a tab in the bar but stays a normal full-screen destination (no top bar, no bottom bar),
 * so it is deliberately not a pager page.
 */
object MainTabs {
    /** Pages of the horizontal pager, in swipe order. */
    val pagerTabs: List<Route.Tab> = listOf(Route.Home, Route.BadHabits, Route.GoodHabits, Route.Events)

    /** Every item of the bottom bar, left to right: the pager pages, then the Coach. */
    val barTabs: List<Route.Tab> = pagerTabs + Route.Coach

    /** Page index of [route] in the pager, or `null` when it is not a pager page. */
    fun pageOf(route: Route): Int? = pagerTabs.indexOf(route).takeIf { it >= 0 }

    fun isPagerTab(route: Route): Boolean = pageOf(route) != null
}
