package app.twicast.tv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.feature.player.components.EmbedState
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
    LaunchedEffect(Unit) { SingletonImageLoader.get(context).memoryCache?.clear() }
    val controller = remember { TwitchEmbedController() }
    val listener = remember(viewModel) {
        object : TwitchEmbedListener {
            override fun onStateChanged(state: EmbedState) = viewModel.onEmbedStateChanged(state)
            override fun onQualities(qualities: List<VideoQuality>, currentId: String) = viewModel.onQualities(qualities, currentId)
            override fun onRendererGone() = viewModel.onRendererGone()
        }
    }
    PlayerScreen(
        state = state,
        controller = controller,
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
    )
}
