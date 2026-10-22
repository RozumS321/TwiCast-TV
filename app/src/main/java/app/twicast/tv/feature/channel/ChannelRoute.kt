package app.twicast.tv.feature.channel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.data.model.ChannelPage
import app.twicast.tv.data.model.Clip
import app.twicast.tv.data.model.Video

@Composable
fun ChannelRoute(
    onWatchLive: (ChannelPage) -> Unit,
    onVideoClick: (ChannelPage, Video) -> Unit,
    onClipClick: (ChannelPage, Clip) -> Unit,
    viewModel: ChannelViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChannelScreen(
        state = state,
        onWatchLive = onWatchLive,
        onVideoClick = onVideoClick,
        onClipClick = onClipClick,
        onRetry = viewModel::onRetry,
    )
}
