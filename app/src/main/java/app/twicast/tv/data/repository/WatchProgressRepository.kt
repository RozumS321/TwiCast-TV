package app.twicast.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Where the viewer stopped in a recording (mockups "Плеер · продолжаем с места", "Канал · записи с прогрессом"). */
@Serializable
data class WatchProgress(val positionMs: Long, val durationMs: Long, val updatedAtMillis: Long) {
    /** Near the end counts as watched: the next time it starts over, the card strip stays full. */
    val isFinished: Boolean get() = durationMs - positionMs <= WatchProgressRules.FinishedTailMillis

    /** Where to start the next time; null from the start (finished). */
    val resumePositionMs: Long? get() = positionMs.takeUnless { isFinished }

    /** The card strip: how much is watched, a finished one full. */
    val watchedFraction: Float
        get() = if (isFinished || durationMs <= 0) 1f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

object WatchProgressRules {
    /** Less than this is a peek, not watching: nothing is remembered. */
    const val MinWatchedMillis = 30_000L

    /** The last two minutes (credits, the raid) count as watched. */
    const val FinishedTailMillis = 120_000L

    /** Recordings remembered; the oldest go first. */
    const val MaxRecordings = 100

    /** Adds [videoId]'s position (or drops it, if it is only a peek) and keeps the newest [MaxRecordings]. */
    fun updated(all: Map<String, WatchProgress>, videoId: String, progress: WatchProgress): Map<String, WatchProgress> {
        val next = all.toMutableMap()
        if (progress.positionMs < MinWatchedMillis) next.remove(videoId) else next[videoId] = progress
        return next.entries.sortedByDescending { it.value.updatedAtMillis }.take(MaxRecordings).associate { it.key to it.value }
    }
}

/** Positions in recordings, on this TV only (Twitch keeps none for third-party apps). */
interface WatchProgressRepository {
    /** By video id. */
    val progress: Flow<Map<String, WatchProgress>>

    suspend fun get(videoId: String): WatchProgress?

    suspend fun save(videoId: String, positionMs: Long, durationMs: Long)
}

private val Context.watchProgressDataStore: DataStore<Preferences> by preferencesDataStore(name = "watch_progress")

@Singleton
class DataStoreWatchProgressRepository @Inject constructor(
    @ApplicationContext context: Context,
) : WatchProgressRepository {

    private val dataStore = context.watchProgressDataStore
    private val key = stringPreferencesKey("recordings")
    private val json = Json { ignoreUnknownKeys = true }

    override val progress: Flow<Map<String, WatchProgress>> = dataStore.data.map { decode(it[key]) }

    override suspend fun get(videoId: String): WatchProgress? = progress.first()[videoId]

    override suspend fun save(videoId: String, positionMs: Long, durationMs: Long) {
        val entry = WatchProgress(positionMs, durationMs, System.currentTimeMillis())
        dataStore.edit { it[key] = json.encodeToString(WatchProgressRules.updated(decode(it[key]), videoId, entry)) }
    }

    // A damaged or older format starts empty rather than crashing the player.
    private fun decode(raw: String?): Map<String, WatchProgress> = try {
        raw?.let { json.decodeFromString<Map<String, WatchProgress>>(it) }.orEmpty()
    } catch (error: SerializationException) {
        emptyMap()
    } catch (error: IllegalArgumentException) {
        emptyMap()
    }
}
