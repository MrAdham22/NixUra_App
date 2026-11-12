package com.adhamelgendy.nixura.presentation.habits

import com.adhamelgendy.nixura.domain.recognition.HabitTopic
import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.money.MoneySaved
import kotlin.time.Duration

data class HabitCardUiModel(
    val habitId: Long,
    val name: String,
    val activeElapsed: Duration,
    val longestStreakLabel: String,
    val activeStageIndex: Int,
    val stageCount: Int,
    /** User-chosen base color (ARGB) or `null` for the default NixUra progression. */
    val colorArgb: Long? = null,
    val longestStreak: Duration = Duration.ZERO,
    val relapseCount: Int = 0,
    /** When the current clean streak began (null if no tracking start). */
    val activePeriodStartEpochMillis: Long? = null,
    val trackingStartEpochMillis: Long? = null,
    /** Timestamps of recorded relapses, newest first. */
    val relapseTimestamps: List<Long> = emptyList(),
    /** What the habit used to cost (`null` = no money tracker). */
    val cost: HabitCost? = null,
    /** Saved during the current streak, live; `null` without a tracker. */
    val moneySaved: MoneySaved? = null,
    /** Kind of habit, for the glyph on the card (derived from the name). */
    val topic: HabitTopic = HabitTopic.OTHER_QUIT,
)

data class HabitsUiState(
    val habits: List<HabitCardUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val createNameInput: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
)
