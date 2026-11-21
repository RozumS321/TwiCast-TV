package app.twicast.tv.data.repository

import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.remote.AccessTokenDto
import app.twicast.tv.data.remote.ClipPlaybackData
import app.twicast.tv.data.remote.GqlCategoryStreamsData
import app.twicast.tv.data.remote.GqlChannelData
import app.twicast.tv.data.remote.GqlRequest
import app.twicast.tv.data.remote.GqlResponse
import app.twicast.tv.data.remote.GqlSearchData
import app.twicast.tv.data.remote.GqlTopCategoriesData
import app.twicast.tv.data.remote.GqlTopStreamsData
import app.twicast.tv.data.remote.GqlUsersData
import app.twicast.tv.data.remote.PlaybackTokenData
import app.twicast.tv.data.remote.TwitchGqlApi
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRepositoryTest {

    private class TokenApi : TwitchGqlApi {
        val requests = mutableListOf<GqlRequest>()
        private val token = AccessTokenDto(value = "{\"channel\":\"pixelmira\"}", signature = "sig123")

        override suspend fun playbackToken(request: GqlRequest): GqlResponse<PlaybackTokenData> {
            requests += request
            return GqlResponse(PlaybackTokenData(streamPlaybackAccessToken = token, videoPlaybackAccessToken = token))
        }

        override suspend fun clipPlayback(request: GqlRequest): GqlResponse<ClipPlaybackData> = error("unused")
        override suspend fun topStreams(request: GqlRequest): GqlResponse<GqlTopStreamsData> = error("unused")
        override suspend fun categoryStreams(request: GqlRequest): GqlResponse<GqlCategoryStreamsData> = error("unused")
        override suspend fun topCategories(request: GqlRequest): GqlResponse<GqlTopCategoriesData> = error("unused")
        override suspend fun usersByLogin(request: GqlRequest): GqlResponse<GqlUsersData> = error("unused")
        override suspend fun search(request: GqlRequest): GqlResponse<GqlSearchData> = error("unused")
        override suspend fun channel(request: GqlRequest): GqlResponse<GqlChannelData> = error("unused")
    }

    @Test
    fun `live stream is asked for as the embedded player, which Twitch plays without ads`() = runTest {
        val api = TokenApi()

        GqlPlaybackRepository(api).playbackUrl(PlaybackSource.Live("Pixelmira"))

        val liveParams = api.requests.single().query.substringAfter("streamPlaybackAccessToken").substringBefore("@include")
        assertTrue(liveParams, liveParams.contains("playerType: \"embed\""))
    }

    @Test
    fun `live playlist comes from usher, signed with the token`() = runTest {
        val url = (GqlPlaybackRepository(TokenApi()).playbackUrl(PlaybackSource.Live("Pixelmira")) as AppResult.Success).value.toHttpUrl()

        assertEquals("usher.ttvnw.net", url.host)
        assertEquals("/api/channel/hls/pixelmira.m3u8", url.encodedPath)
        assertEquals("sig123", url.queryParameter("sig"))
    }

    @Test
    fun `recordings keep the site player`() = runTest {
        val api = TokenApi()

        GqlPlaybackRepository(api).playbackUrl(PlaybackSource.Vod("123"))

        val vodParams = api.requests.single().query.substringAfter("videoPlaybackAccessToken").substringBefore("@include")
        assertTrue(vodParams, vodParams.contains("playerType: \"site\""))
    }
}
