package com.example.reclaimapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reclaimapp.domain.model.AppSettings
import com.example.reclaimapp.domain.usecase.ClearAllDataUseCase
import com.example.reclaimapp.domain.usecase.ObserveSettingsUseCase
import com.example.reclaimapp.domain.usecase.SaveProfileUseCase
import com.example.reclaimapp.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    /** False until the first read from storage, so the app does not flash the wrong theme. */
    val loaded: Boolean = false,
    val errorMessage: String? = null,
)

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val saveProfile: SaveProfileUseCase,
    private val clearAllData: ClearAllDataUseCase,
) : ViewModel() {

    private val transient = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(observeSettings(), transient) { settings, error ->
        SettingsUiState(settings = settings, loaded = true, errorMessage = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onDarkTheme(enabled: Boolean) = update { it.copy(darkTheme = enabled) }

    fun onNotifications(enabled: Boolean) = update { it.copy(notificationsEnabled = enabled) }

    fun onCheckInMinutes(minutes: Int) = update { it.copy(dailyCheckInMinutes = minutes) }

    fun onLanguage(code: String) = update { it.copy(languageCode = code) }

    fun onCompleteOnboarding(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) saveProfile(name, "", null)
            updateSettings { it.copy(onboardingCompleted = true) }
        }
    }

    fun onEraseAllData() {
        viewModelScope.launch {
            clearAllData().onFailure { e -> transient.value = e.message ?: "Could not erase data" }
        }
    }

    fun onDismissError() {
        transient.value = null
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            updateSettings(transform).onFailure { e -> transient.value = e.message ?: "Could not save settings" }
        }
    }
}
