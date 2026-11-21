package com.adhamelgendy.nixura.presentation.goodhabits.reminders

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.notification.NotificationAccess
import com.adhamelgendy.nixura.domain.notification.NotificationPermissionGateway
import com.adhamelgendy.nixura.domain.notification.ReminderCapabilities
import com.adhamelgendy.nixura.domain.reminder.LocationAccess
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.NixSwitch
import com.adhamelgendy.nixura.presentation.components.Text
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The create form's one line for prayer and water habits (PLAN.md): "Reminders on, change anytime" with a switch.
 * The card itself is the reason, so its buttons go straight to the system dialogs: notifications (Android 13+) and,
 * for prayer, the city-level location (the only place besides the habit's sheet that asks for it).
 */
@Composable
fun ReminderFormCard(prayer: Boolean, on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val c = Nix.colors
    val permission = koinInject<NotificationPermissionGateway>()
    val capabilities = koinInject<ReminderCapabilities>()
    val vm: HabitRemindersViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    // Re-read what the OS allows when the user comes back from a system dialog or settings.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
    val access = remember(resumeTick) { permission.access() }
    val locationAccess = remember(resumeTick, state.location) { vm.locationAccess() }

    NixCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NixIconView(NixIcon.Bell, c.accent, size = 22.dp)
            Spacer(Modifier.width(12.dp))
            Text(s.reminderFormLine, style = NixType.BodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
            NixSwitch(on, onChange)
        }
        Spacer(Modifier.height(6.dp))
        Text(if (prayer) s.reminderFormHintPrayer else s.reminderFormHintWater, style = NixType.Small, color = c.textMuted)
        if (!on) return@NixCard

        when (access) {
            NotificationAccess.ALLOWED -> Unit
            NotificationAccess.ASK -> {
                Spacer(Modifier.height(12.dp))
                NixButton(s.reminderAllowNotifications, permission::request, style = NixButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            }
            NotificationAccess.OPEN_SETTINGS -> {
                Spacer(Modifier.height(12.dp))
                Text(s.habitReminderNotificationsOff, style = NixType.Small, color = c.accentWarm)
                Spacer(Modifier.height(8.dp))
                NixButton(s.reminderOpenSettings, capabilities::openNotificationSettings, style = NixButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            }
        }

        if (prayer && state.device.location == null && locationAccess != LocationAccess.UNAVAILABLE) {
            Spacer(Modifier.height(12.dp))
            Text(
                when (state.location) {
                    LocationReading.READING -> s.locationReading
                    LocationReading.FAILED -> s.locationFailed
                    LocationReading.IDLE -> s.locationBody
                },
                style = NixType.Small,
                color = if (state.location == LocationReading.FAILED) c.accentWarm else c.textMuted,
            )
            Spacer(Modifier.height(8.dp))
            NixButton(
                text = if (locationAccess == LocationAccess.OPEN_SETTINGS) s.reminderOpenSettings else s.locationUse,
                onClick = vm::useLocation,
                style = NixButtonStyle.Secondary,
                loading = state.location == LocationReading.READING,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
