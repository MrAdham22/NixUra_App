package com.adhamelgendy.nixura.domain.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val age: Int? = null,
)

data class AppSettings(
    val darkTheme: Boolean = true,
    /** Follow the phone's light/dark setting instead of [darkTheme]. Per device, never synced. */
    val followSystemTheme: Boolean = false,
    /** "system" (follow the phone), "en" or "ar". Synced. See `presentation/locale/AppLocale`. */
    val languageCode: String = LANGUAGE_SYSTEM,
    /** Digits shown on screen (Western 123 or Arabic-Indic ١٢٣). Display only; stored data never changes. Per device. */
    val digitStyle: DigitStyle = DigitStyle.WESTERN,
    val notificationsEnabled: Boolean = true,
    /** Minutes after local midnight for the daily check-in notification. */
    val dailyCheckInMinutes: Int = 8 * 60 + 30,
    /** The starter guide runs once per device; Settings can replay it. Not synced. */
    val guideCompleted: Boolean = false,
    /** Anonymous usage stats (Firebase Analytics). Off until the user turns it on; per device, never synced. */
    val usageStatsEnabled: Boolean = false,
    /**
     * The real AI coach: `null` = not asked yet, `true` = the user agreed that messages and habit data go to online AI
     * services, `false` = declined (offline coach only). Per device, never synced.
     */
    val aiCoachConsent: Boolean? = null,
    /** The opt-in "Today" notification on the lock screen. Per device, never synced. */
    val todayNotification: Boolean = false,
    /** Today also shows the longest quit streak, without a name. Off until the user asks for it. Per device. */
    val todayShowStreak: Boolean = false,
    /** Today was offered once, when a second good habit was added. Per device. */
    val todayOfferSeen: Boolean = false,
)

/** The language choice that follows the phone's language. */
const val LANGUAGE_SYSTEM: String = "system"

/** How numbers are drawn on screen. Stored data is always plain Western digits. */
enum class DigitStyle { WESTERN, ARABIC_INDIC }

enum class CoachRole { USER, COACH }

/** One coach chat in the history. [title] is empty until the user writes something. */
data class CoachConversation(
    val id: Long,
    val title: String,
    val preview: String,
    val updatedAtEpochMillis: Long,
)

data class CoachMessage(
    val id: Long,
    val role: CoachRole,
    val text: String,
    val createdAtEpochMillis: Long,
    /** Keys of the progress cards to draw under this reply ("quit:<id>", "good:<id>", "overview"). */
    val visuals: List<String> = emptyList(),
)
