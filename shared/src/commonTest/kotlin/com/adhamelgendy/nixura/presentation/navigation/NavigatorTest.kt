package com.adhamelgendy.nixura.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {

    @Test fun switchingBetweenSignInAndCreateAccountNeverStacksCopies() {
        val n = Navigator()
        n.push(Route.Auth.Login)
        repeat(5) {
            n.switchTo(Route.Auth.Register)
            n.switchTo(Route.Auth.Login)
        }
        assertEquals(listOf(Route.Home, Route.Auth.Login), n.stack)
        n.switchTo(Route.Auth.Register)
        assertEquals(listOf(Route.Home, Route.Auth.Login, Route.Auth.Register), n.stack)
        n.back()
        assertEquals(Route.Auth.Login, n.current)
    }

    @Test fun backFromAScreenTheAccountSheetOpenedShowsTheSheetAgainOnce() {
        val n = Navigator()
        n.pushFromAccountSheet(Route.Settings)
        assertFalse(n.takeAccountSheetReturn(), "not while Settings is still open")
        n.push(Route.Account)
        n.back()
        assertFalse(n.takeAccountSheetReturn(), "not on the way back through Settings")
        n.back()
        assertTrue(n.takeAccountSheetReturn())
        assertFalse(n.takeAccountSheetReturn(), "only once")
    }

    @Test fun leavingForATabDoesNotReopenTheAccountSheet() {
        val n = Navigator()
        n.pushFromAccountSheet(Route.Settings)
        n.selectTab(Route.Home) // e.g. "Replay the starter guide"
        assertFalse(n.takeAccountSheetReturn())
        n.pushFromAccountSheet(Route.Profile)
        n.popToTab()
        assertFalse(n.takeAccountSheetReturn())
    }

    @Test fun switchToPushesARouteThatIsNotOpenYet() {
        val n = Navigator()
        n.push(Route.Auth.Register)
        n.switchTo(Route.Auth.Login)
        assertEquals(listOf(Route.Home, Route.Auth.Register, Route.Auth.Login), n.stack)
        n.switchTo(Route.Auth.Register)
        assertEquals(listOf(Route.Home, Route.Auth.Register), n.stack)
    }
}
