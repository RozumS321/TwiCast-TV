package app.twicast.tv.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import app.twicast.tv.R
import app.twicast.tv.data.model.AppError

/** User-facing text for an [AppError]. */
@Composable
@ReadOnlyComposable
fun appErrorMessage(error: AppError): String = stringResource(
    when (error) {
        AppError.Network -> R.string.error_network
        AppError.RateLimited -> R.string.error_rate_limited
        AppError.Unauthorized -> R.string.error_unauthorized
        AppError.NotConfigured -> R.string.error_not_configured
        AppError.NotFound -> R.string.error_not_found
        AppError.Unknown -> R.string.error_unknown
    },
)
