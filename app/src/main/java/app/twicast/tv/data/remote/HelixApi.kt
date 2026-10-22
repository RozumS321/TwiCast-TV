package app.twicast.tv.data.remote

import app.twicast.tv.data.remote.dto.ChannelInfoDto
import app.twicast.tv.data.remote.dto.ClipDto
import app.twicast.tv.data.remote.dto.FollowedChannelDto
import app.twicast.tv.data.remote.dto.FollowerDto
import app.twicast.tv.data.remote.dto.GameDto
import app.twicast.tv.data.remote.dto.HelixPage
import app.twicast.tv.data.remote.dto.SearchChannelDto
import app.twicast.tv.data.remote.dto.StreamDto
import app.twicast.tv.data.remote.dto.UserDto
import app.twicast.tv.data.remote.dto.VideoDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/** Twitch Helix (https://api.twitch.tv/helix). Client-Id and Authorization are added by interceptors. */
interface HelixApi {

    @GET("streams")
    suspend fun getStreams(
        @Query("first") first: Int,
        @Query("language") language: String? = null,
        @Query("game_id") gameIds: List<String>? = null,
        @Query("after") after: String? = null,
        @Query("user_id") userIds: List<String>? = null,
    ): HelixPage<StreamDto>

    @GET("streams/followed")
    suspend fun getFollowedStreams(
        @Query("user_id") userId: String,
        @Query("first") first: Int,
        @Query("after") after: String? = null,
    ): HelixPage<StreamDto>

    /** All channels the user follows (scope user:read:follows), up to 100 per page. */
    @GET("channels/followed")
    suspend fun getFollowedChannels(
        @Query("user_id") userId: String,
        @Query("first") first: Int,
        @Query("after") after: String? = null,
        /** Set to check one channel: the page is empty when the user does not follow it. */
        @Query("broadcaster_id") broadcasterId: String? = null,
    ): HelixPage<FollowedChannelDto>

    /** Channel info (current or last category), up to 100 ids. */
    @GET("channels")
    suspend fun getChannels(@Query("broadcaster_id") ids: List<String>): HelixPage<ChannelInfoDto>

    @GET("games/top")
    suspend fun getTopGames(
        @Query("first") first: Int,
        @Query("after") after: String? = null,
    ): HelixPage<GameDto>

    @GET("search/channels")
    suspend fun searchChannels(
        @Query("query") query: String,
        @Query("first") first: Int,
    ): HelixPage<SearchChannelDto>

    @GET("search/categories")
    suspend fun searchCategories(
        @Query("query") query: String,
        @Query("first") first: Int,
    ): HelixPage<GameDto>

    /** Current user. The token is passed explicitly: this runs during sign-in, before the session is saved. */
    @GET("users")
    suspend fun getCurrentUser(@Header("Authorization") authorization: String): HelixPage<UserDto>

    @GET("users")
    suspend fun getUsersByLogin(@Query("login") logins: List<String>): HelixPage<UserDto>

    /** [HelixPage.total] is the follower count; the list itself is only visible to the broadcaster. */
    @GET("channels/followers")
    suspend fun getChannelFollowers(
        @Query("broadcaster_id") broadcasterId: String,
        @Query("first") first: Int = 1,
    ): HelixPage<FollowerDto>

    @GET("videos")
    suspend fun getVideos(
        @Query("user_id") userId: String,
        @Query("first") first: Int,
        @Query("type") type: String = "archive",
    ): HelixPage<VideoDto>

    @GET("clips")
    suspend fun getClips(
        @Query("broadcaster_id") broadcasterId: String,
        @Query("first") first: Int,
        @Query("started_at") startedAt: String,
    ): HelixPage<ClipDto>

    /** Up to 100 ids per call. */
    @GET("users")
    suspend fun getUsers(@Query("id") ids: List<String>): HelixPage<UserDto>
}
