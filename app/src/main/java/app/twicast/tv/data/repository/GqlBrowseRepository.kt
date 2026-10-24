package app.twicast.tv.data.repository

import app.twicast.tv.core.network.Clock
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.ChannelPage
import app.twicast.tv.data.model.ChannelSearchResult
import app.twicast.tv.data.model.Clip
import app.twicast.tv.data.model.Page
import app.twicast.tv.data.model.Stream
import app.twicast.tv.data.model.Video
import app.twicast.tv.data.model.runCatchingRemote
import app.twicast.tv.data.remote.GqlBroadcaster
import app.twicast.tv.data.remote.GqlCategoryStreamsQuery
import app.twicast.tv.data.remote.GqlChannel
import app.twicast.tv.data.remote.GqlChannelQuery
import app.twicast.tv.data.remote.GqlGame
import app.twicast.tv.data.remote.GqlRequest
import app.twicast.tv.data.remote.GqlSearchQuery
import app.twicast.tv.data.remote.GqlStream
import app.twicast.tv.data.remote.GqlStreamConnection
import app.twicast.tv.data.remote.GqlTopCategoriesQuery
import app.twicast.tv.data.remote.GqlTopStreamsQuery
import app.twicast.tv.data.remote.GqlUsersQuery
import app.twicast.tv.data.remote.TwitchGqlApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val CacheTtlMillis = 60_000L
private const val ChannelVideos = 20
private const val ChannelClips = 20

/** "…-440x248.jpg" → "…-{width}x{height}.jpg": GQL hands out sized images, the app sizes them per card. */
internal fun sizedToTemplate(url: String?): String =
    url.orEmpty().replace(SizedSuffix, "-{width}x{height}$1")

private val SizedSuffix = Regex("-\\d+x\\d+(\\.\\w+)$")

private fun parseMillis(value: String?): Long? = value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

internal fun GqlStream.toModel(broadcaster: GqlBroadcaster): Stream = Stream(
    id = id,
    userId = broadcaster.id,
    userLogin = broadcaster.login,
    channelName = broadcaster.displayName ?: broadcaster.login,
    title = title.orEmpty(),
    categoryId = game?.id.orEmpty(),
    categoryName = game?.name.orEmpty(),
    viewerCount = viewersCount,
    startedAtMillis = parseMillis(createdAt),
    language = language.orEmpty().lowercase(),
    thumbnailTemplate = sizedToTemplate(previewImageURL),
    avatarUrl = broadcaster.profileImageURL,
    tags = emptyList(),
)

private fun GqlGame.toCategory() = Category(id, name.orEmpty(), sizedToTemplate(boxArtURL))

private fun GqlStreamConnection?.toPage(): Page<Stream> {
    val edges = this?.edges.orEmpty()
    val streams = edges.mapNotNull { edge -> edge.node.broadcaster?.let { edge.node.toModel(it) } }
    return Page(streams, edges.lastOrNull()?.cursor?.takeIf { this?.pageInfo?.hasNextPage == true })
}

