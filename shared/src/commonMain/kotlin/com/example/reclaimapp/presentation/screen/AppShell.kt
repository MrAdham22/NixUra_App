package com.example.reclaimapp.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.reclaimapp.presentation.auth.AccountSheet
import com.example.reclaimapp.presentation.auth.AuthGate
import com.example.reclaimapp.presentation.components.pressable
import com.example.reclaimapp.presentation.guide.GuideTarget
import com.example.reclaimapp.presentation.guide.GuideTargets
import com.example.reclaimapp.presentation.guide.LocalGuideTargets
import com.example.reclaimapp.presentation.guide.StarterGuide
import com.example.reclaimapp.presentation.guide.guideTarget
import com.example.reclaimapp.presentation.navigation.NavEntry
import com.example.reclaimapp.presentation.navigation.navTransition
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
import kotlinx.coroutines.flow.drop
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.example.reclaimapp.data.auth.SessionManagerImpl
import com.example.reclaimapp.domain.auth.AccountProfileSync
import com.example.reclaimapp.domain.auth.SessionState
import com.example.reclaimapp.presentation.auth.AccountScreen
import com.example.reclaimapp.presentation.auth.AuthHost
import com.example.reclaimapp.presentation.auth.ChangePasswordScreen
import com.example.reclaimapp.presentation.auth.DeleteAccountScreen
import com.example.reclaimapp.presentation.auth.DevicesScreen
import com.example.reclaimapp.presentation.auth.LegalScreen
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.example.reclaimapp.presentation.deletion.UndoDeleteBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.notification.NotificationSyncService
import com.example.reclaimapp.domain.sync.SyncCoordinator
import com.example.reclaimapp.presentation.components.Avatar
import com.example.reclaimapp.presentation.components.DropletNavBar
import com.example.reclaimapp.presentation.components.DropletNavItem
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconButton
import com.example.reclaimapp.presentation.components.NixIconView
import com.example.reclaimapp.presentation.navigation.MainTabs
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.platform.SystemBarsEffect
import com.example.reclaimapp.presentation.platform.rememberReducedMotion
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

    // Cloud sync: acts only while signed in. Guests stay completely offline.
    val cloudSync = koinInject<SyncCoordinator>()
    val syncScope = rememberCoroutineScope()
    LaunchedEffect(Unit) { cloudSync.run() }
    LifecycleResumeEffect(Unit) {
        syncScope.launch { cloudSync.onAppForeground() }
        onPauseOrDispose { }
    }

    // Accounts: optional sign-in. The app stays fully usable offline without an account ("Continue without an account").
    val sessionManager = koinInject<SessionManagerImpl>()
    val profileSync = koinInject<AccountProfileSync>()
    val session by sessionManager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var authPromptSeen by remember { mutableStateOf<Boolean?>(null) }
    var wasSignedIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        sessionManager.restore()
        authPromptSeen = sessionManager.authPromptSeen()
    }
    LaunchedEffect(session) {
        when (session) {
            is SessionState.SignedIn -> {
                wasSignedIn = true
                authPromptSeen = false // a later sign-out shows the Landing screen again
                while (navigator.current is Route.Auth) navigator.back() // leave the sign-in screens
                profileSync.sync()
            }
            SessionState.SignedOut -> if (wasSignedIn) {
                wasSignedIn = false
                navigator.selectTab(Route.Home) // signed out / account deleted: do not stay on an account screen
            }
            SessionState.Loading -> Unit
        }
    }

    // The landing and sign-in screens are always the dark brand look, whatever the app's theme setting is.
    val inGate = state.loaded && session is SessionState.SignedOut && authPromptSeen == false
    val darkTheme = state.settings.darkTheme || inGate || navigator.current is Route.Auth
    NixUraTheme(darkTheme = darkTheme) {
        SystemBarsEffect(darkTheme)
        CompositionLocalProvider(LocalStrings provides stringsFor(state.settings.languageCode)) {
            // The starter guide runs once per device, over the real app. It covers the whole window, bars included.
            val inMainApp = state.loaded && session !is SessionState.Loading && authPromptSeen != null && !inGate
            val guideActive = inMainApp && !state.settings.guideCompleted
            val guideTargets = remember { GuideTargets() }
            Box(Modifier.fillMaxSize().background(Nix.colors.background)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing), // status/nav bars, cutout, keyboard; NOT gesture insets (those caused wide side/bottom gaps)
                ) {
                    when {
                        !state.loaded -> Unit
                        session is SessionState.Loading || authPromptSeen == null -> Unit
                        inGate -> AuthGate(
                            navigator,
                            onEnterAsGuest = {
                                navigator.selectTab(Route.Home) // the guest link can be tapped from inside the sign-in screens
                                scope.launch { sessionManager.markAuthPromptSeen(); authPromptSeen = true }
                            },
                        )
                        else -> CompositionLocalProvider(LocalGuideTargets provides guideTargets.takeIf { guideActive }) {
                            MainContent(navigator)
                        }
                    }
                }
                if (guideActive) {
                    val profileVm: ProfileViewModel = koinViewModel()
                    val profile by profileVm.uiState.collectAsStateWithLifecycle()
                    StarterGuide(
                        navigator,
                        currentName = profile.name,
                        targets = guideTargets,
                        onSaveName = { profileVm.onSave(it, profile.email, profile.age?.toString().orEmpty()) {} },
                        onFinish = settingsVm::onGuideFinished,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainContent(navigator: Navigator) {
    val route = navigator.current
    val reducedMotion = rememberReducedMotion()
    // All pager tabs share one state (Route.Home stands for "the tab host"), so swiping/tapping between them never
    // animates the whole screen; only entering/leaving the host (detail screens, Profile, Settings, the Coach) does.
    val host: Route = if (MainTabs.isPagerTab(route)) Route.Home else route
    AnimatedContent(
        targetState = NavEntry(host, navigator.stack.size),
        contentKey = { it.route },
        transitionSpec = { navTransition(reducedMotion) },
        label = "route",
    ) { entry ->
        Box(Modifier.fillMaxSize().background(Nix.colors.background)) {
            if (entry.route == Route.Home) {
                TabsHost(navigator, initialTab = if (MainTabs.isPagerTab(navigator.current)) navigator.current else Route.Home)
            } else {
                // The coach is full-screen: no top bar, no bottom nav (it has its own compact header with a back button).
                RouteContent(entry.route, navigator)
            }
        }
    }
}

/**
 * Top bar + swipeable pager + droplet bottom bar for the four pager tabs.
 *
 * Source of truth for "which tab" is [Navigator] (so Home's "See all" links, widget deep links and the back
 * button keep working); the pager is kept in sync in both directions:
 * - navigator changed (tap, link, back) -> pager animates to that page;
 * - user swiped and the pager *settled* -> navigator selects that tab (mid-swipe positions are ignored).
 */
@Composable
private fun TabsHost(navigator: Navigator, initialTab: Route) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotion()
    val pagerState = rememberPagerState(initialPage = MainTabs.pageOf(initialTab) ?: 0) { MainTabs.pagerTabs.size }
    // Page the user just tapped; its haptic tick is given on the tap, not again when the pager settles.
    val tappedPage = remember { intArrayOf(-1) }

    // navigator -> pager
    val navigatorTab = navigator.current
    LaunchedEffect(navigatorTab) {
        val page = MainTabs.pageOf(navigatorTab) ?: return@LaunchedEffect // leaving the host (detail, coach...)
        if (page != pagerState.targetPage) {
            if (reducedMotion) {
                pagerState.animateScrollToPage(page, animationSpec = tween(120))
            } else {
                pagerState.animateScrollToPage(page, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            }
        }
    }
    // pager -> navigator (settled page only)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val tab = MainTabs.pagerTabs[page]
            if (MainTabs.isPagerTab(navigator.current) && navigator.current != tab) navigator.selectTab(tab)
        }
    }
    // Haptic tick when a swipe lands on a different page.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            if (tappedPage[0] != page) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            tappedPage[0] = -1
        }
    }

    val items = remember(s) {
        listOf(
            DropletNavItem(NixIcon.Home, s.navHome),
            DropletNavItem(NixIcon.Shield, s.navQuit),
            DropletNavItem(NixIcon.CircleCheck, s.navBuild),
            DropletNavItem(NixIcon.Calendar, s.navEvents),
            DropletNavItem(NixIcon.Sparkle, s.navCoach),
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(navigator)
        Box(Modifier.weight(1f).fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Neighbours are pre-composed so a swipe never reveals an empty page; farther pages are disposed
            // (their scroll positions are saved by the pager and restored when they return).
            beyondViewportPageCount = 1,
            key = { it },
        ) { page ->
            Box(
                Modifier.fillMaxSize().semantics {
                    // TalkBack must only see the visible page; the bar is the way to move between tabs.
                    if (page != pagerState.currentPage) hideFromAccessibility()
                },
            ) {
                when (MainTabs.pagerTabs[page]) {
                    Route.Home -> HomeScreen(navigator)
                    Route.BadHabits -> BadHabitsScreen(navigator)
                    Route.GoodHabits -> GoodHabitsScreen(navigator)
                    else -> EventsScreen(navigator)
                }
            }
        }
            // Undo for a long-press delete, just above the bar, on every tab.
            UndoDeleteBar(Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 8.dp))
        }
        DropletNavBar(
            items = items,
            modifier = Modifier.guideTarget(GuideTarget.NavBar),
            position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
            reducedMotion = reducedMotion,
            onSelect = { index ->
                val tab = MainTabs.barTabs[index]
                if (tab != navigator.current) {
                    tappedPage[0] = MainTabs.pageOf(tab) ?: -1
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navigator.selectTab(tab)
                }
            },
        )
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
        Route.CoachHistory -> CoachHistoryScreen(navigator)
        Route.Profile -> ProfileScreen(navigator)
        Route.Settings -> SettingsScreen(navigator)
        is Route.BadHabitDetail -> BadHabitDetailScreen(route.habitId, navigator)
        is Route.GoodHabitDetail -> GoodHabitDetailScreen(route.habitId, navigator)
        is Route.BadHabitForm -> BadHabitFormScreen(route.editId, navigator)
        is Route.GoodHabitForm -> GoodHabitFormScreen(route.editId, navigator)
        is Route.EventForm -> EventFormScreen(route.editId, navigator)
        is Route.Auth -> AuthHost(route, navigator, onGuest = null)
        Route.Account -> AccountScreen(navigator)
        Route.ChangePassword -> ChangePasswordScreen(navigator)
        Route.DeleteAccount -> DeleteAccountScreen(navigator)
        Route.Devices -> DevicesScreen(navigator)
        is Route.Legal -> LegalScreen(route.document, navigator)
    }
}

@Composable
private fun TopBar(navigator: Navigator) {
    val c = Nix.colors
    val profileVm: ProfileViewModel = koinViewModel()
    val profile by profileVm.uiState.collectAsStateWithLifecycle()
    var accountSheetOpen by rememberSaveable { mutableStateOf(false) }
    if (accountSheetOpen) AccountSheet(profile.name, navigator, onDismiss = { accountSheetOpen = false })
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
        Avatar(
            initial = profile.name.ifBlank { profile.accountEmail ?: "N" },
            size = 40.dp,
            modifier = Modifier.guideTarget(GuideTarget.Avatar).pressable(pressedScale = 0.9f, haptic = true) { accountSheetOpen = true },
        )
    }
}
