package com.example.reclaimapp.presentation.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.notification.NotificationSyncService
import com.example.reclaimapp.presentation.components.Avatar
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconButton
import com.example.reclaimapp.presentation.components.NixIconView
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.platform.SystemBarsEffect
import com.example.reclaimapp.presentation.profile.ProfileViewModel
import com.example.reclaimapp.presentation.settings.SettingsViewModel
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.strings.stringsFor
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.theme.NixUraPalette
import com.example.reclaimapp.presentation.theme.NixUraTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppRoot() {
    val settingsVm: SettingsViewModel = koinViewModel()
    val state by settingsVm.uiState.collectAsStateWithLifecycle()
    val navigator = koinInject<Navigator>()
    val sync = koinInject<NotificationSyncService>()
    LaunchedEffect(Unit) { sync.run() }

    NixUraTheme(darkTheme = state.settings.darkTheme) {
        SystemBarsEffect(state.settings.darkTheme)
        CompositionLocalProvider(LocalStrings provides stringsFor(state.settings.languageCode)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Nix.colors.background)
                    .windowInsetsPadding(WindowInsets.safeDrawing), // status/nav bars, cutout, keyboard; NOT gesture insets (those caused wide side/bottom gaps)
            ) {
                when {
                    !state.loaded -> Unit
                    !state.settings.onboardingCompleted -> OnboardingScreen(settingsVm)
                    else -> MainContent(navigator)
                }
            }
        }
    }
}

@Composable
private fun MainContent(navigator: Navigator) {
    val route = navigator.current
    val isTab = route is Route.Tab
    Column(Modifier.fillMaxSize()) {
        if (isTab) TopBar(navigator)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(targetState = route, animationSpec = tween(180), label = "route") { target ->
                RouteContent(target, navigator)
            }
        }
        if (isTab) BottomNav(route, navigator)
    }
}

@Composable
private fun RouteContent(route: Route, navigator: Navigator) {
    when (route) {
        Route.Home -> HomeScreen(navigator)
        Route.BadHabits -> BadHabitsScreen(navigator)
        Route.GoodHabits -> GoodHabitsScreen(navigator)
        Route.Events -> EventsScreen(navigator)
        Route.Coach -> CoachScreen(navigator)
        Route.Profile -> ProfileScreen(navigator)
        Route.Settings -> SettingsScreen(navigator)
        is Route.BadHabitDetail -> BadHabitDetailScreen(route.habitId, navigator)
        is Route.GoodHabitDetail -> GoodHabitDetailScreen(route.habitId, navigator)
        is Route.BadHabitForm -> BadHabitFormScreen(route.editId, navigator)
        is Route.GoodHabitForm -> GoodHabitFormScreen(route.editId, navigator)
        is Route.EventForm -> EventFormScreen(route.editId, navigator)
    }
}

@Composable
private fun TopBar(navigator: Navigator) {
    val c = Nix.colors
    val profileVm: ProfileViewModel = koinViewModel()
    val profile by profileVm.uiState.collectAsStateWithLifecycle()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(NixUraPalette.Rose, NixUraPalette.Orange))),
            contentAlignment = Alignment.Center,
        ) { NixIconView(NixIcon.Leaf, NixUraPalette.Navy, size = 20.dp) }
        Spacer(Modifier.width(10.dp))
        Text("NixUra", style = NixType.Heading, color = c.textPrimary)
        Spacer(Modifier.weight(1f))
        NixIconButton(NixIcon.Sliders, { navigator.push(Route.Settings) }, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Avatar(
            initial = profile.name.ifBlank { "N" },
            size = 40.dp,
            modifier = Modifier.clickable(interactionSource = MutableInteractionSource(), indication = null) { navigator.push(Route.Profile) },
        )
    }
}

@Composable
private fun BottomNav(current: Route, navigator: Navigator) {
    val s = LocalStrings.current
    val c = Nix.colors
    val items = listOf(
        Triple(Route.Home, NixIcon.Home, s.navHome),
        Triple(Route.BadHabits, NixIcon.Shield, s.navQuit),
        Triple(Route.GoodHabits, NixIcon.CircleCheck, s.navBuild),
        Triple(Route.Events, NixIcon.Calendar, s.navEvents),
        Triple(Route.Coach, NixIcon.Sparkle, s.navCoach),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(24.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEach { (route, icon, label) ->
            val selected = current == route
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) c.accent.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable(interactionSource = MutableInteractionSource(), indication = null) { navigator.selectTab(route as Route.Tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                NixIconView(icon, if (selected) c.accent else c.textMuted, size = 22.dp)
                Text(label, style = NixType.Small.copy(fontSize = 10.5.sp), color = if (selected) c.accent else c.textMuted, maxLines = 1)
            }
        }
    }
}
