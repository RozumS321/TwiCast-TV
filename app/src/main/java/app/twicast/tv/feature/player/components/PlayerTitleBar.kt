package app.twicast.tv.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.LiveBadge
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme

private const val TitleMaxLines = 2

/**
 * Mockup "Плеер · панель управления": the stream's title over the top of the video while the controls are up,
 * on a dark fade so any picture keeps it readable. LIVE for a broadcast, "ЗАПИСЬ" for a recording.
 */
@Composable
fun PlayerTitleBar(title: String, live: Boolean, modifier: Modifier = Modifier) {
    val colors = TwiTheme.colors
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.PlayerTitleGap),
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(colors.overlay, colors.overlay.copy(alpha = 0f))))
            .padding(
                start = TwiDimens.PlayerOverlayPadding,
                end = TwiDimens.PlayerOverlayPadding,
                top = TwiDimens.PlayerTitleTopPadding,
                bottom = TwiDimens.PlayerTitleBottomPadding,
            ),
    ) {
        if (live) {
            LiveBadge(Modifier.padding(top = TwiDimens.PlayerTitleBadgeOffset))
        } else {
            Text(
                stringResource(R.string.player_recording_badge),
                style = TwiTheme.typography.tag,
                color = colors.onOverlay,
                modifier = Modifier
                    .padding(top = TwiDimens.PlayerTitleBadgeOffset)
                    .background(colors.onOverlay.copy(alpha = RecordingBadgeAlpha), TwiShapes.Badge)
                    .padding(horizontal = TwiDimens.CountHorizontalPadding, vertical = TwiDimens.BadgeVerticalPadding),
            )
        }
        Text(
            title,
            style = TwiTheme.typography.heroTitle,
            color = colors.onOverlay,
            maxLines = TitleMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = TwiDimens.PlayerTitleMaxWidth),
        )
    }
}

private const val RecordingBadgeAlpha = 0.18f
