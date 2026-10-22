package app.twicast.tv.feature.categories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.data.model.Stream

@Composable
fun CategoryStreamsRoute(
    onBack: () -> Unit,
    onStreamClick: (Stream) -> Unit,
    viewModel: CategoryStreamsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoryStreamsScreen(
        state = state,
        onBack = onBack,
        onLanguageSelected = viewModel::onLanguageSelected,
        onStreamClick = onStreamClick,
        onLoadMore = viewModel::onLoadMore,
        onRetry = viewModel::onRetry,
    )
}
