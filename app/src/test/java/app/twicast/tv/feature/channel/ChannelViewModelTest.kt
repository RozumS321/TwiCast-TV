package app.twicast.tv.feature.channel

import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.ChannelPage
import app.twicast.tv.data.repository.ChannelRepository
import app.twicast.tv.data.repository.parseTwitchDuration
import app.twicast.tv.feature.channel.components.channelUrl
import app.twicast.tv.testing.FakeAuthRepository
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
    val requests = mutableListOf<Pair<String, String>>()
    override suspend fun getChannel(login: String, viewerId: String): AppResult<ChannelPage> {
        requests += login to viewerId
        return result
    }
}

class ChannelViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(AuthState.SignedIn(testUser))

    @Test
    fun `loads the channel as seen by the signed-in viewer`() = runTest {
        val repository = FakeChannelRepository(AppResult.Success(page))
        val viewModel = ChannelViewModel("sasavot", auth, repository)

        assertEquals(ChannelUiState.Content(page), viewModel.uiState.value)
        assertEquals(listOf("sasavot" to testUser.id), repository.requests)
    }

    @Test
    fun `retry after an error loads again`() = runTest {
        val repository = FakeChannelRepository(AppResult.Failure(AppError.NotFound))
        val viewModel = ChannelViewModel("sasavot", auth, repository)
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
        val viewModel = ChannelViewModel("sasavot", auth, repository)

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
        val viewModel = ChannelViewModel("sasavot", auth, repository)
        viewModel.onFollowClick()

        repository.result = AppResult.Failure(AppError.Network)
        viewModel.onFollowDialogClosed()

        assertEquals(ChannelUiState.Content(page), viewModel.uiState.value)
    }

    @Test
    fun `the QR leads to the channel on twitch tv`() {
        assertEquals("https://www.twitch.tv/sasavot", channelUrl("sasavot"))
    }
}
