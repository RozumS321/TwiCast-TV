package app.twicast.tv.feature.player

import app.twicast.tv.feature.player.components.sleepLeftText
import app.twicast.tv.feature.player.components.SleepWarning
import app.twicast.tv.feature.player.components.SleepTimerPanel
import app.twicast.tv.feature.player.components.SleepAskCard
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import app.twicast.tv.core.designsystem.component.LocalEmberBackdrop
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.feature.player.components.ChatPanel
import app.twicast.tv.feature.player.components.EmbedState
import app.twicast.tv.feature.player.components.NativePlayerController
import app.twicast.tv.feature.player.components.PlaybackController
import app.twicast.tv.feature.player.components.PerfTag
import coil3.SingletonImageLoader
import app.twicast.tv.feature.player.components.TwitchEmbedController
import app.twicast.tv.feature.player.components.TwitchEmbedListener
import app.twicast.tv.feature.player.components.VideoQuality

@Composable
fun PlayerRoute(onClose: () -> Unit, onOpenChannel: (String) -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The video covers the whole screen: the effects background stops animating and blurring underneath.
    val backdrop = LocalEmberBackdrop.current
    DisposableEffect(backdrop) {
        backdrop?.coveredByVideo = true
        onDispose { backdrop?.coveredByVideo = false }
    }
    // Weak TV boxes run the embed close to the memory limit: drop decoded thumbnails while it plays
    // (they come back from the disk cache).
    LaunchedEffect(Unit) {
        Log.i(PerfTag, "player opened")
        SingletonImageLoader.get(context).memoryCache?.clear()
    }
    val chatViewModel: ChatViewModel = hiltViewModel()
    val embedController = remember { TwitchEmbedController() }
    val nativeController = remember { NativePlayerController() }
    val controller: PlaybackController = if (state.engine == PlayerEngine.TwiCast) nativeController else embedController
    val listener = remember(viewModel) {
        object : TwitchEmbedListener {
            override fun onStateChanged(state: EmbedState) = viewModel.onEmbedStateChanged(state)
            override fun onQualities(qualities: List<VideoQuality>, currentId: String) = viewModel.onQualities(qualities, currentId)
            override fun onRendererGone() = viewModel.onRendererGone()
            override fun onAdBreak(active: Boolean) = viewModel.onAdBreak(active)
            override fun onProgress(positionMs: Long, durationMs: Long) = viewModel.onProgress(positionMs, durationMs)
        }
    }
    // Sleep timer (canvas "Плеер · таймер сна"): leaves the player and puts the box to sleep when time is up.
    val sleepViewModel: SleepTimerViewModel = hiltViewModel()
    val sleepUi by sleepViewModel.ui.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(sleepViewModel) {
        sleepViewModel.sleepNow.collect {
            currentOnClose()
            sleepViewModel.sleepDevice()
        }
    }
    LaunchedEffect(state.status) { if (state.status == PlaybackStatus.Ended) sleepViewModel.onStreamEnded() }
    LifecycleResumeEffect(sleepViewModel) {
        sleepViewModel.onResume()
        onPauseOrDispose {}
    }
    val openAdminRequest = {
        sleepViewModel.onExplained()
        runCatching { context.startActivity(sleepViewModel.adminRequest()) }
        Unit
    }
    val currentController by rememberUpdatedState(controller)
    LaunchedEffect(viewModel) { viewModel.seekRequests.collect { currentController.seekTo(it) } }
    PlayerScreen(
        state = state,
        embedController = embedController,
        nativeController = nativeController,
        listener = listener,
        // While the sleep timer's panel or warning is up, keys belong to it: the controls stay down.
        onRemoteActivity = { if (!sleepUi.capturesKeys) viewModel.onRemoteActivity() },
        onOpenQuality = viewModel::onOpenQuality,
        onQualitySelected = { quality ->
            controller.setQuality(quality.id)
            viewModel.onQualitySelected(quality.id)
        },
        onToggleChat = viewModel::onToggleChat,
        onOpenChannel = { onOpenChannel(state.channel.channelLogin) },
        onSeekStep = viewModel::onSeekStep,
        onRestart = viewModel::onRestart,
        onBack = { if (!viewModel.onBack()) onClose() },
        sleepLeft = sleepLeftText(sleepUi.choice, sleepUi.leftMillis),
        onOpenSleep = {
            viewModel.onBack()
            sleepViewModel.onOpenMenu()
        },
        sleepOverlay = {
            when (sleepUi.panel) {
                SleepPanel.Menu -> SleepTimerPanel(
                    choice = sleepUi.choice,
                    allowed = sleepUi.allowed,
                    onChoose = sleepViewModel::onChoose,
                    onAllow = openAdminRequest,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
                SleepPanel.Ask -> SleepAskCard(
                    onAllow = openAdminRequest,
                    onOnlyStop = sleepViewModel::onExplained,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
                SleepPanel.None -> Unit
            }
            val left = sleepUi.leftMillis
            if (sleepUi.warning && left != null) {
                SleepWarning(left, sleepViewModel::onExtend, sleepViewModel::onCancelTimer, Modifier.fillMaxSize())
            }
        },
        chat = { modifier ->
            val messages by chatViewModel.messages.collectAsStateWithLifecycle()
            ChatPanel(messages, modifier)
        },
    )
    // Declared after the screen: closes the sleep panel before the player handles Back.
    BackHandler(enabled = sleepUi.panel != SleepPanel.None) { sleepViewModel.onBack() }
}

