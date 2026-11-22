package app.twicast.tv.core.designsystem.component

import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import app.twicast.tv.core.designsystem.theme.GeistMono
import androidx.compose.ui.unit.dp
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
    /** Live streams: how long they are on ("2 ч"), bottom right beside the viewers. */
    uptimeText: String? = null,
) {
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    val colors = TwiTheme.colors
    val spacing = if (dense) TwiDimens.CardContentSpacingDense else TwiDimens.CardContentSpacing
    // The player grows out of this card when it opens.
    val placed = remember { arrayOfNulls<LayoutCoordinates>(1) }

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
            .onPlaced { placed[0] = it }
            .clickable(interactionSource = interactionSource, indication = null) {
                LaunchOrigin.from(placed[0])
                onClick()
            }
            .padding(bottom = spacing),
    ) {
        // Canvas "Карточка · зрители на превью": live viewers sit on the preview with the live lamp, light on a dark
        // pill (they were too faint under it). The time stays in the other corner; a recording shows its length there.
        StreamThumbnail(
            thumbnailUrl,
            live,
            watchedFraction,
            cornerText = uptimeText ?: viewerCountText.takeIf { !live },
            liveViewers = viewerCountText.takeIf { live && it.isNotBlank() },
        )
        StreamCardInfo(title, subtitle, null, avatarUrl.takeIf { showAvatar }, dense, avatarRing, showAvatar)
    }
}

@Composable
private fun StreamThumbnail(
    thumbnailUrl: String?,
    live: Boolean,
    watchedFraction: Float?,
    cornerText: String?,
    liveViewers: String?,
) {
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
        if (live) {
            val corner = Modifier.align(Alignment.TopStart).padding(TwiDimens.BadgeInset)
            if (liveViewers != null) ViewersBadge(liveViewers, corner) else LiveLamp(corner)
        }
        // Above the watched strip, when there is one.
        val badgeLift = if (watchedFraction != null) TwiDimens.WatchedStripHeight else 0.dp
        if (cornerText != null) {
            TimeBadge(cornerText, Modifier.align(Alignment.BottomEnd).padding(TwiDimens.BadgeInset).padding(bottom = badgeLift))
        }
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
    viewers: String?,
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
            Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.CardMetaGap)) {
                Text(
                    text = subtitle,
                    style = if (dense) TwiTheme.typography.captionDense else TwiTheme.typography.caption,
                    color = TwiTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (viewers != null) {
                    Text(
                        text = viewers,
                        style = (if (dense) TwiTheme.typography.captionDense else TwiTheme.typography.caption).copy(fontFeatureSettings = "tnum"),
                        color = TwiTheme.colors.onSurfaceFaint,
                        maxLines = 1,
                    )
                }
            }
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

/** The "в эфире" lamp on a preview: a small red dot with a dark rim so it reads on any picture. */
@Composable
private fun LiveLamp(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(TwiDimens.LiveLampSize + TwiDimens.LiveLampRim * 2)
            .background(LampRim, CircleShape)
            .padding(TwiDimens.LiveLampRim)
            .background(TwiTheme.colors.live, CircleShape),
    )
}

/** "● 18 тыс" on a live preview: the live lamp and the viewers, light figures of one width on a dark pill. */
@Composable
private fun ViewersBadge(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.TimeBadgeHorizontalPadding),
        modifier = modifier
            .background(TwiTheme.colors.overlay, TwiShapes.Badge)
            .padding(horizontal = TwiDimens.TimeBadgeHorizontalPadding, vertical = TwiDimens.TimeBadgeVerticalPadding),
    ) {
        Box(Modifier.size(TwiDimens.LiveLampSize).background(TwiTheme.colors.live, CircleShape))
        Text(
            text = text,
            style = TwiTheme.typography.captionDense.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
            color = TwiTheme.colors.onOverlay,
            maxLines = 1,
        )
    }
}

/** "3:12" / "1:12 осталось" in the corner of a preview, figures of one width. */
@Composable
private fun TimeBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TwiTheme.typography.captionDense.copy(fontFamily = GeistMono),
        color = TwiTheme.colors.onOverlay,
        maxLines = 1,
        modifier = modifier
            .background(TwiTheme.colors.overlay, TwiShapes.Badge)
            .padding(horizontal = TwiDimens.TimeBadgeHorizontalPadding, vertical = TwiDimens.TimeBadgeVerticalPadding),
    )
}

private val LampRim = Color(0x59000000)
