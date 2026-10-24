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
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.component.StreamCard
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.format.formatPlaybackTime
import app.twicast.tv.core.designsystem.format.formatUptimeShort
import app.twicast.tv.core.designsystem.format.formatViewerCount
import app.twicast.tv.core.designsystem.image.rememberTwitchImageUrl
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.R
import app.twicast.tv.data.model.Stream
import app.twicast.tv.data.repository.HistoryEntry
import app.twicast.tv.feature.home.RecentItem
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
                    uptimeText = stream.startedAtMillis?.let { formatUptimeShort(it, System.currentTimeMillis()) },
                    modifier = if (takesInitialFocus && index == 0) Modifier.requestInitialFocus() else Modifier,
                )
            }
        }
    }
}

/**
 * "Недавно смотрели" (mockup "Главная · недавно смотрели"): recordings with their strip and time left, channels
 * live now (LIVE, viewers) or off air.
 */
@Composable
fun RecentRow(
    items: ImmutableList<RecentItem>,
    onStreamClick: (Stream) -> Unit,
    onRecordingClick: (HistoryEntry) -> Unit,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    takesInitialFocus: Boolean = false,
) {
    Column(modifier) {
        RowHeader(stringResource(R.string.home_row_recent), stringResource(R.string.home_row_recent_hint))
        LazyRow(
            contentPadding = RowPadding,
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.HomeCardSpacing),
            modifier = Modifier.padding(start = TwiDimens.ContentStartPadding - TwiDimens.GridFocusInset).focusRestorer(),
        ) {
            itemsIndexed(items, key = { _, it -> it.entry.key }, contentType = { _, it -> it::class }) { index, item ->
                val focus = if (takesInitialFocus && index == 0) Modifier.requestInitialFocus() else Modifier
                RecentCard(item, onStreamClick, onRecordingClick, onChannelClick, focus)
            }
        }
    }
}

@Composable
private fun RecentCard(
    item: RecentItem,
    onStreamClick: (Stream) -> Unit,
    onRecordingClick: (HistoryEntry) -> Unit,
    onChannelClick: (String) -> Unit,
    modifier: Modifier,
) {
    val entry = item.entry
    when (item) {
        is RecentItem.Recording -> StreamCard(
            title = entry.title ?: entry.channelName,
            subtitle = entry.channelName,
            viewerCountText = item.remainingMs?.let { stringResource(R.string.home_recent_left, formatPlaybackTime(it)) }
                ?: stringResource(R.string.home_recent_recording),
            thumbnailUrl = entry.thumbnailTemplate?.let { rememberTwitchImageUrl(it, TwiDimens.StreamCardDenseWidth, TwiAspect.Thumbnail) },
            avatarUrl = entry.avatarUrl,
            onClick = { onRecordingClick(entry) },
            dense = true,
            live = false,
            watchedFraction = item.watchedFraction,
            modifier = modifier,
        )
        is RecentItem.Channel -> {
            val stream = item.stream
            if (stream != null) {
                StreamCard(
                    title = stream.title,
                    subtitle = listOf(stream.channelName, stream.categoryName).filter { it.isNotEmpty() }.joinToString(" · "),
                    viewerCountText = formatViewerCount(stream.viewerCount),
                    thumbnailUrl = rememberTwitchImageUrl(stream.thumbnailTemplate, TwiDimens.StreamCardDenseWidth, TwiAspect.Thumbnail),
                    avatarUrl = stream.avatarUrl,
                    onClick = { onStreamClick(stream) },
                    dense = true,
                    uptimeText = stream.startedAtMillis?.let { formatUptimeShort(it, System.currentTimeMillis()) },
                    modifier = modifier,
                )
            } else {
                // Off air: nothing to play, the card leads to the channel's recordings.
                StreamCard(
                    title = entry.channelName,
                    subtitle = entry.categoryName,
                    viewerCountText = stringResource(R.string.home_recent_offline),
                    thumbnailUrl = null,
                    avatarUrl = entry.avatarUrl,
                    onClick = { onChannelClick(entry.channelLogin) },
                    dense = true,
                    live = false,
                    modifier = modifier,
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
