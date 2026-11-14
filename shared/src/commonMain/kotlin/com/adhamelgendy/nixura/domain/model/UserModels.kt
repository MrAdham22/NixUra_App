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
    val languageCode: String = "en",
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
)

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
