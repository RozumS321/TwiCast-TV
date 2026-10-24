package app.twicast.tv.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * https://gql.twitch.tv — Twitch's private web API: playback tokens of the TwiCast player (Helix has no endpoint
 * for stream playlists), and everything a guest browses (Helix needs a token, and an app token needs a secret an
 * APK cannot keep). Unofficial: Twitch may change it without notice. Signed-in viewers browse through Helix.
 */
interface TwitchGqlApi {
    @POST("gql")
    suspend fun playbackToken(@Body request: GqlRequest): GqlResponse<PlaybackTokenData>

    @POST("gql")
    suspend fun clipPlayback(@Body request: GqlRequest): GqlResponse<ClipPlaybackData>

    // Guest browsing (no sign-in): the same queries twitch.tv runs for visitors. See GqlBrowse.kt.

    @POST("gql")
    suspend fun topStreams(@Body request: GqlRequest): GqlResponse<GqlTopStreamsData>

    @POST("gql")
    suspend fun categoryStreams(@Body request: GqlRequest): GqlResponse<GqlCategoryStreamsData>

    @POST("gql")
    suspend fun topCategories(@Body request: GqlRequest): GqlResponse<GqlTopCategoriesData>

    @POST("gql")
    suspend fun usersByLogin(@Body request: GqlRequest): GqlResponse<GqlUsersData>

    @POST("gql")
    suspend fun search(@Body request: GqlRequest): GqlResponse<GqlSearchData>

    @POST("gql")
    suspend fun channel(@Body request: GqlRequest): GqlResponse<GqlChannelData>
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
