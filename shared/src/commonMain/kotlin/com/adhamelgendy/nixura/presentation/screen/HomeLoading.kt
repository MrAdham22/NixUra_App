package com.adhamelgendy.nixura.presentation.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.SkeletonCircle
import com.adhamelgendy.nixura.presentation.components.SkeletonLine
import com.adhamelgendy.nixura.presentation.components.SkeletonTextHeight
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixUraTheme
import kotlinx.coroutines.delay

/** What the Home sections show. */
enum class HomePhase {
    /** The lists are still being read. */
    Loading,

    /** Read, and the user has nothing yet: the "Start with one thing" card. */
    Empty,

    /** Read, with something to show. */
    Ready,
}

/**
 * Home's phase from its three lists. A list still loading means Home cannot know yet whether it is empty, so it never
 * shows the start card for a user who has habits (that swap was the jump on every cold start).
 */
fun homePhase(quitLoading: Boolean, goodLoading: Boolean, eventsLoading: Boolean, hasAnything: Boolean): HomePhase = when {
    quitLoading || goodLoading || eventsLoading -> HomePhase.Loading
    hasAnything -> HomePhase.Ready
    else -> HomePhase.Empty
}

/** Loading shorter than this shows nothing at all, so a fast load never flashes a skeleton. */
const val SKELETON_DELAY_MILLIS = 300L

/** True once [loading] has lasted [SKELETON_DELAY_MILLIS]; false again as soon as it ends. */
@Composable
fun rememberShowSkeleton(loading: Boolean): Boolean {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(loading) {
        show = false
        if (loading) {
            delay(SKELETON_DELAY_MILLIS)
            show = true
        }
    }
    return show
}

/**
 * Placeholders shaped like Home's real cards (progress rings, today's habits, a quit card, the next event): the same
 * sizes, corners and spacing, so nothing moves when the data arrives.
 */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Column(
        modifier.fillMaxWidth().semantics { contentDescription = s.homeLoading },
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ProgressCardSkeleton()
        SectionLabelSkeleton()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TodayRowSkeleton()
            TodayRowSkeleton()
        }
        SectionLabelSkeleton()
        QuitRowSkeleton()
        SectionLabelSkeleton()
        EventCardSkeleton()
    }
}

@Composable
private fun SectionLabelSkeleton() = SkeletonLine(0.28f, SkeletonTextHeight)

/** Same frame as the progress card: 18 dp padding around 128 dp rings and two score lines. */
@Composable
private fun ProgressCardSkeleton() {
    NixCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonCircle(128.dp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScoreSkeleton()
                ScoreSkeleton()
            }
        }
    }
}

@Composable
private fun ScoreSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SkeletonLine(0.7f, SkeletonTextHeight)
        SkeletonLine(0.4f, 22.dp)
    }
}

/** Same frame as a "Today" row: 18 dp corners, a name, a progress line and the 40 dp + button. */
@Composable
private fun TodayRowSkeleton() {
    NixCard(
        Modifier.fillMaxWidth(),
        padding = PaddingValues(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        shapeRadius = 18.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(0.55f, 14.dp)
                SkeletonLine(0.35f, SkeletonTextHeight)
            }
            Spacer(Modifier.width(10.dp))
            SkeletonCircle(40.dp)
        }
    }
}

/** Same frame as a quit row: 22 dp corners, the glyph and name, then the counter blocks (44 dp tall with the chip). */
@Composable
private fun QuitRowSkeleton() {
    val c = Nix.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface, shape)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonCircle(36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(0.5f, 14.dp)
                SkeletonLine(0.7f, SkeletonTextHeight)
            }
        }
        SkeletonLine(0.6f, 44.dp)
    }
}

/** Same frame as the event card: the 40 dp category badge, a label, the title and the date. */
@Composable
private fun EventCardSkeleton() {
    NixCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonCircle(40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(0.3f, 10.dp)
                SkeletonLine(0.75f, 20.dp)
                SkeletonLine(0.45f, SkeletonTextHeight)
            }
        }
    }
}

@Composable
private fun SkeletonPreviewFrame(dark: Boolean, rtl: Boolean, content: @Composable ColumnScope.() -> Unit) {
    NixUraTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Column(Modifier.background(Nix.colors.background).padding(20.dp), content = content)
        }
    }
}

@Preview
@Composable
private fun HomeSkeletonDarkPreview() = SkeletonPreviewFrame(dark = true, rtl = false) { HomeSkeleton() }

@Preview
@Composable
private fun HomeSkeletonLightPreview() = SkeletonPreviewFrame(dark = false, rtl = false) { HomeSkeleton() }

@Preview
@Composable
private fun HomeSkeletonDarkRtlPreview() = SkeletonPreviewFrame(dark = true, rtl = true) { HomeSkeleton() }

@Preview
@Composable
private fun HomeSkeletonLightRtlPreview() = SkeletonPreviewFrame(dark = false, rtl = true) { HomeSkeleton() }
