package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Calls [onNearEnd] when the last visible item is within [threshold] items of the end (endless grids). */
@Composable
fun LazyGridState.LoadMoreWhenNearEnd(threshold: Int, onNearEnd: () -> Unit) {
    val currentOnNearEnd by rememberUpdatedState(onNearEnd)
    LaunchedEffect(this) {
        snapshotFlow {
            val info = layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - threshold
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { currentOnNearEnd() }
    }
}
