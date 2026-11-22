package com.adhamelgendy.nixura.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RowHeight = 40.dp // the stepper's button height
private val BandShape = RoundedCornerShape(14.dp) // the same corners as an input

/**
 * Numbers that slide up and down; the one in the band is chosen. Tap a number above or below to choose it.
 *
 * [onChange] follows the finger, so "Done" pressed while the wheel is still turning saves the number on screen. A drag
 * on the wheel never scrolls the form or sheet around it. Screen readers get one adjustable control named
 * [description].
 *
 * @param loop the numbers wrap around (59 then 00).
 * @param visibleRows odd: how many numbers show at once.
 */
@Composable
fun NumberWheel(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    loop: Boolean = false,
    visibleRows: Int = 3,
    width: Dp = 72.dp,
    format: (Int) -> String = { it.toString() },
) {
    val c = Nix.colors
    val math = remember(range, loop) { WheelMath(range, loop) }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = math.startRowOf(value))
    val density = LocalDensity.current
    // Taller and wider rows when the phone's text is larger, so digits are never clipped.
    val scale = density.fontScale.coerceAtLeast(1f)
    val rowHeight = RowHeight * scale
    val halfRowPx = with(density) { rowHeight.toPx() / 2 }
    val centred by remember(math, list, halfRowPx) {
        derivedStateOf {
            val row = list.firstVisibleItemIndex + if (list.firstVisibleItemScrollOffset > halfRowPx) 1 else 0
            row.coerceIn(0, math.rowCount - 1)
        }
    }
    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onChange)
    val haptics = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotion()
    val scope = rememberCoroutineScope()

    LaunchedEffect(math, list, halfRowPx) {
        // Not the first position: showing the wheel is not a choice.
        snapshotFlow { centred }.drop(1).collect { row ->
            val chosen = math.valueAt(row)
            if (chosen != latestValue) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                latestOnChange(chosen)
            }
        }
    }
    // A value set from outside (a screen reader, the caller) turns the wheel to it.
    LaunchedEffect(math, value) {
        if (!list.isScrollInProgress && math.valueAt(centred) != value) list.scrollToItem(math.nearestRowOf(value, centred))
    }

    Box(
        modifier
            .width(width * scale)
            .height(rowHeight * visibleRows.toFloat())
            .clearAndSetSemantics {
                contentDescription = description
                stateDescription = format(value)
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), range.first.toFloat()..range.last.toFloat())
                setProgress { target ->
                    latestOnChange(target.roundToInt().coerceIn(range))
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .background(c.surfaceRaised, BandShape)
                .border(1.dp, c.border, BandShape),
        )
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxWidth().height(rowHeight * visibleRows.toFloat()).nestedScroll(KeepDragOnWheel),
            contentPadding = PaddingValues(vertical = rowHeight * (visibleRows / 2).toFloat()),
            flingBehavior = rememberSnapFlingBehavior(list),
        ) {
            items(math.rowCount) { row ->
                val chosen = row == centred
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clickable(interactionSource = null, indication = null) {
                            scope.launch { if (reducedMotion) list.scrollToItem(row) else list.animateScrollToItem(row) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        format(math.valueAt(row)),
                        style = if (chosen) NixType.Heading else NixType.Body,
                        color = if (chosen) c.textPrimary else c.textMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Takes whatever drag and fling the wheel did not use, so reaching its end does not scroll the screen behind it. */
private object KeepDragOnWheel : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}
