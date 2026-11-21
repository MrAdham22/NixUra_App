package com.adhamelgendy.nixura.domain.support

/**
 * Builds the email a bug report is sent as. The headings stay in English whatever the app language (the owner reads
 * them); the user's own words are kept exactly as typed.
 */
object BugReportEmail {
    const val SUPPORT_ADDRESS = "adhamelgendydev@gmail.com"

    fun draft(report: BugReportDraft, env: SupportEnvironment): EmailDraft {
        val firstLine = report.message.trim().lineSequence().first().take(SUBJECT_SNIPPET)
        val subject = "NixUra ${env.appVersion} bug [${report.category.wireName}]: $firstLine"
        val body = buildString {
            appendLine("What happened:")
            appendLine(report.message.trim())
            if (report.steps.isNotBlank()) {
                appendLine()
                appendLine("Steps to reproduce:")
                appendLine(report.steps.trim())
            }
            appendLine()
            appendLine("----")
            appendLine("Category: ${report.category.wireName}")
            appendLine("App version: ${env.appVersion}")
            appendLine("Device: ${env.deviceModel}")
            appendLine("${if (env.platform == "ios") "iOS" else "Android"} version: ${env.osVersion}")
            append("App language: ${env.language}")
        }
        return EmailDraft(SUPPORT_ADDRESS, subject, body)
    }

    private const val SUBJECT_SNIPPET = 60
}
