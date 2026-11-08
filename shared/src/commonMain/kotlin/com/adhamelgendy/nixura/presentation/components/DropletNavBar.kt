package com.adhamelgendy.nixura.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import kotlinx.coroutines.flow.collectLatest

/** One entry of [DropletNavBar]. */
data class DropletNavItem(val icon: NixIcon, val label: String)

private const val CIRCLE_KAPPA = 0.5523f

/**
 * Bottom navigation with a liquid "water droplet" selection indicator.
 *
 * The droplet's leading edge sits exactly at [position] (so it follows a finger while the pager is dragged
 * and settles when the page snaps). Its tail is a spring that lags behind, which stretches the droplet
 * toward the target, then contracts it with a small overshoot. It is one tapered path drawn in the draw
 * phase: no blur/threshold effect is needed, so the look is identical on every API level and on iOS.
 *
 * @param position continuous selected position in item coordinates (`pagerState.currentPage + currentPageOffsetFraction`).
 *   It is read only inside draw/graphics-layer lambdas, so scrolling never recomposes this bar.
 * @param selectedIndex item announced as selected to accessibility services; also colours the label.
 * @param reducedMotion when true the droplet jumps without stretching or overshoot.
 */
@Composable
fun DropletNavBar(
    items: List<DropletNavItem>,
    position: () -> Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
) {
    val c = Nix.colors
    val currentPosition by rememberUpdatedState(position)
    val tail = remember { Animatable(position()) }

    // The tail chases the leading edge with an under-damped spring (retargeting keeps its velocity).
    LaunchedEffect(reducedMotion) {
        snapshotFlow { currentPosition() }.collectLatest { target ->
            if (reducedMotion) {
                tail.snapTo(target)
            } else {
                tail.animateTo(target, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

    val selectedIndex by remember(items.size) {
        derivedStateOf { DropletMath.nearestIndex(currentPosition(), items.size) }
    }

    val path = remember { Path() }
    val outline = remember { Stroke(width = 1f) }
    val fill = c.accent.copy(alpha = if (c.isDark) 0.22f else 0.16f)
    val rim = c.accent.copy(alpha = if (c.isDark) 0.55f else 0.4f)
    val shape = RoundedCornerShape(24.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .then(modifier) // after the outer padding, so measuring the bar (starter guide) gets the visible pill
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(6.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    buildDroplet(
                        path = path,
                        position = currentPosition(),
                        trailing = tail.value,
                        itemCount = items.size,
                        rtl = layoutDirection == LayoutDirection.Rtl,
                    )
                    drawPath(path, fill)
                    drawPath(path, rim, style = outline)
                },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            selected = isSelected
                            contentDescription = item.label
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        Modifier.graphicsLayer {
                            val scale = 1f + 0.14f * DropletMath.selection(currentPosition(), index)
                            scaleX = scale
                            scaleY = scale
                        },
                    ) {
                        NixIconView(item.icon, c.textMuted, size = 22.dp)
                        // Accent copy fades in over the muted one; alpha is read in the layer phase (no recomposition).
                        Box(Modifier.graphicsLayer { alpha = DropletMath.selection(currentPosition(), index) }) {
                            NixIconView(item.icon, c.accent, size = 22.dp)
                        }
                    }
                    Text(
                        item.label,
                        style = NixType.Small.copy(fontSize = 10.5.sp),
                        color = if (isSelected) c.accent else c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Fills [path] with the droplet for the current frame. Reuses [path] and allocates nothing else, so it is
 * safe to call every frame. The leading end is a full round cap, the trailing end shrinks as the droplet
 * stretches, and a smooth neck joins them.
 */
private fun DrawScope.buildDroplet(
    path: Path,
    position: Float,
    trailing: Float,
    itemCount: Int,
    rtl: Boolean,
) {
    path.reset()
    if (itemCount <= 0 || size.width <= 0f) return
    val itemW = size.width / itemCount
    val inset = 4.dp.toPx()
    val baseHeight = size.height - 2 * inset
    if (baseHeight <= 0f) return

    val stretch = DropletMath.stretch(position, trailing)
    val s = DropletMath.stretchFraction(stretch)
    val h = baseHeight * (1f - DropletMath.MAX_SQUASH * s)
    val cy = size.height / 2f
    val bigR = h / 2f
    val smallR = bigR * (1f - DropletMath.MAX_TAIL_SHRINK * s)

    // Item-space ends of the droplet body: leading cap centre at `position`, tail cap centre at `trailing`.
    val leadX = DropletMath.toPixels(position, itemW, size.width, rtl)
    val tailX = DropletMath.toPixels(trailing, itemW, size.width, rtl)
    // Direction of travel on screen (+1 = toward the right edge of the screen).
    val movingRight = if (rtl) position <= trailing else position >= trailing
    val dir = if (movingRight) 1f else -1f

    // The resting pill is `itemW - 2*inset` wide; stretch adds the lead/tail distance on top of it.
    val halfBody = (itemW - 2 * inset) / 2f
    val leadCentre = leadX + dir * (halfBody - bigR).coerceAtLeast(0f)
    val tailCentre = tailX - dir * (halfBody - smallR).coerceAtLeast(0f)

    val k = CIRCLE_KAPPA
    val neck = kotlin.math.abs(leadCentre - tailCentre) * 0.5f * dir

    // top of tail cap -> neck -> top of lead cap
    path.moveTo(tailCentre, cy - smallR)
    path.cubicTo(tailCentre + neck, cy - smallR, leadCentre - neck, cy - bigR, leadCentre, cy - bigR)
    // lead cap, two quarter circles through its tip
    path.cubicTo(leadCentre + dir * k * bigR, cy - bigR, leadCentre + dir * bigR, cy - k * bigR, leadCentre + dir * bigR, cy)
    path.cubicTo(leadCentre + dir * bigR, cy + k * bigR, leadCentre + dir * k * bigR, cy + bigR, leadCentre, cy + bigR)
    // bottom of lead cap -> neck -> bottom of tail cap
    path.cubicTo(leadCentre - neck, cy + bigR, tailCentre + neck, cy + smallR, tailCentre, cy + smallR)
    // tail cap, two quarter circles through its tip
    path.cubicTo(tailCentre - dir * k * smallR, cy + smallR, tailCentre - dir * smallR, cy + k * smallR, tailCentre - dir * smallR, cy)
    path.cubicTo(tailCentre - dir * smallR, cy - k * smallR, tailCentre - dir * k * smallR, cy - smallR, tailCentre, cy - smallR)
    path.close()
}
