package com.adhamelgendy.nixura.presentation.locale

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import com.adhamelgendy.nixura.domain.model.DigitStyle
import com.adhamelgendy.nixura.domain.locale.AppLocales

/** Western ↔ Arabic-Indic digits for text on screen. Never used on stored data. */
object Digits {
    private const val ARABIC_ZERO = '\u0660'
    private const val ARABIC_DECIMAL = '\u066B'
    private const val ARABIC_PERCENT = '\u066A'

    /**
     * [text] with its digits drawn in [style]. A "." between two digits becomes the Arabic decimal mark and a "%"
     * after a digit the Arabic percent sign. Emails and
     * links are left alone, since they have to stay exactly as typed.
     */
    fun localize(text: String, style: DigitStyle): String {
        if (style == DigitStyle.WESTERN || text.none { it in '0'..'9' }) return text
        if ('@' in text || "://" in text) return text
        val out = CharArray(text.length)
        for (i in text.indices) {
            val ch = text[i]
            out[i] = when {
                ch in '0'..'9' -> ARABIC_ZERO + (ch - '0')
                ch == '.' && i > 0 && i < text.lastIndex && text[i - 1] in '0'..'9' && text[i + 1] in '0'..'9' -> ARABIC_DECIMAL
                ch == '%' && i > 0 && text[i - 1] in '0'..'9' -> ARABIC_PERCENT
                else -> ch
            }
        }
        return out.concatToString()
    }
}

/** The language and digits the UI is drawn with right now. Provided once at the app root. */
@Immutable
data class AppLocale(
    /** The resolved language: "en" or "ar". */
    val language: String = AppLocales.ENGLISH,
    val digitStyle: DigitStyle = DigitStyle.WESTERN,
    /** The Arabic font (IBM Plex Sans Arabic), loaded from Compose resources at the app root; null in tests. */
    val arabicFont: FontFamily? = null,
) {
    val isRtl: Boolean get() = AppLocales.isRtl(language)
    val isArabic: Boolean get() = language == AppLocales.ARABIC

    fun digits(text: String): String = Digits.localize(text, digitStyle)
}

val LocalAppLocale = staticCompositionLocalOf { AppLocale() }

/**
 * Tells the operating system which language the app shows, so it appears in the phone's own per-app language
 * settings (Android 13+), and reports a choice made there. iOS follows the phone (or the per-app language in iOS
 * Settings) through the "system" choice, so its gateway does nothing.
 */
interface AppLocaleGateway {
    /** Called whenever the saved choice changes (and once at start). */
    fun apply(choice: String)

    /** A language picked outside the app (system settings) since the last [apply], or null. */
    fun externalChoice(): String?
}

object NoOpAppLocaleGateway : AppLocaleGateway {
    override fun apply(choice: String) = Unit
    override fun externalChoice(): String? = null
}
