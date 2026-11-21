package com.adhamelgendy.nixura.domain.support

/** What kind of problem, picked on the bug screen. Optional: [OTHER] is used when the user picks nothing. */
enum class BugCategory(val wireName: String) {
    CRASH("crash"),
    WRONG_DATA("wrong_data"),
    LOOKS_WRONG("looks_wrong"),
    NOTIFICATIONS("notifications"),
    IDEA("idea"),
    OTHER("other"),
}

/** What the user typed on the bug screen. */
data class BugReportDraft(
    val message: String,
    val steps: String = "",
    val category: BugCategory = BugCategory.OTHER,
)

/** Technical facts attached to every report so the owner can reproduce it. Never a name, email or account id. */
data class SupportEnvironment(
    val appVersion: String,
    /** "android" or "ios". */
    val platform: String,
    /** "15" (Android release) or "18.1" (iOS). */
    val osVersion: String,
    /** "OPPO CPH2531", "iPhone". The hardware model, never the user-chosen device name. */
    val deviceModel: String,
    /** The app's resolved UI language: "en" or "ar". */
    val language: String,
)

/** Platform facts for [SupportEnvironment]; Android and iOS implement it. */
interface SupportEnvironmentProvider {
    fun current(language: String): SupportEnvironment
}

/** Actions only the platform can do: hand an email to the user's mail app, open the system page of this app. */
interface SupportActions {
    /** Opens the user's email app with everything filled in. False if there is no email app. */
    fun composeEmail(email: EmailDraft): Boolean

    /** Opens the system "App info" page (where Android's Uninstall button lives). False if it cannot be opened. */
    fun openAppSettings(): Boolean
}

data class EmailDraft(val to: String, val subject: String, val body: String)

/** Where a saved report went. */
sealed interface BugRemoteResult {
    data object Saved : BugRemoteResult
    data object Offline : BugRemoteResult
    data object Failed : BugRemoteResult
}

/** Stores a report for signed-in users (Firestore `bugReports`, create-only). */
interface BugReportRemote {
    suspend fun submit(draft: BugReportDraft, environment: SupportEnvironment, uid: String): BugRemoteResult
}
