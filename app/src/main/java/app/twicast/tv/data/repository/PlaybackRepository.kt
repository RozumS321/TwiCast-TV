package app.twicast.tv.data.repository

import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.runCatchingRemote
import app.twicast.tv.data.remote.AccessTokenDto
import app.twicast.tv.data.remote.GqlRequest
import app.twicast.tv.data.remote.TwitchGqlApi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** What the TwiCast player plays. */
sealed interface PlaybackSource {
    data class Live(val login: String) : PlaybackSource
    data class Vod(val videoId: String) : PlaybackSource
    data class Clip(val slug: String) : PlaybackSource
}

interface PlaybackRepository {
    /** A URL ExoPlayer can play: an HLS master playlist (live, recordings) or an MP4 (clips). */
    suspend fun playbackUrl(source: PlaybackSource): AppResult<String>
}

private const val UsherHost = "usher.ttvnw.net"
private const val PlayerRandomBound = 10_000_000

// The web player's own request (same params as twitch.tv), so usher accepts the token.
private const val TokenQuery = "query(\$login: String!, \$isLive: Boolean!, \$vodID: ID!, \$isVod: Boolean!) { " +
    "streamPlaybackAccessToken(channelName: \$login, params: {platform: \"web\", playerBackend: \"mediaplayer\", playerType: \"site\"}) " +
    "@include(if: \$isLive) { value signature } " +
    "videoPlaybackAccessToken(id: \$vodID, params: {platform: \"web\", playerBackend: \"mediaplayer\", playerType: \"site\"}) " +
    "@include(if: \$isVod) { value signature } }"

private const val ClipQuery = "query(\$slug: ID!) { clip(slug: \$slug) { " +
    "playbackAccessToken(params: {platform: \"web\", playerBackend: \"mediaplayer\", playerType: \"site\"}) { value signature } " +
    "videoQualities { quality frameRate sourceURL } } }"

@Singleton
class GqlPlaybackRepository @Inject constructor(private val api: TwitchGqlApi) : PlaybackRepository {

    override suspend fun playbackUrl(source: PlaybackSource): AppResult<String> {
        val result = runCatchingRemote {
            when (source) {
                is PlaybackSource.Live -> liveUrl(source.login)
                is PlaybackSource.Vod -> vodUrl(source.videoId)
                is PlaybackSource.Clip -> clipUrl(source.slug)
            }
        }
        // A null URL: Twitch answered but has nothing to play (unknown channel, deleted recording or clip).
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> result.value?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound)
        }
    }

    private suspend fun liveUrl(login: String): String? {
        val token = token(login = login, videoId = null)?.streamPlaybackAccessToken ?: return null
        return usherUrl("api/channel/hls/${login.lowercase()}.m3u8", token)
    }

    private suspend fun vodUrl(videoId: String): String? {
        val token = token(login = null, videoId = videoId)?.videoPlaybackAccessToken ?: return null
        return usherUrl("vod/$videoId.m3u8", token)
    }

    private suspend fun token(login: String?, videoId: String?) = api.playbackToken(
        GqlRequest(
            query = TokenQuery,
            variables = buildJsonObject {
                put("login", JsonPrimitive(login.orEmpty()))
                put("isLive", JsonPrimitive(login != null))
                put("vodID", JsonPrimitive(videoId.orEmpty()))
                put("isVod", JsonPrimitive(videoId != null))
            },
        ),
    ).data

    /** Clips are plain MP4 files per quality: the best one, signed. */
    private suspend fun clipUrl(slug: String): String? {
        val clip = api.clipPlayback(GqlRequest(ClipQuery, buildJsonObject { put("slug", JsonPrimitive(slug)) })).data?.clip
            ?: return null
        val token = clip.playbackAccessToken ?: return null
        val best = clip.videoQualities.maxByOrNull { it.quality.toIntOrNull() ?: 0 } ?: return null
        return best.sourceURL.toHttpUrl().newBuilder().signed(token).build().toString()
    }

    private fun usherUrl(path: String, token: AccessTokenDto): String = HttpUrl.Builder()
        .scheme("https")
        .host(UsherHost)
        .addPathSegments(path)
        .signed(token)
        .addQueryParameter("allow_source", "true")
        .addQueryParameter("playlist_include_framerate", "true")
        .addQueryParameter("player_backend", "mediaplayer")
        // H.264 only: TV boxes rarely decode Twitch's HEVC/AV1 transcodes in hardware.
        .addQueryParameter("supported_codecs", "avc1")
        .addQueryParameter("p", Random.nextInt(PlayerRandomBound).toString())
        .build()
        .toString()

    private fun HttpUrl.Builder.signed(token: AccessTokenDto) = this
        .addQueryParameter("sig", token.signature)
        .addQueryParameter("token", token.value)
}
