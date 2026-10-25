package app.twicast.tv.feature.player.components

import app.twicast.tv.core.designsystem.component.trackFocus
import app.twicast.tv.core.designsystem.component.rememberFocusProgress
import app.twicast.tv.core.designsystem.component.focusRing
import app.twicast.tv.core.designsystem.component.focusBackground
import androidx.tv.material3.Icon
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
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

/**
 * Bottom panel (mockup "Плеер · панель управления"): channel info and Пауза / Качество / Чат / Канал. A recording
 * also gets its seek bar on top (mockup "Плеер · запись, перемотка"), focused first: Left/Right wind, Down goes
 * to the buttons.
 */
@Composable
fun PlayerControls(
    state: PlayerUiState,
    onTogglePause: () -> Unit,
    onOpenQuality: () -> Unit,
    onToggleChat: () -> Unit,
    onOpenChannel: () -> Unit,
    onSeekStep: (forward: Boolean, repeat: Int) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = state.progress
    val seekable = state.canSeek && progress != null
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.SeekPanelSpacing),
        modifier = modifier
            .fillMaxWidth()
            .background(TwiTheme.colors.overlay, TwiShapes.Hero)
            .padding(horizontal = TwiDimens.PlayerControlsHorizontalPadding, vertical = TwiDimens.PlayerControlsVerticalPadding),
    ) {
        if (seekable && progress != null) {
            Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.SeekHintSpacing)) {
                SeekBar(
                    positionMs = state.shownPositionMs ?: progress.positionMs,
                    durationMs = progress.durationMs,
                    deltaMs = state.seekDeltaMs,
                    focusable = true,
                    onStep = onSeekStep,
                    modifier = Modifier.requestInitialFocus(),
                )
                Text(stringResource(R.string.seek_hint), style = TwiTheme.typography.bodySmall, color = TwiTheme.colors.onOverlayVariant)
            }
        }
        ControlsRow(state, seekable, onTogglePause, onRestart, onOpenQuality, onToggleChat, onOpenChannel)
    }
}

@Composable
private fun ControlsRow(
    state: PlayerUiState,
    seekable: Boolean,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onOpenQuality: () -> Unit,
    onToggleChat: () -> Unit,
    onOpenChannel: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.PlayerControlsSpacing),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val ended = state.status == PlaybackStatus.Ended
        // The channel's avatar and name open its page (no separate «Канал» button); after the broadcast ended it
        // takes the focus, the way on to the recordings.
        ChannelButton(state, onOpenChannel, if (ended) Modifier.weight(1f).requestInitialFocus() else Modifier.weight(1f))
        val paused = state.status == PlaybackStatus.Paused
        TwiSecondaryButton(
            text = stringResource(if (paused) R.string.player_resume else R.string.player_pause),
            icon = if (paused) TwiIcons.Play else TwiIcons.Pause,
            onClick = onTogglePause,
            // A recording's panel starts on its seek bar instead.
            modifier = if (seekable || ended) Modifier else Modifier.requestInitialFocus(),
        )
        // Resumed where the viewer stopped (mockup "Плеер · продолжаем с места").
        if (state.resumedFromMs != null) {
            TwiSecondaryButton(text = stringResource(R.string.player_restart), icon = TwiIcons.Restart, onClick = onRestart)
        }
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
    }
}

/** Avatar, name and "category · viewers" as one focusable block that opens the channel page. */
@Composable
private fun ChannelButton(state: PlayerUiState, onOpenChannel: () -> Unit, modifier: Modifier) {
    val colors = TwiTheme.colors
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.PlayerControlsSpacing),
        modifier = modifier
            .semantics { contentDescription = state.channel.channelName }
            .focusBackground(focusProgress, colors.onOverlay.copy(alpha = ChannelFocusAlpha), TwiShapes.Card)
            .focusRing(focusProgress, colors.primary, TwiShapes.Card, TwiDimens.FocusBorderWidth)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onOpenChannel)
            .padding(TwiDimens.PlayerChannelPadding),
    ) {
        RingedAvatar(state.channel.avatarUrl, TwiDimens.PlayerAvatarSize)
        Column(Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.channel.channelName, style = TwiTheme.typography.channelName, color = colors.onOverlay, maxLines = 1)
                Icon(
                    TwiIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.onOverlayVariant,
                    modifier = Modifier.padding(start = TwiDimens.KeySpacing).size(TwiDimens.NotesBadgeIconSize),
                )
            }
            Text(
                text = listOfNotNull(
                    state.channel.categoryName.ifEmpty { null },
                    state.channel.viewerCount?.let { formatViewersLabel(it) },
                ).joinToString(" · "),
                style = TwiTheme.typography.bodySmall,
                color = colors.onOverlayVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The focused channel block: a faint light plate under the ring. */
private const val ChannelFocusAlpha = 0.12f
