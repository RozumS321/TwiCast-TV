package app.twicast.tv.data.repository

import app.twicast.tv.core.network.Clock
import app.twicast.tv.core.network.TokenRefresher
import app.twicast.tv.core.network.TwitchClientId
import app.twicast.tv.core.network.bearer
import app.twicast.tv.data.local.SessionStore
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.Session
import app.twicast.tv.data.model.User
import app.twicast.tv.data.model.runCatchingRemote
import app.twicast.tv.data.remote.HelixApi
import app.twicast.tv.data.remote.TwitchOAuthApi
import app.twicast.tv.data.remote.dto.OAuthErrorDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Code the user types at twitch.tv/activate, plus how long and how often to poll for it. */
data class DeviceAuthorization(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresAtMillis: Long,
    val pollIntervalMillis: Long,
)

sealed interface DevicePollResult {
    /** The user has not confirmed yet — poll again after the interval. */
    data object Pending : DevicePollResult
    /** Polling too often — increase the interval. */
    data object SlowDown : DevicePollResult
    /** The code expired or was rejected — a new one is needed. */
    data object Expired : DevicePollResult
    data object SignedIn : DevicePollResult
    data class Failed(val error: AppError) : DevicePollResult
}

interface AuthRepository {
    /** [AuthState.SignedIn] while a session is stored. */
    val authState: Flow<AuthState>

    /** Twitch requires validating a stored token on start: refreshes it if expired, signs out if revoked. */
    suspend fun validateSession()

    suspend fun startDeviceAuthorization(): AppResult<DeviceAuthorization>

    suspend fun pollDeviceAuthorization(deviceCode: String): DevicePollResult

    suspend fun signOut()
}

/** Scopes requested at sign-in: followed channels (stage 5). */
private const val Scopes = "user:read:follows"
private const val MillisInSecond = 1000L
private const val HttpUnauthorized = 401
private const val ErrorPending = "authorization_pending"
private const val ErrorSlowDown = "slow_down"

@Singleton
class TwitchAuthRepository @Inject constructor(
    private val oAuthApi: TwitchOAuthApi,
    private val helixApi: HelixApi,
    private val sessionStore: SessionStore,
    private val tokenRefresher: TokenRefresher,
    private val json: Json,
    private val clock: Clock,
    @param:TwitchClientId private val clientId: String,
) : AuthRepository {

    override val authState: Flow<AuthState> = sessionStore.session.map { session ->
        if (session == null) AuthState.SignedOut else AuthState.SignedIn(session.user)
    }

    override suspend fun validateSession() {
        val session = sessionStore.current() ?: return
        try {
            oAuthApi.validate("OAuth ${session.accessToken}")
        } catch (error: HttpException) {
            if (error.code() == HttpUnauthorized) tokenRefresher.refresh(session.accessToken)
        } catch (error: IOException) {
            // Offline at start: keep the session, requests will report the network error.
        }
    }

    override suspend fun startDeviceAuthorization(): AppResult<DeviceAuthorization> = runCatchingRemote {
        val response = oAuthApi.requestDeviceCode(clientId, Scopes)
        DeviceAuthorization(
            deviceCode = response.deviceCode,
            userCode = response.userCode,
            verificationUri = response.verificationUri,
            expiresAtMillis = clock.nowMillis() + response.expiresInSeconds * MillisInSecond,
            pollIntervalMillis = response.intervalSeconds * MillisInSecond,
        )
    }

    override suspend fun pollDeviceAuthorization(deviceCode: String): DevicePollResult {
        val tokens = try {
            oAuthApi.exchangeDeviceCode(clientId, Scopes, deviceCode)
        } catch (error: HttpException) {
            return when (error.oAuthMessage()) {
                ErrorPending -> DevicePollResult.Pending
                ErrorSlowDown -> DevicePollResult.SlowDown
                // "invalid device code" and the rest: the code is no longer usable.
                else -> DevicePollResult.Expired
            }
        } catch (error: IOException) {
            return DevicePollResult.Failed(AppError.Network)
        }
        val user = runCatchingRemote { helixApi.getCurrentUser(bearer(tokens.accessToken)).data.first() }
        return when (user) {
            is AppResult.Failure -> DevicePollResult.Failed(user.error)
            is AppResult.Success -> {
                val dto = user.value
                sessionStore.save(
                    Session(tokens.accessToken, tokens.refreshToken, User(dto.id, dto.login, dto.displayName, dto.profileImageUrl)),
                )
                DevicePollResult.SignedIn
            }
        }
    }

    override suspend fun signOut() {
        sessionStore.clear()
    }

    private fun HttpException.oAuthMessage(): String? = try {
        response()?.errorBody()?.string()?.let { json.decodeFromString<OAuthErrorDto>(it).message }
    } catch (error: SerializationException) {
        null
    }
}
