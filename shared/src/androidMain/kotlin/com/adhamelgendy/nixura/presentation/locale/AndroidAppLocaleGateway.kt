package com.adhamelgendy.nixura.presentation.locale

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.adhamelgendy.nixura.domain.locale.AppLocales

/**
 * Android per-app language. On Android 13+ the choice is handed to the system [LocaleManager], so NixUra's language
 * also shows (and can be changed) in Settings → Apps → NixUra → Language (`res/xml/locales_config.xml`).
 *
 * Why not `AppCompatDelegate.setApplicationLocales`: it only reaches the system through a live AppCompatActivity, and
 * MainActivity is a ComponentActivity (Compose), so it silently did nothing (checked on an Android 16 emulator,
 * `cmd locale get-app-locales` stayed empty). Before Android 13 there is no system record of a per-app language; the
 * app still switches, because the UI reads its own saved choice.
 *
 * MainActivity declares `configChanges="locale|layoutDirection"`, so applying a language never recreates the screen.
 */
class AndroidAppLocaleGateway(private val context: Context) : AppLocaleGateway {
    private var lastApplied: String? = null

    override fun apply(choice: String) {
        val tag = AppLocales.osTag(choice).orEmpty()
        lastApplied = tag
        if (Build.VERSION.SDK_INT < 33) return
        val manager = context.getSystemService(LocaleManager::class.java) ?: return
        if (manager.applicationLocales.toLanguageTags() != tag) {
            manager.applicationLocales = LocaleList.forLanguageTags(tag)
        }
    }

    override fun externalChoice(): String? {
        if (Build.VERSION.SDK_INT < 33) return null
        val applied = lastApplied ?: return null
        val manager = context.getSystemService(LocaleManager::class.java) ?: return null
        val now = AppLocales.osTag(manager.applicationLocales.toLanguageTags()).orEmpty()
        if (now == applied) return null
        lastApplied = now
        return AppLocales.choiceFromOs(now)
    }
}
