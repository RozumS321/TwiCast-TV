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

/** The first start offers "Войти" or "Продолжить без входа"; once chosen, it is not asked again. */
interface GuestChoiceRepository {
    val guestChosen: Flow<Boolean>

    suspend fun chooseGuest()
}

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")

@Singleton
class DataStoreGuestChoiceRepository @Inject constructor(
    @ApplicationContext context: Context,
) : GuestChoiceRepository {

    private val dataStore = context.onboardingDataStore
    private val key = booleanPreferencesKey("guest_chosen")

    override val guestChosen: Flow<Boolean> = dataStore.data.map { it[key] ?: false }

    override suspend fun chooseGuest() {
        dataStore.edit { it[key] = true }
    }
}

/** "Позже" on the home sign-in banner: hidden until the app starts again (kept in memory only). */
@Singleton
class SignInPrompt @Inject constructor() {
    var dismissed: Boolean = false
}
