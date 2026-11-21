package com.adhamelgendy.nixura.data.support

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.adhamelgendy.nixura.domain.support.EmailDraft
import com.adhamelgendy.nixura.domain.support.SupportActions
import com.adhamelgendy.nixura.domain.support.SupportEnvironment
import com.adhamelgendy.nixura.domain.support.SupportEnvironmentProvider

class AndroidSupportEnvironment(private val context: Context) : SupportEnvironmentProvider {
    override fun current(language: String) = SupportEnvironment(
        appVersion = appVersion(),
        platform = "android",
        osVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        deviceModel = "${Build.MANUFACTURER.orEmpty()} ${Build.MODEL.orEmpty()}".trim().take(80).ifBlank { "Android device" },
        language = language,
    )

    private fun appVersion(): String = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") pm.getPackageInfo(context.packageName, 0)
        }
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        "${info.versionName} ($code)"
    }.getOrDefault("unknown").take(40)
}

/** Starts other apps from the application context, so every intent needs NEW_TASK. */
class AndroidSupportActions(private val context: Context) : SupportActions {

    override fun composeEmail(email: EmailDraft): Boolean {
        // The address, subject and body go in the mailto: link AND the extras: some mail apps read only one of them.
        val uri = Uri.parse("mailto:${email.to}?subject=${Uri.encode(email.subject)}&body=${Uri.encode(email.body)}")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(email.to))
            .putExtra(Intent.EXTRA_SUBJECT, email.subject)
            .putExtra(Intent.EXTRA_TEXT, email.body)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(intent)
    }

    override fun openAppSettings(): Boolean = start(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
