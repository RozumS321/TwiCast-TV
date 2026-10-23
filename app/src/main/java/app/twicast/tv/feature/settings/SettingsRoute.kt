package app.twicast.tv.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val engine by viewModel.playerEngine.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    SettingsScreen(
        selected = engine,
        onPlayerSelected = viewModel::onPlayerSelected,
        themeMode = themeMode,
        onThemeSelected = viewModel::onThemeSelected,
        language = language,
        // Resources are picked when the activity is created: recreating it redraws everything in the new language.
        onLanguageSelected = { if (viewModel.onLanguageSelected(it)) activity?.recreate() },
    )
}
