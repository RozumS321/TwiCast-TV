package app.twicast.tv.feature.following

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.data.model.FollowedChannel
import app.twicast.tv.data.model.Stream

@Composable
fun FollowingRoute(
    onStreamClick: (Stream) -> Unit,
    onChannelClick: (FollowedChannel) -> Unit,
    viewModel: FollowingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FollowingScreen(
        state = state,
        onTabSelected = viewModel::onTabSelected,
        onStreamClick = onStreamClick,
        onChannelClick = onChannelClick,
        onRetry = viewModel::onRetry,
    )
}
