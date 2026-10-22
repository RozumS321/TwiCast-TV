package app.twicast.tv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.twicast.tv.data.model.Session
import app.twicast.tv.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * The signed-in session in DataStore. MVP: plain preferences; encrypted storage is post-MVP.
 * DataStore keeps its own in-memory copy, so [current] is cheap after the first read.
 */
@Singleton
class SessionStore @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.sessionDataStore

    val session: Flow<Session?> = dataStore.data.map { it.toSession() }

    suspend fun current(): Session? = session.first()

    suspend fun save(session: Session) {
        dataStore.edit { prefs ->
            prefs[Keys.AccessToken] = session.accessToken
            prefs[Keys.RefreshToken] = session.refreshToken
            prefs[Keys.UserId] = session.user.id
            prefs[Keys.UserLogin] = session.user.login
            prefs[Keys.UserDisplayName] = session.user.displayName
            session.user.avatarUrl?.let { prefs[Keys.UserAvatar] = it } ?: prefs.remove(Keys.UserAvatar)
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[Keys.AccessToken] = accessToken
            prefs[Keys.RefreshToken] = refreshToken
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toSession(): Session? {
        val user = User(
            id = this[Keys.UserId] ?: return null,
            login = this[Keys.UserLogin] ?: return null,
            displayName = this[Keys.UserDisplayName] ?: return null,
            avatarUrl = this[Keys.UserAvatar],
        )
        return Session(
            accessToken = this[Keys.AccessToken] ?: return null,
            refreshToken = this[Keys.RefreshToken] ?: return null,
            user = user,
        )
    }

    private object Keys {
        val AccessToken = stringPreferencesKey("access_token")
        val RefreshToken = stringPreferencesKey("refresh_token")
        val UserId = stringPreferencesKey("user_id")
        val UserLogin = stringPreferencesKey("user_login")
        val UserDisplayName = stringPreferencesKey("user_display_name")
        val UserAvatar = stringPreferencesKey("user_avatar")
    }
}
