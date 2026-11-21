package com.adhamelgendy.nixura.domain.support

/** Why a report cannot be sent yet. Each one has its own message on the bug screen. */
enum class BugReportProblem { EMPTY, TOO_SHORT, TOO_LONG }

/**
 * The input rules for a bug report, shared by the screen (Send button, inline hint) and [SendBugReportUseCase].
 * The same limits are enforced again by `firestore.rules` (bugReports), so keep the numbers in sync.
 */
object BugReportRules {
    /** Longest description / steps text the screen accepts and the Firestore rules allow. */
    const val MAX_MESSAGE = 2000
    const val MAX_STEPS = 2000

    /** Fewer meaningful characters than this cannot describe a problem ("bug", "help"). */
    const val MIN_MESSAGE = 10

    /** Null when [draft] can be sent. */
    fun problem(draft: BugReportDraft): BugReportProblem? {
        // Measured after trimming: that is what is sent, and what the Firestore rules check (never empty, <= 2000).
        val message = draft.message.trim()
        return when {
            message.isEmpty() -> BugReportProblem.EMPTY
            message.length > MAX_MESSAGE || draft.steps.trim().length > MAX_STEPS -> BugReportProblem.TOO_LONG
            message.length < MIN_MESSAGE -> BugReportProblem.TOO_SHORT
            else -> null
        }
    }
}
