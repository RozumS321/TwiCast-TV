package app.twicast.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** "Настройки · Оптимизация": speed against memory. */
interface OptimizationSettingsRepository {
    /** Keep the Twitch player loaded in the background (fast stream start, costs memory). Off by default. */
    val warmTwitchPlayer: Flow<Boolean>

    suspend fun setWarmTwitchPlayer(enabled: Boolean)

    /** Drop the background player and in-memory images when the app is sent to the background. On by default. */
    val freeMemoryInBackground: Flow<Boolean>

    suspend fun setFreeMemoryInBackground(enabled: Boolean)
}

private val Context.optimizationDataStore: DataStore<Preferences> by preferencesDataStore(name = "optimization_settings")

@Singleton
class DataStoreOptimizationSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : OptimizationSettingsRepository {

    private val dataStore = context.optimizationDataStore
    private val warmKey = booleanPreferencesKey("warm_twitch_player")
    private val freeKey = booleanPreferencesKey("free_memory_in_background")

    override val warmTwitchPlayer: Flow<Boolean> = dataStore.data.map { it[warmKey] ?: false }

    override suspend fun setWarmTwitchPlayer(enabled: Boolean) {
        dataStore.edit { it[warmKey] = enabled }
    }

    override val freeMemoryInBackground: Flow<Boolean> = dataStore.data.map { it[freeKey] ?: true }

    override suspend fun setFreeMemoryInBackground(enabled: Boolean) {
        dataStore.edit { it[freeKey] = enabled }
    }
}
