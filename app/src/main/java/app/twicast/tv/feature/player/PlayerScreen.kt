package app.twicast.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.feature.player.components.PlayerControls
import app.twicast.tv.feature.player.components.QualityPanel
import app.twicast.tv.feature.player.components.TwitchEmbedController
import app.twicast.tv.feature.player.components.TwitchEmbedListener
import app.twicast.tv.feature.player.components.TwitchEmbedView
import app.twicast.tv.feature.player.components.VideoQuality

/** Remote keys that only reveal the controls when the video is bare. */
private val RevealKeys = setOf(Key.DirectionCenter, Key.Enter, Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight, Key.Menu)

/**
 * Full-screen stream (mockups "Плеер · …"). Bare video: any key shows the controls, Play/Pause toggles.
 * Back closes the quality menu, then the controls, then the chat, then the screen.
 */
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    controller: TwitchEmbedController,
    listener: TwitchEmbedListener,
    onRemoteActivity: () -> Unit,
    onOpenQuality: () -> Unit,
    onQualitySelected: (VideoQuality) -> Unit,
    onToggleChat: () -> Unit,
    onOpenChannel: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootFocus = remember { FocusRequester() }
    BackHandler(enabled = state.overlay != PlayerOverlay.Hidden || state.chatVisible, onBack = onBack)
    // Bare video keeps focus on the root so the next key press reaches it.
    LaunchedEffect(state.overlay) { if (state.overlay == PlayerOverlay.Hidden) rootFocus.requestFocus() }
    ChatSync(state.chatVisible, controller)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TwiTheme.colors.playerBackground)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.MediaPlayPause -> {
                        controller.togglePause()
                        true
                    }
                    event.key == Key.Back -> false
                    else -> {
                        onRemoteActivity()
                        // From bare video the first press only reveals the controls.
                        state.overlay == PlayerOverlay.Hidden && event.key in RevealKeys
                    }
                }
            }
            .focusRequester(rootFocus)
            .focusable(),
    ) {
        if (state.canPlay && state.settingsLoaded) {
            // A new attempt after a renderer crash: a fresh WebView (a dead one cannot be reused).
            key(state.embedAttempt) {
                TwitchEmbedView(
                    channelLogin = state.channel.channelLogin,
                    videoId = state.channel.videoId,
                    clipSlug = state.channel.clipSlug,
                    preferredQualityId = state.preferredQualityId,
                    maxVideoHeight = state.maxVideoHeight,
                    controller = controller,
                    listener = listener,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (state.status != PlaybackStatus.Playing) {
            StatusPlate(state, Modifier.align(Alignment.TopStart).padding(TwiDimens.PlayerOverlayPadding))
        }
        when (state.overlay) {
            PlayerOverlay.Hidden -> Unit
            PlayerOverlay.Controls -> PlayerControls(
                state = state,
                onTogglePause = controller::togglePause,
                onOpenQuality = onOpenQuality,
                onToggleChat = onToggleChat,
                onOpenChannel = onOpenChannel,
                modifier = Modifier.align(Alignment.BottomCenter).padding(TwiDimens.PlayerControlsInset),
            )
            PlayerOverlay.Quality -> QualityPanel(
                qualities = state.qualities,
                currentQualityId = state.currentQualityId,
                onSelect = onQualitySelected,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/** The chat lives inside the embed page; keep it in step with the state. */
@Composable
private fun ChatSync(visible: Boolean, controller: TwitchEmbedController) {
    val title = stringResource(R.string.player_chat_title)
    val hint = stringResource(R.string.player_chat_read_only)
    LaunchedEffect(visible) { controller.setChatVisible(visible, title, hint) }
}

@Composable
private fun StatusPlate(state: PlayerUiState, modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.CardTextSpacing),
        modifier = modifier
            .background(TwiTheme.colors.overlay, TwiShapes.Card)
            .padding(horizontal = TwiDimens.PlayerPlateHorizontalPadding, vertical = TwiDimens.PlayerPlateVerticalPadding),
    ) {
        Text(state.channel.channelName, style = TwiTheme.typography.channelName, color = TwiTheme.colors.onOverlay)
        Text(
            text = stringResource(
                when (state.status) {
                    PlaybackStatus.Loading, PlaybackStatus.Playing -> R.string.player_loading
                    PlaybackStatus.Paused -> R.string.player_paused
                    PlaybackStatus.Offline -> R.string.player_offline
                    PlaybackStatus.Error -> R.string.player_error
                },
            ),
            style = TwiTheme.typography.bodySmall,
            color = TwiTheme.colors.onOverlay,
        )
    }
}
