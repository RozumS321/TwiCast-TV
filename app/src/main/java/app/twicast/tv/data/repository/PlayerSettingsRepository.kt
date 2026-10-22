package app.twicast.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface PlayerSettingsRepository {
    /** Quality group chosen last time (e.g. "720p60"), null if never chosen. */
    suspend fun preferredQuality(): String?

    suspend fun setPreferredQuality(qualityId: String)
}

private val Context.playerSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_settings")

@Singleton
class DataStorePlayerSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : PlayerSettingsRepository {

    private val dataStore = context.playerSettingsDataStore
    private val preferredQualityKey = stringPreferencesKey("preferred_quality")

    override suspend fun preferredQuality(): String? = dataStore.data.map { it[preferredQualityKey] }.first()

    override suspend fun setPreferredQuality(qualityId: String) {
        dataStore.edit { it[preferredQualityKey] = qualityId }
    }
}
