package com.adhamelgendy.nixura.domain.support

import com.adhamelgendy.nixura.data.support.FirestoreBugReportRemote
import com.adhamelgendy.nixura.domain.auth.AuthUser
import com.adhamelgendy.nixura.domain.auth.GuestDataHandoff
import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.Habit
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.model.TrackingSnapshot
import com.adhamelgendy.nixura.presentation.navigation.AppShortcut
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppShortcutTest {
    @Test fun everyScreenShortcutLeavesHomeUnderneath() {
        for ((shortcut, route) in listOf(
            AppShortcut.KeepNixUra to Route.KeepNixUra,
            AppShortcut.ReportBug to Route.ReportBug,
            AppShortcut.Help to Route.Help,
        )) {
            val nav = Navigator()
            nav.push(Route.Settings) // wherever the user was before (warm start)
            shortcut.open(nav)
            assertEquals(listOf(Route.Home, route), nav.stack)
            assertTrue(nav.back())
            assertEquals(listOf<Route>(Route.Home), nav.stack)
            assertFalse(nav.back(), "a second Back leaves the app")
        }
    }

    @Test fun coachShortcutSelectsTheTabAndBackGoesHome() {
        val nav = Navigator()
        AppShortcut.AiCoach.open(nav)
        assertEquals(listOf<Route>(Route.Coach), nav.stack)
        assertTrue(nav.back())
        assertEquals(Route.Home, nav.current)
    }

    @Test fun idsAreStableAndUnknownIdsAreIgnored() {
        // These ids are in res/xml/shortcuts.xml and iOS Info.plist: renaming one breaks pinned shortcuts.
        assertEquals(listOf("keep_nixura", "report_bug", "help", "ai_coach"), AppShortcut.entries.map { it.id })
        assertEquals(AppShortcut.Help, AppShortcut.fromId("help"))
        assertNull(AppShortcut.fromId("nope"))
        assertNull(AppShortcut.fromId(null))
    }
}

class BugReportEmailTest {
    private val env = SupportEnvironment("1.0 (1)", "android", "15 (API 35)", "OPPO CPH2531", "ar")

    @Test fun addressSubjectAndDeviceFacts() {
        val mail = BugReportEmail.draft(BugReportDraft("The water card\ndid not fill", "1. tap +1", BugCategory.WRONG_DATA), env)
        assertEquals("adhamelgendydev@gmail.com", mail.to)
        assertEquals("NixUra 1.0 (1) bug [wrong_data]: The water card", mail.subject)
        assertTrue("1. tap +1" in mail.body)
        assertTrue("App version: 1.0 (1)" in mail.body)
        assertTrue("Device: OPPO CPH2531" in mail.body)
        assertTrue("Android version: 15 (API 35)" in mail.body)
        assertTrue("App language: ar" in mail.body)
    }

    @Test fun noStepsSectionWhenEmpty() {
        val mail = BugReportEmail.draft(BugReportDraft("Something broke here"), env)
        assertFalse("Steps to reproduce" in mail.body)
    }
}

class KeepNixUraSummaryTest {
    private val day = 86_400_000L

    private fun quit(name: String, elapsed: Long) = HabitTracking(
        Habit(id = name.length.toLong(), name = name, createdAtEpochMillis = 0),
        emptyList(),
        TrackingSnapshot(activePeriodStartEpochMillis = 0, activeElapsedMillis = elapsed, longestStreakMillis = elapsed),
    )

    @Test fun picksTheLongestRunningStreakInWholeDays() {
        val s = KeepNixUraSummary.from(listOf(quit("Smoking", 12 * day + 5), quit("Sugar", 3 * day)), emptyList(), isGuest = true)
        assertEquals(12, s.bestStreakDays)
        assertEquals("Smoking", s.bestStreakHabit)
        assertEquals(2, s.quitHabits)
        assertTrue(s.hasProgress)
    }

    @Test fun nothingTrackedMeansNoProgress() {
        val s = KeepNixUraSummary.from(emptyList(), emptyList(), isGuest = false)
        assertFalse(s.hasProgress)
        assertNull(s.bestStreakHabit)
    }

    @Test fun goodHabitsCountAsProgress() {
        val good = GoodHabitWithLogs(GoodHabit(id = 1, name = "Water", dailyTarget = 8, createdAtEpochMillis = 0), emptyList())
        val s = KeepNixUraSummary.from(emptyList(), listOf(good), isGuest = true)
        assertTrue(s.hasProgress)
        assertEquals(1, s.goodHabits)
        assertNull(s.waterGlasses, "no water habit, no glasses of tears")
    }

    @Test fun waterGoalBecomesGlassesOfTears() {
        val water = GoodHabitWithLogs(
            GoodHabit(id = 2, name = "Water", dailyTarget = 8, createdAtEpochMillis = 0, water = com.adhamelgendy.nixura.domain.theme.WaterGoal(goalMl = 2000, servingMl = 250)),
            emptyList(),
        )
        assertEquals(8, KeepNixUraSummary.from(emptyList(), listOf(water), isGuest = false).waterGlasses)
    }
}

