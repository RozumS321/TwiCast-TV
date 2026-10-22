package app.twicast.tv.feature.following.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.component.focusRing
import app.twicast.tv.core.designsystem.component.focusTransform
import app.twicast.tv.core.designsystem.component.rememberFocusProgress
import app.twicast.tv.core.designsystem.component.trackFocus
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import coil3.compose.AsyncImage

private const val OfflineSaturation = 0.3f
private const val OfflineAlpha = 0.7f

/**
 * Followed channel (mockup "Подписки"): avatar 84 — red ring when live, faded when offline — name,
 * live viewers, and the category. Focus as on stream cards.
 */
@Composable
fun ChannelCard(
    name: String,
    avatarUrl: String?,
    subtitle: String,
    liveViewers: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwiTheme.colors
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwiDimens.ChannelCardSpacing),
        modifier = modifier
            .fillMaxWidth()
            .focusTransform(
                progress = focusProgress,
                focusedScale = TwiScale.CardFocused,
                shape = TwiShapes.ChannelCard,
                shadowElevation = TwiDimens.FocusShadowElevation,
                shadowColor = colors.primary,
            )
            .background(colors.surface, TwiShapes.ChannelCard)
            .border(TwiDimens.OutlineWidth, colors.outlineVariant, TwiShapes.ChannelCard)
            .focusRing(focusProgress, colors.primary, TwiShapes.ChannelCard, TwiDimens.FocusBorderWidth)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(
                start = TwiDimens.ChannelCardSidePadding,
                end = TwiDimens.ChannelCardSidePadding,
                top = TwiDimens.ChannelCardTopPadding,
                bottom = TwiDimens.ChannelCardBottomPadding,
            ),
    ) {
        ChannelAvatar(avatarUrl, live = liveViewers != null)
        Text(name, style = TwiTheme.typography.channelCardName, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (liveViewers != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChannelCardSpacing)) {
                Box(Modifier.size(TwiDimens.LiveDotSize).background(colors.live, CircleShape))
                Text(liveViewers, style = TwiTheme.typography.liveCount, color = colors.live)
            }
        }
        Text(subtitle, style = TwiTheme.typography.captionDense, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChannelAvatar(avatarUrl: String?, live: Boolean) {
    val colors = TwiTheme.colors
    val ring = TwiDimens.ChannelAvatarRing
    Box(
        modifier = Modifier
            .padding(bottom = TwiDimens.ChannelCardSpacing)
            .size(TwiDimens.ChannelAvatarSize)
            .then(
                if (live) {
                    Modifier.border(ring, colors.live, CircleShape).padding(ring).border(ring, colors.surface, CircleShape).padding(ring)
                } else {
                    Modifier.alpha(OfflineAlpha)
                },
            )
            .clip(CircleShape)
            .background(colors.surfaceVariant),
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // Offline: mostly grey, as on the mockup.
                colorFilter = if (live) null else remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(OfflineSaturation) }) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
