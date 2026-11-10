package com.adhamelgendy.nixura.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test fun switchToPushesARouteThatIsNotOpenYet() {
        val n = Navigator()
        n.push(Route.Auth.Register)
        n.switchTo(Route.Auth.Login)
        assertEquals(listOf(Route.Home, Route.Auth.Register, Route.Auth.Login), n.stack)
        n.switchTo(Route.Auth.Register)
        assertEquals(listOf(Route.Home, Route.Auth.Register), n.stack)
    }
}
