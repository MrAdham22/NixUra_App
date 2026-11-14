package com.adhamelgendy.nixura.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import com.adhamelgendy.nixura.presentation.components.pressable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.auth.AuthErrorKind
import com.adhamelgendy.nixura.domain.auth.SocialProvider
import com.adhamelgendy.nixura.domain.auth.AuthValidation
import com.adhamelgendy.nixura.domain.auth.EmailProblem
import com.adhamelgendy.nixura.presentation.legal.LegalDocument
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.Strings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

private fun emailErrorText(email: String, s: Strings): String? = when (AuthValidation.emailProblem(email)) {
    null -> null
    EmailProblem.Empty -> s.authEmailEmpty
    else -> s.authEmailInvalid
}

@Composable
fun LoginScreen(navigator: Navigator, onGuest: (() -> Unit)? = null) {
    val s = LocalStrings.current
    val vm: LoginViewModel = koinViewModel()
    val social: SocialSignInViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val socialState by social.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // never saved to instance state
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.dismissError(); social.dismissError() }
    // Firebase has just mailed a fresh verification link: the account exists but its email is not confirmed.
    LaunchedEffect(state.error) {
        if (state.error?.kind == AuthErrorKind.EmailNotVerified) {
            vm.dismissError()
            navigator.push(Route.Auth.VerifyEmail(AuthValidation.normalizeEmail(email)))
        }
    }
    val submit = { submitted = true; social.dismissError(); vm.login(email, password) }

    AuthScaffold(s.authLoginTitle, s.authLoginSubtitle, onBack = { navigator.back() }) {
        AuthTextField(
            email, { email = it }, s.authEmailLabel, keyboardType = KeyboardType.Email, autofill = AutofillKind.Email,
            error = if (submitted) emailErrorText(email, s) else null,
        )
        AuthTextField(
            password, { password = it }, s.authPasswordLabel, isPassword = true, autofill = AutofillKind.Password,
            imeAction = ImeAction.Done, onImeAction = submit,
            error = if (submitted && password.isEmpty()) s.authPasswordRequired else null,
        )
        AuthLink(s.authForgotLink, { navigator.push(Route.Auth.ForgotPassword) }, Modifier.align(Alignment.End))
        AuthErrorText(state.error)
        AuthPrimaryButton(s.authSignIn, submit, Modifier.fillMaxWidth(), loading = state.loading, enabled = !socialState.loading)
        SocialSignInButtons(social, formBusy = state.loading, onStart = vm::dismissError)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            Text(s.authNewHere, style = NixType.Body, color = Nix.colors.textSecondary)
            AuthLink(s.authCreateAccount, { navigator.switchTo(Route.Auth.Register) })
        }
        if (onGuest != null) {
            AuthLink(s.authContinueOffline, onGuest, Modifier.align(Alignment.CenterHorizontally), color = Nix.colors.textSecondary)
        }
    }
}

@Composable
fun RegisterScreen(navigator: Navigator, onGuest: (() -> Unit)? = null) {
    val s = LocalStrings.current
    val vm: RegisterViewModel = koinViewModel()
    val social: SocialSignInViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val socialState by social.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.dismissError()
        social.dismissError()
        vm.done.collect { navigator.push(Route.Auth.VerifyEmail(AuthValidation.normalizeEmail(email))) }
    }
    val problems = AuthValidation.passwordProblems(password, email)
    val submit = { submitted = true; social.dismissError(); vm.register(email, password, name) }

    AuthScaffold(s.authRegisterTitle, s.authRegisterSubtitle, onBack = { navigator.back() }) {
        AuthTextField(name, { name = it }, s.nameLabel, maxLength = AuthValidation.MAX_NAME_LENGTH, autofill = AutofillKind.Username)
        AuthTextField(
            email, { email = it }, s.authEmailLabel, keyboardType = KeyboardType.Email, autofill = AutofillKind.Email,
            error = if (submitted) emailErrorText(email, s) else null,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthTextField(
                password, { password = it }, s.authPasswordLabel, isPassword = true, autofill = AutofillKind.NewPassword,
                imeAction = ImeAction.Done, onImeAction = submit,
                error = if ((submitted || password.length >= 4) && problems.isNotEmpty()) passwordProblemText(problems.first(), s) else null,
            )
            if (problems.isEmpty() || password.isEmpty()) Text(s.authPasswordHint, style = NixType.Small, color = Nix.colors.textMuted)
            PasswordStrengthBar(password, email)
        }
        AuthErrorText(state.error)
        AuthPrimaryButton(s.authCreateAccount, submit, Modifier.fillMaxWidth(), loading = state.loading, enabled = !socialState.loading)
        SocialSignInButtons(social, formBusy = state.loading, onStart = vm::dismissError)
        Column {
            Text(s.authAgreeText, style = NixType.Small, color = Nix.colors.textMuted)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AuthLink(s.authTermsLink, { navigator.push(Route.Legal(LegalDocument.Terms)) })
                Text(s.authAnd, style = NixType.Small, color = Nix.colors.textMuted)
                AuthLink(s.authPrivacyLink, { navigator.push(Route.Legal(LegalDocument.Privacy)) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.authHaveAccount, style = NixType.Body, color = Nix.colors.textSecondary)
            AuthLink(s.authSignIn, { navigator.switchTo(Route.Auth.Login) })
        }
        if (onGuest != null) {
            AuthLink(s.authContinueOffline, onGuest, Modifier.align(Alignment.CenterHorizontally), color = Nix.colors.textSecondary)
        }
    }
}

