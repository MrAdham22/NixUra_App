@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.example.reclaimapp.data.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.UIKit.UIDevice

/** Keychain generic-password items, readable after first unlock, never synced or backed up (this-device-only). */
class KeychainSecureStore : SecureStore {

    override suspend fun get(key: String): String? = memScoped {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE,
            kSecAttrAccount to key,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(cfQuery, result.ptr)
        CFBridgingRelease(cfQuery)
        if (status != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
    }

    override suspend fun put(key: String, value: String) {
        remove(key)
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val attrs = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE,
            kSecAttrAccount to key,
            kSecValueData to data,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        )
        val cf = CFBridgingRetain(attrs) as CFDictionaryRef
        SecItemAdd(cf, null)
        CFBridgingRelease(cf)
    }

    override suspend fun remove(key: String) {
        val query = mapOf<Any?, Any?>(kSecClass to kSecClassGenericPassword, kSecAttrService to SERVICE, kSecAttrAccount to key)
        val cf = CFBridgingRetain(query) as CFDictionaryRef
        SecItemDelete(cf)
        CFBridgingRelease(cf)
    }

    private companion object {
        const val SERVICE = "com.example.reclaimapp.secure"
    }
}

class IosDeviceInfo : DeviceInfoProvider {
    override fun deviceName(): String = UIDevice.currentDevice.model.ifBlank { "iPhone" } // model, not the user-chosen device name
    override fun platform(): String = "ios"
}

/** Base URL comes from Info.plist (`NixUraApiBaseUrl`); iOS App Transport Security already blocks cleartext. */
fun iosApiConfig(): ApiConfig {
    val url = NSBundle.mainBundle.objectForInfoDictionaryKey("NixUraApiBaseUrl") as? String
    return ApiConfig(baseUrl = url?.takeIf { it.startsWith("https://") } ?: "https://api.nixura.example", allowCleartext = false)
}
