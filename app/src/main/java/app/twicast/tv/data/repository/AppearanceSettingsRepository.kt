package app.twicast.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.twicast.tv.data.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AppearanceSettingsRepository {
    /** Theme chosen in "Настройки · Оформление"; [ThemeMode.System] by default. */
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}

private val Context.appearanceSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "appearance_settings")

@Singleton
class DataStoreAppearanceSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : AppearanceSettingsRepository {

    private val dataStore = context.appearanceSettingsDataStore
    private val themeModeKey = stringPreferencesKey("theme_mode")

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.entries.firstOrNull { it.name == preferences[themeModeKey] } ?: ThemeMode.System
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[themeModeKey] = mode.name }
    }
}
