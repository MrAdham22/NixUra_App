package com.adhamelgendy.nixura.presentation.navigation

import com.adhamelgendy.nixura.domain.analytics.UsageAction
import com.adhamelgendy.nixura.presentation.legal.LegalDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenNamesTest {
    private val firebaseName = Regex("^[a-z][a-z0-9_]{0,39}$") // Firebase: letters, digits, underscores, max 40

    @Test
    fun screenNamesNeverCarryIdsOrEmails() {
        val withData = listOf(
            Route.BadHabitDetail(987654), Route.GoodHabitDetail(987654), Route.BadHabitForm(987654),
            Route.GoodHabitForm(987654), Route.EventForm(987654), Route.Auth.VerifyEmail("someone@example.com"),
        )
        withData.forEach { route ->
            val name = screenNameOf(route)
            assertFalse("987654" in name || "@" in name || "someone" in name, "$route leaked into \"$name\"")
            assertTrue(firebaseName.matches(name), "\"$name\" is not a valid Firebase name")
        }
    }

    @Test
    fun formsSayNewOrEdit() {
        assertEquals("good_habit_new", screenNameOf(Route.GoodHabitForm()))
        assertEquals("good_habit_edit", screenNameOf(Route.GoodHabitForm(3)))
        assertEquals("event_new", screenNameOf(Route.EventForm()))
    }

    @Test
    fun everyNameIsValidForFirebase() {
        val routes = listOf(
            Route.Home, Route.BadHabits, Route.GoodHabits, Route.Events, Route.Coach, Route.CoachHistory, Route.Profile,
            Route.Settings, Route.Integrations, Route.Auth.Welcome, Route.Auth.Login, Route.Auth.Register,
            Route.Auth.ForgotPassword, Route.Account, Route.ChangePassword, Route.DeleteAccount, Route.KeepNixUra,
            Route.ReportBug, Route.Help,
        ) + LegalDocument.entries.map { Route.Legal(it) }
        routes.forEach { assertTrue(firebaseName.matches(screenNameOf(it)), "\"${screenNameOf(it)}\" is not valid") }
        UsageAction.entries.forEach { assertTrue(firebaseName.matches(it.eventName), "${it.eventName} is not valid") }
    }
}
