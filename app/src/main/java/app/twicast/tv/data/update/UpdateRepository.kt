package app.twicast.tv.data.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.twicast.tv.BuildConfig
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.runCatchingRemote
import app.twicast.tv.data.remote.GitHubReleaseDto
import app.twicast.tv.data.remote.GitHubReleasesApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val ApkExtension = ".apk"

interface UpdateRepository {
    /** Latest published release, or null if it has no APK or a foreign tag. */
    suspend fun latestRelease(): AppResult<AppRelease?>

    /** Release of a given version (for "what's new" after an update). */
    suspend fun release(versionName: String): AppResult<AppRelease?>

    /** versionCode the app ran with last time; null on the first run. */
    suspend fun lastSeenVersionCode(): Int?

    suspend fun setLastSeenVersionCode(versionCode: Int)
}

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "updates")

@Singleton
class GitHubUpdateRepository @Inject constructor(
    private val api: GitHubReleasesApi,
    @ApplicationContext context: Context,
) : UpdateRepository {

    private val owner = BuildConfig.UPDATE_REPOSITORY.substringBefore('/')
    private val repo = BuildConfig.UPDATE_REPOSITORY.substringAfter('/')
    private val dataStore = context.updateDataStore
    private val lastSeenKey = intPreferencesKey("last_seen_version_code")

    override suspend fun latestRelease(): AppResult<AppRelease?> =
        runCatchingRemote { api.latestRelease(owner, repo).toModel() }

    override suspend fun release(versionName: String): AppResult<AppRelease?> =
        runCatchingRemote { api.releaseByTag(owner, repo, "v$versionName").toModel() }

    override suspend fun lastSeenVersionCode(): Int? = dataStore.data.map { it[lastSeenKey] }.first()

    override suspend fun setLastSeenVersionCode(versionCode: Int) {
        dataStore.edit { it[lastSeenKey] = versionCode }
    }

    private fun GitHubReleaseDto.toModel(): AppRelease? {
        val (versionCode, versionName) = AppRelease.parseTag(tagName) ?: return null
        val apk = assets.firstOrNull { it.name.endsWith(ApkExtension) } ?: return null
        return AppRelease(versionCode, versionName, ReleaseNotes.parse(body.orEmpty()), apk.downloadUrl, apk.size)
    }
}
