package app.twicast.tv.core.network

import app.twicast.tv.data.local.SessionStore
import app.twicast.tv.data.remote.TwitchOAuthApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Request needs a signed-in user but there is no session. IOException so OkHttp fails the call instead of crashing. */
class NotSignedInException : IOException("Not signed in")

/** twitch.clientId is missing from local.properties. */
class MissingClientIdException : IOException("Twitch Client ID is not configured")

/**
 * Refreshes the user token after a 401. Refresh tokens of a public client are single-use, so parallel 401s
 * must not refresh twice: the mutex serialises them and later callers reuse the token the first one got.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val oAuthApi: TwitchOAuthApi,
    private val sessionStore: SessionStore,
    @param:TwitchClientId private val clientId: String,
) {
    private val mutex = Mutex()

    /** New access token, or null if the session is gone — then it is cleared and the app returns to sign-in. */
    suspend fun refresh(rejectedAccessToken: String): String? = mutex.withLock {
        val session = sessionStore.current() ?: return null
        if (session.accessToken != rejectedAccessToken) return session.accessToken
        try {
            val tokens = oAuthApi.refreshToken(clientId, session.refreshToken)
            sessionStore.updateTokens(tokens.accessToken, tokens.refreshToken)
            tokens.accessToken
        } catch (error: HttpException) {
            // 400/401: refresh token revoked or expired (30 days) — sign out.
            sessionStore.clear()
            null
        } catch (error: IOException) {
            // Offline: keep the session, the request just fails with a network error.
            null
        }
    }
}
