package com.adhamelgendy.nixura.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.presentation.components.ConfirmDialog
import com.adhamelgendy.nixura.presentation.navigation.NavEntry
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.navigation.navTransition
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import org.koin.compose.viewmodel.koinViewModel

/** One account screen. [route] is passed in, so a screen sliding away keeps showing itself. Anything else shows Landing. */
@Composable
fun AuthHost(route: Route, navigator: Navigator, onGuest: (() -> Unit)?) {
    when (route) {
        Route.Auth.Login -> LoginScreen(navigator, onGuest)
        Route.Auth.Register -> RegisterScreen(navigator, onGuest)
        is Route.Auth.VerifyEmail -> VerifyEmailScreen(route.email, navigator)
        Route.Auth.ForgotPassword -> ForgotPasswordScreen(navigator)
        is Route.Legal -> LegalScreen(route.document, navigator)
        else -> LandingScreen(navigator, onGuest)
    }
}

/** The signed-out gate: Landing and the sign-in screens, with the guest path available from all of them. */
@Composable
fun AuthGate(navigator: Navigator, onEnterAsGuest: () -> Unit) {
    val s = LocalStrings.current
    val vm: GuestEntryViewModel = koinViewModel()
    val dialog by vm.dialog.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()

    AnimatedContent(
        targetState = NavEntry(navigator.current, navigator.stack.size),
        contentKey = { it.route },
        transitionSpec = { navTransition(reducedMotion) },
        label = "authRoute",
    ) { entry ->
        Box(Modifier.fillMaxSize().background(Nix.colors.background)) {
            AuthHost(entry.route, navigator, vm::onContinueAsGuest)
        }
    }

    when (dialog) {
        GuestDialog.OfflineWarning -> ConfirmDialog(
            title = s.authOfflineWarnTitle,
            message = s.authOfflineWarnBody,
            confirmText = s.authOfflineWarnConfirm,
            cancelText = s.cancel,
            onConfirm = { vm.onDismiss(); onEnterAsGuest() },
            onDismiss = vm::onDismiss,
        )
        GuestDialog.LeftoverAccountData -> ConfirmDialog(
            title = s.authLeftoverTitle,
            message = s.authLeftoverBody,
            confirmText = s.authLeftoverStartFresh,
            cancelText = s.authSignIn,
            danger = true,
            onConfirm = { vm.onStartFresh(onEnterAsGuest) },
            onDismiss = vm::onDismiss,
            onCancel = { vm.onDismiss(); navigator.switchTo(Route.Auth.Login) },
        )
        null -> Unit
    }
}