class FirestoreBugReportBodyTest {
    @Test fun isACreateWithServerTimeAndOnlyAllowedFields() {
        val env = SupportEnvironment("1.0 (1)", "android", "15", "OPPO CPH2531", "en")
        val body = FirestoreBugReportRemote.commitBody("projects/p/databases/(default)/documents/bugReports/abc", BugReportDraft("msg here!!", "", BugCategory.IDEA), env, "uid1")
        val write = (body["writes"] as JsonArray).single().jsonObject
        assertEquals("false", write["currentDocument"]!!.jsonObject["exists"]!!.jsonPrimitive.content)
        val fields = write["update"]!!.jsonObject["fields"] as JsonObject
        assertEquals(setOf("uid", "category", "message", "steps", "appVersion", "platform", "osVersion", "deviceModel", "language"), fields.keys)
        assertEquals("uid1", fields["uid"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
        assertEquals("idea", fields["category"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
        val transform = (write["updateTransforms"] as JsonArray).single().jsonObject
        assertEquals("createdAt", transform["fieldPath"]!!.jsonPrimitive.content)
    }
}

class SendBugReportUseCaseTest {
    private class Sessions(signedIn: Boolean) : SessionManager {
        override val state: StateFlow<SessionState> = MutableStateFlow(
            if (signedIn) SessionState.SignedIn(AuthUser("u1", "a@b.c", "", null, true)) else SessionState.SignedOut,
        )
        override val currentUserId: Flow<String?> = flowOf(null)
        override suspend fun pendingGuestHandoff(): GuestDataHandoff? = null
        override suspend fun markGuestHandoffHandled() = Unit
        override suspend fun forgetLastUser() = Unit
        override suspend fun hasLeftoverAccountData() = false
        override suspend fun signOutLocally() = Unit
    }

    private class Remote(val answer: BugRemoteResult) : BugReportRemote {
        var calls = 0
        var lastUid: String? = null
        override suspend fun submit(draft: BugReportDraft, environment: SupportEnvironment, uid: String): BugRemoteResult {
            calls++; lastUid = uid; return answer
        }
    }

    private class Actions(val hasMailApp: Boolean = true) : SupportActions {
        var mails = mutableListOf<EmailDraft>()
        override fun composeEmail(email: EmailDraft): Boolean { mails += email; return hasMailApp }
        override fun openAppSettings() = true
    }

    private val env = object : SupportEnvironmentProvider {
        override fun current(language: String) = SupportEnvironment("1.0 (1)", "android", "15", "OPPO", language)
    }

    private val valid = BugReportDraft("  The widget shows yesterday  ", "", BugCategory.WRONG_DATA)

    @Test fun guestSendsEmailOnlyAndNoNetwork() = runTest {
        val remote = Remote(BugRemoteResult.Saved)
        val actions = Actions()
        val result = SendBugReportUseCase(remote, Sessions(signedIn = false), env, actions, ContentLanguage.English)(valid)
        assertEquals(SendBugReportResult.Sent(CloudOutcome.GUEST, emailOpened = true), result)
        assertEquals(0, remote.calls)
        assertEquals(1, actions.mails.size)
        assertTrue(actions.mails.single().body.startsWith("What happened:\nThe widget shows yesterday\n"), "trimmed")
    }

    @Test fun signedInSavesToTheCloudAndEmails() = runTest {
        val remote = Remote(BugRemoteResult.Saved)
        val actions = Actions()
        val result = SendBugReportUseCase(remote, Sessions(signedIn = true), env, actions, ContentLanguage.English)(valid)
        assertEquals(SendBugReportResult.Sent(CloudOutcome.SAVED, emailOpened = true), result)
        assertEquals("u1", remote.lastUid)
        assertEquals(1, actions.mails.size)
    }

    @Test fun offlineStillOpensTheEmail() = runTest {
        val actions = Actions()
        val result = SendBugReportUseCase(Remote(BugRemoteResult.Offline), Sessions(true), env, actions, ContentLanguage.English)(valid)
        assertEquals(SendBugReportResult.Sent(CloudOutcome.OFFLINE, emailOpened = true), result)
    }

    @Test fun noMailAppIsReported() = runTest {
        val result = SendBugReportUseCase(Remote(BugRemoteResult.Saved), Sessions(false), env, Actions(hasMailApp = false), ContentLanguage.English)(valid)
        assertEquals(SendBugReportResult.Sent(CloudOutcome.GUEST, emailOpened = false), result)
    }

    @Test fun invalidInputSendsNothing() = runTest {
        val remote = Remote(BugRemoteResult.Saved)
        val actions = Actions()
        val result = SendBugReportUseCase(remote, Sessions(true), env, actions, ContentLanguage.English)(BugReportDraft("   "))
        assertEquals(SendBugReportResult.Invalid(BugReportProblem.EMPTY), result)
        assertEquals(0, remote.calls)
        assertTrue(actions.mails.isEmpty())
    }
}

/** The rules the bug screen and the Firestore rules agree on (BugReportRules). */
class BugReportRulesTest {
    private fun problem(message: String, steps: String = "") = BugReportRules.problem(BugReportDraft(message, steps))

    @Test fun emptyOrBlankIsEmpty() {
        assertEquals(BugReportProblem.EMPTY, problem(""))
        assertEquals(BugReportProblem.EMPTY, problem("   \n\t "))
    }

    @Test fun aFewLettersAreTooShort() {
        assertEquals(BugReportProblem.TOO_SHORT, problem("bug"))
        assertEquals(BugReportProblem.TOO_SHORT, problem("   help    "), "spaces around do not count")
    }

    @Test fun overTheLimitIsTooLong() {
        assertEquals(BugReportProblem.TOO_LONG, problem("a".repeat(BugReportRules.MAX_MESSAGE + 1)))
        assertEquals(BugReportProblem.TOO_LONG, problem("The app crashed", steps = "s".repeat(BugReportRules.MAX_STEPS + 1)))
    }

    @Test fun aRealSentenceInEitherLanguageIsFine() {
        assertNull(problem("The widget shows yesterday"))
        assertNull(problem("الأداة تعرض يوم أمس"))
        assertNull(problem("a".repeat(BugReportRules.MAX_MESSAGE)))
    }
}
