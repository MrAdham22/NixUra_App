package com.example.reclaimapp.domain.model

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
    val onboardingCompleted: Boolean = false,
)

enum class CoachRole { USER, COACH }

data class CoachMessage(
    val id: Long,
    val role: CoachRole,
    val text: String,
    val createdAtEpochMillis: Long,
)
