package app.twicast.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.twicast.tv.data.model.Stream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Which followed channels were live at the last background check: only channels new since then are announced. */
interface LiveNotificationStateRepository {
    /** Null before the first check. */
    suspend fun lastLive(): Set<String>?

    suspend fun setLastLive(logins: Set<String>)
}

/**
 * The channels to announce: live now and not at the last check. The first check only notes who is live — a
 * notification for every channel already on air would be noise.
 */
fun newlyLive(previous: Set<String>?, current: List<Stream>): List<Stream> {
    if (previous == null) return emptyList()
    return current.filter { it.userLogin.lowercase() !in previous }
}

private val Context.liveNotificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "live_notifications")

@Singleton
class DataStoreLiveNotificationStateRepository @Inject constructor(
    @ApplicationContext context: Context,
) : LiveNotificationStateRepository {

    private val dataStore = context.liveNotificationDataStore
    private val key = stringSetPreferencesKey("last_live")

    override suspend fun lastLive(): Set<String>? = dataStore.data.first()[key]

    override suspend fun setLastLive(logins: Set<String>) {
        dataStore.edit { it[key] = logins.map { login -> login.lowercase() }.toSet() }
    }
}
