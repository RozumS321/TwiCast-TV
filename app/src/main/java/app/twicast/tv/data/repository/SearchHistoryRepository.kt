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
 * A search worth coming back to (mockup "Поиск · недавние"): a channel opened from the results ([channelLogin] set,
 * [text] its name), or a query that led somewhere ([text] the query).
 */
@Serializable
data class SearchHistoryEntry(
    val text: String,
    val channelLogin: String? = null,
    val avatarUrl: String? = null,
    val categoryName: String = "",
) {
    val isChannel: Boolean get() = channelLogin != null

    val key: String get() = channelLogin?.let { "channel:${it.lowercase()}" } ?: "query:${text.trim().lowercase()}"
}

object SearchHistoryRules {
    const val MaxEntries = 10

    /** Shown under "Недавние". */
    const val Shown = 5

    /** [entry] to the front, an older copy of it out. */
    fun updated(all: List<SearchHistoryEntry>, entry: SearchHistoryEntry): List<SearchHistoryEntry> =
        (listOf(entry) + all.filterNot { it.key == entry.key }).take(MaxEntries)
}

/** What was searched on this TV, newest first. */
interface SearchHistoryRepository {
    val entries: Flow<List<SearchHistoryEntry>>

    suspend fun record(entry: SearchHistoryEntry)

    suspend fun clear()
}

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history")

@Singleton
class DataStoreSearchHistoryRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SearchHistoryRepository {

    private val dataStore = context.searchHistoryDataStore
    private val key = stringPreferencesKey("entries")
    private val json = Json { ignoreUnknownKeys = true }

    override val entries: Flow<List<SearchHistoryEntry>> = dataStore.data.map { decode(it[key]) }

    override suspend fun record(entry: SearchHistoryEntry) {
        dataStore.edit { it[key] = json.encodeToString(SearchHistoryRules.updated(decode(it[key]), entry)) }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }

    // A damaged or older format starts empty rather than breaking search.
    private fun decode(raw: String?): List<SearchHistoryEntry> = try {
        raw?.let { json.decodeFromString<List<SearchHistoryEntry>>(it) }.orEmpty()
    } catch (error: SerializationException) {
        emptyList()
    } catch (error: IllegalArgumentException) {
        emptyList()
    }
}
