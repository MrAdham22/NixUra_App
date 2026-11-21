package com.adhamelgendy.nixura.presentation.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.support.BugCategory
import com.adhamelgendy.nixura.domain.support.BugReportDraft
import com.adhamelgendy.nixura.domain.support.BugReportProblem
import com.adhamelgendy.nixura.domain.support.BugReportRules
import com.adhamelgendy.nixura.domain.support.KeepNixUraSummary
import com.adhamelgendy.nixura.domain.support.SendBugReportResult
import com.adhamelgendy.nixura.domain.support.SendBugReportUseCase
import com.adhamelgendy.nixura.domain.support.SupportActions
import com.adhamelgendy.nixura.domain.usecase.ObserveActiveHabitTrackingUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveGoodHabitsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** "Don't delete NixUra!": the user's real progress (null while it loads) and the way to the system app page. */
class KeepNixUraViewModel(
    observeQuit: ObserveActiveHabitTrackingUseCase,
    observeGood: ObserveGoodHabitsUseCase,
    sessions: SessionManager,
    private val actions: SupportActions,
) : ViewModel() {
    val summary: StateFlow<KeepNixUraSummary?> =
        combine(observeQuit(), observeGood(), sessions.state) { quit, good, session ->
            KeepNixUraSummary.from(quit, good, isGuest = session !is SessionState.SignedIn)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** False when the page could not be opened (the screen then says where to uninstall instead). */
    fun openAppSettings(): Boolean = actions.openAppSettings()
}

data class ReportBugUiState(
    val message: String = "",
    val steps: String = "",
    val category: BugCategory? = null,
    /** Shown only after the user pressed Send once, so an empty form does not start with an error. */
    val problem: BugReportProblem? = null,
    val sending: Boolean = false,
    val result: SendBugReportResult.Sent? = null,
)

/** "Bugs or issues?": the form, its validation and the send. */
class ReportBugViewModel(
    private val send: SendBugReportUseCase,
    sessions: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(ReportBugUiState())
    val state: StateFlow<ReportBugUiState> = _state.asStateFlow()

    val signedIn: StateFlow<Boolean> = sessions.state.map { it is SessionState.SignedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), sessions.state.value is SessionState.SignedIn)

    fun onMessage(text: String) = _state.update { it.copy(message = text, problem = it.problem?.let { _ -> problemOf(text, it.steps) }) }
    fun onSteps(text: String) = _state.update { it.copy(steps = text) }

    /** Tapping the selected category again clears it (the category is optional). */
    fun onCategory(category: BugCategory) = _state.update { it.copy(category = if (it.category == category) null else category) }

    fun onSend() {
        val s = _state.value
        if (s.sending) return // double tap
        val draft = s.toDraft()
        BugReportRules.problem(draft)?.let { p -> _state.update { it.copy(problem = p) }; return }
        _state.update { it.copy(sending = true, problem = null) }
        viewModelScope.launch {
            when (val result = send(draft)) {
                is SendBugReportResult.Invalid -> _state.update { it.copy(sending = false, problem = result.problem) }
                is SendBugReportResult.Sent -> _state.update { it.copy(sending = false, result = result) }
            }
        }
    }

    private fun problemOf(message: String, steps: String) =
        BugReportRules.problem(BugReportDraft(message, steps))

    private fun ReportBugUiState.toDraft() = BugReportDraft(message, steps, category ?: BugCategory.OTHER)
}
