package app.twicast.tv.feature.following

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.EmptyState
import app.twicast.tv.core.designsystem.component.ErrorState
import app.twicast.tv.core.designsystem.component.KeepFocusedVisibleScroll
import app.twicast.tv.core.designsystem.component.SegmentedTabs
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.format.formatViewersLabel
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.FollowedChannel
import app.twicast.tv.data.model.Stream
import app.twicast.tv.feature.common.GridSkeleton
import app.twicast.tv.feature.common.appErrorMessage
import app.twicast.tv.feature.following.components.ChannelCard
import kotlinx.collections.immutable.persistentListOf

private const val Columns = 6

/** Mockup "Подписки": title, "В эфире · N / Офлайн · N" tabs, 6-column grid of channels. */
@Composable
fun FollowingScreen(
    state: FollowingUiState,
    onTabSelected: (FollowingTab) -> Unit,
    onStreamClick: (Stream) -> Unit,
    onChannelClick: (FollowedChannel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(top = TwiDimens.ScreenTopPadding)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = TwiDimens.ContentStartPadding),
        ) {
            Text(stringResource(R.string.nav_following), style = TwiTheme.typography.screenTitle, color = TwiTheme.colors.onSurface)
            Spacer(Modifier.weight(1f))
            if (state is FollowingUiState.Content) {
                SegmentedTabs(
                    labels = persistentListOf(
                        stringResource(R.string.following_tab_live, state.live.size),
                        stringResource(R.string.following_tab_offline, state.offline.size),
                    ),
                    selectedIndex = state.tab.ordinal,
                    onSelect = { onTabSelected(FollowingTab.entries[it]) },
                )
            }
        }
        when (state) {
            FollowingUiState.Loading -> GridSkeleton(columns = Columns, cover = true)
            FollowingUiState.Empty -> EmptyState(stringResource(R.string.following_empty))
            is FollowingUiState.Error -> ErrorState(appErrorMessage(state.error), onRetry)
            is FollowingUiState.Content -> KeepFocusedVisibleScroll { ChannelsGrid(state, onStreamClick, onChannelClick) }
        }
    }
}

@Composable
private fun ChannelsGrid(
    state: FollowingUiState.Content,
    onStreamClick: (Stream) -> Unit,
    onChannelClick: (FollowedChannel) -> Unit,
) {
    val items = if (state.tab == FollowingTab.Live) state.live.size else state.offline.size
    if (items == 0) {
        EmptyState(stringResource(if (state.tab == FollowingTab.Live) R.string.following_nobody_live else R.string.following_all_live))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(Columns),
        contentPadding = PaddingValues(
            start = TwiDimens.GridFocusInset,
            end = TwiDimens.GridFocusInset,
            top = TwiDimens.GridTopPadding,
            bottom = TwiDimens.FocusOverflowPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChannelGridSpacing),
        verticalArrangement = Arrangement.spacedBy(TwiDimens.ChannelGridSpacing),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TwiDimens.ContentStartPadding - TwiDimens.GridFocusInset)
            .focusRestorer(),
    ) {
        if (state.tab == FollowingTab.Live) {
            itemsIndexed(state.live, key = { _, stream -> "live-" + stream.userId }) { index, stream ->
                ChannelCardItem(
                    name = stream.channelName,
                    avatarUrl = stream.avatarUrl,
                    subtitle = stream.categoryName,
                    liveViewers = formatViewersLabel(stream.viewerCount),
                    first = index == 0,
                    onClick = { onStreamClick(stream) },
                )
            }
        } else {
            itemsIndexed(state.offline, key = { _, channel -> "offline-" + channel.id }) { index, channel ->
                ChannelCardItem(
                    name = channel.displayName,
                    avatarUrl = channel.avatarUrl,
                    subtitle = channel.categoryName,
                    liveViewers = null,
                    first = index == 0,
                    onClick = { onChannelClick(channel) },
                )
            }
        }
    }
}

@Composable
private fun ChannelCardItem(name: String, avatarUrl: String?, subtitle: String, liveViewers: String?, first: Boolean, onClick: () -> Unit) {
    ChannelCard(
        name = name,
        avatarUrl = avatarUrl,
        subtitle = subtitle,
        liveViewers = liveViewers,
        onClick = onClick,
        modifier = if (first) Modifier.requestInitialFocus() else Modifier,
    )
}
