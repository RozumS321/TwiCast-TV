package app.twicast.tv.feature.following

import app.twicast.tv.core.designsystem.component.leftToSidebar
import app.twicast.tv.core.designsystem.component.isFirstInRow
import app.twicast.tv.core.designsystem.component.LocalSidebarFocus
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.ActionState
import app.twicast.tv.core.designsystem.component.EmptyState
import app.twicast.tv.core.designsystem.component.ErrorState
import app.twicast.tv.core.designsystem.component.KeepFocusedVisibleScroll
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.format.formatViewersLabel
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.FollowedChannel
import app.twicast.tv.data.model.Stream
import app.twicast.tv.feature.common.GridSkeleton
import app.twicast.tv.feature.common.appErrorMessage
import app.twicast.tv.feature.following.components.ChannelCard

/** 24 columns: a live card spans 4 (6 per row), a compact offline card 3 (8 per row), headers the whole line. */
private const val GridColumns = 24
private const val LiveSpan = 4
private const val OfflineSpan = 3
private const val SkeletonColumns = 6

/** Live and offline cards share one type: the grid reuses them across the two sections. */
private object ContentType {
    const val TITLE = "title"
    const val EMPTY = "empty"
    const val CHANNEL = "channel"
}

/** Mockup "Подписки": one screen, "В эфире" first, "Офлайн" below it. */
@Composable
fun FollowingScreen(
    state: FollowingUiState,
    onStreamClick: (Stream) -> Unit,
    onChannelClick: (FollowedChannel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onSignIn: () -> Unit = {},
) {
    Column(modifier.fillMaxSize().padding(top = TwiDimens.ScreenTopPadding)) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().padding(horizontal = TwiDimens.ContentStartPadding),
        ) {
            Text(stringResource(R.string.nav_following), style = TwiTheme.typography.screenTitle, color = TwiTheme.colors.onSurface)
            Spacer(Modifier.weight(1f))
            if (state is FollowingUiState.Content) {
                val total = state.live.size + state.offline.size
                Text(
                    pluralStringResource(R.plurals.following_channels, total, total),
                    style = TwiTheme.typography.body,
                    color = TwiTheme.colors.onSurfaceVariant,
                )
            }
        }
        when (state) {
            FollowingUiState.Loading -> GridSkeleton(columns = SkeletonColumns, cover = true)
            FollowingUiState.Empty -> EmptyState(stringResource(R.string.following_empty))
            FollowingUiState.Guest -> ActionState(
                title = stringResource(R.string.following_guest_title),
                message = stringResource(R.string.following_guest_text),
                actionText = stringResource(R.string.following_guest_sign_in),
                onAction = onSignIn,
            )
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
    val sidebar = LocalSidebarFocus.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(GridColumns),
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
        item(key = "live-title", span = { GridItemSpan(maxLineSpan) }, contentType = ContentType.TITLE) {
            SectionTitle(stringResource(R.string.following_live), state.live.size, live = true)
        }
        if (state.live.isEmpty()) {
            item(key = "live-empty", span = { GridItemSpan(maxLineSpan) }, contentType = ContentType.EMPTY) {
                Text(stringResource(R.string.following_nobody_live), style = TwiTheme.typography.body, color = TwiTheme.colors.onSurfaceVariant)
            }
        }
        itemsIndexed(
            state.live,
            key = { _, stream -> "live-" + stream.userId },
            span = { _, _ -> GridItemSpan(LiveSpan) },
            contentType = { _, _ -> ContentType.CHANNEL },
        ) { index, stream ->
            ChannelCardItem(
                name = stream.channelName,
                avatarUrl = stream.avatarUrl,
                subtitle = stream.categoryName,
                liveViewers = formatViewersLabel(stream.viewerCount),
                first = index == 0,
                onClick = { onStreamClick(stream) },
                modifier = Modifier.leftToSidebar(sidebar, isFirstInRow(index, GridColumns / LiveSpan)),
            )
        }
        if (state.offline.isNotEmpty()) {
            item(key = "offline-title", span = { GridItemSpan(maxLineSpan) }, contentType = ContentType.TITLE) {
                SectionTitle(
                    stringResource(R.string.following_offline),
                    state.offline.size,
                    live = false,
                    Modifier.padding(top = TwiDimens.FollowingSectionSpacing - TwiDimens.ChannelGridSpacing),
                )
            }
        }
        itemsIndexed(
            state.offline,
            key = { _, channel -> "offline-" + channel.id },
            span = { _, _ -> GridItemSpan(OfflineSpan) },
            contentType = { _, _ -> ContentType.CHANNEL },
        ) { index, channel ->
            ChannelCardItem(
                name = channel.displayName,
                avatarUrl = channel.avatarUrl,
                subtitle = channel.categoryName,
                liveViewers = null,
                // Nobody live: the first offline channel takes the initial focus.
                first = state.live.isEmpty() && index == 0,
                onClick = { onChannelClick(channel) },
                modifier = Modifier.leftToSidebar(sidebar, isFirstInRow(index, GridColumns / OfflineSpan)),
            )
        }
    }
}

/** "● В эфире 4" / "Офлайн 16". */
@Composable
private fun SectionTitle(title: String, count: Int, live: Boolean, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.RowHintSpacing),
        modifier = modifier,
    ) {
        if (live) Box(Modifier.size(TwiDimens.LiveDotSize).background(TwiTheme.colors.live, CircleShape))
        Text(title, style = TwiTheme.typography.rowTitleDense, color = TwiTheme.colors.onSurface)
        Text(count.toString(), style = TwiTheme.typography.bodyStrong, color = TwiTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun ChannelCardItem(
    name: String,
    avatarUrl: String?,
    subtitle: String,
    liveViewers: String?,
    first: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChannelCard(
        name = name,
        avatarUrl = avatarUrl,
        subtitle = subtitle,
        liveViewers = liveViewers,
        onClick = onClick,
        modifier = if (first) modifier.requestInitialFocus() else modifier,
    )
}
