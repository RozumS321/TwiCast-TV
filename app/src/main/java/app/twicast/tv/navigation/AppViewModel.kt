package app.twicast.tv.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val StopTimeoutMillis = 5_000L

/** App-level auth gate: sign-in screen until a session exists (sign-in is mandatory in the MVP). */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), AuthState.Loading)

    init {
        viewModelScope.launch { authRepository.validateSession() }
    }

    fun onSignOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
