package app.twicast.tv.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    SettingsScreen(
        state = state,
        onSectionFocused = viewModel::onSectionFocused,
        onPlayerSelected = viewModel::onPlayerSelected,
        onDefaultQualitySelected = viewModel::onDefaultQualitySelected,
        onThemeSelected = viewModel::onThemeSelected,
        // Resources are picked when the activity is created: recreating it redraws everything in the new language.
        onLanguageSelected = { if (viewModel.onLanguageSelected(it)) activity?.recreate() },
        onOpenChatChanged = viewModel::onOpenChatChanged,
        onNotifyLiveStartsChanged = viewModel::onNotifyLiveStartsChanged,
        onWarmTwitchPlayerChanged = viewModel::onWarmTwitchPlayerChanged,
        onFreeMemoryInBackgroundChanged = viewModel::onFreeMemoryInBackgroundChanged,
    )
}
