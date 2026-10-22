package app.twicast.tv.data.model

/** Live stream. Image fields are Twitch templates with `{width}x{height}` placeholders. */
data class Stream(
    val id: String,
    val userId: String,
    val userLogin: String,
    val channelName: String,
    val title: String,
    val categoryId: String,
    val categoryName: String,
    val viewerCount: Int,
    /** Null if Twitch sent an unparsable date. */
    val startedAtMillis: Long?,
    val language: String,
    val thumbnailTemplate: String,
    val avatarUrl: String?,
    val tags: List<String>,
)

/** Category (game). */
data class Category(
    val id: String,
    val name: String,
    val boxArtTemplate: String,
)

/** Signed-in Twitch user. */
data class User(
    val id: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
)

/** Stored sign-in: user tokens (Device Flow) plus the user they belong to. */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
)

sealed interface AuthState {
    /** Stored session is being read. */
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: User) : AuthState
}

/** One page of a Helix list; [nextCursor] is null on the last page. */
data class Page<T>(
    val items: List<T>,
    val nextCursor: String?,
)

/** Channel found by search. [categoryName] is the current one if live, the last one otherwise. */
data class ChannelSearchResult(
    val id: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val isLive: Boolean,
    val categoryName: String,
    val title: String,
)

/** A channel the user follows; [categoryName] is the current or last one. */
data class FollowedChannel(
    val id: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val categoryName: String,
)

/** All follows: [total] as Twitch counts them, [channels] as loaded (capped for huge follow lists). */
data class Follows(
    val total: Int,
    val channels: List<FollowedChannel>,
)

/** Past broadcast (VOD). [thumbnailTemplate] is empty while Twitch still processes it. */
data class Video(
    val id: String,
    val title: String,
    val createdAtMillis: Long?,
    val durationSeconds: Long,
    val viewCount: Int,
    val thumbnailTemplate: String,
)

/** Clip; [id] is the slug the clip embed takes. */
data class Clip(
    val id: String,
    val title: String,
    val viewCount: Int,
    val durationSeconds: Long,
    val thumbnailUrl: String,
)

/** Everything the channel page shows. [stream] is null while the channel is offline. */
data class ChannelPage(
    val id: String,
    val login: String,
    val displayName: String,
    val description: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val followerCount: Int?,
    val isFollowedByViewer: Boolean,
    val stream: Stream?,
    val videos: List<Video>,
    val clips: List<Clip>,
)

/** The player that opens streams: our own (ExoPlayer on Twitch's HLS) or the official Twitch embed. */
enum class PlayerEngine { TwiCast, Twitch }
