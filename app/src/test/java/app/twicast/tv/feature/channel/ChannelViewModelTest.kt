package app.twicast.tv.feature.channel

import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.ChannelPage
import app.twicast.tv.data.repository.ChannelRepository
import app.twicast.tv.data.repository.parseTwitchDuration
import app.twicast.tv.feature.channel.components.channelUrl
import app.twicast.tv.testing.FakeAuthRepository
import app.twicast.tv.testing.FakeWatchProgressRepository
import app.twicast.tv.data.repository.WatchProgress
import app.twicast.tv.testing.MainDispatcherRule
import app.twicast.tv.testing.testUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private val page = ChannelPage(
    id = "1", login = "sasavot", displayName = "sasavot", description = "", avatarUrl = null, bannerUrl = null,
    followerCount = 1_852_903, isFollowedByViewer = true, stream = null, videos = emptyList(), clips = emptyList(),
)

private class FakeChannelRepository(var result: AppResult<ChannelPage>) : ChannelRepository {
    val requests = mutableListOf<Pair<String, String?>>()
    override suspend fun getChannel(login: String, viewerId: String?): AppResult<ChannelPage> {
        requests += login to viewerId
        return result
    }
}

class ChannelViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(AuthState.SignedIn(testUser))
    private val watch = FakeWatchProgressRepository()

    @Test
    fun `loads the channel as seen by the signed-in viewer`() = runTest {
        val repository = FakeChannelRepository(AppResult.Success(page))
        val viewModel = ChannelViewModel("sasavot", auth, repository, watch)

        assertEquals(ChannelUiState.Content(page), viewModel.uiState.value)
        assertEquals(listOf("sasavot" to testUser.id), repository.requests)
    }

    @Test
    fun `retry after an error loads again`() = runTest {
        val repository = FakeChannelRepository(AppResult.Failure(AppError.NotFound))
        val viewModel = ChannelViewModel("sasavot", auth, repository, watch)
        assertEquals(ChannelUiState.Error(AppError.NotFound), viewModel.uiState.value)

        repository.result = AppResult.Success(page)
        viewModel.onRetry()

        assertEquals(ChannelUiState.Content(page), viewModel.uiState.value)
    }

    @Test
    fun `parses Twitch durations`() {
        assertEquals(3 * 3600L + 42 * 60 + 10, parseTwitchDuration("3h42m10s"))
        assertEquals(58L, parseTwitchDuration("58s"))
        assertEquals(0L, parseTwitchDuration(""))
    }

    @Test
    fun `formats durations like the player`() {
        assertEquals("3:42:10", formatDuration(13_330))
        assertEquals("0:58", formatDuration(58))
    }

    @Test
    fun `the follow button opens the QR, and closing it reads the follow mark again`() = runTest {
        val repository = FakeChannelRepository(AppResult.Success(page.copy(isFollowedByViewer = false)))
        val viewModel = ChannelViewModel("sasavot", auth, repository, watch)

        viewModel.onFollowClick()
        assertEquals(true, (viewModel.uiState.value as ChannelUiState.Content).followDialog)

        // Followed on the phone meanwhile.
        repository.result = AppResult.Success(page)
        viewModel.onFollowDialogClosed()

        assertEquals(ChannelUiState.Content(page, followDialog = false), viewModel.uiState.value)
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `a failed refresh after the QR keeps the page`() = runTest {
        val repository = FakeChannelRepository(AppResult.Success(page))
        val viewModel = ChannelViewModel("sasavot", auth, repository, watch)
        viewModel.onFollowClick()

        repository.result = AppResult.Failure(AppError.Network)
        viewModel.onFollowDialogClosed()

        assertEquals(ChannelUiState.Content(page), viewModel.uiState.value)
    }

    @Test
    fun `the QR leads to the channel on twitch tv`() {
        assertEquals("https://www.twitch.tv/sasavot", channelUrl("sasavot"))
    }

    @Test
    fun `recordings show where the viewer stopped, and follow new positions`() = runTest {
        val started = WatchProgress(positionMs = 600_000, durationMs = 3_000_000, updatedAtMillis = 1)
        watch.progress.value = mapOf("v1" to started)
        val viewModel = ChannelViewModel("sasavot", auth, FakeChannelRepository(AppResult.Success(page)), watch)
        assertEquals(mapOf("v1" to started), (viewModel.uiState.value as ChannelUiState.Content).watched)

        watch.save("v2", positionMs = 90_000, durationMs = 1_000_000)
        assertEquals(setOf("v1", "v2"), (viewModel.uiState.value as ChannelUiState.Content).watched.keys)
    }

    @Test
    fun `a guest sees the channel page too, asked for without a viewer`() = runTest {
        val repository = FakeChannelRepository(AppResult.Success(page.copy(isFollowedByViewer = false)))
        ChannelViewModel("sasavot", FakeAuthRepository(AuthState.SignedOut), repository, watch)
        assertEquals(listOf("sasavot" to null), repository.requests)
    }
}
