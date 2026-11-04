package com.adhamelgendy.nixura.domain.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val age: Int? = null,
)

data class AppSettings(
    val darkTheme: Boolean = true,
    val languageCode: String = "en",
    val notificationsEnabled: Boolean = true,
    /** Minutes after local midnight for the daily check-in notification. */
    val dailyCheckInMinutes: Int = 8 * 60 + 30,
    /** The starter guide runs once per device; Settings can replay it. Not synced. */
    val guideCompleted: Boolean = false,
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
)
