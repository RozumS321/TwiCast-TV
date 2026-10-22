package app.twicast.tv.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.BuildConfig

@Composable
fun ProfileRoute(onOpenSettings: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(state = state, versionName = BuildConfig.VERSION_NAME, onOpenSettings = onOpenSettings, onSignOut = viewModel::onSignOut)
}
