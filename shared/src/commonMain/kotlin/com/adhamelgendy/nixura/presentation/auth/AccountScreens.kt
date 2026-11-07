package com.adhamelgendy.nixura.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.auth.AuthValidation
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.sync.SyncStatus
import com.adhamelgendy.nixura.presentation.components.Avatar
import com.adhamelgendy.nixura.presentation.components.ConfirmDialog
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.SubScreen
import com.adhamelgendy.nixura.presentation.legal.LegalBlockStyle
import com.adhamelgendy.nixura.presentation.legal.LegalContent
import com.adhamelgendy.nixura.presentation.legal.LegalDocument
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Settings > Account: who is signed in, sign in / out, password, deletion, legal links. */
@Composable
fun AccountScreen(navigator: Navigator) {
    val s = LocalStrings.current
    val c = Nix.colors
    val vm: AccountViewModel = koinViewModel()
    val user by vm.user.collectAsStateWithLifecycle()
    val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()
    val signOutState by vm.signOutState.collectAsStateWithLifecycle()
    val clock = koinInject<Clock>()
    var confirmSignOut by remember { mutableStateOf(false) }

    SubScreen(title = s.accountTitle, onBack = { navigator.back() }) {
        val u = user
        if (u == null) {
            NixCard {
                Text(s.accountSignedOutBody, style = NixType.Body, color = c.textSecondary)
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NixButton(s.authCreateAccount, { navigator.push(Route.Auth.Register) }, Modifier.fillMaxWidth())
                    NixButton(s.authSignIn, { navigator.push(Route.Auth.Login) }, Modifier.fillMaxWidth(), style = NixButtonStyle.Secondary)
                }
            }
        } else {
            NixCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u.name.ifBlank { u.email }.take(1).uppercase(), size = 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.accountSignedInAs, style = NixType.Small, color = c.textMuted)
                        Text(u.email, style = NixType.BodyStrong, color = c.textPrimary)
                    }
                }
            }
            NixCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.syncTitle, style = NixType.BodyStrong, color = c.textPrimary)
                        val failed = syncStatus is SyncStatus.Failed
                        Text(syncStatusText(syncStatus, clock.nowEpochMillis()), style = NixType.Small, color = if (failed) c.danger else c.textMuted)
                    }
                    Spacer(Modifier.width(10.dp))
                    if (syncStatus is SyncStatus.Syncing) {
                        CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        AuthLink(s.syncNow, vm::syncNow)
                    }
                }
            }
            NixCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (u.hasPassword) {
                        NixButton(s.accountChangePassword, { navigator.push(Route.ChangePassword) }, Modifier.fillMaxWidth(), style = NixButtonStyle.Secondary)
                    }
                    NixButton(s.accountSignOut, { confirmSignOut = true }, Modifier.fillMaxWidth(), style = NixButtonStyle.Secondary, loading = signOutState.inProgress)
                }
            }
            NixButton(s.accountDelete, { navigator.push(Route.DeleteAccount) }, Modifier.fillMaxWidth(), style = NixButtonStyle.Danger, icon = NixIcon.Trash)
        }
        SectionLabel(s.authPrivacyLink)
        NixCard {
            // One centered row; at large font scales the second link wraps under the first instead of clipping
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                AuthLink(s.legalPrivacyTitle, { navigator.push(Route.Legal(LegalDocument.Privacy)) }, Modifier.align(Alignment.CenterVertically))
                Text("·", style = NixType.BodyStrong, color = c.textMuted, modifier = Modifier.align(Alignment.CenterVertically))
                AuthLink(s.legalTermsTitle, { navigator.push(Route.Legal(LegalDocument.Terms)) }, Modifier.align(Alignment.CenterVertically))
            }
        }
    }
    SignOutDialogs(vm, confirmSignOut, onCloseConfirm = { confirmSignOut = false })
}

/** The sign-out confirmation and the "some changes aren't saved" warning, shared by the account sheet and screen. */
@Composable
fun SignOutDialogs(vm: AccountViewModel, confirmSignOut: Boolean, onCloseConfirm: () -> Unit) {
    val s = LocalStrings.current
    val signOutState by vm.signOutState.collectAsStateWithLifecycle()
    if (confirmSignOut) {
        ConfirmDialog(
            title = s.accountSignOutTitle, message = s.accountSignOutBody, confirmText = s.accountSignOut, cancelText = s.cancel,
            onConfirm = { onCloseConfirm(); vm.signOut() }, onDismiss = onCloseConfirm,
        )
    }
    if (signOutState.changesNotUploaded) {
        ConfirmDialog(
            title = s.accountSignOutUnsavedTitle, message = s.accountSignOutUnsavedBody, confirmText = s.accountSignOutAnyway, cancelText = s.cancel,
            danger = true,
            onConfirm = { vm.signOut(discardUnsaved = true) }, onDismiss = vm::dismissSignOutWarning,
        )
    }
}

