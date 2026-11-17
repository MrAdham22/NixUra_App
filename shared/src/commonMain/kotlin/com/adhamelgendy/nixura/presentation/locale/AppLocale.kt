package com.adhamelgendy.nixura.presentation.locale

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import com.adhamelgendy.nixura.domain.model.DigitStyle
import com.adhamelgendy.nixura.domain.model.LANGUAGE_SYSTEM

/**
 * Which language the app shows. The saved choice is "system", "en" or "ar"; [resolve] turns it into the language
 * actually shown ("en" or "ar"). Pure logic, shared with iOS.
 */
object AppLocales {
    const val ENGLISH = "en"
    const val ARABIC = "ar"
    val supported: List<String> = listOf(ENGLISH, ARABIC)

    /**
     * The language to show for a saved [choice]. "system" (or anything unknown) follows [systemTag] (the phone's
     * language, e.g. "ar-EG", "en_US", "ar-XB"), and falls back to English when the phone's language is not one of ours.
     */
    fun resolve(choice: String?, systemTag: String?): String {
        val chosen = primary(choice)
        if (chosen in supported) return chosen
        val system = primary(systemTag)
        return if (system in supported) system else ENGLISH
    }

    /** "ar-EG" → "ar", "en_US" → "en", null → "". */
    fun primary(tag: String?): String =
        tag?.trim()?.lowercase()?.replace('_', '-')?.substringBefore('-').orEmpty()

    fun isRtl(language: String): Boolean = primary(language) == ARABIC

    /** The tag handed to the OS as the app's own language, or null to follow the phone. */
    fun osTag(choice: String): String? = primary(choice).takeIf { it in supported }

    /** The saved choice for a tag read back from the OS: an empty or unknown tag means "follow the phone". */
    fun choiceFromOs(tag: String?): String = primary(tag).takeIf { it in supported } ?: LANGUAGE_SYSTEM
}

/** Western ↔ Arabic-Indic digits for text on screen. Never used on stored data. */
object Digits {
    private const val ARABIC_ZERO = '٠'
    private const val ARABIC_DECIMAL = '٫'

    /**
     * [text] with its digits drawn in [style]. A "." between two digits becomes the Arabic decimal mark. Emails and
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
