package app.twicast.tv.data

import app.twicast.tv.data.remote.GqlChannelData
import app.twicast.tv.data.remote.GqlTopStreamsData
import app.twicast.tv.data.repository.toModel
import app.twicast.tv.data.repository.toPage
import app.twicast.tv.data.repository.sizedToTemplate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class GqlBrowseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `sized images become templates the cards size themselves`() {
        assertEquals(
            "https://static-cdn.jtvnw.net/previews-ttv/live_user_strogo-{width}x{height}.jpg",
            sizedToTemplate("https://static-cdn.jtvnw.net/previews-ttv/live_user_strogo-440x248.jpg"),
        )
        assertEquals("", sizedToTemplate(null))
    }

    @Test
    fun `a stream from gql reads like one from helix`() {
        // Trimmed from a real gql.twitch.tv answer.
        val data = json.decodeFromString<GqlTopStreamsData>(
            """{"streams":{"edges":[{"cursor":"c1","node":{"id":"317","title":"G2 0:1 NaVi","viewersCount":34633,
            "createdAt":"2026-10-24T06:52:57Z","language":"RU","previewImageURL":"https://x/live_user_strogo-440x248.jpg",
            "broadcaster":{"id":"233","login":"strogo","displayName":"StRoGo","profileImageURL":"https://x/p-70x70.png"},
            "game":{"id":"32399","name":"Counter-Strike"}}}],"pageInfo":{"hasNextPage":true}}}""",
        )
        val node = data.streams!!.edges.single().node
        val stream = node.toModel(node.broadcaster!!)
        assertEquals("strogo", stream.userLogin)
        assertEquals("StRoGo", stream.channelName)
        assertEquals("Counter-Strike", stream.categoryName)
        assertEquals(34633, stream.viewerCount)
        assertEquals("ru", stream.language)
        assertEquals("https://x/live_user_strogo-{width}x{height}.jpg", stream.thumbnailTemplate)
        assertEquals(java.time.Instant.parse("2026-10-24T06:52:57Z").toEpochMilli(), stream.startedAtMillis)
    }

    @Test
    fun `a channel page from gql has its recordings and clips, and no follow mark`() {
        val data = json.decodeFromString<GqlChannelData>(
            """{"user":{"id":"104","login":"olyashaa","displayName":"olyashaa","description":"d",
            "profileImageURL":"https://x/a.png","offlineImageURL":"","followers":{"totalCount":717838},"stream":null,
            "videos":{"edges":[{"node":{"id":"2906","title":"соло хк","createdAt":"2026-10-21T13:05:18Z","lengthSeconds":16535,
            "viewCount":56114,"previewThumbnailURL":"https://x/thumb-320x180.jpeg"}}]},
            "clips":{"edges":[{"node":{"slug":"Average","title":"Егэ","viewCount":1987,"durationSeconds":15,"thumbnailURL":"https://x/t.jpg"}}]}}}""",
        )
        val page = data.user!!.toPage()
        assertNull(page.stream)
        assertNull("an empty offline image means none", page.bannerUrl)
        assertFalse(page.isFollowedByViewer)
        assertEquals(717838, page.followerCount)
        assertEquals("https://x/thumb-{width}x{height}.jpeg", page.videos.single().thumbnailTemplate)
        assertEquals(16535L, page.videos.single().durationSeconds)
        assertEquals("Average", page.clips.single().id)
    }
}
