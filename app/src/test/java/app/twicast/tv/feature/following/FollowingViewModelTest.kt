package app.twicast.tv.feature.following

import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.FollowedChannel
import app.twicast.tv.data.model.Follows
import app.twicast.tv.data.repository.FollowsRepository
import app.twicast.tv.feature.profile.ProfileViewModel
import app.twicast.tv.testing.FakeAuthRepository
import app.twicast.tv.testing.FakeStreamsRepository
import app.twicast.tv.testing.MainDispatcherRule
import app.twicast.tv.testing.stream
import app.twicast.tv.testing.testUser
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private class FakeFollowsRepository(var result: AppResult<Follows> = AppResult.Success(Follows(0, emptyList()))) : FollowsRepository {
    override suspend fun getFollows(userId: String) = result
}

/** Matches [stream]'s ids: stream("1") is user "user-1". */
private fun channel(id: String) = FollowedChannel("user-$id", "login_$id", "Channel $id", null, "Game")

class FollowingViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val auth = FakeAuthRepository(initialState = AuthState.SignedIn(testUser))
    private val streams = FakeStreamsRepository()
    private val follows = FakeFollowsRepository()

    private fun kotlinx.coroutines.test.TestScope.viewModel() =
        FollowingViewModel(auth, streams, follows).also { runCurrent() }

    @Test
    fun `live channels are not repeated in offline`() = runTest(mainRule.dispatcher) {
        streams.followed = AppResult.Success(listOf(stream("1")))
        follows.result = AppResult.Success(Follows(3, listOf(channel("1"), channel("2"), channel("3"))))

        val content = viewModel().uiState.value as FollowingUiState.Content
        assertEquals(FollowingTab.Live, content.tab)
        assertEquals(listOf("user-1"), content.live.map { it.userId })
        assertEquals(listOf("user-2", "user-3"), content.offline.map { it.id })
    }

    @Test
    fun `nobody live opens on the offline tab`() = runTest(mainRule.dispatcher) {
        follows.result = AppResult.Success(Follows(1, listOf(channel("2"))))
        val viewModel = viewModel()
        assertEquals(FollowingTab.Offline, (viewModel.uiState.value as FollowingUiState.Content).tab)

        viewModel.onTabSelected(FollowingTab.Live)
        assertEquals(FollowingTab.Live, (viewModel.uiState.value as FollowingUiState.Content).tab)
    }

    @Test
    fun `no follows is empty, failures are errors`() = runTest(mainRule.dispatcher) {
        assertEquals(FollowingUiState.Empty, viewModel().uiState.value)

        follows.result = AppResult.Failure(AppError.Network)
        assertEquals(FollowingUiState.Error(AppError.Network), viewModel().uiState.value)
    }

    @Test
    fun `profile counts follows and live channels`() = runTest(mainRule.dispatcher) {
        streams.followed = AppResult.Success(listOf(stream("1"), stream("2")))
        follows.result = AppResult.Success(Follows(27, emptyList()))

        val state = ProfileViewModel(auth, streams, follows).also { runCurrent() }.uiState.value
        assertEquals(testUser, state.user)
        assertEquals(27, state.followsCount)
        assertEquals(2, state.liveCount)
    }

    @Test
    fun `profile sign out clears the session`() = runTest(mainRule.dispatcher) {
        val viewModel = ProfileViewModel(auth, streams, follows).also { runCurrent() }
        viewModel.onSignOut()
        runCurrent()
        assertEquals(AuthState.SignedOut, auth.authState.value)
    }
}
