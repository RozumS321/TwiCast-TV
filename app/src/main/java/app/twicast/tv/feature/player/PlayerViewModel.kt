package app.twicast.tv.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.twicast.tv.core.device.DeviceProfile
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.repository.PlaybackRepository
import app.twicast.tv.data.repository.PlaybackSource
import app.twicast.tv.data.repository.PlayerSettingsRepository
import app.twicast.tv.data.repository.StreamsRepository
import app.twicast.tv.data.repository.qualityCeiling
import app.twicast.tv.feature.player.components.EmbedState
import app.twicast.tv.feature.player.components.VideoQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    /** Past broadcast to play instead of the live stream. */
    val videoId: String? = null,
    /** Clip slug to play instead of the live stream. */
    val clipSlug: String? = null,
) {
    /** Live stream (not a recording): only then the chat makes sense. */
    val isLive: Boolean get() = videoId == null && clipSlug == null
}

/** Controls hide after this long without remote input (mockup note: "через 4 с"). */
const val ControlsAutoHideMillis = 4_000L

/** Renderer crashes survived by rebuilding the embed; one more is shown as an error. */
const val MaxEmbedRestarts = 2

/** Quality ceiling on low-memory TVs and after a renderer crash: 1080p is what runs weak boxes out of memory. */
const val RecoveryMaxVideoHeight = 720

/**
 * A live stream waiting for data this long gets a "still broadcasting?" check, then again at this pace.
 * When Twitch ends a broadcast the playlist often just stops growing: the picture would freeze with no word.
 */
const val StallCheckMillis = 8_000L

/**
 * Player screen state. Playback state and the quality list come from the embed page; overlays, chat
 * and the preferred quality are decided here. The screen forwards commands to the embed itself.
 */
