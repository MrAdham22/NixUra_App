package com.example.reclaimapp.domain.model

data class Habit(
    val id: Long,
    val name: String,
    val createdAtEpochMillis: Long,
    val archivedAtEpochMillis: Long? = null,
) {
    val isArchived: Boolean get() = archivedAtEpochMillis != null
}
