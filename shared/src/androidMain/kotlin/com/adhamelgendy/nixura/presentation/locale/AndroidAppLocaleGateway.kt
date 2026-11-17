package com.adhamelgendy.nixura.presentation.locale

import com.adhamelgendy.nixura.domain.locale.AppLocales
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Android per-app language. On Android 13+ AppCompat hands the choice to the system LocaleManager, so NixUra's
 * language also shows (and can be changed) in Settings → Apps → NixUra → Language. Older Android keeps no system
 * record of it; the app still switches, because the UI reads its own saved choice (MainActivity is a
 * ComponentActivity, which AppCompat's pre-13 locale override does not apply to, and nothing needs it to).
 * MainActivity declares `configChanges="locale|layoutDirection"`, so applying a language never recreates the screen.
 */
class AndroidAppLocaleGateway : AppLocaleGateway {
    private var lastApplied: String? = null

    override fun apply(choice: String) {
        val tag = AppLocales.osTag(choice)
        lastApplied = tag.orEmpty()
        val run = Runnable {
            val wanted = if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != wanted.toLanguageTags()) {
                AppCompatDelegate.setApplicationLocales(wanted)
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) run.run() else Handler(Looper.getMainLooper()).post(run)
    }

    override fun externalChoice(): String? {
        if (Build.VERSION.SDK_INT < 33) return null
        val applied = lastApplied ?: return null
        val now = AppLocales.osTag(AppCompatDelegate.getApplicationLocales().toLanguageTags()).orEmpty()
        if (now == applied) return null
        lastApplied = now
        return AppLocales.choiceFromOs(now)
    }
}
