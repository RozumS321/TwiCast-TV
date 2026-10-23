package app.twicast.tv.feature.player

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
        }
    }
    PlayerScreen(
        state = state,
        embedController = embedController,
        nativeController = nativeController,
        listener = listener,
        onRemoteActivity = viewModel::onRemoteActivity,
        onOpenQuality = viewModel::onOpenQuality,
        onQualitySelected = { quality ->
            controller.setQuality(quality.id)
            viewModel.onQualitySelected(quality.id)
        },
        onToggleChat = viewModel::onToggleChat,
        onOpenChannel = { onOpenChannel(state.channel.channelLogin) },
        onBack = { if (!viewModel.onBack()) onClose() },
        chat = { modifier ->
            val messages by chatViewModel.messages.collectAsStateWithLifecycle()
            ChatPanel(messages, modifier)
        },
    )
}
