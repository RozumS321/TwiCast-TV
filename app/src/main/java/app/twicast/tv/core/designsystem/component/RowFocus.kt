package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlin.math.abs

/**
 * Column (centre x inside the row's viewport) of the card that last had focus in a stack of rows that share
 * the same start. Moving Up/Down enters the next row at the card closest to it — the one right under/over.
 * Geometric search alone drifted: a row coming in from below the screen has no on-screen position yet, and
 * fast presses landed while the list was still scrolling — focus jumped to the far right card.
 * Positions come from the lazy layout, which exists before the row is placed on screen.
 */
@Stable
class FocusColumnAnchor {
    internal var x: Float = Float.NaN
}

val LocalFocusColumnAnchor = staticCompositionLocalOf<FocusColumnAnchor?> { null }

/** The cards of one lazy row, by item key, so the row can hand focus to the card under the anchor. */
@Stable
class RowFocusTargets internal constructor(internal val state: LazyListState) {
    internal val requesters = HashMap<Any, FocusRequester>()

    internal fun centerOf(key: Any): Float? =
        state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.let { it.offset + it.size / 2f }

    internal fun closestTo(x: Float): FocusRequester? = state.layoutInfo.visibleItemsInfo
        .filter { it.key in requesters }
        .minByOrNull { abs(it.offset + it.size / 2f - x) }
        ?.let { requesters[it.key] }
}

@Composable
fun rememberRowFocusTargets(state: LazyListState): RowFocusTargets = remember(state) { RowFocusTargets(state) }

/** On the row container: Up/Down entries go to the card under the anchor. Other entries keep the default. */
fun Modifier.enterAtFocusColumn(targets: RowFocusTargets, anchor: FocusColumnAnchor?): Modifier =
    if (anchor == null) this else focusProperties {
        onEnter = {
            val vertical = requestedFocusDirection == FocusDirection.Down || requestedFocusDirection == FocusDirection.Up
            if (vertical && !anchor.x.isNaN()) targets.closestTo(anchor.x)?.requestFocus()
        }
        // Right past the last card stays put; geometric search used to fly to whatever sat up-right (Войти).
        onExit = { if (requestedFocusDirection == FocusDirection.Right) cancelFocusChange() }
    }

/** On each card of the row ([key] = its lazy item key): a focus target for the row, and it moves the anchor. */
@Composable
fun Modifier.focusColumnItem(targets: RowFocusTargets, key: Any): Modifier {
    val anchor = LocalFocusColumnAnchor.current ?: return this
    val requester = remember(key) { FocusRequester() }
    DisposableEffect(targets, key) {
        targets.requesters[key] = requester
        onDispose { if (targets.requesters[key] === requester) targets.requesters.remove(key) }
    }
    return this
        .onFocusChanged { state -> if (state.hasFocus) targets.centerOf(key)?.let { anchor.x = it } }
        .focusRequester(requester)
}
