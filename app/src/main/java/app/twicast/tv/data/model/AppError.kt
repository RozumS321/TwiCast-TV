package app.twicast.tv.data.model

import app.twicast.tv.core.network.MissingClientIdException
import app.twicast.tv.core.network.NotSignedInException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/** Errors the UI can show. The UI never sees HTTP codes or exceptions. */
sealed interface AppError {
    data object Network : AppError
    data object Unauthorized : AppError
    data object RateLimited : AppError
    data object NotFound : AppError
    /** twitch.clientId is missing from local.properties. */
    data object NotConfigured : AppError
    data object Unknown : AppError
}

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

private const val HttpUnauthorized = 401
private const val HttpNotFound = 404
private const val HttpTooManyRequests = 429

/** Runs a remote call and maps its exceptions to [AppError]. Coroutine cancellation is rethrown. */
suspend fun <T> runCatchingRemote(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (error: MissingClientIdException) {
    AppResult.Failure(AppError.NotConfigured)
} catch (error: NotSignedInException) {
    AppResult.Failure(AppError.Unauthorized)
} catch (error: IOException) {
    AppResult.Failure(AppError.Network)
} catch (error: HttpException) {
    AppResult.Failure(error.toAppError())
} catch (error: SerializationException) {
    AppResult.Failure(AppError.Unknown)
}

private fun HttpException.toAppError(): AppError = when (code()) {
    HttpUnauthorized -> AppError.Unauthorized
    HttpNotFound -> AppError.NotFound
    HttpTooManyRequests -> AppError.RateLimited
    else -> AppError.Unknown
}
