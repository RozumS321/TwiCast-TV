package app.twicast.tv.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * https://gql.twitch.tv — Twitch's private web API, used only for playback tokens of the TwiCast player
 * (Helix has no endpoint for stream playlists). Unofficial: Twitch may change it without notice;
 * the official embed (Twitch player in settings) stays as the fallback.
 */
interface TwitchGqlApi {
    @POST("gql")
    suspend fun playbackToken(@Body request: GqlRequest): GqlResponse<PlaybackTokenData>

    @POST("gql")
    suspend fun clipPlayback(@Body request: GqlRequest): GqlResponse<ClipPlaybackData>
}

@Serializable
data class GqlRequest(val query: String, val variables: JsonObject = JsonObject(emptyMap()))

@Serializable
data class GqlResponse<T>(val data: T? = null)

/** Signed token that usher.ttvnw.net takes for a playlist. */
@Serializable
data class AccessTokenDto(val value: String, val signature: String)

@Serializable
data class PlaybackTokenData(
    val streamPlaybackAccessToken: AccessTokenDto? = null,
    val videoPlaybackAccessToken: AccessTokenDto? = null,
)

@Serializable
data class ClipPlaybackData(val clip: ClipPlaybackDto? = null)

@Serializable
data class ClipPlaybackDto(
    val playbackAccessToken: AccessTokenDto? = null,
    val videoQualities: List<ClipQualityDto> = emptyList(),
)

@Serializable
data class ClipQualityDto(val quality: String, val frameRate: Double? = null, val sourceURL: String)
