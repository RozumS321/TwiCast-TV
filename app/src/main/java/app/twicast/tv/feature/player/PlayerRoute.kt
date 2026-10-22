package app.twicast.tv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.feature.player.components.EmbedState
import app.twicast.tv.feature.player.components.TwitchEmbedController
import app.twicast.tv.feature.player.components.TwitchEmbedListener
import app.twicast.tv.feature.player.components.VideoQuality

@Composable
fun PlayerRoute(onClose: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = remember { TwitchEmbedController() }
    val listener = remember(viewModel) {
        object : TwitchEmbedListener {
            override fun onStateChanged(state: EmbedState) = viewModel.onEmbedStateChanged(state)
            override fun onQualities(qualities: List<VideoQuality>, currentId: String) = viewModel.onQualities(qualities, currentId)
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
        onBack = { if (!viewModel.onBack()) onClose() },
    )
}
