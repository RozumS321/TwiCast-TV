package app.twicast.tv.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.GuestChoiceRepository
import app.twicast.tv.data.repository.StreamsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val StopTimeoutMillis = 5_000L

/** The sidebar's live count is refreshed at this pace (home asks the same, cached, list). */
const val LiveCountRefreshMillis = 3 * 60_000L

/** As on home: one request covers the follow list's live channels, and shares home's cache. */
private const val FollowedLimit = 100

/** App-level state: the session, the first-start choice (sign in or guest) and the sidebar's live count. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val streamsRepository: StreamsRepository,
    private val guestChoice: GuestChoiceRepository,
) : ViewModel() {

    /** Signed out: the first start asks to sign in or go on as a guest; null while it is read. */
    val guestChosen: StateFlow<Boolean?> = guestChoice.guestChosen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), null)

    /** «Продолжить без входа» on the first start. */
    fun onContinueAsGuest() {
        viewModelScope.launch { guestChoice.chooseGuest() }
    }

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), AuthState.Loading)

    /** Followed channels live now, for "Подписки" in the sidebar; 0 signed out. Polled only while the app is seen. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val liveFollowedCount: StateFlow<Int> = authRepository.authState
        .flatMapLatest { state -> if (state is AuthState.SignedIn) liveCount(state.user.id) else flowOf(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), 0)

    // A failed request keeps the last count: the next tick tries again.
    private fun liveCount(userId: String): Flow<Int> = flow {
        while (true) {
            val result = streamsRepository.getFollowedStreams(userId, FollowedLimit)
            if (result is AppResult.Success) emit(result.value.size)
            delay(LiveCountRefreshMillis)
        }
    }

    init {
        viewModelScope.launch { authRepository.validateSession() }
    }
}
