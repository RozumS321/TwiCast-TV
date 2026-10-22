package app.twicast.tv.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.twicast.tv.data.repository.PlayerSettingsRepository
import app.twicast.tv.feature.player.components.EmbedState
import app.twicast.tv.feature.player.components.VideoQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** Card data is passed along so the control panel can show it without another request. */
@Serializable
data class PlayerDestination(
    val channelLogin: String,
    val channelName: String,
    val categoryName: String,
    /** Null when opened from search (an offline channel has no count). */
    val viewerCount: Int?,
    val avatarUrl: String?,
)

/** Controls hide after this long without remote input (mockup note: "через 4 с"). */
const val ControlsAutoHideMillis = 4_000L

/**
 * Player screen state. Playback state and the quality list come from the embed page; overlays, chat
 * and the preferred quality are decided here. The screen forwards commands to the embed itself.
 */
@HiltViewModel
class PlayerViewModel internal constructor(
    destination: PlayerDestination,
    private val settings: PlayerSettingsRepository,
) : ViewModel() {

    // Hilt entry: the route comes from navigation. Tests use the primary constructor (no Android in JVM tests).
    @Inject
    constructor(savedStateHandle: SavedStateHandle, settings: PlayerSettingsRepository) :
        this(savedStateHandle.toRoute<PlayerDestination>(), settings)

    private val _uiState = MutableStateFlow(PlayerUiState(channel = destination))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var autoHideJob: Job? = null

    init {
        viewModelScope.launch {
            val preferred = settings.preferredQuality()
            _uiState.update { it.copy(preferredQualityId = preferred, settingsLoaded = true) }
        }
    }

    fun onEmbedStateChanged(state: EmbedState) {
        _uiState.update {
            it.copy(
                status = when (state) {
                    // READY comes before the first frame: keep "loading" until PLAYING.
                    EmbedState.Ready -> it.status
                    EmbedState.Playing -> PlaybackStatus.Playing
                    EmbedState.Paused -> PlaybackStatus.Paused
                    EmbedState.Offline -> PlaybackStatus.Offline
                    EmbedState.Error -> PlaybackStatus.Error
                },
            )
        }
    }

    fun onQualities(qualities: List<VideoQuality>, currentId: String) {
        _uiState.update { it.copy(qualities = qualities.toImmutableList(), currentQualityId = currentId.ifEmpty { null }) }
    }

    /** Any remote key: shows the controls from the bare video, and keeps them up while the user acts. */
    fun onRemoteActivity() {
        if (_uiState.value.overlay == PlayerOverlay.Hidden) _uiState.update { it.copy(overlay = PlayerOverlay.Controls) }
        restartAutoHide()
    }

    fun onOpenQuality() {
        autoHideJob?.cancel()
        _uiState.update { it.copy(overlay = PlayerOverlay.Quality) }
    }

    /** Remembers the choice for next streams; the screen applies it to the embed. */
    fun onQualitySelected(qualityId: String) {
        _uiState.update { it.copy(overlay = PlayerOverlay.Hidden, currentQualityId = qualityId, preferredQualityId = qualityId) }
        viewModelScope.launch { settings.setPreferredQuality(qualityId) }
    }

    fun onToggleChat() {
        _uiState.update { it.copy(chatVisible = !it.chatVisible, overlay = PlayerOverlay.Hidden) }
    }

    /** Back steps out of overlays and the chat first; returns false when the screen itself should close. */
    fun onBack(): Boolean {
        val state = _uiState.value
        val next = when {
            state.overlay == PlayerOverlay.Quality -> state.copy(overlay = PlayerOverlay.Controls)
            state.overlay == PlayerOverlay.Controls -> state.copy(overlay = PlayerOverlay.Hidden)
            state.chatVisible -> state.copy(chatVisible = false)
            else -> return false
        }
        _uiState.value = next
        if (next.overlay == PlayerOverlay.Controls) restartAutoHide()
        return true
    }

    private fun restartAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = viewModelScope.launch {
            delay(ControlsAutoHideMillis)
            _uiState.update { if (it.overlay == PlayerOverlay.Controls) it.copy(overlay = PlayerOverlay.Hidden) else it }
        }
    }
}
