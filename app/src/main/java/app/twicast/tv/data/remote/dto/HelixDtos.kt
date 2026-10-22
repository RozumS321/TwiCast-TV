package app.twicast.tv.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HelixPage<T>(
    val data: List<T>,
    val pagination: HelixPagination? = null,
    /** Only some endpoints (e.g. channels/followed) send it. */
    val total: Int? = null,
)

@Serializable
data class HelixPagination(val cursor: String? = null)

@Serializable
data class StreamDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_login") val userLogin: String,
    @SerialName("user_name") val userName: String,
    @SerialName("game_id") val gameId: String,
    @SerialName("game_name") val gameName: String,
    val title: String,
    @SerialName("viewer_count") val viewerCount: Int,
    @SerialName("started_at") val startedAt: String,
    val language: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String,
    val tags: List<String>? = null,
)

@Serializable
data class GameDto(
    val id: String,
    val name: String,
    @SerialName("box_art_url") val boxArtUrl: String,
)

@Serializable
data class UserDto(
    val id: String,
    val login: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("profile_image_url") val profileImageUrl: String,
)

@Serializable
data class DeviceCodeDto(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresInSeconds: Long,
    @SerialName("interval") val intervalSeconds: Long,
)

@Serializable
data class TokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresInSeconds: Long,
)

/** Error body of id.twitch.tv, e.g. {"status":400,"message":"authorization_pending"}. */
@Serializable
data class OAuthErrorDto(val status: Int = 0, val message: String = "")

@Serializable
data class SearchChannelDto(
    val id: String,
    @SerialName("broadcaster_login") val login: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("game_name") val gameName: String = "",
    @SerialName("is_live") val isLive: Boolean = false,
    val title: String = "",
    @SerialName("thumbnail_url") val avatarUrl: String = "",
)

@Serializable
data class FollowedChannelDto(
    @SerialName("broadcaster_id") val id: String,
    @SerialName("broadcaster_login") val login: String,
    @SerialName("broadcaster_name") val name: String,
)

@Serializable
data class ChannelInfoDto(
    @SerialName("broadcaster_id") val id: String,
    @SerialName("game_name") val gameName: String = "",
)
