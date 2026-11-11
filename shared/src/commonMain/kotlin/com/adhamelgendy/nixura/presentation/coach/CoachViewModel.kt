package com.adhamelgendy.nixura.presentation.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamelgendy.nixura.domain.coach.BadHabitSnapshot
import com.adhamelgendy.nixura.domain.coach.CoachNote
import com.adhamelgendy.nixura.domain.coach.ai.AiMode
import com.adhamelgendy.nixura.domain.coach.ai.AiQuota
import com.adhamelgendy.nixura.domain.coach.ai.CoachAction
import com.adhamelgendy.nixura.domain.coach.ai.CoachVisual
import com.adhamelgendy.nixura.domain.usecase.BuildCoachVisualsUseCase
import com.adhamelgendy.nixura.domain.usecase.CoachNotice
import com.adhamelgendy.nixura.domain.usecase.GetAiQuotaUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveAiModeUseCase
import com.adhamelgendy.nixura.domain.usecase.SetAiConsentUseCase
import com.adhamelgendy.nixura.domain.model.CoachConversation
import com.adhamelgendy.nixura.domain.model.CoachMessage
import com.adhamelgendy.nixura.domain.usecase.DeleteCoachChatUseCase
import com.adhamelgendy.nixura.domain.usecase.EnsureCoachGreetingUseCase
import com.adhamelgendy.nixura.domain.usecase.GetCoachContextUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveCoachConversationsUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveCoachMessagesUseCase
import com.adhamelgendy.nixura.domain.usecase.SendCoachMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val aiMode: AiMode = AiMode.Guest,
    /** Today's remaining free AI uses; null until the server said. */
    val quota: AiQuota? = null,
    /** Set while the "use the AI coach?" question is open; holds the message waiting to be sent. */
    val consentFor: String? = null,
    /** Why the offline coach answered the last message although the AI is on. */
    val notice: CoachNotice? = null,
    /** Things the AI wants to add for the user; the screen opens the review sheet and then clears this. */
    val pendingActions: List<CoachAction> = emptyList(),
    /** Data of the progress cards, by key (see [CoachMessage.visuals]). */
    val visuals: Map<String, CoachVisual> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModel(
    observeMessages: ObserveCoachMessagesUseCase,
    observeConversations: ObserveCoachConversationsUseCase,
    private val ensureGreeting: EnsureCoachGreetingUseCase,
    private val sendMessage: SendCoachMessageUseCase,
    private val deleteChat: DeleteCoachChatUseCase,
    private val getContext: GetCoachContextUseCase,
    observeAiMode: ObserveAiModeUseCase,
    private val setAiConsent: SetAiConsentUseCase,
    private val getQuota: GetAiQuotaUseCase,
    private val buildVisuals: BuildCoachVisualsUseCase,
) : ViewModel() {

    private val transient = MutableStateFlow(CoachUiState())
    private val activeChat = MutableStateFlow<Long?>(null)

    /** Habit the user explicitly picked; `null` until they choose one. Sent with every message. */
    private var selectedHabitId: Long? = null

    private val messages = activeChat.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else observeMessages(id) }

    private val aiMode = observeAiMode()

    val uiState: StateFlow<CoachUiState> = combine(messages, observeConversations(), activeChat, transient, aiMode) { messages, chats, active, extra, mode ->
        extra.copy(messages = messages, conversations = chats, activeChatId = active, aiMode = mode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoachUiState(),
    )

    init {
        onScreenShown()
        // Progress cards: whenever the chat shows a card, read its numbers fresh.
        viewModelScope.launch {
            messages.collect { list ->
                val keys = list.flatMap { it.visuals }.toSet()
                transient.update { it.copy(visuals = buildVisuals(keys)) }
            }
        }
        viewModelScope.launch {
            aiMode.collect { mode -> transient.update { it.copy(quota = if (mode == AiMode.On) getQuota() else null) } }
        }
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
        // A signed-in user is asked once whether the real AI may be used, before the first message goes anywhere.
        if (uiState.value.aiMode == AiMode.NeedsConsent) {
            transient.update { it.copy(consentFor = text) }
            return
        }
        viewModelScope.launch {
            transient.update { it.copy(isTyping = true, errorMessage = null, notice = null) }
            val result = sendMessage(chat, text, replyDelayMillis = REPLY_DELAY_MILLIS, focusHabitId = selectedHabitId)
            val outcome = result.getOrNull()
            transient.update {
                it.copy(
                    isTyping = false,
                    errorMessage = result.exceptionOrNull()?.message,
                    notice = outcome?.notice,
                    quota = outcome?.quota ?: if (outcome?.notice == CoachNotice.LimitReached) it.quota?.copy(chatLeft = 0) else it.quota,
                    pendingActions = outcome?.actions.orEmpty(),
                )
            }
            refreshContext()
        }
    }

    /** The answer to "use the AI coach?". Either way the waiting message is then sent (to the AI or the offline coach). */
    fun onConsent(agreed: Boolean) {
        val waiting = transient.value.consentFor ?: return
        transient.update { it.copy(consentFor = null) }
        viewModelScope.launch {
            setAiConsent(agreed)
            aiMode.first { it != AiMode.NeedsConsent }
            onSend(waiting)
        }
    }

    fun onConsentDismissed() = transient.update { it.copy(consentFor = null) }

    fun onActionsHandled() = transient.update { it.copy(pendingActions = emptyList()) }

    fun onDismissNotice() = transient.update { it.copy(notice = null) }

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
