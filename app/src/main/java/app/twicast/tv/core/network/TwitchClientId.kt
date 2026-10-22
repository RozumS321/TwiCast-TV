package app.twicast.tv.core.network

import javax.inject.Qualifier

/** Twitch Client ID from local.properties (via BuildConfig). The app is a public client: there is no secret. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TwitchClientId

/** Time source, replaceable in tests. */
fun interface Clock {
    fun nowMillis(): Long
}
