package com.adhamelgendy.nixura.domain.support

import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import kotlinx.coroutines.withTimeoutOrNull

/** What happened to a report in the cloud. Guests are never sent there (they make no network calls). */
enum class CloudOutcome { SAVED, GUEST, OFFLINE, FAILED }

sealed interface SendBugReportResult {
    data class Invalid(val problem: BugReportProblem) : SendBugReportResult

    /** [emailOpened] is false when the phone has no email app: the report then only reached the cloud (or nowhere). */
    data class Sent(val cloud: CloudOutcome, val emailOpened: Boolean) : SendBugReportResult
}

/**
 * Sends a bug report (owner decision 4, docs/ux-upgrade/PLAN.md): signed-in users to Firestore AND by email, guests by
 * email only. The cloud copy goes first, so the user is still in the app to see whether it arrived; the email app
 * opens after, prefilled, and the user presses Send there (it queues the mail when offline).
 */
class SendBugReportUseCase(
    private val remote: BugReportRemote,
    private val sessions: SessionManager,
    private val environment: SupportEnvironmentProvider,
    private val actions: SupportActions,
    private val language: ContentLanguage,
) {
    suspend operator fun invoke(draft: BugReportDraft): SendBugReportResult {
        BugReportRules.problem(draft)?.let { return SendBugReportResult.Invalid(it) }
        val clean = draft.copy(message = draft.message.trim(), steps = draft.steps.trim())
        val env = environment.current(language.current.value)

        val uid = (sessions.state.value as? SessionState.SignedIn)?.user?.id
        val cloud = if (uid == null) {
            CloudOutcome.GUEST
        } else {
            when (withTimeoutOrNull(CLOUD_TIMEOUT_MILLIS) { remote.submit(clean, env, uid) }) {
                BugRemoteResult.Saved -> CloudOutcome.SAVED
                BugRemoteResult.Failed -> CloudOutcome.FAILED
                BugRemoteResult.Offline, null -> CloudOutcome.OFFLINE
            }
        }
        val emailOpened = actions.composeEmail(BugReportEmail.draft(clean, env))
        return SendBugReportResult.Sent(cloud, emailOpened)
    }

    private companion object {
        const val CLOUD_TIMEOUT_MILLIS = 10_000L
    }
}
