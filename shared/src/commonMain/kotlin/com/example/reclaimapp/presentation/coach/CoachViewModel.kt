package com.example.reclaimapp.presentation.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reclaimapp.domain.coach.BadHabitSnapshot
import com.example.reclaimapp.domain.coach.CoachNote
import com.example.reclaimapp.domain.model.CoachConversation
import com.example.reclaimapp.domain.model.CoachMessage
import com.example.reclaimapp.domain.usecase.DeleteCoachChatUseCase
import com.example.reclaimapp.domain.usecase.EnsureCoachGreetingUseCase
import com.example.reclaimapp.domain.usecase.GetCoachContextUseCase
import com.example.reclaimapp.domain.usecase.ObserveCoachConversationsUseCase
import com.example.reclaimapp.domain.usecase.ObserveCoachMessagesUseCase
import com.example.reclaimapp.domain.usecase.SendCoachMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoachUiState(
    val messages: List<CoachMessage> = emptyList(),
    /** Past chats for the history list, newest first. */
    val conversations: List<CoachConversation> = emptyList(),
    val activeChatId: Long? = null,
    val isTyping: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val insight: CoachNote? = null,
    /** Active quit habits the user can choose to talk about. */
    val habits: List<BadHabitSnapshot> = emptyList(),
    /** Habit currently shown in the focus strip. */
    val focusHabitId: Long? = null,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModel(
    observeMessages: ObserveCoachMessagesUseCase,
    observeConversations: ObserveCoachConversationsUseCase,
    private val ensureGreeting: EnsureCoachGreetingUseCase,
    private val sendMessage: SendCoachMessageUseCase,
    private val deleteChat: DeleteCoachChatUseCase,
    private val getContext: GetCoachContextUseCase,
) : ViewModel() {

    private val transient = MutableStateFlow(CoachUiState())
    private val activeChat = MutableStateFlow<Long?>(null)

    /** Habit the user explicitly picked; `null` until they choose one. Sent with every message. */
    private var selectedHabitId: Long? = null

    private val messages = activeChat.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else observeMessages(id) }

    val uiState: StateFlow<CoachUiState> = combine(messages, observeConversations(), activeChat, transient) { messages, chats, active, extra ->
        extra.copy(messages = messages, conversations = chats, activeChatId = active)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoachUiState(),
    )

    init {
        onScreenShown()
    }

    /** Called whenever the coach screen is shown (e.g. after all data was erased, the chat needs a new greeting). */
    fun onScreenShown() {
        viewModelScope.launch {
            activeChat.value = ensureGreeting(activeChat.value)
            refreshContext()
        }
    }

    fun onNewChat() {
        viewModelScope.launch { activeChat.value = ensureGreeting.newChat() }
    }

    fun onOpenChat(id: Long) {
        activeChat.value = id
    }

    fun onDeleteChat(id: Long) {
        viewModelScope.launch {
            deleteChat(id).onFailure { e -> transient.update { it.copy(errorMessage = e.message) } }
            if (activeChat.value == id) activeChat.value = ensureGreeting()
        }
    }

    fun onSend(text: String) {
        if (transient.value.isTyping) return
        val chat = activeChat.value ?: return
        viewModelScope.launch {
            transient.update { it.copy(isTyping = true, errorMessage = null) }
            val result = sendMessage(chat, text, replyDelayMillis = REPLY_DELAY_MILLIS, focusHabitId = selectedHabitId)
            transient.update { it.copy(isTyping = false, errorMessage = result.exceptionOrNull()?.message) }
            refreshContext()
        }
    }

    /** The user picked which quit habit to talk about; the coach uses its data from now on. */
    fun onSelectHabit(habitId: Long) {
        selectedHabitId = habitId
        viewModelScope.launch { refreshContext() }
    }

    fun onDismissError() {
        transient.update { it.copy(errorMessage = null) }
    }

    private suspend fun refreshContext() {
        val context = getContext(selectedHabitId)
        // If the picked habit was archived or deleted, forget the pick.
        if (selectedHabitId != null && context.habits.none { it.id == selectedHabitId }) selectedHabitId = null
        transient.update {
            it.copy(
                suggestions = context.suggestions,
                insight = context.insight,
                habits = context.habits,
                focusHabitId = context.focusHabitId,
            )
        }
    }

    private companion object {
        const val REPLY_DELAY_MILLIS = 700L
    }
}
