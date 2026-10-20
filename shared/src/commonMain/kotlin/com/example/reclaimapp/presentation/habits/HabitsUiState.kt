package com.example.reclaimapp.presentation.habits

import kotlin.time.Duration

data class HabitCardUiModel(
    val habitId: Long,
    val name: String,
    val activeElapsed: Duration,
    val longestStreakLabel: String,
    val activeStageIndex: Int,
    val stageCount: Int,
)

data class HabitsUiState(
    val habits: List<HabitCardUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val createNameInput: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
)
