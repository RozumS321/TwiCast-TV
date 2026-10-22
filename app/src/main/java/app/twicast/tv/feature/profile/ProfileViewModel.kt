package app.twicast.tv.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.User
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.FollowsRepository
import app.twicast.tv.data.repository.StreamsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LiveLimit = 100

/** Mockup "Профиль". Counters are null while loading or if they failed (shown as "—"). */
data class ProfileUiState(
    val user: User? = null,
    val followsCount: Int? = null,
    val liveCount: Int? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val streamsRepository: StreamsRepository,
    private val followsRepository: FollowsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.authState.filterIsInstance<AuthState.SignedIn>().first().user
            _uiState.update { it.copy(user = user) }
            val follows = async { followsRepository.getFollows(user.id) }
            val live = async { streamsRepository.getFollowedStreams(user.id, LiveLimit) }
            _uiState.update {
                it.copy(
                    followsCount = (follows.await() as? AppResult.Success)?.value?.total,
                    liveCount = (live.await() as? AppResult.Success)?.value?.size,
                )
            }
        }
    }

    /** Clears tokens and the session; the app returns to sign-in by itself. */
    fun onSignOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
