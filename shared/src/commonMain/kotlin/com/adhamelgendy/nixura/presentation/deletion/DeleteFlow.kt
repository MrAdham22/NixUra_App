package com.adhamelgendy.nixura.presentation.deletion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.presentation.components.ConfirmDialog
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.NixSheet
import com.adhamelgendy.nixura.presentation.components.pressable
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import org.koin.compose.koinInject

/** What a long-pressed card asks to delete. */
internal data class DeleteRequest(val item: DeletableItem, val name: String)

/** Opens the delete option for a card. Get one with [rememberDeleteFlow]. */
@Stable
class DeleteFlow internal constructor() {
    internal var request by mutableStateOf<DeleteRequest?>(null)
    internal var confirming by mutableStateOf(false)

    /** Long-press handler: offers "Delete" for [item], then asks for confirmation naming [name]. */
    fun open(item: DeletableItem, name: String) {
        request = DeleteRequest(item, name)
        confirming = false
    }

    /** Skips the sheet and asks for confirmation straight away (e.g. a trash button on a detail screen). */
    fun confirm(item: DeletableItem, name: String) {
        request = DeleteRequest(item, name)
        confirming = true
    }
}

/**
 * Long-press delete: a sheet with one "Delete" action, then a confirmation dialog that names the item, then the
 * delete with an undo snackbar (see [UndoDeleteBar]). [onDeleted] runs once the delete is scheduled, e.g. to leave
 * a detail screen.
 */
@Composable
fun rememberDeleteFlow(onDeleted: () -> Unit = {}): DeleteFlow {
    val flow = remember { DeleteFlow() }
    val pending = koinInject<PendingDeletions>()
    val s = LocalStrings.current
    val c = Nix.colors
    val req = flow.request ?: return flow
    if (!flow.confirming) {
        NixSheet(onDismiss = { flow.request = null }) {
            Text(req.name, style = NixType.Heading, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().pressable(haptic = true) { flow.confirming = true }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NixIconView(NixIcon.Trash, c.danger, size = 22.dp)
                Spacer(Modifier.width(14.dp))
                Text(s.deleteAction, style = NixType.BodyStrong, color = c.danger)
            }
        }
    } else {
        val body = when (req.item) {
            is DeletableItem.BadHabit -> s.deleteBadHabitBody
            is DeletableItem.GoodHabit -> s.deleteGoodHabitBody
            is DeletableItem.Event -> s.deleteEventItemBody
        }
        ConfirmDialog(
            title = s.deleteItemTitle(req.name),
            message = body,
            confirmText = s.deleteAction,
            cancelText = s.cancel,
            danger = true,
            onConfirm = {
                flow.request = null
                pending.schedule(req.item, req.name)
                onDeleted()
            },
            onDismiss = { flow.request = null },
        )
    }
    return flow
}

/** "“Name” deleted · Undo", shown for the newest delete while it can still be undone. */
@Composable
fun UndoDeleteBar(modifier: Modifier = Modifier) {
    val pending = koinInject<PendingDeletions>()
    val list by pending.pending.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val c = Nix.colors
    val newest = list.lastOrNull { it.undoable }
    // Keep the last text while the bar slides away.
    var shown by remember { mutableStateOf<PendingDeletion?>(null) }
    if (newest != null) shown = newest
    AnimatedVisibility(
        visible = newest != null,
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        val p = shown ?: return@AnimatedVisibility
        NixCard(
            Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            color = c.surfaceRaised,
            padding = PaddingValues(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            shapeRadius = 18.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.deletedItem(p.name), style = NixType.Body, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    s.undo,
                    style = NixType.BodyStrong,
                    color = c.accentWarm,
                    modifier = Modifier.pressable(haptic = true) { pending.undo(p.item) }.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
    }
}