@Composable
internal fun syncStatusText(status: SyncStatus, nowEpochMillis: Long): String {
    val s = LocalStrings.current
    val last = status.lastSyncedAt
    val since = when {
        last == null -> s.syncNeverYet
        else -> {
            val minutes = (nowEpochMillis - last).coerceAtLeast(0) / 60_000
            when {
                minutes < 1 -> s.syncJustNow
                minutes < 60 -> s.syncMinutesAgo(minutes)
                minutes < 24 * 60 -> s.syncHoursAgo(minutes / 60)
                else -> s.syncDaysAgo(minutes / (24 * 60))
            }
        }
    }
    return when (status) {
        is SyncStatus.Syncing -> s.syncInProgress
        is SyncStatus.Offline -> s.syncOffline(since)
        is SyncStatus.Failed -> s.syncFailed(since)
        is SyncStatus.Idle -> since
    }
}

@Composable
fun ChangePasswordScreen(navigator: Navigator) {
    val s = LocalStrings.current
    val vm: ChangePasswordViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        vm.dismissError()
        vm.done.collect { finished = true }
    }
    val problems = AuthValidation.passwordProblems(new)
    val submit = { submitted = true; vm.change(current, new) }

    SubScreen(title = s.cpTitle, onBack = { navigator.back() }) {
        if (finished) {
            Text(s.cpDone, style = NixType.Body, color = Nix.colors.positive)
            NixButton(s.authBack, { navigator.back() }, Modifier.fillMaxWidth())
            return@SubScreen
        }
        AuthTextField(current, { current = it }, s.authCurrentPasswordLabel, isPassword = true, autofill = AutofillKind.Password)
        AuthTextField(
            new, { new = it }, s.authNewPasswordLabel, isPassword = true, autofill = AutofillKind.NewPassword,
            imeAction = ImeAction.Done, onImeAction = submit,
            error = if ((submitted || new.length >= 4) && problems.isNotEmpty()) passwordProblemText(problems.first(), s) else null,
        )
        PasswordStrengthBar(new, "")
        AuthErrorText(state.error)
        NixButton(s.cpButton, submit, Modifier.fillMaxWidth(), loading = state.loading)
    }
}

@Composable
fun DeleteAccountScreen(navigator: Navigator) {
    val s = LocalStrings.current
    val vm: DeleteAccountViewModel = koinViewModel()
    val account: AccountViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val user by account.user.collectAsStateWithLifecycle()
    // Accounts that only sign in with Google or Facebook have no password to confirm with.
    val needsPassword = user?.hasPassword != false
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.dismissError() }

    SubScreen(title = s.delTitle, onBack = { navigator.back() }) {
        NixCard {
            Text(s.delWarn, style = NixType.Body, color = Nix.colors.textSecondary)
        }
        if (needsPassword) {
            AuthTextField(password, { password = it }, s.delPasswordLabel, isPassword = true, autofill = AutofillKind.Password)
        } else {
            Text(s.delNoPasswordHint, style = NixType.Small, color = Nix.colors.textMuted)
        }
        AuthErrorText(state.error)
        NixButton(
            s.delButton, { confirm = true }, Modifier.fillMaxWidth(),
            style = NixButtonStyle.Danger, icon = NixIcon.Trash, enabled = !needsPassword || password.isNotEmpty(), loading = state.loading,
        )
    }
    if (confirm) {
        ConfirmDialog(
            title = s.delConfirmTitle, message = s.delConfirmBody, confirmText = s.delButton, cancelText = s.cancel, danger = true,
            onConfirm = { confirm = false; vm.delete(if (needsPassword) password else null) }, onDismiss = { confirm = false },
        )
    }
}

/** In-app Privacy Policy / Terms, generated from the markdown in docs/legal (see tools/gen_legal_text.py). */
@Composable
fun LegalScreen(document: LegalDocument, navigator: Navigator) {
    val s = LocalStrings.current
    val c = Nix.colors
    val (title, blocks) = when (document) {
        LegalDocument.Privacy -> s.legalPrivacyTitle to LegalContent.privacy
        LegalDocument.Terms -> s.legalTermsTitle to LegalContent.terms
    }
    SubScreen(title = title, onBack = { navigator.back() }) {
        for (b in blocks) when (b.style) {
            LegalBlockStyle.Title -> Unit // the screen header already shows it
            LegalBlockStyle.Heading -> Text(b.text, style = NixType.Heading, color = c.textPrimary)
            LegalBlockStyle.Paragraph -> Text(b.text, style = NixType.Body, color = c.textSecondary)
            LegalBlockStyle.Bullet -> Text("\u2022  " + b.text, style = NixType.Body, color = c.textSecondary)
            LegalBlockStyle.Note -> Text(b.text, style = NixType.Small, color = c.textMuted)
        }
    }
}
