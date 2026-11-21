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
import java.io.File
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val ApkExtension = ".apk"
private const val ReleasesCacheFile = "releases.json"
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
    private val cache = ReleasesCache(File(context.filesDir, ReleasesCacheFile))

    override suspend fun latestRelease(): AppResult<AppRelease?> =
        runCatchingRemote { api.latestRelease(owner, repo).toModel() }

    override suspend fun releasesBetween(afterVersionCode: Int, upToVersionCode: Int): AppResult<List<AppRelease>> =
        runCatchingRemote { api.releasesBetween(owner, repo, afterVersionCode, upToVersionCode) }

    // GitHub allows 60 requests an hour per home network without a token: when it says no (or the network is
    // down), "Что нового" shows the list it loaded last time instead of an error.
    override suspend fun allReleases(): AppResult<List<AppRelease>> {
        val result = runCatchingRemote { api.releasesBetween(owner, repo, afterVersionCode = 0, upToVersionCode = Int.MAX_VALUE) }
        return when (result) {
            is AppResult.Success -> result.also { cache.save(it.value) }
            is AppResult.Failure -> cache.load()?.let { AppResult.Success(it) } ?: result
        }
    }

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

/** The last release list that loaded, kept on disk for when GitHub refuses or the network is down. */
internal class ReleasesCache(private val file: File) {

    @Serializable
    private data class Entry(
        val versionCode: Int,
        val versionName: String,
        val added: List<String>,
        val fixed: List<String>,
        val improved: List<String>,
        val apkUrl: String,
        val apkSizeBytes: Long,
        val publishedAtMillis: Long?,
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun save(releases: List<AppRelease>) {
        val entries = releases.map {
            Entry(it.versionCode, it.versionName, it.notes.added, it.notes.fixed, it.notes.improved, it.apkUrl, it.apkSizeBytes, it.publishedAtMillis)
        }
        runCatching { file.writeText(json.encodeToString(entries)) }
    }

    fun load(): List<AppRelease>? = runCatching {
        json.decodeFromString<List<Entry>>(file.readText()).map {
            AppRelease(it.versionCode, it.versionName, ReleaseNotes(it.added, it.fixed, it.improved), it.apkUrl, it.apkSizeBytes, it.publishedAtMillis)
        }
    }.getOrNull()
}
