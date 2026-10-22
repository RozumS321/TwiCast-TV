package app.twicast.tv.data.repository

import app.twicast.tv.core.network.Clock
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.FollowedChannel
import app.twicast.tv.data.model.Follows
import app.twicast.tv.data.model.runCatchingRemote
import app.twicast.tv.data.remote.HelixApi
import app.twicast.tv.data.remote.dto.FollowedChannelDto
import javax.inject.Inject
import javax.inject.Singleton

interface FollowsRepository {
    /** Every channel the user follows, with avatar and current or last category. */
    suspend fun getFollows(userId: String): AppResult<Follows>
}

/** Helix maximum per page and per id list. */
private const val PageSize = 100
/** Enough for any realistic TV list; the screen says the total anyway. */
private const val MaxPages = 5
private const val CacheTtlMillis = 60_000L

@Singleton
class HelixFollowsRepository @Inject constructor(
    private val api: HelixApi,
    clock: Clock,
) : FollowsRepository {

    private val cache = TtlCache<String, Follows>(clock, CacheTtlMillis)

    override suspend fun getFollows(userId: String): AppResult<Follows> = cache.getOrLoad(userId) {
        runCatchingRemote {
            val (total, followed) = loadFollowed(userId)
            // Two batched calls per 100 channels: avatars (/users) and last category (/channels).
            val chunks = followed.chunked(PageSize)
            val avatars = chunks.flatMap { chunk -> api.getUsers(chunk.map { it.id }).data }
                .associate { it.id to it.profileImageUrl }
            val categories = chunks.flatMap { chunk -> api.getChannels(chunk.map { it.id }).data }
                .associate { it.id to it.gameName }
            Follows(
                total = total,
                channels = followed.map {
                    FollowedChannel(it.id, it.login, it.name, avatars[it.id], categories[it.id].orEmpty())
                },
            )
        }
    }

    private suspend fun loadFollowed(userId: String): Pair<Int, List<FollowedChannelDto>> {
        val channels = mutableListOf<FollowedChannelDto>()
        var cursor: String? = null
        var total = 0
        for (page in 0 until MaxPages) {
            val response = api.getFollowedChannels(userId, PageSize, cursor)
            total = response.total ?: total
            channels += response.data
            cursor = response.pagination?.cursor ?: break
        }
        return total.coerceAtLeast(channels.size) to channels
    }
}
