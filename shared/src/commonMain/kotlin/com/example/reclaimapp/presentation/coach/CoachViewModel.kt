package com.example.reclaimapp.presentation.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reclaimapp.domain.coach.CoachNote
import com.example.reclaimapp.domain.model.CoachMessage
import com.example.reclaimapp.domain.usecase.ClearCoachChatUseCase
import com.example.reclaimapp.domain.usecase.EnsureCoachGreetingUseCase
import com.example.reclaimapp.domain.usecase.GetCoachContextUseCase
import com.example.reclaimapp.domain.usecase.ObserveCoachMessagesUseCase
import com.example.reclaimapp.domain.usecase.SendCoachMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoachUiState(
    val messages: List<CoachMessage> = emptyList(),
    val isTyping: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val insight: CoachNote? = null,
    val errorMessage: String? = null,
)

class CoachViewModel(
    observeMessages: ObserveCoachMessagesUseCase,
    private val ensureGreeting: EnsureCoachGreetingUseCase,
    private val sendMessage: SendCoachMessageUseCase,
    private val clearChat: ClearCoachChatUseCase,
    private val getContext: GetCoachContextUseCase,
) : ViewModel() {

    private val transient = MutableStateFlow(CoachUiState())

    val uiState: StateFlow<CoachUiState> = combine(observeMessages(), transient) { messages, extra ->
        extra.copy(messages = messages)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoachUiState(),
    )

    init {
        viewModelScope.launch {
            ensureGreeting()
            refreshContext()
        }
    }

    /** Called whenever the coach screen is shown (e.g. after all data was erased, the chat needs a new greeting). */
    fun onScreenShown() {
        viewModelScope.launch {
            ensureGreeting()
            refreshContext()
        }
    }

    fun onSend(text: String) {
        if (transient.value.isTyping) return
        viewModelScope.launch {
            transient.update { it.copy(isTyping = true, errorMessage = null) }
            val result = sendMessage(text, replyDelayMillis = REPLY_DELAY_MILLIS)
            transient.update { it.copy(isTyping = false, errorMessage = result.exceptionOrNull()?.message) }
            refreshContext()
        }
    }

    fun onClear() {
        viewModelScope.launch {
            clearChat()
            ensureGreeting()
        }
    }

    fun onDismissError() {
        transient.update { it.copy(errorMessage = null) }
    }

    private suspend fun refreshContext() {
        val context = getContext()
        transient.update { it.copy(suggestions = context.suggestions, insight = context.insight) }
    }

    private companion object {
        const val REPLY_DELAY_MILLIS = 700L
    }
}
