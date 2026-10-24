package app.twicast.tv.feature.channel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.ChannelPage
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.ChannelRepository
import app.twicast.tv.data.repository.WatchProgress
import app.twicast.tv.data.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class ChannelDestination(val channelLogin: String)

sealed interface ChannelUiState {
    data object Loading : ChannelUiState
    data class Error(val error: AppError) : ChannelUiState

    /**
     * Mockups "Канал · в эфире" / "Канал · офлайн"; [followDialog]: "Канал · подписка через QR" on top;
     * [watched]: the viewer's positions in recordings, by video id ("Канал · записи с прогрессом").
     */
    @Immutable
    data class Content(
        val channel: ChannelPage,
        val followDialog: Boolean = false,
        val watched: Map<String, WatchProgress> = emptyMap(),
    ) : ChannelUiState
}

@HiltViewModel
class ChannelViewModel internal constructor(
    private val login: String,
    private val authRepository: AuthRepository,
    private val repository: ChannelRepository,
    private val watchProgress: WatchProgressRepository,
) : ViewModel() {

    // Hilt entry: the route comes from navigation. Tests use the primary constructor (no Android in JVM tests).
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        authRepository: AuthRepository,
        repository: ChannelRepository,
        watchProgress: WatchProgressRepository,
    ) : this(savedStateHandle.toRoute<ChannelDestination>().channelLogin, authRepository, repository, watchProgress)

    private val _uiState = MutableStateFlow<ChannelUiState>(ChannelUiState.Loading)
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    /** Latest positions; kept here so a page loaded later starts with them. */
    private var watched: Map<String, WatchProgress> = emptyMap()

    init {
        load()
        // Back from a recording: its strip and «Продолжить» follow right away.
        viewModelScope.launch {
            watchProgress.progress.collect { positions ->
                watched = positions
                updateContent { it.copy(watched = positions) }
            }
        }
    }

    fun onRetry() = load()

    /** "Подписаться" / "В подписках": the QR to do it on the phone (Twitch lets no app follow for the viewer). */
    fun onFollowClick() = updateContent { it.copy(followDialog = true) }

    /** "Готово" or Back: the viewer may have followed or unfollowed on the phone, so the mark is read again quietly. */
    fun onFollowDialogClosed() {
        updateContent { it.copy(followDialog = false) }
        viewModelScope.launch {
            val viewerId = authRepository.authState.filterIsInstance<AuthState.SignedIn>().first().user.id
            val result = repository.getChannel(login, viewerId)
            // A failed refresh keeps what is shown: the page is still right apart from the mark.
            if (result is AppResult.Success) updateContent { it.copy(channel = result.value) }
        }
    }

    private fun updateContent(change: (ChannelUiState.Content) -> ChannelUiState.Content) {
        val current = _uiState.value as? ChannelUiState.Content ?: return
        _uiState.value = change(current)
    }

    private fun load() {
        _uiState.value = ChannelUiState.Loading
        viewModelScope.launch {
            val viewerId = authRepository.authState.filterIsInstance<AuthState.SignedIn>().first().user.id
            _uiState.value = when (val result = repository.getChannel(login, viewerId)) {
                is AppResult.Failure -> ChannelUiState.Error(result.error)
                is AppResult.Success -> ChannelUiState.Content(result.value, watched = watched)
            }
        }
    }
}
