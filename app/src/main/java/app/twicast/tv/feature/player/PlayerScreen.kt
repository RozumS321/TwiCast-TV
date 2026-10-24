package app.twicast.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
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
import app.twicast.tv.core.designsystem.format.formatPlaybackTime
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.feature.player.components.NativePlayerController
import app.twicast.tv.feature.player.components.NativeStreamView
import app.twicast.tv.feature.player.components.PlaybackController
import app.twicast.tv.feature.player.components.PlayerControls
import app.twicast.tv.feature.player.components.QualityPanel
import app.twicast.tv.feature.player.components.SeekBar
import app.twicast.tv.feature.player.components.TwitchEmbedController
import app.twicast.tv.feature.player.components.TwitchEmbedListener
import app.twicast.tv.feature.player.components.TwitchEmbedView
import app.twicast.tv.feature.player.components.VideoQuality

/** Keys that wind a recording right from the bare video. */
private val SeekKeys = setOf(Key.DirectionLeft, Key.DirectionRight, Key.MediaFastForward, Key.MediaRewind)

/** Remote keys that only reveal the controls when the video is bare. */
private val RevealKeys = setOf(Key.DirectionCenter, Key.Enter, Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight, Key.Menu)

/**
 * Full-screen stream (mockups "Плеер · …"). Bare video: any key shows the controls, Play/Pause toggles.
 * Back closes the quality menu, then the controls, then the chat, then the screen.
 */
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    embedController: TwitchEmbedController,
    nativeController: NativePlayerController,
    listener: TwitchEmbedListener,
    onRemoteActivity: () -> Unit,
    onOpenQuality: () -> Unit,
    onQualitySelected: (VideoQuality) -> Unit,
    onToggleChat: () -> Unit,
    onOpenChannel: () -> Unit,
    onSeekStep: (forward: Boolean, repeat: Int) -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** The chat panel; composed (and connected) only while the chat is open. */
    chat: @Composable (Modifier) -> Unit = {},
) {
    val rootFocus = remember { FocusRequester() }
    BackHandler(enabled = state.overlay != PlayerOverlay.Hidden || state.chatVisible, onBack = onBack)
    // Bare video (and its seek bar) keeps focus on the root so the next key press reaches it.
    LaunchedEffect(state.overlay) { if (state.overlay.isBare) rootFocus.requestFocus() }
    val twiCast = state.engine == PlayerEngine.TwiCast
    val controller: PlaybackController = if (twiCast) nativeController else embedController

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
                    // A recording winds from the bare video (only its seek bar comes up); ⏪ ⏩ wind anywhere.
                    state.canSeek && event.key in SeekKeys &&
                        (event.key == Key.MediaFastForward || event.key == Key.MediaRewind || state.overlay.isBare) -> {
                        onSeekStep(event.key == Key.DirectionRight || event.key == Key.MediaFastForward, event.nativeKeyEvent.repeatCount)
                        true
                    }
                    else -> {
                        onRemoteActivity()
                        // From bare video the first press only reveals the controls.
                        state.overlay.isBare && event.key in RevealKeys
                    }
                }
            }
            .focusRequester(rootFocus)
            .focusable(),
    ) {
        val streamUrl = state.streamUrl
        Row(Modifier.fillMaxSize()) {
            val video = Modifier.weight(1f).fillMaxHeight()
            when {
            !state.canPlay || !state.settingsLoaded -> Unit
            twiCast && streamUrl != null ->
                NativeStreamView(
                    url = streamUrl,
                    isLive = state.channel.isLive,
                    // Clips are MP4 files; live streams and recordings are HLS playlists.
                    isHls = state.channel.clipSlug == null,
                    preferredQualityId = state.preferredQualityId,
                    maxVideoHeight = state.maxVideoHeight,
                    controller = nativeController,
                    listener = listener,
                    modifier = video,
                    startPositionMs = state.startPositionMs,
                )
            !twiCast -> key(state.embedAttempt) {
                Box(video) {
                    // A new attempt after a renderer crash: a fresh WebView (a dead one cannot be reused).
                    TwitchEmbedView(
                        channelLogin = state.channel.channelLogin,
                        videoId = state.channel.videoId,
                        clipSlug = state.channel.clipSlug,
                        preferredQualityId = state.preferredQualityId,
                        maxVideoHeight = state.maxVideoHeight,
                        controller = embedController,
                        listener = listener,
                        modifier = Modifier.fillMaxSize(),
                        startPositionMs = state.startPositionMs,
                    )
                    // Until the stream plays: the warm player still shows the previous channel's last frame, and
                    // Twitch's page its own placeholder. Drawn by us, over the WebView, so Twitch's checks don't see it.
                    if (state.status == PlaybackStatus.Loading) {
                        Box(Modifier.matchParentSize().background(TwiTheme.colors.playerBackground))
                    }
                }
            }
            }
            // The same chat for both players: our own, next to the video.
            if (state.chatVisible) chat(Modifier.width(TwiDimens.PlayerChatWidth).fillMaxHeight())
        }
        if (state.status != PlaybackStatus.Playing || state.adBreak || state.resumeNotice) {
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
                onSeekStep = onSeekStep,
                onRestart = onRestart,
                modifier = Modifier.align(Alignment.BottomCenter).padding(TwiDimens.PlayerControlsInset),
            )
            PlayerOverlay.Seek -> state.progress?.let { progress ->
                SeekBar(
                    positionMs = state.shownPositionMs ?: progress.positionMs,
                    durationMs = progress.durationMs,
                    deltaMs = state.seekDeltaMs,
                    focusable = false,
                    onStep = onSeekStep,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(TwiDimens.PlayerControlsInset)
                        .background(TwiTheme.colors.overlay, TwiShapes.Hero)
                        .padding(horizontal = TwiDimens.PlayerControlsHorizontalPadding, vertical = TwiDimens.PlayerControlsVerticalPadding),
                )
            }
            PlayerOverlay.Quality -> QualityPanel(
                qualities = state.qualities,
                currentQualityId = state.currentQualityId,
                onSelect = onQualitySelected,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
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
        val resumedFrom = state.resumedFromMs
        Text(
            text = if (state.resumeNotice && resumedFrom != null && state.status == PlaybackStatus.Playing && !state.adBreak) {
                stringResource(R.string.player_resumed_from, formatPlaybackTime(resumedFrom))
            } else stringResource(
                when {
                    state.adBreak && state.status == PlaybackStatus.Playing -> R.string.player_ad_break
                    else -> when (state.status) {
                        PlaybackStatus.Loading, PlaybackStatus.Playing -> R.string.player_loading
                        PlaybackStatus.Paused -> R.string.player_paused
                        PlaybackStatus.Offline -> R.string.player_offline
                        PlaybackStatus.Ended -> R.string.player_ended
                        PlaybackStatus.Error -> R.string.player_error
                    }
                },
            ),
            style = TwiTheme.typography.bodySmall,
            color = TwiTheme.colors.onOverlay,
        )
    }
}

/** Nothing to focus over the video: the root takes the keys. */
private val PlayerOverlay.isBare: Boolean get() = this == PlayerOverlay.Hidden || this == PlayerOverlay.Seek
