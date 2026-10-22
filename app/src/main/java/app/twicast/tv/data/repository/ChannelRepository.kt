package app.twicast.tv.data.repository

import app.twicast.tv.core.network.Clock
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.ChannelPage
import app.twicast.tv.data.model.Clip
import app.twicast.tv.data.model.Video
import app.twicast.tv.data.model.runCatchingRemote
import app.twicast.tv.data.remote.HelixApi
import app.twicast.tv.data.remote.dto.ClipDto
import app.twicast.tv.data.remote.dto.VideoDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface ChannelRepository {
    /** The channel page of [login] as seen by [viewerId] (for "В подписках"). NotFound if there is no such channel. */
    suspend fun getChannel(login: String, viewerId: String): AppResult<ChannelPage>
}

private const val VideoLimit = 20
private const val ClipLimit = 20
/** "Популярные клипы · за 30 дней" (mockup). */
private const val ClipPeriodDays = 30L
private val DurationPart = Regex("(\\d+)([hms])")
private const val SecondsInMinute = 60L
private const val SecondsInHour = 3600L

@Singleton
class HelixChannelRepository @Inject constructor(
    private val api: HelixApi,
    private val clock: Clock,
) : ChannelRepository {

    override suspend fun getChannel(login: String, viewerId: String): AppResult<ChannelPage> {
        val user = when (val result = runCatchingRemote { api.getUsersByLogin(listOf(login)).data.firstOrNull() }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value ?: return AppResult.Failure(AppError.NotFound)
        }
        return runCatchingRemote {
            coroutineScope {
                // Independent requests for one screen: in parallel.
                val stream = async { api.getStreams(first = 1, userIds = listOf(user.id)).data.firstOrNull() }
                val followers = async { api.getChannelFollowers(user.id).total }
                val following = async { api.getFollowedChannels(viewerId, first = 1, broadcasterId = user.id).data.isNotEmpty() }
                val videos = async { api.getVideos(user.id, VideoLimit).data.map { it.toModel() } }
                val clips = async { api.getClips(user.id, ClipLimit, clipsSince()).data.map { it.toModel() } }
                ChannelPage(
                    id = user.id,
                    login = user.login,
                    displayName = user.displayName,
                    description = user.description,
                    avatarUrl = user.profileImageUrl.ifEmpty { null },
                    bannerUrl = user.offlineImageUrl.ifEmpty { null },
                    followerCount = followers.await(),
                    isFollowedByViewer = following.await(),
                    stream = stream.await()?.toModel(user.profileImageUrl.ifEmpty { null }),
                    videos = videos.await(),
                    // Helix returns clips by views already; keep that order.
                    clips = clips.await(),
                )
            }
        }
    }

    private fun clipsSince(): String =
        Instant.ofEpochMilli(clock.nowMillis() - TimeUnit.DAYS.toMillis(ClipPeriodDays)).toString()
}

private fun VideoDto.toModel() = Video(
    id = id,
    title = title,
    createdAtMillis = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrNull(),
    durationSeconds = parseTwitchDuration(duration),
    viewCount = viewCount,
    thumbnailTemplate = thumbnailUrl,
)

private fun ClipDto.toModel() = Clip(id, title, viewCount, duration.toLong(), thumbnailUrl)

/** "3h42m10s" → 13330. */
internal fun parseTwitchDuration(value: String): Long = DurationPart.findAll(value).sumOf { match ->
    val amount = match.groupValues[1].toLong()
    when (match.groupValues[2]) {
        "h" -> amount * SecondsInHour
        "m" -> amount * SecondsInMinute
        else -> amount
    }
}
