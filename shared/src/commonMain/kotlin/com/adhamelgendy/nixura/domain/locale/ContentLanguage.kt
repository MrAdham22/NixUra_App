package com.adhamelgendy.nixura.domain.locale

import com.adhamelgendy.nixura.domain.model.LANGUAGE_SYSTEM
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

/**
 * The language of text written by the domain (the offline coach, its notifications, the habit catalog). The UI
 * calls [update] whenever the saved choice or the phone's language changes; the notification planner does the same
 * from the saved settings, so background work also writes in the right language. Matching habit names never depends
 * on it.
 */
class ContentLanguage(private val systemTag: () -> String?) {
    private val state = MutableStateFlow(AppLocales.ENGLISH)

    /** The resolved language: "en" or "ar". */
    val current: StateFlow<String> = state.asStateFlow()

    val isArabic: Boolean get() = state.value == AppLocales.ARABIC

    /** Resolves a saved choice ("system", "en", "ar") against the phone's language, remembers it and returns it. */
    fun update(choice: String): String = AppLocales.resolve(choice, systemTag()).also { state.value = it }

    companion object {
        /** Always English: tests and places without a language. */
        val English: ContentLanguage get() = ContentLanguage { null }
    }
}
