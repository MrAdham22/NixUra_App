package com.example.reclaimapp.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.auth.AuthErrorKind
import com.example.reclaimapp.domain.auth.AuthValidation
import com.example.reclaimapp.domain.auth.EmailProblem
import com.example.reclaimapp.presentation.legal.LegalDocument
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.strings.Strings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

private fun emailErrorText(email: String, s: Strings): String? = when (AuthValidation.emailProblem(email)) {
    null -> null
    EmailProblem.Empty -> s.authEmailEmpty
    else -> s.authEmailInvalid
}

@Composable
fun LoginScreen(navigator: Navigator) {
    val s = LocalStrings.current
    val vm: LoginViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // never saved to instance state
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.dismissError() }
    // The server already mailed a fresh code when the account exists but is unverified.
    LaunchedEffect(state.error) {
        if (state.error?.kind == AuthErrorKind.EmailNotVerified) {
            vm.dismissError()
            navigator.push(Route.Auth.VerifyOtp(AuthValidation.normalizeEmail(email)))
        }
    }
    val submit = { submitted = true; vm.login(email, password) }

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
        AuthGradientButton(s.authSignIn, submit, Modifier.fillMaxWidth(), loading = state.loading)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            Text(s.authNewHere, style = NixType.Body, color = Nix.colors.textSecondary)
            AuthLink(s.authCreateAccount, { navigator.push(Route.Auth.Register) })
        }
    }
}

@Composable
fun RegisterScreen(navigator: Navigator) {
    val s = LocalStrings.current
    val vm: RegisterViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.dismissError()
        vm.done.collect { navigator.push(Route.Auth.VerifyOtp(AuthValidation.normalizeEmail(email))) }
    }
    val problems = AuthValidation.passwordProblems(password, email)
    val submit = { submitted = true; vm.register(email, password, name) }

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
        AuthGradientButton(s.authCreateAccount, submit, Modifier.fillMaxWidth(), loading = state.loading)
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
            AuthLink(s.authSignIn, { navigator.push(Route.Auth.Login) })
        }
    }
}

@Composable
fun VerifyOtpScreen(email: String, navigator: Navigator) {
    val s = LocalStrings.current
    val vm: VerifyOtpViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val seconds by vm.resendSeconds.collectAsStateWithLifecycle()
    val resent by vm.resentNotice.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.dismissError() }
    // On success the session becomes SignedIn and the root gate takes over; nothing to navigate here.
    AuthScaffold(s.authVerifyTitle, s.authVerifySubtitle(email), onBack = { navigator.back() }) {
        OtpInput(
            code, { code = it; if (state.error != null) vm.dismissError() }, onComplete = { vm.verify(email, code) },
            enabled = !state.loading, hasError = state.error != null,
        )
        AuthErrorText(state.error)
        if (resent) Text(s.authCodeResent, style = NixType.Small, color = Nix.colors.positive)
        AuthGradientButton(s.authVerifyButton, { vm.verify(email, code) }, Modifier.fillMaxWidth(), loading = state.loading, enabled = AuthValidation.isCompleteOtp(code))
        AuthLink(if (seconds > 0) s.authResendIn(seconds) else s.authResend, { vm.resend(email) }, enabled = seconds == 0)
    }
}

@Composable
fun ForgotPasswordScreen(navigator: Navigator) {
    val s = LocalStrings.current
    val vm: ForgotPasswordViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.dismissError()
        vm.done.collect { navigator.push(Route.Auth.ResetPassword(AuthValidation.normalizeEmail(email))) }
    }
    val submit = { submitted = true; vm.send(email) }

    AuthScaffold(s.authForgotTitle, s.authForgotSubtitle, onBack = { navigator.back() }) {
        AuthTextField(
            email, { email = it }, s.authEmailLabel, keyboardType = KeyboardType.Email, autofill = AutofillKind.Email,
            imeAction = ImeAction.Done, onImeAction = submit, error = if (submitted) emailErrorText(email, s) else null,
        )
        AuthErrorText(state.error)
        AuthGradientButton(s.authSendCode, submit, Modifier.fillMaxWidth(), loading = state.loading)
    }
}

@Composable
fun ResetPasswordScreen(email: String, navigator: Navigator) {
    val s = LocalStrings.current
    val vm: ResetPasswordViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.dismissError()
        vm.done.collect { finished = true }
    }
    val problems = AuthValidation.passwordProblems(password, email)
    val submit = { submitted = true; vm.reset(email, code, password) }

    if (finished) {
        AuthScaffold(s.authResetButton, s.authResetDone, onBack = null) {
            // Back to the Login screen that opened "Forgot password" (Login > Forgot > Reset).
            AuthGradientButton(s.authSignIn, { repeat(2) { navigator.back() } }, Modifier.fillMaxWidth())
        }
        return
    }
    AuthScaffold(s.authResetTitle, s.authResetSubtitle(email), onBack = { navigator.back() }) {
        OtpInput(code, { code = it }, onComplete = {}, enabled = !state.loading, hasError = state.error?.kind == AuthErrorKind.InvalidCode)
        AuthTextField(
            password, { password = it }, s.authNewPasswordLabel, isPassword = true, autofill = AutofillKind.NewPassword,
            imeAction = ImeAction.Done, onImeAction = submit,
            error = if ((submitted || password.length >= 4) && problems.isNotEmpty()) passwordProblemText(problems.first(), s) else null,
        )
        PasswordStrengthBar(password, email)
        AuthErrorText(state.error)
        AuthGradientButton(s.authResetButton, submit, Modifier.fillMaxWidth(), loading = state.loading, enabled = AuthValidation.isCompleteOtp(code))
        AuthLink(s.authResend, { vm.resend(email) })
        Spacer(Modifier.height(8.dp))
    }
}
