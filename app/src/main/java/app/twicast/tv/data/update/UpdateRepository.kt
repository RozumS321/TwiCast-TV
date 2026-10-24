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
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val ApkExtension = ".apk"
private const val ReleasesPerPage = 100

// A bound on requests to the unauthenticated API (60/hour): 1000 releases back is far beyond any real skip.
private const val MaxReleasePages = 10

interface UpdateRepository {
    /** Latest published release, or null if it has no APK or a foreign tag. */
    suspend fun latestRelease(): AppResult<AppRelease?>

    /**
     * Releases with versionCode in (afterVersionCode, upToVersionCode], newest first:
     * every version a user skipped when jumping from one build to another.
     */
    suspend fun releasesBetween(afterVersionCode: Int, upToVersionCode: Int): AppResult<List<AppRelease>>

    /** Every published release, newest first: the "Что нового" screen. */
    suspend fun allReleases(): AppResult<List<AppRelease>>

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

    override suspend fun releasesBetween(afterVersionCode: Int, upToVersionCode: Int): AppResult<List<AppRelease>> =
        runCatchingRemote { api.releasesBetween(owner, repo, afterVersionCode, upToVersionCode) }

    override suspend fun allReleases(): AppResult<List<AppRelease>> =
        runCatchingRemote { api.releasesBetween(owner, repo, afterVersionCode = 0, upToVersionCode = Int.MAX_VALUE) }

    override suspend fun lastSeenVersionCode(): Int? = dataStore.data.map { it[lastSeenKey] }.first()

    override suspend fun setLastSeenVersionCode(versionCode: Int) {
        dataStore.edit { it[lastSeenKey] = versionCode }
    }
}

/** Pages through the release list (newest first) only as far back as [afterVersionCode]. */
internal suspend fun GitHubReleasesApi.releasesBetween(
    owner: String,
    repo: String,
    afterVersionCode: Int,
    upToVersionCode: Int,
): List<AppRelease> {
    val listed = mutableListOf<AppRelease>()
    for (page in 1..MaxReleasePages) {
        val pageReleases = releases(owner, repo, ReleasesPerPage, page)
        pageReleases.mapNotNullTo(listed) { it.toModel() }
        val reachedRangeStart = listed.any { it.versionCode <= afterVersionCode }
        if (pageReleases.size < ReleasesPerPage || reachedRangeStart) break
    }
    return listed
        .filter { it.versionCode in (afterVersionCode + 1)..upToVersionCode }
        .distinctBy { it.versionCode }
        .sortedByDescending { it.versionCode }
}

private fun GitHubReleaseDto.toModel(): AppRelease? {
    val (versionCode, versionName) = AppRelease.parseTag(tagName) ?: return null
    val apk = assets.firstOrNull { it.name.endsWith(ApkExtension) } ?: return null
    val published = publishedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    return AppRelease(versionCode, versionName, ReleaseNotes.parse(body.orEmpty()), apk.downloadUrl, apk.size, published)
}
