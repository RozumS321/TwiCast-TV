package app.twicast.tv.core.network

import app.twicast.tv.data.local.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

private const val HeaderClientId = "Client-Id"
private const val HeaderAuthorization = "Authorization"
private const val BearerPrefix = "Bearer "

fun bearer(accessToken: String): String = BearerPrefix + accessToken

/**
 * Adds Client-Id and the user token to every Helix call. A request that already carries Authorization
 * (sign-in, before the session is saved) keeps it. Runs on OkHttp's thread, so blocking here is fine.
 */
class HelixAuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
    @param:TwitchClientId private val clientId: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (clientId.isBlank()) throw MissingClientIdException()
        val request = chain.request()
        val authorization = request.header(HeaderAuthorization)
            ?: runBlocking { sessionStore.current() }?.let { bearer(it.accessToken) }
            ?: throw NotSignedInException()
        return chain.proceed(
            request.newBuilder()
                .header(HeaderClientId, clientId)
                .header(HeaderAuthorization, authorization)
                .build(),
        )
    }
}

/** On 401 refreshes the user token once and retries; gives up (sign-in again) if refresh fails. */
class HelixTokenAuthenticator @Inject constructor(
    private val tokenRefresher: TokenRefresher,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val rejected = response.request.header(HeaderAuthorization)?.removePrefix(BearerPrefix) ?: return null
        val fresh = runBlocking { tokenRefresher.refresh(rejected) } ?: return null
        return response.request.newBuilder().header(HeaderAuthorization, bearer(fresh)).build()
    }
}
