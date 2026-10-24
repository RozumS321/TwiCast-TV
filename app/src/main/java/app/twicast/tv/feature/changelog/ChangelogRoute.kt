package app.twicast.tv.feature.changelog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.data.update.AppRelease

/** [onUpdate]: the app-wide update offer for a release newer than the installed one. */
@Composable
fun ChangelogRoute(onUpdate: (AppRelease) -> Unit, viewModel: ChangelogViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChangelogScreen(state = state, onRetry = viewModel::onRetry, onUpdate = onUpdate)
}
