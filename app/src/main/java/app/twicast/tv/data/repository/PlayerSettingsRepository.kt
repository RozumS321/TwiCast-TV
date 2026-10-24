package app.twicast.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.twicast.tv.data.model.PlayerEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface PlayerSettingsRepository {
    /**
     * Quality for new streams: a group chosen in the player ("720p60"), a ceiling chosen in Settings ("720p",
     * see [qualityCeiling]) or "auto"; null if never chosen.
     */
    suspend fun preferredQuality(): String?

    /** [preferredQuality] as it changes (Settings highlights it). */
    val preferredQualityUpdates: Flow<String?>

    suspend fun setPreferredQuality(qualityId: String)

    /** Which player opens streams (screen "Настройки"); TwiCast by default. */
    val playerEngine: Flow<PlayerEngine>

    suspend fun setPlayerEngine(engine: PlayerEngine)

    /** "Открывать чат вместе со стримом" (Settings); off by default. Live streams only. */
    val openChatWithStream: Flow<Boolean>

    suspend fun setOpenChatWithStream(open: Boolean)

    /** "Сообщать о начале эфиров" (Settings): a system notification when a followed channel goes live. On by default. */
    val notifyLiveStarts: Flow<Boolean>

    suspend fun setNotifyLiveStarts(enabled: Boolean)
}

private val CeilingPattern = Regex("^(\\d+)p$")

/** "720p" from Settings means "the best quality up to 720"; a player group like "720p60" is exact, so null. */
fun qualityCeiling(qualityId: String?): Int? = qualityId?.let { CeilingPattern.find(it) }?.groupValues?.get(1)?.toIntOrNull()

private val Context.playerSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_settings")

@Singleton
class DataStorePlayerSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : PlayerSettingsRepository {

    private val dataStore = context.playerSettingsDataStore
    private val preferredQualityKey = stringPreferencesKey("preferred_quality")
    private val playerEngineKey = stringPreferencesKey("player_engine")
    private val openChatKey = booleanPreferencesKey("open_chat_with_stream")
    private val notifyLiveKey = booleanPreferencesKey("notify_live_starts")

    override val preferredQualityUpdates: Flow<String?> = dataStore.data.map { it[preferredQualityKey] }

    override suspend fun preferredQuality(): String? = preferredQualityUpdates.first()

    override suspend fun setPreferredQuality(qualityId: String) {
        dataStore.edit { it[preferredQualityKey] = qualityId }
    }

    override val playerEngine: Flow<PlayerEngine> = dataStore.data.map { preferences ->
        PlayerEngine.entries.firstOrNull { it.name == preferences[playerEngineKey] } ?: PlayerEngine.TwiCast
    }

    override suspend fun setPlayerEngine(engine: PlayerEngine) {
        dataStore.edit { it[playerEngineKey] = engine.name }
    }

    override val openChatWithStream: Flow<Boolean> = dataStore.data.map { it[openChatKey] ?: false }

    override suspend fun setOpenChatWithStream(open: Boolean) {
        dataStore.edit { it[openChatKey] = open }
    }

    override val notifyLiveStarts: Flow<Boolean> = dataStore.data.map { it[notifyLiveKey] ?: true }

    override suspend fun setNotifyLiveStarts(enabled: Boolean) {
        dataStore.edit { it[notifyLiveKey] = enabled }
    }
}
