package com.adhamelgendy.nixura.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.adhamelgendy.nixura.MainActivity
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route

/** What a notification tap opens: the screen of the thing it is about, never just Home (same pattern as WidgetDeepLink). */
object NotificationDeepLink {
    private const val EXTRA_KIND = "nixura.notification.kind"
    private const val EXTRA_TARGET = "nixura.notification.target"

    fun contentIntent(context: Context, key: String, kind: NotificationKind, targetId: Long?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            // A per-key data URI keeps PendingIntents for different notifications apart (extras alone do not).
            .setData(Uri.parse("nixura://notification/${Uri.encode(key)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_KIND, kind.name)
            .apply { if (targetId != null) putExtra(EXTRA_TARGET, targetId) }
        return PendingIntent.getActivity(
            context,
            NixNotifications.idOf(key),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Returns true if [intent] came from a notification tap. Extras are removed so a re-delivered intent is not applied twice. */
    fun apply(intent: Intent?, navigator: Navigator): Boolean {
        val kindName = intent?.getStringExtra(EXTRA_KIND) ?: return false
        val kind = NotificationKind.entries.firstOrNull { it.name == kindName }
        val target = intent.getLongExtra(EXTRA_TARGET, -1L).takeIf { it > 0 }
        when (kind) {
            NotificationKind.EVENT -> {
                navigator.selectTab(Route.Events)
                if (target != null) navigator.push(Route.EventForm(editId = target))
            }
            NotificationKind.GOOD_HABIT -> {
                navigator.selectTab(Route.GoodHabits)
                if (target != null) navigator.push(Route.GoodHabitDetail(target))
            }
            NotificationKind.QUIT_MILESTONE -> {
                navigator.selectTab(Route.BadHabits)
                if (target != null) navigator.push(Route.BadHabitDetail(target))
            }
            NotificationKind.COACH -> navigator.selectTab(Route.Coach)
            NotificationKind.APP, null -> Unit // just open the app
        }
        intent.removeExtra(EXTRA_KIND)
        intent.removeExtra(EXTRA_TARGET)
        return true
    }
}
