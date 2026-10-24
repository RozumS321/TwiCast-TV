package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import coil3.compose.AsyncImage

/**
 * Live stream card: 16:9 preview with LIVE and viewer badges, avatar, [title] and a one-line [subtitle]
 * (e.g. "channel · category", or the category when the title is the channel).
 * [dense] is the home-row variant (212 wide, tighter spacing, 30 avatar); [width] overrides it in grids.
 * [avatarRing] marks followed channels. Focus: scale 1.06, 3dp ring, soft primary shadow — all draw-phase.
 */
/** Unwatched part of a recording's strip: the overlay text color, faint. */
private const val WatchedTrackAlpha = 0.28f

@Composable
fun StreamCard(
    title: String,
    subtitle: String,
    viewerCountText: String,
    thumbnailUrl: String?,
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
    width: Dp = if (dense) TwiDimens.StreamCardDenseWidth else TwiDimens.StreamCardWidth,
    avatarRing: Boolean = false,
    /** Recordings and clips: no LIVE badge, and [viewerCountText] carries the duration. */
    live: Boolean = true,
    showAvatar: Boolean = true,
    /** Recordings: the watched share, a strip along the thumbnail's bottom (mockup "Канал · записи с прогрессом"). */
    watchedFraction: Float? = null,
) {
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    val colors = TwiTheme.colors
    val spacing = if (dense) TwiDimens.CardContentSpacingDense else TwiDimens.CardContentSpacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
            .width(width)
            .focusTransform(
                progress = focusProgress,
                focusedScale = TwiScale.CardFocused,
                shape = TwiShapes.Card,
                shadowElevation = TwiDimens.FocusShadowElevation,
                shadowColor = colors.primary,
            )
            .focusBackground(focusProgress, colors.surface, TwiShapes.Card)
            .focusRing(focusProgress, colors.primary, TwiShapes.Card, TwiDimens.FocusBorderWidth)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(bottom = spacing),
    ) {
        StreamThumbnail(thumbnailUrl, viewerCountText, live, watchedFraction)
        StreamCardInfo(title, subtitle, avatarUrl.takeIf { showAvatar }, dense, avatarRing, showAvatar)
    }
}

@Composable
private fun StreamThumbnail(thumbnailUrl: String?, viewerCountText: String, live: Boolean, watchedFraction: Float?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(TwiAspect.Thumbnail)
            .clip(TwiShapes.Thumbnail)
            .background(TwiTheme.colors.surfaceVariant),
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (live) LiveBadge(Modifier.align(Alignment.TopStart).padding(TwiDimens.BadgeInset))
        ViewerCountBadge(viewerCountText, Modifier.align(Alignment.BottomStart).padding(TwiDimens.BadgeInset))
        if (watchedFraction != null) {
            val colors = TwiTheme.colors
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(TwiDimens.WatchedStripHeight)
                    .background(colors.onOverlay.copy(alpha = WatchedTrackAlpha)),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(watchedFraction).background(colors.primary))
            }
        }
    }
}

@Composable
private fun StreamCardInfo(
    title: String,
    subtitle: String,
    avatarUrl: String?,
    dense: Boolean,
    avatarRing: Boolean,
    showAvatar: Boolean,
) {
    val avatarSize = if (dense) TwiDimens.AvatarSmallDense else TwiDimens.AvatarSmall
    Row(
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.CardContentSpacing),
        modifier = Modifier.padding(horizontal = TwiDimens.CardTextHorizontalPadding),
    ) {
        if (showAvatar) {
            if (avatarRing) RingedAvatar(avatarUrl, avatarSize) else Avatar(avatarUrl, avatarSize)
        }
        Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.CardTextSpacing)) {
            Text(
                text = title,
                style = TwiTheme.typography.cardTitle,
                color = TwiTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = if (dense) TwiTheme.typography.captionDense else TwiTheme.typography.caption,
                color = TwiTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Avatar inside a white gap and a primary ring (followed channel); keeps the outer [size]. */
@Composable
fun RingedAvatar(imageUrl: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .border(TwiDimens.AvatarRingWidth, TwiTheme.colors.primary, CircleShape)
            .padding(TwiDimens.AvatarRingWidth)
            .border(TwiDimens.AvatarRingWidth, TwiTheme.colors.surface, CircleShape)
            .padding(TwiDimens.AvatarRingWidth),
    ) {
        Avatar(imageUrl, size - TwiDimens.AvatarRingWidth * 4)
    }
}

@Composable
fun Avatar(imageUrl: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(TwiTheme.colors.surfaceVariant),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}
