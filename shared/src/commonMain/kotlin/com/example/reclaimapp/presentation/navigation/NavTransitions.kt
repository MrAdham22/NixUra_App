package com.example.reclaimapp.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.example.reclaimapp.presentation.components.NixEasing

/** A screen and how deep in the back stack it sits; going deeper is a push, going shallower is a pop. */
data class NavEntry(val route: Route, val depth: Int)

private const val DURATION_MILLIS = 340

/** Forms and sign-in rise from the bottom like a sheet. Everything else slides in from the side. */
private fun Route.isModal(): Boolean = when (this) {
    is Route.BadHabitForm, is Route.GoodHabitForm, is Route.EventForm -> true
    else -> false
}

/**
 * Push: the new screen slides over while the old one drifts back a little. Pop: the reverse. Modal routes rise and fall.
 * With "remove animations" on, screens only cross-fade.
 */
fun AnimatedContentTransitionScope<NavEntry>.navTransition(reducedMotion: Boolean): ContentTransform {
    if (reducedMotion) return fadeIn(tween(120)) togetherWith fadeOut(tween(120))
    val slide = tween<androidx.compose.ui.unit.IntOffset>(DURATION_MILLIS, easing = NixEasing)
    val fade = tween<Float>(DURATION_MILLIS, easing = NixEasing)
    val forward = targetState.depth >= initialState.depth
    val modal = if (forward) targetState.route.isModal() else initialState.route.isModal()
    val transform = when {
        modal && forward -> (slideInVertically(slide) { it }).togetherWith(fadeOut(fade, 0.7f) + scaleOut(fade, 0.96f))
        modal -> (fadeIn(fade, 0.7f) + scaleIn(fade, 0.96f)).togetherWith(slideOutVertically(slide) { it })
        forward -> slideInHorizontally(slide) { it }.togetherWith(slideOutHorizontally(slide) { -it / 4 } + fadeOut(fade, 0.6f))
        else -> (slideInHorizontally(slide) { -it / 4 } + fadeIn(fade, 0.6f)).togetherWith(slideOutHorizontally(slide) { it })
    }
    transform.targetContentZIndex = if (forward) 1f else -1f
    return transform.using(SizeTransform(clip = false))
}
