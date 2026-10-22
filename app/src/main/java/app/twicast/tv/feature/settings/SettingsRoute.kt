package app.twicast.tv.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val engine by viewModel.playerEngine.collectAsStateWithLifecycle()
    SettingsScreen(selected = engine, onPlayerSelected = viewModel::onPlayerSelected)
}
