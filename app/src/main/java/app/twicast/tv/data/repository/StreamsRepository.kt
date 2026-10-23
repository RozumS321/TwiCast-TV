package app.twicast.tv.data.repository

import app.twicast.tv.core.network.Clock
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.Page
import app.twicast.tv.data.model.Stream
import app.twicast.tv.data.model.runCatchingRemote
import app.twicast.tv.data.remote.HelixApi
import app.twicast.tv.data.remote.dto.StreamDto
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface StreamsRepository {
    /** Top live streams by viewers; [language] is an ISO 639-1 code, null for the whole world. */
    suspend fun getTopStreams(language: String?, limit: Int): AppResult<List<Stream>>

    /** Whether [login] is broadcasting right now; never cached (the player asks when a live stream stalls). */
    suspend fun isLive(login: String): AppResult<Boolean>

    /** Live channels the user follows, by viewers (scope user:read:follows). */
    suspend fun getFollowedStreams(userId: String, limit: Int): AppResult<List<Stream>>

    /** Live streams of one category by viewers, a page at a time; [language] null = all languages. */
    suspend fun getCategoryStreams(categoryId: String, language: String?, limit: Int, cursor: String? = null): AppResult<Page<Stream>>

    /** Top live streams across [categoryIds] (one request, up to 100 ids), optionally in one [language]. */
    suspend fun getTopStreamsInCategories(categoryIds: List<String>, language: String?, limit: Int): AppResult<List<Stream>>
}

private const val CacheTtlMillis = 60_000L

@Singleton
class HelixStreamsRepository @Inject constructor(
    private val api: HelixApi,
    clock: Clock,
) : StreamsRepository {

    private val cache = TtlCache<CacheKey, List<Stream>>(clock, CacheTtlMillis)

    override suspend fun getTopStreams(language: String?, limit: Int): AppResult<List<Stream>> =
        cache.getOrLoad(CacheKey.Top(language, limit)) {
            runCatchingRemote { api.getStreams(first = limit, language = language).data.withAvatars() }
        }

    override suspend fun isLive(login: String): AppResult<Boolean> =
        runCatchingRemote { api.getStreams(first = 1, userLogins = listOf(login.lowercase())).data.isNotEmpty() }

    override suspend fun getFollowedStreams(userId: String, limit: Int): AppResult<List<Stream>> =
        cache.getOrLoad(CacheKey.Followed(userId, limit)) {
            runCatchingRemote { api.getFollowedStreams(userId = userId, first = limit).data.withAvatars() }
        }

    override suspend fun getTopStreamsInCategories(
        categoryIds: List<String>,
        language: String?,
        limit: Int,
    ): AppResult<List<Stream>> {
        if (categoryIds.isEmpty()) return AppResult.Success(emptyList())
        return cache.getOrLoad(CacheKey.InCategories(categoryIds.sorted(), language, limit)) {
            runCatchingRemote { api.getStreams(first = limit, language = language, gameIds = categoryIds).data.withAvatars() }
        }
    }

    override suspend fun getCategoryStreams(
        categoryId: String,
        language: String?,
        limit: Int,
        cursor: String?,
    ): AppResult<Page<Stream>> = runCatchingRemote {
        val page = api.getStreams(first = limit, language = language, gameIds = listOf(categoryId), after = cursor)
        Page(page.data.withAvatars(), page.pagination?.cursor)
    }

    /** /streams has no avatars: one /users call for the whole page. */
    private suspend fun List<StreamDto>.withAvatars(): List<Stream> {
        if (isEmpty()) return emptyList()
        val avatars = api.getUsers(map { it.userId }.distinct()).data.associate { it.id to it.profileImageUrl }
        return map { it.toModel(avatars[it.userId]) }
    }

    private sealed interface CacheKey {
        data class Top(val language: String?, val limit: Int) : CacheKey
        data class Followed(val userId: String, val limit: Int) : CacheKey
        data class InCategories(val categoryIds: List<String>, val language: String?, val limit: Int) : CacheKey
    }
}

internal fun StreamDto.toModel(avatarUrl: String?) = Stream(
    id = id,
    userId = userId,
    userLogin = userLogin,
    channelName = userName,
    title = title,
    categoryId = gameId,
    categoryName = gameName,
    viewerCount = viewerCount,
    startedAtMillis = runCatching { Instant.parse(startedAt).toEpochMilli() }.getOrNull(),
    language = language,
    thumbnailTemplate = thumbnailUrl,
    avatarUrl = avatarUrl,
    tags = tags.orEmpty(),
)
