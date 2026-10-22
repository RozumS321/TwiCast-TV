package app.twicast.tv.feature.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Update panel over the whole app; draws nothing when there is no update. */
@Composable
fun UpdateRoute(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Back from "Install unknown apps" settings: install if the permission was granted.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    val actions = remember(viewModel) {
        object : UpdateActions {
            override fun onUpdate() = viewModel.onUpdate()
            override fun onCancelDownload() = viewModel.onCancelDownload()
            override fun onOpenPermissionSettings() = viewModel.onOpenPermissionSettings()
            override fun onDismiss() = viewModel.onDismiss()
        }
    }
    UpdateOverlay(state, actions)
}