@HiltViewModel
class PlayerViewModel internal constructor(
    destination: PlayerDestination,
    private val settings: PlayerSettingsRepository,
    private val playback: PlaybackRepository,
    private val streams: StreamsRepository,
    device: DeviceProfile,
) : ViewModel() {

    // Hilt entry: the route comes from navigation. Tests use the primary constructor (no Android in JVM tests).
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        settings: PlayerSettingsRepository,
        playback: PlaybackRepository,
        streams: StreamsRepository,
        device: DeviceProfile,
    ) : this(savedStateHandle.toRoute<PlayerDestination>(), settings, playback, streams, device)

    // Weak TVs start below 1080p right away instead of waiting for the renderer to crash.
    private val _uiState = MutableStateFlow(
        PlayerUiState(channel = destination, maxVideoHeight = RecoveryMaxVideoHeight.takeIf { device.isLowMemory }),
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var autoHideJob: Job? = null
    private var stallCheckJob: Job? = null

    init {
        viewModelScope.launch {
            val preferred = settings.preferredQuality()
            val engine = settings.playerEngine.first()
            val openChat = settings.openChatWithStream.first()
            // A ceiling from Settings ("720p") is not a quality the player knows: it caps the height instead,
            // together with the low-memory cap, whichever is lower.
            val ceiling = qualityCeiling(preferred)
            _uiState.update {
                it.copy(
                    preferredQualityId = preferred.takeIf { ceiling == null },
                    maxVideoHeight = lowerOf(it.maxVideoHeight, ceiling),
                    chatVisible = openChat && it.channel.isLive,
                )
            }
            if (engine == PlayerEngine.TwiCast && _uiState.value.canPlay) startTwiCast() else useTwitchPlayer()
        }
    }

    /** Resolves the stream's own playlist; if Twitch refuses it, the official embed plays instead. */
    private suspend fun startTwiCast() {
        val channel = _uiState.value.channel
        val source = when {
            channel.clipSlug != null -> PlaybackSource.Clip(channel.clipSlug)
            channel.videoId != null -> PlaybackSource.Vod(channel.videoId)
            else -> PlaybackSource.Live(channel.channelLogin)
        }
        when (val result = playback.playbackUrl(source)) {
            is AppResult.Success -> _uiState.update {
                it.copy(engine = PlayerEngine.TwiCast, streamUrl = result.value, settingsLoaded = true)
            }
            // No network: the embed would not load either.
            is AppResult.Failure -> if (result.error == AppError.Network) {
                _uiState.update { it.copy(engine = PlayerEngine.TwiCast, settingsLoaded = true, status = PlaybackStatus.Error) }
            } else {
                useTwitchPlayer()
            }
        }
    }

    private fun useTwitchPlayer() = _uiState.update { it.copy(engine = PlayerEngine.Twitch, settingsLoaded = true) }

    fun onEmbedStateChanged(state: EmbedState) {
        // The TwiCast player failed on its own (unusual playlist, codec): the official embed gets a try.
        if (state == EmbedState.Error && _uiState.value.engine == PlayerEngine.TwiCast && _uiState.value.streamUrl != null) {
            _uiState.update { it.copy(engine = PlayerEngine.Twitch, streamUrl = null, status = PlaybackStatus.Loading) }
            return
        }
        if (state == EmbedState.Buffering) {
            if (_uiState.value.channel.isLive) startStallCheck()
            return
        }
        stallCheckJob?.cancel()
        _uiState.update {
            it.copy(
                status = when (state) {
                    // READY comes before the first frame: keep "loading" until PLAYING.
                    EmbedState.Ready, EmbedState.Buffering -> it.status
                    EmbedState.Playing -> PlaybackStatus.Playing
                    EmbedState.Paused -> PlaybackStatus.Paused
                    // Offline right after watching it means the broadcast ended in front of the viewer.
                    EmbedState.Offline -> if (it.wasWatching) PlaybackStatus.Ended else PlaybackStatus.Offline
                    EmbedState.Error -> PlaybackStatus.Error
                },
            )
        }
        if (_uiState.value.status == PlaybackStatus.Ended) showEndedControls()
    }

    private val PlayerUiState.wasWatching: Boolean
        get() = status == PlaybackStatus.Playing || status == PlaybackStatus.Paused

    /** Asks Twitch whether the channel still broadcasts while playback keeps waiting; a network error just waits more. */
    private fun startStallCheck() {
        if (stallCheckJob?.isActive == true) return
        stallCheckJob = viewModelScope.launch {
            while (true) {
                delay(StallCheckMillis)
                val live = streams.isLive(_uiState.value.channel.channelLogin)
                if (live is AppResult.Success && !live.value) {
                    _uiState.update { it.copy(status = PlaybackStatus.Ended, adBreak = false) }
                    showEndedControls()
                    return@launch
                }
            }
        }
    }

    /** The "Канал" button is the way on from an ended stream: the controls come up and stay. */
    private fun showEndedControls() {
        autoHideJob?.cancel()
        _uiState.update { it.copy(overlay = PlayerOverlay.Controls) }
    }

    /** The WebView renderer died (usually out of memory): rebuild the embed with a lower quality ceiling. */
    fun onRendererGone() {
        _uiState.update {
            if (it.embedAttempt >= MaxEmbedRestarts) {
                it.copy(status = PlaybackStatus.Error)
            } else {
                it.copy(
                    embedAttempt = it.embedAttempt + 1,
                    maxVideoHeight = lowerOf(it.maxVideoHeight, RecoveryMaxVideoHeight),
                    status = PlaybackStatus.Loading,
                    qualities = persistentListOf(),
                    currentQualityId = null,
                    // The new page starts without the chat; the viewer reopens it.
                    chatVisible = false,
                )
            }
        }
    }

    fun onAdBreak(active: Boolean) = _uiState.update { it.copy(adBreak = active) }

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

    private fun lowerOf(first: Int?, second: Int?): Int? = listOfNotNull(first, second).minOrNull()

    private fun restartAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = viewModelScope.launch {
            delay(ControlsAutoHideMillis)
            _uiState.update {
                // After the broadcast ended the controls stay: "Канал" is the way on.
                if (it.overlay == PlayerOverlay.Controls && it.status != PlaybackStatus.Ended) it.copy(overlay = PlayerOverlay.Hidden) else it
            }
        }
    }
}
