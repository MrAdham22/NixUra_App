package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.reclaimapp.presentation.components.NixButton
import com.example.reclaimapp.presentation.components.NixButtonStyle
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconView
import com.example.reclaimapp.presentation.components.NixTextField
import com.example.reclaimapp.presentation.settings.SettingsViewModel
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.theme.NixUraPalette

@Composable
fun OnboardingScreen(settingsVm: SettingsViewModel) {
    val s = LocalStrings.current
    val c = Nix.colors
    var name by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(NixUraPalette.Rose, NixUraPalette.Orange))),
            contentAlignment = Alignment.Center,
        ) { NixIconView(NixIcon.Leaf, NixUraPalette.Navy, size = 36.dp) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.welcomeTitle, style = NixType.Display, color = c.textPrimary)
            Text(s.welcomeBody, style = NixType.Body, color = c.textSecondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WelcomePoint(NixIcon.Shield, s.welcomePoint1)
            WelcomePoint(NixIcon.CircleCheck, s.welcomePoint2)
            WelcomePoint(NixIcon.Sparkle, s.welcomePoint3)
        }
        NixTextField(name, { name = it }, label = s.nameLabel, placeholder = s.namePlaceholder, maxLength = 40)
        NixButton(s.getStarted, { settingsVm.onCompleteOnboarding(name) }, modifier = Modifier.fillMaxWidth())
        NixButton(s.skipForNow, { settingsVm.onCompleteOnboarding("") }, style = NixButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun WelcomePoint(icon: NixIcon, text: String) {
    val c = Nix.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            NixIconView(icon, c.accent, size = 22.dp)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = NixType.Body, color = c.textPrimary, modifier = Modifier.weight(1f))
    }
}
