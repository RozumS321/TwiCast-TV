package app.twicast.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Something the viewer opened in the player (mockup "Главная · недавно смотрели"): a channel's live stream, or a
 * recording when [videoId] is set. Kept as shown at the time, so the row needs no request to draw it.
 */
@Serializable
data class HistoryEntry(
    val channelLogin: String,
    val channelName: String,
    val avatarUrl: String? = null,
    val categoryName: String = "",
    val videoId: String? = null,
    val title: String? = null,
    /** Twitch's thumbnail template of the recording ("…-%{width}x%{height}.jpg"). */
    val thumbnailTemplate: String? = null,
) {
    /** One card per recording, and one per channel for its live streams. */
    val key: String get() = videoId ?: "live:${channelLogin.lowercase()}"

    val isRecording: Boolean get() = videoId != null
}

object WatchHistoryRules {
    /** Entries kept; home shows the first [RowSize]. */
    const val MaxEntries = 20
    const val RowSize = 10

    /** [entry] moves to the front (newest first); an older copy of it goes. */
    fun updated(all: List<HistoryEntry>, entry: HistoryEntry): List<HistoryEntry> =
        (listOf(entry) + all.filterNot { it.key == entry.key }).take(MaxEntries)
}

/** What was watched on this TV, newest first. */
interface WatchHistoryRepository {
    val entries: Flow<List<HistoryEntry>>

    suspend fun record(entry: HistoryEntry)
}

private val Context.watchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "watch_history")

@Singleton
class DataStoreWatchHistoryRepository @Inject constructor(
    @ApplicationContext context: Context,
) : WatchHistoryRepository {

    private val dataStore = context.watchHistoryDataStore
    private val key = stringPreferencesKey("entries")
    private val json = Json { ignoreUnknownKeys = true }

    override val entries: Flow<List<HistoryEntry>> = dataStore.data.map { decode(it[key]) }

    override suspend fun record(entry: HistoryEntry) {
        dataStore.edit { it[key] = json.encodeToString(WatchHistoryRules.updated(decode(it[key]), entry)) }
    }

    // A damaged or older format starts empty rather than breaking home.
    private fun decode(raw: String?): List<HistoryEntry> = try {
        raw?.let { json.decodeFromString<List<HistoryEntry>>(it) }.orEmpty()
    } catch (error: SerializationException) {
        emptyList()
    } catch (error: IllegalArgumentException) {
        emptyList()
    }
}
