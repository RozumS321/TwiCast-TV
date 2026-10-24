package app.twicast.tv.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.data.model.Stream
import app.twicast.tv.data.repository.HistoryEntry
import app.twicast.tv.feature.player.components.LocalWarmTwitchPlayer

@Composable
fun HomeRoute(
    onSignIn: () -> Unit,
    onStreamClick: (Stream) -> Unit,
    onRecordingClick: (HistoryEntry) -> Unit,
    onChannelClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Visible = started: back from a stream or another section, or the app back from the background.
    LifecycleStartEffect(viewModel) {
        viewModel.onVisible()
        onStopOrDispose { viewModel.onHidden() }
    }
    // The Twitch player loads in the background with the first channel on screen, the likeliest to be opened.
    val warmPlayer = LocalWarmTwitchPlayer.current
    val firstChannel = (state as? HomeUiState.Content)?.let { content ->
        (content.followedLive + content.recommended + content.regional).firstOrNull()?.userLogin
    }
    LaunchedEffect(warmPlayer, firstChannel) {
        if (warmPlayer != null && firstChannel != null) warmPlayer.prepare(firstChannel)
    }
    HomeScreen(
        state = state,
        onRetry = viewModel::onRetry,
        onStreamClick = onStreamClick,
        onRecordingClick = onRecordingClick,
        onChannelClick = onChannelClick,
        onSignIn = onSignIn,
        onDismissSignInBanner = viewModel::onDismissSignInBanner,
    )
}
