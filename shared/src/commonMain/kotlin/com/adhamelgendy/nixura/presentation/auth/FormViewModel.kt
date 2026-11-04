package com.adhamelgendy.nixura.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamelgendy.nixura.domain.auth.AuthError
import com.adhamelgendy.nixura.domain.auth.AuthResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Immutable UI state shared by every auth form: a spinner and the last typed error (localised in the UI). */
data class FormState(val loading: Boolean = false, val error: AuthError? = null)

/**
 * Base for the small per-screen ViewModels. Text the user typed stays in the screen (like the rest of the app);
 * the ViewModel owns "is a request running", the last error and a one-shot [done] event.
 */
abstract class FormViewModel : ViewModel() {
    private val _state = MutableStateFlow(FormState())
    val state: StateFlow<FormState> = _state.asStateFlow()

    private val doneChannel = Channel<Unit>(Channel.BUFFERED)

    /** Fires once after a successful submit. Collect it in a LaunchedEffect; never stored as state. */
    val done: Flow<Unit> = doneChannel.receiveAsFlow()

    fun dismissError() = _state.update { it.copy(error = null) }

    /** Shows a client-side validation error without a request. */
    protected fun fail(error: AuthError) = _state.update { it.copy(error = error) }

    protected fun <T> submit(call: suspend () -> AuthResult<T>, onSuccess: suspend (T) -> Unit = {}, onFailure: suspend (AuthError) -> Unit = {}) {
        if (_state.value.loading) return // ignore double taps
        _state.update { FormState(loading = true) }
        viewModelScope.launch {
            when (val result = call()) {
                is AuthResult.Success -> {
                    onSuccess(result.value)
                    _state.update { it.copy(loading = false) }
                    doneChannel.trySend(Unit)
                }
                is AuthResult.Failure -> {
                    onFailure(result.error)
                    _state.update { FormState(loading = false, error = result.error) }
                }
            }
        }
    }
}
