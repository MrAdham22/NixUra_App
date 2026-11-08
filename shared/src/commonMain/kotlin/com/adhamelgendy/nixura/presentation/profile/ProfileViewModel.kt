package com.adhamelgendy.nixura.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.calculator.HabitScoreLine
import com.adhamelgendy.nixura.domain.calculator.ScoreCalculator
import com.adhamelgendy.nixura.domain.calculator.TrackingStateCalculator
import com.adhamelgendy.nixura.domain.auth.AuthRepository
import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.usecase.ObserveActiveHabitTrackingUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveEventsUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveGoodHabitsUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveProfileUseCase
import com.adhamelgendy.nixura.domain.usecase.SaveProfileUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileStats(
    val quitScore: Int? = null,
    val disciplineScore: Int? = null,
    val momentum: Int? = null,
    /** Points vs the previous 7 days; only when every good habit is at least a week old. */
    val disciplineDelta: Int? = null,
    val badHabitCount: Int = 0,
    val goodHabitCount: Int = 0,
    val totalClearDays: Long = 0,
    val bestStreakDays: Long = 0,
    val totalTimesDone: Int = 0,
    val nextEvent: Event? = null,
    /** Each habit's share of [quitScore] and [disciplineScore], so the app can show how they were reached. */
    val quitLines: List<HabitScoreLine> = emptyList(),
    val disciplineLines: List<HabitScoreLine> = emptyList(),
)

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val age: Int? = null,
    /** Non-null while signed in: the account's email, which cannot be edited here. */
    val accountEmail: String? = null,
    val stats: ProfileStats = ProfileStats(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    observeBad: ObserveActiveHabitTrackingUseCase,
    observeGood: ObserveGoodHabitsUseCase,
    observeEvents: ObserveEventsUseCase,
    observeProfile: ObserveProfileUseCase,
    private val saveProfile: SaveProfileUseCase,
    private val clock: Clock,
    private val dayCalculator: DayCalculator,
    private val sessions: SessionManager,
    private val auth: AuthRepository,
) : ViewModel() {

    private val tick = MutableStateFlow(0L)
    private val transient = MutableStateFlow(ProfileUiState())

    private val stats = combine(observeBad(), observeGood(), observeEvents(), tick) { bad, good, events, _ ->
        val now = clock.nowEpochMillis()
        val today = dayCalculator.dayIndexOf(now)
        val live = bad.map { it.copy(tracking = TrackingStateCalculator.compute(it.events, now)) }
        val createdDay = { item: com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs ->
            dayCalculator.dayIndexOf(item.habit.createdAtEpochMillis)
        }
        val quit = ScoreCalculator.quitScore(live)
        val discipline = ScoreCalculator.disciplineScore(good, today, createdDay)
        val previous = ScoreCalculator.disciplineScore(good, today - 7, createdDay)
        val oldEnough = good.isNotEmpty() && good.all { createdDay(it) <= today - 7 }
        ProfileStats(
            quitScore = quit,
            disciplineScore = discipline,
            momentum = ScoreCalculator.momentum(quit, discipline),
            disciplineDelta = if (oldEnough && discipline != null && previous != null) discipline - previous else null,
            badHabitCount = live.size,
            goodHabitCount = good.size,
            totalClearDays = live.sumOf { it.tracking.activeElapsedMillis / 86_400_000L },
            bestStreakDays = live.maxOfOrNull { it.tracking.longestStreakMillis / 86_400_000L } ?: 0L,
            totalTimesDone = good.sumOf { GoodHabitStatsCalculator.compute(it, today, createdDay(it)).totalTimesDone },
            nextEvent = events.filter { it.targetAtEpochMillis > now }.minByOrNull { it.targetAtEpochMillis },
            quitLines = ScoreCalculator.quitLines(live),
            disciplineLines = ScoreCalculator.disciplineLines(good, today, createdDay),
        )
    }

    val uiState: StateFlow<ProfileUiState> = combine(stats, observeProfile(), transient, sessions.state) { s, profile, extra, session ->
        val accountEmail = (session as? SessionState.SignedIn)?.user?.email
        extra.copy(name = profile.name, email = accountEmail ?: profile.email, age = profile.age, accountEmail = accountEmail, stats = s)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    init {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                tick.update { it + 1 }
            }
        }
    }

    fun onSave(name: String, email: String, ageText: String, onSuccess: () -> Unit) {
        val age = ageText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        if (ageText.isNotBlank() && age == null) {
            transient.update { it.copy(errorMessage = "Enter a valid age") }
            return
        }
        viewModelScope.launch {
            transient.update { it.copy(isSaving = true, errorMessage = null) }
            val signedIn = sessions.state.value as? SessionState.SignedIn
            val result = saveProfile(name, signedIn?.user?.email ?: email, age)
            // Best effort: if the server cannot be reached the local copy stays and the next sign-in sync reconciles it.
            if (result.isSuccess && signedIn != null) viewModelScope.launch { auth.updateProfile(name.trim(), age) }
            transient.update { it.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.message) }
            if (result.isSuccess) onSuccess()
        }
    }

    fun onDismissError() {
        transient.update { it.copy(errorMessage = null) }
    }
}
