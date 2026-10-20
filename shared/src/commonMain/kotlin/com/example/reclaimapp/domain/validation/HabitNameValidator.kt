package com.example.reclaimapp.domain.validation

object HabitNameValidator {
    private const val MAX_LENGTH = 80

    fun validate(raw: String): Result<String> {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> Result.failure(IllegalArgumentException("Habit name cannot be empty"))
            trimmed.length > MAX_LENGTH -> Result.failure(
                IllegalArgumentException("Habit name must be at most $MAX_LENGTH characters"),
            )
            else -> Result.success(trimmed)
        }
    }
}
