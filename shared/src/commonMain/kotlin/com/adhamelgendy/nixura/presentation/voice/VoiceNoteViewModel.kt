package com.adhamelgendy.nixura.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamelgendy.nixura.domain.coach.ai.AiMode
import com.adhamelgendy.nixura.domain.coach.ai.AiResult
import com.adhamelgendy.nixura.domain.coach.ai.CoachAction
import com.adhamelgendy.nixura.domain.coach.ai.VoiceRecorder
import com.adhamelgendy.nixura.domain.usecase.ApplyCoachActionsUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveAiModeUseCase
import com.adhamelgendy.nixura.domain.usecase.SetAiConsentUseCase
import com.adhamelgendy.nixura.domain.usecase.UnderstandVoiceNoteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class VoiceFailure { Microphone, NothingHeard, NothingToAdd, LimitReached, AiResting, Offline }

data class ReviewItem(val action: CoachAction, val checked: Boolean = true)

sealed interface VoiceStage {
    /** The sheet is closed. */
    data object Idle : VoiceStage
    data object NeedsSignIn : VoiceStage
    data object NeedsConsent : VoiceStage
    data object NeedsPermission : VoiceStage
    data class Recording(val seconds: Int, val level: Float) : VoiceStage
    data object Understanding : VoiceStage

    /** What the AI understood; nothing is added until the user confirms. [transcript] is empty for chat suggestions. */
    data class Review(val transcript: String, val items: List<ReviewItem>, val adding: Boolean = false) : VoiceStage
    data class Done(val added: Int, val failed: Int) : VoiceStage
    data class Failed(val reason: VoiceFailure) : VoiceStage
}

/**
 * The voice note flow: record, let the AI understand it, show what it understood, add what the user keeps.
 * One instance is shared by the whole app, so the mic buttons on Home and in the coach open the same sheet.
 */
class VoiceNoteViewModel(
    private val recorder: VoiceRecorder,
    private val understand: UnderstandVoiceNoteUseCase,
    private val apply: ApplyCoachActionsUseCase,
    private val aiMode: ObserveAiModeUseCase,
    private val setConsent: SetAiConsentUseCase,
) : ViewModel() {

    private val _stage = MutableStateFlow<VoiceStage>(VoiceStage.Idle)
    val stage: StateFlow<VoiceStage> = _stage.asStateFlow()
    val supported: Boolean get() = recorder.isSupported

    private var ticker: Job? = null
    private var work: Job? = null

    /** A mic button was tapped. */
    fun open() {
        if (_stage.value != VoiceStage.Idle) return
        viewModelScope.launch {
            _stage.value = when (aiMode().first()) {
                AiMode.Guest -> VoiceStage.NeedsSignIn
                AiMode.NeedsConsent, AiMode.Declined -> VoiceStage.NeedsConsent
                AiMode.On -> if (recorder.hasPermission()) return@launch startRecording() else VoiceStage.NeedsPermission
            }
        }
    }

    /** The coach suggested things to add in the chat: straight to the review list. */
    fun review(actions: List<CoachAction>) {
        if (actions.isEmpty() || _stage.value != VoiceStage.Idle) return
        _stage.value = VoiceStage.Review("", actions.map { ReviewItem(it) })
    }

    fun onConsent() {
        viewModelScope.launch {
            setConsent(true)
            if (recorder.hasPermission()) startRecording() else _stage.value = VoiceStage.NeedsPermission
        }
    }

    fun onAskPermission() = recorder.requestPermission()

    /** Called while the permission question may be open; starts as soon as the user allowed the microphone. */
    fun onPermissionMaybeGranted() {
        if (_stage.value == VoiceStage.NeedsPermission && recorder.hasPermission()) startRecording()
    }

    private fun startRecording() {
        if (!recorder.start()) {
            _stage.value = VoiceStage.Failed(VoiceFailure.Microphone)
            return
        }
        _stage.value = VoiceStage.Recording(0, 0f)
        ticker?.cancel()
        ticker = viewModelScope.launch {
            var ticks = 0
            while (isActive) {
                delay(TICK_MILLIS)
                ticks++
                if (ticks * TICK_MILLIS >= MAX_SECONDS * 1000) {
                    onStop()
                    break
                }
                _stage.value = VoiceStage.Recording((ticks * TICK_MILLIS / 1000).toInt(), recorder.level())
            }
        }
    }

    fun onStop() {
        val recording = _stage.value as? VoiceStage.Recording ?: return
        ticker?.cancel()
        val clip = recorder.stop()
        if (clip == null || recording.seconds < MIN_SECONDS) {
            _stage.value = VoiceStage.Failed(VoiceFailure.NothingHeard)
            return
        }
        _stage.value = VoiceStage.Understanding
        work = viewModelScope.launch {
            _stage.value = when (val r = understand(clip)) {
                is AiResult.Ok -> if (r.value.actions.isEmpty()) {
                    VoiceStage.Failed(if (r.value.transcript.isBlank()) VoiceFailure.NothingHeard else VoiceFailure.NothingToAdd)
                } else {
                    VoiceStage.Review(r.value.transcript, r.value.actions.map { ReviewItem(it) })
                }
                AiResult.LimitReached -> VoiceStage.Failed(VoiceFailure.LimitReached)
                AiResult.Offline -> VoiceStage.Failed(VoiceFailure.Offline)
                AiResult.Unavailable, AiResult.SignedOut -> VoiceStage.Failed(VoiceFailure.AiResting)
            }
        }
    }

    fun onToggle(index: Int) {
        val review = _stage.value as? VoiceStage.Review ?: return
        if (review.adding) return
        _stage.value = review.copy(items = review.items.mapIndexed { i, item -> if (i == index) item.copy(checked = !item.checked) else item })
    }

    fun onConfirm() {
        val review = _stage.value as? VoiceStage.Review ?: return
        val chosen = review.items.filter { it.checked }.map { it.action }
        if (review.adding || chosen.isEmpty()) return
        _stage.value = review.copy(adding = true)
        work = viewModelScope.launch {
            val outcome = apply(chosen)
            // If the sheet was closed meanwhile, the items are still added but the sheet stays closed.
            if (_stage.value is VoiceStage.Review) _stage.value = VoiceStage.Done(outcome.added, outcome.failed)
        }
    }

    /** Try again after a failure, or record another note. */
    fun onRetry() {
        _stage.value = VoiceStage.Idle
        open()
    }

    fun onClose() {
        ticker?.cancel()
        // Adding is quick and must not be cut in half; anything else stops here.
        if ((_stage.value as? VoiceStage.Review)?.adding != true) work?.cancel()
        if (_stage.value is VoiceStage.Recording) recorder.cancel()
        _stage.value = VoiceStage.Idle
    }

    override fun onCleared() {
        recorder.cancel()
    }

    private companion object {
        const val TICK_MILLIS = 100L
        const val MAX_SECONDS = 60
        const val MIN_SECONDS = 1
    }
}
