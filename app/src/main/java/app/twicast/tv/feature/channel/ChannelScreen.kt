package app.twicast.tv.feature.channel

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.EmptyState
import app.twicast.tv.core.designsystem.component.ErrorState
import app.twicast.tv.core.designsystem.component.KeepFocusedVisibleScroll
import app.twicast.tv.core.designsystem.component.SkeletonBlock
import app.twicast.tv.core.designsystem.component.StreamCard
import app.twicast.tv.core.designsystem.component.StreamCardSkeleton
import app.twicast.tv.core.designsystem.component.rememberSkeletonPulse
import app.twicast.tv.core.designsystem.format.formatViewerCount
import app.twicast.tv.core.designsystem.image.rememberTwitchImageUrl
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.ChannelPage
import app.twicast.tv.data.model.Clip
import app.twicast.tv.data.model.Video
import app.twicast.tv.feature.channel.components.ChannelBanner
import app.twicast.tv.feature.channel.components.FollowQrDialog
import app.twicast.tv.feature.common.appErrorMessage
import java.text.NumberFormat
import kotlinx.coroutines.launch

private const val SkeletonCards = 5
private const val SecondsInMinute = 60
private const val SecondsInHour = 3600

// The row itself starts just short of the content line and clips there: scrolled cards leave a gap to the sidebar
// instead of sliding up to it, and the inset is room for the first card's focus scale.
private val RowPadding = PaddingValues(
    start = TwiDimens.GridFocusInset,
    end = TwiDimens.ContentEndPadding,
    top = TwiDimens.HomeRowTitleSpacing,
    bottom = TwiDimens.FocusOverflowPadding,
)

/** Mockups "Канал · в эфире" / "Канал · офлайн": banner, "Информация", recordings and clips. */
@Composable
fun ChannelScreen(
    state: ChannelUiState,
    onWatchLive: (ChannelPage) -> Unit,
    onVideoClick: (ChannelPage, Video) -> Unit,
    onClipClick: (ChannelPage, Clip) -> Unit,
    onRetry: () -> Unit,
    onFollow: () -> Unit,
    onFollowDialogClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ChannelUiState.Loading -> ChannelSkeleton(modifier)
        is ChannelUiState.Error -> ErrorState(appErrorMessage(state.error), onRetry, modifier.fillMaxSize())
        is ChannelUiState.Content -> {
            ChannelContent(state.channel, onWatchLive, onVideoClick, onClipClick, onFollow, modifier)
            if (state.followDialog) {
                FollowQrDialog(
                    login = state.channel.login,
                    displayName = state.channel.displayName,
                    followed = state.channel.isFollowedByViewer,
                    onDone = onFollowDialogClosed,
                )
            }
        }
    }
}

@Composable
private fun ChannelContent(
    channel: ChannelPage,
    onWatchLive: (ChannelPage) -> Unit,
    onVideoClick: (ChannelPage, Video) -> Unit,
    onClipClick: (ChannelPage, Clip) -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier,
) {
    // Live: the stream; offline: the latest recording; neither: no button.
    val lastVideo = channel.videos.firstOrNull()
    val onWatch: (() -> Unit)? = when {
        channel.stream != null -> { { onWatchLive(channel) } }
        lastVideo != null -> { { onVideoClick(channel, lastVideo) } }
        else -> null
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    KeepFocusedVisibleScroll {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(TwiDimens.ChannelSectionSpacing - TwiDimens.FocusOverflowPadding),
            contentPadding = PaddingValues(top = TwiDimens.ScreenTopPadding, bottom = TwiDimens.ScreenTopPadding),
            modifier = modifier.fillMaxSize(),
        ) {
            item(key = "banner") {
                ChannelBanner(
                    channel = channel,
                    onWatch = onWatch,
                    onFollow = onFollow,
                    modifier = Modifier
                        .padding(start = TwiDimens.ContentStartPadding, end = TwiDimens.ContentEndPadding)
                        // The focused button sits low in the banner: bring the whole banner back, not just the button.
                        .onFocusChanged { if (it.hasFocus) scope.launch { listState.animateScrollToItem(0) } },
                )
            }
            if (channel.description.isNotBlank() || channel.followerCount != null) {
                item(key = "about") {
                    AboutBlock(
                        channel,
                        Modifier.padding(
                            start = TwiDimens.ContentStartPadding,
                            end = TwiDimens.ContentEndPadding,
                            top = TwiDimens.ChannelSectionSpacing,
                            bottom = TwiDimens.FocusOverflowPadding,
                        ),
                    )
                }
            }
            if (channel.videos.isNotEmpty()) {
                item(key = "videos") {
                    MediaRow(stringResource(R.string.channel_videos), stringResource(R.string.channel_videos_hint)) {
                        items(channel.videos, key = { it.id }) { video ->
                            StreamCard(
                                title = video.title,
                                subtitle = videoCaption(video),
                                viewerCountText = formatDuration(video.durationSeconds),
                                thumbnailUrl = rememberTwitchImageUrl(video.thumbnailTemplate, TwiDimens.StreamCardDenseWidth, TwiAspect.Thumbnail),
                                avatarUrl = null,
                                onClick = { onVideoClick(channel, video) },
                                dense = true,
                                live = false,
                                showAvatar = false,
                            )
                        }
                    }
                }
            }
            if (channel.clips.isNotEmpty()) {
                item(key = "clips") {
                    MediaRow(stringResource(R.string.channel_clips), stringResource(R.string.channel_clips_hint)) {
                        items(channel.clips, key = { it.id }) { clip ->
                            StreamCard(
                                title = clip.title,
                                subtitle = pluralStringResource(R.plurals.channel_views, clip.viewCount, formatViewerCount(clip.viewCount)),
                                viewerCountText = formatDuration(clip.durationSeconds),
                                thumbnailUrl = clip.thumbnailUrl,
                                avatarUrl = null,
                                onClick = { onClipClick(channel, clip) },
                                dense = true,
                                live = false,
                                showAvatar = false,
                            )
                        }
                    }
                }
            }
            if (channel.videos.isEmpty() && channel.clips.isEmpty()) {
                item(key = "empty") { EmptyState(stringResource(R.string.channel_no_media), Modifier.fillMaxWidth()) }
            }
        }
    }
}

