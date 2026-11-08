package com.adhamelgendy.nixura.domain.auth

enum class EmailProblem { Empty, Invalid, TooLong }

enum class PasswordProblem { TooShort, TooLong, TooCommon, SameAsEmail, LowVariety }

/**
 * Client-side checks for instant feedback. The server enforces the same policy again; this file mirrors
 * `backend/src/auth/passwordPolicy.ts` (keep both in sync: min 10, max 128, common-password denylist).
 */
object AuthValidation {
    const val PASSWORD_MIN_LENGTH = 10
    const val PASSWORD_MAX_LENGTH = 128
    const val OTP_LENGTH = 6
    const val MAX_NAME_LENGTH = 60

    private val commonPasswords = setOf(
        "password", "password1", "password12", "password123", "passw0rd", "p@ssw0rd", "qwertyuiop", "qwerty123",
        "qwerty12345", "1234567890", "12345678910", "0123456789", "1q2w3e4r5t", "1qaz2wsx3edc", "iloveyou12",
        "iloveyou123", "letmein123", "welcome123", "welcome1234", "admin12345", "administrator", "abc1234567",
        "abcdefghij", "abcd123456", "monkey1234", "dragon1234", "football123", "baseball123", "superman123",
        "trustno1234", "changeme123", "internet123", "whatever123", "sunshine123", "princess123", "nixura1234",
        "habbit1234", "nixurapassword", "passwordpassword", "1111111111", "0000000000", "aaaaaaaaaa",
    )

    /** Lower-cases and trims, the same normalisation the server applies. */
    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    /** Deliberately simple (one @, a dot in the domain, no spaces): the server and the inbox have the final say. */
    fun emailProblem(raw: String): EmailProblem? {
        val email = normalizeEmail(raw)
        if (email.isEmpty()) return EmailProblem.Empty
        if (email.length > 254) return EmailProblem.TooLong
        val at = email.indexOf('@')
        if (at <= 0 || at != email.lastIndexOf('@')) return EmailProblem.Invalid
        if (email.any { it.isWhitespace() }) return EmailProblem.Invalid
        val domain = email.substring(at + 1)
        val dot = domain.lastIndexOf('.')
        if (domain.isEmpty() || dot <= 0 || dot == domain.length - 1 || domain.startsWith('.')) return EmailProblem.Invalid
        return null
    }

    /** All policy problems for [password] (empty list = acceptable). Counts code points, like the server. */
    fun passwordProblems(password: String, email: String = ""): List<PasswordProblem> {
        val problems = mutableListOf<PasswordProblem>()
        val length = password.codePointCount()
        if (length < PASSWORD_MIN_LENGTH) problems += PasswordProblem.TooShort
        if (password.length > PASSWORD_MAX_LENGTH) problems += PasswordProblem.TooLong
        val lower = password.lowercase()
        if (lower in commonPasswords) problems += PasswordProblem.TooCommon
        val e = normalizeEmail(email)
        if (e.isNotEmpty()) {
            val local = e.substringBefore('@')
            if (lower == e || (local.length >= 4 && lower == local)) problems += PasswordProblem.SameAsEmail
        }
        if (password.toSet().size < 4) problems += PasswordProblem.LowVariety
        return problems
    }

    /** Keeps only digits and cuts to 6, so pasting "123 456" or "123-456" works. */
    fun sanitizeOtp(raw: String): String = raw.filter { it in '0'..'9' }.take(OTP_LENGTH)

    fun isCompleteOtp(code: String): Boolean = code.length == OTP_LENGTH && code.all { it in '0'..'9' }

    fun ageProblem(raw: String): Boolean {
        if (raw.isBlank()) return false
        val age = raw.trim().toIntOrNull() ?: return true
        return age !in 5..120
    }

    private fun String.codePointCount(): Int {
        var count = 0
        var i = 0
        while (i < length) {
            val c = this[i]
            i += if (c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) 2 else 1
            count++
        }
        return count
    }
}

/** 0 = unusable ... 4 = strong. Display only; the real gate is [AuthValidation.passwordProblems]. */
enum class PasswordStrength { Empty, Weak, Fair, Good, Strong }

object PasswordStrengthMeter {
    fun evaluate(password: String, email: String = ""): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.Empty
        if (AuthValidation.passwordProblems(password, email).isNotEmpty()) return PasswordStrength.Weak
        var classes = 0
        if (password.any { it.isLowerCase() }) classes++
        if (password.any { it.isUpperCase() }) classes++
        if (password.any { it.isDigit() }) classes++
        if (password.any { !it.isLetterOrDigit() }) classes++
        val length = password.length
        // Length matters most (passphrases of plain words are fine); character classes add a little.
        val score = (if (length >= 12) 1 else 0) + (if (length >= 16) 1 else 0) + (if (length >= 22) 1 else 0) + (if (classes >= 3) 1 else 0)
        return when {
            score >= 3 -> PasswordStrength.Strong
            score == 2 -> PasswordStrength.Good
            else -> PasswordStrength.Fair
        }
    }
}
