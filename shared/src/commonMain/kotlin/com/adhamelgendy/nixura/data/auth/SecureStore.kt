package com.adhamelgendy.nixura.data.auth

/**
 * Small encrypted key-value store for tokens and account flags.
 * Android: AES-256-GCM with a non-exportable Android Keystore key. iOS: Keychain (this-device-only).
 * Values never leave the device and are excluded from backups.
 */
interface SecureStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)
}
