package com.adhamelgendy.nixura.presentation.auth

import com.adhamelgendy.nixura.domain.sync.CloudSync
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.adhamelgendy.nixura.presentation.components.NixIconView
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NixIconView(NixIcon.User, c.accent, size = 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.accountGuestName, style = NixType.BodyStrong, color = c.textPrimary)
                        Text(s.accountSignedOutBody, style = NixType.Small, color = c.textMuted)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NixButton(s.authCreateAccount, { navigator.push(Route.Auth.Register) }, Modifier.fillMaxWidth())
                    NixButton(s.authSignIn, { navigator.push(Route.Auth.Login) }, Modifier.fillMaxWidth(), style = NixButtonStyle.Secondary)
                }
            }
        } else {
            // Who is signed in.
            NixCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u.name.ifBlank { u.email }.take(1).uppercase(), size = 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.accountSignedInAs, style = NixType.Small, color = c.textMuted)
                        Text(u.email, style = NixType.BodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            SectionLabel(s.syncTitle)
            NixCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NixIconView(NixIcon.Reset, c.accent, size = 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.syncTitle, style = NixType.BodyStrong, color = c.textPrimary)
                        if (CloudSync.AVAILABLE) {
                            val failed = syncStatus is SyncStatus.Failed
                            Text(syncStatusText(syncStatus, clock.nowEpochMillis()), style = NixType.Small, color = if (failed) c.danger else c.textMuted)
                        } else {
                            Text(s.syncComingSoon, style = NixType.Small, color = c.textMuted)
                        }
                    }
                    if (CloudSync.AVAILABLE) {
                        Spacer(Modifier.width(8.dp))
                        if (syncStatus is SyncStatus.Syncing) {
                            CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            AuthLink(s.syncNow, vm::syncNow)
                        }
                    }
                }
            }

            SectionLabel(s.accountSecurity)
            if (u.hasPassword) {
                NixCard(onClick = { navigator.push(Route.ChangePassword) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NixIconView(NixIcon.Lock, c.accent, size = 22.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.accountChangePassword, style = NixType.BodyStrong, color = c.textPrimary)
                            Text(s.accountChangePasswordHint, style = NixType.Small, color = c.textMuted)
                        }
                        Spacer(Modifier.width(8.dp))
                        NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
                    }
                }
            }
            NixCard(onClick = if (signOutState.inProgress) null else ({ confirmSignOut = true })) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NixIconView(NixIcon.Back, c.accent, size = 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.accountSignOut, style = NixType.BodyStrong, color = c.textPrimary)
                        Text(s.accountSignOutHint, style = NixType.Small, color = c.textMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (signOutState.inProgress) {
                        CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
                    }
                }
            }
        }

        SectionLabel(s.accountLegal)
        NixCard(onClick = { navigator.push(Route.Legal(LegalDocument.Privacy)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Shield, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.legalPrivacyTitle, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.legalPrivacyHint, style = NixType.Small, color = c.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
            }
        }
        NixCard(onClick = { navigator.push(Route.Legal(LegalDocument.Terms)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Info, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.legalTermsTitle, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.legalTermsHint, style = NixType.Small, color = c.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
            }
        }

        if (user != null) {
            // Last and in the danger colour: the one action here that cannot be undone.
            SectionLabel(s.accountDangerZone)
            NixCard(onClick = { navigator.push(Route.DeleteAccount) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NixIconView(NixIcon.Trash, c.danger, size = 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.accountDelete, style = NixType.BodyStrong, color = c.danger)
                        Text(s.accountDeleteHint, style = NixType.Small, color = c.textMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
                }
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
