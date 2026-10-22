package app.twicast.tv.data.repository

import app.twicast.tv.core.network.Clock
import app.twicast.tv.data.model.AppResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory cache of successful results for [ttlMillis] — saves the Helix rate limit (~800 points/min)
 * when the user returns to a screen. Failures are never cached.
 */
class TtlCache<K : Any, V>(
    private val clock: Clock,
    private val ttlMillis: Long,
) {
    private val mutex = Mutex()
    private val entries = mutableMapOf<K, Entry<V>>()

    suspend fun getOrLoad(key: K, load: suspend () -> AppResult<V>): AppResult<V> {
        mutex.withLock { entries[key]?.takeIf { it.expiresAtMillis > clock.nowMillis() } }
            ?.let { return AppResult.Success(it.value) }
        val result = load()
        if (result is AppResult.Success) {
            mutex.withLock { entries[key] = Entry(result.value, clock.nowMillis() + ttlMillis) }
        }
        return result
    }

    private class Entry<V>(val value: V, val expiresAtMillis: Long)
}