@Composable
fun VerifyEmailScreen(email: String, navigator: Navigator) {
    val s = LocalStrings.current
    val vm: VerifyEmailViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val seconds by vm.resendSeconds.collectAsStateWithLifecycle()
    val resent by vm.resentNotice.collectAsStateWithLifecycle()

    // Watch for the tapped link while this screen is shown; on success the session becomes SignedIn and the root
    // gate takes over, so there is nothing to navigate here.
    DisposableEffect(email) {
        vm.dismissError()
        vm.watch(email)
        onDispose { vm.stopWatching() }
    }
    AuthScaffold(s.authVerifyTitle, s.authVerifySubtitle(email), onBack = { navigator.back() }) {
        AuthErrorText(state.error)
        if (resent) Text(s.authLinkResent, style = NixType.Small, color = Nix.colors.positive)
        AuthPrimaryButton(s.authVerifyButton, { vm.check(email) }, Modifier.fillMaxWidth(), loading = state.loading)
        AuthLink(if (seconds > 0) s.authResendIn(seconds) else s.authResend, { vm.resend(email) }, enabled = seconds == 0)
        Text(s.authVerifyCheckSpam, style = NixType.Small, color = Nix.colors.textMuted)
    }
}

@Composable
fun ForgotPasswordScreen(navigator: Navigator) {
    val s = LocalStrings.current
    val vm: ForgotPasswordViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var sent by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.dismissError()
        vm.done.collect { sent = true }
    }
    val submit = { submitted = true; vm.send(email) }

    if (sent) {
        // The link opens Firebase's page to choose the new password; the user then signs in on the Login screen.
        AuthScaffold(s.authForgotSentTitle, s.authForgotSent(AuthValidation.normalizeEmail(email)), onBack = { navigator.back() }) {
            AuthPrimaryButton(s.authSignIn, { navigator.back() }, Modifier.fillMaxWidth())
        }
        return
    }
    AuthScaffold(s.authForgotTitle, s.authForgotSubtitle, onBack = { navigator.back() }) {
        AuthTextField(
            email, { email = it }, s.authEmailLabel, keyboardType = KeyboardType.Email, autofill = AutofillKind.Email,
            imeAction = ImeAction.Done, onImeAction = submit, error = if (submitted) emailErrorText(email, s) else null,
        )
        AuthErrorText(state.error)
        AuthPrimaryButton(s.authSendLink, submit, Modifier.fillMaxWidth(), loading = state.loading)
    }
}

/**
 * "or" plus one button per sign-in method this phone offers (none: nothing is shown). Its error sits above its
 * buttons, like the form's error above the main button; [onStart] clears the form's error, and the buttons wait
 * while the form is [formBusy].
 */
@Composable
private fun SocialSignInButtons(vm: SocialSignInViewModel, formBusy: Boolean, onStart: () -> Unit) {
    val s = LocalStrings.current
    val state by vm.state.collectAsStateWithLifecycle()
    if (vm.providers.isEmpty()) return
    var running by remember { mutableStateOf<SocialProvider?>(null) }
    LaunchedEffect(state.loading) { if (!state.loading) running = null }

    // Decorative: the buttons below already say what they do.
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(Nix.colors.border))
        Text(s.authOrDivider, style = NixType.Small, color = Nix.colors.textMuted)
        Box(Modifier.weight(1f).height(1.dp).background(Nix.colors.border))
    }
    AuthErrorText(state.error)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        vm.providers.forEach { provider ->
            SocialLogoButton(
                provider = provider,
                label = when (provider) {
                    SocialProvider.Google -> s.authContinueWithGoogle
                    SocialProvider.Facebook -> s.authContinueWithFacebook
                },
                onClick = { running = provider; onStart(); vm.signIn(provider) },
                modifier = Modifier.weight(1f),
                loading = state.loading && running == provider,
                enabled = !state.loading && !formBusy,
            )
        }
    }
}

/**
 * One provider's logo on a button shaped like the app's secondary button (same height, corners, surface and border).
 * [label] ("Continue with Google") is what screen readers say, since the button shows no text.
 */
@Composable
private fun SocialLogoButton(
    provider: SocialProvider,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Nix.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .pressable(enabled = enabled && !loading, onClick = onClick)
            .height(50.dp)
            .clip(shape)
            .background(if (enabled) c.surfaceRaised else c.surfaceRaised.copy(alpha = c.surfaceRaised.alpha * 0.45f), shape)
            .border(1.dp, c.border, shape)
            .clearAndSetSemantics { contentDescription = label; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = c.textPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            SocialLogo(provider, Modifier.alpha(if (enabled) 1f else 0.5f))
        }
    }
}
