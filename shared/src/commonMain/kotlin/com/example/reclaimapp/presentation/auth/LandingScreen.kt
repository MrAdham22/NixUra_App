package com.example.reclaimapp.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reclaimapp.presentation.components.ConfirmDialog
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.theme.NixUraPalette

private val TaglineStyle = TextStyle(fontSize = 25.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)

/**
 * The first screen when signed out: brand, the "Let's Nix the bad Aura" line whose last word blows away as ash, and
 * Get Started. [onSkip] is the guest path, which warns first because guest data never leaves the phone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingScreen(navigator: Navigator, onSkip: (() -> Unit)?) {
    val s = LocalStrings.current
    val c = Nix.colors
    var warnOffline by rememberSaveable { mutableStateOf(false) }
    val titleBrush = remember { Brush.horizontalGradient(listOf(NixUraPalette.Blush, NixUraPalette.Orange)) }
    val auraBrush = remember { Brush.horizontalGradient(listOf(NixUraPalette.Orange, NixUraPalette.Rose)) }

    Box(Modifier.fillMaxSize()) {
        AuraBackdrop()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            BrandMark(Modifier.riseIn(0), size = 72.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                "NixUra",
                style = TextStyle(fontSize = 58.sp, lineHeight = 64.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.8).sp, brush = titleBrush),
                modifier = Modifier.riseIn(120).semantics { heading() },
            )
            Spacer(Modifier.height(14.dp))
            val fullLine = "${s.landingTagline} ${s.landingAuraWord}"
            FlowRow(
                Modifier.fillMaxWidth().riseIn(260).clearAndSetSemantics { contentDescription = fullLine },
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            ) {
                Text(s.landingTagline, style = TaglineStyle, color = c.textPrimary)
                AshWord(s.landingAuraWord, TaglineStyle, auraBrush)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                s.landingBody,
                style = NixType.Body,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.riseIn(400),
            )
            Spacer(Modifier.weight(1.2f))
            AuthGradientButton(
                s.landingGetStarted,
                { navigator.push(Route.Auth.Login) },
                Modifier.fillMaxWidth().riseIn(540),
                trailingIcon = NixIcon.Chevron,
            )
            if (onSkip != null) {
                Spacer(Modifier.height(6.dp))
                AuthLink(
                    s.authContinueOffline,
                    { warnOffline = true },
                    Modifier.riseIn(660),
                    color = c.textSecondary,
                )
            }
        }
    }

    if (warnOffline && onSkip != null) {
        ConfirmDialog(
            title = s.authOfflineWarnTitle,
            message = s.authOfflineWarnBody,
            confirmText = s.authOfflineWarnConfirm,
            cancelText = s.cancel,
            onConfirm = { warnOffline = false; onSkip() },
            onDismiss = { warnOffline = false },
        )
    }
}
