package app.twicast.tv.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.RingedAvatar
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.format.formatViewersLabel
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.feature.player.PlaybackStatus
import app.twicast.tv.feature.player.PlayerUiState

/** Bottom panel (mockup "Плеер · панель управления"): channel info and Пауза / Качество / Чат / Канал. */
@Composable
fun PlayerControls(
    state: PlayerUiState,
    onTogglePause: () -> Unit,
    onOpenQuality: () -> Unit,
    onToggleChat: () -> Unit,
    onOpenChannel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.PlayerControlsSpacing),
        modifier = modifier
            .fillMaxWidth()
            .background(TwiTheme.colors.overlay, TwiShapes.Hero)
            .padding(horizontal = TwiDimens.PlayerControlsHorizontalPadding, vertical = TwiDimens.PlayerControlsVerticalPadding),
    ) {
        RingedAvatar(state.channel.avatarUrl, TwiDimens.PlayerAvatarSize)
        Column(Modifier.weight(1f)) {
            Text(state.channel.channelName, style = TwiTheme.typography.channelName, color = TwiTheme.colors.onOverlay, maxLines = 1)
            Text(
                text = listOfNotNull(
                    state.channel.categoryName.ifEmpty { null },
                    state.channel.viewerCount?.let { formatViewersLabel(it) },
                ).joinToString(" · "),
                style = TwiTheme.typography.bodySmall,
                color = TwiTheme.colors.onOverlayVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val paused = state.status == PlaybackStatus.Paused
        TwiSecondaryButton(
            text = stringResource(if (paused) R.string.player_resume else R.string.player_pause),
            icon = if (paused) TwiIcons.Play else TwiIcons.Pause,
            onClick = onTogglePause,
            modifier = Modifier.requestInitialFocus(),
        )
        TwiSecondaryButton(
            text = stringResource(R.string.player_quality_button, state.currentQualityLabel ?: stringResource(R.string.player_quality_auto)),
            icon = TwiIcons.Quality,
            onClick = onOpenQuality,
        )
        if (state.channel.isLive) {
            TwiSecondaryButton(
                text = stringResource(if (state.chatVisible) R.string.player_chat_hide else R.string.player_chat_show),
                icon = TwiIcons.Chat,
                onClick = onToggleChat,
            )
        }
        TwiSecondaryButton(text = stringResource(R.string.player_channel), icon = TwiIcons.Profile, onClick = onOpenChannel)
    }
}
