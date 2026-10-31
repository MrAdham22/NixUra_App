package com.example.reclaimapp.widgets

import android.content.Intent
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route

/** Turns the extras of a widget tap into navigation, using only the existing public [Navigator] API. */
object WidgetDeepLink {

    /** Returns true if [intent] came from a widget. Extras are removed so a re-delivered intent is not applied twice. */
    fun apply(intent: Intent?, navigator: Navigator): Boolean {
        val type = intent?.getStringExtra(WidgetIntents.EXTRA_TYPE) ?: return false
        val id = intent.getLongExtra(WidgetIntents.EXTRA_ID, -1L)
        when (type) {
            WidgetIntents.TYPE_QUIT -> {
                navigator.selectTab(Route.BadHabits)
                if (id > 0) navigator.push(Route.BadHabitDetail(id))
            }
            WidgetIntents.TYPE_GOOD -> {
                navigator.selectTab(Route.GoodHabits)
                if (id > 0) navigator.push(Route.GoodHabitDetail(id))
            }
            WidgetIntents.TYPE_EVENTS -> navigator.selectTab(Route.Events)
            WidgetIntents.TYPE_ADD_QUIT -> {
                navigator.selectTab(Route.BadHabits)
                navigator.push(Route.BadHabitForm())
            }
            WidgetIntents.TYPE_ADD_GOOD -> {
                navigator.selectTab(Route.GoodHabits)
                navigator.push(Route.GoodHabitForm())
            }
            WidgetIntents.TYPE_ADD_EVENT -> {
                navigator.selectTab(Route.Events)
                navigator.push(Route.EventForm())
            }
            else -> Unit // TYPE_HOME: just open the app
        }
        intent.removeExtra(WidgetIntents.EXTRA_TYPE)
        intent.removeExtra(WidgetIntents.EXTRA_ID)
        return true
    }
}
