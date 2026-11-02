package com.example.reclaimapp.presentation.auth

import androidx.compose.runtime.Composable
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route

/**
 * Renders the account flow for whatever [Route.Auth] (or Legal) route is current. Used by the first-launch gate
 * (with [onSkip]) and by Settings > Account (without). Anything else shows the Landing screen.
 */
@Composable
fun AuthHost(navigator: Navigator, onSkip: (() -> Unit)?) {
    when (val r = navigator.current) {
        Route.Auth.Login -> LoginScreen(navigator)
        Route.Auth.Register -> RegisterScreen(navigator)
        is Route.Auth.VerifyOtp -> VerifyOtpScreen(r.email, navigator)
        Route.Auth.ForgotPassword -> ForgotPasswordScreen(navigator)
        is Route.Auth.ResetPassword -> ResetPasswordScreen(r.email, navigator)
        is Route.Legal -> LegalScreen(r.document, navigator)
        else -> LandingScreen(navigator, onSkip)
    }
}
