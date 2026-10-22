package app.twicast.tv.feature.auth

import app.twicast.tv.data.model.AppError

sealed interface LoginUiState {
    data object Loading : LoginUiState

    /** Code is on screen; [verificationUri] already contains the code (it goes into the QR). */
    data class WaitingForConfirmation(
        val userCode: String,
        val verificationUri: String,
        val expiresAtMillis: Long,
    ) : LoginUiState

    data object Expired : LoginUiState

    data class Error(val error: AppError) : LoginUiState
}
