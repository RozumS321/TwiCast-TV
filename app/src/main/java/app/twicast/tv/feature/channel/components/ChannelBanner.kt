package app.twicast.tv.feature.channel.components

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.LiveBadge
import app.twicast.tv.core.designsystem.component.TwiPrimaryButton
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.format.formatPlaybackTime
import app.twicast.tv.core.designsystem.format.formatViewerCount
import app.twicast.tv.core.designsystem.format.formatViewersLabel
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.ChannelPage
import coil3.compose.AsyncImage

private const val OfflineAvatarAlpha = 0.75f
private const val OfflineSaturation = 0.4f

/**
 * Channel header (mockup "Канал"): the channel's offline image as a banner under a dark plate, avatar,
 * name, status line, the watch button and "Подписаться" / "В подписках" ([onFollow] opens the QR to do it).
 * [onWatch] is null when there is nothing to play (offline and no recordings).
 */
@Composable
fun ChannelBanner(
    channel: ChannelPage,
    onWatch: (() -> Unit)?,
    onFollow: () -> Unit,
    modifier: Modifier = Modifier,
    /** Offline, the latest recording was started: the button says where it goes on from. */
    continueFromMs: Long? = null,
) {
    val colors = TwiTheme.colors
    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = modifier
            .fillMaxWidth()
            .height(TwiDimens.ChannelBannerHeight)
            .clip(TwiShapes.Banner)
            .background(colors.inverseSurface),
    ) {
        if (channel.bannerUrl != null) {
            AsyncImage(model = channel.bannerUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        // Keeps white text readable on any banner image.
        Box(Modifier.fillMaxSize().background(colors.overlay))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChannelHeaderSpacing),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TwiDimens.ChannelBannerHorizontalPadding,
                    end = TwiDimens.ChannelBannerHorizontalPadding,
                    bottom = TwiDimens.ChannelBannerBottomPadding,
                ),
        ) {
            BannerAvatar(channel.avatarUrl, live = channel.stream != null)
            ChannelInfo(channel, Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwiDimens.ButtonSpacing)) {
                if (onWatch != null) {
                    TwiPrimaryButton(
                        text = when {
                            channel.stream != null -> stringResource(R.string.channel_watch_live)
                            continueFromMs != null -> stringResource(R.string.channel_watch_continue, formatPlaybackTime(continueFromMs))
                            else -> stringResource(R.string.channel_watch_last)
                        },
                        onClick = onWatch,
                        icon = TwiIcons.Play,
                        modifier = Modifier.requestInitialFocus(),
                    )
                }
                TwiSecondaryButton(
                    text = stringResource(if (channel.isFollowedByViewer) R.string.channel_following else R.string.channel_follow),
                    onClick = onFollow,
                    icon = if (channel.isFollowedByViewer) TwiIcons.Check else TwiIcons.Plus,
                    // Nothing to watch: the follow button is the way in.
                    modifier = if (onWatch == null) Modifier.requestInitialFocus() else Modifier,
                )
            }
        }
    }
}

@Composable
private fun ChannelInfo(channel: ChannelPage, modifier: Modifier) {
    val colors = TwiTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.ChannelInfoSpacing), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwiDimens.ButtonSpacing)) {
            Text(channel.displayName, style = TwiTheme.typography.screenTitle, color = colors.onOverlay, maxLines = 1)
            if (channel.stream != null) LiveBadge(large = true)
        }
        Text(statusLine(channel), style = TwiTheme.typography.body, color = colors.onOverlayVariant, maxLines = 1)
    }
}

/**
 * Live: "Категория · N зрителей · N подписчиков"; offline: "Офлайн · следующий эфир завтра в 18:00 · N подписчиков",
 * or "был в эфире вчера" without a schedule.
 */
@Composable
private fun statusLine(channel: ChannelPage): String {
    val followers = channel.followerCount?.let { pluralStringResource(R.plurals.channel_followers, it, formatViewerCount(it)) }
    val stream = channel.stream
    val parts = if (stream != null) {
        listOf(stream.categoryName, formatViewersLabel(stream.viewerCount), followers)
    } else {
        // The next scheduled stream says more than the last one.
        val next = channel.schedule.firstOrNull()?.let { nextStreamLabel(it) }
        val lastLive = next ?: channel.videos.firstOrNull()?.createdAtMillis?.let { createdAt ->
            val relative = remember(createdAt) {
                DateUtils.getRelativeTimeSpanString(createdAt, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString().lowercase()
            }
            stringResource(R.string.channel_last_live, relative)
        }
        listOf(stringResource(R.string.channel_offline), lastLive, followers)
    }
    return parts.filterNotNull().filter { it.isNotEmpty() }.joinToString(" · ")
}

/** 112 avatar; live: red ring with a gap, offline: faded. */
@Composable
private fun BannerAvatar(avatarUrl: String?, live: Boolean) {
    val colors = TwiTheme.colors
    val ring = TwiDimens.ChannelLargeAvatarRing
    Box(
        modifier = Modifier
            .size(TwiDimens.ChannelLargeAvatarSize)
            .then(if (live) Modifier.border(ring, colors.live, CircleShape).padding(ring * 2) else Modifier.alpha(OfflineAvatarAlpha))
            .clip(CircleShape)
            .background(colors.surfaceVariant),
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = if (live) null else remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(OfflineSaturation) }) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
