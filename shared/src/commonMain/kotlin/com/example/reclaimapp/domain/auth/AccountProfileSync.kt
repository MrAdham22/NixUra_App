package com.example.reclaimapp.domain.auth

import com.example.reclaimapp.domain.model.UserProfile
import com.example.reclaimapp.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

/**
 * Connects the local Profile screen (name, email, age) to the signed-in account. Runs after sign-in and at app start.
 *
 * - The account's email always wins locally (it is read-only while signed in).
 * - Local name/age typed as a guest are adopted by an account that has none yet (nothing is lost on first sign-in).
 * - Otherwise the account's values are copied down. Offline or failed: silently does nothing, retried next time.
 */
class AccountProfileSync(private val auth: AuthRepository, private val prefs: PreferencesRepository) {

    suspend fun sync() {
        val remote = (auth.fetchProfile() as? AuthResult.Success)?.value ?: return
        val local = prefs.observeProfile().first()
        val adoptName = remote.name.isBlank() && local.name.isNotBlank()
        val adoptAge = remote.age == null && local.age != null
        val merged = UserProfile(
            name = if (adoptName) local.name else remote.name.ifBlank { local.name },
            email = remote.email,
            age = if (adoptAge) local.age else remote.age ?: local.age,
        )
        if (adoptName || adoptAge) {
            auth.updateProfile(name = if (adoptName) local.name.take(AuthValidation.MAX_NAME_LENGTH) else null, age = merged.age)
        }
        if (merged != local) prefs.saveProfile(merged)
    }
}
