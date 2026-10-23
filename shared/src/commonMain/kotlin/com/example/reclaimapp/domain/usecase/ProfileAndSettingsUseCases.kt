package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.model.AppSettings
import com.example.reclaimapp.domain.model.UserProfile
import com.example.reclaimapp.domain.repository.DataMaintenanceRepository
import com.example.reclaimapp.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ObserveProfileUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<UserProfile> = repository.observeProfile()
}

class ObserveSettingsUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<AppSettings> = repository.observeSettings()
}

class SaveProfileUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(name: String, email: String, age: Int?): Result<Unit> = runCatching {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        require(cleanName.length <= 40) { "Name is too long" }
        require(cleanEmail.isEmpty() || isPlausibleEmail(cleanEmail)) { "Enter a valid email address" }
        require(age == null || age in 5..120) { "Enter a valid age" }
        repository.saveProfile(UserProfile(cleanName, cleanEmail, age))
    }

    companion object {
        fun isPlausibleEmail(value: String): Boolean {
            val at = value.indexOf('@')
            if (at <= 0 || at != value.lastIndexOf('@')) return false
            val domain = value.substring(at + 1)
            return domain.length >= 3 && domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.') && !value.contains(' ')
        }
    }
}

class UpdateSettingsUseCase(private val repository: PreferencesRepository) {
    // Read-modify-write: serialize so quick consecutive toggles cannot overwrite each other.
    private val lock = Mutex()

    suspend operator fun invoke(transform: (AppSettings) -> AppSettings): Result<Unit> = runCatching {
        lock.withLock {
            val updated = transform(repository.observeSettings().first())
            require(updated.dailyCheckInMinutes in 0 until 24 * 60) { "Invalid time" }
            repository.saveSettings(updated)
        }
    }
}

class ClearAllDataUseCase(private val repository: DataMaintenanceRepository) {
    suspend operator fun invoke(): Result<Unit> = runCatching { repository.clearAllData() }
}
