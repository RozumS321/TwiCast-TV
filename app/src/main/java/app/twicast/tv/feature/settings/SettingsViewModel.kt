package app.twicast.tv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.repository.PlayerSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data object SettingsDestination

private const val StopTimeoutMillis = 5_000L

/** Mockup "Настройки · плеер". The selected player; null until read (nothing is highlighted yet). */
@HiltViewModel
class SettingsViewModel @Inject constructor(private val settings: PlayerSettingsRepository) : ViewModel() {

    val playerEngine: StateFlow<PlayerEngine?> =
        settings.playerEngine.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), null)

    fun onPlayerSelected(engine: PlayerEngine) {
        viewModelScope.launch { settings.setPlayerEngine(engine) }
    }
}