private fun variables(vararg pairs: Pair<String, Any?>) = JsonObject(
    pairs.filter { it.second != null }.associate { (key, value) ->
        key to when (value) {
            is List<*> -> JsonArray(value.map { JsonPrimitive(it.toString()) })
            is Number -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
    },
)

internal fun GqlChannel.toPage(): ChannelPage = ChannelPage(
    id = id,
    login = login,
    displayName = displayName.ifEmpty { login },
    description = description.orEmpty(),
    avatarUrl = profileImageURL,
    bannerUrl = offlineImageURL?.ifEmpty { null },
    followerCount = followers?.totalCount,
    // Nobody is signed in: there is no "following" to show.
    isFollowedByViewer = false,
    stream = stream?.toModel(GqlBroadcaster(id, login, displayName, profileImageURL)),
    videos = videos?.edges.orEmpty().map { (video) ->
        Video(
            id = video.id,
            title = video.title.orEmpty(),
            createdAtMillis = parseMillis(video.createdAt),
            durationSeconds = video.lengthSeconds,
            viewCount = video.viewCount,
            thumbnailTemplate = sizedToTemplate(video.previewThumbnailURL),
        )
    },
    clips = clips?.edges.orEmpty().map { (clip) ->
        Clip(clip.slug, clip.title.orEmpty(), clip.viewCount, clip.durationSeconds, clip.thumbnailURL.orEmpty())
    },
)

/**
 * What a guest browses, from gql.twitch.tv (see [TwitchGqlApi]): the same shapes as the Helix repositories,
 * so the screens do not care who answered.
 */
@Singleton
class GqlBrowseRepository @Inject constructor(
    private val api: TwitchGqlApi,
    clock: Clock,
) {
    private val topCache = TtlCache<Pair<String?, Int>, List<Stream>>(clock, CacheTtlMillis)
    private val categoriesCache = TtlCache<Int, Page<Category>>(clock, CacheTtlMillis)

    suspend fun topStreams(language: String?, limit: Int): AppResult<List<Stream>> = topCache.getOrLoad(language to limit) {
        runCatchingRemote {
            val request = GqlRequest(GqlTopStreamsQuery, variables("first" to limit, "languages" to language?.let { listOf(it.uppercase()) }))
            api.topStreams(request).data?.streams.toPage().items
        }
    }

    suspend fun categoryStreams(categoryId: String, language: String?, limit: Int, cursor: String?): AppResult<Page<Stream>> =
        runCatchingRemote {
            val request = GqlRequest(
                GqlCategoryStreamsQuery,
                variables("id" to categoryId, "first" to limit, "after" to cursor, "languages" to language?.let { listOf(it.lowercase()) }),
            )
            api.categoryStreams(request).data?.game?.streams.toPage()
        }

    suspend fun topCategories(limit: Int, cursor: String?): AppResult<Page<Category>> {
        val load: suspend () -> AppResult<Page<Category>> = {
            runCatchingRemote {
                val games = api.topCategories(GqlRequest(GqlTopCategoriesQuery, variables("first" to limit, "after" to cursor))).data?.games
                val edges = games?.edges.orEmpty()
                Page(edges.map { it.node.toCategory() }, edges.lastOrNull()?.cursor?.takeIf { games?.pageInfo?.hasNextPage == true })
            }
        }
        return if (cursor == null) categoriesCache.getOrLoad(limit, load) else load()
    }

    /** The live streams of those of [logins] that broadcast now. */
    suspend fun liveStreams(logins: List<String>): AppResult<List<Stream>> {
        if (logins.isEmpty()) return AppResult.Success(emptyList())
        return runCatchingRemote {
            val users = api.usersByLogin(GqlRequest(GqlUsersQuery, variables("logins" to logins.map { it.lowercase() }))).data?.users.orEmpty()
            users.filterNotNull().mapNotNull { user ->
                user.stream?.toModel(GqlBroadcaster(user.id, user.login, user.displayName, user.profileImageURL))
            }
        }
    }

    suspend fun isLive(login: String): AppResult<Boolean> = when (val live = liveStreams(listOf(login))) {
        is AppResult.Failure -> live
        is AppResult.Success -> AppResult.Success(live.value.isNotEmpty())
    }

    suspend fun search(query: String, channelLimit: Int, categoryLimit: Int): AppResult<SearchResults> = runCatchingRemote {
        val data = api.search(GqlRequest(GqlSearchQuery, variables("query" to query, "channels" to channelLimit, "categories" to categoryLimit))).data
        SearchResults(
            channels = data?.searchFor?.channels?.edges.orEmpty().mapNotNull { it.item }.map { channel ->
                ChannelSearchResult(
                    id = channel.id,
                    login = channel.login,
                    displayName = channel.displayName.ifEmpty { channel.login },
                    avatarUrl = channel.profileImageURL,
                    isLive = channel.stream != null,
                    categoryName = (channel.stream?.game ?: channel.broadcastSettings?.game)?.name.orEmpty(),
                    title = channel.stream?.title.orEmpty(),
                )
            }.sortedByDescending { it.isLive },
            categories = data?.searchCategories?.edges.orEmpty().map { it.node.toCategory() },
        )
    }

    suspend fun channel(login: String): AppResult<ChannelPage> {
        val result = runCatchingRemote {
            api.channel(GqlRequest(GqlChannelQuery, variables("login" to login.lowercase(), "videos" to ChannelVideos, "clips" to ChannelClips)))
                .data?.user?.toPage()
        }
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> result.value?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound)
        }
    }
}
