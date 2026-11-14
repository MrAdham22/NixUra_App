package com.adhamelgendy.nixura.presentation.auth

import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixButton
import nixura.shared.generated.resources.nixura_icon
import nixura.shared.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
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
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.theme.NixUraPalette

private val TaglineStyle = TextStyle(fontSize = 25.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)

/**
 * The first screen when signed out: brand, the "Let's Nix the bad Aura" line whose last word blows away as ash, and
 * Get Started. [onGuest] is the guest path; [AuthGate] handles its warnings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingScreen(navigator: Navigator, onGuest: (() -> Unit)?) {
    val s = LocalStrings.current
    val c = Nix.colors
    val titleBrush = remember { Brush.horizontalGradient(listOf(NixUraPalette.Blush, NixUraPalette.Orange)) }
    val auraBrush = remember { Brush.horizontalGradient(listOf(NixUraPalette.Orange, NixUraPalette.Rose)) }

    Box(Modifier.fillMaxSize()) {
        AuraBackdrop()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            // The official icon (owner decision 2026-11-14): the chain broken by a glowing gold "i".
            Image(
                painterResource(Res.drawable.nixura_icon),
                contentDescription = null,
                modifier = Modifier.riseIn(40).size(88.dp).clip(RoundedCornerShape(22.dp)),
            )
            Spacer(Modifier.height(18.dp))
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
            // New people start by creating an account; the guest path is a full button too, because NixUra works
            // entirely offline (PRODUCT.md: private comes first).
            AuthPrimaryButton(
                s.landingGetStarted,
                { navigator.push(Route.Auth.Register) },
                Modifier.fillMaxWidth().riseIn(540),
                trailingIcon = NixIcon.Chevron,
            )
            if (onGuest != null) {
                Spacer(Modifier.height(10.dp))
                NixButton(
                    s.authContinueOffline,
                    onGuest,
                    style = NixButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth().riseIn(620),
                )
                Spacer(Modifier.height(4.dp))
                Text(s.landingGuestNote, style = NixType.Small, color = c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.riseIn(660))
            }
            Spacer(Modifier.height(6.dp))
            AuthLink(s.landingHaveAccount, { navigator.push(Route.Auth.Login) }, Modifier.riseIn(700), color = c.textSecondary)
        }
    }
}
