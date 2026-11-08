package com.adhamelgendy.nixura.domain.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthValidationTest {

    @Test fun emailAcceptsNormalAddressesAndTrims() {
        assertNull(AuthValidation.emailProblem("  Ann@Example.com "))
        assertNull(AuthValidation.emailProblem("a.b+tag@sub.example.co"))
        assertEquals("ann@example.com", AuthValidation.normalizeEmail("  ANN@Example.com "))
    }

    @Test fun emailRejectsBadShapes() {
        assertEquals(EmailProblem.Empty, AuthValidation.emailProblem("   "))
        for (bad in listOf("ann", "ann@", "@x.com", "a@b", "a@@b.com", "a b@x.com", "a@.com", "a@x.", "a@x@y.com")) {
            assertEquals(EmailProblem.Invalid, AuthValidation.emailProblem(bad), bad)
        }
        assertEquals(EmailProblem.TooLong, AuthValidation.emailProblem("a".repeat(250) + "@x.com"))
    }

    @Test fun passwordPolicyMatchesServerRules() {
        assertTrue(AuthValidation.passwordProblems("correct horse battery staple").isEmpty())
        assertTrue(PasswordProblem.TooShort in AuthValidation.passwordProblems("short1"))
        assertTrue(PasswordProblem.TooCommon in AuthValidation.passwordProblems("Password123"))
        assertTrue(PasswordProblem.TooLong in AuthValidation.passwordProblems("a".repeat(129)))
        assertTrue(PasswordProblem.LowVariety in AuthValidation.passwordProblems("aaaaaaaaaaaa"))
        assertTrue(PasswordProblem.LowVariety in AuthValidation.passwordProblems("ababababab"))
        assertTrue(PasswordProblem.SameAsEmail in AuthValidation.passwordProblems("ann@example.com", "Ann@Example.com"))
        assertTrue(PasswordProblem.SameAsEmail in AuthValidation.passwordProblems("annsmith12", "annsmith12@example.com"))
    }

    @Test fun passwordLengthCountsEmojiAsOneCharacter() {
        // 10 emoji = 20 UTF-16 units but 10 code points: long enough, like on the server.
        val tenEmoji = "\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02\uD83E\uDD23\uD83D\uDE03\uD83D\uDE04\uD83D\uDE05\uD83D\uDE06\uD83D\uDE09\uD83D\uDE0A"
        assertFalse(PasswordProblem.TooShort in AuthValidation.passwordProblems(tenEmoji))
        assertTrue(PasswordProblem.TooShort in AuthValidation.passwordProblems(tenEmoji.take(18)))
    }

    @Test fun otpSanitizingHandlesPasteFormats() {
        assertEquals("123456", AuthValidation.sanitizeOtp("123 456"))
        assertEquals("123456", AuthValidation.sanitizeOtp("123-456"))
        assertEquals("123456", AuthValidation.sanitizeOtp("1234567890"))
        assertEquals("12", AuthValidation.sanitizeOtp("a1b2c"))
        assertEquals("", AuthValidation.sanitizeOtp("abc"))
        assertTrue(AuthValidation.isCompleteOtp("012345"))
        assertFalse(AuthValidation.isCompleteOtp("01234"))
        assertFalse(AuthValidation.isCompleteOtp("01234a"))
    }

    @Test fun ageValidation() {
        assertFalse(AuthValidation.ageProblem(""))
        assertFalse(AuthValidation.ageProblem("29"))
        assertTrue(AuthValidation.ageProblem("4"))
        assertTrue(AuthValidation.ageProblem("121"))
        assertTrue(AuthValidation.ageProblem("abc"))
    }

    @Test fun strengthMeterOrdersPasswords() {
        assertEquals(PasswordStrength.Empty, PasswordStrengthMeter.evaluate(""))
        assertEquals(PasswordStrength.Weak, PasswordStrengthMeter.evaluate("password123"))
        assertEquals(PasswordStrength.Weak, PasswordStrengthMeter.evaluate("short"))
        val fair = PasswordStrengthMeter.evaluate("abcdefghij1")
        val good = PasswordStrengthMeter.evaluate("correct horse battery")
        val strong = PasswordStrengthMeter.evaluate("correct horse battery staple 42!")
        assertTrue(fair.ordinal < good.ordinal || fair == PasswordStrength.Fair)
        assertTrue(good.ordinal <= strong.ordinal)
        assertEquals(PasswordStrength.Strong, strong)
    }
}
