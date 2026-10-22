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

    /** Mockups "Канал · в эфире" / "Канал · офлайн". */
    @Immutable
    data class Content(val channel: ChannelPage) : ChannelUiState
}

@HiltViewModel
class ChannelViewModel internal constructor(
    private val login: String,
    private val authRepository: AuthRepository,
    private val repository: ChannelRepository,
) : ViewModel() {

    // Hilt entry: the route comes from navigation. Tests use the primary constructor (no Android in JVM tests).
    @Inject
    constructor(savedStateHandle: SavedStateHandle, authRepository: AuthRepository, repository: ChannelRepository) :
        this(savedStateHandle.toRoute<ChannelDestination>().channelLogin, authRepository, repository)

    private val _uiState = MutableStateFlow<ChannelUiState>(ChannelUiState.Loading)
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        _uiState.value = ChannelUiState.Loading
        viewModelScope.launch {
            val viewerId = authRepository.authState.filterIsInstance<AuthState.SignedIn>().first().user.id
            _uiState.value = when (val result = repository.getChannel(login, viewerId)) {
                is AppResult.Failure -> ChannelUiState.Error(result.error)
                is AppResult.Success -> ChannelUiState.Content(result.value)
            }
        }
    }
}
