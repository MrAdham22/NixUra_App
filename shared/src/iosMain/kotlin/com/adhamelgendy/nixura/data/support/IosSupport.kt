package com.adhamelgendy.nixura.data.support

import com.adhamelgendy.nixura.domain.support.EmailDraft
import com.adhamelgendy.nixura.domain.support.SupportActions
import com.adhamelgendy.nixura.domain.support.SupportEnvironment
import com.adhamelgendy.nixura.domain.support.SupportEnvironmentProvider
import platform.Foundation.NSBundle
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDevice

// UNVERIFIED: written on Windows; compile on a Mac (:shared:compileKotlinIosSimulatorArm64).

class IosSupportEnvironment : SupportEnvironmentProvider {
    override fun current(language: String): SupportEnvironment {
        val info = NSBundle.mainBundle.infoDictionary
        val version = info?.get("CFBundleShortVersionString") as? String ?: "unknown"
        val build = info?.get("CFBundleVersion") as? String ?: "?"
        return SupportEnvironment(
            appVersion = "$version ($build)".take(40),
            platform = "ios",
            osVersion = UIDevice.currentDevice.systemVersion.take(40),
            deviceModel = UIDevice.currentDevice.model.ifBlank { "iPhone" }, // model, not the user-chosen device name
            language = language,
        )
    }
}

class IosSupportActions : SupportActions {
    override fun composeEmail(email: EmailDraft): Boolean {
        val url = NSURL.URLWithString("mailto:${email.to}?subject=${encode(email.subject)}&body=${encode(email.body)}") ?: return false
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(url)) return false
        app.openURL(url, emptyMap<Any?, Any>(), null)
        return true
    }

    /** iOS has no uninstall page an app can open; this opens NixUra's page in the Settings app instead. */
    override fun openAppSettings(): Boolean {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return false
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
        return true
    }

    // Query-allowed minus '&', '=', '+' and '?', so they cannot end the subject or body early.
    private fun encode(text: String): String {
        val allowed = NSCharacterSet.URLQueryAllowedCharacterSet.mutableCopy() as platform.Foundation.NSMutableCharacterSet
        allowed.removeCharactersInString("&=+?")
        @Suppress("CAST_NEVER_SUCCEEDS")
        return (text as NSString).stringByAddingPercentEncodingWithAllowedCharacters(allowed) ?: ""
    }
}
