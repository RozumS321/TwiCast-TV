package app.twicast.tv.feature.following

import kotlinx.collections.immutable.persistentListOf
import java.util.Locale
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.FollowedChannel
import app.twicast.tv.data.model.Stream
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.FollowsRepository
import app.twicast.tv.data.repository.StreamsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LiveLimit = 100
private const val PopularLimit = 8

sealed interface FollowingUiState {
    data object Loading : FollowingUiState

    /** The user follows nobody yet. */
    data object Empty : FollowingUiState

    /** Not signed in: following needs a Twitch account. */
    data object Guest : FollowingUiState

    data class Error(val error: AppError) : FollowingUiState

    /** Mockup "Подписки": one screen, live channels with their stream, then offline ones with their last category. */
    @Immutable
    data class Content(
        val live: ImmutableList<Stream>,
        val offline: ImmutableList<FollowedChannel>,
        /** Nobody followed is live: what is popular now in the device language, so the screen is not a dead end. */
        val popular: ImmutableList<Stream> = persistentListOf(),
    ) : FollowingUiState
}

@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val streamsRepository: StreamsRepository,
    private val followsRepository: FollowsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FollowingUiState>(FollowingUiState.Loading)
    val uiState: StateFlow<FollowingUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        _uiState.value = FollowingUiState.Loading
        viewModelScope.launch {
            val user = (authRepository.authState.first { it != AuthState.Loading } as? AuthState.SignedIn)?.user
            if (user == null) {
                _uiState.value = FollowingUiState.Guest
                return@launch
            }
            val userId = user.id
            val liveRequest = async { streamsRepository.getFollowedStreams(userId, LiveLimit) }
            val followsRequest = async { followsRepository.getFollows(userId) }
            val live = liveRequest.await()
            val follows = followsRequest.await()
            _uiState.value = when {
                live is AppResult.Failure -> FollowingUiState.Error(live.error)
                follows is AppResult.Failure -> FollowingUiState.Error(follows.error)
                else -> content((live as AppResult.Success).value, (follows as AppResult.Success).value.channels)
            }
            // Canvas "Подписки · никого нет в эфире": popular streams instead of an empty section. Best effort.
            val shown = _uiState.value
            if (shown is FollowingUiState.Content && shown.live.isEmpty()) {
                val popular = streamsRepository.getTopStreams(Locale.getDefault().language, PopularLimit)
                if (popular is AppResult.Success) _uiState.value = shown.copy(popular = popular.value.toImmutableList())
            }
        }
    }

    private fun content(live: List<Stream>, followed: List<FollowedChannel>): FollowingUiState {
        if (live.isEmpty() && followed.isEmpty()) return FollowingUiState.Empty
        val liveIds = live.mapTo(HashSet()) { it.userId }
        return FollowingUiState.Content(
            live = live.toImmutableList(),
            offline = followed.filterNot { it.id in liveIds }.toImmutableList(),
        )
    }
}
