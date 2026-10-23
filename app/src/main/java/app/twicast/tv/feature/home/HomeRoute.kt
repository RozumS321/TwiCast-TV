package app.twicast.tv.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.data.model.Stream

@Composable
fun HomeRoute(
    onStreamClick: (Stream) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Visible = started: back from a stream or another section, or the app back from the background.
    LifecycleStartEffect(viewModel) {
        viewModel.onVisible()
        onStopOrDispose { viewModel.onHidden() }
    }
    HomeScreen(state = state, onRetry = viewModel::onRetry, onStreamClick = onStreamClick)
}
