package com.adhamelgendy.nixura.domain.notification

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationAccessPolicyTest {

    private fun resolve(
        runtime: Boolean = true,
        granted: Boolean = false,
        appEnabled: Boolean = true,
        asked: Boolean = false,
        rationale: Boolean = false,
    ) = NotificationAccessPolicy.resolve(runtime, granted, appEnabled, asked, rationale)

    @Test
    fun grantedAndEnabledIsAllowed() = assertEquals(NotificationAccess.ALLOWED, resolve(granted = true))

    @Test
    fun neverAskedOnAndroid13AsksWithAReason() = assertEquals(NotificationAccess.ASK, resolve())

    @Test
    fun deniedOnceCanStillAsk() = assertEquals(NotificationAccess.ASK, resolve(asked = true, rationale = true))

    @Test
    fun deniedTwiceSendsToSettings() = assertEquals(NotificationAccess.OPEN_SETTINGS, resolve(asked = true, rationale = false))

    @Test
    fun grantedButSwitchedOffInSystemSendsToSettings() =
        assertEquals(NotificationAccess.OPEN_SETTINGS, resolve(granted = true, appEnabled = false))

    @Test
    fun olderAndroidNeedsNoRuntimePermission() =
        assertEquals(NotificationAccess.ALLOWED, resolve(runtime = false, granted = false))

    @Test
    fun olderAndroidSwitchedOffSendsToSettings() =
        assertEquals(NotificationAccess.OPEN_SETTINGS, resolve(runtime = false, appEnabled = false))
}
