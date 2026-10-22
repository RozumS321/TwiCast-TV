package app.twicast.tv.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.core.network.Clock
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.DeviceAuthorization
import app.twicast.tv.data.repository.DevicePollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Twitch asks to add 5 s to the interval on "slow_down". */
private const val SlowDownStepMillis = 5_000L

/**
 * Device Code Grant: shows the code and polls Twitch until the user confirms on the phone.
 * Success needs no UI state: the stored session flips the app to the signed-in shell.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var flowJob: Job? = null

    init {
        requestCode()
    }

    fun onNewCode() = requestCode()

    private fun requestCode() {
        flowJob?.cancel()
        _uiState.value = LoginUiState.Loading
        flowJob = viewModelScope.launch {
            when (val result = authRepository.startDeviceAuthorization()) {
                is AppResult.Failure -> _uiState.value = LoginUiState.Error(result.error)
                is AppResult.Success -> {
                    val authorization = result.value
                    _uiState.value = LoginUiState.WaitingForConfirmation(
                        userCode = authorization.userCode,
                        verificationUri = authorization.verificationUri,
                        expiresAtMillis = authorization.expiresAtMillis,
                    )
                    pollUntilDone(authorization)
                }
            }
        }
    }

    private suspend fun pollUntilDone(authorization: DeviceAuthorization) {
        var intervalMillis = authorization.pollIntervalMillis
        while (true) {
            delay(intervalMillis)
            if (clock.nowMillis() >= authorization.expiresAtMillis) {
                _uiState.value = LoginUiState.Expired
                return
            }
            when (val result = authRepository.pollDeviceAuthorization(authorization.deviceCode)) {
                DevicePollResult.Pending -> Unit
                DevicePollResult.SlowDown -> intervalMillis += SlowDownStepMillis
                DevicePollResult.Expired -> {
                    _uiState.value = LoginUiState.Expired
                    return
                }
                DevicePollResult.SignedIn -> return
                // A dropped connection should not cost the user their code: keep polling.
                is DevicePollResult.Failed -> if (result.error != AppError.Network) {
                    _uiState.value = LoginUiState.Error(result.error)
                    return
                }
            }
        }
    }
}
