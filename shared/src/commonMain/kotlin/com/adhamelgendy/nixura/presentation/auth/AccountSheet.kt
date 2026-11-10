package com.adhamelgendy.nixura.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.sync.SyncStatus
import com.adhamelgendy.nixura.presentation.components.Avatar
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.NixSheet
import com.adhamelgendy.nixura.presentation.components.pressable
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Opened from the avatar at the top of every tab: who you are, plus sign in or sign out in one tap, so nobody has to dig
 * through Profile and Settings for them.
 */
@Composable
fun AccountSheet(displayName: String, navigator: Navigator, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    val vm: AccountViewModel = koinViewModel()
    val user by vm.user.collectAsStateWithLifecycle()
    val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()
    val clock = koinInject<Clock>()
    var confirmSignOut by remember { mutableStateOf(false) }
    val u = user

    fun open(route: Route) {
        onDismiss()
        navigator.push(route)
    }

    NixSheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(displayName.ifBlank { u?.email ?: "N" }, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(displayName.ifBlank { if (u == null) s.accountGuestName else s.profileNoName }, style = NixType.Heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    u?.email ?: s.accountGuestBody,
                    style = NixType.Small,
                    color = c.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (u != null) {
                    val failed = syncStatus is SyncStatus.Failed
                    Text(syncStatusText(syncStatus, clock.nowEpochMillis()), style = NixType.Small, color = if (failed) c.danger else c.textMuted, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (u == null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NixButton(s.authSignIn, { open(Route.Auth.Login) }, Modifier.fillMaxWidth())
                NixButton(s.authCreateAccount, { open(Route.Auth.Register) }, Modifier.fillMaxWidth(), style = NixButtonStyle.Secondary)
            }
            Spacer(Modifier.height(14.dp))
        }
        // Profile and Settings come back here: pressing Back on them shows this sheet again. Account lives in Settings.
        SheetRow(NixIcon.Leaf, s.profileTitle) { onDismiss(); navigator.pushFromAccountSheet(Route.Profile) }
        SheetRow(NixIcon.Gear, s.settingsTitle) { onDismiss(); navigator.pushFromAccountSheet(Route.Settings) }
        if (u != null) {
            SheetRow(NixIcon.Back, s.accountSignOut, color = c.danger) { confirmSignOut = true }
        }
    }
    SignOutDialogs(vm, confirmSignOut, onCloseConfirm = { confirmSignOut = false })
}

@Composable
private fun SheetRow(icon: NixIcon, label: String, color: Color = Nix.colors.textPrimary, onClick: () -> Unit) {
    val c = Nix.colors
    Row(
        Modifier.fillMaxWidth().pressable(pressedScale = 0.98f, onClick = onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NixIconView(icon, if (color == c.textPrimary) c.textSecondary else color, size = 20.dp)
        Spacer(Modifier.width(14.dp))
        Text(label, style = NixType.Body, color = color, modifier = Modifier.weight(1f))
        NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
    }
}
