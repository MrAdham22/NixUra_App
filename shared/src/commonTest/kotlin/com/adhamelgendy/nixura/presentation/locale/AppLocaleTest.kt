package com.adhamelgendy.nixura.presentation.locale

import com.adhamelgendy.nixura.domain.locale.AppLocales
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.adhamelgendy.nixura.domain.model.DigitStyle
import com.adhamelgendy.nixura.presentation.strings.ArabicStrings
import com.adhamelgendy.nixura.presentation.strings.EnglishStrings
import com.adhamelgendy.nixura.presentation.strings.availableLanguages
import com.adhamelgendy.nixura.presentation.strings.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppLocaleTest {

    @Test
    fun explicitChoiceWinsOverThePhone() {
        assertEquals("ar", AppLocales.resolve("ar", "en-US"))
        assertEquals("en", AppLocales.resolve("en", "ar-EG"))
    }

    @Test
    fun systemFollowsThePhoneAndFallsBackToEnglish() {
        assertEquals("ar", AppLocales.resolve("system", "ar-EG"))
        assertEquals("ar", AppLocales.resolve("system", "ar_SA"))
        assertEquals("ar", AppLocales.resolve("system", "ar-XB")) // Android's RTL pseudo-locale
        assertEquals("en", AppLocales.resolve("system", "en-XA"))
        assertEquals("en", AppLocales.resolve("system", "fr-FR"))
        assertEquals("en", AppLocales.resolve("system", null))
        assertEquals("en", AppLocales.resolve(null, null))
    }

    @Test
    fun osTagsRoundTrip() {
        assertNull(AppLocales.osTag("system"))
        assertEquals("ar", AppLocales.osTag("ar"))
        assertEquals("system", AppLocales.choiceFromOs(""))
        assertEquals("system", AppLocales.choiceFromOs("de"))
        assertEquals("ar", AppLocales.choiceFromOs("ar-EG"))
        assertEquals("en", AppLocales.choiceFromOs("en"))
    }

    @Test
    fun onlyArabicIsRightToLeft() {
        assertTrue(AppLocales.isRtl("ar"))
        assertTrue(AppLocale(language = "ar").isRtl)
        assertFalse(AppLocales.isRtl("en"))
    }

    @Test
    fun stringsAreRegisteredForEveryChoice() {
        assertSame(ArabicStrings, stringsFor("ar"))
        assertSame(ArabicStrings, stringsFor("ar-EG"))
        assertSame(EnglishStrings, stringsFor("en"))
        assertSame(EnglishStrings, stringsFor("system"))
        assertEquals(listOf("system", "en", "ar"), availableLanguages.map { it.code })
        assertEquals("العربية", availableLanguages.last().nativeName)
    }

    @Test
    fun westernDigitsLeaveTextAlone() {
        assertEquals("3 / 8 cups", Digits.localize("3 / 8 cups", DigitStyle.WESTERN))
    }

    @Test
    fun arabicIndicDigitsAndDecimalMark() {
        assertEquals("١٢٣", Digits.localize("123", DigitStyle.ARABIC_INDIC))
        assertEquals("١٫٢٥ / ٢ لتر", Digits.localize("1.25 / 2 لتر", DigitStyle.ARABIC_INDIC))
        assertEquals("٨:٣٠ م", Digits.localize("8:30 م", DigitStyle.ARABIC_INDIC))
        // A full stop that is not between two digits stays a full stop.
        assertEquals("تم ٣.", Digits.localize("تم 3.", DigitStyle.ARABIC_INDIC))
        assertEquals("", Digits.localize("", DigitStyle.ARABIC_INDIC))
    }

    @Test
    fun emailsAndLinksKeepTheirDigits() {
        assertEquals("sam42@mail.com", Digits.localize("sam42@mail.com", DigitStyle.ARABIC_INDIC))
        assertEquals("https://x.dev/1", Digits.localize("https://x.dev/1", DigitStyle.ARABIC_INDIC))
    }

    @Test
    fun arabicLettersAreDetectedButNotArabicDigits() {
        assertTrue(hasArabicLetters("اشرب مياه"))
        assertFalse(hasArabicLetters("١٢٣"))
        assertFalse(hasArabicLetters("Drink water 2L"))
    }

    @Test
    fun arabicTextGetsATallerLineAndNoTracking() {
        val base = TextStyle(fontSize = 20.sp, lineHeight = 22.sp, letterSpacing = (-0.5).sp)
        val ar = AppLocale(language = "ar").style(base, "مرحبًا")
        assertEquals(30.sp, ar.lineHeight)
        assertEquals(0.sp, ar.letterSpacing)
        // English text in the English UI is untouched.
        assertEquals(base, AppLocale(language = "en").style(base, "Hello"))
        // Digits only (a counter) in Arabic: no taller line, so cards keep their size.
        assertEquals(22.sp, AppLocale(language = "ar").style(base, "12:30").lineHeight)
        // An Arabic habit name inside the English UI still gets room for its marks.
        assertEquals(30.sp, AppLocale(language = "en").style(base, "قهوة").lineHeight)
    }
}
