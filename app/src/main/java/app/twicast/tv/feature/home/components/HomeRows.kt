package app.twicast.tv.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.component.StreamCard
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.format.formatViewerCount
import app.twicast.tv.core.designsystem.image.rememberTwitchImageUrl
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.Stream
import kotlinx.collections.immutable.ImmutableList

/** Below the title: 12 (mockup); bottom: room for the focused card's scale and shadow. */
// The row itself starts just short of the content line and clips there: scrolled cards leave a gap to the sidebar
// instead of sliding up to it, and the inset is room for the first card's focus scale.
private val RowPadding = PaddingValues(
    start = TwiDimens.GridFocusInset,
    end = TwiDimens.ContentEndPadding,
    top = TwiDimens.HomeRowTitleSpacing,
    bottom = TwiDimens.FocusOverflowPadding,
)

/**
 * Titled row of dense stream cards (mockup "Главная"): the stream's title, then "channel · category".
 * [takesInitialFocus]: the first card is focused when home opens (also collapses the sidebar).
 */
@Composable
fun StreamRow(
    title: String,
    hint: String,
    streams: ImmutableList<Stream>,
    onStreamClick: (Stream) -> Unit,
    modifier: Modifier = Modifier,
    followed: Boolean = false,
    takesInitialFocus: Boolean = false,
) {
    val listState = rememberLazyListState()
    // A refresh can reorder the cards, and a lazy row keeps following its old first card. A row the viewer had
    // not scrolled stays at its start instead. Read without observation: scrolling must not recompose the row.
    val startReset = remember(streams) {
        StartReset(Snapshot.withoutReadObservation { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 })
    }
    // Once per new list, so a later recomposition never pulls back a row the viewer has scrolled since.
    SideEffect { if (startReset.consume()) listState.requestScrollToItem(0) }
    // No top padding: the previous row's bottom focus room (24) already separates rows (mockup: 22).
    Column(modifier) {
        RowHeader(title, hint)
        LazyRow(
            state = listState,
            contentPadding = RowPadding,
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.HomeCardSpacing),
            // Coming back to the row lands on the card that was focused last.
            modifier = Modifier.padding(start = TwiDimens.ContentStartPadding - TwiDimens.GridFocusInset).focusRestorer(),
        ) {
            itemsIndexed(streams, key = { _, it -> it.id }, contentType = { _, _ -> "stream" }) { index, stream ->
                StreamCard(
                    title = stream.title,
                    subtitle = listOf(stream.channelName, stream.categoryName).filter { it.isNotEmpty() }.joinToString(" · "),
                    viewerCountText = formatViewerCount(stream.viewerCount),
                    thumbnailUrl = rememberTwitchImageUrl(stream.thumbnailTemplate, TwiDimens.StreamCardDenseWidth, TwiAspect.Thumbnail),
                    avatarUrl = stream.avatarUrl,
                    onClick = { onStreamClick(stream) },
                    dense = true,
                    avatarRing = followed,
                    modifier = if (takesInitialFocus && index == 0) Modifier.requestInitialFocus() else Modifier,
                )
            }
        }
    }
}

/** Whether a row should go back to its start for a new list; answers true at most once. */
private class StartReset(private var pending: Boolean) {
    fun consume(): Boolean = pending.also { pending = false }
}

@Composable
private fun RowHeader(title: String, hint: String) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.RowHintSpacing),
        modifier = Modifier.padding(start = TwiDimens.ContentStartPadding),
    ) {
        Text(title, style = TwiTheme.typography.rowTitleDense, color = TwiTheme.colors.onSurface, modifier = Modifier.alignByBaseline())
        Text(hint, style = TwiTheme.typography.caption, color = TwiTheme.colors.onSurfaceVariant, modifier = Modifier.alignByBaseline())
    }
}