/** Like the "Информация" panel on twitch.tv: exact follower count and the full description. */
@Composable
private fun AboutBlock(channel: ChannelPage, modifier: Modifier) {
    val colors = TwiTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.ChannelAboutSpacing),
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, TwiShapes.Card)
            .padding(TwiDimens.ChannelAboutPadding),
    ) {
        Text(stringResource(R.string.channel_about), style = TwiTheme.typography.rowTitleDense, color = colors.onSurface)
        channel.followerCount?.let { count ->
            val exact = remember(count) { NumberFormat.getIntegerInstance().format(count) }
            Text(pluralStringResource(R.plurals.channel_followers, count, exact), style = TwiTheme.typography.bodyStrong, color = colors.onSurface)
        }
        if (channel.description.isNotBlank()) {
            Text(
                text = channel.description,
                style = TwiTheme.typography.body,
                color = colors.onSurfaceVariant,
                modifier = Modifier.widthIn(max = TwiDimens.ChannelAboutMaxWidth),
            )
        }
    }
}

@Composable
private fun MediaRow(title: String, hint: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.RowHintSpacing),
            modifier = Modifier.padding(start = TwiDimens.ContentStartPadding),
        ) {
            Text(title, style = TwiTheme.typography.rowTitleDense, color = TwiTheme.colors.onSurface, modifier = Modifier.alignByBaseline())
            Text(hint, style = TwiTheme.typography.caption, color = TwiTheme.colors.onSurfaceVariant, modifier = Modifier.alignByBaseline())
        }
        LazyRow(
            contentPadding = RowPadding,
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.HomeCardSpacing),
            modifier = Modifier.padding(start = TwiDimens.ContentStartPadding - TwiDimens.GridFocusInset).focusRestorer(),
            content = content,
        )
    }
}

/** "вчера · 48 тыс просмотров". */
@Composable
private fun videoCaption(video: Video): String {
    val views = pluralStringResource(R.plurals.channel_views, video.viewCount, formatViewerCount(video.viewCount))
    val createdAt = video.createdAtMillis ?: return views
    val relative = remember(createdAt) {
        DateUtils.getRelativeTimeSpanString(createdAt, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString().lowercase()
    }
    return "$relative · $views"
}

/** 3:42:10 or 0:58. */
internal fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / SecondsInHour
    val minutes = totalSeconds % SecondsInHour / SecondsInMinute
    val seconds = totalSeconds % SecondsInMinute
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/** Banner block plus a row of cards, one shared pulse: the page does not sit blank while it loads. */
@Composable
private fun ChannelSkeleton(modifier: Modifier) {
    val pulse = rememberSkeletonPulse()
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.ChannelSectionSpacing),
        modifier = modifier
            .fillMaxSize()
            .padding(start = TwiDimens.ContentStartPadding, end = TwiDimens.ContentEndPadding, top = TwiDimens.ScreenTopPadding),
    ) {
        SkeletonBlock(pulse, TwiShapes.Banner, Modifier.fillMaxWidth().height(TwiDimens.ChannelBannerHeight))
        // Unbounded, as on home: cards past the edge keep their width instead of turning into a huge 16:9 block.
        Row(
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.HomeCardSpacing),
            modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
        ) {
            repeat(SkeletonCards) { StreamCardSkeleton(pulse, width = TwiDimens.StreamCardDenseWidth) }
        }
    }
}
